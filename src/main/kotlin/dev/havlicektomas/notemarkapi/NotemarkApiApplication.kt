package dev.havlicektomas.notemarkapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NotemarkApiApplication

fun main(args: Array<String>) {
    runApplication<NotemarkApiApplication>(*args)
}
