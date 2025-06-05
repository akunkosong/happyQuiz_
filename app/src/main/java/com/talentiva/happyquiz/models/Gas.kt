package com.talentiva.happyquiz.models

data class GasResponse<T>(
    val status: String,
    val data: List<T>?
)
