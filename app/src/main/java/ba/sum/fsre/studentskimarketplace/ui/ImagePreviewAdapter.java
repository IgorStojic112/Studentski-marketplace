package ba.sum.fsre.studentskimarketplace.ui;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ba.sum.fsre.studentskimarketplace.R;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.VH> {

    public interface OnRemoveClick {
        void onRemove(Uri uri);
    }

    private final Context ctx;
    private final OnRemoveClick onRemoveClick;
    private final List<Uri> items = new ArrayList<>();

    public ImagePreviewAdapter(Context ctx, OnRemoveClick onRemoveClick) {
        this.ctx = ctx;
        this.onRemoveClick = onRemoveClick;
    }

    public void setItems(List<Uri> uris) {
        items.clear();
        if (uris != null) items.addAll(uris);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_preview, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Uri uri = items.get(position);
        h.iv.setImageURI(uri);
        h.btnRemove.setOnClickListener(v -> {
            if (onRemoveClick != null) onRemoveClick.onRemove(uri);
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