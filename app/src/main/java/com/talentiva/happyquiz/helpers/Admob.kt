package com.talentiva.happyquiz.helpers

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {
    private var rewardedAd: RewardedAd? = null

    fun loadAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, "YOUR_UNIT_ID", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                Log.d("AdMob", "Rewarded ad loaded.")

            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e("AdMob", "Failed to load rewarded ad: $error")
                rewardedAd = null
            }
        })
    }

    fun showAd(activity: Activity, onUserEarnedReward: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadAd(activity) // reload iklan setelah ditutup
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d("AdMob", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onUserEarnedReward()
            }
        } ?: run {
            Log.d("AdMob", "Iklan belum siap, tampilkan langsung penjelasan.")
            onUserEarnedReward() // Fallback jika iklan belum siap
        }
    }

}
