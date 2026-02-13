package ba.sum.fsre.studentskimarketplace.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import ba.sum.fsre.studentskimarketplace.R;

public class ExistingImagesAdapter extends RecyclerView.Adapter<ExistingImagesAdapter.VH> {

    public interface OnRemoveUrl {
        void onRemove(String url);
    }
    private final Context ctx;
    private final OnRemoveUrl onRemove;
    private final List<String> items = new ArrayList<>();

    public ExistingImagesAdapter(Context ctx, OnRemoveUrl onRemove) {
        this.ctx = ctx;
        this.onRemove = onRemove;
    }

    public void setItems(List<String> urls) {
        items.clear();
        if (urls != null) items.addAll(urls);
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_preview, parent, false);
        return new VH(v);
    }
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String url = items.get(position);

        Glide.with(h.itemView.getContext()).clear(h.iv);
        h.iv.setImageDrawable(null);

        if (url != null && !url.trim().isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(url.trim())
                    .centerCrop()
                    .into(h.iv);
        }
        h.btnRemove.setOnClickListener(v -> {
            if (onRemove != null) onRemove.onRemove(url);
        });
    }
    @Override
    public int getItemCount() { return items.size(); }
    static class VH extends RecyclerView.ViewHolder {
        ImageView iv;
        ImageButton btnRemove;
        VH(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.ivPreview);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}