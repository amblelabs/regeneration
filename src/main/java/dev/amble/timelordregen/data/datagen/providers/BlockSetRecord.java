package dev.amble.timelordregen.data.datagen.providers;

import net.minecraft.block.Block;

/**
 * A record to hold a set of related blocks.
 * @param block
 * @param stairs
 * @param slab
 * @param fence
 * @param fenceGate
 * @param trapdoor
 * @param door
 * @param pressurePlate
 * @param button
 * @author Loqor
 */
public record BlockSetRecord(Block block, Block stairs, Block slab, Block fence, Block fenceGate, Block trapdoor, Block door, Block pressurePlate, Block button) { }
