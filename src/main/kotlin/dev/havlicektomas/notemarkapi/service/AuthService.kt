package dev.havlicektomas.notemarkapi.service

import dev.havlicektomas.notemarkapi.dto.LoginRequest
import dev.havlicektomas.notemarkapi.dto.LoginResponse
import dev.havlicektomas.notemarkapi.dto.RefreshResponse
import dev.havlicektomas.notemarkapi.dto.RegisterRequest
import dev.havlicektomas.notemarkapi.model.RefreshToken
import dev.havlicektomas.notemarkapi.model.User
import dev.havlicektomas.notemarkapi.repository.RefreshTokenRepository
import dev.havlicektomas.notemarkapi.repository.UserRepository
import dev.havlicektomas.notemarkapi.security.JwtService
import dev.havlicektomas.notemarkapi.web.ConflictException
import dev.havlicektomas.notemarkapi.web.UnauthorizedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
) {

    // Serializes the username check + insert so two concurrent registrations
    // can't both claim the same username (email uniqueness is atomic on its own).
    private val registerLock = Any()

    fun register(request: RegisterRequest) {
        val email = normalizeEmail(request.email)
        val username = request.username.trim()
        val passwordHash = requireNotNull(passwordEncoder.encode(request.password))
        synchronized(registerLock) {
            if (userRepository.findByEmail(email) != null) {
                throw ConflictException("Email already registered")
            }
            if (userRepository.existsByUsername(username)) {
                throw ConflictException("Username already taken")
            }
            val user = User(username = username, email = email, passwordHash = passwordHash)
            if (!userRepository.saveIfAbsent(user)) {
                throw ConflictException("Email already registered")
            }
        }
    }

    fun login(request: LoginRequest, debug: Boolean): LoginResponse {
        val user = userRepository.findByEmail(normalizeEmail(request.email))
            ?: throw UnauthorizedException("Invalid email or password")
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid email or password")
        }
        val accessToken = jwtService.generateAccessToken(user.email, debug)
        val refreshToken = issueRefreshToken(user.email)
        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            username = user.username,
        )
    }

    fun refresh(refreshToken: String, debug: Boolean): RefreshResponse {
        // Rotate: consuming removes the old token atomically, so two concurrent
        // refreshes with the same token can't both succeed.
        val stored = refreshTokenRepository.consume(refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")
        if (stored.expiresAt.isBefore(Instant.now())) {
            throw UnauthorizedException("Refresh token expired")
        }
        if (userRepository.findByEmail(stored.email) == null) {
            throw UnauthorizedException("Invalid refresh token")
        }
        val newAccessToken = jwtService.generateAccessToken(stored.email, debug)
        val newRefreshToken = issueRefreshToken(stored.email)
        return RefreshResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    private fun normalizeEmail(email: String) = email.trim().lowercase()

    private fun issueRefreshToken(email: String): String {
        val token = jwtService.generateRefreshToken()
        refreshTokenRepository.save(
            RefreshToken(
                token = token,
                email = email,
                expiresAt = Instant.now().plus(jwtService.refreshValidity),
            ),
        )
        return token
    }
}
