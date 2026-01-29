package ba.sum.fsre.studentskimarketplace.data.repository;

import java.util.HashMap;
import java.util.Map;

import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import okhttp3.Callback;

public class AdsRepository {
    private final SupabaseRestClient client;
    public AdsRepository(SupabaseRestClient client) {
        this.client = client;
    }

    public void search(String titleQuery, String faculty, Callback cb) {
        Map<String, String> q = new HashMap<>();
        q.put("select", "id,user_id,title,description,faculty,price,created_at");
        q.put("order", "created_at.desc");

        if (titleQuery != null && !titleQuery.trim().isEmpty()) {
            q.put("title", "ilike.*" + esc(titleQuery.trim()) + "*");
        }
        if (faculty != null && !faculty.trim().isEmpty()) {
            q.put("faculty", "eq." + esc(faculty.trim()));
        }

        client.get("/ads", q, cb);
    }
    public void createAd(String accessToken,
                         String userId,
                         String title,
                         String description,
                         String faculty,
                         Double price,
                         Callback cb) {

        String body = "{"
                + "\"user_id\":\"" + esc(userId) + "\","
                + "\"title\":\"" + esc(title) + "\","
                + "\"description\":\"" + esc(description) + "\","
                + "\"faculty\":\"" + esc(faculty) + "\","
                + (price == null ? "\"price\":null" : "\"price\":" + price)
                + "}";

        client.post("/ads", body, accessToken, cb);
    }
    public void updateAd(String accessToken,
                         String adId,
                         String title,
                         String description,
                         String faculty,
                         Double price,
                         Callback cb) {
        String body = "{"
                + "\"title\":\"" + esc(title) + "\","
                + "\"description\":\"" + esc(description) + "\","
                + "\"faculty\":\"" + esc(faculty) + "\","
                + (price == null ? "\"price\":null" : "\"price\":" + price)
                + "}";

        client.patch("/ads?id=eq." + esc(adId), body, accessToken, cb);
    }
    public void deleteAd(String accessToken, String adId, Callback cb) {
        Map<String, String> q = new HashMap<>();
        q.put("id", "eq." + adId);
        client.delete("/ads", q, accessToken, cb);
    }
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

