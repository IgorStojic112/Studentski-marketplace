package ba.sum.fsre.studentskimarketplace.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.repository.AdsRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CreateAdActivity extends AppCompatActivity {
    private EditText etTitle, etDescription, etFaculty, etPrice;
    private Button btnCreate;
    private ProgressBar progress;
    private AdsRepository adsRepository;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ad);
        etTitle = findViewById(R.id.etAdTitle);
        etDescription = findViewById(R.id.etAdDescription);
        etFaculty = findViewById(R.id.etAdFaculty);
        etPrice = findViewById(R.id.etAdPrice);
        btnCreate = findViewById(R.id.btnSaveAd);
        progress = findViewById(R.id.progressAd);
        adsRepository = new AdsRepository(new SupabaseRestClient());
        if (!AuthSession.isLoggedIn()) {
            Toast.makeText(this, "Za kreiranje oglasa moraš biti ulogiran.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btnCreate.setOnClickListener(v -> createAd());
    }
    private void createAd() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String faculty = etFaculty.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Naslov je obavezan.", Toast.LENGTH_SHORT).show();
            return;
        }
        Double price = null;
        if (!priceStr.isEmpty()) {
            try {
                price = Double.parseDouble(priceStr);
            } catch (Exception e) {
                Toast.makeText(this, "Cijena mora biti broj.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        setLoading(true);
        adsRepository.createAd(
                AuthSession.accessToken,
                AuthSession.userId,
                title,
                desc,
                faculty,
                price,
                new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(CreateAdActivity.this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";

                        runOnUiThread(() -> setLoading(false));

                        if (!response.isSuccessful()) {
                            runOnUiThread(() ->
                                    Toast.makeText(CreateAdActivity.this, "HTTP " + response.code() + ": " + body, Toast.LENGTH_LONG).show()
                            );
                            return;
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(CreateAdActivity.this, "Oglas kreiran", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                }
        );
    }
    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnCreate.setEnabled(!loading);
        etTitle.setEnabled(!loading);
        etDescription.setEnabled(!loading);
        etFaculty.setEnabled(!loading);
        etPrice.setEnabled(!loading);
    }
}
