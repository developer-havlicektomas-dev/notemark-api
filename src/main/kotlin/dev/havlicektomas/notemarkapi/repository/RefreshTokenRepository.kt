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

    fun find(token: String): RefreshToken? = tokens[token]

    fun delete(token: String) {
        tokens.remove(token)
    }
}
