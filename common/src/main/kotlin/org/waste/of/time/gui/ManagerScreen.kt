package org.waste.of.time.gui

import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.*
import net.minecraft.text.Text
import org.waste.of.time.CaptureManager
import org.waste.of.time.CaptureManager.levelName
import org.waste.of.time.WorldTools.MAX_LEVEL_NAME_LENGTH
import org.waste.of.time.config.WorldToolsConfig

object ManagerScreen : Screen(Text.translatable("worldtools.gui.manager.title")) {
    override fun init() {
        val title = TextWidget(Text.translatable("worldtools.gui.manager.title"), textRenderer)
        SimplePositioningWidget.setPos(title, 0, 0, width, height, 0.5f, 0.01f)
        addDrawableChild(title)

        val entryGridWidget = GridWidget()
        entryGridWidget.mainPositioner.margin(4, 4, 4, 0)
        val adder = entryGridWidget.createAdder(3)
        val worldNameTextEntryWidget = TextFieldWidget(textRenderer, 0, 0, 250, 20, Text.of(levelName))
        worldNameTextEntryWidget.setPlaceholder(Text.translatable("worldtools.gui.manager.world_name_placeholder", levelName))
        worldNameTextEntryWidget.setMaxLength(MAX_LEVEL_NAME_LENGTH)
        adder.add(worldNameTextEntryWidget, 2)
        adder.add(ButtonWidget.Builder(Text.translatable("worldtools.gui.manager.button.start_download")) { _ ->
            CaptureManager.start(worldNameTextEntryWidget.text)
            client?.setScreen(null)
        }.width(90).build(), 1)
        entryGridWidget.refreshPositions()
        SimplePositioningWidget.setPos(entryGridWidget, 0, title.y, width, height, 0.5f, 0.05f)
        entryGridWidget.forEachChild(this::addDrawableChild)

        val bottomGridWidget = GridWidget()
        bottomGridWidget.mainPositioner.margin(4, 4, 4, 4)
        val bottomAdder = bottomGridWidget.createAdder(2)
        // no mod menu on forge so we need a way to get to the config screen ~somewhere~
        val configButton = ButtonWidget.Builder(Text.translatable("worldtools.gui.manager.button.config")) { _ ->
            client?.setScreen(AutoConfig.getConfigScreen(WorldToolsConfig::class.java, this).get())
        }.width(90).build()
        bottomAdder.add(configButton, 1)

        val cancelButton = ButtonWidget.Builder(Text.translatable("worldtools.gui.manager.button.cancel")) { _ ->
            client?.setScreen(null)
        }.width(90).build()
        bottomAdder.add(cancelButton, 1)

        bottomGridWidget.refreshPositions()
        SimplePositioningWidget.setPos(bottomGridWidget, 0, 0, width, height, 0.5f, .95f)
        bottomGridWidget.forEachChild(this::addDrawableChild)
    }
}

