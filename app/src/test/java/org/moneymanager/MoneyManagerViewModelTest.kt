package org.moneymanager

import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.moneymanager.data.MoneyManagerApi
import org.moneymanager.data.SessionStore
import org.moneymanager.model.AuthResult
import org.moneymanager.model.Category
import org.moneymanager.model.ImportResult
import org.moneymanager.model.Transaction
import org.moneymanager.model.TransactionRequest
import org.moneymanager.model.TransactionSummary
import org.moneymanager.model.User

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyManagerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial session loads month-keyed content`() = runTest(dispatcher) {
        val api = FakeApi()
        val store = FakeSessionStore(token = "token", email = "person@example.com")
        val viewModel = MoneyManagerViewModel(api, store, dispatcher)

        advanceUntilIdle()

        assertEquals(MonthLoadPhase.Content, viewModel.state.value.monthLoadPhase)
        assertEquals(viewModel.state.value.month, viewModel.state.value.loadedMonth)
        assertEquals("EUR", viewModel.state.value.summary?.currency)
        assertTrue(viewModel.state.value.hasMonthContent)
    }

    @Test
    fun `failed month load never fabricates zero content`() = runTest(dispatcher) {
        val api = FakeApi(monthFailure = true)
        val viewModel = MoneyManagerViewModel(api, FakeSessionStore("token", "person@example.com"), dispatcher)

        advanceUntilIdle()

        assertEquals(MonthLoadPhase.Failure, viewModel.state.value.monthLoadPhase)
        assertNull(viewModel.state.value.summary)
        assertTrue(viewModel.state.value.transactions.isEmpty())
        assertFalse(viewModel.state.value.hasMonthContent)
    }

    @Test
    fun `invalid registration reports a scoped auth error`() = runTest(dispatcher) {
        val viewModel = MoneyManagerViewModel(FakeApi(), FakeSessionStore(), dispatcher)
        viewModel.toggleAuthMode()
        viewModel.updateEmail("not-an-email")
        viewModel.updatePassword("short")
        viewModel.updateConfirmPassword("different")

        viewModel.submitAuth()

        assertEquals("Enter a valid email address", viewModel.state.value.authError)
        assertNull(viewModel.state.value.monthError)
    }

    @Test
    fun `duplicate auth submission starts only one request`() = runTest(dispatcher) {
        val api = FakeApi()
        val viewModel = MoneyManagerViewModel(api, FakeSessionStore(), dispatcher)
        viewModel.updateEmail("person@example.com")
        viewModel.updatePassword("correct-password")

        viewModel.submitAuth()
        viewModel.submitAuth()
        advanceUntilIdle()

        assertEquals(1, api.loginRequests)
    }

    @Test
    fun `export defaults to selected month boundaries`() = runTest(dispatcher) {
        val viewModel = MoneyManagerViewModel(
            FakeApi(),
            FakeSessionStore("token", "person@example.com"),
            dispatcher,
        )
        advanceUntilIdle()
        viewModel.previousMonth()
        advanceUntilIdle()

        viewModel.openExportDialog()

        val selectedMonth = YearMonth.now().minusMonths(1)
        assertEquals(selectedMonth.atDay(1).toString(), viewModel.state.value.exportFrom)
        assertEquals(selectedMonth.atEndOfMonth().toString(), viewModel.state.value.exportTo)
    }

    @Test
    fun `deleting account clears encrypted session state`() = runTest(dispatcher) {
        val store = FakeSessionStore("token", "person@example.com")
        val api = FakeApi()
        val viewModel = MoneyManagerViewModel(api, store, dispatcher)
        advanceUntilIdle()

        viewModel.deleteAccount()
        advanceUntilIdle()

        assertTrue(api.accountDeleted)
        assertNull(viewModel.state.value.token)
        assertNull(store.getToken())
    }

    @Test
    fun `editing a transaction across months invalidates source and destination caches`() = runTest(dispatcher) {
        val api = FakeApi()
        val viewModel = MoneyManagerViewModel(api, FakeSessionStore("token", "person@example.com"), dispatcher)
        advanceUntilIdle()

        val destination = YearMonth.now()
        val source = destination.minusMonths(1)
        viewModel.previousMonth()
        advanceUntilIdle()
        viewModel.editTransaction(viewModel.state.value.transactions.single())
        viewModel.updateFormOccurredAt(destination.atDay(1).toString())
        viewModel.saveTransaction()
        advanceUntilIdle()

        assertEquals(destination.toString(), viewModel.state.value.month)
        viewModel.previousMonth()
        advanceUntilIdle()
        assertEquals(2, api.transactionRequests[source.toString()])
        assertEquals(2, api.transactionRequests[destination.toString()])
    }

    @Test
    fun `delete invalidates its source month when navigation happens during request`() = runTest(dispatcher) {
        val api = FakeApi()
        val viewModel = MoneyManagerViewModel(api, FakeSessionStore("token", "person@example.com"), dispatcher)
        advanceUntilIdle()

        val source = YearMonth.now().minusMonths(1)
        viewModel.previousMonth()
        advanceUntilIdle()
        api.onDelete = viewModel::nextMonth
        viewModel.deleteTransaction(viewModel.state.value.transactions.single().id)
        advanceUntilIdle()

        assertEquals(YearMonth.now().toString(), viewModel.state.value.month)
        viewModel.previousMonth()
        advanceUntilIdle()
        assertEquals(2, api.transactionRequests[source.toString()])
    }

    @Test
    fun `amounts with more than two decimals are not form-valid`() {
        val state = MoneyManagerUiState(
            formAmount = "1.999",
            formCategory = "food",
            formOccurredAt = LocalDate.now().toString(),
        )

        assertFalse(state.isFormValid)
    }
}

private class FakeSessionStore(
    private var token: String? = null,
    private var email: String = "",
) : SessionStore {
    private var pushDeviceID: Int? = null
    override fun getToken(): String? = token
    override fun getEmail(): String = email
    override fun saveSession(token: String, email: String) {
        this.token = token
        this.email = email
    }
    override fun clearToken() {
        token = null
        email = ""
    }
    override fun getPushDeviceID(): Int? = pushDeviceID
    override fun savePushDeviceID(id: Int) { pushDeviceID = id }
    override fun clearPushDeviceID() { pushDeviceID = null }
}

private class FakeApi(
    private val monthFailure: Boolean = false,
) : MoneyManagerApi {
    var accountDeleted = false
    var loginRequests = 0
    val transactionRequests = mutableMapOf<String, Int>()
    var onDelete: (() -> Unit)? = null

    override fun checkHealth(): Boolean = true
    override fun register(email: String, password: String): AuthResult =
        AuthResult("token", User(1, email))
    override fun login(email: String, password: String): AuthResult {
        loginRequests += 1
        return AuthResult("token", User(1, email))
    }
    override fun getSummary(token: String, month: String): TransactionSummary {
        if (monthFailure) error("network unavailable")
        return TransactionSummary(month, "1000.00", "125.00", "875.00", "EUR", 1)
    }
    override fun getTransactions(
        token: String,
        month: String,
        type: String?,
        category: String?,
    ): List<Transaction> {
        transactionRequests[month] = transactionRequests.getOrDefault(month, 0) + 1
        return listOf(
            Transaction(1, "expense", "food", "Lunch", "125.00", "EUR", YearMonth.parse(month).atDay(1).toString()),
        )
    }
    override fun getCategories(token: String, type: String): List<Category> = listOf(
        Category(if (type == "expense") 1 else 2, type, if (type == "expense") "food" else "salary", true),
    )
    override fun createCategory(token: String, type: String, name: String): Category =
        Category(3, type, name, false)
    override fun deleteCategory(token: String, id: Int) = Unit
    override fun exportTransactionsCsv(token: String, from: String, to: String): String = "amount\n125.00"
    override fun importRevolutCsv(token: String, contents: ByteArray): ImportResult = ImportResult(1, 0, 0)
    override fun createTransaction(token: String, transaction: TransactionRequest): Transaction =
        Transaction(2, transaction.type, transaction.category, transaction.description, transaction.amount, transaction.currency, transaction.occurredAt)
    override fun updateTransaction(token: String, id: Int, transaction: TransactionRequest): Transaction =
        Transaction(id, transaction.type, transaction.category, transaction.description, transaction.amount, transaction.currency, transaction.occurredAt)
    override fun deleteTransaction(token: String, id: Int) {
        onDelete?.invoke()
    }
    override fun deleteAccount(token: String) {
        accountDeleted = true
    }
}
