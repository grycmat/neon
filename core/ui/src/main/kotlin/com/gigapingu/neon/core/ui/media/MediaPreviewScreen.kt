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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.media.VideoPlayer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun MediaPreviewScreen(
    url: String,
    previewUrl: String? = null,
    type: String? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableStateOf(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    val density = LocalDensity.current
    val dismissThreshold = remember { with(density) { 150.dp.toPx() } }

    val backgroundAlpha = remember(scale) {
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha.value))
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformAndDismissGestures(
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
            val isVideo = type == "video" || type == "gifv"
            val isGifv = type == "gifv"
            if (isVideo) {
                VideoPlayer(
                    url = url,
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
                        .data(url)
                        .placeholderMemoryCacheKey(previewUrl)
                        .build(),
                    contentDescription = "Media Preview",
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
    }
}

private suspend fun PointerInputScope.detectTransformAndDismissGestures(
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onGestureEnd: () -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
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
                    }
                }

                if (pastTouchSlop) {
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

        onGestureEnd()
    }
}
