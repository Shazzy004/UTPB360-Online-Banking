package com.example.banca_en_linea.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val nombreCliente: String = "",
    val cuentas: List<CuentaDto> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
)

class DashboardViewModel(private val repository: BancaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Nota: la carga inicial NO va en init{} sino en un LaunchedEffect de la
    // pantalla. Razón: el ViewModel sobrevive a la navegación (queda en el
    // back stack), así que init{} solo correría una vez y los saldos quedarían
    // desactualizados al volver de una transferencia.
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            // Perfil y cuentas se piden en secuencia por simplicidad.
            // Optimización futura: lanzarlas en paralelo con async/awaitAll.
            val perfil = repository.obtenerPerfil()
            val cuentas = repository.obtenerCuentas()

            when {
                perfil is Resultado.Error -> _uiState.update {
                    it.copy(cargando = false, error = perfil.mensaje)
                }
                cuentas is Resultado.Error -> _uiState.update {
                    it.copy(cargando = false, error = cuentas.mensaje)
                }
                perfil is Resultado.Exito && cuentas is Resultado.Exito -> _uiState.update {
                    it.copy(
                        cargando = false,
                        nombreCliente = perfil.datos.nombre,
                        cuentas = cuentas.datos,
                    )
                }
            }
        }
    }

    fun cerrarSesion() = repository.cerrarSesion()

    fun crearNuevaCuenta(tipo: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.crearCuenta(tipo)) {
                is Resultado.Exito -> {
                    cargar()
                    onResultado(true)
                }
                is Resultado.Error -> {
                    onResultado(false)
                }
            }
        }
    }
}
