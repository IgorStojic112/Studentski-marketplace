package ba.sum.fsre.studentskimarketplace.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.Ad;
import ba.sum.fsre.studentskimarketplace.data.repository.FavoritesRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AdsAdapter extends RecyclerView.Adapter<AdsAdapter.VH> {

    public interface OnAdActionsListener { void onEdit(Ad ad); }
    private OnAdActionsListener actionsListener;
    public void setOnAdActionsListener(OnAdActionsListener l) { this.actionsListener = l; }

    public interface OnMessageClickListener { void onMessageClick(Ad ad); }
    private OnMessageClickListener onMessageClickListener;
    public void setOnMessageClickListener(OnMessageClickListener l) { this.onMessageClickListener = l; }

    private final List<Ad> items = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();
    private final FavoritesRepository favoritesRepository;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AdsAdapter(FavoritesRepository favoritesRepository) {
        this.favoritesRepository = favoritesRepository;
    }

    public void setItems(List<Ad> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        moveFavoritesToTop();
        notifyDataSetChanged();
    }
    public void setFavoriteIds(Set<String> ids) {
        favoriteIds.clear();
        if (ids != null) favoriteIds.addAll(ids);
        moveFavoritesToTop();
        notifyDataSetChanged();
    }
    private void moveFavoritesToTop() {
        if (items.isEmpty()) return;
        List<Ad> fav = new ArrayList<>();
        List<Ad> rest = new ArrayList<>();
        for (Ad a : items) {
            String id = safeId(a != null ? a.getId() : null);
            if (id != null && favoriteIds.contains(id)) fav.add(a);
            else rest.add(a);
        }
        items.clear();
        items.addAll(fav);
        items.addAll(rest);
    }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ad, parent, false);
        return new VH(v);
    }
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Ad ad = items.get(position);

        String adId = safeId(ad != null ? ad.getId() : null);
        String ownerId = safeId(ad != null ? ad.getUser_id() : null);

        h.tvTitle.setText(safeText(ad != null ? ad.getTitle() : null, ""));
        h.tvDescription.setText(safeText(ad != null ? ad.getDescription() : null, ""));
        h.tvPrice.setText(formatPriceKm(ad != null ? ad.getPrice() : null));
        h.tvTime.setText(formatRelativeTime(ad != null ? ad.getCreated_at() : null));

        boolean isFav = adId != null && favoriteIds.contains(adId);
        setStarIcon(h.btnFavorite, isFav);

        boolean isMine = AuthSession.isLoggedIn()
                && safeId(AuthSession.userId) != null
                && ownerId != null
                && AuthSession.userId.equals(ownerId);

        h.btnEdit.setVisibility(isMine ? View.VISIBLE : View.GONE);
        h.btnEdit.setOnClickListener(v -> {
            if (!isMine) {
                Toast.makeText(v.getContext(), "Možeš uređivati samo svoje oglase.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (actionsListener != null) actionsListener.onEdit(ad);
        });
        h.btnMessage.setOnClickListener(v -> {
            Context ctx = v.getContext();

            if (onMessageClickListener != null) {
                onMessageClickListener.onMessageClick(ad);
                return;
            }
            if (!AuthSession.isLoggedIn()) {
                Toast.makeText(ctx, "Prvo se prijavi.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ownerId == null || adId == null) return;
            if (AuthSession.userId.equals(ownerId)) {
                Toast.makeText(ctx, "Ne možeš poslati poruku sebi.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(ctx, ConversationActivity.class);
            i.putExtra("otherUserId", ownerId);
            i.putExtra("adId", adId);
            i.putExtra("adTitle", ad != null ? ad.getTitle() : "Oglas");
            ctx.startActivity(i);
        });
        h.btnFavorite.setOnClickListener(v -> toggleFavorite(v.getContext(), adId));

        String imageUrl = (ad != null) ? ad.getImageUrl() : null;

        Glide.with(h.itemView.getContext()).clear(h.ivAdImage);
        h.ivAdImage.setImageDrawable(null);

        h.tvTitle.setTextSize(18);
        h.tvDescription.setMaxLines(3);

        if (imageUrl != null) imageUrl = imageUrl.trim();

        boolean hasImage = imageUrl != null
                && !imageUrl.isEmpty()
                && !"null".equalsIgnoreCase(imageUrl);

        if (hasImage) {

            h.ivAdImage.setVisibility(View.VISIBLE);

            ViewGroup.LayoutParams params = h.ivAdImage.getLayoutParams();
            params.height = (int) (150 * h.itemView.getResources()
                    .getDisplayMetrics().density);
            h.ivAdImage.setLayoutParams(params);

            Glide.with(h.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .into(h.ivAdImage);

        } else {

            h.ivAdImage.setVisibility(View.GONE);

            h.tvTitle.setTextSize(19);
            h.tvDescription.setMaxLines(5);
        }

        h.itemView.setOnClickListener(null);
    }
    private void toggleFavorite(Context ctx, String adId) {
        if (adId == null) return;
        if (!AuthSession.isLoggedIn()) {
            Toast.makeText(ctx, "Login potreban.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean nowFav = !favoriteIds.contains(adId);
        if (nowFav) favoriteIds.add(adId);
        else favoriteIds.remove(adId);

        moveFavoritesToTop();
        notifyDataSetChanged();

        if (favoritesRepository == null) return;

        Callback cb = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                rollback(ctx, adId, !nowFav);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) return;
                String err = response.body() != null ? response.body().string() : "";
                int code = response.code();
                if (nowFav && code == 409) return;
                rollback(ctx, adId, !nowFav);
                android.util.Log.d("FAV_ERR", "code=" + code + " body=" + err);
            }
        };

        if (nowFav) {
            favoritesRepository.addFavorite(AuthSession.accessToken, AuthSession.userId, adId, cb);
        } else {
            favoritesRepository.removeFavorite(AuthSession.accessToken, AuthSession.userId, adId, cb);
        }
    }

    private void rollback(Context ctx, String adId, boolean shouldBeFav) {
        mainHandler.post(() -> {
            if (shouldBeFav) favoriteIds.add(adId);
            else favoriteIds.remove(adId);

            moveFavoritesToTop();
            notifyDataSetChanged();

            Toast.makeText(ctx, "Greška", Toast.LENGTH_SHORT).show();
        });
    }
    private void setStarIcon(ImageButton btn, boolean isFav) {
        btn.setImageResource(isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
    }

    private String safeId(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }

    private String safeText(String s, String fallback) {
        if (s == null) return fallback;
        s = s.trim();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? fallback : s;
    }

    private String formatPriceKm(Double price) {
        if (price == null) return "-";
        if (price % 1 == 0) return ((int) Math.round(price)) + " KM";
        return price + " KM";
    }
    private String formatRelativeTime(String createdAt) {
        long ts = parseIsoToMillis(createdAt);
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
    private long parseIsoToMillis(String s) {
        if (s == null) return -1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(s).getTime();
        } catch (Exception e) { return -1; }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvPrice, tvDescription;
        ImageButton btnFavorite, btnEdit, btnMessage;
        ImageView ivAdImage;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnMessage = itemView.findViewById(R.id.btnMessage);
            ivAdImage = itemView.findViewById(R.id.ivAdImage);
        }
    }
}