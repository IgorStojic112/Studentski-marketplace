package ba.sum.fsre.studentskimarketplace.data.repository;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import okhttp3.Callback;


public class MessageRepository {

    private final SupabaseRestClient client;

    public MessageRepository(SupabaseRestClient client) { this.client = client;}

    public void getMessages (String conversation_id, String accessToken, Callback callback){
        Map<String, String> query = new HashMap<>();
        query.put("conversation_id","eq." + conversation_id);
        query.put("order", "created_at.asc");

        client.get("message", query,accessToken,callback);
    }

    public void createMessages (String conversation_id, String sender_id , String content ,String accessToken, Callback callback) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("conversation_id",conversation_id);
        json.put("sender_id", sender_id);
        json.put("content",content);

        client.post("message",json.toString(),accessToken,callback);
    }


}
