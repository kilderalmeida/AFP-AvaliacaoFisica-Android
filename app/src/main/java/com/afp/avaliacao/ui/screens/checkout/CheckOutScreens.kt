package com.afp.avaliacao.ui.screens.checkout

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afp.avaliacao.ui.components.common.AppScaffold
import com.afp.avaliacao.ui.components.common.ErrorStateView
import com.afp.avaliacao.ui.components.common.LoadingView
import com.afp.avaliacao.util.ResultState
import com.afp.avaliacao.viewmodel.CheckOutViewModel

@Composable
fun CheckOutFlow(
    onFinish: () -> Unit,
    onBack: () -> Unit,
    viewModel: CheckOutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.saveState) {
        when (val state = uiState.saveState) {
            is ResultState.Success -> {
                Toast.makeText(context, "Check-out realizado com sucesso!", Toast.LENGTH_SHORT).show()
                onFinish()
            }
            is ResultState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    AppScaffold(
        title = "Check-out Passo ${uiState.currentStep}/2",
        onBack = onBack,
        bottomBar = {
            if (uiState.loadState is ResultState.Success && uiState.saveState !is ResultState.Loading) {
                Button(
                    onClick = { viewModel.nextStep() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(if (uiState.currentStep == 2) "Finalizar Check-out" else "Próximo")
                }
            }
        }
    ) { padding ->
        when (val loadState = uiState.loadState) {
            is ResultState.Loading -> LoadingView("Buscando seu check-in...")
            is ResultState.Error -> ErrorStateView(loadState.message, onRetry = { viewModel.buscarUltimoCheckIn() })
            is ResultState.Success -> {
                if (uiState.saveState is ResultState.Loading) {
                    LoadingView("Salvando check-out...")
                } else {
                    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                        // Resumo do Check-in
                        uiState.sessionToUpdate?.let { session ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = padding.let { Modifier.padding(12.dp) }) {
                                    Text("Resumo Pré-Treino (Check-in)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(session.bemEstar.entries.joinToString(" | ") { "${it.key}: ${it.value}" }, fontSize = 12.sp)
                                    Text("Recuperação: ${session.recuperacao}", fontSize = 12.sp)
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { uiState.currentStep / 2f },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                        )

                        when (uiState.currentStep) {
                            1 -> StepPSE(uiState.pseFoster) { viewModel.onPseChange(it) }
                            2 -> StepDuracao(uiState.duracaoMin) { viewModel.onDuracaoChange(it) }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepPSE(pseSelected: Int, onUpdate: (Int) -> Unit) {
    val options = listOf(
        10 to "Exausto / Esforço Máximo",
        9 to "Muito, Muito difícil",
        8 to "Muito difícil",
        7 to "Muito Cansado",
        6 to "Cansado / Pesado",
        5 to "Pesado",
        4 to "Um pouco pesado",
        3 to "Moderado",
        2 to "Leve",
        1 to "Muito Leve",
        0 to "Repouso / Muito Fácil"
    )
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.find { it.first == pseSelected }?.second ?: "Selecione"

    Text("Como foi o seu esforço no treino? (PSE Foster)", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(8.dp))
    Text("0 é muito fácil e 10 é o seu limite máximo.", fontSize = 12.sp, color = Color.Gray)
    Spacer(modifier = Modifier.height(16.dp))

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = "$pseSelected - $currentLabel",
            onValueChange = {},
            readOnly = true,
            label = { Text("Escala de Esforço (Foster)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (valor, desc) ->
                DropdownMenuItem(
                    text = { Text("$valor - $desc") }, 
                    onClick = { onUpdate(valor); expanded = false }
                )
            }
        }
    }
}

@Composable
fun StepDuracao(duracao: String, onUpdate: (String) -> Unit) {
    Text("Duração da sessão", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = duracao,
        onValueChange = onUpdate,
        label = { Text("Tempo em minutos") },
        placeholder = { Text("Ex: 50") },
        suffix = { Text("min") },
        supportingText = { Text("Intervalo sugerido: 1 a 180 min") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
