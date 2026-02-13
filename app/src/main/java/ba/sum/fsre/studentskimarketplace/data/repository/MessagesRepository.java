package ba.sum.fsre.studentskimarketplace.data.repository;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;

public class MessagesRepository {

    private final SupabaseRestClient client;

    private static final String TABLE = "/messages";
    private static final String COL_ID = "id";
    private static final String COL_SENDER = "sender_id";
    private static final String COL_RECEIVER = "receiver_id";
    private static final String COL_AD = "ad_id";
    private static final String COL_CONTENT = "content";
    private static final String COL_CREATED = "created_at";

    public MessagesRepository(SupabaseRestClient client) {
        this.client = client;
    }

    private void fail(Callback cb, String msg) {
        if (cb == null) return;

        Request req = new Request.Builder()
                .url("http://localhost/")
                .build();

        Call fakeCall = client.rawClient().newCall(req);
        cb.onFailure(fakeCall, new IOException(msg));
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

    private String selectCols() {
        return COL_ID + "," + COL_SENDER + "," + COL_RECEIVER + "," + COL_AD + "," + COL_CONTENT + "," + COL_CREATED;
    }

    public void listMyRecentMessages(String accessToken, String userId, int limit, Callback cb) {
        String token = accessToken == null ? "" : accessToken.trim();
        String uid = safeId(userId);

        if (token.isEmpty()) { fail(cb, "Nema access token (prijava)."); return; }
        if (!isUuid(uid)) { fail(cb, "Neispravan userId (UUID)."); return; }
        if (limit <= 0) limit = 50;

        Map<String, String> q = new HashMap<>();
        q.put("select", selectCols());

        q.put("or", "(" + COL_SENDER + ".eq." + uid + "," + COL_RECEIVER + ".eq." + uid + ")");

        q.put("order", COL_CREATED + ".desc");
        q.put("limit", String.valueOf(limit));

        client.get(TABLE, q, token, cb);
    }

    public void listConversation(String accessToken,
                                 String userId,
                                 String otherUserId,
                                 String adId,
                                 int limit,
                                 Callback cb) {

        String token = accessToken == null ? "" : accessToken.trim();
        String uid = safeId(userId);
        String other = safeId(otherUserId);
        String aid = safeId(adId);

        if (token.isEmpty()) { fail(cb, "Nema access token (prijava)."); return; }
        if (!isUuid(uid)) { fail(cb, "Neispravan userId (UUID)."); return; }
        if (!isUuid(other)) { fail(cb, "Neispravan otherUserId (UUID)."); return; }
        if (!isUuid(aid)) { fail(cb, "Neispravan adId (UUID)."); return; }
        if (limit <= 0) limit = 200;

        Map<String, String> q = new HashMap<>();
        q.put("select", selectCols());

        String a = "and(" + COL_SENDER + ".eq." + uid + "," + COL_RECEIVER + ".eq." + other + ")";
        String b = "and(" + COL_SENDER + ".eq." + other + "," + COL_RECEIVER + ".eq." + uid + ")";
        q.put("or", "(" + a + "," + b + ")");

        q.put(COL_AD, "eq." + aid);
        q.put("order", COL_CREATED + ".asc");
        q.put("limit", String.valueOf(limit));

        client.get(TABLE, q, token, cb);
    }
    public void sendMessage(String accessToken,
                            String senderId,
                            String receiverId,
                            String adId,
                            String content,
                            Callback cb) {

        String token = accessToken == null ? "" : accessToken.trim();
        String sender = safeId(senderId);
        String receiver = safeId(receiverId);
        String aid = safeId(adId);
        String text = content == null ? "" : content.trim();

        if (token.isEmpty()) { fail(cb, "Nema access token (prijava)."); return; }
        if (!isUuid(sender)) { fail(cb, "Neispravan senderId (UUID)."); return; }
        if (!isUuid(receiver)) { fail(cb, "Neispravan receiverId (UUID)."); return; }
        if (!isUuid(aid)) { fail(cb, "Neispravan adId (UUID)."); return; }
        if (text.isEmpty()) { fail(cb, "Poruka je prazna."); return; }

        try {
            JSONObject body = new JSONObject();
            body.put(COL_SENDER, sender);
            body.put(COL_RECEIVER, receiver);
            body.put(COL_AD, aid);
            body.put(COL_CONTENT, text);

            client.post(TABLE, body.toString(), token, cb);
        } catch (Exception e) {
            fail(cb, "Greška pri kreiranju JSON poruke: " + e.getMessage());
        }
    }
}