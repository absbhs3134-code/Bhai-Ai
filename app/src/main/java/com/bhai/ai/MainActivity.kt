package com.bhai.ai

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private var speaker: TextToSpeech? = null
    private lateinit var status: TextView
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speaker = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val langResult =
                    speaker?.setLanguage(Locale("ur", "PK"))
                ttsReady = langResult !=
                    TextToSpeech.LANG_MISSING_DATA &&
                    langResult !=
                    TextToSpeech.LANG_NOT_SUPPORTED
            }
        }

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER_HORIZONTAL
        layout.setPadding(32, 60, 32, 32)

        val title = TextView(this)
        title.text = "Bhai AI"
        title.textSize = 32f
        layout.addView(title)

        status = TextView(this)
        status.text = "Assalam-o-Alaikum! Main Bhai AI hoon."
        status.textSize = 20f
        status.setPadding(0, 30, 0, 30)
        layout.addView(status)

        val button = Button(this)
        button.text = "🎤 SPEAK"
        layout.addView(button)

        button.setOnClickListener {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "ur-PK"
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "ur-PK"
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Bhai AI se baat karein"
            )

            try {
                startActivityForResult(intent, 100)
            } catch (e: Exception) {
                status.text =
                    "Speech recognition service available nahi hai."
            }
        }

        setContentView(layout)
    }

    private fun getReply(command: String): String {
        val c = command.lowercase(Locale.ROOT)

        return when {
            "salam" in c ||
            "assalam" in c ||
            "السلام" in c ->
                "Wa alaikum assalam bhai! Kya haal hai?"

            "kaise ho" in c ||
            "kaisay ho" in c ||
            "how are you" in c ->
                "Main theek hoon bhai! Aap sunao."

            "tumhara naam" in c ||
            "naam kya" in c ||
            "your name" in c ->
                "Mera naam Bhai AI hai."

            "time" in c ||
            "waqt" in c ||
            "kitne baje" in c ||
            "kitna baj" in c ->
                "Abhi waqt hai " +
                SimpleDateFormat(
                    "hh:mm a", Locale.getDefault()
                ).format(Date())

            "date" in c ||
            "tareekh" in c ||
            "tarikh" in c ||
            "aaj kya din" in c ->
                "Aaj ki tareekh " +
                SimpleDateFormat(
                    "dd MMMM yyyy, EEEE",
                    Locale("ur", "PK")
                ).format(Date())

            "hello" in c ||
            "hi" == c.trim() ||
            "hello bhai" in c ->
                "Hello bhai! Batao, kya madad chahiye?"

            "shukriya" in c ||
            "thank you" in c ||
            "thanks" in c ->
                "Khushi hui bhai! Aur batao."

            "joke" in c ||
            "mazaq" in c ||
            "latifa" in c ->
                "Bhai, computer thak kyun gaya? Kyun ke usne bohat saare tabs khol rakhe thay!"

            "tum kya kar sakte" in c ||
            "help" in c ||
            "madad" in c ->
                "Bhai, main salam ka jawab, waqt, tareekh aur kuch sawalon ke jawab de sakta hoon."

            "band ho jao" in c ||
            "stop speaking" in c ->
                "Theek hai bhai."

            else ->
                "Bhai, yeh sawal mujhe abhi nahi aata. Koi doosra sawal poochho."
        }
    }

    @Deprecated("Deprecated by Android")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )

            val command = words?.firstOrNull() ?: ""
            val reply = getReply(command)

            status.text = "$command\n\n$reply"

            if (ttsReady) {
                speaker?.speak(
                    reply,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "bhai_reply"
                )
            }
        }
    }

    override fun onDestroy() {
        speaker?.stop()
        speaker?.shutdown()
        super.onDestroy()
    }
}
