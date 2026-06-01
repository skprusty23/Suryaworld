package com.personaltracker.ui.screens.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.personaltracker.ui.components.PTTopBar

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body { font-family: sans-serif; padding: 20px; color: #1a1a2e; line-height: 1.6; }
          h1 { color: #0d47a1; font-size: 22px; }
          h2 { color: #1565c0; font-size: 16px; margin-top: 20px; }
          p  { font-size: 14px; margin: 8px 0; }
          .badge { background: #e3f2fd; border-radius: 8px; padding: 4px 10px; display: inline-block; font-size: 13px; margin: 4px 2px; }
          .footer { margin-top: 32px; padding-top: 12px; border-top: 1px solid #ccc; font-size: 12px; color: #777; }
        </style>
        </head>
        <body>
        <h1>Welcome to WealthHub</h1>
        <p>WealthHub is a secure personal management application designed to help users organize
        important information, credentials, financial records, documents, reminders, notes,
        and daily activities in one centralized location.</p>
        <p>The application is built with a strong focus on <strong>privacy</strong>,
        <strong>security</strong>, <strong>usability</strong>, and <strong>offline
        accessibility</strong>.</p>

        <h2>Key Features</h2>
        <span class="badge">📄 Documents</span>
        <span class="badge">🔑 Credentials</span>
        <span class="badge">💰 Expenses</span>
        <span class="badge">📈 Investments</span>
        <span class="badge">🏦 EMI Tracker</span>
        <span class="badge">🥇 Gold</span>
        <span class="badge">🎓 School Fees</span>
        <span class="badge">✈️ Travel</span>
        <span class="badge">📝 Notes</span>
        <span class="badge">📅 Events</span>

        <h2>Technology</h2>
        <p>Built using Jetpack Compose, Room Database with SQLCipher encryption,
        Hilt dependency injection, and Kotlin Coroutines — all running entirely offline.</p>

        <h2>Version</h2>
        <p>Version: <strong>1.0.0</strong></p>
        <p>Developer: <strong>SuryaKP</strong></p>

        <div class="footer">
          <p>© WealthHub. All Rights Reserved.</p>
        </div>
        </body>
        </html>
    """.trimIndent()

    Scaffold(topBar = { PTTopBar(title = "About WealthHub", onBack = onBack) }) { padding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
