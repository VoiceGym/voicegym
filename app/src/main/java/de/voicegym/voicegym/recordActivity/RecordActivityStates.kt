package de.voicegym.voicegym.recordActivity

/**
 * The state in which the RecordActivity currently resides in
 */
enum class RecordActivityState {
    LIVEVIEW,
    RECORDING,
    PLAYBACK,
    PLAYBACK_FROM_FILE
}

/**
 * During playback it is possible to seek to a specific position in the record,
 * this enum is responsible for storing whether the user is currently seeking to another position in the record
 * and whether the activity was playing beforehand
 */
enum class TouchPlaybackState {
    TOUCHED,
    TOUCHED_WHILE_PLAYING,
    RELEASED
}
