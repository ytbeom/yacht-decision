package com.example.yachtdecision.controller

import com.example.yachtdecision.dto.InputDto
import com.example.yachtdecision.dto.OutputDto
import com.example.yachtdecision.service.DecisionService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val logger = LoggerFactory.getLogger("yacht")

@RestController
class YachtController(
    private val decisionService: DecisionService
) {
    @PostMapping("/decide")
    fun decide(
        @RequestBody inputDto: InputDto
    ): OutputDto {
        println("${inputDto.state.turn}, ${inputDto.state.trial}")
        val decision = decisionService.decide(inputDto.state)

        assert(inputDto.state.trial != 3 || (inputDto.state.trial == 3 && decision.choice != null))

        return OutputDto(
            decision = decision
        )
    }
}
