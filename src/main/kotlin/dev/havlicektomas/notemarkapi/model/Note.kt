package dev.havlicektomas.notemarkapi.model

import java.time.Instant

data class Note(
    val id: String,
    val ownerEmail: String,
    val title: String,
    val content: String,
    // Kept as ISO-8601 strings to round-trip the exact values sent by the client.
    val createdAt: String,
    val lastEditedAt: String,
    // Parsed form of [lastEditedAt], used for ordering and last-writer-wins.
    val lastEditedAtInstant: Instant,
)
