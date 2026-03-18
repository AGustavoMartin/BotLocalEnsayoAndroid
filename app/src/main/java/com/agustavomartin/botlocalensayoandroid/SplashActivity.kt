package com.agustavomartin.botlocalensayoandroid

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashImage = ImageView(this).apply {
            setBackgroundColor(Color.BLACK)
            setImageResource(R.drawable.splash_screen)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
        setContentView(splashImage)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtras(intent ?: Intent())
            })
            finish()
        }, 1600L)
    }
}
