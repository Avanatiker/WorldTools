package org.waste.of.time.mixin;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.waste.of.time.event.Events;

import java.util.function.Consumer;

@Mixin(ClientChunkManager.class)
public class ClientChunkManagerMixin {
    @Inject(method = "loadChunkFromPacket", at = @At("TAIL"))
    private void onChunkLoad(final int x, final int z, final PacketByteBuf buf, final NbtCompound nbt, final Consumer<ChunkData.BlockEntityVisitor> consumer, final CallbackInfoReturnable<WorldChunk> cir) {
        Events.INSTANCE.onChunkLoad(cir.getReturnValue());
    }

    @Inject(
        method = "unload",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientChunkManager$ClientChunkMap;compareAndSet(ILnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/chunk/WorldChunk;)Lnet/minecraft/world/chunk/WorldChunk;"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onChunkUnload(int chunkX, int chunkZ, CallbackInfo ci, int i, WorldChunk worldChunk) {
        Events.INSTANCE.onChunkUnload(worldChunk);
    }
}
