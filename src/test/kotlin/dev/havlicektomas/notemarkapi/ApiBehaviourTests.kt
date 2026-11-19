package dev.havlicektomas.notemarkapi

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiBehaviourTests {

    @Value("\${local.server.port}")
    private var port: Int = 0

    private val http = HttpClient.newHttpClient()
    private val json = JsonMapper.builder().build()

    private data class Res(val status: Int, val body: String) {
        fun json(mapper: JsonMapper): JsonNode = mapper.readTree(body)
    }

    private fun call(method: String, path: String, body: String? = null, token: String? = null): Res {
        val builder = HttpRequest.newBuilder(URI.create("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
            .method(
                method,
                body?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody(),
            )
        token?.let { builder.header("Authorization", "Bearer $it") }
        val response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        return Res(response.statusCode(), response.body())
    }

    private fun uniq() = UUID.randomUUID().toString().take(8)

    /** Registers a fresh user and returns (accessToken, refreshToken, email). */
    private fun newUser(emailOverride: String? = null): Triple<String, String, String> {
        val name = "u${uniq()}"
        val email = emailOverride ?: "$name@example.com"
        val reg = call("POST", "/api/auth/register", """{"username":"$name","email":"$email","password":"Secret123"}""")
        assertEquals(201, reg.status, reg.body)
        val login = call("POST", "/api/auth/login", """{"email":"$email","password":"Secret123"}""")
        assertEquals(200, login.status, login.body)
        val node = login.json(json)
        return Triple(node["accessToken"].asString(), node["refreshToken"].asString(), email)
    }

    private fun noteJson(
        id: String,
        title: String = "T",
        lastEditedAt: String = "2026-06-19T10:00:00Z",
        content: String? = "C",
    ): String {
        val contentPart = content?.let { ""","content":"$it"""" } ?: ""
        return """{"id":"$id","title":"$title"$contentPart,"createdAt":"2026-06-19T10:00:00Z","lastEditedAt":"$lastEditedAt"}"""
    }

    @Test
    fun `errors keep their real status instead of 401`() {
        val (token) = newUser()
        assertEquals(400, call("GET", "/api/notes?page=abc", token = token).status)
        assertEquals(404, call("GET", "/api/does-not-exist", token = token).status)
        assertEquals(405, call("PATCH", "/api/notes", "{}", token = token).status)
        assertEquals(401, call("GET", "/api/notes").status)
    }

    @Test
    fun `bad paging values return 400 and huge pages are empty`() {
        val (token) = newUser()
        assertEquals(400, call("GET", "/api/notes?page=-2", token = token).status)
        assertEquals(400, call("GET", "/api/notes?size=0", token = token).status)
        val huge = call("GET", "/api/notes?page=1&size=2147483647", token = token)
        assertEquals(200, huge.status, huge.body)
        assertEquals(0, huge.json(json)["notes"].size())
    }

    @Test
    fun `kotlin defaults apply so content may be omitted`() {
        val (token) = newUser()
        val res = call("POST", "/api/notes", noteJson(UUID.randomUUID().toString(), content = null), token)
        assertEquals(200, res.status, res.body)
        assertEquals("", res.json(json)["content"].asString())
    }

    @Test
    fun `invalid id or timestamp is rejected`() {
        val (token) = newUser()
        assertEquals(400, call("POST", "/api/notes", noteJson("not-a-uuid"), token).status)
        assertEquals(
            400,
            call("POST", "/api/notes", noteJson(UUID.randomUUID().toString(), lastEditedAt = "garbage"), token).status,
        )
    }

    @Test
    fun `older edit does not overwrite newer one`() {
        val (token) = newUser()
        val id = UUID.randomUUID().toString()
        call("PUT", "/api/notes", noteJson(id, title = "new", lastEditedAt = "2026-06-20T10:00:00Z"), token)
        val stale = call("PUT", "/api/notes", noteJson(id, title = "old", lastEditedAt = "2026-06-19T10:00:00Z"), token)
        assertEquals(200, stale.status)
        assertEquals("new", stale.json(json)["title"].asString())
    }

    @Test
    fun `notes sort by parsed time not by string`() {
        val (token) = newUser()
        val a = UUID.randomUUID().toString()
        val b = UUID.randomUUID().toString()
        // As strings "...T10:00:00.500Z" < "...T10:00:00Z", but it is the later instant.
        call("POST", "/api/notes", noteJson(a, lastEditedAt = "2026-06-19T10:00:00Z"), token)
        call("POST", "/api/notes", noteJson(b, lastEditedAt = "2026-06-19T10:00:00.500Z"), token)
        val notes = call("GET", "/api/notes?page=-1", token = token).json(json)["notes"]
        val ids = (0 until notes.size()).map { notes[it]["id"].asString() }
        assertEquals(listOf(b, a), ids)
    }

    @Test
    fun `other users notes are hidden`() {
        val (tokenA) = newUser()
        val (tokenB) = newUser()
        val id = UUID.randomUUID().toString()
        call("POST", "/api/notes", noteJson(id), tokenA)
        assertEquals(404, call("PUT", "/api/notes", noteJson(id, lastEditedAt = "2027-01-01T00:00:00Z"), tokenB).status)
        assertEquals(404, call("DELETE", "/api/notes/$id", token = tokenB).status)
        assertEquals(200, call("DELETE", "/api/notes/$id", token = tokenA).status)
    }

    @Test
    fun `email is case-insensitive`() {
        val email = "Mixed${uniq()}@Example.com"
        newUser(email)
        val login = call("POST", "/api/auth/login", """{"email":"${email.lowercase()}","password":"Secret123"}""")
        assertEquals(200, login.status)
        val dup = call("POST", "/api/auth/register", """{"username":"x${uniq()}","email":"${email.uppercase()}","password":"Secret123"}""")
        assertEquals(409, dup.status)
    }

    @Test
    fun `refresh token works once even under concurrency`() {
        val (_, refresh) = newUser()
        val body = """{"refreshToken":"$refresh"}"""
        val pool = Executors.newFixedThreadPool(8)
        val statuses = try {
            pool.invokeAll((1..8).map { Callable { call("POST", "/api/auth/refresh", body).status } }).map { it.get() }
        } finally {
            pool.shutdown()
        }
        assertEquals(1, statuses.count { it == 200 }, statuses.toString())
        assertTrue(statuses.filter { it != 200 }.all { it == 401 }, statuses.toString())
    }
}
