package ba.sum.fsre.studentskimarketplace.data.activity;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.AdsAdapter;
import ba.sum.fsre.studentskimarketplace.data.viewModel.AdsViewModel;

public class AdsActivity extends AppCompatActivity {

    private AdsViewModel viewModel;
    private AdsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ads);

        Log.d("ADS_DEBUG", "AdsActivity CREATED");

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdsAdapter();
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AdsViewModel.class);

        viewModel.getAds().observe(this,ads -> {
            adapter.setAds(ads);
        });

        viewModel.searchAds(null,null);


    }

}
