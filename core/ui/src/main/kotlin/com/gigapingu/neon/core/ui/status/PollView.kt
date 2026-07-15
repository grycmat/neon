package com.gigapingu.neon.core.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.component.GradientButton
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.compactCount
import com.gigapingu.neon.core.designsystem.util.pollTimeLeft
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.ui.LocalStatusActionHandler

/**
 * Poll rendering + voting. Results show gradient bars; before voting the
 * options are selectable and a Vote pill submits.
 */
@Composable
fun PollView(poll: Poll, modifier: Modifier = Modifier) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val actions = LocalStatusActionHandler.current
    var selected by remember(poll.id) { mutableStateOf(setOf<Int>()) }
    val showResults = poll.showResults
    val total = if (poll.votesCount == 0) 1 else poll.votesCount

    Column(modifier = modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        poll.options.forEachIndexed { index, _ ->
            if (showResults) {
                ResultRow(poll, index, total)
            } else {
                ChoiceRow(
                    poll = poll,
                    index = index,
                    selected = index in selected,
                    onToggle = {
                        selected = when {
                            index in selected -> selected - index
                            poll.multiple -> selected + index
                            else -> setOf(index)
                        }
                    },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${compactCount(poll.votesCount)} votes · ${pollTimeLeft(poll.expiresAt, poll.expired)}",
                style = type.bodySmall,
                color = palette.textMute,
            )
            if (poll.voted) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(palette.pink),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Your vote",
                    style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.pink,
                )
            }
            Spacer(Modifier.weight(1f))
            if (!showResults) {
                GradientButton(
                    label = "Vote",
                    height = 36.dp,
                    modifier = Modifier.width(92.dp),
                    onClick = if (selected.isEmpty()) {
                        null
                    } else {
                        { actions.vote(poll, selected.sorted()) }
                    },
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(poll: Poll, index: Int, selected: Boolean, onToggle: () -> Unit) {
    val palette = NeonTheme.palette
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) palette.cyan.copy(alpha = .08f) else palette.surface)
            .border(1.dp, if (selected) palette.cyan.copy(alpha = .45f) else palette.border, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val markShape = if (poll.multiple) RoundedCornerShape(5.dp) else CircleShape
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(markShape)
                .background(if (selected) palette.cyan.copy(alpha = .25f) else palette.bg.copy(alpha = 0f))
                .border(1.6.dp, if (selected) palette.cyan else palette.borderStrong, markShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.cyan, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.width(11.dp))
        Text(
            poll.options[index].title,
            style = NeonTheme.type.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = palette.text,
        )
    }
}

@Composable
private fun ResultRow(poll: Poll, index: Int, total: Int) {
    val palette = NeonTheme.palette
    val option = poll.options[index]
    val fraction = (option.votesCount.toFloat() / total).coerceIn(0f, 1f)
    val mine = index in poll.ownVotes
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, if (mine) palette.cyan.copy(alpha = .45f) else palette.border, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(palette.pink.copy(alpha = .35f), palette.purple.copy(alpha = .35f)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                option.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = NeonTheme.type.bodyMedium.copy(
                    fontWeight = if (mine) FontWeight.ExtraBold else FontWeight.SemiBold,
                ),
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
            if (mine) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Your vote",
                    tint = palette.cyan,
                    modifier = Modifier.size(15.dp).padding(end = 0.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "${(fraction * 100).toInt()}%",
                style = NeonTheme.type.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = palette.text,
            )
        }
    }
}
