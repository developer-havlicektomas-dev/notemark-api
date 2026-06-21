package dev.havlicektomas.notemarkapi.web

/** Resource already exists (e.g. duplicate email/username on register) -> 409. */
class ConflictException(message: String) : RuntimeException(message)

/** Bad credentials or invalid/expired refresh token -> 401. */
class UnauthorizedException(message: String) : RuntimeException(message)

/** Note does not exist or is not owned by the caller -> 404. */
class NotFoundException(message: String) : RuntimeException(message)
