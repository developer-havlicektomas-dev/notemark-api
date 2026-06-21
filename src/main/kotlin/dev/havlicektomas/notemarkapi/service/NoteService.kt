package dev.havlicektomas.notemarkapi.service

import dev.havlicektomas.notemarkapi.dto.NoteRequest
import dev.havlicektomas.notemarkapi.dto.NoteResponse
import dev.havlicektomas.notemarkapi.dto.NotesResponse
import dev.havlicektomas.notemarkapi.model.Note
import dev.havlicektomas.notemarkapi.repository.NoteRepository
import dev.havlicektomas.notemarkapi.web.NotFoundException
import org.springframework.stereotype.Service

@Service
class NoteService(
    private val noteRepository: NoteRepository,
) {

    /** Upsert by id. A caller may only overwrite a note they already own. */
    fun upsert(ownerEmail: String, request: NoteRequest): NoteResponse {
        noteRepository.findById(request.id)?.let { existing ->
            if (existing.ownerEmail != ownerEmail) {
                // Don't reveal that another user's note exists.
                throw NotFoundException("Note not found")
            }
        }
        val note = Note(
            id = request.id,
            ownerEmail = ownerEmail,
            title = request.title,
            content = request.content,
            createdAt = request.createdAt,
            lastEditedAt = request.lastEditedAt,
        )
        return noteRepository.save(note).toResponse()
    }

    /** `page == -1` returns all of the user's notes; otherwise a 0-based page slice. */
    fun list(ownerEmail: String, page: Int, size: Int): NotesResponse {
        val all = noteRepository.findByOwner(ownerEmail)
        val notes = if (page == -1) {
            all
        } else {
            val from = (page * size).coerceIn(0, all.size)
            val to = (from + size).coerceIn(0, all.size)
            all.subList(from, to)
        }
        return NotesResponse(
            notes = notes.map { it.toResponse() },
            total = all.size.toLong(),
        )
    }

    fun delete(ownerEmail: String, id: String) {
        val note = noteRepository.findById(id) ?: throw NotFoundException("Note not found")
        if (note.ownerEmail != ownerEmail) {
            throw NotFoundException("Note not found")
        }
        noteRepository.delete(id)
    }

    private fun Note.toResponse() = NoteResponse(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        lastEditedAt = lastEditedAt,
    )
}
