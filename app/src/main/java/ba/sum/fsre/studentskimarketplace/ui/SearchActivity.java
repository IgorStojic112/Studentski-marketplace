package ba.sum.fsre.studentskimarketplace.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.Ad;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.repository.AdsRepository;
import ba.sum.fsre.studentskimarketplace.data.repository.FavoritesRepository;
import ba.sum.fsre.studentskimarketplace.ui.AdsAdapter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText etQuery, etFaculty;
    private Button btnSearch;
    private ProgressBar progress;
    private AdsAdapter adapter;

    private AdsRepository adsRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etQuery = findViewById(R.id.etQuery);
        etFaculty = findViewById(R.id.etFaculty);
        btnSearch = findViewById(R.id.btnSearch);
        progress = findViewById(R.id.progress);

        RecyclerView rv = findViewById(R.id.rvAds);
        rv.setLayoutManager(new LinearLayoutManager(this));
        SupabaseRestClient client = new SupabaseRestClient();
        adsRepository = new AdsRepository(client);

        FavoritesRepository favoritesRepository = new FavoritesRepository(client);

        adapter = new AdsAdapter(favoritesRepository);
        rv.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> doSearch());
    }

    private void doSearch() {
        String q = etQuery.getText().toString().trim();
        String faculty = etFaculty.getText().toString().trim();

        progress.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);

        adsRepository.search(q, faculty, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnSearch.setEnabled(true);
                    Toast.makeText(SearchActivity.this, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        btnSearch.setEnabled(true);
                        Toast.makeText(SearchActivity.this, "" + response.code() + "" + body, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                List<Ad> ads = parseAds(body);

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnSearch.setEnabled(true);
                    adapter.setItems(ads);

                    if (ads.isEmpty()) {
                        Toast.makeText(SearchActivity.this, "Nema rezultata.", Toast.LENGTH_SHORT).show();
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

                list.add(ad);
            }
        } catch (Exception ignore) {
        }
        return list;
    }
}
