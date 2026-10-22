package com.example.stockmarket.data

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

data class LoadState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: String? = null   // stays null until the first successful load
)

// ONE shared copy for the whole app.
object CompanyRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Starts with the built-in sample list, so there is always something to show
    private val _companies = MutableStateFlow(builtInCompanies)
    val companies: StateFlow<List<Company>> = _companies.asStateFlow()

    private val _loadState = MutableStateFlow(LoadState())
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    // One-time lookup, for screens that only need the price at this moment
    fun find(symbol: String): Company? = _companies.value.find { it.symbol == symbol }

    fun refresh() {
        if (_loadState.value.isLoading) return   // already busy, do not start a second request

        scope.launch {
            _loadState.update { it.copy(isLoading = true, error = null) }
            try {
                val dtos = Api.service.getCompanies()
                val list = dtos.map { it.toCompany() }

                if (list.isEmpty()) {
                    _loadState.update { it.copy(isLoading = false, error = "The server sent no companies") }
                    return@launch
                }

                _companies.value = list
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