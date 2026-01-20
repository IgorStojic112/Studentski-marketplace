package ba.sum.fsre.studentskimarketplace.data.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import java.util.ArrayList;
import java.util.List;

import ba.sum.fsre.studentskimarketplace.data.model.Ad;
import ba.sum.fsre.studentskimarketplace.data.network.SupabaseRestClient;
import ba.sum.fsre.studentskimarketplace.data.repository.AdsRepository;

public class AdsViewModel extends ViewModel {

    private final AdsRepository repository;
    private final MutableLiveData<List<Ad>> ads = new MutableLiveData<>();

    public AdsViewModel(){
        SupabaseRestClient client = new SupabaseRestClient();
        repository = new AdsRepository(client);
    }

    public LiveData<List<Ad> > getAds(){
        return ads;
    }

    public void searchAds(String title, String faculty){


        List<Ad> testAds = new ArrayList<>();

        testAds.add(new Ad("1", "Bike for sale", "FESB", 150));
        testAds.add(new Ad("2", "Used laptop", "FSRE", 500));
        testAds.add(new Ad("3", "Room for rent", "EFZG", 300));

        ads.setValue(testAds);
    }





}
