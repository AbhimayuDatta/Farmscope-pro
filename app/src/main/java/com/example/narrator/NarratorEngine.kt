package com.example.narrator

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Abstraction over speech output so the ViewModel can be unit-tested with a fake. */
interface NarratorEngine {
    val state: StateFlow<EngineState>
    fun initialize()
    fun applySettings(settings: NarratorSettings)
    /** Replaces anything currently being spoken. */
    fun speak(items: List<NarrationItem>)
    fun pause()
    fun resume()
    fun stop()
    /** Re-checks voice data, e.g. after the user returns from the installer. */
    fun refreshLanguage()
    fun shutdown()
}

/**
 * [TextToSpeech]-backed engine.
 *
 * Utterance ids are "<session>:<index>". Every stop/pause/new speak bumps the session so late
 * callbacks from flushed utterances are ignored.
 */
class TtsNarratorEngine(context: Context) : NarratorEngine {

    private val appContext = context.applicationContext
    private val audioManager = ContextCompat.getSystemService(appContext, AudioManager::class.java)

    private val _state = MutableStateFlow(EngineState())
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var settings = NarratorSettings()
    @Volatile private var items: List<NarrationItem> = emptyList()
    private val session = AtomicInteger(0)
    private var focusRequest: AudioFocusRequestCompat? = null

    override fun initialize() {
        if (tts != null) return
        _state.update { it.copy(tts = TtsStatus.INITIALIZING) }
        val instance = TextToSpeech(appContext) { status -> onInit(status) }
        // On some devices the init callback fires synchronously on failure, before `tts` is set.
        if (_state.value.tts == TtsStatus.INITIALIZING) tts = instance else instance.shutdown()
    }

