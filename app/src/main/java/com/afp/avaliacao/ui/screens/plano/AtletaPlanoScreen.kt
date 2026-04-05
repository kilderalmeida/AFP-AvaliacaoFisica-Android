package com.afp.avaliacao.ui.screens.plano

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afp.avaliacao.ui.components.common.*
import com.afp.avaliacao.util.ResultState
import com.afp.avaliacao.viewmodel.PlanoViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AtletaPlanoScreen(
    viewModel: PlanoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { viewModel.carregarPlanoAtleta(it) }
    }

    AppScaffold(title = "Meu Plano de Treino") { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoadingPlano -> LoadingView("Carregando seu plano de treino...")
                uiState.error != null -> ErrorStateView(uiState.error!!, onRetry = { auth.currentUser?.uid?.let { viewModel.carregarPlanoAtleta(it) } })
                uiState.currentPlanoAtleta == null -> EmptyStateView("Nenhum plano ativo no momento. Fale com seu Coach.")
                else -> {
                    val plano = uiState.currentPlanoAtleta!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("Semana Atual: Semana 1", style = MaterialTheme.typography.titleLarge)
                        }

                        val dias = plano.periodo["semana1"] ?: emptyList()
                        items(dias) { dia ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(dia.dia) }
                                        Spacer(Modifier.width(8.dp))
                                        Text(dia.modalidade, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(dia.descricao, fontSize = 14.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Row {
                                        Text("Carga Alvo: ${dia.cargaAlvo}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        Text("Duração: ${dia.duracao}min", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
