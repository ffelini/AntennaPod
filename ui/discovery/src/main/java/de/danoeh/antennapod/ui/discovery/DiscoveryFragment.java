package de.danoeh.antennapod.ui.discovery;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;

import static de.danoeh.antennapod.ui.discovery.DiscoveryComposeHostKt.setupDiscoveryContent;

public class DiscoveryFragment extends Fragment implements PodcastClickListener {
    public static final String TAG = "DiscoveryFragment";

    public DiscoveryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ComposeView composeView = new ComposeView(requireContext());
        setupDiscoveryContent(composeView, this, this);
        return composeView;
    }

    @Override
    public void onPodcastClick(PodcastSearchResult podcast) {
        if (podcast.feedUrl != null) {
            startActivity(new OnlineFeedviewActivityStarter(requireContext(), podcast.feedUrl).getIntent());
        }
    }
}
