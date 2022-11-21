package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditServerViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {
    fun addServer(serverConfig: ServerConfig) = viewModelScope.launch {
        settingsManager.addServer(serverConfig)
    }

    fun editServer(serverConfig: ServerConfig) = viewModelScope.launch {
        settingsManager.editServer(serverConfig)
    }

    fun removeServer(serverConfig: ServerConfig) = viewModelScope.launch {
        settingsManager.removeServer(serverConfig)
    }
}