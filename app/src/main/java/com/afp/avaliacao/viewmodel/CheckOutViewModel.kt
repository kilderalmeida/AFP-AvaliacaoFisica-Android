package com.afp.avaliacao.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afp.avaliacao.data.SessionData
import com.afp.avaliacao.data.SessionRepository
import com.afp.avaliacao.util.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckOutUiState(
    val currentStep: Int = 1,
    val pseFoster: Int = 5,
    val duracaoMin: String = "50",
    val sessionToUpdate: SessionData? = null,
    val loadState: ResultState<SessionData?> = ResultState.Idle,
    val saveState: ResultState<Unit> = ResultState.Idle
)

class CheckOutViewModel(
    private val repository: SessionRepository = SessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckOutUiState())
    val uiState: StateFlow<CheckOutUiState> = _uiState.asStateFlow()

    init {
        buscarUltimoCheckIn()
    }

    fun buscarUltimoCheckIn() {
        _uiState.update { it.copy(loadState = ResultState.Loading) }
        viewModelScope.launch {
            try {
                // Agora busca qualquer sessão que não tenha sido finalizada
                val session = repository.getSessaoAberta()
                if (session == null) {
                    _uiState.update { it.copy(loadState = ResultState.Error("Nenhum check-in em aberto encontrado.")) }
                } else {
                    _uiState.update { it.copy(loadState = ResultState.Success(session), sessionToUpdate = session) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadState = ResultState.Error("Erro ao carregar treino: ${e.message}")) }
            }
        }
    }

    fun nextStep() {
        if (_uiState.value.currentStep < 2) {
            _uiState.update { it.copy(currentStep = it.currentStep + 1) }
        } else {
            finalizarCheckOut()
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun onPseChange(value: Int) {
        _uiState.update { it.copy(pseFoster = value) }
    }

    fun onDuracaoChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(duracaoMin = value) }
        }
    }

    private fun finalizarCheckOut() {
        val duracao = _uiState.value.duracaoMin.toIntOrNull() ?: 50
        val sessionId = _uiState.value.sessionToUpdate?.id ?: return

        if (duracao < 1 || duracao > 180) {
            _uiState.update { it.copy(saveState = ResultState.Error("Duração deve ser entre 1 e 180 min")) }
            return
        }

        _uiState.update { it.copy(saveState = ResultState.Loading) }
        viewModelScope.launch {
            repository.salvarCheckOut(sessionId, _uiState.value.pseFoster, duracao)
                .onSuccess {
                    _uiState.update { it.copy(saveState = ResultState.Success(Unit)) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(saveState = ResultState.Error(e.message ?: "Erro ao salvar checkout")) }
                }
        }
    }

    fun resetSaveState() {
        _uiState.update { it.copy(saveState = ResultState.Idle) }
    }
}
