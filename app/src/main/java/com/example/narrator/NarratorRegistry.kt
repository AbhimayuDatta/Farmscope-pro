package com.example.narrator

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.example.R

/**
 * Collects the narratable elements of one screen. Reading order is `order`, then the order in
 * which elements first entered composition (which matches visual order in a normal Column).
 */
class NarratorRegistry {
    private class Entry(val seq: Long, val order: Int, val kind: NarrationKind, val text: NarrationText)

    private val entries = LinkedHashMap<Any, Entry>()
    private var nextSeq = 0L

    fun register(key: Any, order: Int, kind: NarrationKind, text: NarrationText) {
        val seq = entries[key]?.seq ?: nextSeq++ // keep position when text updates
        entries[key] = Entry(seq, order, kind, text)
    }

    fun unregister(key: Any) {
        entries.remove(key)
    }

    /** Resolves every entry in [localizedContext]'s language, in reading order. */
    fun snapshot(localizedContext: Context): List<NarrationItem> =
        entries.values
            .sortedWith(compareBy<Entry> { it.order }.thenBy { it.seq })
            .map { it.text.toNarrationItem(it.kind, localizedContext) }
}

fun NarrationText.toNarrationItem(kind: NarrationKind, localizedContext: Context): NarrationItem {
    val raw = resolve(localizedContext)
    val spoken = when (kind) {
        NarrationKind.TITLE -> localizedContext.getString(R.string.narrator_screen_format, raw)
        NarrationKind.HEADING -> localizedContext.getString(R.string.narrator_heading_format, raw)
        NarrationKind.TEXT -> raw
        NarrationKind.IMAGE -> localizedContext.getString(R.string.narrator_role_image, raw)
        NarrationKind.ACTION -> localizedContext.getString(R.string.narrator_role_button, raw)
    }
    return NarrationItem(kind, spoken)
}

/** Actions exposed to descendants of [NarratorScreen]. */
class NarratorActions(
    val narrateScreen: () -> Unit,
    val narrateElement: (NarrationKind, NarrationText) -> Unit,
)

val LocalNarratorRegistry = compositionLocalOf<NarratorRegistry?> { null }
val LocalNarratorActions = staticCompositionLocalOf<NarratorActions?> { null }

/**
 * Marks an element as part of the screen narration. Skip it for decorative content.
 * Does nothing outside a [NarratorScreen]. Returns the modifier unchanged: the element keeps
 * its own accessibility semantics.
 */
@Composable
fun Modifier.narratable(
    text: NarrationText,
    kind: NarrationKind = NarrationKind.TEXT,
    order: Int = 0,
): Modifier {
    val registry = LocalNarratorRegistry.current ?: return this
    val key = remember { Any() }
    SideEffect { registry.register(key, order, kind, text) }
    DisposableEffect(registry, key) { onDispose { registry.unregister(key) } }
    return this
}
