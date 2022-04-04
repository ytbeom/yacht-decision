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
        val notScoredCategorySet = scores.filter {
            it.value == null
        }.map {
            it.key
        }.toSet()

        check(notScoredCategorySet.isNotEmpty())

        return decide(trial, dices, notScoredCategorySet)
    }

    private fun decide(trial: Int, dices: Map<Int, List<Int>>, notScoredCategorySet: Set<String>): Decision {
        return if (trial == 3) {
            decideByCurrent(dices, notScoredCategorySet)
        }
        else {
            decideByExpectation(dices, notScoredCategorySet)
        }

    }

    private fun decideByCurrent(dices: Map<Int, List<Int>>, notScoredCategorySet: Set<String>): Decision {
        val matchedCategoryList = notScoredCategorySet.filter {
            it.isMatched(dices)
        }.sortedBy {
            it.choicePriority()
        }

        return Decision(
            choice = matchedCategoryList.firstOrNull() ?: notScoredCategorySet.minByOrNull {
                it.discardPriority(dices)
            }
        )
    }

    private fun decideByExpectation(dices: Map<Int, List<Int>>, notScoredCategorySet: Set<String>): Decision {
        val matchedCategoryList = notScoredCategorySet.filter {
            it.isMatched(dices) && it != "choice"
        }.sortedBy {
            it.choicePriority()
        }
        val maxSameCountMap = getMaxSameCountMap(dices)

        return if (matchedCategoryList.isNotEmpty()) {
            if (matchedCategoryList.first() == "yacht"
                || matchedCategoryList.first() == "largeStraight"
                || matchedCategoryList.first() == "fullHouse") {
                Decision(
                    choice = matchedCategoryList.first()
                )
            }
            else if (matchedCategoryList.first() == "smallStraight") {
                if (notScoredCategorySet.contains("largeStraight")) {
                    getSmallStraightIndices(dices)
                }
                else {
                    Decision(
                        choice = "smallStraight"
                    )
                }
            }
            else if (matchedCategoryList.first() == "fourKind") {
                if (notScoredCategorySet.contains("yacht")) {
                    Decision(
                        keep = maxSameCountMap.values.first()
                    )
                }
                else {
                    Decision(
                        choice = "fourKind"
                    )
                }
            }
            // 상단 항목만 matchedCategoryList에 남아있는 경우에 해당
            // 상단 항목이 matchedCategoryList에 포함되어 있다면 눈의 개수가 3개 이상이므로
            // maxSameCount의 first가 곧 해당 눈이 됨
            else {
                Decision(
                    keep = maxSameCountMap.entries.first().value
                )
            }
        }
        else {
            doGidoMeta(dices, maxSameCountMap, notScoredCategorySet)
        }
    }

    private fun doGidoMeta(
        dices: Map<Int, List<Int>>,
        maxSameCountMap: Map<Int, List<Int>>,
        notScoredCategorySet: Set<String>
    ): Decision {
        if (notScoredCategorySet.size == 1 && notScoredCategorySet.contains("choice")) {
            return getLargeDices(dices)
        }

        val possibleUpperCategorySet = dices.filter {
            notScoredCategorySet.contains(it.key.toUpperScoreName())
        }.map {
            it.key
        }.toSet()

        return when (dices.size) {
            5 -> {
                when {
                    notScoredCategorySet.contains("largeStraight") -> {
                        getDecisionForStraight(dices, 3)
                    }
                    notScoredCategorySet.contains("smallStraight") -> {
                        getDecisionForStraight(dices, 2)
                    }
                    else -> {
                        val targetEye = possibleUpperCategorySet.toList().sortedBy { it }.reversed().firstOrNull()
                            ?: dices.maxOf { it.key }
                        Decision(
                            keep = dices[targetEye]!!
                        )
                    }
                }
            }
            4 -> {
                when {
                    notScoredCategorySet.contains("largeStraight") -> {
                        getDecisionForStraight(dices, 3)
                    }
                    notScoredCategorySet.contains("smallStraight") -> {
                        getDecisionForStraight(dices, 2)
                    }
                    notScoredCategorySet.contains("yacht")
                        || notScoredCategorySet.contains("fourKind")
                        || notScoredCategorySet.contains("fullHouse") -> {
                        val targetEye = if (possibleUpperCategorySet.contains(maxSameCountMap.keys.first())) {
                            maxSameCountMap.keys.first()
                        } else {
                            dices.maxOf { it.key }
                        }
                        Decision(
                            keep = dices[targetEye]!!
                        )
                    }
                    else -> {
                        if (possibleUpperCategorySet.isNotEmpty()) {
                            Decision(
                                keep = dices[possibleUpperCategorySet.first()]!!
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
                    notScoredCategorySet.contains("fullHouse") -> {
                        Decision(
                            keep = maxSameCountMap.values.flatten()
                        )
                    }
                    notScoredCategorySet.contains("largeStraight") -> {
                        getDecisionForStraight(dices, 3)
                    }
                    notScoredCategorySet.contains("smallStraight") -> {
                        getDecisionForStraight(dices, 2)
                    }
                    else -> {
                        println(maxSameCountMap.keys.size)
                        // 3, 1, 1
                        if (maxSameCountMap.keys.size == 1) {
                            if (notScoredCategorySet.contains("yacht") || notScoredCategorySet.contains("fourKind")) {
                                Decision(
                                    keep = maxSameCountMap.values.first()
                                )
                            }
                            else {
                                if (possibleUpperCategorySet.isNotEmpty()) {
                                    Decision(
                                        keep = dices[possibleUpperCategorySet.first()]!!
                                    )
                                }
                                else {
                                    Decision()
                                }
                            }
                        }
                        // 2, 2, 1
                        else {
                            if (possibleUpperCategorySet.isNotEmpty()) {
                                Decision(
                                    keep = dices[possibleUpperCategorySet.first()]!!
                                )
                            }
                            else {
                                Decision()
                            }
                        }
                    }
                }
            }
            2 -> {
                if (notScoredCategorySet.contains("yacht") || notScoredCategorySet.contains("fourKind")) {
                    Decision(
                        keep = maxSameCountMap.values.first()
                    )
                }
                else if (notScoredCategorySet.contains("fullHouse")) {
                    Decision(
                        keep = maxSameCountMap.values.first().subList(0, 3)
                    )
                }
                else {
                    if (notScoredCategorySet.contains("smallStraight")) {
                        getDecisionForStraight(dices, 2)
                    }
                    else if (notScoredCategorySet.contains("largeStraight")) {
                        getDecisionForStraight(dices, 3)
                    }
                    else {
                        val less = dices.keys.first {
                            it != maxSameCountMap.keys.first()
                        }
                        if (notScoredCategorySet.contains(less.toUpperScoreName())) {
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

    private fun getMaxSameCountMap(dices: Map<Int, List<Int>>): Map<Int, List<Int>> {
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
