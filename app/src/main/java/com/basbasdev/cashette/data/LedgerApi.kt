package com.basbasdev.cashette.data

import com.basbasdev.cashette.data.model.AccountDto
import com.basbasdev.cashette.data.model.BudgetDto
import com.basbasdev.cashette.data.model.BudgetSummaryDto
import com.basbasdev.cashette.data.model.CategoryDto
import com.basbasdev.cashette.data.model.CreateAccountBody
import com.basbasdev.cashette.data.model.CreateBudgetBody
import com.basbasdev.cashette.data.model.CreateDebtBody
import com.basbasdev.cashette.data.model.CreateSubscriptionBody
import com.basbasdev.cashette.data.model.CreateTransactionBody
import com.basbasdev.cashette.data.model.CreateTransferBody
import com.basbasdev.cashette.data.model.DebtDto
import com.basbasdev.cashette.data.model.DueSubscriptionsDto
import com.basbasdev.cashette.data.model.ErrorDto
import com.basbasdev.cashette.data.model.RecordSubscriptionBody
import com.basbasdev.cashette.data.model.RepayDebtBody
import com.basbasdev.cashette.data.model.SubscriptionDto
import com.basbasdev.cashette.data.model.TransactionDto
import com.basbasdev.cashette.data.model.UpdateAccountBody
import com.basbasdev.cashette.data.model.UpdateBudgetBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The Go handler parses dates with `time.Parse("2006-01-02", …)` and, on failure,
 * **drops the filter and returns everything unfiltered** rather than erroring. A wrong
 * format therefore looks like working code with wrong numbers, so no caller formats a
 * date itself.
 */
private val API_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun LocalDate.asApiDate(): String = format(API_DATE)

/**
 * Request *bodies* bind to Go's `time.Time`, which unmarshals RFC3339 and rejects a
 * bare date. Query parameters take the bare date above. Getting these two the wrong way
 * round is a 400 on writes and silently unfiltered results on reads.
 */
fun LocalDate.asApiTimestamp(): String = "${format(API_DATE)}T00:00:00Z"

@Singleton
class LedgerApi @Inject constructor(
    private val client: HttpClient,
    @Named("apiBaseUrl") private val baseUrl: String,
) {
    suspend fun transactions(userId: String, from: LocalDate, to: LocalDate): List<TransactionDto> =
        getList("/api/transactions") {
            parameter("user_id", userId)
            parameter("from_date", from.asApiDate())
            parameter("to_date", to.asApiDate())
        }

    suspend fun accounts(userId: String): List<AccountDto> =
        getList("/api/accounts") { parameter("user_id", userId) }

    suspend fun categories(userId: String): List<CategoryDto> =
        getList("/api/categories") { parameter("user_id", userId) }

    suspend fun budgets(userId: String, month: Int, year: Int): List<BudgetDto> =
        getList("/api/budgets") {
            parameter("user_id", userId)
            parameter("month", month)
            parameter("year", year)
        }

    suspend fun budgetSummary(userId: String, month: Int, year: Int): BudgetSummaryDto =
        get("/api/budgets/summary") {
            parameter("user_id", userId)
            parameter("month", month)
            parameter("year", year)
        }

    suspend fun subscriptions(userId: String): List<SubscriptionDto> =
        getList("/api/subscriptions") { parameter("user_id", userId) }

    suspend fun dueSubscriptions(userId: String): DueSubscriptionsDto =
        get("/api/subscriptions/due") { parameter("user_id", userId) }

    suspend fun debts(userId: String): List<DebtDto> =
        getList("/api/debts") { parameter("user_id", userId) }

    // ── Writes ───────────────────────────────────────────────────────────────

    suspend fun createAccount(body: CreateAccountBody) = send(HttpMethod.Post, "/api/accounts", body)

    suspend fun updateAccount(id: String, body: UpdateAccountBody) =
        send(HttpMethod.Put, "/api/accounts/$id", body)

    suspend fun deleteAccount(id: String) = send(HttpMethod.Delete, "/api/accounts/$id", null)

    suspend fun createTransaction(body: CreateTransactionBody) =
        send(HttpMethod.Post, "/api/transactions", body)

    suspend fun deleteTransaction(id: String) = send(HttpMethod.Delete, "/api/transactions/$id", null)

    suspend fun createTransfer(body: CreateTransferBody) = send(HttpMethod.Post, "/api/transfers", body)

    suspend fun createBudget(body: CreateBudgetBody) = send(HttpMethod.Post, "/api/budgets", body)

    suspend fun updateBudget(id: String, body: UpdateBudgetBody) =
        send(HttpMethod.Put, "/api/budgets/$id", body)

    suspend fun deleteBudget(id: String) = send(HttpMethod.Delete, "/api/budgets/$id", null)

    suspend fun createSubscription(body: CreateSubscriptionBody) =
        send(HttpMethod.Post, "/api/subscriptions", body)

    suspend fun deleteSubscription(id: String) = send(HttpMethod.Delete, "/api/subscriptions/$id", null)

    suspend fun recordSubscription(id: String, userId: String, body: RecordSubscriptionBody) =
        send(HttpMethod.Post, "/api/subscriptions/$id/record", body) { parameter("user_id", userId) }

    suspend fun createDebt(body: CreateDebtBody) = send(HttpMethod.Post, "/api/debts", body)

    suspend fun deleteDebt(id: String) = send(HttpMethod.Delete, "/api/debts/$id", null)

    suspend fun repayDebt(id: String, userId: String, body: RepayDebtBody) =
        send(HttpMethod.Post, "/api/debts/$id/pay", body) { parameter("user_id", userId) }

    /**
     * Writes return the created row, which no caller needs — only whether it worked. The
     * server's own error text is surfaced when it sends one, because "name is required"
     * beats "accounts failed (400)".
     */
    private suspend fun send(
        method: HttpMethod,
        path: String,
        body: Any?,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ) {
        val response = client.request("$baseUrl$path") {
            this.method = method
            body?.let { setBody(it) }
            block()
        }
        if (!response.status.isSuccess()) {
            val detail = runCatching { response.body<ErrorDto>().error }.getOrNull()
            error(detail ?: "Request failed (${response.status.value})")
        }
    }

    /**
     * `expectSuccess` is off on the shared client so the 401 validator can sign out
     * cleanly, which means a non-2xx arrives here as a normal response. Turning it into
     * an exception is this layer's job; callers then wrap it in a Result.
     */
    private suspend inline fun <reified T> get(
        path: String,
        crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): T {
        val response = client.get("$baseUrl$path") { block() }
        if (!response.status.isSuccess()) {
            error("${path.substringAfterLast('/')} failed (${response.status.value})")
        }
        return response.body()
    }

    /**
     * Go marshals a nil slice as `null`, not `[]`, so every list endpoint returns null
     * for an empty result — an empty month, a user with no subscriptions. Decoding that
     * as a list throws, which would turn "nothing here yet" into an error state on the
     * most common screen in the app.
     */
    private suspend inline fun <reified T> getList(
        path: String,
        crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): List<T> = get<List<T>?>(path, block) ?: emptyList()
}
