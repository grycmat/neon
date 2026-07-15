package com.gigapingu.neon.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gigapingu.neon.core.designsystem.theme.NeonAccents
import com.gigapingu.neon.core.designsystem.theme.NeonDims
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

/** Glass surface card — rgba white fill, hairline border, soft shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 12.dp),
    radius: Dp = NeonDims.RadiusCard,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = NeonTheme.palette
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .shadow(elevation = 14.dp, shape = shape, ambientColor = palette.shadow, spotColor = palette.shadow)
            .clip(shape)
            .background(if (highlighted) palette.surfaceHi else palette.surface)
            .border(1.dp, if (highlighted) palette.borderStrong else palette.border, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
    ) {
        content()
    }
}

/** Kicker label — uppercase, letterspaced, cyan. */
@Composable
fun NeonLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(),
        style = NeonTheme.type.labelMedium,
        color = color ?: NeonTheme.palette.label,
        modifier = modifier,
    )
}

/** Primary gradient pill button with purple glow (design CTA). */
@Composable
fun GradientButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    height: Dp = NeonDims.ButtonHeight,
    trailingArrow: Boolean = false,
    busy: Boolean = false,
) {
    val palette = NeonTheme.palette
    val enabled = onClick != null && !busy
    val shape = RoundedCornerShape(NeonDims.RadiusButton)
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else .55f)
            .height(height)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = NeonAccents.Purple.copy(alpha = .45f),
                spotColor = NeonAccents.Purple.copy(alpha = .45f),
            )
            .clip(shape)
            .background(palette.gradient)
            .clickable(enabled = enabled) { onClick?.invoke() }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.4.dp,
                color = palette.onGradient,
            )
        } else {
            Text(
                text = label,
                style = NeonTheme.type.labelLarge.copy(fontSize = 16.sp),
                color = palette.onGradient,
            )
            if (trailingArrow) {
                Spacer(Modifier.width(9.dp))
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = palette.onGradient,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

/** Secondary glass pill button. */
@Composable
fun GlassButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    height: Dp = NeonDims.ButtonHeight,
    tinted: Boolean = false,
) {
    val palette = NeonTheme.palette
    val shape = RoundedCornerShape(NeonDims.RadiusButton)
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (tinted) palette.cyan.copy(alpha = .08f) else palette.surface)
            .border(1.dp, if (tinted) palette.cyan.copy(alpha = .3f) else palette.borderStrong, shape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = NeonTheme.type.labelLarge,
            color = if (tinted) palette.cyan else palette.text,
        )
    }
}

/** Small round glass icon button (back / overflow / actions). 40×40 within a ≥44dp hit target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tinted: Boolean = false,
    contentDescription: String? = null,
) {
    val palette = NeonTheme.palette
    val shape = RoundedCornerShape(NeonDims.RadiusIcon)
    Box(
        modifier = modifier
            .size(NeonDims.MinTap)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onClick != null,
            ) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(NeonDims.IconButton)
                .clip(shape)
                .background(if (tinted) palette.cyan.copy(alpha = .08f) else palette.surface)
                .border(1.dp, if (tinted) palette.cyan.copy(alpha = .3f) else palette.border, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (tinted) palette.cyan else palette.text,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/** Glass input container with an uppercase micro-label (design form field). */
@Composable
fun GlassField(
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    val palette = NeonTheme.palette
    val shape = RoundedCornerShape(NeonDims.RadiusField)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .padding(start = 15.dp, top = 10.dp, end = 15.dp, bottom = 10.dp),
    ) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = NeonTheme.type.labelSmall,
                color = palette.textDim,
            )
            Spacer(Modifier.height(4.dp))
        }
        content()
    }
}
