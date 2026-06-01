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
fun PrivacyPolicyScreen(onBack: () -> Unit) {
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
          li { font-size: 14px; margin: 6px 0; }
          .footer { margin-top: 32px; padding-top: 12px; border-top: 1px solid #ccc; font-size: 12px; color: #777; }
        </style>
        </head>
        <body>
        <h1>Privacy Policy</h1>
        <p>Your privacy is important to us.</p>
        <p>WealthHub stores your personal information securely on your device.</p>

        <h2>Key Principles</h2>
        <ul>
          <li>Data remains under your control at all times.</li>
          <li>No personal information is sold to third parties.</li>
          <li>No data is shared without user consent.</li>
          <li>Sensitive information is protected using AES-256 encryption.</li>
          <li>Application PIN protection secures access to the app.</li>
          <li>Users may export or delete their own data at any time.</li>
        </ul>

        <h2>Data Storage</h2>
        <p>All data is stored locally on your device using an encrypted SQLite database
        (SQLCipher). No data is transmitted over the internet unless explicitly initiated
        by the user (e.g., manual backup export).</p>

        <h2>Cloud Synchronization</h2>
        <p>Cloud synchronization is <strong>disabled by default</strong>. WealthHub does
        not automatically upload, sync, or share your data with any remote server.</p>

        <h2>Permissions</h2>
        <p>WealthHub requests only the permissions necessary to function: camera (for
        document scanning), biometric (for unlock), storage (for backup/restore), and
        notifications (for event reminders).</p>

        <h2>Contact</h2>
        <p>For privacy concerns, contact the developer through the application's support
        channel.</p>

        <div class="footer">
          <p>By using WealthHub, you agree to this privacy policy.</p>
          <p>© WealthHub. All Rights Reserved.</p>
        </div>
        </body>
        </html>
    """.trimIndent()

    Scaffold(topBar = { PTTopBar(title = "Privacy Policy", onBack = onBack) }) { padding ->
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
