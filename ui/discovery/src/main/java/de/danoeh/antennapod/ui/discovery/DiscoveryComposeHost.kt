package de.danoeh.antennapod.ui.discovery

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory

/**
 * Helper to set Compose content from Java Fragment and wire up the ViewModel.
 */
fun setupDiscoveryContent(composeView: ComposeView, fragment: Fragment, listener: PodcastClickListener? = null) {
    val vmFactory = AndroidViewModelFactory.getInstance(fragment.requireActivity().application)
    val viewModel = ViewModelProvider(fragment as ViewModelStoreOwner, vmFactory).get(DiscoveryViewModel::class.java)

    composeView.setContent {
        val state = viewModel.uiState.collectAsState()
        DiscoveryScreen(
            uiState = state.value,
            onPodcastClick = { podcast ->
                listener?.onPodcastClick(podcast)
            },
            onHideDiscoveryChanged = { hidden -> viewModel.onHideDiscoveryChanged(hidden) },
            onConfirmDiscovery = { viewModel.onConfirmDiscovery() },
            onCountrySelected = { code -> viewModel.onCountrySelected(code) },
            modifier = Modifier
        )
    }
}
