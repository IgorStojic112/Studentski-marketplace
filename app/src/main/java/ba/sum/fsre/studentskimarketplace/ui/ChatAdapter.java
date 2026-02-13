package ba.sum.fsre.studentskimarketplace.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.ChatRow;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    public interface OnClick { void onChatClick(String otherUserId, String adId, String adTitle); }

    private OnClick onClick;
    public void setOnClick(OnClick c) { this.onClick = c; }

    private final List<ChatRow> items = new ArrayList<>();

    public ChatAdapter() { setHasStableIds(true); }

    public void setItems(List<ChatRow> rows) {
        items.clear();
        if (rows != null) items.addAll(rows);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        ChatRow r = items.get(position);
        String key = safe(r != null ? r.otherUserId : null, "") + "|" + safe(r != null ? r.adId : null, "");
        return key.hashCode();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatRow r = items.get(position);

        h.tvOtherUser.setText(safe(r != null ? r.otherUserName : null, "Korisnik"));
        h.tvAdTitle.setText(safe(r != null ? r.adTitle : null, "Oglas"));     // ✅ tvAdTitle
        h.tvLastMessage.setText(safe(r != null ? r.lastMessage : null, ""));
        h.tvTime.setText(relTime(r != null ? r.createdAt : null));

        h.itemView.setOnClickListener(v -> {
            if (onClick == null || r == null) return;

            String ou = safe(r.otherUserId, "").trim();
            String aid = safe(r.adId, "").trim();
            if (ou.isEmpty() || aid.isEmpty()) return;

            onClick.onChatClick(ou, aid, safe(r.adTitle, "Oglas"));
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOtherUser, tvAdTitle, tvLastMessage, tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvOtherUser = itemView.findViewById(R.id.tvOtherUser);
            tvAdTitle = itemView.findViewById(R.id.tvAdTitle);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }

    private String safe(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    private String relTime(String createdAt) {
        long ts = parseIso(createdAt);
        if (ts <= 0) return "";
        long diff = System.currentTimeMillis() - ts;
        if (diff < 0) diff = 0;

        long min = diff / 60000L;
        if (min < 1) return "upravo";
        if (min < 60) return "prije " + min + " min";
        long h = min / 60;
        if (h < 24) return "prije " + h + " h";
        long d = h / 24;
        if (d < 7) return "prije " + d + " dana";
        long w = d / 7;
        return "prije " + w + " tj.";
    }

    private long parseIso(String s) {
        if (s == null) return -1;
        String t = s.trim();
        if (t.isEmpty()) return -1;

        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
        };

        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf.parse(t).getTime();
            } catch (ParseException ignore) {}
        }
        return -1;
    }
}