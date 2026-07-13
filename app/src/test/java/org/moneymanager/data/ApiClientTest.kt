package org.moneymanager.data

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ApiClient(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `health accepts the public plain-text response`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        assertTrue(client.checkHealth())
        assertEquals("/health", server.takeRequest().path)
    }

    @Test
    fun `delete account sends bearer token to me endpoint`() {
        server.enqueue(MockResponse().setResponseCode(204))

        client.deleteAccount("secret-token")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/me", request.path)
        assertEquals("Bearer secret-token", request.getHeader("Authorization"))
    }
}
