package com.yoheiyayoi.mixin;

import com.yoheiyayoi.ImposterCoal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartFurnace.class)
public class FurnaceMinecart {
    //-- Local
    private int currentFuel = 0;
    private int MAX_FUEL_TO_PUSH = 10;

    private int COAL_COOLDOWN = 0; // 1.5s
    private int CHARCOAL_COOLDOWN = 20 * 3; // 3s
    private int AFTER_FIRE_COOLDOWN = 20 * 20; // 20s
    private int fuelCooldown = 0;

    //-- Functions

    // react useState but minecraft edition
    @Unique
    private static final EntityDataAccessor<Boolean> IS_OVERHEAT = SynchedEntityData.defineId(
            MinecartFurnace.class,
            EntityDataSerializers.BOOLEAN
    );

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void addSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(IS_OVERHEAT, false);
    }

    @Unique
    private boolean getIsOverheat() {
        return ((MinecartFurnace)(Object) this).getEntityData().get(IS_OVERHEAT);
    }

    @Unique
    private void setIsOverheat(boolean value) {
        ((MinecartFurnace)(Object) this).getEntityData().set(IS_OVERHEAT, value);
    }

    // custom fuel method
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onInteract(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        MinecartFurnace cart = (MinecartFurnace)(Object) this;
        ItemStack stack = player.getItemInHand(interactionHand);
        Item item = stack.getItem();

        // dub fire
        if (getIsOverheat() && item == Items.WATER_BUCKET) {
            setIsOverheat(false);
            cart.clearFire();
            fuelCooldown = AFTER_FIRE_COOLDOWN;

            if (!player.isCreative()) {
                player.setItemInHand(interactionHand, new ItemStack(Items.BUCKET));
            }

            // play sound on server
            if (!cart.level().isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) cart.level();
                serverLevel.playSound(
                        null,
                        cart.blockPosition(),
                        SoundEvents.BUCKET_EMPTY,
                        SoundSource.BLOCKS,
                        1.0f,
                        1.0f
                );
            }

            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        // cannot add fuel at the moment
        if (fuelCooldown > 0 || getIsOverheat()) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        // add fuel
        if (item == Items.COAL || item == Items.CHARCOAL) {
            currentFuel += 1;

            // set cooldown
            if (currentFuel != MAX_FUEL_TO_PUSH) {
                fuelCooldown = item == Items.COAL ? COAL_COOLDOWN : CHARCOAL_COOLDOWN;
            }

            pushCartIfCan(player);

            if (!player.isCreative())
                stack.consume(1, player);
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    // Cooldown handler
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickCooldown(CallbackInfo ci) {
        MinecartFurnace cart = (MinecartFurnace)(Object) this;

        // render fire effect (client)
        if (getIsOverheat()) {
            cart.igniteForTicks(1);
        }

        if (cart.level().isClientSide()) return;

        // fire effect (server)
        if (getIsOverheat()) {
            ServerLevel serverLevel = (ServerLevel) cart.level();

            // play sound every 2 sec
            if (cart.tickCount % 40 == 0) {
                serverLevel.playSound(
                        null,
                        cart.blockPosition(),
                        SoundEvents.FIRE_AMBIENT,
                        SoundSource.BLOCKS,
                        2.0f,
                        1.0f
                );
            }
        }

        if (fuelCooldown > 0) {
            fuelCooldown--;

            double secondsLeft = fuelCooldown / 20.0;
            Component title = Component.literal("[").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%.1fs", secondsLeft)).withStyle(ChatFormatting.BOLD, ChatFormatting.RED))
                    .append(Component.literal("]").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY));

            cart.setCustomName(title);
            cart.setCustomNameVisible(true);
        } else {
            updateTitle();
        }
    }

    @Unique
    private void updateTitle() {
        MinecartFurnace cart = (MinecartFurnace)(Object) this;
        int red = MAX_FUEL_TO_PUSH - currentFuel;

        Component content;

        if (getIsOverheat()) {
            content = Component.literal("เครื่องยนต์ร้อนเกินไป!")
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
        } else {
            content = Component.literal("❙".repeat(currentFuel))
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN)
                    .append(Component.literal("❙".repeat(red))
                            .withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
        }

        Component title = Component.literal("[")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY)
                .append(content)
                .append(Component.literal("]").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY));

        cart.setCustomName(title);
        cart.setCustomNameVisible(true);
    }

    @Unique
    private void pushCartIfCan(@Nullable Player player) {
        MinecartFurnace cart = (MinecartFurnace)(Object) this;

        if (player != null && currentFuel == MAX_FUEL_TO_PUSH) {
            // reset fuel
            currentFuel = 0;

            // random overheat
            if (!cart.level().isClientSide()) {
                int random = (int) (Math.random() * 4) + 1; // 1 in 4
                if (random == 2) {
                    ImposterCoal.LOGGER.info("FIRE");
                    setIsOverheat(true);
                };
            }

            Vec3 playerPos = player.position();

            // cart not move if overheat
            if (!getIsOverheat()) {
                cart.push = cart.position().subtract(playerPos).horizontal().normalize().scale(0.035);
            }

            if (!cart.level().isClientSide()) {
                // effect
                ServerLevel serverLevel = (ServerLevel) cart.level();
                Vec3 pos = cart.position();
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        pos.x, pos.y + 0.5, pos.z,
                        6, 0.4, 0.4, 0.4, 0.02
                );

                serverLevel.playSound(
                        null,
                        cart.blockPosition(),
                        SoundEvents.ENDER_EYE_DEATH,
                        SoundSource.BLOCKS,
                        1.0f,
                        1.0f
                );
            }
        }
    }
}
