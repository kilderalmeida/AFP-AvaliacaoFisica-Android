package com.afp.avaliacao.ui.screens.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afp.avaliacao.ui.components.common.AppScaffold
import com.afp.avaliacao.util.ResultState
import com.afp.avaliacao.viewmodel.MetricsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroTreinadorScreen(
    onBack: () -> Unit,
    viewModel: MetricsViewModel = viewModel()
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val saveState by viewModel.saveAtletaState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is ResultState.Success) {
            onBack()
            viewModel.resetSaveState()
        }
    }

    AppScaffold(
        title = "Cadastrar Treinador",
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Novo Treinador",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Cadastre um novo treinador para que ele possa gerenciar seus próprios atletas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do Treinador") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Badge, null) }
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail de Acesso") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            if (saveState is ResultState.Error) {
                Text(
                    text = (saveState as ResultState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { viewModel.cadastrarTreinador(nome, email) },
                modifier = Modifier.fillMaxWidth(),
                enabled = nome.isNotBlank() && email.contains("@") && saveState !is ResultState.Loading,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (saveState is ResultState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Cadastrar Treinador")
                }
            }
        }
    }
}
