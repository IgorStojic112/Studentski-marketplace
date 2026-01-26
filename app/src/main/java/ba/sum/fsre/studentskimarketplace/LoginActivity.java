package ba.sum.fsre.studentskimarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import ba.sum.fsre.studentskimarketplace.data.network.SupabaseConfig;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progress;
    private TextView tvRegister;

    private final OkHttpClient http = new OkHttpClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> doLogin());

        tvRegister.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });
    }

    private void doLogin() {
        if (SupabaseConfig.SUPABASE_URL == null || SupabaseConfig.SUPABASE_URL.trim().isEmpty()
                || SupabaseConfig.SUPABASE_ANON_KEY == null || SupabaseConfig.SUPABASE_ANON_KEY.trim().isEmpty()) {
            Toast.makeText(this, "SUPABASE_URL ili SUPABASE_ANON_KEY nisu postavljeni.", Toast.LENGTH_LONG).show();
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Unesi email i lozinku.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        String url = SupabaseConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password";

        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Login failed", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Mrežna greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "HTTP " + response.code() + " body=" + resp);

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Login nije uspio (HTTP " + response.code() + ")", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                try {
                    JSONObject obj = new JSONObject(resp);

                    String accessToken = obj.optString("access_token", "");
                    JSONObject user = obj.optJSONObject("user");
                    String userId = (user != null) ? user.optString("id", "") : "";

                    if (accessToken.isEmpty() || userId.isEmpty()) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this,
                                    "Login OK, ali nedostaje token ili userId.", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    AuthSession.accessToken = accessToken;
                    AuthSession.userId = userId;

                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Uspješan login!", Toast.LENGTH_SHORT).show();

                        Intent i = new Intent(LoginActivity.this,
                                ba.sum.fsre.studentskimarketplace.ui.SearchActivity.class);
                        startActivity(i);
                        finish();
                    });

                } catch (Exception ex) {
                    Log.e(TAG, "JSON parse error", ex);
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Greška u odgovoru servera.", Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        tvRegister.setEnabled(!loading);
    }
}
