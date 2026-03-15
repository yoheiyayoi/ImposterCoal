package com.yoheiyayoi.event;

import com.yoheiyayoi.Utils;
import com.yoheiyayoi.manager.BrokenRailManager;
import com.yoheiyayoi.manager.GameManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ImposterBreakRail {

    private static final int COOLDOWN_TICKS = Utils.convertMinuteToTick(5);
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            var block = world.getBlockState(pos).getBlock();
            var item = serverPlayer.getItemInHand(hand).getItem();
            var level = (ServerLevel) world;
            var gameManager = GameManager.getInstance();
            var brm = BrokenRailManager.getInstance();

            boolean isRail = block instanceof BaseRailBlock;
            boolean isImposter = gameManager.isImposter(serverPlayer.getUUID());
            boolean isHammer = item == Items.NETHERITE_HOE;
            boolean isIron = item == Items.IRON_INGOT;

            // imposter break rail
            if (isRail && isImposter && isHammer && gameManager.isStart()) {
                if (!brm.isBroken(pos)) {
                    UUID uuid = serverPlayer.getUUID();
                    long currentTick = serverPlayer.level().getGameTime();

                    // check cooldown
                    if (cooldowns.containsKey(uuid)) {
                        long ticksLeft = COOLDOWN_TICKS - (currentTick - cooldowns.get(uuid));

                        if (ticksLeft > 0) {
                            int secsLeft = (int)(ticksLeft / 20);
                            serverPlayer.sendSystemMessage(
                                    Component.literal("⏳ ยังใช้ไม่ได้! รออีก " + secsLeft + " วินาที")
                                            .withStyle(ChatFormatting.RED)
                            );

                            Utils.sendSoundToPlayer(serverPlayer, SoundEvents.ENDER_EYE_DEATH, 1.0f, 1.0f);
                            return InteractionResult.FAIL;
                        }
                    }

                    // break the rail + set cooldown
                    brm.breakRail(pos, level);
                    cooldowns.put(uuid, currentTick);

                    serverPlayer.sendSystemMessage(
                            Component.literal("💥 ทำลายรางรถไฟ ตู้มมม").withStyle(ChatFormatting.RED)
                    );

                    Utils.sendSoundToPlayer(serverPlayer, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
                return InteractionResult.SUCCESS;
            }

            // repair the rail with iron
            if (isRail && isIron && brm.isBroken(pos) && gameManager.isStart()) {
                serverPlayer.getItemInHand(hand).shrink(1);
                boolean fullyRepaired = brm.repair(pos, level);
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);

                if (fullyRepaired) {
                    serverPlayer.sendSystemMessage(
                            Component.literal("✔ ซ่อมรางรถไฟสำเร็จ!").withStyle(ChatFormatting.GREEN)
                    );
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }

    // call this when game ends/resets so cooldowns don't carry over
    public static void reset() {
        cooldowns.clear();
    }
}