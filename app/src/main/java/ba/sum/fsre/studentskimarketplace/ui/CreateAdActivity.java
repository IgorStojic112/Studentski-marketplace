package ba.sum.fsre.studentskimarketplace.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

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

public class CreateAdActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etPrice;
    private MaterialButton btnPickImages, btnCreate;
    private android.widget.ProgressBar progress;

    private AdsRepository adsRepository;
    private SupabaseRestClient client;

    private final List<Uri> selectedImages = new ArrayList<>();
    private ImagePreviewAdapter imageAdapter;

    private final ActivityResultLauncher<String[]> pickImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null) return;

                for (Uri u : uris) {
                    if (selectedImages.size() >= 10) break;
                    if (!selectedImages.contains(u)) selectedImages.add(u);
                }

                for (Uri u : selectedImages) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                u, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception ignored) {}
                }

                imageAdapter.setItems(selectedImages);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ad);

        etTitle = findViewById(R.id.etAdTitle);
        etDescription = findViewById(R.id.etAdDescription);
        etPrice = findViewById(R.id.etAdPrice);

        btnPickImages = findViewById(R.id.btnPickImages);
        btnCreate = findViewById(R.id.btnSaveAd);
        progress = findViewById(R.id.progressAd);

        client = new SupabaseRestClient();
        adsRepository = new AdsRepository(client);

        if (!AuthSession.isLoggedIn()) {
            Toast.makeText(this, "Za kreiranje oglasa moraš biti ulogiran.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (AuthSession.accessToken == null || AuthSession.accessToken.trim().isEmpty()) {
            Toast.makeText(this, "Odjavi se i prijavi ponovo.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        RecyclerView rvImages = findViewById(R.id.rvImages);
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImagePreviewAdapter(this, uri -> {
            selectedImages.remove(uri);
            imageAdapter.setItems(selectedImages);
        });
        rvImages.setAdapter(imageAdapter);

        btnPickImages.setOnClickListener(v -> pickImagesLauncher.launch(new String[]{"image/*"}));
        btnCreate.setOnClickListener(v -> createAd());
    }
    private void createAd() {
        String title = safe(etTitle.getText());
        String desc = safe(etDescription.getText());
        String priceStr = safe(etPrice.getText());

        if (title.isEmpty()) {
            toast("Naslov je obavezan.");
            return;
        }

        Double price = null;
        if (!priceStr.isEmpty()) {
            try { price = Double.parseDouble(priceStr); }
            catch (Exception e) { toast("Cijena mora biti broj."); return; }
        }

        setLoading(true);

        adsRepository.createAd(AuthSession.accessToken, AuthSession.userId, title, desc, price, new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> { setLoading(false); toast("Greška: " + e.getMessage()); });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> { setLoading(false); toast("HTTP " + response.code() + ": " + body); });
                    return;
                }

                String adId = extractCreatedAdId(body);
                if (adId == null) {
                    runOnUiThread(() -> { setLoading(false); toast("Oglas kreiran"); finish(); });
                    return;
                }

                try { UUID.fromString(adId); }
                catch (Exception e) {
                    runOnUiThread(() -> { setLoading(false); toast("" + adId); });
                    return;
                }

                if (selectedImages.isEmpty()) {
                    runOnUiThread(() -> { setLoading(false); toast("Oglas kreiran"); finish(); });
                    return;
                }

                uploadNext(0, adId);
            }
        });
    }
    private void uploadNext(int index, String adId) {
        if (index >= selectedImages.size()) {
            runOnUiThread(() -> { setLoading(false); toast("Oglas kreiran"); finish(); });
            return;
        }

        Uri uri = selectedImages.get(index);

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
                            uploadNext(index + 1, adId);
                        }
                    });
                }
            });

        } catch (Exception e) {
            runOnUiThread(() -> { setLoading(false); toast("Greška čitanja slike."); });
        }
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
    private String extractCreatedAdId(String body) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray(body);
            if (arr.length() > 0) {
                JSONObject o = arr.getJSONObject(0);
                String id = o.optString("id", null);
                if (id != null && !id.trim().isEmpty()) return id.trim();
            }
        } catch (Exception ignored) {}
        return null;
    }
    private byte[] readAllBytes(Uri uri) throws IOException {
        ContentResolver cr = getContentResolver();
        InputStream is = cr.openInputStream(uri);
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
    private String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
    private void toast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }
    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnCreate.setEnabled(!loading);
            btnPickImages.setEnabled(!loading);
            etTitle.setEnabled(!loading);
            etDescription.setEnabled(!loading);
            etPrice.setEnabled(!loading);
        });
    }
}