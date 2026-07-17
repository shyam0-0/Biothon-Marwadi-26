package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import com.medfusion.ai.core.network.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes live connectivity to the app-wide scaffold's offline banner (Phase 6.5). */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
}
