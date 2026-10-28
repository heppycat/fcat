package io.github.heppycat.fcat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.heppycat.fcat.ui.FirstPageScreen
import io.github.heppycat.fcat.ui.theme.FcatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FcatTheme {
                FirstPageScreen()
            }
        }
    }
}
