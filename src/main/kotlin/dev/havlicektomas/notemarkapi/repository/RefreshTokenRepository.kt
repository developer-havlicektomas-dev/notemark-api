package dev.havlicektomas.notemarkapi.repository

import dev.havlicektomas.notemarkapi.model.RefreshToken
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class RefreshTokenRepository {

    // Keyed by the opaque refresh token string.
    private val tokens = ConcurrentHashMap<String, RefreshToken>()

    fun save(token: RefreshToken): RefreshToken {
        tokens[token.token] = token
        return token
    }

    /** Atomically removes and returns the token, so each one can be used only once. */
    fun consume(token: String): RefreshToken? = tokens.remove(token)
}
