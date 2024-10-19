package org.waste.of.time.manager

import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.WorldTools.config
import org.waste.of.time.storage.cache.HotCache

object StatisticManager {
    var chunks = 0
    var entities = 0
    var players = 0
    private val containers get(): Int =
        HotCache.scannedBlockEntities.size + HotCache.loadedBlockEntities.size
    val dimensions = mutableSetOf<String>()

    fun reset() {
        chunks = 0
        entities = 0
        players = 0
        dimensions.clear()
    }

    val infoMessage: Text
        get() {
            val savedElements = mutableListOf<Text>().apply {
                if (chunks == 1) {
                    add(translateHighlight("worldtools.capture.chunk", chunks))
                }
                if (chunks > 1) {
                    add(translateHighlight("worldtools.capture.chunks", "%,d".format(chunks)))
                }

                if (entities == 1) {
                    add(translateHighlight("worldtools.capture.entity", entities))
                }
                if (entities > 1) {
                    add(translateHighlight("worldtools.capture.entities", "%,d".format(entities)))
                }

                if (players == 1) {
                    add(translateHighlight("worldtools.capture.player", players))
                }
                if (players > 1) {
                    add(translateHighlight("worldtools.capture.players", "%,d".format(players)))
                }

                if (containers == 1) {
                    add(translateHighlight("worldtools.capture.container", containers))
                }
                if (containers > 1) {
                    add(translateHighlight("worldtools.capture.containers", "%,d".format(containers)))
                }
            }

            return if (savedElements.isEmpty()) {
                translateHighlight("worldtools.capture.nothing_saved_yet", currentLevelName)
            } else {
                val dimensionsFormatted = dimensions.map {
                    Text.literal(it).styled { text ->
                        text.withColor(TextColor.fromRgb(config.render.accentColor))
                    }
                }.joinWithAnd()
                Text.translatable("worldtools.capture.saved").copy()
                    .append(savedElements.joinWithAnd())
                    .append(Text.translatable("worldtools.capture.in_dimension"))
                    .append(dimensionsFormatted)
            }
        }

    fun List<Text>.joinWithAnd(): Text {
        val and = Text.translatable("worldtools.capture.and")
        return when (size) {
            0 -> Text.of("")
            1 -> this[0]
            2 -> this[0].copy().append(and).append(this[1])
            else -> dropLast(1).join().append(and).append(last())
        }
    }

    private fun List<Text>.join(): MutableText {
        val comma = Text.of(", ")
        return foldIndexed(Text.literal("")) { index, acc, text ->
            if (index == 0) return@foldIndexed text.copy()
            acc.append(comma).append(text)
        }
    }
}