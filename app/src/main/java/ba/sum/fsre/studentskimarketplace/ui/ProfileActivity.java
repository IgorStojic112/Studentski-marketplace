package ba.sum.fsre.studentskimarketplace.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import ba.sum.fsre.studentskimarketplace.LoginActivity;
import ba.sum.fsre.studentskimarketplace.R;
import ba.sum.fsre.studentskimarketplace.data.session.AuthSession;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserId;
    private Button btnLogout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        btnLogout = findViewById(R.id.btnLogout);

        if (!AuthSession.isLoggedIn()){
            Toast.makeText(this, "Prijevate se prvo!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnLogout.setOnClickListener(v ->{
            AuthSession.accessToken = null;
            AuthSession.userId = null;
            Toast.makeText(this,"Odjavljeni ste", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if(id == R.id.nav_favorites){
                startActivity(new Intent(this,FavoriteActivity.class));
                finish();
            } else if (id == R.id.nav_search){
                startActivity(new Intent(this,SearchActivity.class));
                finish();
            }else if (id == R.id.nav_chat){
                startActivity(new Intent(this,ChatActivity.class));
                finish();
            } else if (id == R.id.nav_profile){
                return true;
            }
            return true;
        });


    }


}
