package com.afp.avaliacao.ui.screens.plano

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afp.avaliacao.viewmodel.MetricsViewModel
import com.afp.avaliacao.viewmodel.PlanoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachPlanoScreen(
    onBack: () -> Unit,
    planoViewModel: PlanoViewModel = viewModel(),
    metricsViewModel: MetricsViewModel = viewModel()
) {
    val uiState by planoViewModel.uiState.collectAsState()
    val metricsState by metricsViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerar Plano IA") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Configurações do Plano", style = MaterialTheme.typography.titleLarge)
            }

            item {
                AtletaDropdown(metricsState.atletas, uiState.selectedAtletaId) { planoViewModel.onAtletaSelected(it) }
            }

            item {
                OutlinedTextField(
                    value = uiState.objetivo,
                    onValueChange = { planoViewModel.onObjetivoChange(it) },
                    label = { Text("Objetivo (Ex: Hipertrofia, Maratona)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.nivel,
                        onValueChange = { planoViewModel.onNivelChange(it) },
                        label = { Text("Nível") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.frequencia.toString(),
                        onValueChange = { it.toIntOrNull()?.let { f -> planoViewModel.onFrequenciaChange(f) } },
                        label = { Text("Freq. Semanal") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.observacoes,
                    onValueChange = { planoViewModel.onObservacoesChange(it) },
                    label = { Text("Observações/Limitações") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            item {
                Button(
                    onClick = { planoViewModel.gerarPlanoIA() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isGenerating && uiState.selectedAtletaId.isNotEmpty()
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Gerar Plano com IA")
                    }
                }
            }

            uiState.generatedPlano?.let { plano ->
                item {
                    HorizontalDivider()
                    Text("Plano Gerado (4 Semanas)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                plano.periodo.forEach { (semana, dias) ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(semana.uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                dias.forEach { dia ->
                                    Text("• ${dia.dia}: ${dia.modalidade} (${dia.cargaAlvo} carga, ${dia.duracao}min)", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { planoViewModel.salvarPlano() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        enabled = !uiState.isSaving
                    ) {
                        Text("Confirmar e Enviar ao Atleta")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtletaDropdown(atletas: List<com.afp.avaliacao.data.Atleta>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentNome = atletas.find { it.id == selectedId }?.nome ?: "Selecionar Atleta"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = currentNome,
            onValueChange = {},
            readOnly = true,
            label = { Text("Atleta") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            atletas.forEach { atleta ->
                DropdownMenuItem(
                    text = { Text(atleta.nome) },
                    onClick = {
                        onSelect(atleta.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
