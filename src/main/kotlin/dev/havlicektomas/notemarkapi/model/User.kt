package dev.havlicektomas.notemarkapi.model

data class User(
    val username: String,
    val email: String,
    val passwordHash: String,
)
