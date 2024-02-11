package org.waste.of.time.mixin;

import net.minecraft.block.entity.LockableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.waste.of.time.extension.IBlockEntityContainerExtension;

@Mixin(LockableContainerBlockEntity.class)
public abstract class ContainerBlockEntityExtension implements IBlockEntityContainerExtension {
    private boolean wtContentsRead = false;

    @Override
    public void setWTContentsRead(final boolean read) {
        this.wtContentsRead = read;
    }

    @Override
    public boolean getWTContentsRead() {
        return this.wtContentsRead;
    }
}
