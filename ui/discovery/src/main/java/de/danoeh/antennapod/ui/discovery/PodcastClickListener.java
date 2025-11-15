package de.danoeh.antennapod.ui.discovery;

import de.danoeh.antennapod.net.discovery.PodcastSearchResult;

public interface PodcastClickListener {
    void onPodcastClick(PodcastSearchResult podcast);
}

