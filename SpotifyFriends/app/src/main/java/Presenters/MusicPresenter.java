package Presenters;

import com.example.madeleine.spotifyfriends.MusicActivity;

import java.lang.reflect.Array;
import java.util.ArrayList;

import Model.Artist;
import Model.FakeData;
import Model.Playlist;
import Model.Song;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TrackSimple;
import kaaes.spotify.webapi.android.models.UserPublic;

public class MusicPresenter {
    public FakeData fakeData = new FakeData();
    public ArrayList<String> usernames;
    public ArrayList<Track> songList = new ArrayList<>();
    public ArrayList<Playlist> playlistList = new ArrayList<>();
    public ArrayList<ArtistSimple> artistList = new ArrayList<>();

    public ArrayList<UserPublic> selectedUsers;


    public MusicPresenter() {
    }
}
