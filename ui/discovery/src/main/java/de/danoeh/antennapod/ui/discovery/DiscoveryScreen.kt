package de.danoeh.antennapod.ui.discovery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import de.danoeh.antennapod.net.discovery.PodcastSearchResult
import java.util.Locale

@Composable
fun DiscoveryScreen(
    uiState: DiscoveryUiState,
    onPodcastClick: (PodcastSearchResult) -> Unit,
    onHideDiscoveryChanged: (Boolean) -> Unit,
    onConfirmDiscovery: () -> Unit,
    onCountrySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val showCountryDialogState = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Discover") },
                actions = {
                    TextButton(onClick = { onHideDiscoveryChanged(!uiState.isHidden) }) { Text(if (uiState.isHidden) "Unhide" else "Hide") }
                    TextButton(onClick = { showCountryDialogState.value = true }) { Text("Country") }
                    IconButton(onClick = { /* overflow placeholder */ }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            when {
                uiState.isHidden -> {
                    Text(
                        text = "Discovery is hidden",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                uiState.needsConfirm -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Please confirm to show discovery")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onConfirmDiscovery) {
                            Text("Confirm")
                        }
                    }
                }
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // uiState.error is already non-null inside this branch
                        Text(text = uiState.error, color = MaterialTheme.colors.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onConfirmDiscovery) {
                            Text("Retry")
                        }
                    }
                }
                uiState.toplist.isEmpty() -> {
                    Text(
                        text = "No podcasts found",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(180.dp),
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                    ) {
                        items(uiState.toplist) { podcast ->
                            PodcastGridItem(podcast, onClick = { onPodcastClick(podcast) })
                        }
                    }
                }
            }

            if (showCountryDialogState.value) {
                CountrySelectionDialog(
                    currentCode = uiState.countryCode,
                    onDismiss = { showCountryDialogState.value = false },
                    onSelect = { code ->
                        showCountryDialogState.value = false
                        onCountrySelected(code)
                    }
                )
            }
        }
    }
}

@Composable
fun CountrySelectionDialog(currentCode: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val countryCodes = Locale.getISOCountries().toList().sortedBy { Locale("", it).getDisplayCountry() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select country") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                // show current selection at top to mark the parameter as used
                val currentName = Locale("", currentCode).displayCountry
                Text(text = "Current: $currentName", modifier = Modifier.padding(bottom = 8.dp))

                // simple list of countries as buttons
                for (code in countryCodes) {
                    val name = Locale("", code).displayCountry
                    TextButton(onClick = { onSelect(code) }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = name, textAlign = TextAlign.Left)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PodcastGridItem(podcast: PodcastSearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = 4.dp
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            val imageUrl = podcast.imageUrl ?: ""
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (imageUrl.isNotEmpty()) imageUrl else null)
                    .crossfade(true)
                    .build(),
                contentDescription = podcast.title ?: "",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                val state = painter.state
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        Image(
                            painter = painterResource(id = android.R.drawable.ic_menu_report_image),
                            contentDescription = "Podcast artwork not available for ${podcast.title ?: ""}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(8.dp))
                        )
                    }
                    else -> {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcast.title ?: "",
                    style = MaterialTheme.typography.h6.copy(lineHeight = 20.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                // Prefer safe-handling: show author if present, otherwise show feedUrl if usable
                if (!podcast.author.isNullOrEmpty()) {
                    Text(
                        text = podcast.author.orEmpty(),
                        style = MaterialTheme.typography.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    podcast.feedUrl?.let { url ->
                        if (url.isNotEmpty() && !url.contains("itunes.apple.com")) {
                            Text(text = url, style = MaterialTheme.typography.body2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
