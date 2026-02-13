package ba.sum.fsre.studentskimarketplace.data.repository;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import okhttp3.Callback;

public class FavoritesRepository {
    private final SupabaseRestClient client;
    public FavoritesRepository(SupabaseRestClient client) {
        this.client = client;
    }
    public void listMyFavorites(String accessToken, String userId, Callback cb) {
        Map<String, String> q = new HashMap<>();
        q.put("select", "ad_id,created_at");
        q.put("user_id", "eq." + userId);
        q.put("order", "created_at.desc");

        client.get("/favorites", q, accessToken, cb);
    }
    public void addFavorite(String accessToken, String userId, String adId, Callback cb) {
        try {
            JSONObject o = new JSONObject();
            o.put("user_id", userId);
            o.put("ad_id", adId);

            client.post("/favorites", o.toString(), accessToken, cb);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void removeFavorite(String accessToken, String userId, String adId, Callback cb) {
        Map<String, String> q = new HashMap<>();
        q.put("user_id", "eq." + userId);
        q.put("ad_id", "eq." + adId);

        client.delete("/favorites", q, accessToken, cb);
    }
}