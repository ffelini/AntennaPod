package de.danoeh.antennapod.ui.discovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import de.danoeh.antennapod.net.discovery.ItunesTopListLoader
import de.danoeh.antennapod.storage.database.DBReader
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit

import java.util.Locale

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<Application>()
    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    val events = MutableSharedFlow<DiscoveryEvent>()

    private val prefs = app.getSharedPreferences(ItunesTopListLoader.PREFS, 0)

    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + viewModelJob)

    init {
        val countryCode = prefs.getString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE, Locale.getDefault().country) ?: "US"
        val hidden = prefs.getBoolean(ItunesTopListLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, false)
        val needsConfirm = prefs.getBoolean(ItunesTopListLoader.PREF_KEY_NEEDS_CONFIRM, true)
        _uiState.value = _uiState.value.copy(countryCode = countryCode, isHidden = hidden, needsConfirm = needsConfirm)
        loadToplist()
    }

    fun loadToplist() {
        val state = _uiState.value
        if (state.isHidden) {
            _uiState.value = state.copy(isLoading = false, error = null, toplist = emptyList())
            return
        }
        if (state.needsConfirm) {
            _uiState.value = state.copy(isLoading = false, error = null, toplist = emptyList())
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        scope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            scope.launch { events.emit(DiscoveryEvent.ShowError(e.message ?: "Unknown error")) }
            _uiState.value = state.copy(isLoading = false, error = e.message)
        }) {
            val loader = ItunesTopListLoader(app)
            val podcasts = loader.loadToplist(state.countryCode, 25, DBReader.getFeedList())
            _uiState.value = state.copy(isLoading = false, toplist = podcasts, error = null)
        }
    }

    fun onHideDiscoveryChanged(hidden: Boolean) {
        prefs.edit { putBoolean(ItunesTopListLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, hidden) }
        _uiState.value = _uiState.value.copy(isHidden = hidden)
        loadToplist()
    }

    fun onConfirmDiscovery() {
        prefs.edit { putBoolean(ItunesTopListLoader.PREF_KEY_NEEDS_CONFIRM, false) }
        _uiState.value = _uiState.value.copy(needsConfirm = false)
        loadToplist()
    }

    fun onCountrySelected(countryCode: String) {
        prefs.edit { putString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE, countryCode) }
        _uiState.value = _uiState.value.copy(countryCode = countryCode, isHidden = false)
        loadToplist()
    }

    fun onShowCountryDialog() {
        scope.launch { events.emit(DiscoveryEvent.ShowCountryDialog) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
