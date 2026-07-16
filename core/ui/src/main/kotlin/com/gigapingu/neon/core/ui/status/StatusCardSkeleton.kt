package com.gigapingu.neon.core.ui.status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.SkeletonBlock

/** Ghost of a [StatusCard] shown while a list makes its first load. */
@Composable
fun StatusCardSkeleton(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row {
            SkeletonBlock(Modifier.size(38.dp), shape = CircleShape)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    SkeletonBlock(Modifier.height(12.dp).fillMaxWidth(.4f))
                    Spacer(Modifier.width(8.dp))
                    SkeletonBlock(Modifier.height(12.dp).fillMaxWidth(.35f))
                }
                Spacer(Modifier.height(12.dp))
                SkeletonBlock(Modifier.height(10.dp).fillMaxWidth())
                Spacer(Modifier.height(7.dp))
                SkeletonBlock(Modifier.height(10.dp).fillMaxWidth(.82f))
                Spacer(Modifier.height(7.dp))
                SkeletonBlock(Modifier.height(10.dp).fillMaxWidth(.55f))
            }
        }
    }
}

/** Full-pane first-load placeholder for status lists. */
@Composable
fun StatusListSkeleton(modifier: Modifier = Modifier, cards: Int = 6) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp),
    ) {
        repeat(cards) { StatusCardSkeleton() }
    }
}
