package com.bhai.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ready = false
    private var speaking = false
    private var listening = false
    private var torchOn = false

    private lateinit var status: TextView
    private lateinit var mic: Button
    private val handler = Handler(Looper.getMainLooper())

    private val blue = Color.rgb(0, 190, 255)
    private val white = Color.WHITE
    private val darkBg = Color.rgb(3, 12, 32)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = darkBg
        window.navigationBarColor = darkBg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 30, 24, 24)
            setBackgroundColor(darkBg)
        }

        // Title
        TextView(this).apply {
            text = "✦  Bhai AI  👑"
            textSize = 32f
            setTextColor(blue)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            root.addView(this)
        }

        // Subtitle
        TextView(this).apply {
            text = "Aapka Digital Dost"
            textSize = 16f
            setTextColor(white)
            gravity = Gravity.CENTER
            root.addView(this)
        }

        // Greeting
        TextView(this).apply {
            text = "\nAssalam-o-Alaikum! 👋\nMain Bhai AI hoon.\n"
            textSize = 22f
            setTextColor(white)
            gravity = Gravity.CENTER
            root.addView(this)
        }

        // Status
        status = TextView(this).apply {
            text = "Aapki awaaz ka intezar hai..."
            textSize = 17f
            setTextColor(blue)
            gravity = Gravity.CENTER
            setPadding(8, 24, 8, 24)
            root.addView(this)
        }

        // Mic Button
        mic = Button(this).apply {
            text = "🎙  START LISTENING"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(20, 80, 190))
        }
        root.addView(mic, LinearLayout.LayoutParams(-1, 140).apply {
            setMargins(0, 16, 0, 20)
        })

        // Quick Commands Title
        TextView(this).apply {
            text = "QUICK COMMANDS"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(blue)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 12)
            root.addView(this)
        }

        // Quick command list
        val commands = listOf(
            "🕒  Waqt kya hai",
            "📅  Aaj ki tareekh",
            "💡  Torch on / off",
            "📺  YouTube kholo",
            "💬  WhatsApp kholo",
            "⚙️  Settings kholo",
            "😄  Mazaq sunao",
            "👋  Salam karo"
        )

        commands.forEach { text ->
            TextView(this).apply {
                this.text = text
                textSize = 16f
                setTextColor(white)
                setPadding(18, 14, 18, 14)
                setBackgroundColor(Color.rgb(13, 31, 62))
                val params = LinearLayout.LayoutParams(-1, -2)
                params.setMargins(0, 6, 0, 6)
                root.addView(this, params)
            }
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        // Text to Speech setup
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val lang = tts?.setLanguage(Locale("ur", "PK"))
                ready = lang != TextToSpeech.LANG_MISSING_DATA &&
                        lang != TextToSpeech.LANG_NOT_SUPPORTED

                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        speaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        speaking = false
                        handler.post {
                            if (mic.isEnabled) startListening()
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        speaking = false
                        handler.post { startListening() }
                    }
                })
            }
        }

        mic.setOnClickListener {
            if (listening) {
                stopListening()
                mic.text = "🎙  RESUME LISTENING"
                status.text = "Listening pause hai."
            } else {
                ensurePermissionAndListen()
            }
        }

        ensurePermissionAndListen()
    }

    private fun ensurePermissionAndListen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 21)
        } else {
            startListening()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 21 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            status.text = "Microphone permission dena zaroori hai."
        }
    }

    private fun startListening() {
        if (isFinishing || speaking || listening) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition available nahi hai."
            return
        }

        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)

            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    runOnUiThread {
                        status.text = "🔵 Listening... Ab boliye"
                        mic.text = "🎙  LISTENING"
                    }
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val words = results?.getStringArrayListExtra(SpeechRecognizer.RESULTS_RECOGNITION)
                    val command = words?.firstOrNull().orEmpty()
                    respond(command)
                }

                override fun onError(error: Int) {
                    listening = false
                    if (!speaking) {
                        handler.postDelayed({ startListening() }, 900)
                    }
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            recognizer?.startListening(intent)
        } catch (e: Exception) {
            listening = false
            status.text = "Mic start nahi hua. Dobara try karein."
        }
    }

    private fun stopListening() {
        listening = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun respond(command: String) {
        val c = command.lowercase(Locale.ROOT)
        var reply = ""

        when {
            // Greetings
            "salam" in c || "assalam" in c || "hello" in c || "hi" in c ->
                reply = "Wa alaikum assalam bhai! Kya haal hai? Kaise ho?"

            "kaise ho" in c || "kaisay ho" in c || "kya haal" in c ->
                reply = "Main bilkul theek hoon bhai! Aap sunao, kya haal hai?"

            // Time & Date
            "waqt" in c || "time" in c || "baje" in c || "kitne baje" in c ->
                reply = "Abhi waqt hai " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

            "tareekh" in c || "tarikh" in c || "date" in c || "aaj" in c && "din" in c ->
                reply = "Aaj ki tareekh hai " + SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

            // Torch
            "torch on" in c || "flashlight on" in c || "light on" in c || "torch jalao" in c || "roshni on" in c -> {
                toggleTorch(true)
                reply = "Torch on kar diya bhai."
            }

            "torch off" in c || "flashlight off" in c || "light off" in c || "torch band" in c || "roshni off" in c -> {
                toggleTorch(false)
                reply = "Torch off kar diya."
            }

            "torch" in c || "flashlight" in c -> {
                toggleTorch(!torchOn)
                reply = if (torchOn) "Torch on kar diya." else "Torch off kar diya."
            }

            // Open Apps
            "youtube" in c || "you tube" in c -> {
                openApp("com.google.android.youtube", "https://youtube.com")
                reply = "YouTube khol raha hoon."
            }

            "whatsapp" in c || "whats app" in c -> {
                openApp("com.whatsapp", "https://whatsapp.com")
                reply = "WhatsApp khol raha hoon."
            }

            "chrome" in c || "browser" in c -> {
                openApp("com.android.chrome", "https://google.com")
                reply = "Chrome khol raha hoon."
            }

            "settings" in c || "setting" in c -> {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                reply = "Settings khol diye."
            }

            "camera" in c -> {
                openApp("com.android.camera", null)
                reply = "Camera khol raha hoon."
            }

            // Fun
            "mazaq" in c || "joke" in c || "hasao" in c ->
                reply = listOf(
                    "Bhai computer thak kyun gaya? Bohat saare tabs khol rakhe thay!",
                    "Ek programmer ne shaadi nahi ki... kyun? Usay bugs se pyar ho gaya tha.",
                    "WiFi se poocha: Tum itne slow kyun ho? Usne kaha: Traffic mein atka hoon bhai!"
                ).random()

            "madad" in c || "help" in c || "kya kar sakte" in c ->
                reply = "Bhai main yeh kar sakta hoon: waqt bata sakta hoon, tareekh, torch on off, YouTube, WhatsApp, Settings khol sakta hoon, aur mazaq bhi suna sakta hoon."

            "shukriya" in c || "thank" in c ->
                reply = "Koi baat nahi bhai! Kabhi bhi bolna."

            "bye" in c || "allah hafiz" in c || "khuda hafiz" in c ->
                reply = "Allah Hafiz bhai! Phir milte hain."

            else ->
                reply = "Bhai yeh baat mujhe abhi nahi aati. Kuch aur try karo jaise waqt, torch, youtube ya mazaq."
        }

        runOnUiThread {
            status.text = "Aap: $command\n\nBhai AI: $reply"
            if (ready) {
                tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "bhai_reply")
            } else {
                handler.postDelayed({ startListening() }, 1200)
            }
        }
    }

    private fun toggleTorch(turnOn: Boolean) {
        try {
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, turnOn)
            torchOn = turnOn
        } catch (e: Exception) {
            // Permission or hardware issue
        }
    }

    private fun openApp(packageName: String, webUrl: String?) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else if (webUrl != null) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
            }
        } catch (e: Exception) {
            if (webUrl != null) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
