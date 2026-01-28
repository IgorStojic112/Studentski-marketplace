package ba.sum.fsre.studentskimarketplace.ui;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
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

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.Ad;
import ba.sum.fsre.studentskimarketplace.data.repository.FavoritesRepository;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AdsAdapter extends RecyclerView.Adapter<AdsAdapter.VH> {

    private static final String TAG = "ADS_ADAPTER";

    private final List<Ad> items = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();
    private final FavoritesRepository favoritesRepository;
    public AdsAdapter(FavoritesRepository favoritesRepository) {
        this.favoritesRepository = favoritesRepository;
    }
    public void setItems(List<Ad> newItems) {
        items.clear();
        favoriteIds.clear();

        if (newItems != null) {
            for(Ad ad : newItems){
                items.add(ad);
                if(ad.getId() != null){
                    favoriteIds.add(ad.getId());
                }
            }
        }
        notifyDataSetChanged();
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

        String adId = ad.getId();

        h.tvTitle.setText(safeText(ad.getTitle(), ""));
        h.tvDescription.setText(safeText(ad.getDescription(), ""));
        h.tvFaculty.setText("Fakultet: " + safeText(ad.getFaculty(), "-"));
        h.tvPrice.setText(formatPriceKm(ad.getPrice()));

        boolean isFav = (adId != null) && favoriteIds.contains(adId);
        setStarIcon(h.btnFavorite, isFav);

        h.btnFavorite.setOnClickListener(v -> {
            Context ctx = v.getContext();

            if (adId == null || adId.trim().isEmpty()) {
                toast(ctx, "Greška");
                return;
            }

            if (!AuthSession.isLoggedIn()) {
                toast(ctx, "Za favorite je potreban login.");
                return;
            }

            if (favoritesRepository == null) {
                boolean nowFav = !favoriteIds.contains(adId);
                if (nowFav) favoriteIds.add(adId); else favoriteIds.remove(adId);
                setStarIcon(h.btnFavorite, nowFav);
                toast(ctx, "");
                return;
            }
            boolean nowFav = !favoriteIds.contains(adId);

            if (nowFav) favoriteIds.add(adId); else favoriteIds.remove(adId);
            setStarIcon(h.btnFavorite, nowFav);

            if (nowFav) {
                favoritesRepository.addFavorite(AuthSession.accessToken, AuthSession.userId, adId, new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Add favorite failed", e);
                        rollbackFavorite(h, adId, false);
                        toast(ctx, "Neuspjelo spremanje favorita.");
                    }

                    @Override public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            String body = response.body() != null ? response.body().string() : "";
                            Log.e(TAG, "Add favorite HTTP " + response.code() + " " + body);
                            rollbackFavorite(h, adId, false);
                            toast(ctx, "Greška" + response.code());
                        } else {
                            Log.d(TAG, "Added favorite OK");
                        }
                    }
                });
            } else {
                favoritesRepository.removeFavorite(AuthSession.accessToken, AuthSession.userId, adId, new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Remove favorite failed", e);
                        rollbackFavorite(h, adId, true);
                        toast(ctx, "Neuspjelo brisanje favorita.");
                    }

                    @Override public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            String body = response.body() != null ? response.body().string() : "";
                            Log.e(TAG, "Remove favorite HTTP " + response.code() + " " + body);
                            rollbackFavorite(h, adId, true);
                            toast(ctx, "Greška: HTTP " + response.code());
                        } else {
                            Log.d(TAG, "Removed favorite OK");
                        }
                    }
                });
            }
        });
    }

    private void rollbackFavorite(VH h, String adId, boolean shouldBeFav) {
        if (shouldBeFav) favoriteIds.add(adId);
        else favoriteIds.remove(adId);
        h.itemView.post(() -> setStarIcon(h.btnFavorite, shouldBeFav));
    }

    private void setStarIcon(ImageButton btn, boolean isFav) {
        btn.setImageResource(isFav
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
    }
    private void toast(Context ctx, String msg) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(() ->
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            );
        } else {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }
    }
    private String safeText(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("null")) return fallback;
        return t;
    }
    private String formatPriceKm(Double price) {
        if (price == null) return "-";
        if (price % 1 == 0) return ((int) Math.round(price)) + " KM";
        return price + " KM";
    }
    @Override
    public int getItemCount() {
        return items.size();
    }
    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvFaculty, tvPrice;
        ImageButton btnFavorite;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvFaculty = itemView.findViewById(R.id.tvFaculty);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}


