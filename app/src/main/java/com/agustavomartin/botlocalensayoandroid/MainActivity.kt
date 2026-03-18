package com.agustavomartin.botlocalensayoandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.agustavomartin.botlocalensayoandroid.ui.AppContainer
import com.agustavomartin.botlocalensayoandroid.ui.BotLocalEnsayoApp
import com.agustavomartin.botlocalensayoandroid.ui.theme.BotLocalEnsayoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initialize(applicationContext)
        AppContainer.handleLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            BotLocalEnsayoTheme {
                BotLocalEnsayoApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AppContainer.handleLaunchIntent(intent)
    }
}
