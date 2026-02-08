package ba.sum.fsre.studentskimarketplace;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private EditText messageInput;
    private ImageButton sendButton;
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        recyclerView = findViewById(R.id.chatRecyclerView);

        messages = new ArrayList<>();
        adapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchMessagesFromSupabase();

        sendButton.setOnClickListener(v -> {
            String msgText = messageInput.getText().toString().trim();
            if (!msgText.isEmpty()) {
                sendMessageToSupabase(msgText);
            }
        });
    }

    private void fetchMessagesFromSupabase() {
    }

    private void sendMessageToSupabase(String text) {


        String myId = "trenutni_user_uuid";
        String targetId = "uuid_primatelja";

        ChatMessage newMessage = new ChatMessage(myId, targetId, text);


        messageInput.setText("");

        messages.add(newMessage);
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);

        Toast.makeText(this, "Poruka poslana u Supabase", Toast.LENGTH_SHORT).show();
    }
}