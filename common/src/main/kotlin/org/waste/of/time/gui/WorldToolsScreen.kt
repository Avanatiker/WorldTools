package org.waste.of.time.gui

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.text.Text
import org.waste.of.time.WorldTools

object WorldToolsScreen : Screen(Text.of("WorldTools")) {
    // todo: rewrite this without a fabric-only library :doomcat:

    override fun init() {
        val title = TextWidget(Text.of("WorldTools World Downloader"), textRenderer)
        title.x = this.width / 2 - title.width / 2
        title.y = 10
        this.addDrawableChild(title)

//        val browseExistingDownloadsButton = ButtonWidget.Builder(Text.of("Browse Existing Downloads"), { buttonWidget ->
//            this.client?.setScreen(BrowseDownloadsScreen())
//        }).build()
//        browseExistingDownloadsButton.x = 20
//        browseExistingDownloadsButton.y = 30
//        addDrawableChild(browseExistingDownloadsButton)

        val worldNameWidget = TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.of("World Name"))
        worldNameWidget.setPlaceholder(Text.of("Enter World Name"))
        worldNameWidget.setMaxLength(64)
        worldNameWidget.width = this.width - 150
        worldNameWidget.x = 20
//        worldNameWidget.y = browseExistingDownloadsButton.y + browseExistingDownloadsButton.height + 20
        worldNameWidget.y = title.y + title.height + 20
        addDrawableChild(worldNameWidget)

        val startDownloadButton = ButtonWidget.Builder(Text.of("Start Download")) { _ ->
            WorldTools.startCapture(worldNameWidget.text)
            this.client?.setScreen(null)
        }.build()
        startDownloadButton.x = worldNameWidget.x + worldNameWidget.width + 10
        startDownloadButton.y = worldNameWidget.y
        startDownloadButton.width = 90
        addDrawableChild(startDownloadButton)

        val cancelButton = ButtonWidget.Builder(Text.of("Cancel")) { _ ->
            this.client?.setScreen(null)
        }.build()
        cancelButton.x = this.width / 2 - cancelButton.width / 2
        cancelButton.y = this.height - cancelButton.height - 40
        addDrawableChild(cancelButton)
    }
}
