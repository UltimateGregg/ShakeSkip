package com.shakeskip.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.shakeskip.player.ui.ShakeSkipNavHost
import com.shakeskip.player.ui.theme.ShakeSkipTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShakeSkipTheme {
                val navController = rememberNavController()

                Surface(modifier = Modifier.fillMaxSize()) {
                    ShakeSkipNavHost(navController = navController)
                }
            }
        }
    }
}
