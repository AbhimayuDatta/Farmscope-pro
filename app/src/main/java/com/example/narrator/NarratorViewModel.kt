package com.example.narrator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Activity-scoped so one TextToSpeech instance serves every screen and survives rotation.
 * Dependencies are plain constructor parameters, so Hilt can replace [Factory] later.
 */
class NarratorViewModel(
    private val engine: NarratorEngine,
    private val preferences: NarratorPreferences,
) : ViewModel() {

    val uiState: StateFlow<NarratorUiState> =
        combine(preferences.settings, engine.state) { settings, engineState ->
            NarratorUiState(settings, engineState)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NarratorUiState())

    init {
        engine.initialize()
        viewModelScope.launch {
            preferences.settings.collect { engine.applySettings(it) }
        }
    }

    // Playback
    fun narrate(items: List<NarrationItem>) = engine.speak(items)
    fun pause() = engine.pause()
    fun resume() = engine.resume()
    fun stop() = engine.stop()

    // Lifecycle hooks, called from NarratorScreen
    fun onAppStopped() = engine.stop()
    fun onAppResumed() = engine.refreshLanguage()

    fun retryInitialization() {
        engine.shutdown()
        engine.initialize()
    }

    // Settings
    fun setLanguage(language: NarratorLanguage) = viewModelScope.launch { preferences.setLanguage(language) }
    fun setSpeechRate(rate: Float) = viewModelScope.launch { preferences.setSpeechRate(rate) }
    fun setPitch(pitch: Float) = viewModelScope.launch { preferences.setPitch(pitch) }
    fun setAutoNarrate(enabled: Boolean) = viewModelScope.launch { preferences.setAutoNarrate(enabled) }
    fun setMuted(muted: Boolean) = viewModelScope.launch { preferences.setMuted(muted) }

    override fun onCleared() {
        engine.shutdown()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                NarratorViewModel(TtsNarratorEngine(app), DataStoreNarratorPreferences(app))
            }
        }
    }
}
