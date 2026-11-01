package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.service.InstaQuickDownloadService
import com.example.util.VibrationHelper
import java.util.regex.Pattern

class ShareHandlerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIncomingShareIntent(intent)

        // Close immediately without opening any app screen so user returns directly to Reels
        finish()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText.isNullOrBlank()) return

        val instagramUrl = extractInstagramUrl(sharedText)
        if (instagramUrl != null) {
            VibrationHelper.click(this, 25L)
            Toast.makeText(this, "InstaDL: Downloading in background...", Toast.LENGTH_SHORT).show()
            InstaQuickDownloadService.start(this, instagramUrl)
        } else {
            Toast.makeText(this, "No valid Instagram link detected in share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractInstagramUrl(text: String): String? {
        val pattern = Pattern.compile("https?://(?:www\\.)?instagr(?:am\\.com|\\.am)/(?:p|reel|reels|tv)/[A-Za-z0-9_-]+/?")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(0)
        }
        return null
    }
}
