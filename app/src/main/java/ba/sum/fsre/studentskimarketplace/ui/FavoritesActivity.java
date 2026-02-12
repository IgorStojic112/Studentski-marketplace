package ba.sum.fsre.studentskimarketplace.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ba.sum.fsre.studentskimarketplace.ChatActivity;
import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.Ad;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.repository.FavoritesRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FavoritesActivity extends AppCompatActivity {

    private ProgressBar progress;
    private AdsAdapter adapter;
    private FavoritesRepository favoritesRepository;
    private SupabaseRestClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        if (!AuthSession.isLoggedIn()) {
            Toast.makeText(this, "Prijavi se.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progress = findViewById(R.id.progressFavs);
        RecyclerView rv = findViewById(R.id.rvContent);
        rv.setLayoutManager(new LinearLayoutManager(this));

        client = new SupabaseRestClient();
        favoritesRepository = new FavoritesRepository(client);

        adapter = new AdsAdapter(favoritesRepository);
        rv.setAdapter(adapter);

        setupBottomNav();
        loadFavorites();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        bottomNav.setSelectedItemId(R.id.nav_favorites);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_favorites) return true;

            Intent i = null;
            if (id == R.id.nav_home) {
                i = new Intent(FavoritesActivity.this, SearchActivity.class);
            } else if (id == R.id.nav_chat) {
                i = new Intent(FavoritesActivity.this, ChatActivity.class);
            } else if (id == R.id.nav_profile) {
                i = new Intent(FavoritesActivity.this, ProfileActivity.class);
            }
            if (i == null) return false;

            startActivity(i);
            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }
    private void loadFavorites() {
        progress.setVisibility(View.VISIBLE);

        favoritesRepository.listMyFavorites(AuthSession.accessToken, AuthSession.userId, new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(FavoritesActivity.this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(FavoritesActivity.this, "Favorites error (" + response.code() + ")", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                Set<String> favIds = parseFavoriteAdIds(body);
                loadAdsByIds(favIds);
            }
        });
    }

    private void loadAdsByIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                adapter.setItems(new ArrayList<>());
                adapter.setFavoriteIds(new HashSet<>());
                Toast.makeText(this, "Nema favorita.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        StringBuilder in = new StringBuilder("in.(");
        for (String id : ids) in.append(id).append(",");
        in.deleteCharAt(in.length() - 1).append(")");

        Map<String, String> q = new HashMap<>();
        q.put("select", "id,user_id,title,description,faculty,price,created_at,ad_images(url)");
        q.put("id", in.toString());

        client.get("/ads", q, AuthSession.accessToken, new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(FavoritesActivity.this, "Greška pri učitavanju oglasa.", Toast.LENGTH_LONG).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(FavoritesActivity.this, "Ads error (" + response.code() + ")", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                List<Ad> ads = parseAds(json);

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    adapter.setItems(ads);
                    adapter.setFavoriteIds(ids);
                });
            }
        });
    }
    private Set<String> parseFavoriteAdIds(String json) {
        Set<String> set = new HashSet<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String id = o.optString("ad_id", "").trim();
                if (!id.isEmpty() && !"null".equalsIgnoreCase(id)) set.add(id);
            }
        } catch (Exception ignore) {}
        return set;
    }

    private List<Ad> parseAds(String json) {
        List<Ad> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                Ad ad = new Ad();
                ad.setId(o.optString("id", null));
                ad.setUser_id(o.optString("user_id", null));
                ad.setTitle(o.optString("title", ""));
                ad.setDescription(o.optString("description", ""));
                ad.setFaculty(o.optString("faculty", ""));
                if (!o.isNull("price")) ad.setPrice(o.optDouble("price"));
                ad.setCreated_at(o.optString("created_at", null));

                JSONArray imgs = o.optJSONArray("ad_images");
                if (imgs != null && imgs.length() > 0) {
                    JSONObject img0 = imgs.optJSONObject(0);
                    if (img0 != null) {
                        String url = img0.optString("url", null);
                        if (url != null && !"null".equalsIgnoreCase(url.trim()) && !url.trim().isEmpty()) {
                            ad.setImageUrl(url.trim());
                        }
                    }
                }
                list.add(ad);
            }
        } catch (Exception ignore) {}
        return list;
    }
}