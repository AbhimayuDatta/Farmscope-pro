package com.example.narrator

import android.content.Context
import androidx.annotation.StringRes
import java.util.Locale

enum class NarratorLanguage(val tag: String) {
    ENGLISH("en-US"),
    HINDI("hi-IN");

    // forLanguageTag avoids the Locale constructors deprecated since Java 19.
    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?): NarratorLanguage =
            entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

/** Persisted user preferences. `autoNarrate` is the "narrator enabled" switch. */
data class NarratorSettings(
    val language: NarratorLanguage = NarratorLanguage.ENGLISH,
    val speechRate: Float = 1f,
    val pitch: Float = 1f,
    val autoNarrate: Boolean = false,
    val muted: Boolean = false,
) {
    companion object {
        const val MIN_VALUE = 0.5f
        const val MAX_VALUE = 2.0f
    }
}

enum class TtsStatus { INITIALIZING, READY, NO_ENGINE, FAILED }
enum class LanguageStatus { AVAILABLE, MISSING_DATA, NOT_SUPPORTED }
enum class PlaybackState { IDLE, SPEAKING, PAUSED }

/** Everything the engine knows; produced by [NarratorEngine]. */
data class EngineState(
    val tts: TtsStatus = TtsStatus.INITIALIZING,
    val requestedLanguage: NarratorLanguage = NarratorLanguage.ENGLISH,
    val languageStatus: LanguageStatus = LanguageStatus.AVAILABLE,
    /** Language actually used for speech (English when the requested voice is missing). */
    val effectiveLanguage: NarratorLanguage = NarratorLanguage.ENGLISH,
    val playback: PlaybackState = PlaybackState.IDLE,
    val currentIndex: Int = -1,
    val itemCount: Int = 0,
    val speechError: Boolean = false,
)

/** Settings + engine state, as consumed by the UI. */
data class NarratorUiState(
    val settings: NarratorSettings = NarratorSettings(),
    val engine: EngineState = EngineState(),
) {
    val canSpeak: Boolean get() = engine.tts == TtsStatus.READY && !settings.muted

    /** Non-null when the *selected* language cannot be spoken. */
    val languageIssue: LanguageStatus?
        get() = engine.takeIf {
            it.tts == TtsStatus.READY &&
                it.requestedLanguage == settings.language &&
                it.languageStatus != LanguageStatus.AVAILABLE
        }?.languageStatus

    val hasIssue: Boolean
        get() = engine.tts == TtsStatus.FAILED ||
            engine.tts == TtsStatus.NO_ENGINE ||
            languageIssue != null ||
            engine.speechError
}

enum class NarrationKind { TITLE, HEADING, TEXT, IMAGE, ACTION }

/** A resolved, ready-to-speak sentence. */
data class NarrationItem(val kind: NarrationKind, val text: String)

/**
 * Text to narrate. Prefer [Res]: it is resolved in the *narrator* language at speak time,
 * which can differ from the app's UI language.
 */
sealed interface NarrationText {
    fun resolve(context: Context): String

    data class Literal(val value: String) : NarrationText {
        override fun resolve(context: Context): String = value
    }

    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : NarrationText {
        override fun resolve(context: Context): String = context.getString(id, *args.toTypedArray())
    }
}

fun narrationRes(@StringRes id: Int, vararg args: Any): NarrationText =
    NarrationText.Res(id, args.toList())
