package com.basbasdev.cashette.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.core.money.toAmount
import com.basbasdev.cashette.core.text.sentenceCase
import com.basbasdev.cashette.data.LedgerApi
import com.basbasdev.cashette.data.LedgerRefresh
import com.basbasdev.cashette.data.model.AccountDto
import com.basbasdev.cashette.data.model.CategoryDto
import com.basbasdev.cashette.data.model.TransactionDto
import com.basbasdev.cashette.data.toDataMessage
import com.basbasdev.cashette.feature.home.Section
import com.basbasdev.cashette.feature.home.TxKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/** How far back the ledger is being read. Mirrors the web's period select. */
enum class Range(val label: String, val months: Long) {
    THIS_MONTH("This month", 0),
    LAST_3("Last 3 months", 2),
    LAST_6("Last 6 months", 5),
    YEAR("This year", 11),
}

data class LedgerEntry(
    val id: String,
    val title: String,
    val account: String,
    val category: String?,
    val date: LocalDate?,
    val dateLabel: String,
    /** Clock time the entry was recorded, in the device's zone. Null if never sent. */
    val timeLabel: String?,
    val amount: BigDecimal,
    val kind: TxKind,
)

/**
 * One day's worth of entries, so the list reads as a diary rather than a dump.
 *
 * [subtitle] carries the calendar date behind a relative header. "Today" answers when
 * only while today lasts, and a ledger is read back weeks later; an absolute header is
 * already the date and gets none.
 */
data class DayGroup(
    val header: String,
    val subtitle: String?,
    val net: BigDecimal,
    val entries: List<LedgerEntry>,
)

data class HistoryUiState(
    val range: Range = Range.THIS_MONTH,
    val query: String = "",
    val accountFilter: AccountDto? = null,
    val categoryFilter: CategoryDto? = null,
    val kindFilter: TxKind? = null,
    val all: Section<List<LedgerEntry>> = Section.Loading,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val working: Boolean = false,
) {
    val filtersActive: Boolean
        get() = query.isNotBlank() || accountFilter != null ||
            categoryFilter != null || kindFilter != null

    /** Filtering happens client-side: the range is already loaded, and it is instant. */
    val visible: List<LedgerEntry>
        get() = (all as? Section.Data)?.value.orEmpty().filter { entry ->
            (accountFilter == null || entry.account == accountFilter.name) &&
                (categoryFilter == null || entry.category == categoryFilter.name) &&
                (kindFilter == null || entry.kind == kindFilter) &&
                (query.isBlank() || listOfNotNull(entry.title, entry.category, entry.account)
                    .any { it.contains(query, ignoreCase = true) })
        }

    val groups: List<DayGroup>
        get() = visible
            .groupBy { it.dateLabel }
            .map { (label, entries) ->
                DayGroup(
                    header = label,
                    subtitle = entries.firstOrNull()?.date
                        ?.takeIf { label == TODAY || label == YESTERDAY }
                        ?.format(DAY),
                    net = entries.fold(BigDecimal.ZERO) { acc, e ->
                        when (e.kind) {
                            TxKind.INCOME -> acc + e.amount
                            TxKind.EXPENSE -> acc - e.amount
                            TxKind.TRANSFER -> acc
                        }
                    },
                    entries = entries,
                )
            }

    val totalIn: BigDecimal
        get() = visible.filter { it.kind == TxKind.INCOME }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }

    val totalOut: BigDecimal
        get() = visible.filter { it.kind == TxKind.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val api: LedgerApi,
    private val auth: AuthRepository,
    private val refresh: LedgerRefresh,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch { refresh.revision.drop(1).collect { load() } }
    }

    fun setRange(range: Range) {
        _state.update { it.copy(range = range, all = Section.Loading) }
        load()
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setAccount(a: AccountDto?) = _state.update { it.copy(accountFilter = a) }
    fun setCategory(c: CategoryDto?) = _state.update { it.copy(categoryFilter = c) }
    fun setKind(k: TxKind?) = _state.update { it.copy(kindFilter = k) }

    fun clearFilters() = _state.update {
        it.copy(query = "", accountFilter = null, categoryFilter = null, kindFilter = null)
    }

    fun load() {
        val userId = auth.currentUserId ?: return
        val range = _state.value.range
        val end = YearMonth.now().atEndOfMonth()
        val start = YearMonth.now().minusMonths(range.months).atDay(1)

        viewModelScope.launch {
            coroutineScope {
                val txs = async { runCatching { api.transactions(userId, start, end) } }
                val accounts = async { runCatching { api.accounts(userId) } }
                val categories = async { runCatching { api.categories(userId) } }

                val entries = txs.await().map { list -> list.map(::toEntry).sortedByDescending { it.date } }

                _state.update {
                    it.copy(
                        all = entries.fold(
                            onSuccess = { v -> Section.Data(v) },
                            onFailure = { e -> Section.Failed(e.toDataMessage()) },
                        ),
                        accounts = accounts.await().getOrNull().orEmpty(),
                        categories = categories.await().getOrNull().orEmpty(),
                    )
                }
            }
        }
    }

    fun delete(id: String) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            runCatching { api.deleteTransaction(id) }
            _state.update { it.copy(working = false) }
            refresh.invalidate()
            load()
        }
    }

    private fun toEntry(tx: TransactionDto): LedgerEntry {
        val date = runCatching { LocalDate.parse(tx.transactionDate.take(10)) }.getOrNull()
        return LedgerEntry(
            id = tx.id,
            title = (tx.note?.takeIf { it.isNotBlank() } ?: tx.categoryName ?: "Untitled")
                .sentenceCase(),
            account = tx.accountName.orEmpty(),
            category = tx.categoryName,
            date = date,
            dateLabel = date?.friendly() ?: "Undated",
            timeLabel = tx.createdAt?.recordedAt(),
            amount = tx.amount.toAmount(),
            kind = when (tx.type) {
                "income" -> TxKind.INCOME
                "transfer" -> TxKind.TRANSFER
                else -> TxKind.EXPENSE
            },
        )
    }
}

private val DAY = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
private val CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

private const val TODAY = "Today"
private const val YESTERDAY = "Yesterday"

/**
 * `transaction_date` is a date — it says which day the money moved, never what time. The
 * clock lives on `created_at`, which is `timestamp without time zone` holding UTC, so it
 * arrives with no offset to read. Taking it at face value would file a WIB lunch at five
 * in the morning; it is anchored to UTC and rendered in the device's zone.
 *
 * The offset branch is not dead code — it covers the day that column becomes timestamptz.
 */
private fun String.recordedAt(): String? {
    val normalized = trim().replace(' ', 'T')
    return runCatching { OffsetDateTime.parse(normalized).toInstant() }
        .recoverCatching { LocalDateTime.parse(normalized).toInstant(ZoneOffset.UTC) }
        .getOrNull()
        ?.atZone(ZoneId.systemDefault())
        ?.format(CLOCK)
}

/** "Today" and "Yesterday" carry more than a date does, and only for two days a year. */
private fun LocalDate.friendly(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> TODAY
        today.minusDays(1) -> YESTERDAY
        else -> format(DAY)
    }
}

