package org.waste.of.time.gui

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.text.Text
import org.waste.of.time.CaptureManager
import org.waste.of.time.CaptureManager.levelName
import org.waste.of.time.MessageManager.BRAND
import org.waste.of.time.WorldTools.mm

object WorldToolsScreen : Screen(BRAND.mm()) {
    override fun init() {
        val title = title()

//        val browseExistingDownloadsButton = ButtonWidget.Builder(Text.of("Browse Existing Downloads"), { buttonWidget ->
//            this.client?.setScreen(BrowseDownloadsScreen())
//        }).build()
//        browseExistingDownloadsButton.x = 20
//        browseExistingDownloadsButton.y = 30
//        addDrawableChild(browseExistingDownloadsButton)

        val worldNameWidget = textFieldWidget(title)
        downloadButton(worldNameWidget)
        cancelButton()
    }

    private fun title(): TextWidget {
        val title = TextWidget(Text.translatable("gui.title", Text.translatable("worldtools")), textRenderer)
        title.x = width / 2 - title.width / 2
        title.y = 10
        addDrawableChild(title)
        return title
    }

    private fun cancelButton() {
        val cancelButton = ButtonWidget.Builder(Text.translatable("gui.cancel")) { _ ->
            client?.setScreen(null)
        }.build()
        cancelButton.x = width / 2 - cancelButton.width / 2
        cancelButton.y = height - cancelButton.height - 40
        addDrawableChild(cancelButton)
    }

    private fun downloadButton(worldNameWidget: TextFieldWidget) {
        val startDownloadButton = ButtonWidget.Builder(Text.translatable("gui.start_download")) { _ ->
            CaptureManager.start(worldNameWidget.text)
            client?.setScreen(null)
        }.build()
        startDownloadButton.x = worldNameWidget.x + worldNameWidget.width + 10
        startDownloadButton.y = worldNameWidget.y
        startDownloadButton.width = 90
        addDrawableChild(startDownloadButton)
    }

    private fun textFieldWidget(title: TextWidget): TextFieldWidget {
        val worldNameWidget = TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.of(levelName))
        worldNameWidget.setPlaceholder(Text.translatable("gui.world_name_placeholder", levelName))
        worldNameWidget.setMaxLength(64)
        worldNameWidget.width = width - 150
        worldNameWidget.x = 20
        //        worldNameWidget.y = browseExistingDownloadsButton.y + browseExistingDownloadsButton.height + 20
        worldNameWidget.y = title.y + title.height + 20
        addDrawableChild(worldNameWidget)
        return worldNameWidget
    }
}
