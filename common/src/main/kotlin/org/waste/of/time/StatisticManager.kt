package org.waste.of.time

object StatisticManager {
    var chunks = 0
    var entities = 0
    var players = 0
    var container = 0

    fun reset() {
        chunks = 0
        entities = 0
        players = 0
        container = 0
    }
}