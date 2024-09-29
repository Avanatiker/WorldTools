package org.waste.of.time.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.text.Text

// todo: we need a local database of downloads like original wdl mod
object BrowseDownloadsScreen : Screen(Text.translatable("worldtools.gui.browser.title")) {
    override fun init() {

    }

    object DownloadListWidget : AlwaysSelectedEntryListWidget<WorldDownloadEntry>(
        MinecraftClient.getInstance(),
        width,
        height,
        20,
        height - 30
    ) {


    }

    class WorldDownloadEntry : AlwaysSelectedEntryListWidget.Entry<WorldDownloadEntry>() {
        override fun render(
            context: DrawContext?,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            TODO("Not yet implemented")
        }

        override fun getNarration(): Text {
            TODO("Not yet implemented")
        }

    }
}
