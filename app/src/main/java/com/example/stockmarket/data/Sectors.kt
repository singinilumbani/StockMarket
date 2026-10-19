package com.example.stockmarket.data


private val sectorBySymbol = mapOf(
    "NBM" to "Banking",
    "NBS" to "Banking",
    "FDHB" to "Banking",
    "FMBCH" to "Banking",
    "STANDARD" to "Banking",
    "NICO" to "Insurance",
    "OMU" to "Insurance",
    "NITL" to "Investment",
    "AIRTEL" to "Telecoms",
    "TNM" to "Telecoms",
    "ICON" to "Property",
    "MPICO" to "Property",
    "BHL" to "Hospitality",
    "SUNBIRD" to "Hospitality",
    "ILLOVO" to "Agriculture",
    "PCL" to "Conglomerate"
)

val Company.sector: String
    get() = sectorBySymbol[symbol] ?: "Other"