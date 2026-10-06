package com.iqbox.app.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * AdManager - Manages AdMob ads for IQBox
 * Uses test ad unit IDs. Replace with real IDs from AdMob console before publishing.
 */
object AdManager {
    
    private const val TAG = "AdManager"
    
    // Test Ad Unit IDs (replace with real ones from AdMob)
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    
    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false
    
    /**
     * Initialize the Mobile Ads SDK. Call once in Application or MainActivity.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: $initializationStatus")
            isInitialized = true
        }
    }
    
    /**
     * Load an interstitial ad. Should be called ahead of time so it's ready when needed.
     */
    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }
    
    /**
     * Show the interstitial ad if loaded.
     * @return true if ad was shown, false if not available
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}): Boolean {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity) // Preload next one
                    onDismissed()
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    onDismissed()
                }
            }
            ad.show(activity)
            return true
        } else {
            onDismissed()
            return false
        }
    }
    
    /**
     * Check if interstitial is ready
     */
    fun isInterstitialReady(): Boolean = interstitialAd != null
    
    /**
     * Create a banner AdRequest
     */
    fun createAdRequest(): AdRequest = AdRequest.Builder().build()
}
