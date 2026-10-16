package com.bhai.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var torchOn = false
    private var cameraId: String? = null
    private var readyToSpeak = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.RECORD_AUDIO] == true) {
            Toast.makeText(this, "Mic permission mil gayi. Ab Speak dabao.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Voice ke liye microphone permission zaroori hai.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        try {
            val cm = getSystemService(CAMERA_SERVICE) as CameraManager
            cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(
                    android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
                ) == true
            }
        } catch (_: Exception) { }

        setContent {
            MaterialTheme {
                var transcript by remember { mutableStateOf("") }
                var reply by remember { mutableStateOf("Assalam-o-alaikum bhai! Main Bhai AI hoon. Speak dabao ya neeche command likho.") }
                var command by remember { mutableStateOf("") }

                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Bhai AI", style = MaterialTheme.typography.headlineLarge)
                        Text("Tecno Spark 10 Pro • Voice + basic mobile control")
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Tumne kaha:", style = MaterialTheme.typography.titleSmall)
                                Text(transcript.ifBlank { "Abhi koi command nahi." })
                                Spacer(Modifier.height(12.dp))
                                Text("Bhai ka jawab:", style = MaterialTheme.typography.titleSmall)
                                Text(reply)
                            }
                        }
                        Button(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                                } else {
                                    startListening { spoken ->
                                        transcript = spoken
                                        reply = handleCommand(spoken)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("🎙 Speak / Bolo") }

                        OutlinedTextField(
                            value = command,
                            onValueChange = { command = it },
                            label = { Text("Command yahan likho") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = {
                            transcript = command
                            reply = handleCommand(command)
                            command = ""
                        }, modifier = Modifier.fillMaxWidth()) { Text("Command chalao") }

                        Text("Try these commands", style = MaterialTheme.typography.titleMedium)
                        Text("• torch on\n• torch off\n• settings kholo\n• YouTube kholo\n• time kya hai\n• help")
                        Text("Note: Yeh starter version hai. General AI answers ke liye secure online AI backend add karna hoga.")
                    }
                }
            }
        }
    }

    private fun startListening(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Is phone par speech recognition service available nahi.", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bhai, bolo kya karna hai")
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 42)
        } catch (_: Exception) {
            Toast.makeText(this, "Voice input start nahi ho saka.", Toast.LENGTH_LONG).show()
        }
        pendingResult = onResult
    }

    private var pendingResult: ((String) -> Unit)? = null

    @Deprecated("Deprecated in Android API but used here for broad starter compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == Activity.RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            pendingResult?.invoke(spoken)
        }
    }

    private fun handleCommand(raw: String): String {
        val text = raw.lowercase(Locale.ROOT).trim()
        val response = when {
            text.contains("torch on") || text.contains("torch chala") || text.contains("flash on") || text.contains("torch kholo") -> {
                setTorch(true); if (torchOn) "Theek hai bhai, torch on kar di." else "Torch on nahi ho saki. Camera permission ya flash check karo."
            }
            text.contains("torch off") || text.contains("torch band") || text.contains("flash off") -> {
                setTorch(false); "Theek hai bhai, torch off kar di."
            }
            text.contains("settings") || text.contains("setting kholo") -> {
                runCatching { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
                "Bhai, Settings khol raha hoon."
            }
            text.contains("youtube") -> {
                val launch = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (launch != null) startActivity(launch)
                else startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com")))
                "Bhai, YouTube khol raha hoon."
            }
            text.contains("time") || text.contains("waqt") || text.contains("kitne baje") -> {
                "Bhai, abhi " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + " hain."
            }
            text.contains("help") || text.contains("madad") -> "Bhai, abhi torch on/off, Settings, YouTube aur time commands chala sakta hoon."
            text.isBlank() -> "Bhai, command sunai nahi di. Dobara try karo."
            else -> "Bhai, command samajh nahi aayi. Filhaal torch on/off, Settings kholo, YouTube kholo ya time kya hai bolo. General AI chat abhi connect nahi hai."
        }
        if (readyToSpeak) tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "bhai_reply")
        return response
    }

    private fun setTorch(enabled: Boolean) {
        try {
            val id = cameraId ?: return
            val cm = getSystemService(CAMERA_SERVICE) as CameraManager
            cm.setTorchMode(id, enabled)
            torchOn = enabled
        } catch (_: Exception) {
            torchOn = false
        }
    }

    override fun onInit(status: Int) {
        readyToSpeak = status == TextToSpeech.SUCCESS
        if (readyToSpeak) {
            tts?.language = Locale("ur", "PK")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
