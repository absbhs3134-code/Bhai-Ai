package com.bhai.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ready = false
    private var speaking = false
    private var listening = false

    private lateinit var status: TextView
    private lateinit var mic: Button
    private val handler = Handler(Looper.getMainLooper())

    private val blue = Color.rgb(0, 190, 255)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(3, 12, 32)
        window.navigationBarColor = Color.rgb(3, 12, 32)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 30, 24, 24)
            setBackgroundColor(Color.rgb(3, 12, 32))
        }

        val title = TextView(this).apply {
            text = "✦  Bhai AI  👑"
            textSize = 32f
            setTextColor(blue)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Aapka Digital Dost"
            textSize = 16f
            setTextColor(white)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle)

        val greeting = TextView(this).apply {
            text = "\nAssalam-o-Alaikum! 👋\nMain Bhai AI hoon.\n"
            textSize = 23f
            setTextColor(white)
            gravity = Gravity.CENTER
        }
        root.addView(greeting)

        status = TextView(this).apply {
            text = "Aapki awaaz ka intezar hai..."
            textSize = 17f
            setTextColor(blue)
            gravity = Gravity.CENTER
            setPadding(8, 24, 8, 24)
        }
        root.addView(status)

        mic = Button(this).apply {
            text = "🎙  START LISTENING"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(20, 80, 190))
        }
        root.addView(
            mic,
            LinearLayout.LayoutParams(
                -1, 64
            ).apply {
                setMargins(0, 16, 0, 20)
            }
        )

        val apps = TextView(this).apply {
            text = "YOUR QUICK COMMANDS"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(blue)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        root.addView(apps)

        listOf(
            "🕒  Waqt kya hua hai?",
            "📅  Aaj ki tareekh",
            "👋  Salam karo",
            "😄  Koi mazaq sunao",
            "💬  Madad chahiye"
        ).forEach { text ->
            val item = TextView(this).apply {
                this.text = text
                textSize = 17f
                setTextColor(white)
                setPadding(18, 15, 18, 15)
                setBackgroundColor(Color.rgb(13, 31, 62))
            }
            val params = LinearLayout.LayoutParams(-1, -2)
            params.setMargins(0, 5, 0, 5)
            root.addView(item, params)
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val language = tts?.setLanguage(Locale("ur", "PK"))
                ready = language !=
                    TextToSpeech.LANG_MISSING_DATA &&
                    language != TextToSpeech.LANG_NOT_SUPPORTED

                tts?.setOnUtteranceProgressListener(
                    object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(id: String?) {
                            speaking = true
                        }

                        override fun onDone(id: String?) {
                            speaking = false
                            handler.post {
                                if (mic.isEnabled) startListening()
                            }
                        }

                        override fun onError(id: String?) {
                            speaking = false
                            handler.post { startListening() }
                        }
                    }
                )
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
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO), 21
            )
        } else {
            startListening()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )
        if (requestCode == 21 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            status.text = "Microphone permission dena zaroori hai."
        }
    }

    private fun startListening() {
        if (isFinishing || speaking || listening) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition service available nahi hai."
            return
        }

        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)

            recognizer?.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        listening = true
                        runOnUiThread {
                            status.text = "🔵 Listening... Ab boliye"
                            mic.text = "🎙  LISTENING"
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        listening = false
                        val words = results?.getStringArrayListExtra(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        val command = words?.firstOrNull().orEmpty()
                        respond(command)
                    }

                    override fun onError(error: Int) {
                        listening = false
                        if (!speaking) {
                            handler.postDelayed({
                                startListening()
                            }, 900)
                        }
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        listening = false
                    }
                    override fun onPartialResults(partial: Bundle?) {}
                    override fun onEvent(type: Int, params: Bundle?) {}
                }
            )

            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "ur-PK"
                )
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )
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

        val reply = when {
            "salam" in c || "assalam" in c ->
                "Wa alaikum assalam bhai! Kya haal hai?"

            "kaise ho" in c || "kaisay ho" in c ->
                "Main theek hoon bhai! Aap sunao."

            "waqt" in c || "time" in c ||
                "baje" in c ->
                "Abhi waqt hai " +
                    SimpleDateFormat(
                        "hh:mm a", Locale.getDefault()
                    ).format(Date())

            "tareekh" in c || "tarikh" in c ||
                "date" in c ->
                "Aaj ki tareekh " +
                    SimpleDateFormat(
                        "dd MMMM yyyy",
                        Locale.getDefault()
                    ).format(Date())

            "mazaq" in c || "joke" in c ->
                "Bhai, computer thak kyun gaya? Bohat saare tabs khol rakhe thay!"

            "madad" in c || "help" in c ->
                "Bhai, waqt, tareekh, salam aur mazaq ke liye mujhse pooch sakte ho."

            else ->
                "Bhai, yeh baat mujhe abhi nahi aati."
        }

        runOnUiThread {
            status.text = "Aap: $command\n\nBhai AI: $reply"
            if (ready) {
                tts?.speak(
                    reply,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "bhai_reply"
                )
            } else {
                handler.postDelayed({ startListening() }, 1000)
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
