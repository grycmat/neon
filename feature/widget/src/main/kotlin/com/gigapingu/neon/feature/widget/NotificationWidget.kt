package com.gigapingu.neon.feature.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gigapingu.neon.core.designsystem.theme.NeonPalette
import dagger.hilt.android.EntryPointAccessors

/**
 * Home-screen notifications widget.
 *
 * Data is resolved once, off the composition, in [provideGlance] — Glance composes synchronously,
 * so the Room read and the avatar decoding all happen before a single [WidgetSnapshot] is handed
 * to a pure composable. Every refresh path (push, streaming, the refresh button, the system's
 * periodic update) works by re-running this, never by mutating widget state in place.
 */
class NotificationWidget : GlanceAppWidget() {

    /**
     * Single, not Exact/Responsive: those compose one RemoteViews tree *per host size*, which
     * would duplicate every row's avatar bitmap across the variants and put the update in real
     * danger of the ~1MB Binder limit. One layout for all sizes is fine here — the row list
     * scrolls, so a shorter cell simply shows fewer rows.
     */
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors
            .fromApplication(context, WidgetEntryPoint::class.java)
            .widgetRepository()
        // Being composed at all is the only refresh signal available when the app isn't running,
        // so top the cache up first — gated on staleness, and bounded, inside refreshIfStale.
        repository.refreshIfStale()
        val snapshot = repository.snapshot()
        provideContent { NotificationWidgetContent(snapshot) }
    }
}

@Composable
private fun NotificationWidgetContent(snapshot: WidgetSnapshot) {
    val palette = snapshot.palette
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(palette.res(R.drawable.widget_bg_dark, R.drawable.widget_bg_light)))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Header(snapshot)
        Spacer(GlanceModifier.height(8.dp))
        when (val content = snapshot.content) {
            WidgetContent.SignedOut -> Message(
                palette = palette,
                text = "Sign in to Neon to see your notifications.",
                action = "Open Neon" to openAppIntent(context, statusId = null, key = "signin"),
            )

            WidgetContent.Loading -> Message(palette = palette, text = "Loading notifications…")

            is WidgetContent.Ready ->
                if (content.rows.isEmpty()) {
                    Message(palette = palette, text = "All quiet — for now.")
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(content.rows) { row ->
                            NotificationRow(row, palette)
                        }
                    }
                }
        }
    }
}

@Composable
private fun Header(snapshot: WidgetSnapshot) {
    val palette = snapshot.palette
    val context = LocalContext.current
    val openNotifications = openAppIntent(context, statusId = null, key = "header")

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorProvider(palette.accentPink)),
            modifier = GlanceModifier.size(18.dp),
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .let { if (openNotifications != null) it.clickable(actionStartActivity(openNotifications)) else it },
        ) {
            Text(
                text = "Notifications",
                style = TextStyle(
                    color = colorProvider(palette.text),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            if (snapshot.updatedLabel.isNotEmpty()) {
                Text(
                    text = snapshot.updatedLabel,
                    style = TextStyle(color = colorProvider(palette.textMute), fontSize = 10.sp),
                    maxLines = 1,
                )
            }
        }
        Image(
            provider = ImageProvider(R.drawable.ic_widget_refresh),
            contentDescription = "Refresh notifications",
            colorFilter = ColorFilter.tint(colorProvider(palette.textDim)),
            modifier = GlanceModifier
                .size(28.dp)
                .padding(5.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
        )
    }
}

@Composable
private fun NotificationRow(row: WidgetRow, palette: NeonPalette) {
    val context = LocalContext.current
    val intent = openAppIntent(context, row.statusId, key = row.id)
    val avatar = row.avatar

    Box(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ImageProvider(palette.res(R.drawable.widget_card_dark, R.drawable.widget_card_light)))
                .padding(10.dp)
                .let { if (intent != null) it.clickable(actionStartActivity(intent)) else it },
            verticalAlignment = Alignment.Top,
        ) {
            Image(
                provider = if (avatar != null) {
                    ImageProvider(avatar)
                } else {
                    ImageProvider(R.drawable.widget_avatar_fallback)
                },
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.size(WidgetAvatars.SIZE_DP.dp),
            )
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = row.name,
                        style = TextStyle(
                            color = colorProvider(palette.text),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = row.time,
                        style = TextStyle(color = colorProvider(palette.textMute), fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
                // Glance has no AnnotatedString, so the verb and the toot preview share one dim
                // line instead of the two-tone treatment NotificationsScreen uses.
                Text(
                    text = if (row.preview.isEmpty()) row.verb else "${row.verb} · ${row.preview}",
                    style = TextStyle(color = colorProvider(palette.textDim), fontSize = 12.sp),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun Message(
    palette: NeonPalette,
    text: String,
    action: Pair<String, Intent?>? = null,
) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = TextStyle(
                    color = colorProvider(palette.textDim),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                ),
            )
            val intent = action?.second
            if (action != null && intent != null) {
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    text = action.first,
                    style = TextStyle(
                        color = colorProvider(palette.onGradient),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_cta))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable(actionStartActivity(intent)),
                )
            }
        }
    }
}

/**
 * Glance's fixed-[Color] provider factory.
 *
 * Wrapped so the lint suppression sits in one place: the sibling `ColorProvider(@ColorRes Int)`
 * overload declared in the same Glance file is `@RestrictTo(LIBRARY_GROUP)`, and because the
 * public [Color] overload is name-mangled (Color is a value class) lint resolves these calls to
 * the restricted one. The compiler binds the public overload — this is a lint-only false positive.
 */
@SuppressLint("RestrictedApi")
private fun colorProvider(color: Color): ColorProvider = ColorProvider(color)

/** Picks the dark or light variant of a resource for the palette in play. */
private fun NeonPalette.res(dark: Int, light: Int): Int = if (isLight) light else dark

/**
 * Launch intent into MainActivity, carrying the same `status_id` / `open_notifications` extras the
 * push notifications use, so taps land on `Navigator.handleNotificationClick`.
 *
 * The [key]-derived data URI matters: PendingIntents are matched with `Intent.filterEquals()`,
 * which ignores extras, so without it every row would reuse the first row's PendingIntent and open
 * the same thread.
 */
private fun openAppIntent(context: Context, statusId: String?, key: String): Intent? {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
    return intent.apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        statusId?.let { putExtra("status_id", it) }
        putExtra("open_notifications", true)
        data = "neon://widget/$key".toUri()
    }
}
