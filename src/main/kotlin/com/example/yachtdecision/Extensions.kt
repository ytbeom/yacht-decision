package com.example.yachtdecision

fun String.isMatched(dices: Map<Int, List<Int>>): Boolean {
    return when (this) {
        "aces" -> dices.containsKey(1) && dices[1]!!.size >= 3
        "deuces" -> dices.containsKey(2) && dices[2]!!.size >= 3
        "threes" -> dices.containsKey(3) && dices[3]!!.size >= 3
        "fours" -> dices.containsKey(4) && dices[4]!!.size >= 3
        "fives" -> dices.containsKey(5) && dices[5]!!.size >= 3
        "sixes" -> dices.containsKey(6) && dices[6]!!.size >= 3
        "fourKind" -> dices.filter {
            it.value.size >= 4
        }.isNotEmpty()
        "fullHouse" -> "yacht".isMatched(dices) || (dices.size == 2 && dices.filter {
            it.value.size >= 2
        }.size == 2)
        "smallStraight" -> dices.keys.containsAll(listOf(1, 2, 3, 4))
            || dices.keys.containsAll(listOf(2, 3, 4, 5))
            || dices.keys.containsAll(listOf(3, 4, 5, 6))
        "largeStraight" -> dices.keys.containsAll(listOf(1, 2, 3, 4, 5))
            || dices.keys.containsAll(listOf(2, 3, 4, 5, 6))
        "yacht" -> dices.size == 1
        else -> true
    }
}

// match된 경우 먼저 고르는 순서를 의미
fun String.choicePriority(): Int {
    return when (this) {
        "aces" -> 4
        "deuces" -> 4
        "threes" -> 4
        "fours" -> 4
        "fives" -> 4
        "sixes" -> 4
        "fourKind" -> 3
        "fullHouse" -> 1
        "smallStraight" -> 2
        "largeStraight" -> 0
        "yacht" -> 0
        // choice
        else -> 5
    }
}

fun String.discardPriority(dices: Map<Int, List<Int>>): Int {
    val sum = dices.entries.fold(0) { acc, item ->
        acc + item.key * item.value.size
    }
    return when (this) {
        "aces" -> if (sum > 17) 1 else 0
        "choice" -> if (sum > 17) 1 else 0
        "deuces" -> 2
        "yacht" -> 3
        else -> 4
    }
}

fun Int.toUpperScoreName(): String {
    return when (this) {
        1 -> "aces"
        2 -> "deuces"
        3 -> "threes"
        4 -> "fours"
        5 -> "fives"
        6 -> "sixes"
        else -> ""
    }
}
