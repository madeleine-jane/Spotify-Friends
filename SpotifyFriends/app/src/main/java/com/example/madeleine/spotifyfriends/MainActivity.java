package com.example.madeleine.spotifyfriends;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Presenters.SearchPresenter;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.UserPrivate;
import kaaes.spotify.webapi.android.models.UserPublic;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {
    SearchPresenter presenter = new SearchPresenter();

    private RecyclerView.Adapter searchAdapter;
    private RecyclerView searchResults;

    private RecyclerView friendsList;
    private RecyclerView.Adapter friendsAdapter;

    private Button submitBtn;
    private EditText searchBar;
    private ArrayList<UserPublic> selectedUsers = new ArrayList();
    private ImageView clearBtn;
    private TextView title;
    private TextView subtitle;
    private ConstraintLayout layout;
    private AlphaAnimation fadeOut = new AlphaAnimation( 1.0f , 0.0f );
    private ProgressBar spinner;

    private int playlistOffset = 0;
    private static final int REQUEST_CODE = 1337;
    private static final String REDIRECT_URI = "spotify-friends-login://callback";
    private String CLIENT_ID = "ea287033be7e49438029b4a147bc7d7b";
    private String accessToken;
    private String myId;
    private SpotifyApi api;
    private SpotifyService spotify;


    //-------------------SPOTIFY API---------------//

    //Takes care of authorization from the Spotify user. Gets auth token from Spotify app or opens login page.
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

    //Called on return from authorization activity
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
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

    //Sets the spotify API and service objects
    public void setSpotifyObjects() {
        api = new SpotifyApi();
        api.setAccessToken(accessToken);
        spotify = api.getService();

    }

    //Entry point for making API calls.
    public void makeApiCalls() {
        if (selectedUsers.size() == 0) {
            spinner.setVisibility(View.VISIBLE);
            getPlaylistOwners();
            getMe();
        }
    }

    //Gets data for logged in user. We need their ID.
    public void getMe() {
        spotify.getMe(new Callback<UserPrivate>() {
            @Override
            public void success(UserPrivate userPrivate, Response response) {
                myId = userPrivate.id;
            }

            @Override
            public void failure(RetrofitError error) {
                return;
            }
        });
    }

    //Examines all playlists owned and followed by the user.
    //Collects all of the playlist authors into a list that the user can search through.
    //The Spotify API does not have a way to get the users that a user follows, so the next
    //best thing is to look at the users who own playlists that the user follows.
    public void getPlaylistOwners() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", 50);
        params.put("offset", playlistOffset);
        spotify.getMyPlaylists(params, new Callback<Pager<PlaylistSimple>>() {
            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                Object[] items = playlistSimplePager.items.toArray();
                ArrayList<String> idsFromPlaylists = new ArrayList<>();
                for (PlaylistSimple playlist : playlistSimplePager.items) {
                    if (!idsFromPlaylists.contains(playlist.owner.id)) {
                        idsFromPlaylists.add(playlist.owner.id);
                        presenter.allUsers.add(playlist.owner);
                    }
                }
                if (playlistSimplePager.items.size() == 50) {
                    playlistOffset += 50;
                    getPlaylistOwners();
                }
                else {
                    filterUsers(presenter.allUsers);
                    setUserImages(presenter.allUsers);
                    spinner.setVisibility(View.GONE);
                    searchBar.setHintTextColor(ContextCompat.getColor(getApplicationContext(), R.color.backgroundMedium));
                    searchBar.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_search_black_24dp,0, 0, 0);
                    return;
                }
            }
            @Override
            public void failure(RetrofitError error) {
                return;
            }
        });
    }

    //Some playlists are owned by Spotify, and some are owned by the user. We don't want either of those.
    public ArrayList<UserPublic> filterUsers(ArrayList<UserPublic> users) {
        for (int i = 0; i < users.size(); ++i) {
            if (users.get(i).display_name.toLowerCase().equals("spotify")) {
                users.remove(i);
                i = 0;
            }
        }
        for (int i = 0; i < users.size(); ++i) {
            if (users.get(i).id.equals(myId) || users.get(i).display_name.toLowerCase().equals("madeleine andersen")) {
                users.remove(i);
                i = 0;
            }
        }
        return users;
    }

    //Gets the image URLS for each user.
    public void setUserImages(ArrayList<UserPublic> users) {
        for (UserPublic userInList : users) {
            spotify.getUser(userInList.id, new Callback<UserPublic>() {
                @Override
                public void success(UserPublic userPublic, Response response) {
                    System.out.println("getting image for: " + userPublic.id);
                    if (userPublic.images.size() > 0) {
                        presenter.userImages.put(userPublic.id, userPublic.images.get(0).url);
                    }
                }

                @Override
                public void failure(RetrofitError error) {

                }
            });
        }
    }



    //-------------------STUFF THAT ISN'T THE SPOTIFY API---------------//

    //Secret shortcut for bypassing this page and going straight to the next one
    public void bypassToMusic() {
        Intent intent = new Intent(MainActivity.this, MusicActivity.class);
        ArrayList<String> userIds = new ArrayList<>();
        userIds.add("paynejoh001");
        intent.putExtra("selectedUserId", "paynejoh001");
        intent.putExtra("selectedUserName", "Caitlin Payne");
        intent.putExtra("myId", myId);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        bypassToMusic();
//        return;

        authSetup();

        submitBtn = findViewById(R.id.get_music_button);
        searchBar = findViewById(R.id.search_friends);
        searchResults = findViewById(R.id.search_results);
        friendsList = findViewById(R.id.selected_users);
        title = findViewById(R.id.spotify_object_title);
        subtitle = findViewById(R.id.subtitle);
        layout = findViewById(R.id.constraintLayout);
        spinner = (ProgressBar)findViewById(R.id.progressBar);
        fadeOut.setDuration(300);

        searchResults.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager friendsManager = new LinearLayoutManager(this);
        friendsManager.setStackFromEnd(true);
        friendsList.setLayoutManager(friendsManager);
        friendsAdapter = new FriendsAdapter(this, selectedUsers);
        friendsList.setAdapter(friendsAdapter);



        submitBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                spinner.setVisibility(View.VISIBLE);
                goToMusic();
            }
        });

        //Changes the layout to remove the title and show only the search bar.
        //Animated fadeout shenanigans, and a fun gradient background.
        searchBar.setOnFocusChangeListener(new TextView.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    title.startAnimation(fadeOut);
                    subtitle.startAnimation(fadeOut);
                    title.setVisibility(View.INVISIBLE);
                    subtitle.setVisibility(View.INVISIBLE);
                    friendsList.setVisibility(View.VISIBLE);
                    renderSearchResults(presenter.allUsers);
                    layout.setBackground(ContextCompat.getDrawable(v.getContext(), R.drawable.full_search_gradient));
                }
            }
        });

        //Updates search results as the user is typing
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ArrayList<UserPublic> results = presenter.search(searchBar.getText().toString());
                renderSearchResults(results);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });

    }

    //Display the search results in the recyclerview
    public void renderSearchResults(ArrayList<UserPublic> userList) {
        System.out.print(this);
        System.out.println(userList);
        searchResults.setVisibility(View.VISIBLE);
        searchAdapter = new SearchAdapter(this, userList);
        searchResults.setAdapter(searchAdapter);
    }

    //--------ADAPTERS, HOLDERS AND ONCLICKLISTENERS OH MY ---------//
    class SearchAdapter extends RecyclerView.Adapter<SearchHolder> {
        private ArrayList<UserPublic> items;
        private LayoutInflater inflater;
        private Context context;

        public SearchAdapter(Context context, ArrayList<UserPublic> items) {
            this.items = items;
            inflater = LayoutInflater.from(context);
            this.context = context;
        }

        @Override
        public SearchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.user_list_item, parent, false);
            return new SearchHolder(view, context);
        }

        @Override
        public void onBindViewHolder(SearchHolder holder, int position) {
            UserPublic item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

    }

    class SearchHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView name;
        private ImageView selected;
        private UserPublic item;
        private LinearLayout itemLayout;
        private ImageView profilePic;
        private Context context;


        public SearchHolder(View view, Context context) {
            super(view);
            name = view.findViewById(R.id.name);
            selected = view.findViewById(R.id.selected_mark);
            itemLayout = view.findViewById(R.id.result_layout);
            profilePic = view.findViewById(R.id.profile_pic);
            itemLayout.setOnClickListener(this);
            this.context = context;

        }

        void bind(UserPublic item) {
            this.item = item;
            name.setText(item.display_name);
            if (presenter.userImages.containsKey(item.id)) {
                Picasso.get().load(presenter.getImage(item)).into(profilePic);
            }
            else {
                profilePic.setBackgroundResource(R.drawable.ic_person_outline_light_24dp);

            }
        }

        @Override
        public void onClick(View view) {
            if (!selectedUsers.contains(item)) {
                addUser(item);
                submitBtn.setVisibility(View.VISIBLE);

            }
            friendsList.scrollToPosition(selectedUsers.size() - 1);

        }
    }

    class FriendsAdapter extends RecyclerView.Adapter<FriendsHolder> {
        private ArrayList<UserPublic> items;
        private LayoutInflater inflater;
        private Context context;

        public FriendsAdapter(Context context, ArrayList<UserPublic> items) {
            this.items = items;
            inflater = LayoutInflater.from(context);
            this.context = context;
        }

        @Override
        public FriendsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.selected_user_item, parent, false);
            return new FriendsHolder(view, context);
        }

        @Override
        public void onBindViewHolder(FriendsHolder holder, final int position) {
            UserPublic item = items.get(position);
            holder.bind(item);
            holder.cancelIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedUsers.remove(selectedUsers.get(position));
                    //find thing remove thing
                    if (selectedUsers.size() == 0) {
                        submitBtn.setVisibility(GONE);
                    }
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, selectedUsers.size());
                }
            });

        }

        @Override
        public int getItemCount() {
            return items.size();
        }

    }

    class FriendsHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView name;
        private UserPublic item;
        private ImageView cancelIcon;
        private FrameLayout itemLayout;
        private Context context;


        public FriendsHolder(View view, Context context) {
            super(view);
            name = view.findViewById(R.id.selected_name);
            itemLayout = view.findViewById(R.id.selected_layout);
            cancelIcon = view.findViewById(R.id.cancel);
            this.context = context;

        }

        void bind(UserPublic item) {
            this.item = item;
            name.setText(item.display_name);

        }

        @Override
        public void onClick(View view) {
        }
    }

    public void addUser(UserPublic user) {
        selectedUsers.add(user);

    }

    public void goToMusic() {
        //timeout on the loader so it disappears after the next activity is started
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(GONE);
            }
        }, 5000);
        Intent intent = new Intent(MainActivity.this, MusicActivity.class);

        //Note: current implementation only supports comparing your music with one other user.
        intent.putExtra("selectedUserId", selectedUsers.get(0).id);
        intent.putExtra("selectedUserName", selectedUsers.get(0).display_name);
        intent.putExtra("myId", myId);
        startActivity(intent);
    }
}
