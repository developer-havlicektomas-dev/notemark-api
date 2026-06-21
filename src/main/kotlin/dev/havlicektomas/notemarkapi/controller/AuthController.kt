package dev.havlicektomas.notemarkapi.controller

import dev.havlicektomas.notemarkapi.dto.LoginRequest
import dev.havlicektomas.notemarkapi.dto.LoginResponse
import dev.havlicektomas.notemarkapi.dto.RefreshRequest
import dev.havlicektomas.notemarkapi.dto.RefreshResponse
import dev.havlicektomas.notemarkapi.dto.RegisterRequest
import dev.havlicektomas.notemarkapi.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest) {
        authService.register(request)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        @RequestHeader(value = "Debug", defaultValue = "false") debug: Boolean,
    ): LoginResponse = authService.login(request, debug)

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
        @RequestHeader(value = "Debug", defaultValue = "false") debug: Boolean,
    ): RefreshResponse = authService.refresh(request.refreshToken, debug)
}
