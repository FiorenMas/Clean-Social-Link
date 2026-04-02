package com.fiorenmas.cleansociallink.history.data

data class HistoryMetadata(
    val image: String?,
    val text: String,
    val domain: String
)

data class HistoryEntry(
    val id: String,
    val cleanUrl: String,
    val storedAtEpochMs: Long,
    val storedAtLocalText: String,
    val metadata: HistoryMetadata
)

