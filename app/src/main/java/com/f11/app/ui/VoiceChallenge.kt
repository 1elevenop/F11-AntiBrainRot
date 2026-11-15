package com.f11.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.f11.app.data.FrictionLanguage
import com.f11.app.data.FrictionLines

/** Whether this phone can recognise speech fully on-device (no network, API 31+). */
fun isOnDeviceSpeechAvailable(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

private sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Heard(val text: String) : VoiceState
    data class Failed(val message: String) : VoiceState
    data object Passed : VoiceState
}

/**
 * Asks the user to say [line] out loud. Uses only Android's on-device recognizer, so audio never
 * leaves the phone (F11 has no internet permission and never asks a cloud service).
 * Calls [onPassed] once the spoken words match.
 */
@Composable
fun VoiceChallenge(
    line: String,
    language: FrictionLanguage,
    onPassed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var state by remember(line) { mutableStateOf<VoiceState>(VoiceState.Idle) }
    val recognizer = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            null
        }
    }
    DisposableEffect(recognizer) { onDispose { recognizer?.destroy() } }

    val intent = remember(language) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.speechTag)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
    }

    fun listen() {
        val r = recognizer ?: return
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { state = VoiceState.Listening }
            override fun onResults(results: Bundle?) {
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                state = if (heard.any { FrictionLines.spokenMatches(line, it) }) {
                    onPassed()
                    VoiceState.Passed
                } else {
                    VoiceState.Heard(heard.firstOrNull().orEmpty())
                }
            }
            override fun onError(error: Int) {
                state = VoiceState.Failed(describeError(error, language))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && error in LANGUAGE_ERRORS) {
                    // Ask Android to fetch the offline language pack for next time.
                    runCatching { r.triggerModelDownload(intent) }
                }
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        state = VoiceState.Listening
        r.startListening(intent)
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listen() else state = VoiceState.Failed("Microphone permission is needed for this step.")
    }

    Column(modifier) {
        Text("Say it out loud", style = MaterialTheme.typography.labelLarge, color = F11Palette.TextDim)
        Spacer(Modifier.padding(top = 6.dp))
        Text("“$line”", style = MaterialTheme.typography.titleMedium, color = F11Palette.Text)
        Spacer(Modifier.padding(top = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val passed = state == VoiceState.Passed
            val listening = state == VoiceState.Listening
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (passed) F11Palette.Mint else if (listening) F11Palette.Violet else F11Palette.Tile)
                    .border(1.dp, F11Palette.Hairline, CircleShape)
                    .clickable(enabled = recognizer != null && !passed && !listening, role = Role.Button) {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) listen() else permission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                contentAlignment = Alignment.Center,
            ) {
                MicGlyph(color = if (passed || listening) F11Palette.Black else F11Palette.Text)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = when (val s = state) {
                    VoiceState.Idle -> if (recognizer == null) "On-device speech isn't available here." else "Tap the mic and speak."
                    VoiceState.Listening -> "Listening…"
                    is VoiceState.Heard -> "Heard: “${s.text}”. Try again."
                    is VoiceState.Failed -> s.message
                    VoiceState.Passed -> "Done."
                },
                style = MaterialTheme.typography.bodySmall,
                color = F11Palette.TextDim,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val LANGUAGE_ERRORS = setOf(
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
)

private fun describeError(error: Int, language: FrictionLanguage): String = when (error) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Tap and try again."
    SpeechRecognizer.ERROR_AUDIO -> "Microphone error. Try again."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for this step."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy. Try again in a moment."
    in LANGUAGE_ERRORS -> "The ${language.label} offline speech pack isn't installed yet. Android is downloading it; try again shortly."
    else -> "Couldn't listen (error $error). Try again."
}
