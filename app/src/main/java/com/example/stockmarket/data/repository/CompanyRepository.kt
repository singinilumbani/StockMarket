package com.example.stockmarket.data.repository

import android.content.Context
import com.example.stockmarket.data.local.AppDatabase
import com.example.stockmarket.data.local.CompanyDao
import com.example.stockmarket.data.local.toCompany
import com.example.stockmarket.data.local.toEntity
import com.example.stockmarket.data.model.Company
import com.example.stockmarket.data.model.builtInCompanies
import com.example.stockmarket.data.remote.Api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import com.example.stockmarket.data.remote.toCompany

data class LoadState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: String? = null   // stays null until we have real prices, saved or downloaded
)

// ONE shared copy for the whole app.
object CompanyRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // null until init() runs
    private var dao: CompanyDao? = null

    // Starts with the built-in sample list. It is used only when the database is empty
    // AND the first download has not worked yet (a brand-new install with no internet).
    private val _companies = MutableStateFlow(builtInCompanies)
    val companies: StateFlow<List<Company>> = _companies.asStateFlow()

    private val _loadState = MutableStateFlow(LoadState())
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    // One-time lookup, for screens that only need the price at this moment
    fun find(symbol: String): Company? = _companies.value.find { it.symbol == symbol }

    // Call once, when the app starts
    fun init(context: Context) {
        if (dao != null) return
        val database = AppDatabase.get(context).companyDao()
        dao = database

        // SINGLE SOURCE OF TRUTH: whenever the table changes, the screens get the new list.
        // This runs once at start-up, so saved prices appear immediately, even offline.
        scope.launch {
            database.observeAll().collect { saved ->
                if (saved.isNotEmpty()) {
                    _companies.value = saved.map { it.toCompany() }
                    // Saved prices count as "loaded", so the skeleton is not needed
                    _loadState.update { state ->
                        if (state.lastUpdated == null) {
                            state.copy(lastUpdated = saved.first().updatedAt)
                        } else {
                            state
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        if (_loadState.value.isLoading) return   // already busy, do not start a second request

        scope.launch {
            _loadState.update { it.copy(isLoading = true, error = null) }
            try {
                val dtos = Api.service.getCompanies()

                if (dtos.isEmpty()) {
                    _loadState.update {
                        it.copy(
                            isLoading = false,
                            error = "The server sent no companies"
                        )
                    }
                    return@launch
                }

                val database = dao
                if (database != null) {
                    // Save to the database. The collector in init() then updates _companies.
                    database.replaceAll(dtos.map { it.toEntity() })
                } else {
                    // init() was not called: still work, just without saving
                    _companies.value = dtos.map { it.toCompany() }
                }

                _loadState.value = LoadState(
                    isLoading = false,
                    error = null,
                    lastUpdated = dtos.firstOrNull()?.updatedAt
                )
            } catch (e: CancellationException) {
                throw e   // never swallow this one, coroutines use it to stop work
            } catch (e: IOException) {
                fail("No internet connection")
            } catch (e: HttpException) {
                fail("The server returned an error (${e.code()})")
            } catch (e: IllegalArgumentException) {
                fail("The app's server address is wrong")
            } catch (e: Exception) {
                fail("Unexpected data from the server")
            }
        }
    }

    // Keeps the old list and records what went wrong
    private fun fail(message: String) {
        _loadState.update { it.copy(isLoading = false, error = message) }
    }
}