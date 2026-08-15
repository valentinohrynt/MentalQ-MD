package com.c242_ps246.mentalq.ui.component

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VoiceToTextParser(
    private val app: Application
) : RecognitionListener {

    private val _state = MutableStateFlow(VoiceToTextParserState())
    val state = _state.asStateFlow()

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(app)
    private var isDestroyed = false

    fun startListening(langCode: String) {
        _state.update {
            VoiceToTextParserState(
                isSpeaking = true,
                hasStartedSpeaking = false
            )
        }

        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            _state.update {
                it.copy(error = "Speech recognition is not available on this device.")
            }
            return
        }

        if (isDestroyed) return

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
        }

        recognizer.setRecognitionListener(this)
        recognizer.startListening(recognizerIntent)

        _state.update { it.copy(isSpeaking = true) }
    }

    fun stopListening() {
        if (isDestroyed) return
        recognizer.stopListening()
        _state.update { it.copy(isSpeaking = false) }
    }

    fun destroy() {
        if (isDestroyed) return
        recognizer.cancel()
        recognizer.destroy()
        isDestroyed = true
        _state.update { it.copy(isSpeaking = false) }
    }

    override fun onBeginningOfSpeech() {
        _state.update { it.copy(hasStartedSpeaking = true) }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.update { it.copy(error = null) }
    }

    override fun onEndOfSpeech() {
        _state.update { it.copy(isSpeaking = false) }
    }

    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_CLIENT) {
            return
        } else {
            _state.update { it.copy(error = "Error: $error") }
        }
    }

    override fun onResults(results: Bundle?) {
        results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.getOrNull(0)
            ?.let { spokenTexts ->
                _state.update { it.copy(spokenText = spokenTexts) }
            }
    }

    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

data class VoiceToTextParserState(
    val spokenText: String? = "",
    val isSpeaking: Boolean = false,
    val error: String? = null,
    val hasStartedSpeaking: Boolean = false
)
