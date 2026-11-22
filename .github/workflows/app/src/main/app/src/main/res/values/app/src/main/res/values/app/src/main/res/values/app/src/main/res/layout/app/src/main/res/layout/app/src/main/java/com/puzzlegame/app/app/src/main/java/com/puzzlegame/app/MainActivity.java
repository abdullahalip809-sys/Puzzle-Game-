package com.puzzlegame.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, status -> { });
        AdHelper.loadBanner((AdView) findViewById(R.id.bannerAd));
        AdHelper.loadInterstitial(this);

        findViewById(R.id.btnEasy).setOnClickListener(v -> start(3));
        findViewById(R.id.btnMedium).setOnClickListener(v -> start(4));
        findViewById(R.id.btnHard).setOnClickListener(v -> start(5));
    }

    private void start(int size) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra("size", size);
        startActivity(i);
    }
}
