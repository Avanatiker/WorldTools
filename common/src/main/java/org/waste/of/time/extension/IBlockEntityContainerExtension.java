package org.waste.of.time.extension;

/**
 * If we have container contents already saved, and the block entity is being saved with no contents, we take the old container contents as truth
 * But if a container is not opened by the player, it has the same NBT contents as an empty container.
 * We need to differentiate those two states
 * Scenario:
 * - We save a container's contents
 * - Player moves and unloads the block entity
 * - Player comes back, opens the container, and removes all items
 * - We should save the container as empty, not with the old contents
 */
public interface IBlockEntityContainerExtension {
    void setWTContentsRead(boolean read);
    boolean getWTContentsRead();
}
