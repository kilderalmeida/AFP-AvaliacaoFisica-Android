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

data class CheckInUiState(
    val currentStep: Int = 1,
    val atividades: List<String> = emptyList(),
    val vfc: String = "",
    val bemEstar: Map<String, Int> = mapOf(
        "Fadiga" to 3, "Sono" to 3, "Dor" to 3, "Estresse" to 3, "Humor" to 3
    ),
    val recuperacao: String = "Boa",
    val dorRegioes: List<String> = emptyList(),
    val hidratacao: Int = 4,
    val saveState: ResultState<Unit> = ResultState.Idle
)

class CheckInViewModel(
    private val repository: SessionRepository = SessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    fun nextStep() {
        if (_uiState.value.currentStep < 6) {
            _uiState.update { it.copy(currentStep = it.currentStep + 1) }
        } else {
            salvar()
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun onAtividadesChange(atividades: List<String>) {
        _uiState.update { it.copy(atividades = atividades) }
    }

    fun onVfcChange(vfc: String) {
        _uiState.update { it.copy(vfc = vfc) }
    }

    fun onBemEstarChange(item: String, value: Int) {
        val newMap = _uiState.value.bemEstar.toMutableMap()
        newMap[item] = value
        _uiState.update { it.copy(bemEstar = newMap) }
    }

    fun onRecuperacaoChange(recuperacao: String) {
        _uiState.update { it.copy(recuperacao = recuperacao) }
    }

    fun toggleDorRegiao(regiao: String) {
        val current = _uiState.value.dorRegioes.toMutableList()
        if (current.contains(regiao)) current.remove(regiao) else current.add(regiao)
        _uiState.update { it.copy(dorRegioes = current) }
    }

    fun onHidratacaoChange(hidratacao: Int) {
        _uiState.update { it.copy(hidratacao = hidratacao) }
    }

    private fun salvar() {
        _uiState.update { it.copy(saveState = ResultState.Loading) }
        viewModelScope.launch {
            val data = SessionData(
                atividades = _uiState.value.atividades,
                vfc = _uiState.value.vfc.toIntOrNull() ?: 0,
                bemEstar = _uiState.value.bemEstar,
                recuperacao = _uiState.value.recuperacao,
                dorRegioes = _uiState.value.dorRegioes,
                hidratacao = _uiState.value.hidratacao
            )
            repository.salvarCheckIn(data).onSuccess {
                _uiState.update { it.copy(saveState = ResultState.Success(Unit)) }
            }.onFailure { e ->
                _uiState.update { it.copy(saveState = ResultState.Error(e.message ?: "Erro desconhecido")) }
            }
        }
    }

    fun resetSaveState() {
        _uiState.update { it.copy(saveState = ResultState.Idle) }
    }
}
