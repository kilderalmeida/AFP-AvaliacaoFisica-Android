package com.afp.avaliacao.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afp.avaliacao.ui.components.common.AppScaffold
import com.afp.avaliacao.util.ShareApkHelper
import com.afp.avaliacao.viewmodel.DemoModeViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    demoViewModel: DemoModeViewModel = viewModel()
) {
    val isDemoMode by demoViewModel.isDemoMode.collectAsState()
    val context = LocalContext.current
    val shareHelper = remember { ShareApkHelper(context) }

    AppScaffold(title = "Configurações", onBack = onBack) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Desenvolvimento", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            ListItem(
                headlineContent = { Text("Modo Demo") },
                supportingContent = { Text("Usa dados fictícios para demonstração") },
                leadingContent = { Icon(Icons.Default.BugReport, null) },
                trailingContent = {
                    Switch(checked = isDemoMode, onCheckedChange = { demoViewModel.toggleDemoMode() })
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Aplicativo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            ListItem(
                headlineContent = { Text("Compartilhar App (APK)") },
                supportingContent = { Text("Enviar instalador para outro dispositivo") },
                leadingContent = { Icon(Icons.Default.Share, null) },
                modifier = Modifier.padding(vertical = 8.dp),
                trailingContent = {
                    Button(onClick = { shareHelper.shareApp() }) {
                        Text("Enviar")
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Versão") },
                supportingContent = { Text("1.0.0 (Build 1)") },
                leadingContent = { Icon(Icons.Default.Info, null) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "Política de Privacidade",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
