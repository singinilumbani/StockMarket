package com.example.stockmarket.data

import kotlin.random.Random

data class Company (
    val symbol: String,
    val name : String,
    val sector : String,
    val price : Double,
     val changePct: Double,
    val history: List<Double> = emptyList()

)

val builtInCompanies = listOf(
    Company(
        "NBM", "National Bank of Malawi", "Banking",
        2010.0, 1.25,
        history = generateHistory(2010.0, 1)
    ),
    Company(
        "TNM", "Telekom Networks Malawi", "Telecommunications",
        24.5, -0.80,
        history = generateHistory(24.5, 2)
    ),
    Company(
        "AIRTEL", "Airtel Malawi", "Telecommunications",
        95.0, 2.15,
        history = generateHistory(95.0, 3)
    ),
    Company(
        "FDHB", "FDH Bank", "Banking",
        78.5, 1.42,
        history = generateHistory(78.5, 4)
    ),
    Company(
        "NBS", "NBS Bank", "Banking",
        42.3, -0.85,
        history = generateHistory(42.3, 5)
    ),
    Company(
        "NICO", "NICO Holdings", "Financial Services",
        125.0, 0.74,
        history = generateHistory(125.0, 6)
    ),
    Company(
        "ILLOVO", "Illovo Sugar Malawi", "Agriculture",
        850.0, 1.10,
        history = generateHistory(850.0, 7)
    ),
    Company(
        "PCL", "Press Corporation", "Conglomerate",
        2800.0, -0.45,
        history = generateHistory(2800.0, 8)
    ),
    Company(
        "FMBCH", "FMB Capital Holdings", "Financial Services",
        650.0, 2.35,
        history = generateHistory(650.0, 9)
    ),
    Company(
        "SUNBIRD", "Sunbird Tourism", "Tourism",
        320.0, 0.95,
        history = generateHistory(320.0, 10)
    ),
    Company(
        "MPICO", "MPICO", "Property",
        18.2, -1.25,
        history = generateHistory(18.2, 11)
    ),
    Company(
        "ICON", "ICON Properties", "Property",
        45.0, 0.65,
        history = generateHistory(45.0, 12)
    )
)

fun generateHistory(start: Double, seed: Int, days: Int = 30): List<Double> {
    val random = Random(seed)
    var price = start
    val list = mutableListOf<Double>()
    repeat(days) {
        price = price * ( 1 + random.nextDouble(-0.03 ,0.035))
        list.add(price)
    }
    return list
}
