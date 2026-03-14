package com.yoheiyayoi.mixin;

import com.yoheiyayoi.ImposterCoal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartFurnace.class)
public class FurnaceMinecart {
    //-- Local
    @Unique private int currentFuel = 0;
    @Unique private int fuelCooldown = 0;

    @Unique private static final int MAX_FUEL_TO_PUSH = 10;
    @Unique private static final int COAL_COOLDOWN = 30; // 1.5s
    @Unique private static final int CHARCOAL_COOLDOWN = 20 * 3; // 3s
    @Unique private static final int AFTER_FIRE_COOLDOWN = 20 * 20; // 20s

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


    @Unique
    private void playServerSound(MinecartFurnace cart, SoundEvent sound, float volume, float pitch) {
        if (!cart.level().isClientSide()) {
            ((ServerLevel) cart.level()).playSound(
                    null,
                    cart.blockPosition(),
                    sound,
                    SoundSource.BLOCKS,
                    volume,
                    pitch
            );
        }
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
            playServerSound(cart, SoundEvents.BUCKET_EMPTY, 1.0f, 1.0f);

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

            int itemCooldown = item == Items.COAL ? COAL_COOLDOWN : CHARCOAL_COOLDOWN;

            // set cooldown
            if (currentFuel < MAX_FUEL_TO_PUSH) {
                fuelCooldown = itemCooldown;
            }

            // effect
            if (!cart.level().isClientSide() && currentFuel < MAX_FUEL_TO_PUSH) {
                Vec3 pos = cart.position();
                ((ServerLevel) cart.level()).sendParticles(
                        ParticleTypes.SMOKE,
                        pos.x, pos.y + 0.5, pos.z,
                        10, 0.5, 0.5, 0.5, 0.05
                );
                playServerSound(cart, SoundEvents.WITHER_SHOOT, 0.1f, 1.0f);
            }

            pushCartIfCan(player);

            if (!player.isCreative())
                stack.consume(1, player);

            cir.setReturnValue(InteractionResult.SUCCESS);
        }
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
            // play sound every 2 sec
            if (cart.tickCount % 40 == 0) {
                playServerSound(cart, SoundEvents.FIRE_AMBIENT, 2.0f, 1.0f);
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
    private boolean hasRailAhead() {
        MinecartFurnace cart = (MinecartFurnace)(Object) this;

        Direction dir = cart.getMotionDirection();
        BlockPos aheadPos = cart.blockPosition().relative(dir);

        Level level = cart.level();
        return isRail(level, aheadPos) || isRail(level, aheadPos.below());
    }

    @Unique
    private boolean isRail(Level level, BlockPos pos) {
        ImposterCoal.LOGGER.info(String.format("checking rail at %d %d %d", pos.getX(), pos.getY(), pos.getZ()));
        return level.getBlockState(pos).getBlock() instanceof BaseRailBlock;
    }

    @Unique
    private void pushCartIfCan(@Nullable Player player) {
        MinecartFurnace cart = (MinecartFurnace)(Object) this;

        if (player != null && currentFuel == MAX_FUEL_TO_PUSH) {
            // reset fuel
            currentFuel = 0;

            // random overheat
            if (!cart.level().isClientSide()) {
                // 1 in 4 chance logic
                // in old code who use 1-4 to random? oh it's me bruh
                if (Math.random() < 0.25) {
                    ImposterCoal.LOGGER.info("FIRE");
                    setIsOverheat(true);
                }
            }

            Vec3 playerPos = player.position();
            Vec3 pushDir = cart.position().subtract(playerPos).horizontal().normalize();

            // move cart if not overheat and have rail ahead skibidi toilet
            ImposterCoal.LOGGER.info(hasRailAhead() ? "true" : "false");
            if (!getIsOverheat() && hasRailAhead()) {
                cart.push = pushDir.scale(0.035);
            }

            if (!cart.level().isClientSide()) {
                // effect
                ServerLevel serverLevel = (ServerLevel) cart.level();
                Vec3 pos = cart.position();
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        pos.x, pos.y + 0.5, pos.z,
                        50, 0.5, 0.5, 0.5, 0.05
                );
                playServerSound(cart, SoundEvents.GHAST_SHOOT, 1.0f, 1.0f);
            }
        }
    }
}