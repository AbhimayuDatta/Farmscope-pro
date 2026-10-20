package com.example.narrator

import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Always resolves the Activity-scoped ViewModel, even inside a NavHost destination. */
@Composable
fun rememberNarratorViewModel(): NarratorViewModel {
    val activity = LocalContext.current.findActivity() as? ComponentActivity
        ?: error("NarratorScreen must be hosted in a ComponentActivity")
    return viewModel(viewModelStoreOwner = activity, factory = NarratorViewModel.Factory)
}

/**
 * Wrap each screen in this. It provides the registry, the floating control, the settings sheet,
 * optional auto-narration and the stop-on-leave / stop-on-background behavior.
 *
 * Add bottom padding (~88.dp) to scrolling content so the floating control never hides it.
 */
@Composable
fun NarratorScreen(
    screenTitle: NarrationText,
    modifier: Modifier = Modifier,
    viewModel: NarratorViewModel = rememberNarratorViewModel(),
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestState by rememberUpdatedState(uiState)

    val registry = remember { NarratorRegistry() }
    val titleKey = remember { Any() }
    SideEffect { registry.register(titleKey, Int.MIN_VALUE, NarrationKind.TITLE, screenTitle) }

    var showSettings by rememberSaveable { mutableStateOf(false) }
    // Survives rotation and process death, so a recreated screen does not re-narrate itself.
    var autoNarrated by rememberSaveable { mutableStateOf(false) }

    val actions = remember(viewModel, registry) {
        // Text is resolved in the language actually being spoken (English if Hindi data is missing).
        fun localized() = context.localizedFor(latestState.engine.effectiveLanguage.locale)
        NarratorActions(
            narrateScreen = { viewModel.narrate(registry.snapshot(localized())) },
            narrateElement = { kind, text ->
                viewModel.narrate(listOf(text.toNarrationItem(kind, localized())))
            },
        )
    }

    // Automatic narration on screen entry, only when the user enabled it.
    val autoReady = uiState.settings.autoNarrate && uiState.engine.tts == TtsStatus.READY
    LaunchedEffect(autoReady) {
        if (!autoReady || autoNarrated) return@LaunchedEffect
        // Avoid talking over TalkBack, which already reads the screen.
        val a11y = ContextCompat.getSystemService(context, AccessibilityManager::class.java)
        if (a11y?.isTouchExplorationEnabled == true) return@LaunchedEffect
        withFrameNanos { } // let children register first
        autoNarrated = true
        actions.narrateScreen()
    }

    // Leaving the screen stops speech; rotation does not (the ViewModel keeps speaking).
    DisposableEffect(viewModel) {
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.stop() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.onAppStopped()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAppResumed() }

    CompositionLocalProvider(
        LocalNarratorRegistry provides registry,
        LocalNarratorActions provides actions,
    ) {
        Box(modifier.fillMaxSize()) {
            content()

            NarratorControlBar(
                state = uiState,
                onPlayPause = {
                    when (uiState.engine.playback) {
                        PlaybackState.IDLE -> actions.narrateScreen()
                        PlaybackState.SPEAKING -> viewModel.pause()
                        PlaybackState.PAUSED -> viewModel.resume()
                    }
                },
                onStop = viewModel::stop,
                onToggleMute = { viewModel.setMuted(!uiState.settings.muted) },
                onOpenSettings = { showSettings = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )

            if (showSettings) {
                NarratorSettingsSheet(
                    state = uiState,
                    onDismiss = { showSettings = false },
                    onLanguageSelected = viewModel::setLanguage,
                    onSpeechRateChanged = viewModel::setSpeechRate,
                    onPitchChanged = viewModel::setPitch,
                    onAutoNarrateChanged = viewModel::setAutoNarrate,
                    onMutedChanged = viewModel::setMuted,
                    onRetry = viewModel::retryInitialization,
                )
            }
        }
    }
}
