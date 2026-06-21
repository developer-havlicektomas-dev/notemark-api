# NoteMark Local Backend

A local-only Spring Boot backend compatible with the NoteMark mobile app. All data
(users, refresh tokens, notes) is stored **in memory** — everything is lost on
restart. No database required.

- **Stack:** Spring Boot 4.1, Kotlin, Spring Web, Spring Security, Bean Validation, JJWT
- **Base URL:** `http://localhost:8080`
- **JDK:** the Gradle toolchain targets **Java 21** (the project was scaffolded for
  25, which wasn't installed on this machine; 21 is LTS and fully supported).

## Run

```bash
./gradlew bootRun
```

Server listens on `0.0.0.0:8080`.

## API

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/api/auth/register` | public | 201 / 400 (validation) / 409 (duplicate email or username) |
| POST | `/api/auth/login` | public | returns `accessToken`, `refreshToken`, `username` |
| POST | `/api/auth/refresh` | public | returns a new `accessToken` + `refreshToken`; old refresh token is invalidated |
| POST | `/api/notes` | Bearer | create; client sends `id` + timestamps |
| PUT | `/api/notes` | Bearer | update (upsert by `id`) |
| GET | `/api/notes?page=0&size=20` | Bearer | `page=-1` returns all; response `{ notes, total }` |
| DELETE | `/api/notes/{id}` | Bearer | delete |

### Tokens

- **Access token** — JWT (subject = user email), lifetime **15 min**, or **30 s**
  when the request carries header `Debug: true`. Sent as `Authorization: Bearer <token>`.
- **Refresh token** — opaque, lifetime **30 days**, used only on `/api/auth/refresh`.
  Refreshing rotates the token (the old one is rejected with 401).

### Note request body (offline-first)

The client generates the `id` (UUID) and timestamps; the server trusts them.

```json
{
  "id": "uuid",
  "title": "Title",
  "content": "Body text",
  "createdAt": "2026-06-19T10:00:00.000Z",
  "lastEditedAt": "2026-06-19T10:00:00.000Z"
}
```

## Connecting the mobile app

The server uses **plain HTTP** (no TLS), so the Android app must allow cleartext
traffic to the host (`android:usesCleartextTraffic="true"` or a
`network_security_config.xml` entry for the host). This is an app-side setting.

CORS is **not** configured because native apps don't send a browser `Origin`; add a
`CorsConfigurationSource` bean only if you later add a browser-based client.

### Android emulator

The emulator reaches the host machine via the special alias `10.0.2.2`:

```
BASE_URL = http://10.0.2.2:8080
```

### Physical device (same Wi-Fi)

Use the host machine's LAN IP (e.g. `ipconfig getifaddr en0` on macOS) and make sure
the device is on the same network and macOS firewall allows incoming connections:

```
BASE_URL = http://<your-mac-LAN-IP>:8080
```
