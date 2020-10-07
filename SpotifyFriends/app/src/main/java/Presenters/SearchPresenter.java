package Presenters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Model.FakeData;
import Model.User;
import kaaes.spotify.webapi.android.models.UserPublic;

public class SearchPresenter {
    public ArrayList<UserPublic> allUsers = new ArrayList<>();
    public HashMap<String, String> userImages = new HashMap<String, String>();
    public String getImage(UserPublic user) {
        return userImages.get(user.id);
    }
    public ArrayList<UserPublic> search(String searchTerm) {
        ArrayList<UserPublic> results = new ArrayList<>();
        for (UserPublic user : allUsers) {
            if (user.display_name.toLowerCase().replaceAll("\\s+","").contains(searchTerm.replaceAll("\\s+","").toLowerCase())) {
                results.add(user);
            }
        }
        return results;
    }
}
