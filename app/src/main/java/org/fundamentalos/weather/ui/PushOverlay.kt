package org.fundamentalos.weather.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/** How long a screen takes to slide in or out. */
private const val SlideMillis = 320

/** The fraction of the screen the layer underneath drifts while one is pushed over it. */
private const val UnderlyingParallax = 4f

private val SlideSpec = tween<Float>(SlideMillis, easing = FastOutSlowInEasing)

/**
 * Whether the screen this is read from is completely hidden behind a pushed one. Read it from a
 * frame loop rather than from composition: a screen that nobody can see should not ask for frames.
 */
val LocalScreenCovered = staticCompositionLocalOf<() -> Boolean> { { false } }

/** How deep in the back stack the screen this is read from sits: the home page is 0. */
val LocalScreenDepth = staticCompositionLocalOf { 0 }

/** One pushed screen's travel: 0 is fully off-screen, 1 is covering what is under it. */
@Stable
class PushSlide(initialProgress: Float = 0f) {
    val progress = Animatable(initialProgress)
    var fromLeft by mutableStateOf(false)
}

/** The travel of every pushed screen, indexed by its depth in the back stack. */
@Stable
class PushMotion {
    private val slides = mutableStateMapOf<Int, PushSlide>()

    /** The depth of the screen on top; only that one answers the back gesture. */
    var topDepth by mutableIntStateOf(0)

    /** Each stable stack entry owns the slide at its depth. */
    fun slideAt(depth: Int): PushSlide = slides.getOrPut(depth) { PushSlide() }

    /**
     * Marks a depth as already arrived. A back stack restored after the activity was recreated
     * names screens that were open before, and they must appear where the user left them rather
     * than sliding in over the home screen again.
     */
    fun settle(depth: Int) {
        slides.getOrPut(depth) { PushSlide(initialProgress = 1f) }
    }

    fun slideOrNull(depth: Int): PushSlide? = slides[depth]

    /** Whether the screen at [depth] is completely hidden by the one above it. */
    fun isCovered(depth: Int): Boolean = (slideOrNull(depth + 1)?.progress?.value ?: 0f) >= 1f
}

/**
 * Drifts the screen at [depth] a quarter of the way out while the screen above it slides in, and
 * stops drawing it once that screen covers it completely. Both are draw-phase reads, so a push
 * costs redraws and never a recomposition — and components that place themselves against the
 * window, like the banner and the bottom bar, travel with the screen instead of correcting for it.
 */
fun Modifier.pushedUnder(motion: PushMotion, depth: Int): Modifier = this
    .graphicsLayer {
        val above = motion.slideOrNull(depth + 1) ?: return@graphicsLayer
        val direction = if (above.fromLeft) 1f else -1f
        translationX = direction * size.width * above.progress.value / UnderlyingParallax
        // Keep the recorded content during a slide; observing progress in
        // drawWithContent invalidates the entire underlying screen every frame.
        alpha = if (above.progress.value >= 1f) 0f else 1f
    }
    // The screen underneath is still alive, so it must ignore touches meant for the one above it.
    // Consuming in the initial pass, before its own children see the event, is what stops it.
    .pointerInput(motion, depth) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (motion.topDepth > depth) {
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }

/** A stable sibling of the home screen: no scene reparenting on push/pop. */
@Composable
fun PushStackEntry(
    depth: Int,
    motion: PushMotion,
    fromLeft: Boolean,
    onRemoved: () -> Unit,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    val slide = androidx.compose.runtime.remember(motion, depth) { motion.slideAt(depth) }
    androidx.compose.runtime.SideEffect { slide.fromLeft = fromLeft }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var closing by androidx.compose.runtime.remember { mutableStateOf(false) }
    val remove by androidx.compose.runtime.rememberUpdatedState(onRemoved)
    val close: () -> Unit = {
        if (!closing && motion.topDepth == depth) {
            closing = true
            scope.launch {
                slide.progress.animateTo(0f, SlideSpec)
                remove()
            }
        }
    }
    LaunchedEffect(slide) {
        withFrameNanos { }
        withFrameNanos { }
        if (!closing && slide.progress.value < 1f) slide.progress.animateTo(1f, SlideSpec)
    }
    PredictiveBackHandler(enabled = motion.topDepth == depth && !closing) { events ->
        try {
            events.collect { slide.progress.snapTo(1f - it.progress) }
            closing = true
            // Commit: finish the push-out from wherever the gesture was released, with the same
            // animation the back arrow uses. A bare snapTo(0) here made a half-swipe release jump
            // to the home screen with no transition. Run it in the composition scope, since the
            // back-handler coroutine is torn down as this lambda returns.
            scope.launch {
                slide.progress.animateTo(0f, SlideSpec)
                remove()
            }
        } catch (_: CancellationException) {
            // Cancel: the back-handler coroutine is already cancelled here, so the recovery
            // animation must run in the composition scope — a suspend on this job throws first.
            scope.launch { slide.progress.animateTo(1f, SlideSpec) }
        }
    }
    Box(Modifier.fillMaxSize().zIndex(depth.toFloat()).graphicsLayer {
        translationX = (if (slide.fromLeft) -1f else 1f) * size.width * (1f - slide.progress.value)
    }.pushedUnder(motion, depth)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalScreenDepth provides depth) { content(close) }
    }
}
