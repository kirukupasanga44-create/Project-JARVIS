
package com.kishore.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity(), RecognitionListener {

    private var status by mutableStateOf("SYSTEM ONLINE")
    private var recognizedText by mutableStateOf("Awaiting your command...")
    private var isListening by mutableStateOf(false)

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var textToSpeech: TextToSpeech

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startListening()
            } else {
                status = "MICROPHONE PERMISSION REQUIRED"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }

        setContent {
            JarvisDashboard(
                status = status,
                recognizedText = recognizedText,
                isListening = isListening,
                onMicClick = { checkMicrophonePermission() },
                onStopClick = { stopListening() }
            )
        }
    }

    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            microphonePermission.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status = "SPEECH RECOGNITION UNAVAILABLE"
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(this)

            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    true
                )
            }

            status = "LISTENING..."
            isListening = true
            speechRecognizer?.startListening(intent)

        } catch (e: Exception) {
            status = "COULD NOT START MICROPHONE"
            isListening = false
        }
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        status = "SYSTEM ONLINE"
    }

    private fun speak(message: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_REPLY"
            )
        }
    }

    private fun processCommand(command: String) {
        val input = command.lowercase(Locale.getDefault())

        recognizedText = command
        isListening = false
        status = "PROCESSING COMMAND..."

        when {
            input.contains("hello") ||
            input.contains("hi jarvis") -> {
                status = "COMMAND COMPLETED"
                speak("Hello. JARVIS is ready to assist you.")
            }

            input.contains("your name") -> {
                status = "IDENTITY CONFIRMED"
                speak("I am JARVIS, your personal AI assistant.")
            }

            input.contains("open settings") -> {
                status = "OPENING SETTINGS"
                speak("Opening system settings.")
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }

            input.contains("open youtube") -> {
                openApp("com.google.android.youtube", "YouTube")
            }

            input.contains("open chrome") -> {
                openApp("com.android.chrome", "Chrome")
            }

            input.contains("stop") -> {
                status = "SYSTEM ONLINE"
                speak("Understood. Stopping.")
            }

            else -> {
                status = "BASIC MODE"
                speak(
                    "I heard you say: $command. " +
                    "My advanced AI connection will be added in the next stage."
                )
            }
        }
    }

    private fun openApp(packageName: String, appName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            status = "OPENING $appName"
            speak("Opening $appName.")
            startActivity(launchIntent)
        } else {
            status = "$appName NOT FOUND"
            speak("$appName is not available on this device.")
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        status = "READY"
    }

    override fun onBeginningOfSpeech() {
        status = "HEARING YOU..."
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        status = "PROCESSING..."
    }

    override fun onError(error: Int) {
        isListening = false
        status = "VOICE INPUT ENDED - TRY AGAIN"
    }

    override fun onResults(results: Bundle?) {
        isListening = false

        val matches = results?.getStringArrayList(
            SpeechRecognizer.RESULTS_RECOGNITION
        )

        if (!matches.isNullOrEmpty()) {
            processCommand(matches[0])
        } else {
            status = "NO COMMAND DETECTED"
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        speechRecognizer?.destroy()

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        super.onDestroy()
    }
}

@Composable
fun JarvisDashboard(
    status: String,
    recognizedText: String,
    isListening: Boolean,
    onMicClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val cyan = Color(0xFF00D9FF)
    val background = Color(0xFF050B16)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF102B4A),
                        background,
                        Color(0xFF02050C)
                    )
                )
            )
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "J.A.R.V.I.S.",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = cyan,
                letterSpacing = 5.sp
            )

            Text(
                "PERSONAL INTELLIGENCE SYSTEM",
                color = Color.LightGray,
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF164D77),
                                Color(0xFF09233D),
                                background
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(155.dp),
                    shape = CircleShape,
                    color = Color(0xFF07182B),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        cyan
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (isListening) "◉" else "✦",
                            color = cyan,
                            fontSize = 64.sp
                        )
                    }
                }
            }

            Text(
                status,
                color = cyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102034)
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "LAST COMMAND",
                        color = cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        recognizedText,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onMicClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007FA5)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Microphone")
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isListening) "LISTENING..." else "TALK TO JARVIS",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onStopClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = cyan
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
                Spacer(Modifier.width(8.dp))
                Text("STOP LISTENING")
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "PROJECT JARVIS  •  VERSION 1.0",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
