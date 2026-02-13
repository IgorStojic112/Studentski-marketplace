package ba.sum.fsre.studentskimarketplace.data.network;

import java.util.Map;
import java.util.Map.Entry;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SupabaseRestClient {
    private final OkHttpClient http = new OkHttpClient();

    public void get(String path, Map<String, String> query, Callback cb) {
        get(path, query, null, cb);
    }

    public OkHttpClient rawClient() {
        return http;
    }

    public void get(String path, Map<String, String> query, String accessToken, Callback cb) {
        HttpUrl base = HttpUrl.parse(SupabaseConfig.REST_BASE + path);
        if (base == null) throw new IllegalArgumentException("Bad URL: " + SupabaseConfig.REST_BASE + path);

        HttpUrl.Builder url = base.newBuilder();

        if (query != null && !query.isEmpty()) {
            for (Entry<String, String> e : query.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    url.addQueryParameter(e.getKey(), e.getValue());
                }
            }
        }

        Request.Builder req = baseRequest(url.build().toString(), accessToken).get();
        Call call = http.newCall(req.build());
        call.enqueue(cb);
    }

    public void post(String path, String jsonBody, String accessToken, Callback cb) {
        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request.Builder req = baseRequest(SupabaseConfig.REST_BASE + path, accessToken)
                .addHeader("Prefer", "return=representation")
                .post(body);

        http.newCall(req.build()).enqueue(cb);
    }
    public void delete(String path, Map<String, String> query, String accessToken, Callback cb) {
        HttpUrl base = HttpUrl.parse(SupabaseConfig.REST_BASE + path);
        if (base == null) throw new IllegalArgumentException("Bad URL: " + SupabaseConfig.REST_BASE + path);

        HttpUrl.Builder url = base.newBuilder();

        if (query != null && !query.isEmpty()) {
            for (Entry<String, String> e : query.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    url.addQueryParameter(e.getKey(), e.getValue());
                }
            }
        }
        Request.Builder req = baseRequest(url.build().toString(), accessToken)
                .addHeader("Prefer", "return=representation")
                .delete();

        http.newCall(req.build()).enqueue(cb);
    }

    public void patch(String pathWithQuery, String jsonBody, String accessToken, Callback cb) {
        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request.Builder req = baseRequest(SupabaseConfig.REST_BASE + pathWithQuery, accessToken)
                .addHeader("Prefer", "return=representation")
                .patch(body);

        http.newCall(req.build()).enqueue(cb);
    }

    private Request.Builder baseRequest(String url, String accessToken) {
        Request.Builder b = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Accept", "application/json");

        if (accessToken != null && !accessToken.trim().isEmpty()) {
            b.addHeader("Authorization", "Bearer " + accessToken.trim());
        }
        return b;
    }
}