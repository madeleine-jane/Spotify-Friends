package Model;

import android.widget.ImageView;

public class Song implements SpotifyObject{
    public String title;
  //  public ImageView image;
    public Artist artist;
    public String imageName;
    public Song(String title, Artist artist, String imageName) {
        this.title = title;
        this.artist = artist;
        this.imageName = imageName;
    }
}
