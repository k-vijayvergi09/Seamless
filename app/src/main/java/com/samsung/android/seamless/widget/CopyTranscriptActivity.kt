package com.samsung.android.seamless.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

/**
 * Transparent trampoline activity invoked from the widget to copy the latest transcript.
 */
class CopyTranscriptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transcript = WidgetStateManager(this).transcriptText.trim()
        if (transcript.isNotBlank()) {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("Transcript", transcript))
            Toast.makeText(this, "Transcript copied", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No transcript to copy", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
