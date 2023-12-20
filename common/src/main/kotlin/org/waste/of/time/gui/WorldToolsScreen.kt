package org.waste.of.time.gui

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.*
import net.minecraft.text.Text
import org.waste.of.time.CaptureManager
import org.waste.of.time.CaptureManager.levelName
import org.waste.of.time.MessageManager.BRAND
import org.waste.of.time.WorldTools.mm

object WorldToolsScreen : Screen(BRAND.mm()) {
    override fun init() {
        val title = TextWidget(Text.translatable("gui.title", Text.translatable("worldtools")), textRenderer)
        SimplePositioningWidget.setPos(title, 0, 0, this.width, this.height, 0.5f, 0.01f)
        addDrawableChild(title)

        val entryGridWidget = GridWidget()
        entryGridWidget.mainPositioner.margin(4, 4, 4, 0)
        val adder = entryGridWidget.createAdder(3)
        val worldNameTextEntryWidget = TextFieldWidget(textRenderer, 0, 0, 250, 20, Text.of(levelName))
        worldNameTextEntryWidget.setPlaceholder(Text.translatable("gui.world_name_placeholder", levelName))
        worldNameTextEntryWidget.setMaxLength(64)
        adder.add(worldNameTextEntryWidget, 2)
        adder.add(ButtonWidget.Builder(Text.translatable("gui.start_download")) { _ ->
            CaptureManager.start(worldNameTextEntryWidget.text)
            client?.setScreen(null)
        }.width(90).build(), 1)
        entryGridWidget.refreshPositions()
        SimplePositioningWidget.setPos(entryGridWidget, 0, title.y, this.width, this.height, 0.5f, 0.05f)
        entryGridWidget.forEachChild(this::addDrawableChild)

        val cancelButton = ButtonWidget.Builder(Text.translatable("gui.cancel")) { _ ->
            client?.setScreen(null)
        }.build()
        SimplePositioningWidget.setPos(cancelButton, 0, 0, this.width, this.height, 0.5f, 0.95f)
        addDrawableChild(cancelButton)
    }
}

