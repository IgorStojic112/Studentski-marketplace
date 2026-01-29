package ba.sum.fsre.studentskimarketplace.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ba.sum.fsre.studentskimarketplace.ChatActivity;
import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.Ad;
import ba.sum.fsre.studentskimarketplace.data.repository.FavoritesRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AdsAdapter extends RecyclerView.Adapter<AdsAdapter.VH> {
    private static final String TAG = "ADS_ADAPTER";
    public interface OnAdActionsListener {
        void onEdit(Ad ad);
    }
    private OnAdActionsListener actionsListener;
    public void setOnAdActionsListener(OnAdActionsListener l) {
        this.actionsListener = l;
    }
    private final List<Ad> items = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();
    private final FavoritesRepository favoritesRepository;
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
        h.tvFaculty.setText("Fakultet: " + safeText(ad != null ? ad.getFaculty() : null, "-"));
        h.tvPrice.setText(formatPriceKm(ad != null ? ad.getPrice() : null));

        boolean isFav = (adId != null) && favoriteIds.contains(adId);
        setStarIcon(h.btnFavorite, isFav);
        boolean isMine = AuthSession.isLoggedIn()
                && safeId(AuthSession.userId) != null
                && ownerId != null
                && AuthSession.userId.equals(ownerId);

        if (h.btnEdit != null) {
            h.btnEdit.setVisibility(isMine ? View.VISIBLE : View.GONE);

            h.btnEdit.setOnClickListener(v -> {
                Context ctx = v.getContext();

                if (!isMine) {
                    toast(ctx, "Možeš uređivati samo svoje oglase.");
                    return;
                }
                if (adId == null) {
                    toast(ctx, "Nedostaje ID oglasa.");
                    return;
                }
                if (actionsListener != null) actionsListener.onEdit(ad);
            });
        }

        h.itemView.setOnClickListener(v -> {
            Context ctx = v.getContext();

            if (!AuthSession.isLoggedIn()) {
                toast(ctx, "Prvo se prijavi.");
                return;
            }
            if (ownerId == null) {
                toast(ctx, "Oglas nema user_id.");
                return;
            }
            if (safeId(AuthSession.userId) != null && AuthSession.userId.equals(ownerId)) {
                return;
            }

            Intent i = new Intent(ctx, ChatActivity.class);
            i.putExtra("otherUserId", ownerId);
            i.putExtra("adTitle", ad != null ? ad.getTitle() : "");
            ctx.startActivity(i);
        });
        h.btnFavorite.setOnClickListener(v -> {
            Context ctx = v.getContext();

            if (adId == null) {
                toast(ctx, "Nedostaje ID oglasa.");
                return;
            }
            if (!AuthSession.isLoggedIn()) {
                toast(ctx, "Za favorite je potreban login.");
                return;
            }
            boolean nowFav = !favoriteIds.contains(adId);

            if (nowFav) favoriteIds.add(adId); else favoriteIds.remove(adId);
            moveFavoritesToTop();
            notifyDataSetChanged();

            if (favoritesRepository == null) return;
            if (nowFav) {
                favoritesRepository.addFavorite(AuthSession.accessToken, AuthSession.userId, adId, new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        rollback(ctx, adId, false);
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) rollback(ctx, adId, false);
                    }
                });
            } else {
                favoritesRepository.removeFavorite(AuthSession.accessToken, AuthSession.userId, adId, new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        rollback(ctx, adId, true);
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) rollback(ctx, adId, true);
                    }
                });
            }
        });
    }
    private void rollback(Context ctx, String adId, boolean shouldBeFav) {
        if (shouldBeFav) favoriteIds.add(adId);
        else favoriteIds.remove(adId);

        moveFavoritesToTop();
        notifyDataSetChanged();
        toast(ctx, "Greška, vraćam stanje favorita.");
    }
    private void setStarIcon(ImageButton btn, boolean isFav) {
        btn.setImageResource(isFav
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
    }
    private void toast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }
    private String safeId(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || "null".equalsIgnoreCase(t)) return null;
        return t;
    }
    private String safeText(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        if (t.isEmpty() || "null".equalsIgnoreCase(t)) return fallback;
        return t;
    }
    private String formatPriceKm(Double price) {
        if (price == null) return "-";
        if (price % 1 == 0) return ((int) Math.round(price)) + " KM";
        return price + " KM";
    }
    @Override
    public int getItemCount() { return items.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvFaculty, tvPrice;
        ImageButton btnFavorite, btnEdit;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvFaculty = itemView.findViewById(R.id.tvFaculty);
            tvPrice = itemView.findViewById(R.id.tvPrice);

            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnEdit = itemView.findViewById(R.id.btnEdit); // mora postojati u item_ad.xml
        }
    }
}








