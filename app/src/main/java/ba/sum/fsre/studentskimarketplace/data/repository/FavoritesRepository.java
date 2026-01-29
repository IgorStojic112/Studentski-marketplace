package ba.sum.fsre.studentskimarketplace.data.repository;

import java.util.HashMap;
import java.util.Map;

import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Callback;

public class FavoritesRepository {

    private final SupabaseRestClient client;


    public FavoritesRepository(SupabaseRestClient client) {
        this.client = client;
    }
    public void listMyFavorites(String accessToken, Callback cb) {
        Map<String, String> q = new HashMap<>();
        q.put("select", "ad_id,created_at");
        q.put("order", "created_at.desc");

        client.get("/favorites", q, accessToken, cb);
    }
    public void addFavorite(String accessToken, String userId, String adId, Callback cb) {
        String body = "{"
                + "\"user_id\":\"" + userId + "\","
                + "\"ad_id\":\"" + adId + "\""
                + "}";

        client.post("/favorites", body, accessToken, cb);
    }
    public void removeFavorite(String accessToken, String userId, String adId, Callback cb) {
        Map<String, String> q = new HashMap<>();
        q.put("user_id", "eq." + userId);
        q.put("ad_id", "eq." + adId);

        client.delete("/favorites", q, accessToken, cb);
    }

    public void getFavorites (String accessToken, String userId, Callback callback){
        Map <String, String> query = new HashMap<>();
        query.put("user_id", "eq." + userId);
        query.put("select", "ads(*)");

        client.get("/favorites", query,accessToken,callback);
    }
}
