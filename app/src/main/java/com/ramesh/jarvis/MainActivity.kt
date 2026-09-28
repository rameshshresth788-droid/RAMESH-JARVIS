package com.ramesh.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var speech: SpeechRecognizer
    private lateinit var configStore: JarvisConfigStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configStore = JarvisConfigStore(this)
        tts = TextToSpeech(this, this)
        speech = SpeechRecognizer.createSpeechRecognizer(this)

        setContent {
            JarvisScreen(
                startListening = { onListenRequest(it) },
                openSettings = { openSettingsDialog = true },
                initialConfig = configStore.load()
            )
        }
    }

    private var openSettingsDialog by mutableStateOf(false)

    private fun onListenRequest(callback: (String, String) -> Unit) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 700)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { callback("LISTENING...", "") }
            override fun onBeginningOfSpeech() { callback("LISTENING...", "") }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { callback("VOICE ERROR", "") }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                callback("PROCESSING...", text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speech.startListening(intent)
    }

    fun speak(text: String) {
        if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.getDefault()
    }

    override fun onDestroy() {
        if (::speech.isInitialized) speech.destroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }

    @Composable
    fun JarvisScreen(
        startListening: ((String, String) -> Unit) -> Unit,
        openSettings: () -> Unit,
        initialConfig: JarvisConfig
    ) {
        var status by remember { mutableStateOf("SYSTEM READY") }
        var lastUserText by remember { mutableStateOf("") }
        var lastAnswer by remember { mutableStateOf("") }
        var listening by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        var config by remember { mutableStateOf(initialConfig) }
        val scope = rememberCoroutineScope()

        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

        MaterialTheme(
            colorScheme = darkColorScheme(
                background = Color.Black,
                surface = Color(0xFF0C0A06),
                primary = Color(0xFFD7A928)
            )
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        listOf(Color(0xFF302714), Color(0xFF0D0B07), Color.Black)
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxSize().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "⚙",
                            color = Color(0xFFFFE9A0),
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clickable { showSettings = true }
                                .padding(8.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    VoiceCore(
                        listening = listening,
                        onClick = {
                            listening = true
                            status = "LISTENING..."
                            startListening { newStatus, text ->
                                status = newStatus
                                if (text.isNotBlank()) {
                                    lastUserText = text
                                    scope.launch {
                                        status = "THINKING..."
                                        val answer = withContext(Dispatchers.IO) {
                                            AIClient.ask(
                                                config,
                                                listOf("user" to text)
                                            )
                                        }
                                        lastAnswer = answer
                                        status = "SPEAKING..."
                                        speak(answer)
                                        status = "SYSTEM READY"
                                        listening = false
                                    }
                                } else if (newStatus == "VOICE ERROR") {
                                    listening = false
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        status,
                        color = Color(0xFFFFF7D1),
                        fontSize = 11.sp,
                        letterSpacing = 4.sp
                    )

                    if (lastUserText.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            lastUserText,
                            color = Color(0xFFDDC98D),
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    }

                    Spacer(Modifier.weight(1f))
                }

                if (showSettings) {
                    SettingsDialog(
                        initial = config,
                        onDismiss = { showSettings = false },
                        onSave = {
                            config = it
                            configStore.save(it)
                            showSettings = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun VoiceCore(listening: Boolean, onClick: () -> Unit) {
        val infinite = rememberInfiniteTransition(label = "core")
        val r1 by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "r1")
        val r2 by infinite.animateFloat(360f, 0f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "r2")
        val pulse by infinite.animateFloat(.96f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")

        Box(Modifier.size(290.dp).scale(if (listening) pulse else 1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().rotate(r1)) {
                drawCircle(Color(0xFFD7A928).copy(alpha = .22f), size.minDimension*.42f, style=Stroke(2f))
                drawArc(Color(0xFFFFF7D1), 10f, 100f, false, style=Stroke(3f),
                    topLeft = Offset(size.width*.09f,size.height*.09f),
                    size = androidx.compose.ui.geometry.Size(size.width*.82f,size.height*.82f))
            }
            Canvas(Modifier.fillMaxSize().rotate(r2)) {
                drawCircle(Color(0xFFFFF7D1).copy(alpha=.2f), size.minDimension*.46f, style=Stroke(1.5f))
                drawArc(Color(0xFFD7A928), 190f, 120f, false, style=Stroke(3f),
                    topLeft = Offset(size.width*.04f,size.height*.04f),
                    size = androidx.compose.ui.geometry.Size(size.width*.92f,size.height*.92f))
            }
            Box(
                Modifier.size(120.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFFFF7D1).copy(alpha=.3f), Color(0xFFD7A928).copy(alpha=.12f), Color.Black)))
                    .border(2.dp, Color(0xFFFFF7D1).copy(alpha=.85f), CircleShape)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("◉", color=Color.White, fontSize=44.sp)
            }
        }
    }

    @Composable
    private fun SettingsDialog(
        initial: JarvisConfig,
        onDismiss: () -> Unit,
        onSave: (JarvisConfig) -> Unit
    ) {
        var endpoint by remember { mutableStateOf(initial.endpoint) }
        var apiKey by remember { mutableStateOf(initial.apiKey) }
        var model by remember { mutableStateOf(initial.model) }
        var prompt by remember { mutableStateOf(initial.systemPrompt) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("JARVIS SETTINGS") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("AI connection", color = Color(0xFFD7A928))
                    OutlinedTextField(endpoint, { endpoint = it }, label={Text("API endpoint")}, singleLine=true)
                    OutlinedTextField(apiKey, { apiKey = it }, label={Text("API key")}, singleLine=true)
                    OutlinedTextField(model, { model = it }, label={Text("Model")}, singleLine=true)
                    OutlinedTextField(prompt, { prompt = it }, label={Text("System prompt")}, minLines=3)
                    Text("API key is encrypted with Android Keystore and is not stored in the source code.", fontSize=11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(JarvisConfig(endpoint, apiKey, model, prompt))
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("CANCEL") }
            }
        )
    }
}
