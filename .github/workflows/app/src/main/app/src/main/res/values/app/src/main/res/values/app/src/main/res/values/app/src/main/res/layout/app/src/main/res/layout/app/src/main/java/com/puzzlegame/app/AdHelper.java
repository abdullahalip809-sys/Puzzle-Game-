package com.puzzlegame.app;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdHelper {

    // Google के TEST ad unit IDs। AdMob से असली IDs मिलने पर यहीं बदलें।
    public static final String BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

    private static InterstitialAd interstitial;

    public static void loadBanner(AdView adView) {
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(BANNER_ID);
        adView.loadAd(new AdRequest.Builder().build());
    }

    public static void loadInterstitial(Context context) {
        InterstitialAd.load(context, INTERSTITIAL_ID, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitial = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        interstitial = null;
                    }
                });
    }

    /** Ad तैयार हो तो full-screen ad दिखाता है, फिर next चलाता है। Ad न हो तो सीधे next। */
    public static void showNow(final Activity activity, final Runnable next) {
        if (interstitial == null) {
            loadInterstitial(activity);
            next.run();
            return;
        }
        interstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitial = null;
                loadInterstitial(activity);
                next.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                interstitial = null;
                loadInterstitial(activity);
                next.run();
            }
        });
        interstitial.show(activity);
    }
}
