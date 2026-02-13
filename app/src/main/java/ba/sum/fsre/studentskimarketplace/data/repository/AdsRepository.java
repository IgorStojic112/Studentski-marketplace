package ba.sum.fsre.studentskimarketplace.data.repository;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import okhttp3.Callback;

public class AdsRepository {

    private final SupabaseRestClient client;

    public AdsRepository(SupabaseRestClient client) {
        this.client = client;
    }

    public void search(String titleQuery, Callback cb) {
        Map<String, String> q = new HashMap<>();
        q.put("select", "id,user_id,title,description,price,created_at,ad_images(url)");;
        q.put("order", "created_at.desc");

        if (titleQuery != null && !titleQuery.trim().isEmpty()) {
            q.put("title", "ilike.*" + titleQuery.trim() + "*");
        }

        client.get("/ads", q, cb);
    }
    public void createAd(String accessToken,
                         String userId,
                         String title,
                         String description,
                         Double price,
                         Callback cb) {
        try {
            JSONObject o = new JSONObject();
            o.put("user_id", userId);
            o.put("title", title);
            o.put("description", description);
            if (price == null) o.put("price", JSONObject.NULL);
            else o.put("price", price);

            client.post("/ads", o.toString(), accessToken, cb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void updateAd(String accessToken,
                         String adId,
                         String title,
                         String description,
                         Double price,
                         Callback cb) {
        try {
            JSONObject o = new JSONObject();
            o.put("title", title);
            o.put("description", description);
            if (price == null) o.put("price", JSONObject.NULL);
            else o.put("price", price);

            client.patch("/ads?id=eq." + adId, o.toString(), accessToken, cb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void deleteAd(String accessToken, String adId, Callback cb) {
        Map<String, String> q = new HashMap<>();
        q.put("id", "eq." + adId);
        client.delete("/ads", q, accessToken, cb);
    }
}