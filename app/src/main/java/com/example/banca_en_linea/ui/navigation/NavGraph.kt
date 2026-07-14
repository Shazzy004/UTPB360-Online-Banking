package com.example.banca_en_linea.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.ui.dashboard.DashboardScreen
import com.example.banca_en_linea.ui.dashboard.DashboardViewModel
import com.example.banca_en_linea.ui.login.LoginScreen
import com.example.banca_en_linea.ui.login.LoginViewModel
import com.example.banca_en_linea.ui.transferencia.TransferenciaScreen
import com.example.banca_en_linea.ui.transferencia.TransferenciaViewModel

/**
 * Rutas de navegación como constantes: un typo en un string de ruta es un
 * crash en runtime, así que las centralizamos aquí.
 */
object Rutas {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val TRANSFERENCIA = "transferencia"
}

/**
 * Factory genérica: como no usamos Hilt, así le pasamos el repositorio a los
 * ViewModels por constructor (que es lo que los hace testeables).
 */
class BancaViewModelFactory(
    private val repository: BancaRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LoginViewModel::class.java) ->
            LoginViewModel(repository) as T
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(repository) as T
        modelClass.isAssignableFrom(TransferenciaViewModel::class.java) ->
            TransferenciaViewModel(repository) as T
        else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}

@Composable
fun UtpbNavGraph(
    navController: NavHostController,
    repository: BancaRepository,
) {
    val factory = BancaViewModelFactory(repository)

    // Si ya hay sesión guardada, saltamos el login directamente.
    val destinoInicial = if (repository.haySesion()) Rutas.DASHBOARD else Rutas.LOGIN

    NavHost(navController = navController, startDestination = destinoInicial) {

        composable(Rutas.LOGIN) {
            LoginScreen(
                viewModel = viewModel(factory = factory),
                onLoginExitoso = {
                    navController.navigate(Rutas.DASHBOARD) {
                        // Saca el login del back stack: el botón "atrás" en el
                        // dashboard no debe regresar a la pantalla de login.
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Rutas.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel(factory = factory),
                onCerrarSesion = {
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(0) { inclusive = true } // limpia todo el stack
                    }
                },
                onNuevaTransferencia = {
                    navController.navigate(Rutas.TRANSFERENCIA)
                },
            )
        }

        composable(Rutas.TRANSFERENCIA) {
            TransferenciaScreen(
                viewModel = viewModel(factory = factory),
                // popBackStack (no navigate): regresa al dashboard existente
                // en vez de apilar una instancia nueva encima.
                onVolver = { navController.popBackStack() },
            )
        }
    }
}
