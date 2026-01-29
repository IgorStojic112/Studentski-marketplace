package ba.sum.fsre.studentskimarketplace.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class EditAdActivity extends AppCompatActivity {
    private TextView tvHeader;
    private EditText etTitle, etDescription, etFaculty, etPrice;
    private Button btnSave, btnDelete;
    private ProgressBar progress;
    private AdsRepository adsRepository;
    private String adId;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_ad);

        if (!AuthSession.isLoggedIn()) {
            Toast.makeText(this, "Prijavi se.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvHeader = findViewById(R.id.tvAdHeader);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etFaculty = findViewById(R.id.etFaculty);
        etPrice = findViewById(R.id.etPrice);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        progress = findViewById(R.id.progress);
        tvHeader.setText("Edit ad");
        adsRepository = new AdsRepository(new SupabaseRestClient());

        String id1 = getIntent().getStringExtra("adId");
        String id2 = getIntent().getStringExtra("editAdId");
        adId = (id1 != null && !id1.trim().isEmpty()) ? id1 : id2;
        String title = getIntent().getStringExtra("title");
        String desc = getIntent().getStringExtra("description");
        String faculty = getIntent().getStringExtra("faculty");

        Double price = null;
        if (getIntent().hasExtra("price")) {
            try {
                price = getIntent().getDoubleExtra("price", 0);
            } catch (Exception ignore) {}
        } else {
            String priceStr = getIntent().getStringExtra("price");
            try {
                if (priceStr != null && !priceStr.trim().isEmpty()) price = Double.parseDouble(priceStr.trim());
            } catch (Exception ignore) {}
        }
        if (adId == null || adId.trim().isEmpty()) {
            Toast.makeText(this, "Nedostaje ID oglasa.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etTitle.setText(title != null ? title : "");
        etDescription.setText(desc != null ? desc : "");
        etFaculty.setText(faculty != null ? faculty : "");
        etPrice.setText(price == null ? "" : String.valueOf(price));
        btnSave.setOnClickListener(v -> doUpdate());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }
    private void doUpdate() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String faculty = etFaculty.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title je obavezan.", Toast.LENGTH_SHORT).show();
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
        adsRepository.updateAd(AuthSession.accessToken, adId, title, desc, faculty, price, new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(EditAdActivity.this, "Update error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    setLoading(false);
                    if (!response.isSuccessful()) {
                        Toast.makeText(EditAdActivity.this,
                                "UPDATE FAIL HTTP " + response.code() + "\n" + body,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(EditAdActivity.this, "Oglas ažuriran.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
        });
    }
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Obriši oglas")
                .setMessage("Jesi siguran da želiš obrisati ovaj oglas?")
                .setPositiveButton("Da", (d, w) -> doDelete())
                .setNegativeButton("Ne", null)
                .show();
    }
    private void doDelete() {
        setLoading(true);
        adsRepository.deleteAd(AuthSession.accessToken, adId, new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(EditAdActivity.this, "Delete error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    setLoading(false);
                    if (!response.isSuccessful()) {
                        Toast.makeText(EditAdActivity.this,
                                "DELETE FAIL HTTP " + response.code() + "\n" + body,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(EditAdActivity.this, "Oglas obrisan.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
        });
    }
    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnDelete.setEnabled(!loading);
        etTitle.setEnabled(!loading);
        etDescription.setEnabled(!loading);
        etFaculty.setEnabled(!loading);
        etPrice.setEnabled(!loading);
    }
}
