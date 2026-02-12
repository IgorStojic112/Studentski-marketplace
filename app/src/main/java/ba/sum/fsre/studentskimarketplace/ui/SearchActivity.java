package ba.sum.fsre.studentskimarketplace.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ba.sum.fsre.studentskimarketplace.ChatActivity;
import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.Ad;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.repository.AdsRepository;
import ba.sum.fsre.studentskimarketplace.data.repository.FavoritesRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SearchActivity extends AppCompatActivity {
    private android.widget.EditText etQuery;
    private Button btnSearch;
    private ProgressBar progress;
    private AdsAdapter adapter;
    private AdsRepository adsRepository;
    private FavoritesRepository favoritesRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etQuery = findViewById(R.id.etQuery);
        btnSearch = findViewById(R.id.btnSearch);
        progress = findViewById(R.id.progress);

        RecyclerView rv = findViewById(R.id.rvAds);
        rv.setLayoutManager(new LinearLayoutManager(this));

        SupabaseRestClient client = new SupabaseRestClient();
        adsRepository = new AdsRepository(client);
        favoritesRepository = new FavoritesRepository(client);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        adapter = new AdsAdapter(favoritesRepository);
        rv.setAdapter(adapter);

        adapter.setOnAdActionsListener(ad -> {
            if (!AuthSession.isLoggedIn()) {
                Toast.makeText(SearchActivity.this, "Prijavi se.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ad == null || ad.getId() == null || ad.getId().trim().isEmpty()) {
                Toast.makeText(SearchActivity.this, "Nedostaje ID oglasa.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ad.getUser_id() == null || AuthSession.userId == null || !ad.getUser_id().equals(AuthSession.userId)) {
                Toast.makeText(SearchActivity.this, "Možeš uređivati samo svoje oglase.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(SearchActivity.this, EditAdActivity.class);
            i.putExtra("adId", ad.getId());
            i.putExtra("title", ad.getTitle());
            i.putExtra("description", ad.getDescription());
            i.putExtra("faculty", ad.getFaculty());
            if (ad.getPrice() != null) i.putExtra("price", ad.getPrice());
            startActivity(i);
        });

        adapter.setOnMessageClickListener(ad -> {
            if (!AuthSession.isLoggedIn()) {
                Toast.makeText(SearchActivity.this, "Prijavi se za chat.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ad == null || ad.getId() == null || ad.getId().trim().isEmpty()) {
                Toast.makeText(SearchActivity.this, "Nedostaje adId.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ad.getUser_id() == null || ad.getUser_id().trim().isEmpty()) {
                Toast.makeText(SearchActivity.this, "Nedostaje otherUserId.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (AuthSession.userId != null && AuthSession.userId.equals(ad.getUser_id())) {
                Toast.makeText(SearchActivity.this, "To je tvoj oglas.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(SearchActivity.this, ConversationActivity.class);
            i.putExtra("otherUserId", ad.getUser_id());
            i.putExtra("adId", ad.getId());
            i.putExtra("adTitle", ad.getTitle());
            startActivity(i);
        });

        btnSearch.setOnClickListener(v -> doSearch());

        View btnReset = findViewById(R.id.btnReset);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                etQuery.setText("");
                doSearch();
            });
        }

        View btnCreateAd = findViewById(R.id.btnCreateAd);
        if (btnCreateAd != null) {
            btnCreateAd.setOnClickListener(v -> {
                if (!AuthSession.isLoggedIn()) {
                    Toast.makeText(this, "Prijavi se.", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(new Intent(this, CreateAdActivity.class));
            });
        }
        doSearch();
    }
    @Override
    protected void onResume() {
        super.onResume();
        doSearch();
    }
    private void doSearch() {
        String q = etQuery.getText().toString().trim();

        progress.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);

        adsRepository.search(q, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnSearch.setEnabled(true);
                    Toast.makeText(SearchActivity.this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        btnSearch.setEnabled(true);
                        Toast.makeText(SearchActivity.this, "HTTP " + response.code() + ": " + body, Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                List<Ad> ads = parseAds(body);

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnSearch.setEnabled(true);
                    adapter.setItems(ads);
                    if (ads.isEmpty()) Toast.makeText(SearchActivity.this, "Nema rezultata.", Toast.LENGTH_SHORT).show();
                });

                if (!AuthSession.isLoggedIn()) {
                    runOnUiThread(() -> adapter.setFavoriteIds(new HashSet<>()));
                    return;
                }
                favoritesRepository.listMyFavorites(AuthSession.accessToken, AuthSession.userId, new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> adapter.setFavoriteIds(new HashSet<>()));
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String favJson = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            runOnUiThread(() -> adapter.setFavoriteIds(new HashSet<>()));
                            return;
                        }
                        Set<String> favIds = parseFavoriteAdIds(favJson);
                        runOnUiThread(() -> adapter.setFavoriteIds(favIds));
                    }
                });
            }
        });
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
                    JSONObject img = imgs.optJSONObject(0);
                    if (img != null) ad.setImageUrl(img.optString("url", ""));
                }
                list.add(ad);
            }
        } catch (Exception ignore) { }
        return list;
    }
    private Set<String> parseFavoriteAdIds(String json) {
        Set<String> set = new HashSet<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String adId = o.optString("ad_id", "");
                if (adId != null && !adId.trim().isEmpty() && !"null".equalsIgnoreCase(adId.trim())) {
                    set.add(adId.trim());
                }
            }
        } catch (Exception ignore) { }
        return set;
    }
}