package Model;

public class Playlist implements SpotifyObject{
    //image
    public String a;
    public String b;
    public String c;
    public String imageName;
    public Playlist(String a, String b, String c, String imageName) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.imageName = imageName;
    }
}
