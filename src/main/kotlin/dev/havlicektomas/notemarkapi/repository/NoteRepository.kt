package dev.havlicektomas.notemarkapi.repository

import dev.havlicektomas.notemarkapi.model.Note
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class NoteRepository {

    // Keyed by note id.
    private val notes = ConcurrentHashMap<String, Note>()

    fun save(note: Note): Note {
        notes[note.id] = note
        return note
    }

    fun findById(id: String): Note? = notes[id]

    fun findByOwner(ownerEmail: String): List<Note> =
        notes.values
            .filter { it.ownerEmail == ownerEmail }
            // Stable ordering: most recently edited first.
            .sortedByDescending { it.lastEditedAt }

    fun delete(id: String) {
        notes.remove(id)
    }
}
