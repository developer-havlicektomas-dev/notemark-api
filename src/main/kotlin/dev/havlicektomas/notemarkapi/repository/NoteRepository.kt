package dev.havlicektomas.notemarkapi.repository

import dev.havlicektomas.notemarkapi.model.Note
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class NoteRepository {

    // Keyed by note id.
    private val notes = ConcurrentHashMap<String, Note>()

    /**
     * Atomically computes the new value for [id] from the current one (null if absent).
     * Returning null from [remapping] removes the entry.
     */
    fun compute(id: String, remapping: (Note?) -> Note?): Note? =
        notes.compute(id) { _, existing -> remapping(existing) }

    fun findById(id: String): Note? = notes[id]

    fun findByOwner(ownerEmail: String): List<Note> =
        notes.values
            .filter { it.ownerEmail == ownerEmail }
            // Stable ordering: most recently edited first.
            .sortedByDescending { it.lastEditedAtInstant }
}
