package com.c242_ps246.mentalq.ui.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.c242_ps246.mentalq.R
import com.c242_ps246.mentalq.ui.component.CustomDialog
import com.c242_ps246.mentalq.ui.theme.MentalQTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(checkNetworkConnection(connectivityManager))
    val isConnected = _isConnected.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = checkNetworkConnection(connectivityManager)
            }

            override fun onLost(network: Network) {
                _isConnected.value = checkNetworkConnection(connectivityManager)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _isConnected.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    fun close() = runCatching { connectivityManager.unregisterNetworkCallback(callback) }.getOrNull()

    private fun checkNetworkConnection(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

@Composable
fun NetworkAwareContent(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val networkMonitor = remember(context.applicationContext) { NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsStateWithLifecycle()
    var showOfflineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        showOfflineDialog = !isConnected
    }

    DisposableEffect(networkMonitor) {
        onDispose(networkMonitor::close)
    }

    if (isConnected) {
        content()
    } else {
        MentalQTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                content()
                if (showOfflineDialog) {
                    CustomDialog(
                        dialogTitle = stringResource(R.string.offline_title),
                        dialogMessage = stringResource(R.string.offline_message),
                        onConfirm = { showOfflineDialog = false },
                        onDismiss = { showOfflineDialog = false },
                        showCancelButton = false
                    )
                }
            }
        }
    }
}
