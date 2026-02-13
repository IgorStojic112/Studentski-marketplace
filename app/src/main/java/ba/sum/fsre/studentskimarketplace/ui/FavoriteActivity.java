package ba.sum.fsre.studentskimarketplace.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.Ad;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.repository.FavoritesRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FavoriteActivity extends AppCompatActivity {


    private AdsAdapter adapter;
    private FavoritesRepository favoritesRepository;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        progressBar = findViewById(R.id.progress);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if(id == R.id.nav_favorites){
                return true;
            }
            if (id == R.id.nav_search){
                startActivity(new Intent(this,SearchActivity.class));
                finish();
            }else if(id == R.id.nav_chat){
                startActivity(new Intent(this, ChatActivity.class));
                finish();
            }else if (id == R.id.nav_profile){
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
            }

            return true;
        });

        ImageButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            startActivity(new Intent(FavoriteActivity.this, SearchActivity.class));
        });

        RecyclerView rv = findViewById(R.id.rvFavorites);
        rv.setLayoutManager(new LinearLayoutManager(this));

        SupabaseRestClient client = new SupabaseRestClient();
        favoritesRepository = new FavoritesRepository(client);

        adapter = new AdsAdapter(favoritesRepository);
        rv.setAdapter(adapter);

        loadFavorites();

    }

    private void loadFavorites(){

        String userId = AuthSession.userId;
        String token = AuthSession.accessToken;

        progressBar.setVisibility(View.VISIBLE);

        favoritesRepository.getFavorites(token, userId, new Callback(){

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(FavoriteActivity.this, "Ne moguce ucitati favorite", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException{
                if(!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(FavoriteActivity.this, "Ne moze se ucitati", Toast.LENGTH_SHORT).show();
                    });
                    return;
                };

                String body = response.body().string();
                List<Ad> ads = parseAds(body);
                runOnUiThread(() -> {

                        if(adapter != null){

                            HashSet<String> favIds = new HashSet<>();
                            for (Ad ad: ads){
                                if(ad.getId() != null){
                                    favIds.add(ad.getId());
                                }
                            }
                            adapter.setFavoriteIds(favIds);
                            adapter.setItems(ads);
                        }
                        progressBar.setVisibility(View.GONE);
                    }
                );
            }


        });
    }

    private List<Ad> parseAds(String json){
        List<Ad> list = new ArrayList<>();
        try{
            JSONArray arr = new JSONArray(json);
            for(int i = 0; i < arr.length(); i++){
                JSONObject fav = arr.getJSONObject(i);
                JSONObject o = fav.optJSONObject("ads");

                if (o == null) continue;

                Ad ad = new Ad();
                ad.setId(o.optString("id", null));
                ad.setUser_id(o.optString("user_id", null));
                ad.setTitle(o.optString("title", ""));
                ad.setDescription(o.optString("description", ""));
                ad.setFaculty(o.optString("faculty", ""));

                if (!o.isNull("price")) ad.setPrice(o.optDouble("price"));
                ad.setCreated_at(o.optString("created_at", null));

                list.add(ad);
            }
        } catch (Exception ignore) {}
        return list;
    }

    public void refreshFavorites(){
        loadFavorites();
    }



}
