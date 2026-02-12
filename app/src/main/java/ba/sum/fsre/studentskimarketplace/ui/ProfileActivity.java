package ba.sum.fsre.studentskimarketplace.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ba.sum.fsre.studentskimarketplace.ChatActivity;
import ba.sum.fsre.studentskimarketplace.LoginActivity;
import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName;
    private ProgressBar progress;
    private Button btnLogout;

    private SupabaseRestClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (!AuthSession.isLoggedIn()) {
            Toast.makeText(this, "Prijavi se.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvName = findViewById(R.id.tvName);
        progress = findViewById(R.id.progressProfile);
        btnLogout = findViewById(R.id.btnLogout);

        client = new SupabaseRestClient();

        setupBottomNav();
        setupLogout();
        loadProfile();
    }
    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, SearchActivity.class));
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
    private void setupLogout() {
        btnLogout.setOnClickListener(v -> {
            AuthSession.accessToken = null;
            AuthSession.userId = null;

            Intent i = new Intent(ProfileActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
    private void loadProfile() {
        setLoading(true);

        Map<String, String> q = new HashMap<>();
        q.put("select", "full_name");
        q.put("id", "eq." + AuthSession.userId);

        client.get("/profiles", q, AuthSession.accessToken, new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(ProfileActivity.this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> setLoading(false));

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this,
                            "HTTP " + response.code(), Toast.LENGTH_LONG).show());
                    return;
                }

                try {
                    JSONArray arr = new JSONArray(body);
                    JSONObject o = (arr.length() > 0) ? arr.getJSONObject(0) : null;
                    String fullName = (o != null) ? o.optString("full_name", "") : "";

                    runOnUiThread(() ->
                            tvName.setText(fullName.isEmpty() ? "Korisnik" : fullName)
                    );

                } catch (Exception ex) {
                    runOnUiThread(() ->
                            Toast.makeText(ProfileActivity.this, "Greška pri čitanju profila.", Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogout.setEnabled(!loading);
    }
}