package ba.sum.fsre.studentskimarketplace.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseConfig;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.repository.AdsRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditAdActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etPrice;
    private MaterialButton btnSave, btnDelete, btnPickImages;
    private ProgressBar progress;

    private AdsRepository adsRepository;
    private SupabaseRestClient client;
    private String adId;

    private final List<String> existingImageUrls = new ArrayList<>();
    private ExistingImagesAdapter existingAdapter;

    private final List<Uri> newImages = new ArrayList<>();
    private ImagePreviewAdapter newImagesAdapter;

    private final ActivityResultLauncher<String[]> pickImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null) return;

                for (Uri u : uris) {
                    if (newImages.size() >= 10) break;
                    if (!newImages.contains(u)) newImages.add(u);
                }

                for (Uri u : newImages) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                u, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception ignored) {}
                }

                newImagesAdapter.setItems(newImages);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_ad);

        if (!AuthSession.isLoggedIn()) {
            Toast.makeText(this, "Prijavi se.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnPickImages = findViewById(R.id.btnPickImages);

        progress = findViewById(R.id.progress);

        client = new SupabaseRestClient();
        adsRepository = new AdsRepository(client);

        String id1 = getIntent().getStringExtra("adId");
        String id2 = getIntent().getStringExtra("editAdId");
        adId = (id1 != null && !id1.trim().isEmpty()) ? id1 : id2;

        if (adId == null || adId.trim().isEmpty()) {
            Toast.makeText(this, "Nedostaje ID oglasa.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etTitle.setText(safe(getIntent().getStringExtra("title")));
        etDescription.setText(safe(getIntent().getStringExtra("description")));

        Double price = null;
        if (getIntent().hasExtra("price")) {
            try { price = getIntent().getDoubleExtra("price", 0); } catch (Exception ignore) {}
        } else {
            String priceStr = getIntent().getStringExtra("price");
            try { if (priceStr != null && !priceStr.trim().isEmpty()) price = Double.parseDouble(priceStr.trim()); }
            catch (Exception ignore) {}
        }
        etPrice.setText(price == null ? "" : String.valueOf(price));

        RecyclerView rvExisting = findViewById(R.id.rvImages);
        rvExisting.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        existingAdapter = new ExistingImagesAdapter(this, url -> confirmRemoveImage(url));
        rvExisting.setAdapter(existingAdapter);

        RecyclerView rvNew = findViewById(R.id.rvNewImages);
        rvNew.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        newImagesAdapter = new ImagePreviewAdapter(this, uri -> {
            newImages.remove(uri);
            newImagesAdapter.setItems(newImages);
        });
        rvNew.setAdapter(newImagesAdapter);

        btnPickImages.setOnClickListener(v -> pickImagesLauncher.launch(new String[]{"image/*"}));
        btnSave.setOnClickListener(v -> doUpdateThenUpload());
        btnDelete.setOnClickListener(v -> confirmDelete());

        loadExistingImages();
    }

    private void doUpdateThenUpload() {
        String title = safe(etTitle.getText());
        String desc = safe(etDescription.getText());
        String priceStr = safe(etPrice.getText());

        if (title.isEmpty()) {
            toast("Naziv je obavezan.");
            return;
        }

        Double price = null;
        if (!priceStr.isEmpty()) {
            try { price = Double.parseDouble(priceStr); }
            catch (Exception e) { toast("Cijena mora biti broj."); return; }
        }

        setLoading(true);

        adsRepository.updateAd(AuthSession.accessToken, adId, title, desc, price, new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> { setLoading(false); toast("Update error: " + e.getMessage()); });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> { setLoading(false); toast("UPDATE FAIL HTTP " + response.code() + "\n" + body); });
                    return;
                }

                if (newImages.isEmpty()) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(EditAdActivity.this, "Oglas ažuriran.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                    return;
                }

                uploadNewNext(0);
            }
        });
    }
    private void uploadNewNext(int index) {
        if (index >= newImages.size()) {
            runOnUiThread(() -> {
                setLoading(false);
                Toast.makeText(EditAdActivity.this, "Oglas ažuriran.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
            return;
        }

        Uri uri = newImages.get(index);
        try {
            byte[] bytes = readAllBytes(uri);
            String ext = guessExt(uri);

            String path = "ads/" + adId + "/" + UUID.randomUUID() + ext;

            uploadToSupabaseStorage(path, bytes, new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> { setLoading(false); toast("Upload greška: " + e.getMessage()); });
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    String resp = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> { setLoading(false); toast("Upload HTTP " + response.code() + ": " + resp); });
                        return;
                    }

                    String publicUrl = SupabaseConfig.SUPABASE_URL
                            + "/storage/v1/object/public/ad-images/" + path;

                    saveImageRef(adId, publicUrl, new Callback() {
                        @Override public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> { setLoading(false); toast("Greška spremanja slike: " + e.getMessage()); });
                        }

                        @Override public void onResponse(Call call, Response response) throws IOException {
                            String b = response.body() != null ? response.body().string() : "";
                            if (!response.isSuccessful()) {
                                runOnUiThread(() -> { setLoading(false); toast("DB HTTP " + response.code() + ": " + b); });
                                return;
                            }
                            uploadNewNext(index + 1);
                        }
                    });
                }
            });

        } catch (Exception e) {
            runOnUiThread(() -> { setLoading(false); toast("Greška čitanja slike."); });
        }
    }

    private void loadExistingImages() {
        setLoading(true);
        client.get("/ad_images", mapOf(
                "select", "url",
                "ad_id", "eq." + adId,
                "order", "created_at.asc"
        ), new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> { setLoading(false); toast("Greška slika: " + e.getMessage()); });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> { setLoading(false); toast("HTTP " + response.code() + ": " + body); });
                    return;
                }

                existingImageUrls.clear();
                try {
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        String url = o.optString("url", "");
                        if (url != null && !url.trim().isEmpty()) existingImageUrls.add(url);
                    }
                } catch (Exception ignore) {}

                runOnUiThread(() -> {
                    setLoading(false);
                    existingAdapter.setItems(existingImageUrls);
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
                runOnUiThread(() -> { setLoading(false); toast("Delete error: " + e.getMessage()); });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    setLoading(false);
                    if (!response.isSuccessful()) {
                        toast("DELETE FAIL HTTP " + response.code() + "\n" + body);
                        return;
                    }
                    Toast.makeText(EditAdActivity.this, "Oglas obrisan.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
        });
    }

    private void confirmRemoveImage(String url) {
        new AlertDialog.Builder(this)
                .setTitle("Ukloni sliku")
                .setMessage("Želiš ukloniti ovu sliku?")
                .setPositiveButton("Ukloni", (d, w) -> removeImage(url))
                .setNegativeButton("Odustani", null)
                .show();
    }

    private void removeImage(String url) {
        setLoading(true);

        client.delete("/ad_images",
                mapOf("ad_id", "eq." + adId, "url", "eq." + url),
                AuthSession.accessToken,
                new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> { setLoading(false); toast("Greška brisanja DB: " + e.getMessage()); });
                    }

                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            runOnUiThread(() -> { setLoading(false); toast("DB HTTP " + response.code() + ": " + body); });
                            return;
                        }

                        String objectPath = extractStoragePathFromPublicUrl(url);
                        if (objectPath == null) {
                            runOnUiThread(() -> { setLoading(false); loadExistingImages(); });
                            return;
                        }

                        deleteFromStorage(objectPath, new Callback() {
                            @Override public void onFailure(Call call, IOException e) {
                                runOnUiThread(() -> { setLoading(false); toast("Storage delete greška: " + e.getMessage()); });
                            }
                            @Override public void onResponse(Call call, Response response) {
                                runOnUiThread(() -> { setLoading(false); loadExistingImages(); });
                            }
                        });
                    }
                });
    }

    private void uploadToSupabaseStorage(String objectPath, byte[] bytes, Callback cb) {
        String url = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/ad-images/" + objectPath;
        RequestBody body = RequestBody.create(bytes, MediaType.parse("application/octet-stream"));

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + AuthSession.accessToken)
                .addHeader("Content-Type", "application/octet-stream")
                .addHeader("x-upsert", "false")
                .put(body)
                .build();

        client.rawClient().newCall(req).enqueue(cb);
    }

    private void saveImageRef(String adId, String url, Callback cb) {
        try {
            JSONObject o = new JSONObject();
            o.put("ad_id", adId);
            o.put("url", url);
            client.post("/ad_images", o.toString(), AuthSession.accessToken, cb);
        } catch (Exception e) {
            cb.onFailure(null, new IOException("JSON error"));
        }
    }

    private void deleteFromStorage(String objectPath, Callback cb) {
        String delUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/ad-images/" + objectPath;

        Request req = new Request.Builder()
                .url(delUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + AuthSession.accessToken)
                .delete()
                .build();

        client.rawClient().newCall(req).enqueue(cb);
    }

    private String extractStoragePathFromPublicUrl(String url) {
        String marker = "/storage/v1/object/public/ad-images/";
        int idx = url.indexOf(marker);
        if (idx == -1) return null;
        return url.substring(idx + marker.length());
    }

    private byte[] readAllBytes(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("No stream");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
        is.close();
        return bos.toByteArray();
    }

    private String guessExt(Uri uri) {
        String s = uri.toString().toLowerCase(Locale.ROOT);
        if (s.contains(".png")) return ".png";
        if (s.contains(".webp")) return ".webp";
        return ".jpg";
    }

    private java.util.Map<String, String> mapOf(String k1, String v1, String k2, String v2, String k3, String v3) {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        return m;
    }

    private java.util.Map<String, String> mapOf(String k1, String v1, String k2, String v2) {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private void toast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSave.setEnabled(!loading);
            btnDelete.setEnabled(!loading);
            btnPickImages.setEnabled(!loading);
            etTitle.setEnabled(!loading);
            etDescription.setEnabled(!loading);
            etPrice.setEnabled(!loading);
        });
    }
}