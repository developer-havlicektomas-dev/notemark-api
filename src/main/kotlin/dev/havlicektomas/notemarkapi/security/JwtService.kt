package dev.havlicektomas.notemarkapi.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-validity}") private val accessValidity: Duration,
    @Value("\${jwt.access-debug-validity}") private val accessDebugValidity: Duration,
    @Value("\${jwt.refresh-validity}") val refreshValidity: Duration,
) {

    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))

    /**
     * Issues an access token whose subject is the user's [email].
     * When [debug] is true the token lives for only 30 seconds (per the spec's
     * `Debug: true` behavior) so the refresh flow can be exercised quickly.
     */
    fun generateAccessToken(email: String, debug: Boolean): String {
        val now = Instant.now()
        val validity = if (debug) accessDebugValidity else accessValidity
        return Jwts.builder()
            .subject(email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(validity)))
            .signWith(key)
            .compact()
    }

    /** Refresh tokens are opaque random strings (stored in-memory, not JWTs). */
    fun generateRefreshToken(): String = UUID.randomUUID().toString() + UUID.randomUUID().toString()

    /** Returns the email (subject) for a valid, unexpired access token, or null. */
    fun parseEmail(token: String): String? = try {
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    } catch (_: JwtException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
