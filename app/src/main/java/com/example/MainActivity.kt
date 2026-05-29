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
    
    try {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    setContent {
      MyApplicationTheme {
        HomeScreen(modifier = Modifier.fillMaxSize())
      }
    }
  }
}
