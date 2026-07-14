package com.example.banca_en_linea.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.banca_en_linea.R
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCerrarSesion: () -> Unit,
    onNuevaTransferencia: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Se ejecuta cada vez que el dashboard ENTRA a composición — incluida la
    // vuelta desde la pantalla de transferencia. Así los saldos se refrescan
    // solos después de transferir, sin que el usuario tenga que hacer nada.
    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    Scaffold(
        floatingActionButton = {
            // Botón circular inferior derecho → nueva transferencia
            FloatingActionButton(
                onClick = onNuevaTransferencia,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.transferencia_nueva),
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    TextButton(onClick = {
                        viewModel.cerrarSesion()
                        onCerrarSesion()
                    }) {
                        Text(
                            stringResource(R.string.dashboard_cerrar_sesion),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Estado: cargando
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                // Estado: error con opción de reintentar
                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::cargar) {
                        Text(stringResource(R.string.reintentar))
                    }
                }

                // Estado: contenido
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.dashboard_saludo, state.nombreCliente),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.dashboard_mis_cuentas),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    items(state.cuentas, key = { it.id }) { cuenta ->
                        TarjetaCuenta(cuenta)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaCuenta(cuenta: CuentaDto) {
    // Formato de moneda según convención local (ej. B/. o $ 5,240.75)
    val formato = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (cuenta.tipo == "AHORRO")
                    stringResource(R.string.cuenta_tipo_ahorro)
                else
                    stringResource(R.string.cuenta_tipo_corriente),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = cuenta.numeroCuenta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formato.format(cuenta.saldo),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
