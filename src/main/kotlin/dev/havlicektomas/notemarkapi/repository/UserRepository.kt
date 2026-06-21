package dev.havlicektomas.notemarkapi.repository

import dev.havlicektomas.notemarkapi.model.User
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class UserRepository {

    // Keyed by email (the unique identity used as the JWT subject).
    private val usersByEmail = ConcurrentHashMap<String, User>()

    fun save(user: User): User {
        usersByEmail[user.email] = user
        return user
    }

    fun findByEmail(email: String): User? = usersByEmail[email]

    fun existsByEmail(email: String): Boolean = usersByEmail.containsKey(email)

    fun existsByUsername(username: String): Boolean =
        usersByEmail.values.any { it.username == username }
}
