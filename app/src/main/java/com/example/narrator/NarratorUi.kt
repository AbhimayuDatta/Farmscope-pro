package com.example.narrator

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R

/** Floating start/pause/stop/mute/settings control. Every state is conveyed by icon *and* label. */
@Composable
fun NarratorControlBar(
    state: NarratorUiState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = state.engine
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (engine.tts == TtsStatus.INITIALIZING) {
                val initializing = stringResource(R.string.narrator_cd_initializing)
                Box(
                    Modifier.size(48.dp).semantics { contentDescription = initializing },
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
            } else {
                val (icon, label) = when (engine.playback) {
                    PlaybackState.IDLE -> Icons.Filled.PlayArrow to R.string.narrator_cd_play
                    PlaybackState.SPEAKING -> Icons.Filled.Pause to R.string.narrator_cd_pause
                    PlaybackState.PAUSED -> Icons.Filled.PlayArrow to R.string.narrator_cd_resume
                }
                IconButton(onClick = onPlayPause, enabled = state.canSpeak) {
                    Icon(icon, contentDescription = stringResource(label))
                }
            }

            IconButton(onClick = onStop, enabled = engine.playback != PlaybackState.IDLE) {
                Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.narrator_cd_stop))
            }

            IconButton(onClick = onToggleMute) {
                if (state.settings.muted) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = stringResource(R.string.narrator_cd_unmute),
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.narrator_cd_mute),
                    )
                }
            }

            IconButton(onClick = onOpenSettings) {
                // A different icon shape (not just a color) signals that something needs attention.
                if (state.hasIssue) {
                    Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.narrator_cd_settings_issue))
                } else {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.narrator_cd_settings))
                }
            }
        }
    }
}

/** Small "read this aloud" affordance for element narration. */
@Composable
fun NarrateElementButton(
    text: NarrationText,
    modifier: Modifier = Modifier,
    kind: NarrationKind = NarrationKind.TEXT,
) {
    val actions = LocalNarratorActions.current ?: return
    IconButton(onClick = { actions.narrateElement(kind, text) }, modifier = modifier) {
        Icon(
            Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = stringResource(R.string.narrator_cd_narrate_element),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NarratorSettingsSheet(
    state: NarratorUiState,
    onDismiss: () -> Unit,
    onLanguageSelected: (NarratorLanguage) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onAutoNarrateChanged: (Boolean) -> Unit,
    onMutedChanged: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    var installFailed by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.narrator_settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )

            NarratorStatusBanner(
                state = state,
                installFailed = installFailed,
                onRetry = onRetry,
                onInstallVoiceData = { installFailed = !context.openTtsVoiceDataInstaller() },
                onOpenTtsSettings = { installFailed = !context.openTtsVoiceDataInstaller() },
            )

            Text(
                text = stringResource(R.string.narrator_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NarratorLanguage.entries.forEach { language ->
                    val selected = state.settings.language == language
                    FilterChip(
                        selected = selected,
                        onClick = { onLanguageSelected(language) },
                        label = { Text(stringResource(language.labelRes())) },
                        // Check icon + selected semantics: selection is not color-only.
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else null,
                    )
                }
            }

            LabeledSlider(R.string.narrator_speech_rate, state.settings.speechRate, onSpeechRateChanged)
            LabeledSlider(R.string.narrator_pitch, state.settings.pitch, onPitchChanged)

            SwitchRow(R.string.narrator_auto_narrate, state.settings.autoNarrate, onAutoNarrateChanged)
            SwitchRow(R.string.narrator_mute, state.settings.muted, onMutedChanged)
        }
    }
}

private class BannerAction(@StringRes val label: Int, val onClick: () -> Unit)

@Composable
private fun NarratorStatusBanner(
    state: NarratorUiState,
    installFailed: Boolean,
    onRetry: () -> Unit,
    onInstallVoiceData: () -> Unit,
    onOpenTtsSettings: () -> Unit,
) {
    val engine = state.engine
    when {
        engine.tts == TtsStatus.INITIALIZING ->
            Banner(R.string.narrator_status_initializing, showProgress = true)

        engine.tts == TtsStatus.FAILED -> Banner(
            R.string.narrator_status_failed,
            actions = listOf(
                BannerAction(R.string.narrator_action_retry, onRetry),
                BannerAction(R.string.narrator_action_tts_settings, onOpenTtsSettings),
            ),
            extra = if (installFailed) R.string.narrator_install_failed else null,
        )

        engine.tts == TtsStatus.NO_ENGINE -> Banner(
            R.string.narrator_status_no_engine,
            actions = listOf(
                BannerAction(R.string.narrator_action_retry, onRetry),
                BannerAction(R.string.narrator_action_tts_settings, onOpenTtsSettings),
            ),
            extra = if (installFailed) R.string.narrator_install_failed else null,
        )

        state.languageIssue == LanguageStatus.MISSING_DATA -> Banner(
            R.string.narrator_status_hindi_missing,
            actions = listOf(BannerAction(R.string.narrator_action_install_hindi, onInstallVoiceData)),
            extra = if (installFailed) R.string.narrator_install_failed else null,
        )

        state.languageIssue == LanguageStatus.NOT_SUPPORTED ->
            Banner(R.string.narrator_status_hindi_unsupported)

        engine.speechError -> Banner(R.string.narrator_status_speech_error)
    }
}

@Composable
private fun Banner(
    @StringRes message: Int,
    showProgress: Boolean = false,
    actions: List<BannerAction> = emptyList(),
    @StringRes extra: Int? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        // Polite live region: TalkBack announces status changes without interrupting.
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showProgress) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Info, contentDescription = null)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
                if (extra != null) {
                    Text(stringResource(extra), style = MaterialTheme.typography.bodySmall)
                }
                actions.forEach { action ->
                    TextButton(onClick = action.onClick) { Text(stringResource(action.label)) }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    @StringRes label: Int,
    value: Float,
    onValueChangeFinished: (Float) -> Unit,
) {
    // Keep the thumb responsive locally; persist only when the drag ends.
    var local by remember(value) { mutableFloatStateOf(value) }
    val labelText = stringResource(label)
    val valueText = stringResource(R.string.narrator_value_multiplier, local)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(labelText, style = MaterialTheme.typography.titleMedium)
            Text(valueText, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onValueChangeFinished(local) },
            valueRange = NarratorSettings.MIN_VALUE..NarratorSettings.MAX_VALUE,
            steps = 14, // 0.1 increments
            modifier = Modifier.semantics {
                contentDescription = labelText
                stateDescription = valueText
            },
        )
    }
}

@Composable
private fun SwitchRow(@StringRes label: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null) // the row handles the toggle
    }
}

@StringRes
private fun NarratorLanguage.labelRes(): Int = when (this) {
    NarratorLanguage.ENGLISH -> R.string.narrator_language_english
    NarratorLanguage.HINDI -> R.string.narrator_language_hindi
}
