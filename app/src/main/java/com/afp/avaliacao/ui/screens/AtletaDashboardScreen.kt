package com.afp.avaliacao.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afp.avaliacao.ui.components.common.*
import com.afp.avaliacao.ui.screens.coach.MetricCard
import com.afp.avaliacao.ui.screens.coach.ModalidadesChart
import com.afp.avaliacao.util.ResultState
import com.afp.avaliacao.viewmodel.AtletaMetricsViewModel
import com.afp.avaliacao.viewmodel.ProcessedMetrics
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtletaDashboardScreen(
    onStartCheckIn: () -> Unit,
    onStartCheckOut: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AtletaMetricsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showOpenCheckInAlert by remember { mutableStateOf(false) }

    val hasOpenCheckIn = (uiState.metricsState as? ResultState.Success)?.data?.hasOpenCheckIn ?: false

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.carregarMetricas()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    if (showOpenCheckInAlert) {
        AlertDialog(
            onDismissRequest = { showOpenCheckInAlert = false },
            title = { Text("Check-in em Aberto") },
            text = { Text("Você possui um treino em andamento. É necessário realizar o Check-out antes de iniciar um novo Check-in.") },
            confirmButton = {
                Button(onClick = { 
                    showOpenCheckInAlert = false
                    onStartCheckOut()
                }) { Text("Ir para Check-out") }
            },
            dismissButton = {
                TextButton(onClick = { showOpenCheckInAlert = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (uiState.userName.isNotEmpty()) "Olá, ${uiState.userName}" else "AFP Performance", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Configurações") }
                    IconButton(onClick = { showLogoutDialog = true }) { Icon(Icons.Default.Logout, "Sair") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (hasOpenCheckIn) {
                            showOpenCheckInAlert = true
                        } else {
                            onStartCheckIn()
                        }
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Check-in") },
                    containerColor = if (hasOpenCheckIn) Color.Gray else Color(0xFF4CAF50),
                    contentColor = if (hasOpenCheckIn) Color.LightGray else Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExtendedFloatingActionButton(
                    onClick = onStartCheckOut,
                    icon = { Icon(Icons.Default.ExitToApp, null) },
                    text = { Text("Check-out") },
                    containerColor = Color(0xFFF44336),
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Suas Métricas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            
            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Período: ", fontWeight = FontWeight.Bold)
                listOf(7, 30).forEach { dias ->
                    FilterChip(
                        selected = uiState.diasSelecionados == dias,
                        onClick = { viewModel.onDiasSelected(dias) },
                        label = { Text("$dias dias") },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            if (hasOpenCheckIn) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEB3B).copy(alpha = 0.2f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBC02D))
                        Spacer(Modifier.width(8.dp))
                        Text("Você tem um treino em andamento!", fontWeight = FontWeight.Bold, color = Color(0xFFFBC02D))
                    }
                }
            }

            when (val state = uiState.metricsState) {
                is ResultState.Loading -> LoadingView("Carregando suas métricas...")
                is ResultState.Error -> ErrorStateView(state.message, onRetry = { viewModel.carregarMetricas() })
                is ResultState.Success -> {
                    AtletaMetricsContent(state.data)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun AtletaMetricsContent(metrics: ProcessedMetrics) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Minha Carga", "${metrics.cargaTotal}", Modifier.weight(1f), Color(0xFFBBDEFB))
                    MetricCard("Meu PSE", "%.1f".format(metrics.pseMedio), Modifier.weight(1f), Color(0xFFC8E6C9))
                }
            }
        }

        if (metrics.distribuicaoModalidades.isNotEmpty()) {
            item {
                Text("Minhas Modalidades", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                ModalidadesChart(metrics.distribuicaoModalidades)
            }
        }

        item {
            Text("Histórico Recente", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        if (metrics.sessoesBrutas.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhuma sessão registrada no período.", color = Color.Gray)
                }
            }
        } else {
            items(metrics.sessoesBrutas) { sessao ->
                SessionHistoryItem(sessao, sdf)
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SessionHistoryItem(sessao: Map<String, Any>, sdf: SimpleDateFormat) {
    val data = (sessao["dataCheckin"] as? com.google.firebase.Timestamp)?.toDate()
    val atividades = (sessao["atividades"] as? List<*>)?.joinToString(", ") ?: "Sem atividades"
    val carga = (sessao["carga"] as? Long) ?: 0L
    val finalizado = sessao["pseFoster"] != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data?.let { sdf.format(it) } ?: "Data desconhecida",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Text(
                    text = atividades,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (finalizado) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Carga: $carga",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text("Em andamento", color = Color(0xFFFFA000), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
