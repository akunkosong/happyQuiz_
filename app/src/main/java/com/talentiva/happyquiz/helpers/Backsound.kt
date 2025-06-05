package com.talentiva.happyquiz.helpers


import android.content.Context
import android.media.MediaPlayer
import com.talentiva.happyquiz.R

object MediaPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

    fun startMusic(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.backsound)
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.start()
    }

    fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
