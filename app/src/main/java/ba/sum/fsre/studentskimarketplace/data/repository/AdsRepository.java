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
            q.put("title", "ilike.*" + titleQuery.trim() + "*");
        }
        if (faculty != null && !faculty.trim().isEmpty()) {
            q.put("faculty", "ilike." + faculty.trim());
        }

        client.get("/ads", q, cb);
    }
}
