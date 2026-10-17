
package com.bhai.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceService : Service(), RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var running = false
    private var restarting = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = Notification.Builder(this, "bhai_voice")
            .setContentTitle("Bhai AI listening")
            .setContentText("Voice assistant active. Stop it from the app.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(101, notification)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ur", "PK")
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        running = true
        listenAgain()
        return START_NOT_STICKY
    }

    private fun listenAgain() {
        if (!running || restarting) return
        restarting = true

        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(this)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            restarting = false
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            restarting = false
        }
    }

    override fun onResults(results: Bundle?) {
        val words = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()

        val text = words.lowercase(Locale.ROOT)

        if (text.contains("bhai")) {
            val question = text.replace("bhai", "").trim()

            val answer = when {
                question.contains("salam") ||
                question.contains("assalam") ->
                    "Wa alaikum assalam!"

                question.contains("kaise ho") ->
                    "Main theek hoon bhai!"

                question.contains("time") ||
                question.contains("waqt") ->
                    "Abhi waqt ${java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())} hai."

                question.isBlank() ->
                    "Ji bhai, bolo!"

                else ->
                    "Bhai, is sawal ka jawab abhi mere paas nahi hai."
            }

            tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "bhai_reply")
        }

        restartListening()
    }

    private fun restartListening() {
        if (!running) return
        restarting = false
        android.os.Handler(mainLooper).postDelayed({
            listenAgain()
        }, 700)
    }

    override fun onError(error: Int) {
        restartListening()
    }

    override fun onEndOfSpeech() {}
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bhai_voice",
                "Bhai AI Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        running = false
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
