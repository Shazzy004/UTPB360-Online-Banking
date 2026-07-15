package com.example.banca_en_linea.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroUiState(
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val cedula: String = "",
    val contrasena: String = "",
    val tipoCuenta: String = "AHORRO", // "AHORRO" | "CORRIENTE"
    val cargando: Boolean = false,
    val error: String? = null,
    val registroExitoso: Boolean = false,
) {
    val puedeEnviar: Boolean get() =
        nombre.isNotBlank() &&
        apellido.isNotBlank() &&
        email.isNotBlank() &&
        cedula.isNotBlank() &&
        contrasena.length >= 6 &&
        !cargando
}

class RegistroViewModel(private val repository: BancaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun onNombreChange(valor: String) {
        _uiState.update { it.copy(nombre = valor, error = null) }
    }

    fun onApellidoChange(valor: String) {
        _uiState.update { it.copy(apellido = valor, error = null) }
    }

    fun onEmailChange(valor: String) {
        _uiState.update { it.copy(email = valor, error = null) }
    }

    fun onCedulaChange(valor: String) {
        _uiState.update { it.copy(cedula = valor, error = null) }
    }

    fun onContrasenaChange(valor: String) {
        _uiState.update { it.copy(contrasena = valor, error = null) }
    }

    fun onTipoCuentaChange(valor: String) {
        _uiState.update { it.copy(tipoCuenta = valor, error = null) }
    }

    fun registrarse() {
        val estado = _uiState.value
        if (!estado.puedeEnviar) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            val resultado = repository.registrarse(
                nombre = estado.nombre.trim(),
                apellido = estado.apellido.trim(),
                email = estado.email.trim(),
                cedula = estado.cedula.trim(),
                contrasena = estado.contrasena,
                tipoCuenta = estado.tipoCuenta
            )

            when (resultado) {
                is Resultado.Exito -> _uiState.update {
                    it.copy(cargando = false, registroExitoso = true)
                }
                is Resultado.Error -> _uiState.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun consumirNavegacion() {
        _uiState.update { it.copy(registroExitoso = false) }
    }
}
