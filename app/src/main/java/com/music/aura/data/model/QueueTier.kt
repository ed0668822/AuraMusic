package com.music.aura.data.model

/**
 * The tier a queue entry belongs to.
 *
 * CONTEXT: the album, playlist, artist or folder currently being played.
 * USER_QUEUE: tracks manually added via "Play Next" or "Add to Queue".
 */
enum class QueueTier {
    CONTEXT,
    USER_QUEUE,
}
