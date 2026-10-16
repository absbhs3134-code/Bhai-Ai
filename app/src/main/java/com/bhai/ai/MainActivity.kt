
package com.bhai.ai

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {

    private var speaker: TextToSpeech? = null
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speaker = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                speaker?.language = Locale("ur", "PK")
            }
        }

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 60, 32, 32)

        val title = TextView(this)
        title.text = "Bhai AI"
        title.textSize = 30f
        layout.addView(title)

        status = TextView(this)
        status.text = "Assalam-o-Alaikum! Speak dabayein."
        status.textSize = 18f
        layout.addView(status)

        val speakButton = Button(this)
        speakButton.text = "Speak"
        layout.addView(speakButton)

        speakButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "ur-PK"
            )

            try {
                startActivityForResult(intent, 100)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Speech service available nahi hai",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        setContentView(layout)
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
            val command = words?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""
            status.text = command

            val reply = when {
                "time" in command || "waqt" in command ->
                    "Abhi ka waqt check kar lein."
                "salam" in command ->
                    "Wa alaikum assalam!"
                else ->
                    "Maaf kijiye, yeh command abhi nahi aati."
            }

            status.text = "$command\n\n$reply"
            speaker?.speak(
                reply,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "bhai_reply"
            )
        }
    }

    override fun onDestroy() {
        speaker?.stop()
        speaker?.shutdown()
        super.onDestroy()
    }
}
