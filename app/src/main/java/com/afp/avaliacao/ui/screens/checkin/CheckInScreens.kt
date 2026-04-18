package com.afp.avaliacao.ui.screens.checkin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afp.avaliacao.ui.components.common.AppScaffold
import com.afp.avaliacao.ui.components.common.LoadingView
import com.afp.avaliacao.util.ResultState
import com.afp.avaliacao.viewmodel.CheckInViewModel

@Composable
fun CheckInFlow(
    onFinish: () -> Unit,
    onBack: () -> Unit,
    viewModel: CheckInViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.saveState) {
        when (val state = uiState.saveState) {
            is ResultState.Success -> {
                Toast.makeText(context, "Check-in realizado com sucesso!", Toast.LENGTH_SHORT).show()
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
        title = "Check-in Passo ${uiState.currentStep}/6",
        onBack = { if (uiState.currentStep > 1) viewModel.previousStep() else onBack() },
        bottomBar = {
            if (uiState.saveState !is ResultState.Loading) {
                Button(
                    onClick = { viewModel.nextStep() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(if (uiState.currentStep == 6) "Finalizar" else "Próximo")
                }
            }
        }
    ) { padding ->
        if (uiState.saveState is ResultState.Loading) {
            LoadingView("Salvando seu check-in...")
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                LinearProgressIndicator(
                    progress = { uiState.currentStep / 6f },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                when (uiState.currentStep) {
                    1 -> StepAtividades(uiState.atividades) { viewModel.onAtividadesChange(it) }
                    2 -> StepVFC(uiState.vfc) { viewModel.onVfcChange(it) }
                    3 -> StepBemEstar(uiState.bemEstar) { item, v -> viewModel.onBemEstarChange(item, v) }
                    4 -> StepRecuperacao(uiState.recuperacao) { viewModel.onRecuperacaoChange(it) }
                    5 -> StepDorRegioes(uiState.dorRegioes, onToggle = { viewModel.toggleDorRegiao(it) }, onClear = { viewModel.onDorRegioesChange(emptyList()) })
                    6 -> StepHidratacao(uiState.hidratacao) { viewModel.onHidratacaoChange(it) }
                }
            }
        }
    }
}

@Composable
fun StepAtividades(selecionadas: List<String>, onUpdate: (List<String>) -> Unit) {
    val opcoes = listOf("Musculação", "Funcional", "Bike", "Corrida", "Tênis", "Futebol", "Futvolei", "Natação piscina", "Natação mar")
    Text("Quais atividades você realizou hoje?", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(opcoes) { opcao ->
            FilterChip(
                selected = selecionadas.contains(opcao),
                onClick = {
                    val novaLista = selecionadas.toMutableList()
                    if (novaLista.contains(opcao)) novaLista.remove(opcao) else novaLista.add(opcao)
                    onUpdate(novaLista)
                },
                label = { Text(opcao) }
            )
        }
    }
}

@Composable
fun StepVFC(vfc: String, onUpdate: (String) -> Unit) {
    Text("Variabilidade da Frequência Cardíaca (VFC)", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = vfc,
        onValueChange = { if (it.all { char -> char.isDigit() }) onUpdate(it) },
        label = { Text("Valor em ms") },
        placeholder = { Text("Ex: 65") },
        suffix = { Text("ms") },
        supportingText = { Text("Variabilidade média (miligramas)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun StepBemEstar(bemEstar: Map<String, Int>, onUpdate: (String, Int) -> Unit) {
    val itens = listOf("Fadiga", "Sono", "Dor", "Estresse", "Humor")
    Text("Como você se sente hoje?", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        itens.forEach { item ->
            Column {
                Text(item, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (1..5).forEach { valor ->
                        IconButton(onClick = { onUpdate(item, valor) }) {
                            Icon(
                                imageVector = if ((bemEstar[item] ?: 0) >= valor) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if ((bemEstar[item] ?: 0) >= valor) Color(0xFFFFC107) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepRecuperacao(selecionada: String, onUpdate: (String) -> Unit) {
    val opcoes = listOf("Excelente", "Muito boa", "Boa", "Regular", "Ruim")
    Text("Qual seu nível de recuperação?", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    Column {
        opcoes.forEach { opcao ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().selectable(selected = selecionada == opcao, onClick = { onUpdate(opcao) })) {
                RadioButton(selected = selecionada == opcao, onClick = { onUpdate(opcao) })
                Text(opcao, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun StepDorRegioes(
    selecionadas: List<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mapa de Dor Muscular", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onClear) {
                Text("Limpar", color = MaterialTheme.colorScheme.error)
            }
        }
        
        BodyPainMap(
            selectedRegions = selecionadas,
            onToggle = onToggle,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StepHidratacao(hidratacao: Int, onUpdate: (Int) -> Unit) {
    Text("Nível de Hidratação (Cor da Urina)", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    val cores = listOf(
        Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFA5D6A7), 
        Color(0xFFFFEE58), Color(0xFFFFD54F), Color(0xFFFFA726),
        Color(0xFFFB8C00), Color(0xFFD84315)
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            cores.forEachIndexed { index, cor ->
                val level = index + 1
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cor)
                        .clickable { onUpdate(level) }
                        .padding(2.dp)
                ) {
                    if (hidratacao == level) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (hidratacao > 5) Color.Red.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.05f)
            )
        ) {
            Text(
                text = when(hidratacao) {
                    in 1..3 -> "Ótima Hidratação"
                    in 4..5 -> "Beba um pouco mais de água"
                    else -> "ALERTA: Desidratação!"
                },
                modifier = Modifier.padding(16.dp),
                color = if (hidratacao > 5) Color.Red else Color.DarkGray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