    private fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            fail(TtsStatus.FAILED)
            return
        }
        val engine = tts ?: return
        if (engine.engines.isNullOrEmpty()) {
            fail(TtsStatus.NO_ENGINE)
            return
        }
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        engine.setOnUtteranceProgressListener(progressListener)
        _state.update { it.copy(tts = TtsStatus.READY) }
        applyVoiceParams(engine)
        applyLanguage()
    }

    private fun fail(status: TtsStatus) {
        tts?.shutdown()
        tts = null
        _state.update {
            it.copy(tts = status, playback = PlaybackState.IDLE, currentIndex = -1, itemCount = 0)
        }
    }

    override fun applySettings(settings: NarratorSettings) {
        val old = this.settings
        this.settings = settings
        val engine = tts
        if (engine != null && _state.value.tts == TtsStatus.READY) {
            applyVoiceParams(engine)
            if (old.language != settings.language) {
                stop() // queued sentences were resolved in the old language
                applyLanguage()
            }
        }
        if (settings.muted && !old.muted) stop()
    }

    private fun applyVoiceParams(engine: TextToSpeech) {
        // Rate/pitch apply to utterances queued after the change, not to the current one.
        engine.setSpeechRate(settings.speechRate)
        engine.setPitch(settings.pitch)
    }

    override fun refreshLanguage() = applyLanguage()

    private fun applyLanguage() {
        val engine = tts ?: return
        if (_state.value.tts != TtsStatus.READY) return
        val requested = settings.language
        val status = engine.statusOf(requested)
        // Fall back safely to English when the requested voice data is missing.
        val effective = if (status == LanguageStatus.AVAILABLE) requested else NarratorLanguage.ENGLISH
        val usable = engine.statusOf(effective) == LanguageStatus.AVAILABLE &&
            engine.setLanguage(effective.locale) >= TextToSpeech.LANG_AVAILABLE
        if (!usable) {
            fail(TtsStatus.FAILED)
            return
        }
        _state.update {
            it.copy(requestedLanguage = requested, languageStatus = status, effectiveLanguage = effective)
        }
    }

    private fun TextToSpeech.statusOf(language: NarratorLanguage): LanguageStatus =
        when (isLanguageAvailable(language.locale)) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> LanguageStatus.AVAILABLE
            TextToSpeech.LANG_MISSING_DATA -> LanguageStatus.MISSING_DATA
            else -> LanguageStatus.NOT_SUPPORTED
        }

    override fun speak(items: List<NarrationItem>) {
        if (tts == null || _state.value.tts != TtsStatus.READY || settings.muted || items.isEmpty()) return
        this.items = items
        startFrom(0)
    }

    private fun startFrom(index: Int) {
        val engine = tts ?: return
        val list = items
        if (index !in list.indices) {
            finish()
            return
        }
        if (!requestFocus()) { // e.g. a phone call is active
            _state.update {
                it.copy(playback = PlaybackState.IDLE, currentIndex = -1, itemCount = 0, speechError = true)
            }
            return
        }
        val id = session.incrementAndGet()
        _state.update {
            it.copy(
                playback = PlaybackState.SPEAKING,
                currentIndex = index,
                itemCount = list.size,
                speechError = false,
            )
        }
        val maxLength = TextToSpeech.getMaxSpeechInputLength()
        for (i in index until list.size) {
            // First sentence flushes whatever was playing; the rest queue behind it.
            val mode = if (i == index) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val result = engine.speak(list[i].text.take(maxLength), mode, null, "$id:$i")
            if (result == TextToSpeech.ERROR) {
                session.incrementAndGet()
                abandonFocus()
                _state.update {
                    it.copy(playback = PlaybackState.IDLE, currentIndex = -1, itemCount = 0, speechError = true)
                }
                return
            }
        }
    }

    /** TextToSpeech has no native pause: stop, remember the sentence, replay it on resume. */
    override fun pause() {
        if (_state.value.playback != PlaybackState.SPEAKING) return
        session.incrementAndGet()
        tts?.stop()
        abandonFocus()
        _state.update { it.copy(playback = PlaybackState.PAUSED) }
    }

    override fun resume() {
        if (_state.value.playback != PlaybackState.PAUSED) return
        startFrom(_state.value.currentIndex.coerceAtLeast(0))
    }

    override fun stop() {
        session.incrementAndGet()
        tts?.stop()
        abandonFocus()
        items = emptyList()
        _state.update { it.copy(playback = PlaybackState.IDLE, currentIndex = -1, itemCount = 0) }
    }

    override fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }

    private fun finish() {
        abandonFocus()
        items = emptyList()
        _state.update { it.copy(playback = PlaybackState.IDLE, currentIndex = -1, itemCount = 0) }
    }

    // --- Audio focus: duck other audio while speaking, stop if something else takes over. ---

    private fun requestFocus(): Boolean {
        val manager = audioManager ?: return true
        val request = focusRequest ?: AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setUsage(AudioAttributesCompat.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener { change -> onFocusChange(change) }
            .build()
            .also { focusRequest = it }
        return AudioManagerCompat.requestAudioFocus(manager, request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        val manager = audioManager ?: return
        focusRequest?.let { AudioManagerCompat.abandonAudioFocusRequest(manager, it) }
    }

    private fun onFocusChange(change: Int) {
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause()
    }

    // --- Utterance callbacks (binder thread) ---

    private fun indexOf(utteranceId: String?): Int? {
        val parts = utteranceId?.split(':') ?: return null
        if (parts.size != 2 || parts[0].toIntOrNull() != session.get()) return null // stale
        return parts[1].toIntOrNull()
    }

    private fun onUtteranceError(utteranceId: String?) {
        if (indexOf(utteranceId) == null) return
        session.incrementAndGet()
        abandonFocus()
        _state.update {
            it.copy(playback = PlaybackState.IDLE, currentIndex = -1, itemCount = 0, speechError = true)
        }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val index = indexOf(utteranceId) ?: return
            _state.update { it.copy(playback = PlaybackState.SPEAKING, currentIndex = index) }
        }

        override fun onDone(utteranceId: String?) {
            val index = indexOf(utteranceId) ?: return
            if (index == items.lastIndex) finish()
        }

        // Abstract in the base class; the errorCode overload below is the one the system calls.
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = onUtteranceError(utteranceId)

        override fun onError(utteranceId: String?, errorCode: Int) = onUtteranceError(utteranceId)
    }
}
