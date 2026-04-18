package com.afp.avaliacao.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afp.avaliacao.data.MetricsRepository
import com.afp.avaliacao.util.ResultState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AtletaMetricsViewModel(
    private val repository: MetricsRepository = MetricsRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsUiState(diasSelecionados = 7))
    val uiState: StateFlow<MetricsUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private var lastFetchTime: Long = 0
    private val CACHE_TIMEOUT = 5 * 60 * 1000 // 5 minutos em milisegundos

    init {
        carregarNome()
        carregarMetricas(forceRefresh = true)
    }

    private fun carregarNome() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val doc = firestore.collection("users").document(uid).get().await()
                    val name = doc.getString("nome") ?: "Atleta"
                    _uiState.update { it.copy(userName = name) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userName = "Atleta") }
            }
        }
    }

    fun onDiasSelected(dias: Int) {
        _uiState.update { it.copy(diasSelecionados = dias) }
        carregarMetricas(forceRefresh = true)
    }

    fun carregarMetricas(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && (now - lastFetchTime) < CACHE_TIMEOUT && _uiState.value.metricsState is ResultState.Success) {
            return // Usa o cache da memória se for recente
        }

        val uid = auth.currentUser?.uid ?: return
        val dias = _uiState.value.diasSelecionados

        // Só mostra "Loading" se não tivermos dados nenhuns ainda
        if (_uiState.value.metricsState !is ResultState.Success) {
            _uiState.update { it.copy(metricsState = ResultState.Loading) }
        }

        viewModelScope.launch {
            try {
                val sessoes = repository.getSessoesTreino(uid, dias)
                lastFetchTime = System.currentTimeMillis()
                
                if (sessoes.isEmpty()) {
                    _uiState.update { it.copy(metricsState = ResultState.Success(
                        ProcessedMetrics(0, 0.0, 0, emptyMap(), emptyList(), emptyList())
                    )) }
                    return@launch
                }

                val totalCarga = sessoes.sumOf { (it["carga"] as? Long)?.toInt() ?: 0 }
                val mediaPse = sessoes.map { (it["pseFoster"] as? Long)?.toDouble() ?: 0.0 }.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0
                val mediaDuracao = sessoes.map { (it["duracaoMin"] as? Long)?.toInt() ?: 0 }.filter { it > 0 }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
                
                val modalidades = mutableMapOf<String, Int>()
                val evolucao = mutableListOf<Pair<String, Int>>()
                val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())

                sessoes.forEach { sessao ->
                    (sessao["atividades"] as? List<*>)?.forEach { 
                        val nome = it.toString()
                        modalidades[nome] = modalidades.getOrDefault(nome, 0) + 1
                    }

                    val data = (sessao["dataCheckin"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                    val carga = (sessao["carga"] as? Long)?.toInt() ?: 0
                    evolucao.add(sdf.format(data) to carga)
                }

                val sessaoMaisRecente = sessoes.firstOrNull()
                val hasOpen = sessaoMaisRecente != null && 
                             sessaoMaisRecente["bemEstar"] != null && 
                             sessaoMaisRecente["pseFoster"] == null

                val processed = ProcessedMetrics(
                    cargaTotal = totalCarga,
                    pseMedio = mediaPse,
                    duracaoMedia = mediaDuracao,
                    distribuicaoModalidades = modalidades,
                    evolucaoCarga = evolucao,
                    sessoesBrutas = sessoes,
                    ultimoCheckIn = sessoes.firstOrNull { it["bemEstar"] != null },
                    ultimoCheckOut = sessoes.firstOrNull { it["pseFoster"] != null },
                    hasOpenCheckIn = hasOpen
                )

                _uiState.update { it.copy(metricsState = ResultState.Success(processed)) }
            } catch (e: Exception) {
                // Se der erro mas já tivermos dados, não substituímos por erro, apenas ignoramos a atualização silenciosa
                if (_uiState.value.metricsState !is ResultState.Success) {
                    _uiState.update { it.copy(metricsState = ResultState.Error(e.message ?: "Erro desconhecido")) }
                }
            }
        }
    }
}
