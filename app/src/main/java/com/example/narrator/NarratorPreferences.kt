package com.example.narrator

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.narratorDataStore: DataStore<Preferences> by preferencesDataStore(name = "narrator_settings")

interface NarratorPreferences {
    val settings: Flow<NarratorSettings>
    suspend fun setLanguage(language: NarratorLanguage)
    suspend fun setSpeechRate(rate: Float)
    suspend fun setPitch(pitch: Float)
    suspend fun setAutoNarrate(enabled: Boolean)
    suspend fun setMuted(muted: Boolean)
}

class DataStoreNarratorPreferences(context: Context) : NarratorPreferences {

    private val store = context.applicationContext.narratorDataStore

    override val settings: Flow<NarratorSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            NarratorSettings(
                language = NarratorLanguage.fromTag(prefs[Keys.LANGUAGE]),
                speechRate = (prefs[Keys.RATE] ?: 1f).clampToRange(),
                pitch = (prefs[Keys.PITCH] ?: 1f).clampToRange(),
                autoNarrate = prefs[Keys.AUTO_NARRATE] ?: false,
                muted = prefs[Keys.MUTED] ?: false,
            )
        }

    override suspend fun setLanguage(language: NarratorLanguage) {
        store.edit { it[Keys.LANGUAGE] = language.tag }
    }

    override suspend fun setSpeechRate(rate: Float) {
        store.edit { it[Keys.RATE] = rate.clampToRange() }
    }

    override suspend fun setPitch(pitch: Float) {
        store.edit { it[Keys.PITCH] = pitch.clampToRange() }
    }

    override suspend fun setAutoNarrate(enabled: Boolean) {
        store.edit { it[Keys.AUTO_NARRATE] = enabled }
    }

    override suspend fun setMuted(muted: Boolean) {
        store.edit { it[Keys.MUTED] = muted }
    }

    private fun Float.clampToRange() = coerceIn(NarratorSettings.MIN_VALUE, NarratorSettings.MAX_VALUE)

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_tag")
        val RATE = floatPreferencesKey("speech_rate")
        val PITCH = floatPreferencesKey("pitch")
        val AUTO_NARRATE = booleanPreferencesKey("auto_narrate")
        val MUTED = booleanPreferencesKey("muted")
    }
}
