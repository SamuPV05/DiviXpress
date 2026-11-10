package com.samuel.retodivisas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.samuel.retodivisas.navigation.NavGraph
import com.samuel.retodivisas.ui.theme.RetoDivisasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetoDivisasTheme { NavGraph() }
        }
    }
}
