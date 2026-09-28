package com.example.data.remote.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    NONE
}

/**
 * Production Hardware Network Connectivity Monitor.
 * Uses Android ConnectivityManager callbacks to track live internet accessibility.
 */
class NetworkConnectivityMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isConnected = MutableStateFlow(checkInitialConnectivity())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionType = MutableStateFlow(checkInitialConnectionType())
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        startMonitoring()
    }

    private fun checkInitialConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun checkInitialConnectionType(): ConnectionType {
        val cm = connectivityManager ?: return ConnectionType.NONE
        val activeNetwork = cm.activeNetwork ?: return ConnectionType.NONE
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return ConnectionType.NONE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.NONE
        }
    }

    fun startMonitoring() {
        if (connectivityManager == null || networkCallback != null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = true
                updateTransport(network)
            }

            override fun onLost(network: Network) {
                // Verify if any other network is available
                _isConnected.value = checkInitialConnectivity()
                _connectionType.value = checkInitialConnectionType()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                _isConnected.value = hasInternet && isValidated
                updateTransport(network)
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            // Fallback for restricted sandbox environments
            _isConnected.value = true
        }
    }

    private fun updateTransport(network: Network) {
        val caps = connectivityManager?.getNetworkCapabilities(network) ?: return
        _connectionType.value = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.NONE
        }
    }

    /**
     * For manual injection / mock testing
     */
    fun setOverrideConnection(connected: Boolean, type: ConnectionType = ConnectionType.WIFI) {
        _isConnected.value = connected
        _connectionType.value = if (connected) type else ConnectionType.NONE
    }

    fun stopMonitoring() {
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
            networkCallback = null
        }
    }
}
