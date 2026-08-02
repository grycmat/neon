package com.gigapingu.neon.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.SuccessResult
import com.gigapingu.neon.core.designsystem.theme.NeonPalette
import com.gigapingu.neon.core.model.Account
import kotlin.math.roundToInt

/**
 * Builds the one bitmap each widget row needs: the account avatar, circle-cropped, with the
 * notification-type badge composited into its bottom-right corner.
 *
 * The badge is baked into the bitmap rather than stacked as a second Glance `Image` because a
 * Glance `Box` applies a single `contentAlignment` to *all* of its children — there is no
 * per-child alignment to offset a badge with. Drawing it here also keeps each row down to one
 * RemoteViews bitmap, which matters: the whole widget tree has to fit in a Binder transaction.
 */
internal object WidgetAvatars {

    /** Rendered avatar edge, in dp. */
    const val SIZE_DP = 40

    /**
     * Hard pixel cap. Every row ships its bitmap inside the RemoteViews parcel, which has to clear
     * the ~1MB Binder transaction limit, so at `NotificationWidgetRepository.MAX_ROWS` rows this
     * keeps the total around 400KB. On xxhdpi it means a ~1.2x upscale of the avatar; honouring
     * the real density there would nearly double the budget for a barely visible gain.
     */
    private const val MAX_PX = 100

    suspend fun render(
        context: Context,
        account: Account,
        look: NotificationLook,
        palette: NeonPalette,
    ): Bitmap? = runCatching {
        val size = (SIZE_DP * context.resources.displayMetrics.density)
            .roundToInt()
            .coerceAtMost(MAX_PX)
        val avatarEdge = size * 0.84f

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawAvatar(context, account, canvas, paint, avatarEdge, palette)
        drawBadge(context, canvas, paint, size.toFloat(), look, palette)
        bitmap
    }.getOrNull()

    private suspend fun drawAvatar(
        context: Context,
        account: Account,
        canvas: Canvas,
        paint: Paint,
        edge: Float,
        palette: NeonPalette,
    ) {
        val radius = edge / 2f
        val source = loadAvatar(context, account.avatar, edge.roundToInt())

        paint.shader = if (source != null) {
            // Scale the (square-ish, but not guaranteed square) source to cover the circle.
            val scale = edge / minOf(source.width, source.height).toFloat()
            val matrix = Matrix().apply {
                setScale(scale, scale)
                postTranslate(
                    (edge - source.width * scale) / 2f,
                    (edge - source.height * scale) / 2f,
                )
            }
            BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(matrix)
            }
        } else {
            // Same pink→purple fallback NeonAvatar paints when an account has no avatar.
            LinearGradient(
                0f, 0f, edge, edge,
                palette.accentPink.toArgb(),
                palette.accentPurple.toArgb(),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(radius, radius, radius, paint)
        paint.shader = null

        // Hairline border, as on the in-app avatar.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = palette.border.toArgb()
        canvas.drawCircle(radius, radius, radius - 0.5f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawBadge(
        context: Context,
        canvas: Canvas,
        paint: Paint,
        size: Float,
        look: NotificationLook,
        palette: NeonPalette,
    ) {
        val diameter = size * 0.44f
        val radius = diameter / 2f
        val centerX = size - radius
        val centerY = size - radius

        paint.color = palette.surfaceSolid.toArgb()
        canvas.drawCircle(centerX, centerY, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = look.accent.copy(alpha = .5f).toArgb()
        canvas.drawCircle(centerX, centerY, radius - 0.5f, paint)
        paint.style = Paint.Style.FILL

        val icon = ContextCompat.getDrawable(context, look.icon)?.mutate() ?: return
        val inset = diameter * 0.26f
        icon.setTint(look.accent.toArgb())
        icon.setBounds(
            (centerX - radius + inset).roundToInt(),
            (centerY - radius + inset).roundToInt(),
            (centerX + radius - inset).roundToInt(),
            (centerY + radius - inset).roundToInt(),
        )
        icon.draw(canvas)
    }

    private suspend fun loadAvatar(context: Context, url: String, edgePx: Int): Bitmap? {
        if (url.isEmpty()) return null
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(edgePx, edgePx)
            // Hardware bitmaps can't be read back by a software Canvas, and RemoteViews can't
            // marshal them either.
            .allowHardware(false)
            .build()
        val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()
        return ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
    }
}
