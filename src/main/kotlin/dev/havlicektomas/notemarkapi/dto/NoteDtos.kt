package dev.havlicektomas.notemarkapi.dto

import jakarta.validation.constraints.NotBlank

/**
 * Create/update payload. Offline-first: the client generates [id] and the
 * timestamps and the server trusts them. `PUT /api/notes` upserts by [id].
 */
data class NoteRequest(
    @field:NotBlank
    val id: String,
    @field:NotBlank
    val title: String,
    val content: String = "",
    @field:NotBlank
    val createdAt: String,
    @field:NotBlank
    val lastEditedAt: String,
)

data class NoteResponse(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val lastEditedAt: String,
)

data class NotesResponse(
    val notes: List<NoteResponse>,
    val total: Long,
)
