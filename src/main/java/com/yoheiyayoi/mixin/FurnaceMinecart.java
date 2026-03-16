package com.yoheiyayoi.mixin;

import com.yoheiyayoi.ImposterCoal;
import com.yoheiyayoi.Utils;
import com.yoheiyayoi.manager.BrokenRailManager;
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
    @Unique private static final int COAL_COOLDOWN = Utils.convertSecondToTick(2);
    @Unique private static final int CHARCOAL_COOLDOWN = Utils.convertSecondToTick(4);
    @Unique private static final int AFTER_FIRE_COOLDOWN = Utils.convertSecondToTick(15);

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


    // make it cannot be push by water
    // don't know is this work? cuz this fn exist in Entity class
    // btw i test it and the cart doesn't move so i think it's ok
    @Unique
    public boolean isPushedByFluid() {
        return false;
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
            // and only cooldown if player is in survival cuz i want to test and don't want to wait for skibidi times
            if (currentFuel < MAX_FUEL_TO_PUSH && !player.isCreative()) {
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
        } else {
            // clear fire is already exting skibiding
            if (cart.isOnFire()) cart.clearFire();
        }

        if (cart.level().isClientSide()) return;

        BrokenRailManager brm = BrokenRailManager.getInstance();
        BlockPos currentPos = cart.blockPosition();
        Direction dir = cart.getMotionDirection();
        BlockPos aheadPos = currentPos.relative(dir);

        if (brm.isBroken(currentPos) || brm.isBroken(aheadPos) || brm.isBroken(aheadPos.below())) {
            cart.setDeltaMovement(Vec3.ZERO);
            cart.push = Vec3.ZERO;
        }

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

    // btw this function bug and i use ai to fix and its work omg claude is the best
    @Unique
    private boolean hasRailAhead(Vec3 pushDir) {
        MinecartFurnace cart = (MinecartFurnace)(Object) this;

        // Pick the dominant cardinal direction from the push vector
        Direction dir;
        if (Math.abs(pushDir.x) >= Math.abs(pushDir.z)) {
            dir = pushDir.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            dir = pushDir.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        BlockPos aheadPos = cart.blockPosition().relative(dir);
        Level level = cart.level();
        return isRail(level, aheadPos) && !isBrokenRailAhead(aheadPos);
    }

    @Unique
    private boolean isBrokenRailAhead(BlockPos aheadPos) {
        MinecartFurnace cart = (MinecartFurnace)(Object) this;
        if (cart.level().isClientSide()) return false;

        BrokenRailManager brm = BrokenRailManager.getInstance();
        return brm.isBroken(aheadPos);
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
                // btw i reduce to 20% because 25% is too much for me ngl
                if (Math.random() < 0.20) {
                    ImposterCoal.LOGGER.info("FIRE");
                    setIsOverheat(true);
                }
            }

            Vec3 playerPos = player.position();
            Vec3 pushDir = cart.position().subtract(playerPos).horizontal().normalize();

            // move cart if not overheat and have rail ahead skibidi toilet
            ImposterCoal.LOGGER.info(hasRailAhead(pushDir) ? "true" : "false");
            if (!getIsOverheat() && hasRailAhead(pushDir)) {
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

    //-- Utils
    @Unique
    private void playServerSound(MinecartFurnace cart, SoundEvent sound, float volume, float pitch) {
        if (!cart.level().isClientSide()) {
            cart.level().playSound(
                    null,
                    cart.blockPosition(),
                    sound,
                    SoundSource.BLOCKS,
                    volume,
                    pitch
            );
        }
    }
}