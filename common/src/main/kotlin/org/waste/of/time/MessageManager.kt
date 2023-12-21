package org.waste.of.time

import net.minecraft.client.toast.SystemToast
import net.minecraft.client.toast.Toast
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.WorldTools.mm

object MessageManager {
    const val BRAND = "<color:green>W<color:white>orld<color:green>T<color:white>ools<reset>"
    private val converted by lazy {
        "[${BRAND}] ".mm()
    }
    private val fullBrand: MutableText
        get() = converted.copy()

    private const val ERROR_COLOR = 0xff3333

    fun String.info() =
        Text.of(this).sendInfo()

    fun sendInfo(translateKey: String, vararg args: Any) = Text.translatable(translateKey, *args).sendInfo()

    fun sendError(translateKey: String, vararg args: Any) = Text.translatable(translateKey, *args).sendError()

    fun Text.infoToast() {
        SystemToast.create(
            mc,
            SystemToast.Type.WORLD_BACKUP,
            BRAND.mm(),
            this
        ).addToast()
    }

    private fun Text.errorToast() {
        SystemToast.create(
            mc,
            SystemToast.Type.WORLD_ACCESS_FAILURE,
            BRAND.mm(),
            this
        ).addToast()
    }

    fun Text.sendInfo() =
        fullBrand.append(this).addMessage()

    private fun Text.sendError() {
        LOG.error(this.string)
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
}
