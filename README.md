# NoteMark Local Backend

A local-only Spring Boot backend compatible with the NoteMark mobile app. All data
(users, refresh tokens, notes) is stored **in memory** — everything is lost on
restart. No database required.

- **Stack:** Spring Boot 4.1.2, Kotlin 2.3.21, Spring Web, Spring Security, Bean
  Validation, JJWT 0.13.0, Jackson 3 (`tools.jackson.module:jackson-module-kotlin`),
  Gradle 9.9.1
- **Base URL:** `http://localhost:8080`
- **JDK:** the Gradle toolchain targets **Java 21** (the project was scaffolded for
  25, which wasn't installed on this machine; 21 is LTS and fully supported).

## Run

```bash
./gradlew bootRun
```

Server listens on `0.0.0.0:8080`. Settings (port, `jwt.secret`, token lifetimes) are in
`src/main/resources/application.properties`.

## Test

```bash
./gradlew test
```

`ApiBehaviourTests` starts the real server on a random port and drives it with Java's
`HttpClient`. It covers error statuses, validation, paging, last-writer-wins, email and
username handling, and refresh-token rotation.

## API

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/api/auth/register` | public | 201 / 400 (validation) / 409 (duplicate email or username) |
| POST | `/api/auth/login` | public | returns `accessToken`, `refreshToken`, `username`; 401 on bad credentials |
| POST | `/api/auth/refresh` | public | returns a new `accessToken` + `refreshToken`; old refresh token is invalidated (401 if reused) |
| POST | `/api/notes` | Bearer | upsert by `id` (client sends `id` + timestamps); last writer wins |
| PUT | `/api/notes` | Bearer | same upsert as `POST` |
| GET | `/api/notes?page=0&size=20` | Bearer | `page=-1` returns all; `page < -1` or `size < 1` → 400; response `{ notes, total }`, newest `lastEditedAt` first |
| DELETE | `/api/notes/{id}` | Bearer | delete; 200, or 404 if not found |

### Errors

Errors return `{ "message": "..." }` with a matching status: **400** (validation or
malformed body), **401** (unauthenticated, bad credentials, invalid/expired refresh token),
**404** (note not found), **409** (duplicate email or username). A note that belongs to
another user is reported as **404**, so its existence is never revealed.

### Accounts

Emails are normalized (trimmed and lowercased). Usernames are unique case-insensitively.

### Tokens

- **Access token** — JWT (subject = user email), lifetime **15 min**, or **30 s**
  when the request carries header `Debug: true`. Sent as `Authorization: Bearer <token>`.
- **Refresh token** — opaque, lifetime **30 days**, used only on `/api/auth/refresh`.
  Refreshing rotates the token (the old one is rejected with 401). Each refresh token is
  consumed atomically, so concurrent refreshes with the same token can't both succeed.

### Note request body (offline-first)

The client generates the `id` and timestamps; the server trusts them. `id` must be a
UUID and `createdAt` / `lastEditedAt` must be ISO-8601 instants, otherwise the request
fails with 400.

**Last writer wins:** if the stored note has a later `lastEditedAt` than the upload, the
server keeps the stored note and returns it unchanged (200).

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

The app targets SDK 37, so Android blocks local-network addresses (including `10.0.2.2`
and LAN IPs) unless the app has the `ACCESS_LOCAL_NETWORK` permission. NoteMark debug
builds ask for it on launch. If it's denied, every request fails as "No internet
connection". This is also an app-side setting.

### Physical device (same Wi-Fi)

Use the host machine's LAN IP (e.g. `ipconfig getifaddr en0` on macOS) and make sure
the device is on the same network and macOS firewall allows incoming connections:

```
BASE_URL = http://<your-mac-LAN-IP>:8080
```
