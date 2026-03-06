package com.samsung.android.seamless.widget

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.seamless.data.SarvamSttRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

class SttViewModel(
    private val repository: SarvamSttRepository
) : ViewModel() {

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private val _latestTranscript = MutableLiveData("")
    val latestTranscript: LiveData<String> = _latestTranscript

    private val _fullTranscript = MutableLiveData("")
    val fullTranscript: LiveData<String> = _fullTranscript

    private var collectionJob: Job? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun toggleRecording() {
        if (_isRecording.value == true) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        repository.startStreaming()

        collectionJob = viewModelScope.launch {
            repository.transcripts.collect { rawMessage ->
                val transcript = parseSarvamMessage(rawMessage)
                if (transcript != null) {
                    _latestTranscript.postValue(transcript)
                    val prev = _fullTranscript.value ?: ""
                    _fullTranscript.postValue(if (prev.isBlank()) transcript else "$prev $transcript")
                }
            }
        }
        _isRecording.value = true
    }

    /**
     * Parses Sarvam AI WebSocket response JSON.
     *
     * Transcript: {"type": "data", "data": {"transcript": "...", "language_code": "...", ...}}
     * VAD events: {"type": "events", "data": {"signal_type": "START_SPEECH"|"END_SPEECH", ...}}
     * Errors:     {"type": "error", "data": {"error": "...", "code": ...}}
     *
     * Returns the transcript string if present, null otherwise.
     */
    private fun parseSarvamMessage(raw: String): String? {
        return try {
            val json = JSONObject(raw)
            val data = json.optJSONObject("data")
            when (json.optString("type")) {
                "data" -> data?.optString("transcript", "")?.takeIf { it.isNotBlank() }
                "events" -> null // VAD signals handled by service layer, not emitted as text
                "error" -> null  // Errors handled by service layer
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun stopRecording() {
        collectionJob?.cancel()
        repository.stopStreaming()
        _isRecording.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
    }
}
