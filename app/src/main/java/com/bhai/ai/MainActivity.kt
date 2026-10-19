
package com.bhai.ai

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

class MainActivity : Activity() {
    private var tts: TextToSpeech? = null
    private var ready = false
    private lateinit var replyView: TextView
    private lateinit var status: TextView
    private val bg = Color.rgb(8, 10, 18)
    private val cyan = Color.rgb(65, 230, 205)
    private val muted = Color.rgb(155, 163, 181)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val language = tts?.setLanguage(Locale("ur", "PK"))
                ready = language != TextToSpeech.LANG_MISSING_DATA &&
                        language != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(22, 18, 22, 12)
            setBackgroundColor(bg)
        }

        val header = TextView(this).apply {
            text = "✦  BHAI AI                         ● ONLINE"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(header, LinearLayout.LayoutParams(-1, 48))

        root.addView(label("YOUR PERSONAL AI COMPANION", 11f, cyan))
        root.addView(label("Assalam-o-Alaikum, Bhai", 25f, Color.WHITE))
        root.addView(label("Main yahan hoon. Bolo, kya help chahiye?", 13f, muted))

        root.addView(GlowOrb(), LinearLayout.LayoutParams(-1, 0, 1f))

        status = label("READY TO LISTEN", 12f, cyan)
        root.addView(status, LinearLayout.LayoutParams(-1, 32))

        replyView = label("Mic dabao aur apna sawal bolo", 15f, Color.WHITE)
        replyView.setPadding(12, 14, 12, 14)
        replyView.background = rounded(Color.rgb(22, 26, 39), 16)
        root.addView(replyView, LinearLayout.LayoutParams(-1, 76))

        val mic = label("🎙   TAP TO SPEAK", 17f, Color.WHITE)
        mic.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.rgb(190, 35, 65), Color.rgb(100, 35, 150))
        ).apply { cornerRadius = 22f }
        mic.setOnClickListener { listen() }
        root.addView(mic, LinearLayout.LayoutParams(-1, 58).apply {
            topMargin = 14
        })

        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER
        }
        listOf("Home", "Chat", "Triggers", "Settings").forEach { item ->
            val tab = label(item, 11f, muted)
            tab.setOnClickListener {
                status.text = item.uppercase(Locale.ROOT)
                replyView.text = when (item) {
                    "Home" -> "Bhai AI ready hai. Mic dabao aur baat karo."
                    "Chat" -> "Chat ke liye mic button dabao."
                    "Triggers" -> "Voice trigger service alag se configure karni hogi."
                    else -> "Language: Urdu | Voice: ${if (ready) "Ready" else "Loading"}"
                }
            }
            nav.addView(tab, LinearLayout.LayoutParams(0, 52, 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun label(value: String, size: Float, color: Int) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
        }

    private fun rounded(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bhai AI se baat karein")
        }
        try {
            status.text = "LISTENING..."
            startActivityForResult(intent, 100)
        } catch (_: Exception) {
            status.text = "SPEECH SERVICE NOT AVAILABLE"
            replyView.text = "Phone par speech recognition service check karo."
        }
    }

    private fun answer(q: String): String {
        val s = q.lowercase(Locale.ROOT)
        return when {
            "salam" in s || "assalam" in s ->
                "Wa alaikum assalam bhai!"
            "kaise ho" in s || "kaisay ho" in s ->
                "Main theek hoon bhai! Aap sunao."
            "naam" in s || "your name" in s ->
                "Mera naam Bhai AI hai."
            "time" in s || "waqt" in s || "baje" in s ->
                "Abhi " + SimpleDateFormat("hh:mm a",
                    Locale.getDefault()).format(Date()) + " hain."
            "tareekh" in s || "date" in s ->
                "Aaj " + SimpleDateFormat("dd MMMM yyyy",
                    Locale.getDefault()).format(Date())
            "shukriya" in s || "thanks" in s ->
                "Khushi hui bhai!"
            else ->
                "Bhai, is sawal ke liye AI API connect karni hogi."
        }
    }

    @Deprecated("Deprecated by Android")
    override fun onActivityResult(code: Int, result: Int, data: Intent?) {
        super.onActivityResult(code, result, data)
        if (code == 100 && result == RESULT_OK) {
            val q = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            val a = answer(q)
            status.text = "BHAI AI REPLIED"
            replyView.text = "You: $q\n\nBhai AI: $a"
            if (ready) tts?.speak(a, TextToSpeech.QUEUE_FLUSH, null, "reply")
        } else {
            status.text = "READY TO LISTEN"
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private inner class GlowOrb(context: Activity = this) : View(context) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private var phase = 0f

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val x = width / 2f
            val y = height / 2f
            val r = minOf(width, height) * .29f
            phase += .035f

            p.shader = RadialGradient(
                x, y, r * 1.7f,
                intArrayOf(Color.argb(90, 0, 230, 200),
                    Color.argb(40, 60, 50, 200), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, r * 1.7f, p)

            p.shader = RadialGradient(
                x - r * .2f, y - r * .2f, r,
                intArrayOf(Color.rgb(140, 255, 235),
                    Color.rgb(20, 160, 175), Color.rgb(20, 30, 80)),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, r * (1f + .04f *
                sin(phase.toDouble()).toFloat()), p)
            p.shader = null
            p.color = Color.WHITE
            p.textSize = r * .65f
            p.textAlign = Paint.Align.CENTER
            canvas.drawText("B", x, y + r * .22f, p)
            postInvalidateDelayed(40)
        }
    }
}
