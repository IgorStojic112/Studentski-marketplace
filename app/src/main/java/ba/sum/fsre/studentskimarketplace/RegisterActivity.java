package ba.sum.fsre.studentskimarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

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

    private EditText emailField, passField, confirmPassField, nameField;
    private Button btnRegister;
    private final OkHttpClient http = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailField = findViewById(R.id.emailField);
        passField = findViewById(R.id.passField);
        confirmPassField = findViewById(R.id.confirmPassField);
        nameField = findViewById(R.id.nameField);
        TextView loginHint = findViewById(R.id.loginHint);
        loginHint.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passField.getText().toString().trim();
            String confirmPassword = confirmPassField.getText().toString().trim();
            String name = nameField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Popunite sva polja", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Lozinke se ne podudaraju", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Lozinka mora imati barem 6 znakova", Toast.LENGTH_SHORT).show();
                return;
            }
            registerUser(email, password, name);
        });
    }
    private void registerUser(String email, String password, String name) {
        String url = SupabaseConfig.SUPABASE_URL + "/auth/v1/signup";

        try {
            JSONObject data = new JSONObject();
            data.put("full_name", name);

            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("password", password);
            json.put("data", data);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            http.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Signup failed", e);
                    showToast("Mrežna greška: " + e.getMessage());
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Signup HTTP " + response.code() + " body=" + responseData);
                    if (!response.isSuccessful()) {
                        String msg = "Registracija neuspješna (HTTP " + response.code() + ")";
                        try {
                            JSONObject err = new JSONObject(responseData);
                            String errorDesc = err.optString("error_description", "");
                            String message = err.optString("message", "");
                            String msg2 = err.optString("msg", "");
                            if (!errorDesc.isEmpty()) msg += "\n" + errorDesc;
                            else if (!message.isEmpty()) msg += "\n" + message;
                            else if (!msg2.isEmpty()) msg += "\n" + msg2;
                            else msg += "\n" + responseData; //
                        } catch (Exception e) {
                            msg += "\n" + responseData;
                        }

                        showToast(msg);
                        return;
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this,
                                "Registracija uspješna! Provjeri email za potvrdu.",
                                Toast.LENGTH_LONG).show();

                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "JSON error", e);
            showToast("Greška pri pripremi podataka.");
        }
    }
    private void showToast(String message) {
        runOnUiThread(() ->
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show()
        );
    }
}
