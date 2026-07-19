package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.myapplication.ui.compose.MainNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support for drawing behind the transparent navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MainNavigation()
        }
    }
}
