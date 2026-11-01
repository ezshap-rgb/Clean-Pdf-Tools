package com.cleanpdftools.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.CleanPdfToolsTheme
import com.cleanpdftools.app.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CleanPdfToolsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.Background
                ) {
                    MainScreen()
                }
            }
        }
    }
}
