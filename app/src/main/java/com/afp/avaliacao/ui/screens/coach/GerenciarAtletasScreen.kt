package com.afp.avaliacao.ui.screens.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afp.avaliacao.data.Atleta
import com.afp.avaliacao.ui.components.common.AppScaffold
import com.afp.avaliacao.ui.components.common.LoadingView
import com.afp.avaliacao.util.ResultState
import com.afp.avaliacao.viewmodel.MetricsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarAtletasScreen(
    onBack: () -> Unit,
    viewModel: MetricsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveAtletaState.collectAsState()

    // Carregar atletas ao abrir a tela para garantir dados atualizados
    LaunchedEffect(Unit) {
        viewModel.carregarAtletas()
    }

    AppScaffold(
        title = "Gerenciar Atletas",
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            val ativosCount = uiState.atletas.count { it.ativo }
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Limite de Ativos", fontWeight = FontWeight.Bold)
                        Text("$ativosCount de 5 atletas utilizados", style = MaterialTheme.typography.bodySmall)
                    }
                    CircularProgressIndicator(
                        progress = { ativosCount / 5f },
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 4.dp,
                    )
                }
            }

            if (uiState.atletas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum atleta cadastrado.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.atletas) { atleta ->
                        AtletaManagementItem(
                            atleta = atleta,
                            onToggleActive = { viewModel.toggleAtletaAtivo(atleta.id, it) }
                        )
                    }
                }
            }
        }

        if (saveState is ResultState.Loading) {
            LoadingView("Atualizando status...")
        }
    }
}

@Composable
fun AtletaManagementItem(
    atleta: Atleta,
    onToggleActive: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(atleta.nome, fontWeight = FontWeight.Bold)
                Text(atleta.email, fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = atleta.ativo,
                onCheckedChange = { onToggleActive(it) }
            )
        }
    }
}
