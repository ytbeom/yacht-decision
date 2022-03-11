package com.example.yachtdecision.service

import com.example.yachtdecision.choicePriority
import com.example.yachtdecision.discardPriority
import com.example.yachtdecision.dto.Decision
import com.example.yachtdecision.dto.State
import com.example.yachtdecision.isMatched
import com.example.yachtdecision.toUpperScoreName
import org.springframework.stereotype.Service

@Service
class DecisionService {
    fun decide(state: State): Decision {
        val player = state.player
        require(state.scoreBoard.keys.contains(player))

        val trial = state.trial

        val dices = state.dices.foldIndexed(mutableMapOf<Int, MutableList<Int>>()) { index, acc, item ->
            if (acc.containsKey(item)) {
                acc[item]?.add(index)
            }
            else {
                acc[item] = mutableListOf(index)
            }
            acc
        }

        val scores = state.scoreBoard[player]!!
        val possibleScores = scores.filter {
            it.value == null
        }.map {
            it.key
        }.toSet()

        check(possibleScores.isNotEmpty())

        return decide(trial, dices, possibleScores)
    }

    private fun decide(trial: Int, dices: Map<Int, List<Int>>, possibleScores: Set<String>): Decision {
        return if (trial == 3) {
            decideByCurrent(dices, possibleScores)
        }
        else {
            decideByExpectation(dices, possibleScores)
        }

    }

    private fun decideByCurrent(dices: Map<Int, List<Int>>, possibleScores: Set<String>): Decision {
        val matchedScores = possibleScores.filter {
            it.isMatched(dices)
        }.sortedBy {
            it.choicePriority()
        }

        return Decision(
            choice = matchedScores.firstOrNull() ?: possibleScores.minByOrNull {
                it.discardPriority(dices)
            }
        )
    }

    private fun decideByExpectation(dices: Map<Int, List<Int>>, possibleScores: Set<String>): Decision {
        val matchedScores = possibleScores.filter {
            it.isMatched(dices) && it != "choice"
        }.sortedBy {
            it.choicePriority()
        }
        val maxSameCount = getMaxSameCount(dices)

        return if (matchedScores.isNotEmpty()) {
            // yacht, largeStraight, fullHouse
            if (matchedScores[0].choicePriority() < 2) {
                Decision(
                    choice = matchedScores[0]
                )
            }
            else if (matchedScores[0] == "smallStraight") {
                if (possibleScores.contains("largeStraight")) {
                    getSmallStraightIndices(dices)
                }
                else {
                    Decision(
                        choice = "smallStraight"
                    )
                }
            }
            else if (matchedScores[0] == "fourKind") {
                if (possibleScores.contains("yacht")) {
                    Decision(
                        keep = maxSameCount.values.first()
                    )
                }
                else {
                    Decision(
                        choice = "fourKind"
                    )
                }
            }
            // 상단 항목이 matchedScores에 남아있는 경우에 해당
            else {
                Decision(
                    keep = maxSameCount.entries.first().value
                )
            }
        }
        else {
            doGidoMeta(dices, maxSameCount, possibleScores)
        }
    }

