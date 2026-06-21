package dev.havlicektomas.notemarkapi.model

data class Note(
    val id: String,
    val ownerEmail: String,
    val title: String,
    val content: String,
    // Kept as ISO-8601 strings to round-trip the exact values sent by the client.
    val createdAt: String,
    val lastEditedAt: String,
)
