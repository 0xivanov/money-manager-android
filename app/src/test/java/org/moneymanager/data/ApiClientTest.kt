package org.moneymanager.data

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.moneymanager.model.BudgetRequest
import org.moneymanager.model.TransactionScheduleRequest

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

    @Test
    fun `portfolio preserves missing prices instead of inventing a total`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{
                    "positions":[{
                        "asset_type":"crypto","symbol":"BTC","asset_name":"Bitcoin","broker":"revolut_x",
                        "quantity":"0.01","average_cost":"50000","invested_amount":"500",
                        "realized_profit":"0","currency":"EUR","price_status":"missing"
                    }],
                    "invested_amount":"500","realized_profit":"0","currency":"EUR","missing_prices":1
                }""".trimIndent(),
            ),
        )

        val portfolio = client.getInvestmentPortfolio("token")

        assertEquals(1, portfolio.missingPrices)
        assertEquals(null, portfolio.currentValue)
        assertEquals(null, portfolio.positions.single().currentPrice)
        assertEquals("/investments/portfolio", server.takeRequest().path)
    }

    @Test
    fun `schedule request includes day granularity and auto post choice`() {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{
                    "id":4,"type":"expense","name":"Rent","category":"housing","description":"",
                    "amount":"850","currency":"EUR","frequency":"monthly","frequency_interval":1,
                    "start_date":"2026-07-15","day_of_month":15,"timezone":"Europe/Sofia",
                    "auto_post":true,"status":"active","next_occurrence_date":"2026-07-15"
                }""".trimIndent(),
            ),
        )

        val schedule = client.createSchedule(
            "token",
            TransactionScheduleRequest(
                type = "expense",
                name = "Rent",
                category = "housing",
                amount = "850",
                frequency = "monthly",
                startDate = "2026-07-15",
                dayOfMonth = 15,
                timezone = "Europe/Sofia",
                autoPost = true,
            ),
        )

        val request = server.takeRequest()
        assertEquals("Rent", schedule.name)
        assertEquals("POST", request.method)
        assertEquals("/schedules", request.path)
        assertTrue(request.body.readUtf8().contains("\"day_of_month\":15"))
    }

    @Test
    fun `budget creation sends threshold and scope`() {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{
                    "id":8,"name":"Food","category":"food","amount":"400","currency":"EUR",
                    "period":"monthly","warning_threshold":80,"status":"active",
                    "period_start":"2026-07-01","period_end":"2026-07-31","spent_amount":"120",
                    "remaining_amount":"280","progress_percent":"30","alert_level":"none"
                }""".trimIndent(),
            ),
        )

        val budget = client.createBudget(
            "token",
            BudgetRequest("Food", "food", "400", period = "monthly", warningThreshold = 80),
        )

        assertEquals("120", budget.spentAmount)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"category\":\"food\""))
        assertTrue(body.contains("\"warning_threshold\":80"))
    }
}
