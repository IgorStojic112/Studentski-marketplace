package ba.sum.fsre.studentskimarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import ba.sum.fsre.studentskimarketplace.data.network.SupabaseConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "REGISTER";
    private EditText emailField, passField, nameField, facultyField;
    private Button btnRegister;
    private final OkHttpClient http = new OkHttpClient();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        emailField = findViewById(R.id.emailField);
        passField = findViewById(R.id.passField);
        nameField = findViewById(R.id.nameField);
        facultyField = findViewById(R.id.facultyField);
        btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passField.getText().toString().trim();
            String name = nameField.getText().toString().trim();
            String faculty = facultyField.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || faculty.isEmpty()) {
                Toast.makeText(this, "Popunite sva polja", Toast.LENGTH_SHORT).show();
                return;
            }
            registerUser(email, password, name, faculty);
        });
    }
    private void registerUser(String email, String password, String name, String faculty) {
        String url = SupabaseConfig.SUPABASE_URL + "/auth/v1/signup";

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
                Log.e(TAG, "Signup failed", e);
                showToast("Mrežna greška: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Signup HTTP " + response.code() + " body=" + responseData);

                if (!response.isSuccessful()) {
                    showToast("Registracija neuspješna (HTTP " + response.code() + ")");
                    return;
                }
                String userId = extractUserId(responseData);
                if (userId == null || userId.isEmpty()) {
                    showToast("Registracija OK, ali nije pronađen user id u odgovoru.");
                    return;
                }
                saveToProfiles(userId, name, faculty, email);
            }
        });
    }
    private void saveToProfiles(String userId, String name, String faculty, String email) {
        String url = SupabaseConfig.REST_BASE + "/profiles";

        String json = "{\"id\":\"" + userId + "\",\"full_name\":\"" + name + "\",\"faculty\":\"" + faculty + "\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();
        http.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "profiles insert failed", e);
                showToast("Greška pri spremanju profila: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "profiles HTTP " + response.code() + " body=" + resp);

                if (!response.isSuccessful()) {
                    showToast("Greška pri spremanju profila (HTTP " + response.code() + ")");
                    return;
                }
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "Registracija uspješna!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                });
            }
        });
    }
    private String extractUserId(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            if (obj.has("user")) {
                JSONObject user = obj.getJSONObject("user");
                if (user.has("id")) return user.getString("id");
            }
            if (obj.has("id")) return obj.getString("id");
        } catch (Exception e) {
            Log.e(TAG, "extractUserId parse error", e);
        }
        return null;
    }
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}