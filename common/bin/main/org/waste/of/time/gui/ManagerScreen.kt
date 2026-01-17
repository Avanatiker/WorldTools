package org.waste.of.time.gui

import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.*
import net.minecraft.text.Text
import org.waste.of.time.WorldTools.MAX_LEVEL_NAME_LENGTH
import org.waste.of.time.config.WorldToolsConfig
import org.waste.of.time.manager.CaptureManager
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.CaptureManager.levelName

object ManagerScreen : Screen(Text.translatable("worldtools.gui.manager.title")) {
    private lateinit var worldNameTextEntryWidget: TextFieldWidget
    private lateinit var titleWidget: TextWidget
    private lateinit var downloadButton: ButtonWidget
    private lateinit var configButton: ButtonWidget
    private lateinit var cancelButton: ButtonWidget
    private const val BUTTON_WIDTH = 90

    override fun init() {
        setupTitle()
        setupEntryGrid()
        setupBottomGrid()
    }

    override fun tick() {
        if (CaptureManager.capturing) {
            downloadButton.message = Text.translatable("worldtools.gui.manager.button.stop_download")
            worldNameTextEntryWidget.setPlaceholder(Text.of(currentLevelName))
            worldNameTextEntryWidget.setEditable(false)
        } else {
            downloadButton.message = Text.translatable("worldtools.gui.manager.button.start_download")
            worldNameTextEntryWidget.setEditable(true)
        }
        super.tick()
    }

    private fun setupTitle() {
        titleWidget = TextWidget(Text.translatable("worldtools.gui.manager.title"), textRenderer)
        SimplePositioningWidget.setPos(titleWidget, 0, 0, width, height, 0.5f, 0.01f)
        addDrawableChild(titleWidget)
    }

    private fun setupEntryGrid() {
        val entryGridWidget = createGridWidget()
        val adder = entryGridWidget.createAdder(3)

        worldNameTextEntryWidget = EnterTextField(
            textRenderer, 0, 0, 250, 20, Text.of(levelName), client
        ).apply {
            setPlaceholder(Text.translatable("worldtools.gui.manager.world_name_placeholder", levelName))
            setMaxLength(MAX_LEVEL_NAME_LENGTH)
        }
        downloadButton = createButton("worldtools.gui.manager.button.start_download") {
            if (CaptureManager.capturing) {
                client?.setScreen(null)
                CaptureManager.stop()
            } else {
                client?.setScreen(null)
                CaptureManager.start(worldNameTextEntryWidget.text)
            }
        }

        adder.add(worldNameTextEntryWidget, 2)
        adder.add(downloadButton, 1)

        entryGridWidget.refreshPositions()
        SimplePositioningWidget.setPos(entryGridWidget, 0, titleWidget.y, width, height, 0.5f, 0.05f)
        entryGridWidget.forEachChild(this::addDrawableChild)
    }

    private fun setupBottomGrid() {
        val bottomGridWidget = createGridWidget()
        val bottomAdder = bottomGridWidget.createAdder(2)
        configButton = createButton("worldtools.gui.manager.button.config") {
            client?.setScreen(AutoConfig.getConfigScreen(WorldToolsConfig::class.java, this).get())
        }
        cancelButton = createButton("worldtools.gui.manager.button.cancel") {
            client?.setScreen(null)
        }

        bottomAdder.add(configButton, 1)
        bottomAdder.add(cancelButton, 1)

        bottomGridWidget.refreshPositions()
        SimplePositioningWidget.setPos(bottomGridWidget, 0, 0, width, height, 0.5f, .95f)
        bottomGridWidget.forEachChild(this::addDrawableChild)
    }

    private fun createGridWidget() = GridWidget().apply {
        mainPositioner.margin(4, 4, 4, 4)
    }

    private fun createButton(textKey: String, onClick: (ButtonWidget) -> Unit) =
        ButtonWidget.Builder(Text.translatable(textKey), onClick).width(BUTTON_WIDTH).build()
}
