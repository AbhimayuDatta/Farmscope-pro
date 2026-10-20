package com.example.narrator

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.speech.tts.TextToSpeech
import java.util.Locale

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** A context whose resources resolve in [locale], regardless of the app's UI language. */
fun Context.localizedFor(locale: Locale): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

private const val TTS_SETTINGS_ACTION = "com.android.settings.TTS_SETTINGS"

/**
 * Opens the voice-data installer (Google TTS), falling back to the system TTS settings.
 * Returns false when neither screen could be opened.
 */
fun Context.openTtsVoiceDataInstaller(): Boolean {
    val candidates = listOf(
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
        Intent(TTS_SETTINGS_ACTION),
    )
    for (intent in candidates) {
        if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // try the next candidate
        }
    }
    return false
}
