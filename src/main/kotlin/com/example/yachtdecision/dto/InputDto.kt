package com.example.yachtdecision.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class InputDto(
    @JsonProperty("state")
    val state: State
)

data class State(
    val turn: Int,
    val trial: Int,
    val player: String,
    val dices: List<Int>,
    val scoreBoard: Map<String, Map<String, Int?>>,
)


