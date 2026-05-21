package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.ListingAssistantApp
import com.example.ui.ListingAssistantViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge layouts as required by style guides
        enableEdgeToEdge()
        
        // Instantiate our State ViewModel provider
        val viewModel = ViewModelProvider(this)[ListingAssistantViewModel::class.java]
        
        setContent {
            MyApplicationTheme {
                ListingAssistantApp(viewModel = viewModel)
            }
        }
    }
}
