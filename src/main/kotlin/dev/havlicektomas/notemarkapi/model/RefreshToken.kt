package dev.havlicektomas.notemarkapi.model

import java.time.Instant

data class RefreshToken(
    val token: String,
    val email: String,
    val expiresAt: Instant,
)
