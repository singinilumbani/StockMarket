package com.example.stockmarket.background

// Is the app on screen right now? The worker stays out of the way when it is.
object AppVisibility {
    @Volatile
    var isVisible: Boolean = false
}

