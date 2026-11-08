package com.auramusic.app.tv

/** Only an empty result backed by successful snapshots may remove the existing launcher row contents. */
internal fun shouldRemoveStaleTvPrograms(
    queueSnapshotAvailable: Boolean,
    quickPicksSnapshotAvailable: Boolean,
    selectedProgramCount: Int,
): Boolean =
    selectedProgramCount == 0 && queueSnapshotAvailable && quickPicksSnapshotAvailable
