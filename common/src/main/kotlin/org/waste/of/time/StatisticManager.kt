package org.waste.of.time

import org.waste.of.time.WorldTools.highlight

object StatisticManager {
    var chunks = 0
    var entities = 0
    var players = 0
    var container = 0
    val dimensions = mutableSetOf<String>()

    fun reset() {
        chunks = 0
        entities = 0
        players = 0
        container = 0
        dimensions.clear()
    }

    val message: String
        get() {
            val elements = mutableListOf<String>()

            if (chunks != 0) {
                elements.add("${highlight(String.format("%,d", chunks))} chunks")
            }

            if (entities != 0) {
                elements.add("${highlight(String.format("%,d", entities))} entities")
            }

            if (players != 0) {
                elements.add("${highlight(String.format("%,d", players))} players")
            }

            if (container != 0) {
                elements.add("${highlight(String.format("%,d", container))} containers")
            }

            return if (elements.isEmpty()) {
                "Nothing saved yet."
            } else {
                "Saved ${elements.joinWithAnd()}${
                    if (dimensions.isNotEmpty()) {
                        " in ${highlight(dimensions.joinToString())}"
                    } else {
                        ""
                    }
                }"
            }
        }

    private fun List<String>.joinWithAnd() =
        when (this.size) {
            0 -> ""
            1 -> this[0]
            else -> this.dropLast(1).joinToString() + " and " + this.last()
        }
}