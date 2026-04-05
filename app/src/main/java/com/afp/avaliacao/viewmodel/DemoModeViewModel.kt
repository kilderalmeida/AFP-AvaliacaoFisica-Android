package com.afp.avaliacao.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DemoModeViewModel : ViewModel() {
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    fun toggleDemoMode() {
        _isDemoMode.update { !it }
    }

    fun getDemoAtletas() = listOf(
        com.afp.avaliacao.data.Atleta("demo1", "Atleta Demo 1"),
        com.afp.avaliacao.data.Atleta("demo2", "Atleta Demo 2")
    )

    fun getDemoMetrics() = ProcessedMetrics(
        cargaTotal = 8500,
        pseMedio = 6.5,
        duracaoMedia = 55,
        distribuicaoModalidades = mapOf("Musculação" to 12, "Corrida" to 5, "Natação" to 3),
        evolucaoCarga = listOf("01/10" to 1200, "02/10" to 1100, "03/10" to 1500, "04/10" to 1300),
        sessoesBrutas = emptyList()
    )
}
