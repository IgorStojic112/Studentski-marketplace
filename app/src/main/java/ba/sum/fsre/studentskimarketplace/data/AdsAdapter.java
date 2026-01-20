package ba.sum.fsre.studentskimarketplace.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.model.Ad;

public class AdsAdapter extends RecyclerView.Adapter<AdsAdapter.AdViewHolder>{

    private List<Ad> ads = new ArrayList<>();

    public void setAds(List<Ad> ads){
        this.ads = ads;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ad,parent,false);
        return new AdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdViewHolder holder, int position){
        Ad ad = ads.get(position);
        holder.title.setText(ad.getTitle());
        holder.faculty.setText(ad.getFaculty());
        holder.price.setText(String.valueOf(ad.getPrice()));
    }


    @Override
    public int getItemCount(){
        return ads.size();
    }

    public static class AdViewHolder extends RecyclerView.ViewHolder{
        TextView title, faculty, price;

        AdViewHolder(View itemView){
            super(itemView);
            title = itemView.findViewById(R.id.title);
            faculty = itemView.findViewById(R.id.faculty);
            price = itemView.findViewById(R.id.price);
        }
    }
}
