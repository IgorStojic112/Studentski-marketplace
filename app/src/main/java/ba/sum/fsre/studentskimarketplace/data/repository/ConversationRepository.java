package ba.sum.fsre.studentskimarketplace.data.repository;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Callback;

import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;

public class ConversationRepository {

    private final SupabaseRestClient client;


    public ConversationRepository(SupabaseRestClient client) { this.client = client;}

    public void getConversation (String user1, String user2, String accessToken, Callback callback){
        Map<String,String> query = new HashMap<>();
        query.put("or",
                "(user1_id.eq." + user1 + ",user2_id.eq." + user2 + ")," +
                "(user1_id.eq." + user2 + ",user2_id.eq." + user1 + ")"
        );
        client.get("conversations",query,accessToken, callback);
    }

    public void createConversation (String user1,String user2, String accessToken, Callback callback) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("user1_id", user1);
        json.put("user2_id", user2);

        client.post("conversations",json.toString(),accessToken,callback);
    }


}
