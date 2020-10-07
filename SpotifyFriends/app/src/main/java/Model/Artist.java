package Model;

import android.widget.ImageView;

public class Artist implements SpotifyObject{
    public String name;
    public String imageName;
    //include image
    public Artist(String name, String imageName) {
        this.name = name;
        this.imageName = imageName;
    }
}
