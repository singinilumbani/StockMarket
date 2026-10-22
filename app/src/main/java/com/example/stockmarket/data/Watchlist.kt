package com.example.stockmarket.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ONE shared copy for the whole app. A list keeps the order you added them in.
object WatchlistStore {

    private val _symbols = MutableStateFlow(listOf("NBM", "AIRTEL"))   // SAMPLE starting list
    val symbols: StateFlow<List<String>> = _symbols.asStateFlow()

    fun toggle(symbol: String) {
        _symbols.update { list ->
            if (symbol in list) list - symbol else list + symbol
        }
    }
}

