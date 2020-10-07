package com.example.madeleine.spotifyfriends;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import Model.Playlist;
import Model.Song;
import Model.SpotifyObject;
import Presenters.MusicPresenter;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.ArtistsCursorPager;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.SavedAlbum;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MusicActivity extends AppCompatActivity {

    ///////---VARIABLES RELEVANT TO THE DEMO---/////////

    //Api setup
    private String id;
    private String name;
    private MusicPresenter presenter;
    private String accessToken;
    private static final int REQUEST_CODE = 1337;
    private static final String REDIRECT_URI = "spotify-friends-login://callback";
    private String CLIENT_ID = "ea287033be7e49438029b4a147bc7d7b";
    private String myId;
    private SpotifyApi api;
    private SpotifyService spotify;

    //UI objects
    private Button tracks_btn;
    private ProgressBar spinner;

    //For keeping track of recursive API calls
    public boolean theirDataComplete = false;
    public int theirTracksFromPlaylistsOffset = 0;
    public HashMap<String, Integer> theirTracksInsidePlaylistsOffsets = new HashMap<>();
    public int theirEvaluatedPlaylists = 0;
    public int theirTotalPlaylists = 0;
    public boolean theirPlaylistsEvaluated = false;

    //The selected Spotify user's data
    public ArrayList<Track> theirTracks = new ArrayList<>();
    public ArrayList<ArtistSimple> theirArtists = new ArrayList<>();
    public ArrayList<Artist> displayArtists = new ArrayList<>();


    //---VARIABLES THAT ARE STILL IN DEVELOPMENT---//

    public HashSet<Track> myTracksFromSongs = new HashSet<>();
    public HashSet<ArtistSimple> myArtistsFromSongs = new HashSet<>();
    public HashSet<Track> myTracksFromAlbums = new HashSet<>();
    public HashSet<ArtistSimple> myArtistsFromAlbums = new HashSet<>();
    public HashSet<ArtistSimple> myArtistsFromArtists = new HashSet<>();
    public HashSet<Track> myTracksFromPlaylists = new HashSet<>();
    public HashSet<ArtistSimple> myArtistsFromPlaylists = new HashSet<>();

    public int tracksFromSongsOffset = 0;
    public int tracksFromAlbumsOffset = 0;
    public int artistsFromArtistsOffset = 0;
    public int tracksFromPlaylistsOffset = 0;
    public HashMap<String, Integer> tracksInsidePlaylistsOffsets = new HashMap<>();

    public int evaluatedPlaylists = 0;
    public int totalPlaylists = 0;
    public boolean songsEvaluated = false;
    public boolean albumsEvaluated = false;
    public boolean playlistsEvaluated = false;
    public boolean artistsEvaluated = false;
    public boolean myDataComplete = false;


    ////---METHODS RELEVANT TO THE DEMO---////

    //Spotify authorization setup
    public void authSetup() {
        final AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"playlist-read-collaborative",
                        "user-read-private",
                        "playlist-modify-public",
                        "playlist-read-private",
                        "user-follow-read",
                        "user-top-read",
                        "playlist-modify-private",
                        "user-library-read"})
                .build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

    }

    //Callback from Spotify authorization
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    accessToken = response.getAccessToken();
                    setSpotifyObjects();
                    makeApiCalls();
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }

    //Set Spotify API and Service objects
    public void setSpotifyObjects() {
        api = new SpotifyApi();
        api.setAccessToken(accessToken);
        spotify = api.getService();
    }

    //Entry point for API calls
    public void makeApiCalls() {
        fakeDataRedirect();
//        getMyData();
//        getTheirData();
    }

    /*
    Get data for the demo.

    Note: Getting all of my data from Spotify and all of the other user's data from Spotify
    took longer than was reasonable for a demo. This is method redirects to a scaled-down
    implementation that only pulls the other user's data, and then selects random tracks and artists
    from their data and displays it as "matching" on the page.
    There are ways I could make the original concept work, but I couldn't finish before the due date.
     */
    public void fakeDataRedirect() {
        getTheirData();

    }
    public void getTheirData() {
        spinner.setVisibility(View.VISIBLE);
        getTheirPlaylistsSegment(id);
    }
    public void checkIfTheirDataComplete() {
        if (theirPlaylistsEvaluated) {
            theirDataComplete = true;
            displayData();
        }
    }
    //Get all playlists from the selected user
    public void getTheirPlaylistsSegment(final String username) {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 50);
        params.put("offset", theirTracksFromPlaylistsOffset);
        spotify.getPlaylists(username, params, new Callback<Pager<PlaylistSimple>>() {
            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                theirTotalPlaylists = playlistSimplePager.total;
                for (PlaylistSimple playlist : playlistSimplePager.items) {
                    theirTracksInsidePlaylistsOffsets.put(playlist.id, 0);
                    addTheirTracksFromPlaylist(playlist.id);
                }
                if (playlistSimplePager.items.size() == 50) {
                    theirTracksFromPlaylistsOffset += 50;
                    getTheirPlaylistsSegment(username);
                }
                else {
                    return;
                }
            }
            @Override
            public void failure(RetrofitError error) {
                return;
            }
        });
    }
    //Get all tracks from a given playlist
    public void addTheirTracksFromPlaylist(final String playlistID) {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 50);
        params.put("offset", theirTracksInsidePlaylistsOffsets.get(playlistID));
        spotify.getPlaylistTracks(myId, playlistID, params, new Callback<Pager<PlaylistTrack>>() {
            @Override
            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                for (PlaylistTrack track : playlistTrackPager.items) {
                    try {
                        if (!listContainsTrack(track.track)) {
                            theirTracks.add(track.track);
                        }
                    }
                    catch (NullPointerException e) {
                        System.out.println(e.toString());
                    }
                    try {
                        if (!listContainsArtist(track.track.artists.get(0))) {
                            theirArtists.add(track.track.artists.get(0));
                        }
                    }
                    catch (NullPointerException e) {
                        System.out.println(e.toString());
                    }
                }
                if (playlistTrackPager.items.size() == 50) {
                    Integer offset = theirTracksInsidePlaylistsOffsets.get(playlistID);
                    theirTracksInsidePlaylistsOffsets.put(playlistID, offset + 50);
                    addTheirTracksFromPlaylist(playlistID);
                }
                else {
                    ++theirEvaluatedPlaylists;
                    if (theirEvaluatedPlaylists == theirTotalPlaylists) {
                        theirPlaylistsEvaluated = true;
                    }
                    checkIfTheirDataComplete();
                    return;
                }
            }
            @Override
            public void failure(RetrofitError error) {

            }
        });

    }
    public boolean listContainsArtist(ArtistSimple a) {
        for (ArtistSimple b : theirArtists) {
            if (b.uri.equals(a.uri)) {
                return true;
            }
        }
        return false;
    }
    public boolean listContainsTrack(Track t) {
        for (Track b : theirTracks) {
            if (b.uri.equals(t.uri)) {
                return true;
            }
        }
        return false;
    }

    //Grabs random tracks/artists from the pulled data and displays it
    public void displayData() {
        ArrayList<Track> selectedTracks = new ArrayList<>();
        ArrayList<ArtistSimple> selectedArtists = new ArrayList<>();
        ArrayList<String> mixTitles = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            presenter.artistList.add(theirArtists.get(new Random().nextInt(theirArtists.size())));
        }
        for (int i = 0; i < 15; ++i) {
            presenter.songList.add(theirTracks.get(new Random().nextInt(theirTracks.size())));
        }
        for (int i = 0; i < 3; ++i) {
            Playlist p = new Playlist(theirArtists.get(new Random().nextInt(theirArtists.size())).name,
                    theirArtists.get(new Random().nextInt(theirArtists.size())).name,
                    theirArtists.get(new Random().nextInt(theirArtists.size())).name, "mix_" + (i+1));
            presenter.playlistList.add(p);
        }

        renderMainSections();
        getArtistData(presenter.artistList);
    }

    //Gets artist images
    public void getArtistData(ArrayList<ArtistSimple> artists) {
        for (ArtistSimple a : artists) {
            spotify.getArtist(a.id, new Callback<Artist>() {
                @Override
                public void success(Artist artist, Response response) {
                    displayArtists.add(artist);
                    checkArtistsEvaluated();
                }

                @Override
                public void failure(RetrofitError error) {

                }
            });
        }
    }

    public void checkArtistsEvaluated() {
        if (displayArtists.size() == 5) {
            renderArtists(displayArtists);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        Bundle extras = getIntent().getExtras();
        authSetup();
        id = extras.getString("selectedUserId");
        name = extras.getString("selectedUserName");
        myId = extras.getString("myId");
        presenter = new MusicPresenter();
        spinner = (ProgressBar)findViewById(R.id.progressBar);

        tracks_btn = findViewById(R.id.create_playlist_btn);
        tracks_btn.setOnClickListener(new View.OnClickListener() {

            /*
            Creates a playlist in Spotify, and then adds all tracks in the "songs" category to it.
            Displays a toast when complete.
             */
            @Override
            public void onClick(View v) {
                spinner.setVisibility(View.VISIBLE);
                Map<String, Object> params = new HashMap<>();
                params.put("name", "Tracks Shared With " + name); //fix later
                params.put("public", true);
                spotify.createPlaylist(myId, params, new Callback<kaaes.spotify.webapi.android.models.Playlist>() {
                    @Override
                    public void success(kaaes.spotify.webapi.android.models.Playlist playlist, Response response) {
                        String uriStr = presenter.songList.get(0).uri;
                        for (int i = 1; i < presenter.songList.size(); ++i) {
                            uriStr += "," + presenter.songList.get(i).uri;
                        }
                        Map<String, Object> params = new HashMap<>();
                        params.put("uris", uriStr);
                        Map<String, Object> bodyParams = new HashMap<>();
                        spotify.addTracksToPlaylist(myId, playlist.id, params, bodyParams, new Callback<Pager<PlaylistTrack>>() {
                            @Override
                            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                                spinner.setVisibility(View.GONE);
                                runOnUiThread(new Runnable() {
                                    public void run()
                                    {
                                        LayoutInflater inflater = getLayoutInflater();
                                        View layout = inflater.inflate(R.layout.custom_toast,
                                                (ViewGroup) findViewById(R.id.custom_toast_container));

                                        TextView text = (TextView) layout.findViewById(R.id.toast_text);
                                        Toast toast = new Toast(getApplicationContext());
                                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                                        toast.setDuration(Toast.LENGTH_LONG);
                                        toast.setView(layout);
                                        toast.show();
                                        //Toast.makeText(getApplicationContext(), "Playlist created! Find it in your Spotify app.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                            @Override
                            public void failure(RetrofitError error) {
                                return;
                            }
                        });
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        return;
                    }
                });
            }
        });
    }
    public void renderMainSections() {
        spinner.setVisibility(View.GONE);
        renderPlaylists(presenter.playlistList);
        //renderArtists(presenter.artistList);
        renderSongs(presenter.songList);
    }
    public void renderPlaylists(ArrayList<Playlist> playlists) {
        LinearLayout list = (LinearLayout) findViewById(R.id.playlist_list);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Playlist playlist : playlists) {
            View to_add = inflater.inflate(R.layout.playlist_result, list, false);
            TextView title = to_add.findViewById(R.id.playlist_name);
            ImageView image = to_add.findViewById(R.id.playlist_image);

            String uri = "@drawable/" + playlist.imageName;
            int imageResource = getResources().getIdentifier(uri, null, getPackageName());
            Drawable res = getResources().getDrawable(imageResource);
            image.setImageDrawable(res);

            title.setText(playlist.a.concat(", ").concat(playlist.b).concat(", ").concat(playlist.c));
            list.addView(to_add);
        }
    }
    public void renderArtists(ArrayList<kaaes.spotify.webapi.android.models.Artist> artists) {
        LinearLayout list = (LinearLayout) findViewById(R.id.artist_list);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Artist artist : artists) {
            View to_add = inflater.inflate(R.layout.artist_result, list, false);
            TextView title = to_add.findViewById(R.id.artist_name);
            ImageView image = to_add.findViewById(R.id.artist_image);
            if (artist.images.size() > 0) {
                Picasso.get().load(artist.images.get(0).url).into(image);
            }
            title.setText(artist.name);
            list.addView(to_add);
        }
    }
    public void renderSongs(ArrayList<Track> songs) {
        LinearLayout list = (LinearLayout) findViewById(R.id.song_list);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Track song: songs) {
            View to_add = inflater.inflate(R.layout.song_result, list, false);
            TextView title = to_add.findViewById(R.id.song_title);
            TextView artist = to_add.findViewById(R.id.artist_name);
            ImageView image = to_add.findViewById(R.id.song_image);
            title.setText(song.name);
            Picasso.get().load(song.album.images.get(0).url).into(image);
            artist.setText(song.artists.get(0).name);
            list.addView(to_add);
        }

    }


    ////---METHODS STILL IN DEVELOPMENT---////

    public void getMyData() {
        getTracksSegment();
        getAlbumsSegment();
        getArtistsSegment();
        getPlaylistsSegment();

    }
    public void checkIfMyDataComplete() {
        if (songsEvaluated && playlistsEvaluated) {
            myDataComplete = true;
            checkIfAllDataComplete();
        }
    }
    public void checkIfAllDataComplete() {
        if (myDataComplete && theirDataComplete) {
            compare();
        }
    }
    //Get songs and artists from my saved songs
    public void getTracksSegment() {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 50);
        params.put("offset", tracksFromSongsOffset);
        spotify.getMySavedTracks(params, new Callback<Pager<SavedTrack>>() {
            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                for (SavedTrack track : savedTrackPager.items) {
                    System.out.println(track.track.name);
                    myTracksFromSongs.add(track.track);
                    myArtistsFromSongs.add(track.track.artists.get(0));
                }
                if (savedTrackPager.items.size() == 50) {
                    tracksFromSongsOffset += 50;
                    getTracksSegment();
                }
                else {
                    songsEvaluated = true;
                    checkIfMyDataComplete();
                    return;
                }
            }
            @Override
            public void failure(RetrofitError error) {
                return;
            }
        });
    }
    //Get songs from my saved albums
    public void getAlbumsSegment() {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 50);
        params.put("offset", tracksFromAlbumsOffset);

        spotify.getMySavedAlbums(params, new Callback<Pager<SavedAlbum>>() {
            @Override
            public void success(Pager<SavedAlbum> savedAlbumPager, Response response) {
                for (SavedAlbum album : savedAlbumPager.items) {
                    System.out.println(album.album.name);
                    myArtistsFromAlbums.add(album.album.artists.get(0));
//                    for (Track track : album.album.tracks.items) {
//                        myTracksFromAlbums.add(track);
//
//                    }
                }
                if (savedAlbumPager.items.size() == 50) {
                    tracksFromAlbumsOffset += 50;
                    getAlbumsSegment();
                }
                else {
                    albumsEvaluated = true;
                    checkIfMyDataComplete();
                    return;
                }
            }
            @Override
            public void failure(RetrofitError error) {
                return;
            }
        });
    }
    //Get my followed artists
    public void getArtistsSegment() {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 50);
        params.put("offset", artistsFromArtistsOffset);
        spotify.getFollowedArtists(params, new Callback<ArtistsCursorPager>() {
            @Override
            public void success(ArtistsCursorPager artistsCursorPager, Response response) {
                for (kaaes.spotify.webapi.android.models.Artist artist : artistsCursorPager.artists.items) {
                    ArtistSimple s = new ArtistSimple();
                    s.href = artist.href;
                    s.external_urls = artist.external_urls;
                    s.id = artist.id;
                    s.name = artist.name;
                    s.type = artist.type;
                    s.uri = artist.uri;
                    myArtistsFromArtists.add(s);

                }
                if (artistsCursorPager.artists.items.size() == 50) {
                    artistsFromArtistsOffset += 50;
                    getArtistsSegment();
                }
                else {
                    artistsEvaluated = true;
                    checkIfMyDataComplete();
                    return;
                }

            }

            @Override
            public void failure(RetrofitError error) {
                return;
            }
        });
    }
    //Get my playlists
    public void getPlaylistsSegment() {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 50);
        params.put("offset", tracksFromPlaylistsOffset);
        spotify.getMyPlaylists(params, new Callback<Pager<PlaylistSimple>>() {
            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                totalPlaylists = playlistSimplePager.total;
                for (PlaylistSimple playlist : playlistSimplePager.items) {
                    tracksInsidePlaylistsOffsets.put(playlist.id, 0);
                    addTracksFromPlaylist(playlist.id);
                }
                if (playlistSimplePager.items.size() == 50) {
                    tracksFromPlaylistsOffset += 50;
                    getPlaylistsSegment();
                }
                else {
                    return;
                }
            }
            @Override
            public void failure(RetrofitError error) {
                return;
            }
        });
    }
    //Get all tracks from a given playlist
    public void addTracksFromPlaylist(final String playlistID) {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 50);
        params.put("offset", tracksInsidePlaylistsOffsets.get(playlistID));
        spotify.getPlaylistTracks(myId, playlistID, params, new Callback<Pager<PlaylistTrack>>() {
            @Override
            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                for (PlaylistTrack track : playlistTrackPager.items) {
                    System.out.println(track.track.name);
                    try {
                        myTracksFromPlaylists.add(track.track);
                    }
                    catch (NullPointerException e) {
                        System.out.println(e.toString());
                    }
                    try {
                        myArtistsFromPlaylists.add(track.track.artists.get(0));
                    }
                    catch (NullPointerException e) {
                        System.out.println(e.toString());
                    }
                }
                if (playlistTrackPager.items.size() == 50) {
                    Integer offset = tracksInsidePlaylistsOffsets.get(playlistID);
                    tracksInsidePlaylistsOffsets.put(playlistID, offset + 50);
                    addTracksFromPlaylist(playlistID);
                }
                else {
                    ++evaluatedPlaylists;
                    if (evaluatedPlaylists == totalPlaylists) {
                        playlistsEvaluated = true;
                    }
                    checkIfMyDataComplete();
                    return;
                }
            }
            @Override
            public void failure(RetrofitError error) {

            }
        });

    }
    //Compare my tracks/artists to their tracks/artists
     public void compare() {
        HashSet<Track> myTracks = mergeMyTracks();
        HashSet<ArtistSimple> myArtists = mergeMyArtists();
        HashSet<Track> matchingTracks = new HashSet<>();
        HashSet<ArtistSimple> matchingArtists = new HashSet<>();

        matchingTracks.retainAll(theirTracks);
        matchingArtists.retainAll(theirArtists);

        for (Track i : myTracks) {
            if (theirTracks.contains(i)) {
                matchingTracks.add(i);
            }
        }
        for (ArtistSimple i : myArtists) {
            if (theirArtists.contains(i)) {
                matchingArtists.add(i);
            }
        }
        System.out.println("Success! Matching tracks: "+ matchingTracks.size());
        System.out.println(matchingArtists);
    }
    public HashSet<ArtistSimple> mergeMyArtists() {
        HashSet<ArtistSimple> allArtists = new HashSet<>();
        allArtists.addAll(myArtistsFromPlaylists);
        allArtists.addAll(myArtistsFromArtists);
        allArtists.addAll(myArtistsFromSongs);
        //allArtists.addAll(myArtistsFromAlbums);
        return allArtists;
    }
    public HashSet<Track> mergeMyTracks() {
        HashSet<Track> allTracks = new HashSet<>();
        allTracks.addAll(myTracksFromPlaylists);
        allTracks.addAll(myTracksFromSongs);
        allTracks.addAll(myTracksFromAlbums);
        return allTracks;
    }

}
