package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Status

/** Applies an updated status to a list, following boosts. */
fun patchStatusList(list: List<Status>, updated: Status): List<Status> =
    list.map { status ->
        when {
            status.id == updated.id -> updated
            status.reblog?.id == updated.id -> status.copy(reblog = updated)
            else -> status
        }
    }

/** Applies an updated poll to every status carrying it. */
fun patchPollList(list: List<Status>, poll: Poll): List<Status> =
    list.map { status ->
        when {
            status.poll?.id == poll.id -> status.copy(poll = poll)
            status.reblog?.poll?.id == poll.id ->
                status.copy(reblog = status.reblog!!.copy(poll = poll))
            else -> status
        }
    }
