package ba.sum.fsre.studentskimarketplace.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.ChatMessage;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final String myUserId;
    private final List<ChatMessage> items = new ArrayList<>();

    public ConversationAdapter(String myUserId) {
        this.myUserId = myUserId;
        setHasStableIds(true);
    }

    public void setItems(List<ChatMessage> msgs) {
        items.clear();
        if (msgs != null) items.addAll(msgs);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        ChatMessage m = items.get(position);
        String key = safe(m != null ? m.getId() : null);
        if (key.isEmpty()) {
            // fallback stable-ish id
            key = safe(m != null ? m.getCreatedAt() : null) + "|" + safe(m != null ? m.getSenderId() : null) + "|" + position;
        }
        return key.hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = items.get(position);
        String senderId = safe(m != null ? m.getSenderId() : null);

        boolean isSent = myUserId != null && !myUserId.trim().isEmpty()
                && senderId.equals(myUserId);

        return isSent ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_SENT)
                ? R.layout.item_message_sent
                : R.layout.item_message_received;

        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatMessage m = items.get(position);
        h.textMessage.setText(m != null ? safe(m.getContent()) : "");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView textMessage;

        VH(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage); // mora postojati u oba layouta
        }
    }
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}