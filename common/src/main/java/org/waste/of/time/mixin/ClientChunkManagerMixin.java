package org.waste.of.time.mixin;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.waste.of.time.event.Events;

import java.util.function.Consumer;

@Mixin(ClientChunkManager.class)
public class ClientChunkManagerMixin {
    @Final
    @Shadow
    private ClientWorld world;

    @Inject(method = "loadChunkFromPacket", at = @At("TAIL"))
    private void onChunkLoad(final int x, final int z, final PacketByteBuf buf, final NbtCompound nbt, final Consumer<ChunkData.BlockEntityVisitor> consumer, final CallbackInfoReturnable<WorldChunk> cir) {
        Events.INSTANCE.onChunkLoad(this.world, cir.getReturnValue());
    }

    @Inject(method = "loadChunkFromPacket", at = @At(value = "NEW", target = "net/minecraft/world/chunk/WorldChunk", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onChunkUnload(final int x, final int z, final PacketByteBuf buf, final NbtCompound nbt, final Consumer<ChunkData.BlockEntityVisitor> consumer, final CallbackInfoReturnable<WorldChunk> cir, int index, WorldChunk worldChunk, ChunkPos chunkPos) {
        if (worldChunk != null) {
            Events.INSTANCE.onChunkUnload(this.world, worldChunk);
        }
    }

    @Inject(method = "unload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientChunkManager$ClientChunkMap;compareAndSet(ILnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/chunk/WorldChunk;)Lnet/minecraft/world/chunk/WorldChunk;"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void onChunkUnload(int chunkX, int chunkZ, CallbackInfo ci, int i, WorldChunk chunk) {
        Events.INSTANCE.onChunkUnload(this.world, chunk);
    }

//    @Inject(
//        method = "updateLoadDistance",
//        at = @At(
//            value = "INVOKE",
//            target = "net/minecraft/client/world/ClientChunkManager$ClientChunkMap.isInRadius(II)Z"
//        ),
//        locals = LocalCapture.CAPTURE_FAILHARD
//    )
//    private void onUpdateLoadDistance(int loadDistance, CallbackInfo ci, int oldRadius, int newRadius, ClientChunkManager.ClientChunkMap clientChunkMap, int k, WorldChunk oldChunk, ChunkPos chunkPos) {
//        if (!clientChunkMap.isInRadius(chunkPos.x, chunkPos.z)) {
//            ClientChunkEvents.Companion.getUNLOAD_DATA().invoker().unload(this.world, oldChunk);
//        }
//    }
}
