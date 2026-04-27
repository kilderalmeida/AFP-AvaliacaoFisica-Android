package com.afp.avaliacao.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afp.avaliacao.data.Atleta
import com.afp.avaliacao.data.MetricsRepository
import com.afp.avaliacao.data.Treinador
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

data class MetricsUiState(
    val userName: String = "",
    val atletas: List<Atleta> = emptyList(),
    val treinadores: List<Treinador> = emptyList(),
    val atletaSelecionadoId: String = "",
    val treinadorSelecionadoId: String = "todos", // "todos", "meus", ou ID específico
    val diasSelecionados: Int = 7,
    val metricsState: ResultState<ProcessedMetrics> = ResultState.Idle,
    val userRole: String = "treinador" // "coach", "treinador" ou "admin"
)

data class ProcessedMetrics(
    val cargaTotal: Int,
    val pseMedio: Double,
    val duracaoMedia: Int,
    val distribuicaoModalidades: Map<String, Int>,
    val evolucaoCarga: List<Pair<String, Int>>,
    val sessoesBrutas: List<Map<String, Any>>,
    val ultimoCheckIn: Map<String, Any>? = null,
    val ultimoCheckOut: Map<String, Any>? = null,
    val hasOpenCheckIn: Boolean = false
)

class MetricsViewModel(
    private val repository: MetricsRepository = MetricsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsUiState())
    val uiState: StateFlow<MetricsUiState> = _uiState.asStateFlow()

    private val _saveAtletaState = MutableStateFlow<ResultState<Unit>>(ResultState.Idle)
    val saveAtletaState: StateFlow<ResultState<Unit>> = _saveAtletaState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        detectUserRoleAndLoadData()
    }

    private fun detectUserRoleAndLoadData() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val doc = firestore.collection("users").document(uid).get().await()
                    val role = doc.getString("papel") ?: "treinador"
                    val name = doc.getString("nome") ?: "Usuário"
                    
                    _uiState.update { it.copy(userRole = role, userName = name) }
                    
                    if (role != "coach" && role != "admin") {
                        _uiState.update { it.copy(treinadorSelecionadoId = "meus") }
                    }
                }
                
                val treinadores = repository.getTreinadores()
                _uiState.update { it.copy(treinadores = treinadores) }
                carregarAtletas()
            } catch (e: Exception) {
                carregarAtletas()
            }
        }
    }

    fun carregarAtletas() {
        viewModelScope.launch {
            try {
                val filterId = _uiState.value.treinadorSelecionadoId
                val role = _uiState.value.userRole
                val treinadorNome = _uiState.value.treinadores.find { it.id == filterId }?.nome ?: "N/A"
                
                Log.d("MetricsDebug", "--- Carregando Atletas ---")
                Log.d("MetricsDebug", "Treinador Selecionado ID: $filterId")
                Log.d("MetricsDebug", "Treinador Selecionado Nome: $treinadorNome")
                Log.d("MetricsDebug", "User Role: $role")

                val listaFiltrada = if (role != "coach" && role != "admin") {
                    val meus = repository.getMeusAtletas()
                    Log.d("MetricsDebug", "Busca direta (meus atletas). Retornados: ${meus.size}")
                    meus
                } else {
                    val listaCompleta = repository.getTodosAtletasDoSistema()
                    Log.d("MetricsDebug", "Lista completa do sistema carregada: ${listaCompleta.size} atletas")
                    
                    when (filterId) {
                        "todos" -> {
                            Log.d("MetricsDebug", "Filtro 'todos' aplicado")
                            listaCompleta
                        }
                        "meus" -> {
                            val meus = repository.getMeusAtletas()
                            Log.d("MetricsDebug", "Filtro 'meus' aplicado. Retornados: ${meus.size}")
                            meus
                        }
                        else -> {
                            // Filtro por ID de treinador específico
                            val filtrada = listaCompleta.filter { it.treinadorId == filterId }
                            Log.d("MetricsDebug", "Filtro por treinador específico. Valor usado na query (filterId): $filterId")
                            Log.d("MetricsDebug", "Quantidade de atletas retornados após filtro: ${filtrada.size}")
                            
                            // Debug de cada atleta para ver o que tem no campo treinadorId
                            listaCompleta.forEach { atleta ->
                                if (atleta.nome.contains("Kilder", ignoreCase = true)) {
                                    Log.d("MetricsDebug", "Debug Atleta Kilder -> Nome: ${atleta.nome}, treinadorId: ${atleta.treinadorId}")
                                }
                            }
                            filtrada
                        }
                    }
                }

                _uiState.update { it.copy(atletas = listaFiltrada) }
                
                if (listaFiltrada.none { it.id == _uiState.value.atletaSelecionadoId }) {
                    if (listaFiltrada.isNotEmpty()) {
                        onAtletaSelected(listaFiltrada.first().id)
                    } else {
                        _uiState.update { it.copy(atletaSelecionadoId = "", metricsState = ResultState.Idle) }
                    }
                }
            } catch (e: Exception) {
                Log.e("MetricsDebug", "Erro ao carregar atletas", e)
                _uiState.update { it.copy(metricsState = ResultState.Error("Erro ao carregar atletas: ${e.message}")) }
            }
        }
    }

    fun onTreinadorFilterChanged(treinadorId: String) {
        _uiState.update { it.copy(treinadorSelecionadoId = treinadorId) }
        carregarAtletas()
    }

    fun onAtletaSelected(id: String) {
        _uiState.update { it.copy(atletaSelecionadoId = id) }
        carregarMetricas()
    }

    fun onDiasSelected(dias: Int) {
        _uiState.update { it.copy(diasSelecionados = dias) }
        carregarMetricas()
    }

    fun cadastrarAtleta(nome: String, email: String) {
        _saveAtletaState.value = ResultState.Loading
        viewModelScope.launch {
            repository.cadastrarAtleta(nome, email)
                .onSuccess {
                    _saveAtletaState.value = ResultState.Success(Unit)
                    carregarAtletas()
                }
                .onFailure { e ->
                    _saveAtletaState.value = ResultState.Error(e.message ?: "Erro ao cadastrar")
                }
        }
    }

    fun cadastrarTreinador(nome: String, email: String) {
        _saveAtletaState.value = ResultState.Loading
        viewModelScope.launch {
            repository.cadastrarTreinador(nome, email)
                .onSuccess {
                    _saveAtletaState.value = ResultState.Success(Unit)
                    detectUserRoleAndLoadData() // Recarrega lista de treinadores
                }
                .onFailure { e ->
                    _saveAtletaState.value = ResultState.Error(e.message ?: "Erro ao cadastrar treinador")
                }
        }
    }

    fun toggleAtletaAtivo(atletaId: String, novoStatus: Boolean) {
        _saveAtletaState.value = ResultState.Loading
        viewModelScope.launch {
            repository.toggleAtletaAtivo(atletaId, novoStatus)
                .onSuccess {
                    _saveAtletaState.value = ResultState.Success(Unit)
                    carregarAtletas()
                }
                .onFailure { e ->
                    _saveAtletaState.value = ResultState.Error(e.message ?: "Erro ao atualizar status")
                }
        }
    }

    fun resetSaveState() {
        _saveAtletaState.value = ResultState.Idle
    }

    private fun carregarMetricas() {
        val atletaId = _uiState.value.atletaSelecionadoId
        val dias = _uiState.value.diasSelecionados
        if (atletaId.isEmpty()) return

        _uiState.update { it.copy(metricsState = ResultState.Loading) }

        viewModelScope.launch {
            try {
                val sessoes = repository.getSessoesTreino(atletaId, dias)
                
                if (sessoes.isEmpty()) {
                    _uiState.update { it.copy(metricsState = ResultState.Error("Nenhum dado encontrado.")) }
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
                        modalidades[it.toString()] = modalidades.getOrDefault(it.toString(), 0) + 1
                    }
                    val data = (sessao["dataCheckin"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                    evolucao.add(sdf.format(data) to ((sessao["carga"] as? Long)?.toInt() ?: 0))
                }

                val sessaoMaisRecente = sessoes.firstOrNull()
                val hasOpen = sessaoMaisRecente != null && sessaoMaisRecente.containsKey("bemEstar") && !sessaoMaisRecente.containsKey("pseFoster")

                val processed = ProcessedMetrics(
                    cargaTotal = totalCarga,
                    pseMedio = mediaPse,
                    duracaoMedia = mediaDuracao,
                    distribuicaoModalidades = modalidades,
                    evolucaoCarga = evolucao,
                    sessoesBrutas = sessoes,
                    ultimoCheckIn = sessoes.firstOrNull { it.containsKey("bemEstar") },
                    ultimoCheckOut = sessoes.firstOrNull { it.containsKey("pseFoster") },
                    hasOpenCheckIn = hasOpen
                )

                _uiState.update { it.copy(metricsState = ResultState.Success(processed)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(metricsState = ResultState.Error(e.message ?: "Erro")) }
            }
        }
    }
}
