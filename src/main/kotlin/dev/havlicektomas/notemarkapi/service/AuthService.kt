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

    fun register(request: RegisterRequest) {
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("Email already registered")
        }
        if (userRepository.existsByUsername(request.username)) {
            throw ConflictException("Username already taken")
        }
        userRepository.save(
            User(
                username = request.username,
                email = request.email,
                passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
            ),
        )
    }

    fun login(request: LoginRequest, debug: Boolean): LoginResponse {
        val user = userRepository.findByEmail(request.email)
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
        val stored = refreshTokenRepository.find(refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")
        if (stored.expiresAt.isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken)
            throw UnauthorizedException("Refresh token expired")
        }
        // Rotate: the old refresh token becomes invalid immediately.
        refreshTokenRepository.delete(refreshToken)
        val newAccessToken = jwtService.generateAccessToken(stored.email, debug)
        val newRefreshToken = issueRefreshToken(stored.email)
        return RefreshResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

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
