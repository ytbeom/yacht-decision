package com.example.yachtdecision.dto

data class OutputDto(
    val decision: Decision
)

data class Decision(
    val keep: List<Int> = emptyList(),
    val choice: String? = null
)
