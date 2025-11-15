package de.danoeh.antennapod.ui.discovery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.Dp
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
                navigationIcon = {
                    IconButton(onClick = { /* back handled by host */ }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
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
                        Text(text = uiState.error, color = MaterialTheme.colors.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onConfirmDiscovery) { Text("Retry") }
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
                    // Match old design: vertical list with square thumbnails (56dp) and title/author on the right
                    val thumbSize = 56.dp
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.toplist) { podcast ->
                            DiscoveryListItem(podcast = podcast, thumbSize = thumbSize, onClick = { onPodcastClick(podcast) })
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
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
                        Text(text = name, textAlign = TextAlign.Start)
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
private fun DiscoveryListItem(podcast: PodcastSearchResult, thumbSize: Dp, onClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(podcast.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = podcast.title ?: "",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(thumbSize)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                val state = painter.state
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) }
                    }
                    is AsyncImagePainter.State.Error -> {
                        Image(
                            painter = painterResource(id = android.R.drawable.ic_menu_report_image),
                            contentDescription = "Podcast artwork not available for ${podcast.title ?: ""}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(6.dp))
                        )
                    }
                    else -> SubcomposeAsyncImageContent()
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcast.title ?: "",
                    style = MaterialTheme.typography.h6.copy(lineHeight = 20.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = podcast.author.orEmpty(),
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
