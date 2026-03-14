package com.yoheiyayoi.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.BaseRailBlock;

public class CancelRailBreak {
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) return true;

            boolean isRail = state.getBlock() instanceof BaseRailBlock;
            if (isRail && !player.isCreative()) {
                // play npc sound cuz i think it cool 555555
                world.playSound(
                        null,
                        pos,
                        SoundEvents.VILLAGER_NO,
                        SoundSource.BLOCKS,
                        0.3f,
                        1.0f
                );
            }

            return !isRail || player.isCreative();
        });
    }
}
