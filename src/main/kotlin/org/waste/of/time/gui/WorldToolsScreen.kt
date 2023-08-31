package org.waste.of.time.gui

import io.github.cottonmc.cotton.gui.client.CottonClientScreen
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.data.Insets
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object WorldToolsScreen : CottonClientScreen(Gui()) {

    class Gui : LightweightGuiDescription() {
        init {
            val root = WGridPanel()
            setRootPanel(root)
            root.setSize(256,250)
            root.setInsets(Insets.ROOT_PANEL)

            val icon = WSprite(Identifier("minecraft:textures/item/redstone.png"))
            root.add(icon, 0, 0, 1, 1)

            val button = WButton(Text.translatable("gui.worldtools.download"))
            root.add(button, 1, 0, 4, 1)

            val textfield = WTextField()
            textfield.text = "Name"
            root.add(textfield, 5, 0, 4, 1)

            val label = WLabel(Text.of("Test"), 0xFFFFFF)
            root.add(label, 9, 0, 2, 1)

            val testPanel = WGridPanel()
            testPanel.setSize(240, 1200)
            val scrollPanel = WScrollPanel(testPanel)

            root.add(scrollPanel, 3,0, 10, 10)

            root.validate(this)
        }

    }
}