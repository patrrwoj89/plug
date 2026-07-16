package com.polishmediahub.app.data.tv

/**
 * Semantic kinds for the Android TV Watch Next row.
 *
 * The integer values match the framework's `TvContractCompat.WatchNextPrograms`
 * watch-next type constants so they can be passed directly to
 * `WatchNextProgram.Builder.setWatchNextType()`.
 */
internal enum class WatchNextKind(val value: Int) {
    CONTINUE(0),
    NEXT(1),
    NEW(2),
    WATCHLIST(3)
}
