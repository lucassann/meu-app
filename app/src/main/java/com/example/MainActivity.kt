package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    if (FirebaseApp.getApps(this).isEmpty()) {
        val options = FirebaseOptions.Builder()
            .setProjectId("cinepremiumap")
            .setApplicationId("1:949049110824:android:4c5781cfe6fb78166da01d")
            .setApiKey("AIzaSyAzzVP1wMQESGBpLrKuqWxYVodPmtn8t88")
            .build()
        FirebaseApp.initializeApp(this, options)
    }

    setContent {
      MyApplicationTheme {
        HomeScreen(modifier = Modifier.fillMaxSize())
      }
    }
  }
}
