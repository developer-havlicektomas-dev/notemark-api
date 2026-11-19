package dev.havlicektomas.notemarkapi.service

import dev.havlicektomas.notemarkapi.dto.NoteRequest
import dev.havlicektomas.notemarkapi.dto.NoteResponse
import dev.havlicektomas.notemarkapi.dto.NotesResponse
import dev.havlicektomas.notemarkapi.model.Note
import dev.havlicektomas.notemarkapi.repository.NoteRepository
import dev.havlicektomas.notemarkapi.web.BadRequestException
import dev.havlicektomas.notemarkapi.web.NotFoundException
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

@Service
class NoteService(
    private val noteRepository: NoteRepository,
) {

    /**
     * Upsert by id. A caller may only overwrite a note they already own.
     * Last writer wins: if the stored note was edited later than the incoming
     * one, the stored note is kept and returned unchanged.
     */
    fun upsert(ownerEmail: String, request: NoteRequest): NoteResponse {
        requireUuid(request.id)
        parseInstant(request.createdAt, "createdAt")
        val lastEditedAt = parseInstant(request.lastEditedAt, "lastEditedAt")

        val incoming = Note(
            id = request.id,
            ownerEmail = ownerEmail,
            title = request.title,
            content = request.content,
            createdAt = request.createdAt,
            lastEditedAt = request.lastEditedAt,
            lastEditedAtInstant = lastEditedAt,
        )
        var foreign = false
        val result = noteRepository.compute(request.id) { existing ->
            when {
                existing == null -> incoming
                existing.ownerEmail != ownerEmail -> {
                    foreign = true
                    existing
                }
                existing.lastEditedAtInstant > lastEditedAt -> existing
                else -> incoming
            }
        }
        // Don't reveal that another user's note exists.
        if (foreign || result == null) throw NotFoundException("Note not found")
        return result.toResponse()
    }

    /** `page == -1` returns all of the user's notes; otherwise a 0-based page slice. */
    fun list(ownerEmail: String, page: Int, size: Int): NotesResponse {
        if (page < -1) throw BadRequestException("page must be >= -1")
        if (size < 1) throw BadRequestException("size must be >= 1")

        val all = noteRepository.findByOwner(ownerEmail)
        val notes = if (page == -1) {
            all
        } else {
            val from = (page.toLong() * size).coerceAtMost(all.size.toLong()).toInt()
            val to = (from.toLong() + size).coerceAtMost(all.size.toLong()).toInt()
            all.subList(from, to)
        }
        return NotesResponse(
            notes = notes.map { it.toResponse() },
            total = all.size.toLong(),
        )
    }

    fun delete(ownerEmail: String, id: String) {
        var found = false
        noteRepository.compute(id) { existing ->
            if (existing != null && existing.ownerEmail == ownerEmail) {
                found = true
                null
            } else {
                existing
            }
        }
        if (!found) throw NotFoundException("Note not found")
    }

    private fun requireUuid(id: String) {
        try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            throw BadRequestException("id must be a UUID")
        }
    }

    private fun parseInstant(value: String, field: String): Instant =
        try {
            Instant.parse(value)
        } catch (_: DateTimeParseException) {
            throw BadRequestException("$field must be an ISO-8601 instant")
        }

    private fun Note.toResponse() = NoteResponse(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        lastEditedAt = lastEditedAt,
    )
}
