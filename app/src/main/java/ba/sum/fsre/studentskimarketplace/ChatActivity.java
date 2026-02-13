package ba.sum.fsre.studentskimarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ba.sum.fsre.studentskimarketplace.data.model.ChatRow;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.repository.MessagesRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import ba.sum.fsre.studentskimarketplace.ui.ChatAdapter;
import ba.sum.fsre.studentskimarketplace.ui.ConversationActivity;
import ba.sum.fsre.studentskimarketplace.ui.FavoritesActivity;
import ba.sum.fsre.studentskimarketplace.ui.ProfileActivity;
import ba.sum.fsre.studentskimarketplace.ui.SearchActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private ProgressBar progress;
    private ChatAdapter adapter;
    private MessagesRepository repo;

    private final List<ChatRow> rows = new ArrayList<>();
    private boolean namesLoaded = false;
    private boolean titlesLoaded = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (!AuthSession.isLoggedIn() || AuthSession.userId == null) {
            Toast.makeText(this, "Prijavi se za chat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progress = findViewById(R.id.progressChats);

        RecyclerView rv = findViewById(R.id.rvContent);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter();
        rv.setAdapter(adapter);

        repo = new MessagesRepository(new SupabaseRestClient());

        setupBottomNav();
        setupOpenConversationOnClick();

        loadChats();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_chat);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chat) return true;

            if (id == R.id.nav_home) startActivity(new Intent(this, SearchActivity.class));
            else if (id == R.id.nav_favorites) startActivity(new Intent(this, FavoritesActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            else return false;

            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }

    private void setupOpenConversationOnClick() {
        adapter.setOnClick((otherUserId, adId, adTitle) -> {
            otherUserId = safeId(otherUserId);
            adId = safeId(adId);

            if (!isUuid(otherUserId) || !isUuid(adId)) {
                Toast.makeText(
                        ChatActivity.this,
                        "Neispravan chat parametar.\notherUserId=" + otherUserId + "\nadId=" + adId,
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            Intent i = new Intent(ChatActivity.this, ConversationActivity.class);
            i.putExtra("otherUserId", otherUserId);
            i.putExtra("adId", adId);
            i.putExtra("adTitle", adTitle);
            startActivity(i);
        });
    }

    private void loadChats() {
        progress.setVisibility(View.VISIBLE);
        namesLoaded = false;
        titlesLoaded = false;
        rows.clear();
        adapter.setItems(new ArrayList<>());

        repo.listMyRecentMessages(AuthSession.accessToken, AuthSession.userId, 200, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(ChatActivity.this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(ChatActivity.this, "HTTP " + response.code() + ": " + body, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                List<ChatRow> parsed = parseAndGroup(body);

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);

                    rows.clear();
                    if (parsed != null) rows.addAll(parsed);

                    adapter.setItems(new ArrayList<>(rows));

                    loadUserNames(rows);
                    loadAdTitles(rows);
                });
            }
        });
    }

    private List<ChatRow> parseAndGroup(String json) {
        Map<String, ChatRow> map = new LinkedHashMap<>();
        try {
            JSONArray arr = new JSONArray(json);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                String sender = safeId(o.optString("sender_id", null));
                String receiver = safeId(o.optString("receiver_id", null));
                String adId = safeId(o.optString("ad_id", null));

                if (!isUuid(sender) || !isUuid(receiver) || !isUuid(adId)) continue;

                String other = AuthSession.userId.equals(sender) ? receiver : sender;
                if (!isUuid(other)) continue;

                String key = other + "|" + adId;

                if (!map.containsKey(key)) { // query je desc => prvo je najnovije
                    ChatRow r = new ChatRow();
                    r.otherUserId = other;
                    r.adId = adId;
                    r.lastMessage = o.optString("content", "");
                    r.createdAt = o.optString("created_at", "");
                    r.adTitle = "Oglas";
                    r.otherUserName = "Korisnik";
                    map.put(key, r);
                }
            }
        } catch (Exception ignore) {}

        return new ArrayList<>(map.values());
    }

    private void loadUserNames(List<ChatRow> rows) {
        if (rows == null || rows.isEmpty()) {
            namesLoaded = true;
            refreshIfReady();
            return;
        }

        Set<String> ids = new HashSet<>();
        for (ChatRow r : rows) if (r != null && isUuid(r.otherUserId)) ids.add(r.otherUserId);

        if (ids.isEmpty()) {
            namesLoaded = true;
            refreshIfReady();
            return;
        }

        String in = "in.(" + joinCsv(ids) + ")";

        Map<String, String> q = new HashMap<>();
        q.put("select", "id,full_name");
        q.put("id", in);

        new SupabaseRestClient().get("/profiles", q, AuthSession.accessToken, new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    namesLoaded = true;
                    refreshIfReady();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Map<String, String> nameMap = new HashMap<>();

                if (response.isSuccessful()) {
                    try {
                        JSONArray arr = new JSONArray(body);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            nameMap.put(o.optString("id"), o.optString("full_name"));
                        }
                    } catch (Exception ignore) {}
                }

                runOnUiThread(() -> {
                    for (ChatRow r : rows) {
                        if (r == null) continue;
                        String name = nameMap.get(r.otherUserId);
                        if (name != null && !name.trim().isEmpty()) r.otherUserName = name;
                    }
                    namesLoaded = true;
                    refreshIfReady();
                });
            }
        });
    }

    private void loadAdTitles(List<ChatRow> rows) {
        if (rows == null || rows.isEmpty()) {
            titlesLoaded = true;
            refreshIfReady();
            return;
        }

        Set<String> ids = new HashSet<>();
        for (ChatRow r : rows) if (r != null && isUuid(r.adId)) ids.add(r.adId);

        if (ids.isEmpty()) {
            titlesLoaded = true;
            refreshIfReady();
            return;
        }

        String in = "in.(" + joinCsv(ids) + ")";

        Map<String, String> q = new HashMap<>();
        q.put("select", "id,title");
        q.put("id", in);

        new SupabaseRestClient().get("/ads", q, AuthSession.accessToken, new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    titlesLoaded = true;
                    refreshIfReady();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Map<String, String> titleMap = new HashMap<>();

                if (response.isSuccessful()) {
                    try {
                        JSONArray arr = new JSONArray(body);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            titleMap.put(o.optString("id"), o.optString("title"));
                        }
                    } catch (Exception ignore) {}
                }

                runOnUiThread(() -> {
                    for (ChatRow r : rows) {
                        if (r == null) continue;
                        String t = titleMap.get(r.adId);
                        if (t != null && !t.trim().isEmpty()) r.adTitle = t;
                    }
                    titlesLoaded = true;
                    refreshIfReady();
                });
            }
        });
    }

    private void refreshIfReady() {
        if (!namesLoaded || !titlesLoaded) return;
        adapter.setItems(new ArrayList<>(rows));
    }

    private String joinCsv(Set<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String s : ids) sb.append(s).append(",");
        if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private String safeId(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
        return s;
    }

    private boolean isUuid(String s) {
        if (s == null) return false;
        return s.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }
}