    private fun doGidoMeta(dices: Map<Int, List<Int>>, maxSameCount: Map<Int, List<Int>>,
                           possibleScores: Set<String>): Decision {
        // 가능한 경우가 choice 밖에 없는 경우: 4 이상의 값만 남김
        if (possibleScores.first() == "choice") {
            return getLargeDices(dices)
        }

        val possibleUpperCategories = dices.entries.sortedBy {
            it.value.size
        }.reversed().filter {
            possibleScores.contains(it.key.toUpperScoreName())
        }.map {
            it.key
        }

        return when (dices.size) {
            5, 4 -> {
                when {
                    possibleScores.contains("largeStraight") -> {
                        getDecisionForStraight(dices, 3)
                    }
                    possibleScores.contains("smallStraight") -> {
                        getDecisionForStraight(dices, 2)
                    }
                    possibleScores.contains("yacht")
                        || possibleScores.contains("fourKind")
                        || possibleScores.contains("fullHouse") -> {
                        Decision(
                            keep = maxSameCount.values.first()
                        )
                    }
                    else -> {
                        if (possibleUpperCategories.isNotEmpty()) {
                            Decision(
                                keep = dices[possibleUpperCategories.first()]!!
                            )
                        }
                        else {
                            Decision()
                        }
                    }
                }
            }
            3 -> {
                when {
                    possibleScores.contains("fullHouse") -> {
                        Decision(
                            keep = maxSameCount.values.flatten()
                        )
                    }
                    possibleScores.contains("largeStraight") -> {
                        getDecisionForStraight(dices, 3)
                    }
                    possibleScores.contains("smallStraight") -> {
                        getDecisionForStraight(dices, 2)
                    }
                    else -> {
                        if (maxSameCount.keys.size == 1) {
                            if (possibleScores.contains("yacht") || possibleScores.contains("fourKind")) {
                                Decision(
                                    keep = maxSameCount.values.first()
                                )
                            }
                            else {
                                if (possibleUpperCategories.isNotEmpty()) {
                                    Decision(
                                        keep = dices[possibleUpperCategories.first()]!!
                                    )
                                }
                                else {
                                    Decision()
                                }
                            }
                        }
                        else {
                            if (possibleUpperCategories.isNotEmpty()) {
                                Decision(
                                    keep = dices[possibleUpperCategories.first()]!!
                                )
                            }
                            else {
                                Decision(
                                    keep = maxSameCount.values.first()
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                if (possibleScores.contains("yacht") || possibleScores.contains("fourKind")) {
                    Decision(
                        keep = maxSameCount.values.first()
                    )
                }
                else if (possibleScores.contains("fullHouse")) {
                    Decision(
                        keep = maxSameCount.values.first().subList(0, 3)
                    )
                }
                else {
                    if (possibleScores.contains("smallStraight")) {
                        getDecisionForStraight(dices, 2)
                    }
                    else if (possibleScores.contains("largeStraight")) {
                        getDecisionForStraight(dices, 3)
                    }
                    else {
                        val less = dices.keys.first {
                            it != maxSameCount.keys.first()
                        }
                        if (possibleScores.contains(less.toUpperScoreName())) {
                            Decision(
                                keep = dices[less]!!
                            )
                        }
                        else {
                            Decision()
                        }
                    }
                }
            }
            else -> {
                Decision()
            }
        }
    }

    private fun getMaxSameCount(dices: Map<Int, List<Int>>): Map<Int, List<Int>> {
        val maxCount = dices.values.maxOf { it.size }
        return dices.filter {
            it.value.size == maxCount
        }
    }

    private fun getSmallStraightIndices(dices: Map<Int, List<Int>>): Decision {
        val indices = when {
            dices.keys.containsAll(listOf(1, 2, 3, 4)) -> {
                listOf(dices[1]!!.first(), dices[2]!!.first(), dices[3]!!.first(), dices[4]!!.first())
            }
            dices.keys.containsAll(listOf(2, 3, 4, 5)) -> {
                listOf(dices[2]!!.first(), dices[3]!!.first(), dices[4]!!.first(), dices[5]!!.first())
            }
            else -> {
                listOf(dices[3]!!.first(), dices[4]!!.first(), dices[5]!!.first(), dices[6]!!.first())
            }
        }
        return Decision(
            keep = indices
        )
    }

    private fun getLargeDices(dices: Map<Int, List<Int>>): Decision {
        return Decision(
            keep = dices.entries.filter {
                it.key >= 4
            }.flatMap {
                it.value
            }
        )
    }

    // straight를 위한 decision, 여기서 straight type에 따라 sequence length를 check와 비교해서 앞뒤 2 차이나는 것들까지 포함
    private fun getDecisionForStraight(dices: Map<Int, List<Int>>, check: Int): Decision {
        val sequence = getSequence(dices).toMutableList()
        if (sequence.size <= check) {
            if (dices.keys.contains(sequence.first() - 2)) {
                sequence.add(0, sequence.first() - 2)
            }
            if (dices.keys.contains(sequence.last() + 2)) {
                sequence.add(sequence.last() + 2)
            }
        }
        return Decision(
            keep = sequence.map {
                dices[it]!!.first()
            }
        )
    }

    private fun getSequence(dices: Map<Int, List<Int>>): List<Int> {
        val diceList = dices.keys.toList().sorted()
        val parseIndexList = mutableListOf(0)
        for (i in 1 until diceList.size) {
            if (diceList[i] - diceList[i-1] != 1) {
                parseIndexList.add(i)
            }
        }
        parseIndexList.add(diceList.size)
        return parseIndexList.mapIndexed { index, i ->
            val end = if (index == parseIndexList.size - 1) i else parseIndexList[index + 1]
            diceList.subList(i, end)
        }.maxByOrNull { it.size }!!
    }
}
