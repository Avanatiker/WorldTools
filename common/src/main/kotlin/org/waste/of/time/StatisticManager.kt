package org.waste.of.time

import org.waste.of.time.WorldTools.highlight

object StatisticManager {
    var chunks = 0
    var entities = 0
    var players = 0
    var containers = 0
    val dimensions = mutableSetOf<String>()

    fun reset() {
        chunks = 0
        entities = 0
        players = 0
        containers = 0
        dimensions.clear()
    }

    val message: String
        get() {
            val elements = mutableListOf<String>()

            if (chunks != 0) {
                elements.add("${highlight("%,d".format(chunks))} <lang:capture.chunks>")
            }

            if (entities != 0) {
                elements.add("${highlight("%,d".format(entities))} <lang:capture.entities>")
            }

            if (players != 0) {
                elements.add("${highlight("%,d".format(players))} <lang:capture.players>")
            }

            if (containers != 0) {
                elements.add("${highlight("%,d".format(containers))} <lang:capture.containers>")
            }

            return if (elements.isEmpty()) {
                "<lang:capture.nothing_saved_yet>"
            } else {
                val dimensionsAppendix = if (dimensions.isNotEmpty()) {
                    " <lang:capture.in_dimensions> ${highlight(dimensions.toList().joinWithAnd())}"
                } else {
                    ""
                }

                "<lang:capture.saved> ${elements.joinWithAnd()}$dimensionsAppendix"
            }
        }

    private fun List<String>.joinWithAnd() =
        when (size) {
            0 -> ""
            1 -> this[0]
            else -> {
                dropLast(1).joinToString() + " <lang:capture.and> " + last()
            }
        }
}