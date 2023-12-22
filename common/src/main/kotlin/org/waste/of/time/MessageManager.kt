package org.waste.of.time

import net.minecraft.client.toast.SystemToast
import net.minecraft.client.toast.Toast
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc

object MessageManager {
    private const val ERROR_COLOR = 0xff3333

    val brand: Text = Text.of("W").copy().styled {
        it.withColor(TextColor.fromRgb(config.advanced.accentColor))
    }.append(
        Text.of("orld").copy().styled {
            it.withColor(Formatting.RESET)
        }
    ).append(
        Text.of("T").copy().styled {
            it.withColor(TextColor.fromRgb(config.advanced.accentColor))
        }
    ).append(
        Text.of("ools").copy().styled {
            it.withColor(Formatting.RESET)
        }
    )
    private val converted by lazy {
        Text.of("[").copy().append(brand).append(Text.of("] "))
    }
    private val fullBrand: MutableText
        get() = converted.copy()

    fun String.info() =
        Text.of(this).sendInfo()

    fun sendInfo(translateKey: String, vararg args: Any) = translateHighlight(translateKey, *args).sendInfo()

    fun sendError(translateKey: String, vararg args: Any) = Text.translatable(translateKey, *args).sendError()

    fun Text.infoToast() {
        SystemToast.create(
            mc,
            SystemToast.Type.WORLD_BACKUP,
            brand,
            this
        ).addToast()
    }

    private fun Text.errorToast() {
        SystemToast.create(
            mc,
            SystemToast.Type.WORLD_ACCESS_FAILURE,
            brand,
            this
        ).addToast()
    }

    fun Text.sendInfo() =
        fullBrand.append(this).addMessage()

    private fun Text.sendError() {
        LOG.error(string)
        val errorText = copy().styled {
            it.withColor(ERROR_COLOR)
        }

        fullBrand.append(errorText).addMessage()
        errorText.errorToast()
    }

    private fun Text.addMessage() {
        if (!config.advanced.showChatMessages) return

        mc.execute {
            mc.inGameHud.chatHud.addMessage(this)
        }
    }

    private fun Toast.addToast() {
        if (!config.advanced.showToasts) return

        mc.execute {
            mc.toastManager.add(this)
        }
    }

    fun translateHighlight(key: String, vararg args: Any): MutableText {
        args.map { element ->
            val secondaryColor = TextColor.fromRgb(config.advanced.accentColor)
            if (element is Text) {
                if (element.style.color != null) {
                    element
                } else {
                    element.copy().styled { style ->
                        style.withColor(secondaryColor)
                    }
                }
            } else {
                Text.of(element.toString()).copy().styled { style ->
                    style.withColor(secondaryColor)
                }
            }
        }.toTypedArray().let {
            return Text.translatable(key, *it)
        }
    }

    fun Vec3d.asString() = "(%.2f, %.2f, %.2f)".format(x, y, z)
}
