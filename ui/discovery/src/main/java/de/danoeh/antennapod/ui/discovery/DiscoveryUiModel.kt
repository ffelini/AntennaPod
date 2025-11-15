package de.danoeh.antennapod.ui.discovery

import de.danoeh.antennapod.net.discovery.PodcastSearchResult

sealed class DiscoveryEvent {
    object NavigateBack : DiscoveryEvent()
    data class ShowError(val message: String) : DiscoveryEvent()
    object ShowCountryDialog : DiscoveryEvent()
}

data class DiscoveryUiState(
    val isLoading: Boolean = false,
    val isHidden: Boolean = false,
    val needsConfirm: Boolean = false,
    val toplist: List<PodcastSearchResult> = emptyList(),
    val error: String? = null,
    val countryCode: String = "US",
    val countryName: String = "United States"
)
