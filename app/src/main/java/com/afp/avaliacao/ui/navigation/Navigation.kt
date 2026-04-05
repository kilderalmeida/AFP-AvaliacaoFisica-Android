package com.afp.avaliacao.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.afp.avaliacao.ui.screens.LoginScreen
import com.afp.avaliacao.ui.screens.AtletaDashboardScreen
import com.afp.avaliacao.ui.screens.checkin.CheckInFlow
import com.afp.avaliacao.ui.screens.checkout.CheckOutFlow
import com.afp.avaliacao.ui.screens.coach.CoachMetricsScreen
import com.afp.avaliacao.ui.screens.coach.CadastroAtletaScreen
import com.afp.avaliacao.ui.screens.coach.GerenciarAtletasScreen
import com.afp.avaliacao.ui.screens.coach.CadastroTreinadorScreen
import com.afp.avaliacao.ui.screens.plano.AtletaPlanoScreen
import com.afp.avaliacao.ui.screens.plano.CoachPlanoScreen
import com.afp.avaliacao.ui.screens.settings.SettingsScreen
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object AtletaDashboard : Screen("atletaDashboard")
    object CoachMetrics : Screen("coachMetrics")
    object CheckIn : Screen("checkin")
    object CheckOut : Screen("checkout")
    object CoachPlano : Screen("coachPlano")
    object AtletaPlano : Screen("atletaPlano")
    object Settings : Screen("settings")
    object CadastroAtleta : Screen("cadastroAtleta")
    object GerenciarAtletas : Screen("gerenciarAtletas")
    object CadastroTreinador : Screen("cadastroTreinador")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val auth = FirebaseAuth.getInstance()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val destination = when (role.lowercase()) {
                        "coach", "treinador", "admin" -> Screen.CoachMetrics.route
                        else -> Screen.AtletaDashboard.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.CoachMetrics.route) {
            CoachMetricsScreen(
                onLogout = {
                    auth.signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                },
                onNavigateToGerarPlano = { navController.navigate(Screen.CoachPlano.route) },
                onNavigateToCadastroAtleta = { navController.navigate(Screen.CadastroAtleta.route) },
                onNavigateToGerenciarAtletas = { navController.navigate(Screen.GerenciarAtletas.route) },
                onNavigateToCadastroTreinador = { navController.navigate(Screen.CadastroTreinador.route) }
            )
        }

        composable(Screen.CadastroAtleta.route) {
            CadastroAtletaScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.GerenciarAtletas.route) {
            GerenciarAtletasScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.CadastroTreinador.route) {
            CadastroTreinadorScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.CoachPlano.route) {
            CoachPlanoScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AtletaDashboard.route) { 
            AtletaDashboardScreen(
                onStartCheckIn = { navController.navigate(Screen.CheckIn.route) },
                onStartCheckOut = { navController.navigate(Screen.CheckOut.route) },
                onViewPlano = { navController.navigate(Screen.AtletaPlano.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    auth.signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                }
            ) 
        }

        composable(Screen.AtletaPlano.route) {
            AtletaPlanoScreen()
        }

        composable(Screen.CheckIn.route) {
            CheckInFlow(
                onFinish = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CheckOut.route) {
            CheckOutFlow(
                onFinish = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
