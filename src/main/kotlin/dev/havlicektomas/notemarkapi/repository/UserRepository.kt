package dev.havlicektomas.notemarkapi.repository

import dev.havlicektomas.notemarkapi.model.User
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Emails are stored normalized (trimmed, lowercase) by the caller; usernames are
 * unique case-insensitively.
 */
@Component
class UserRepository {

    // Keyed by email (the unique identity used as the JWT subject).
    private val usersByEmail = ConcurrentHashMap<String, User>()

    /** Saves [user] unless the email is taken; returns false on a duplicate. */
    fun saveIfAbsent(user: User): Boolean = usersByEmail.putIfAbsent(user.email, user) == null

    fun findByEmail(email: String): User? = usersByEmail[email]

    fun existsByUsername(username: String): Boolean =
        usersByEmail.values.any { it.username.equals(username, ignoreCase = true) }
}
