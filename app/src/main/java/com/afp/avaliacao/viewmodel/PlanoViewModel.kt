package com.afp.avaliacao.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afp.avaliacao.data.DiaTreino
import com.afp.avaliacao.data.PlanoRepository
import com.afp.avaliacao.data.PlanoTreino
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

data class PlanoUiState(
    val selectedAtletaId: String = "",
    val objetivo: String = "",
    val nivel: String = "Intermediário",
    val frequencia: Int = 3,
    val observacoes: String = "",
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val generatedPlano: PlanoTreino? = null,
    val currentPlanoAtleta: PlanoTreino? = null,
    val isLoadingPlano: Boolean = false,
    val error: String? = null
)

class PlanoViewModel(
    private val repository: PlanoRepository = PlanoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanoUiState())
    val uiState: StateFlow<PlanoUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    fun onAtletaSelected(id: String) {
        _uiState.update { it.copy(selectedAtletaId = id) }
        carregarPlanoAtleta(id)
    }

    fun onObjetivoChange(value: String) { _uiState.update { it.copy(objetivo = value) } }
    fun onNivelChange(value: String) { _uiState.update { it.copy(nivel = value) } }
    fun onFrequenciaChange(value: Int) { _uiState.update { it.copy(frequencia = value) } }
    fun onObservacoesChange(value: String) { _uiState.update { it.copy(observacoes = value) } }

    fun carregarPlanoAtleta(atletaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlano = true) }
            val plano = repository.getUltimoPlanoAtleta(atletaId)
            _uiState.update { it.copy(isLoadingPlano = false, currentPlanoAtleta = plano) }
        }
    }

    fun gerarPlanoIA() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            try {
                // MOCK AI GENERATION (Gemini integration would go here)
                val mockPeriodo = mapOf(
                    "semana1" to generateMockWeek("1"),
                    "semana2" to generateMockWeek("2"),
                    "semana3" to generateMockWeek("3"),
                    "semana4" to generateMockWeek("4")
                )
                
                val novoPlano = PlanoTreino(
                    coachId = auth.currentUser?.uid ?: "",
                    atletaId = _uiState.value.selectedAtletaId,
                    periodo = mockPeriodo
                )
                
                _uiState.update { it.copy(isGenerating = false, generatedPlano = novoPlano) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = "Erro ao gerar plano: ${e.message}") }
            }
        }
    }

    fun salvarPlano() {
        val plano = _uiState.value.generatedPlano ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.salvarPlano(plano).onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true, generatedPlano = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    private fun generateMockWeek(prefix: String): List<DiaTreino> {
        return listOf(
            DiaTreino("Seg", "Musculação", 1200, 60, "Treino A - Foco em Hipertrofia"),
            DiaTreino("Qua", "Corrida", 800, 45, "Intervalado 5x400m"),
            DiaTreino("Sex", "Musculação", 1200, 60, "Treino B - Membros Inferiores")
        )
    }
}
