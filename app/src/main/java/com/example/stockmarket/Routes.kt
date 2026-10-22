package com.example.stockmarket
import com.example.stockmarket.data.OrderSide



object Routes {

    const val LOGIN = "login"
    const val HOME = "home"
    const val MARKET = "market"
    const val PORTFOLIO = "portfolio"
    const val HISTORY = "history"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val COMPANY = "company/{symbol}"
    const val SETTINGS = "settings"
    const val WALLET = "wallet"

    const val REGISTER = "register"
    const val ORDER = "order/{symbol}/{side}"

    const val SIGNUP = "signup"
    const val ORDER_STATUS = "order_status/{orderId}"

    fun order(symbol: String, side: OrderSide) = "order/$symbol/${side.name}"
    fun orderStatus(orderId: Int) = "order_status/$orderId"

    fun company(symbol: String) = "company/$symbol"
}