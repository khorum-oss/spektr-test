package org.khorum.oss.spektr.test

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.JsonPathAssertions
import org.springframework.test.web.reactive.server.StatusAssertions
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Lightweight DSL wrapper over [WebTestClient] for use in Spektr integration tests.
 *
 * Usage:
 * ```kotlin
 * val client = SpektrTestClient(webTestClient, "/api/users")
 *
 * client.get {
 *     expect {
 *         hasOkStatus()
 *         "$.name".jsonPathValueEquals("Wraith")
 *     }
 * }
 * ```
 */
class SpektrTestClient(
    val webClient: WebTestClient,
    private val baseUri: String
) {
    fun get(path: String = "", scope: RequestSpec.() -> Unit) {
        val spec = RequestSpec().apply(scope)
        val response = webClient.get().uri(baseUri + path).exchange()
        spec.applyExpectations(response)
    }

    fun post(path: String = "", body: Any? = null, scope: RequestSpec.() -> Unit) {
        val spec = RequestSpec().apply(scope)
        val setup = webClient.post().uri(baseUri + path).contentType(MediaType.APPLICATION_JSON)
        val response = body?.let { setup.bodyValue(it).exchange() } ?: setup.exchange()
        spec.applyExpectations(response)
    }

    fun put(path: String = "", body: Any? = null, scope: RequestSpec.() -> Unit) {
        val spec = RequestSpec().apply(scope)
        val setup = webClient.put().uri(baseUri + path).contentType(MediaType.APPLICATION_JSON)
        val response = body?.let { setup.bodyValue(it).exchange() } ?: setup.exchange()
        spec.applyExpectations(response)
    }

    fun delete(path: String = "", scope: RequestSpec.() -> Unit) {
        val spec = RequestSpec().apply(scope)
        val response = webClient.delete().uri(baseUri + path).exchange()
        spec.applyExpectations(response)
    }

    private fun RequestSpec.applyExpectations(response: WebTestClient.ResponseSpec) {
        statusCheck(response.expectStatus())
        val body = response.expectBody()
        jsonPathChecks.forEach { it(body) }
    }

    inner class RequestSpec {
        internal var statusCheck: (StatusAssertions) -> Unit = {}
        internal val jsonPathChecks = mutableListOf<(WebTestClient.BodyContentSpec) -> Unit>()

        fun expect(scope: ExpectationSpec.() -> Unit) {
            ExpectationSpec().apply(scope)
        }

        inner class ExpectationSpec {
            fun hasOkStatus() { statusCheck = { it.isOk } }
            fun hasCreatedStatus() { statusCheck = { it.isCreated } }
            fun hasNoContentStatus() { statusCheck = { it.isNoContent } }
            fun hasStatus(code: Int) { statusCheck = { it.isEqualTo(code) } }

            fun String.jsonPathValueEquals(expected: Any) {
                jsonPathChecks += { it.jsonPath(this).isEqualTo(expected) }
            }

            fun String.jsonPathExists() {
                jsonPathChecks += { it.jsonPath(this).exists() }
            }

            fun String.jsonPathIsEmpty() {
                jsonPathChecks += { it.jsonPath(this).isEmpty }
            }

            fun String.jsonPathIsNotEmpty() {
                jsonPathChecks += { it.jsonPath(this).isNotEmpty }
            }

            fun String.jsonPath(scope: JsonPathAssertions.() -> Unit) {
                jsonPathChecks += { scope(it.jsonPath(this)) }
            }
        }
    }
}

fun spektrClient(webClient: WebTestClient, baseUri: String) =
    SpektrTestClient(webClient, baseUri)
