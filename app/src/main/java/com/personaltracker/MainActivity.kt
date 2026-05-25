package com.personaltracker

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.personaltracker.ui.navigation.SuryaWorldNavGraph
import com.personaltracker.ui.theme.SuryaWorldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Prevent screenshots on secure screens (set per-screen in compose)
        setContent {
            SuryaWorldTheme {
                SuryaWorldNavGraph()
            }
        }
    }
}
