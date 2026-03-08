package com.samsung.android.seamless.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.runBlocking

/**
 * Transparent trampoline Activity that clears transcript/error from widget state.
 */
class ClearTranscriptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runBlocking {
            WidgetStateManager(this@ClearTranscriptActivity).clearTranscriptAndRefresh()
        }
        Toast.makeText(this, "Transcript cleared", Toast.LENGTH_SHORT).show()
        finish()
    }
}
