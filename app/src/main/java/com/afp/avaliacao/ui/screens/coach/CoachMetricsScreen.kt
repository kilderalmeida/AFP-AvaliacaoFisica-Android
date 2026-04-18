package com.afp.avaliacao.ui.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afp.avaliacao.ui.components.common.*
import com.afp.avaliacao.util.PdfGenerator
import com.afp.avaliacao.util.ResultState
import com.afp.avaliacao.viewmodel.MetricsViewModel
import com.afp.avaliacao.viewmodel.ProcessedMetrics
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CoachMetricsScreen(
    onLogout: () -> Unit,
    onNavigateToCadastroAtleta: () -> Unit,
    onNavigateToGerenciarAtletas: () -> Unit,
    onNavigateToCadastroTreinador: () -> Unit,
    viewModel: MetricsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pdfGenerator = remember { PdfGenerator(context) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair do App") },
            text = { Text("Tem certeza que deseja sair?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Sair", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    AppScaffold(
        title = if (uiState.userName.isNotEmpty()) "Olá, ${uiState.userName}" else "Métricas de Treino",
        onBack = null,
        actions = {
            if (uiState.userRole == "coach" || uiState.userRole == "admin") {
                IconButton(onClick = onNavigateToCadastroTreinador) {
                    Icon(Icons.Default.Badge, contentDescription = "Cadastrar Treinador")
                }
            }
            IconButton(onClick = onNavigateToGerenciarAtletas) {
                Icon(Icons.Default.Group, contentDescription = "Gerenciar Atletas")
            }
            IconButton(onClick = onNavigateToCadastroAtleta) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Cadastrar Atleta")
            }
            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(Icons.Default.Logout, contentDescription = "Sair")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            FiltersSection(uiState, viewModel)
            
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState.metricsState) {
                is ResultState.Loading -> LoadingView("Analisando dados do atleta...")
                is ResultState.Error -> ErrorStateView(state.message, onRetry = { viewModel.carregarAtletas() })
                is ResultState.Success -> {
                    MetricsContent(state.data, uiState.diasSelecionados)
                }
                else -> {
                    val ativosCount = uiState.atletas.count { it.ativo }
                    if (ativosCount == 0 && uiState.atletaSelecionadoId.isEmpty()) {
                        EmptyStateView("Nenhum atleta encontrado para os filtros selecionados.")
                    } else {
                        EmptyStateView("Selecione um atleta para ver as métricas.")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSection(uiState: com.afp.avaliacao.viewmodel.MetricsUiState, viewModel: MetricsViewModel) {
    var coachExpanded by remember { mutableStateOf(false) }
    var athleteExpanded by remember { mutableStateOf(false) }
    var rangeExpanded by remember { mutableStateOf(false) }
    
    val atletasAtivos = uiState.atletas.filter { it.ativo }
    val isCoachOrAdmin = uiState.userRole == "coach" || uiState.userRole == "admin"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isCoachOrAdmin) {
            ExposedDropdownMenuBox(
                expanded = coachExpanded,
                onExpandedChange = { coachExpanded = !coachExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val label = when(uiState.treinadorSelecionadoId) {
                    "todos" -> "Todos os Treinadores"
                    "meus" -> "Meus Atletas Diretos"
                    else -> uiState.treinadores.find { it.id == uiState.treinadorSelecionadoId }?.nome ?: "Selecionar Origem"
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrar por Treinador") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = coachExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = coachExpanded, onDismissRequest = { coachExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Todos os Atletas") },
                        onClick = { viewModel.onTreinadorFilterChanged("todos"); coachExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Meus Atletas Diretos") },
                        onClick = { viewModel.onTreinadorFilterChanged("meus"); coachExpanded = false }
                    )
                    uiState.treinadores.forEach { treinador ->
                        DropdownMenuItem(
                            text = { Text(treinador.nome) },
                            onClick = { viewModel.onTreinadorFilterChanged(treinador.id); coachExpanded = false }
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = athleteExpanded,
                onExpandedChange = { athleteExpanded = !athleteExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = uiState.atletas.find { it.id == uiState.atletaSelecionadoId }?.nome ?: "Selecionar Atleta",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Atleta") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = athleteExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = athleteExpanded, onDismissRequest = { athleteExpanded = false }) {
                    atletasAtivos.forEach { atleta ->
                        DropdownMenuItem(
                            text = { Text(atleta.nome) },
                            onClick = { viewModel.onAtletaSelected(atleta.id); athleteExpanded = false }
                        )
                    }
                    if (atletasAtivos.isEmpty()) {
                        DropdownMenuItem(text = { Text("Nenhum atleta ativo") }, onClick = {})
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = rangeExpanded,
                onExpandedChange = { rangeExpanded = !rangeExpanded },
                modifier = Modifier.weight(0.7f)
            ) {
                OutlinedTextField(
                    value = "${uiState.diasSelecionados} dias",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Período") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rangeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = rangeExpanded, onDismissRequest = { rangeExpanded = false }) {
                    listOf(7, 30, 90).forEach { dias ->
                        DropdownMenuItem(
                            text = { Text("$dias dias") },
                            onClick = { viewModel.onDiasSelected(dias); rangeExpanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricsContent(metrics: ProcessedMetrics, dias: Int) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ProntidaoCard(metrics.ultimoCheckIn)
        }

        item {
            LastCheckOutCard(metrics.ultimoCheckOut)
        }

        item {
            Text("Resumo do Período", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Carga Total", "${metrics.cargaTotal}", Modifier.weight(1f), Color(0xFFBBDEFB))
                    MetricCard("PSE Médio", "%.1f".format(metrics.pseMedio), Modifier.weight(1f), Color(0xFFC8E6C9))
                }
                MetricCard("Duração Média Sessão", "${metrics.duracaoMedia} min", Modifier.fillMaxWidth(), Color(0xFFFFF9C4))
            }
        }

        item {
            Text("Distribuição de Modalidades", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            ModalidadesChart(metrics.distribuicaoModalidades)
        }

        item {
            Text("Evolução da Carga", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            EvolucaoCargaChart(metrics.evolucaoCarga)
        }

        item {
            Text("Histórico de Sessões", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        items(metrics.sessoesBrutas) { sessao ->
            CoachSessionItem(sessao, sdf)
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun CoachSessionItem(sessao: Map<String, Any>, sdf: SimpleDateFormat) {
    val data = (sessao["dataCheckin"] as? com.google.firebase.Timestamp)?.toDate()
    val atividades = (sessao["atividades"] as? List<*>)?.joinToString(", ") ?: "Sem atividades"
    val carga = (sessao["carga"] as? Long) ?: 0L
    val pse = (sessao["pseFoster"] as? Long) ?: 0L
    val finalizado = sessao["pseFoster"] != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data?.let { sdf.format(it) } ?: "Data desconhecida",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                if (finalizado) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Carga: $carga",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text("Em andamento", color = Color(0xFFFFA000), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = atividades,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            if (finalizado) {
                Text(
                    text = "PSE: $pse | Duração: ${sessao["duracaoMin"]} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ProntidaoCard(checkIn: Map<String, Any>?) {
    if (checkIn == null) return

    val bemEstar = checkIn["bemEstar"] as? Map<String, Long> ?: emptyMap()
    val data = (checkIn["dataCheckin"] as? com.google.firebase.Timestamp)?.toDate()
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val recuperacao = checkIn["recuperacao"]?.toString() ?: "N/A"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Último Check-in (${data?.let { sdf.format(it) }})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                
                Surface(
                    color = when(recuperacao.lowercase()) {
                        "ótima", "boa" -> Color(0xFF4CAF50)
                        "regular" -> Color(0xFFFFA000)
                        else -> Color(0xFFF44336)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = recuperacao,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BemEstarIndicator("Sono", bemEstar["Sono"]?.toInt() ?: 0)
                BemEstarIndicator("Estresse", bemEstar["Estresse"]?.toInt() ?: 0)
                BemEstarIndicator("Fadiga", bemEstar["Fadiga"]?.toInt() ?: 0)
                BemEstarIndicator("Humor", bemEstar["Humor"]?.toInt() ?: 0)
            }

            val dores = checkIn["dorRegioes"] as? List<*>
            if (!dores.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dores: ${dores.joinToString(", ")}", color = Color(0xFFD32F2F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LastCheckOutCard(checkOut: Map<String, Any>?) {
    if (checkOut == null) return

    val data = (checkOut["dataCheckin"] as? com.google.firebase.Timestamp)?.toDate()
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val pse = (checkOut["pseFoster"] as? Long) ?: 0
    val duracao = (checkOut["duracaoMin"] as? Long) ?: 0
    val carga = (checkOut["carga"] as? Long) ?: 0
    val atividades = (checkOut["atividades"] as? List<*>)?.joinToString(", ") ?: "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Último Treino (${data?.let { sdf.format(it) }})", 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("PSE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("$pse", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column {
                    Text("Duração", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("$duracao min", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column {
                    Text("Carga", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("$carga", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Modalidades: $atividades", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun BemEstarIndicator(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(
            text = "$value/5",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = when {
                value <= 2 -> Color(0xFFF44336)
                value == 3 -> Color(0xFFFFA000)
                else -> Color(0xFF4CAF50)
            }
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, color = Color.DarkGray)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ModalidadesChart(dados: Map<String, Int>) {
    Card(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        Row(Modifier.padding(16.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(80.dp).background(Color.LightGray, CircleShape))
            Spacer(Modifier.width(16.dp))
            Column {
                dados.forEach { (mod, count) ->
                    Text("$mod: $count", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EvolucaoCargaChart(evolucao: List<Pair<String, Int>>) {
    val maxCarga = evolucao.maxOfOrNull { it.second }?.toFloat() ?: 1f
    
    Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        if (evolucao.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sem dados para o período", color = Color.Gray)
            }
        } else {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    evolucao.takeLast(10).forEach { (data, carga) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("$carga", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((120 * (carga / maxCarga)).dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(data, fontSize = 8.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
