package Model;

import android.media.Image;

import java.util.ArrayList;
import java.util.Arrays;

public class FakeData {
    public User[] fakeUsers = {
            new User("Madeleine Andersen", "phasergirl", ""),
            new User("Maxie Hendworth", "mjmadness", ""),
            new User("Garfield Dealswarlock", "youlikedeals", ""),
            new User("Douglas Eiffel", "hello-universe", ""),
            new User("Boaty McBoatface", "itcouldhavebeenreal", ""),
            new User("George Takei", "ohmy", "")
    };
    public ArrayList<Artist> fakeArtists = new ArrayList<>(Arrays.asList(
            new Artist("Jack Johnson", "jack_johnson"),
            new Artist("Ok Go", "ok_go"),
            new Artist("Regina Spektor", "regina_spektor"),
            new Artist("Norah Jones", "norah_jones"),
            new Artist("Mika", "mika")
    ));
//    public ArrayList<Playlist> fakePlaylists = new ArrayList<>(Arrays.asList(
////            new Playlist(fakeArtists.get(0), fakeArtists.get(1), fakeArtists.get(2), "mix_1"),
////            new Playlist(fakeArtists.get(3), fakeArtists.get(4), fakeArtists.get(1), "mix_2"),
////            new Playlist(fakeArtists.get(2), fakeArtists.get(0), fakeArtists.get(3), "mix_3")
//            return null;
//    ));
    public ArrayList<Song> fakeSongs = new ArrayList<>(Arrays.asList(
            new Song("Banana Pancakes", fakeArtists.get(0), "banana_pancakes"),
            new Song("Far Away", fakeArtists.get(1), "far_away"),
            new Song("Wallet", fakeArtists.get(2), "wallet"),
            new Song("Don't Know Why", fakeArtists.get(3), "dont_know_why"),
            new Song("Pantsuit Sasquatch", fakeArtists.get(4), "pantsuit_sasquatch"),
            new Song("In Too Deep", fakeArtists.get(1), "in_too_deep"),
            new Song("Through Fire and Flames", fakeArtists.get(2), "through_fire_and_flames"),
            new Song("In The End", fakeArtists.get(3), "in_the_end")

    ));


}
