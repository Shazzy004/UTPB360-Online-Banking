package com.example.banca_en_linea.ui.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.remote.dto.MovimientoDto
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovimientosUiState(
    val cuenta: CuentaDto? = null,
    val movimientos: List<MovimientoDto> = emptyList(),
    val otrasCuentas: List<CuentaDto> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
)

class MovimientosViewModel(
    private val cuentaId: Long,
    private val repository: BancaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovimientosUiState())
    val uiState: StateFlow<MovimientosUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            val cuentasRes = repository.obtenerCuentas()
            val cuenta = when (cuentasRes) {
                is Resultado.Exito -> cuentasRes.datos.firstOrNull { it.id == cuentaId }
                is Resultado.Error -> null
            }

            val otras = when (cuentasRes) {
                is Resultado.Exito -> cuentasRes.datos.filter { it.id != cuentaId }
                is Resultado.Error -> emptyList()
            }

            if (cuenta == null) {
                _uiState.update {
                    it.copy(cargando = false, error = "No se pudo cargar la información de la cuenta")
                }
                return@launch
            }

            when (val movsRes = repository.obtenerMovimientos(cuentaId)) {
                is Resultado.Exito -> _uiState.update {
                    it.copy(cuenta = cuenta, movimientos = movsRes.datos, otrasCuentas = otras, cargando = false)
                }
                is Resultado.Error -> _uiState.update {
                    it.copy(cuenta = cuenta, error = movsRes.mensaje, otrasCuentas = otras, cargando = false)
                }
            }
        }
    }

    fun cerrarCuenta(destinoId: Long?, onCompletado: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.cerrarCuenta(cuentaId, destinoId)) {
                is Resultado.Exito -> onCompletado(true, null)
                is Resultado.Error -> onCompletado(false, res.mensaje)
            }
        }
    }
}
