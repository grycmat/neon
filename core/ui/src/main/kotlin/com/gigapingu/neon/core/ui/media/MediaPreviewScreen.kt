package com.gigapingu.neon.core.ui.media

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.gigapingu.neon.core.model.MediaAttachment
import com.gigapingu.neon.core.model.MediaType
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.StatusActionService
import com.gigapingu.neon.core.ui.media.VideoPlayer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun MediaPreviewScreen(
    attachments: List<MediaAttachment>,
    startIndex: Int = 0,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, attachments.lastIndex.coerceAtLeast(0)),
    ) { attachments.size }

    // Shared across pages so a dismiss-drag on the current page fades the whole
    // screen's background; idle pages never touch this (see MediaPreviewPage).
    var backgroundAlpha by remember { mutableFloatStateOf(1f) }

    var showAltText by remember { mutableStateOf(false) }
    val currentAttachment = attachments.getOrNull(pagerState.currentPage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            MediaPreviewPage(
                attachment = attachments[page],
                onBackgroundAlphaChange = { backgroundAlpha = it },
            )
        }

        // Floating Close Button in top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 48.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { Navigator.back() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (currentAttachment?.altText?.isNotBlank() == true) {
                AltTextBadge(onClick = { showAltText = true })
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { currentAttachment?.let { StatusActionService.saveMedia(it) } },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Save to device",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showAltText && currentAttachment != null) {
        AltTextSheet(currentAttachment.altText) { showAltText = false }
    }
}

/**
 * One pager page: pinch-zoom/pan + swipe-down-to-dismiss for a single
 * attachment. At scale == 1f a single-finger drag that's more horizontal than
 * vertical is ceded (left unconsumed) to the enclosing [HorizontalPager] so it
 * pages between attachments instead of being treated as a dismiss; a
 * vertical-dominant drag keeps dismissing as before. Once zoomed in
 * (scale > 1f) every drag is consumed here to pan the image, which naturally
 * blocks the pager from swiping.
 */
@Composable
private fun MediaPreviewPage(
    attachment: MediaAttachment,
    onBackgroundAlphaChange: (Float) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableStateOf(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    val density = LocalDensity.current
    val dismissThreshold = remember { with(density) { 150.dp.toPx() } }

    val backgroundAlpha by remember(scale) {
        derivedStateOf {
            if (scale == 1f) {
                val distance = offset.value.getDistance()
                val fraction = (distance / dismissThreshold).coerceIn(0f, 1f)
                1f - (fraction * 0.7f)
            } else {
                1f
            }
        }
    }
    SideEffect { onBackgroundAlphaChange(backgroundAlpha) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformAndDismissGestures(
                        scaleProvider = { scale },
                        onGesture = { centroid, pan, zoom ->
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(1f, 5f)

                            if (zoom != 1f) {
                                val scaleFactor = newScale / oldScale
                                val relativeCentroid = centroid - Offset(screenWidth / 2f, screenHeight / 2f)
                                val targetOffset = offset.value + (relativeCentroid - offset.value) * (1f - scaleFactor)
                                scale = newScale
                                scope.launch { offset.snapTo(targetOffset) }
                            } else {
                                scope.launch { offset.snapTo(offset.value + pan) }
                            }
                        },
                        onGestureEnd = {
                            if (scale == 1f) {
                                val distance = offset.value.getDistance()
                                if (distance > dismissThreshold) {
                                    Navigator.back()
                                } else {
                                    scope.launch {
                                        offset.animateTo(
                                            Offset.Zero,
                                            spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            } else {
                                val maxOffsetX = (screenWidth * (scale - 1f)) / 2f
                                val maxOffsetY = (screenHeight * (scale - 1f)) / 2f
                                val clampedX = offset.value.x.coerceIn(-maxOffsetX, maxOffsetX)
                                val clampedY = offset.value.y.coerceIn(-maxOffsetY, maxOffsetY)
                                scope.launch {
                                    offset.animateTo(
                                        Offset(clampedX, clampedY),
                                        spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    )
                }
        ) {
            val isVideo = attachment.isPlayable
            val isGifv = attachment.type == MediaType.Gifv
            if (isVideo) {
                VideoPlayer(
                    url = attachment.url,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) },
                    muted = isGifv,
                    looping = isGifv,
                    useController = !isGifv,
                )
            } else {
                AsyncImage(
                    // Start from the already-cached grid thumbnail so the image
                    // never flashes while the full-size version loads.
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(attachment.url)
                        .placeholderMemoryCacheKey(attachment.preview)
                        .build(),
                    contentDescription = attachment.altText.ifEmpty { "Media Preview" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }
    }
}

private suspend fun PointerInputScope.detectTransformAndDismissGestures(
    scaleProvider: () -> Float,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onGestureEnd: () -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        // Once a single-finger drag at scale 1f turns out to be more
        // horizontal than vertical, stop consuming so the pager can swipe.
        var cedeToPager = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                        if (event.changes.size == 1 && scaleProvider() == 1f && abs(pan.x) > abs(pan.y)) {
                            cedeToPager = true
                        }
                    }
                }

                if (pastTouchSlop && !cedeToPager) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        onGesture(centroid, panChange, zoomChange)
                    }
                    event.changes.forEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        if (!cedeToPager) {
            onGestureEnd()
        }
    }
}
