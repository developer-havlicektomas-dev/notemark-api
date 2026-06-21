package dev.havlicektomas.notemarkapi.controller

import dev.havlicektomas.notemarkapi.dto.NoteRequest
import dev.havlicektomas.notemarkapi.dto.NoteResponse
import dev.havlicektomas.notemarkapi.dto.NotesResponse
import dev.havlicektomas.notemarkapi.service.NoteService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notes")
class NoteController(
    private val noteService: NoteService,
) {

    @PostMapping
    fun create(
        @Valid @RequestBody request: NoteRequest,
        authentication: Authentication,
    ): NoteResponse = noteService.upsert(authentication.name, request)

    @PutMapping
    fun update(
        @Valid @RequestBody request: NoteRequest,
        authentication: Authentication,
    ): NoteResponse = noteService.upsert(authentication.name, request)

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): NotesResponse = noteService.list(authentication.name, page, size)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun delete(
        @PathVariable id: String,
        authentication: Authentication,
    ) {
        noteService.delete(authentication.name, id)
    }
}
