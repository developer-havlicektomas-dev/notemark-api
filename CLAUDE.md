# CLAUDE.md — notemark-api

Local-only Spring Boot backend for the NoteMark mobile app (`../notemark-kmp`).
Everything (users, refresh tokens, notes) is stored **in memory** in `ConcurrentHashMap`s — lost on restart.

## Stack & commands

Spring Boot 4.1.2, Kotlin 2.3.21 (the version Boot 4.1.x is built against), Spring Web + Security + Validation, JJWT 0.13.0, Java 21 toolchain, Gradle 9.9.1.

```bash
./gradlew bootRun     # listens on 0.0.0.0:8080
./gradlew test        # ApiBehaviourTests: real server on a random port + java HttpClient
```

Config in `src/main/resources/application.properties` (port, `jwt.secret`, `jwt.access-validity=15m`,
`jwt.access-debug-validity=30s`, `jwt.refresh-validity=30d`).

## Layout (`src/main/kotlin/dev/havlicektomas/notemarkapi/`)

- `controller/` — `AuthController` (`/api/auth/register|login|refresh`), `NoteController` (`/api/notes`)
- `service/` — `AuthService`, `NoteService` (business rules)
- `repository/` — in-memory `@Component` stores (`UserRepository`, `RefreshTokenRepository`, `NoteRepository`)
- `model/` — `User`, `Note`, `RefreshToken`
- `dto/` — request/response data classes; validation via `@field:NotBlank` / `@field:Email`
- `security/` — `JwtService`, `JwtAuthenticationFilter`
- `config/SecurityConfig.kt` — stateless, CSRF off, auth endpoints public, everything else authenticated;
  unauthenticated → 401 `{"message":"Unauthorized"}`
- `web/` — `ConflictException` (409), `UnauthorizedException` (401), `NotFoundException` (404), mapped by
  `GlobalExceptionHandler` to `ErrorResponse(message)`; validation/malformed body → 400

## Behaviour the mobile client depends on

- **Don't change the API contract without updating `notemark-kmp`** (`sharedLogic/.../auth/data/AuthDtos.kt`,
  `notes/data/NoteDtos.kt`, `KtorAuthDataSource`, `KtorNotesDataSource`).
- Access token: JWT, subject = user email; 15 min, or 30 s when the request has header `Debug: true`.
- Refresh token: opaque, 30 days, rotated on `/api/auth/refresh` (old one → 401).
- Login response: `accessToken`, `refreshToken`, `username`.
- Notes are offline-first: the **client** supplies `id` (must be a UUID), `createdAt`, `lastEditedAt`
  (must be ISO-8601 instants, else 400). `POST` and `PUT /api/notes` both upsert by `id` via
  `NoteService.upsert`, **last writer wins**: if the stored note has a later `lastEditedAt`, it's kept and
  returned unchanged (200).
- Emails are normalized (trim + lowercase); usernames are unique case-insensitively.
- `/error` is `permitAll` in `SecurityConfig` — removing it turns every 4xx/5xx into a 401.
- Jackson 3 (Boot 4): use `tools.jackson.*`, not `com.fasterxml.jackson.*` (that's only pulled in by jjwt).
- Ownership: touching another user's note returns **404** (never reveal it exists).
- `GET /api/notes?page=0&size=20` — `page=-1` returns all; `page < -1` or `size < 1` → 400;
  response `{ notes, total }`, sorted by parsed `lastEditedAt` desc.
- Refresh tokens are consumed atomically (`RefreshTokenRepository.consume`), so each works exactly once.
- Plain HTTP, no CORS (native clients only).
