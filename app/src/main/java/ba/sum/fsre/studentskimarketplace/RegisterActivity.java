package ba.sum.fsre.studentskimarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import ba.sum.fsre.studentskimarketplace.ui.ChatActivity;
import kotlinx.serialization.json.Json;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailField, passField, nameField, facultyField;
    private Button btnRegister;

    //SUPABASE
    private static final String SUPABASE_URL = "https://srnlvpmnoywqwysvgile.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNybmx2cG1ub3l3cXd5c3ZnaWxlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjgzOTc2OTUsImV4cCI6MjA4Mzk3MzY5NX0.oeMe5p5pdxzWTKPwgaoWL23ZKUVUs3nApm5hHeTOO5Y";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        String url = SUPABASE_URL + "/auth/v1/signup";

        String json = "{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}";

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                json, okhttp3.MediaType.parse("application/json; charset=utf-8"));

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        new Thread(() -> {
            try (okhttp3.Response response = client.newCall(request).execute()) {
                String responseData = response.body() != null ? response.body().string() : "";
                Log.d("SUPABASE_AUTH", "Auth Response: " + responseData);

                if (response.isSuccessful() && responseData.contains("\"id\":\"")) {
                    // Izvlačenje ID-a korisnika iz odgovora
                    JSONObject jsonObject = new JSONObject(responseData);
                    String accessToken = jsonObject.getString("access_token");
                    String userId = jsonObject.getJSONObject("user").getString("id");

                    //String userId = responseData.split("\"id\":\"")[1].split("\"")[0];
                    // Sada spremi ostale podatke u profiles
                    saveToProfiles(userId, name, faculty, email,accessToken);

                } else {
                    showToast("Registracija neuspješna: Provjerite mail ili lozinku (min. 6 znakova)");
                }
            } catch (Exception e) {
                showToast("Sistemska greška: " + e.getMessage());
            }
        }).start();
    }

    private void saveToProfiles(String userId, String name, String faculty, String email, String accessToken) {
        // URL ide na profiles tablicu
        String url = SUPABASE_URL + "/rest/v1/profiles";

        String json = "{\"id\":\"" + userId + "\", \"full_name\":\"" + name + "\", \"faculty\":\"" + faculty + "\", \"email\":\"" + email + "\"}";

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                json, okhttp3.MediaType.parse("application/json; charset=utf-8"));

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .build();

        new Thread(() -> {
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Registracija uspješna!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(RegisterActivity.this, ChatActivity.class));
                        finish();
                    });
                } else {
                    String errorLog = response.body() != null ? response.body().string() : "";
                    Log.e("SUPABASE_PROFILES", "Profil error: " + errorLog);
                    showToast("Greška pri spremanju profila: " + response.code());
                }
            } catch (Exception e) {
                showToast("Mrežna greška u profilu: " + e.getMessage());
            }
        }).start();
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}