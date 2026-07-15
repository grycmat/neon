package com.gigapingu.neon.core.data

/** Generic async state for StateFlow-driven repositories. */
enum class AsyncPhase { Idle, Loading, LoadingMore, Refreshing, Ready, Error }

data class AsyncState<T>(
    val phase: AsyncPhase = AsyncPhase.Idle,
    val data: T? = null,
    val error: String? = null,
    val hasMore: Boolean = true,
) {
    val isLoading: Boolean get() = phase == AsyncPhase.Loading
    val isError: Boolean get() = phase == AsyncPhase.Error && data == null
    val hasData: Boolean get() = data != null

    fun withPhase(phase: AsyncPhase): AsyncState<T> = copy(phase = phase)

    fun withData(data: T, hasMore: Boolean = this.hasMore): AsyncState<T> =
        copy(phase = AsyncPhase.Ready, data = data, error = null, hasMore = hasMore)

    companion object {
        fun <T> idle(): AsyncState<T> = AsyncState()
        fun <T> loading(): AsyncState<T> = AsyncState(phase = AsyncPhase.Loading)
        fun <T> ready(data: T, hasMore: Boolean = true): AsyncState<T> =
            AsyncState(phase = AsyncPhase.Ready, data = data, hasMore = hasMore)
        fun <T> error(message: String): AsyncState<T> =
            AsyncState(phase = AsyncPhase.Error, error = message)
    }
}
