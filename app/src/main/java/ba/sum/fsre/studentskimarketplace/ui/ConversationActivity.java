package ba.sum.fsre.studentskimarketplace.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.ChatMessage;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.repository.MessagesRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ConversationActivity extends AppCompatActivity {

    private String otherUserId;
    private String adId;
    private String adTitle;

    private String otherUserName ;

    private ProgressBar progress;
    private RecyclerView rv;
    private ConversationAdapter adapter;

    private EditText etMessage;
    private MaterialButton btnSend;
    private TextView tvHeader;

    private MessagesRepository repo;

    private boolean isLoading = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            loadMessages(false);
            handler.postDelayed(this, 2500);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        if (!AuthSession.isLoggedIn() || !isUuid(AuthSession.userId)) {
            toast("Prijavi se za chat.");
            finish();
            return;
        }

        otherUserId = safeId(getIntent().getStringExtra("otherUserId"));
        adId = safeId(getIntent().getStringExtra("adId"));
        adTitle = safeText(getIntent().getStringExtra("adTitle"), "Razgovor");

        if (!isUuid(otherUserId) || !isUuid(adId)) {
            toast("Neispravan chat parametar (otherUserId/adId).");
            finish();
            return;
        }

        progress = findViewById(R.id.progressConversation);
        rv = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend); // ✅ FIX: cast u MaterialButton
        tvHeader = findViewById(R.id.tvHeaderConversation);

        if (progress == null || rv == null || etMessage == null || btnSend == null || tvHeader == null) {
            toast("Layout activity_conversation nema sve potrebne view-e.");
            finish();
            return;
        }

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);

        adapter = new ConversationAdapter(AuthSession.userId);
        rv.setAdapter(adapter);

        repo = new MessagesRepository(new SupabaseRestClient());

        btnSend.setOnClickListener(v -> send());

        updateHeader();
        loadOtherUserName();

        loadMessages(true);
    }

    private void loadOtherUserName() {
        if (!isUuid(otherUserId)) return;

        Map<String, String> q = new HashMap<>();
        q.put("select", "full_name");
        q.put("id", "eq." + otherUserId);

        new SupabaseRestClient().get("/profiles", q, AuthSession.accessToken, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) return;

                try {
                    JSONArray arr = new JSONArray(body);
                    if (arr.length() > 0) {
                        JSONObject o = arr.getJSONObject(0);
                        String name = o.optString("full_name", "").trim();
                        if (!name.isEmpty()) otherUserName = name;
                    }
                } catch (Exception ignore) {
                }

                runOnUiThread(() -> updateHeader());
            }
        });
    }

    private void updateHeader() {
        String title = safeText(adTitle, "Razgovor");
        tvHeader.setText(title + "\n" + safeText(otherUserName, "Korisnik"));
    }

    private void loadMessages(boolean showLoader) {
        if (isLoading) return;
        isLoading = true;

        if (showLoader) setLoading(true);

        if (!isUuid(AuthSession.userId) || !isUuid(otherUserId) || !isUuid(adId)) {
            isLoading = false;
            if (showLoader) setLoading(false);
            toast("Neispravan ID (UUID).");
            finish();
            return;
        }

        repo.listConversation(AuthSession.accessToken, AuthSession.userId, otherUserId, adId, 200, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    isLoading = false;
                    if (showLoader) setLoading(false);
                    if (showLoader) toast("Greška: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        if (showLoader) setLoading(false);
                        if (showLoader) toast("HTTP " + response.code() + ": " + body);
                    });
                    return;
                }

                List<ChatMessage> msgs = parseMessages(body);

                runOnUiThread(() -> {
                    isLoading = false;
                    if (showLoader) setLoading(false);

                    adapter.setItems(msgs);
                    if (!msgs.isEmpty()) rv.scrollToPosition(msgs.size() - 1);
                });
            }
        });
    }

    private void send() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        if (!isUuid(AuthSession.userId) || !isUuid(otherUserId) || !isUuid(adId)) {
            toast("Neispravan ID (UUID).");
            return;
        }

        btnSend.setEnabled(false);

        repo.sendMessage(AuthSession.accessToken, AuthSession.userId, otherUserId, adId, text, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnSend.setEnabled(true);
                    toast("Greška: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        toast("HTTP " + response.code() + ": " + body);
                    });
                    return;
                }

                runOnUiThread(() -> {
                    etMessage.setText("");
                    btnSend.setEnabled(true);
                    loadMessages(false);
                });
            }
        });
    }

    private List<ChatMessage> parseMessages(String json) {
        List<ChatMessage> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ChatMessage m = new ChatMessage();
                m.setId(o.optString("id", ""));
                m.setSenderId(o.optString("sender_id", ""));
                m.setReceiverId(o.optString("receiver_id", ""));
                m.setContent(o.optString("content", ""));
                m.setCreatedAt(o.optString("created_at", ""));
                list.add(m);
            }
        } catch (Exception ignore) {
        }
        return list;
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!loading);
        etMessage.setEnabled(!loading);
    }

    private void toast(String msg) {
        if (isFinishing() || isDestroyed()) return;
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private String safeId(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
        return s;
    }

    private String safeText(String s, String fallback) {
        if (s == null) return fallback;
        s = s.trim();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? fallback : s;
    }

    private boolean isUuid(String s) {
        if (s == null) return false;
        return s.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(poll);
        handler.postDelayed(poll, 2500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(poll);
    }
}