package com.yoheiyayoi.mixin;

import com.yoheiyayoi.ImposterCoal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
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
    private boolean isOverheat = false;

    private int COAL_COOLDOWN = 30; // 1.5s
    private int CHARCOAL_COOLDOWN = 60; // 3s
    private int fuelCooldown = 0;

    //-- Functions

    // custom fuel method
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void addFuel(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(interactionHand);

        if (fuelCooldown > 0) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        if (stack.getItem() == Items.COAL || stack.getItem() == Items.CHARCOAL) {
            currentFuel += 1;

            // set cooldown
            if (currentFuel != MAX_FUEL_TO_PUSH) {
                if (stack.getItem() == Items.COAL) {
                    fuelCooldown = COAL_COOLDOWN;
                    ImposterCoal.LOGGER.info(String.valueOf(fuelCooldown));
                } else {
                    fuelCooldown = CHARCOAL_COOLDOWN;
                }
            }

            pushCartIfCan(player);
            // cart.push = cart.position().subtract(playerPos).horizontal().normalize().scale(0.02);

            if (!player.isCreative())
                stack.consume(1, player);
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    // Cooldown handler
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickCooldown(CallbackInfo ci) {
        MinecartFurnace cart = (MinecartFurnace)(Object)this;

        if (cart.level().isClientSide()) return;

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
        int red = MAX_FUEL_TO_PUSH - currentFuel;
        MinecartFurnace cart = (MinecartFurnace)(Object)this;
        Component title = Component.literal("[").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY)
                .append(Component.literal("❙".repeat(currentFuel)).withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN))
                .append(Component.literal("❙".repeat(red)).withStyle(ChatFormatting.BOLD, ChatFormatting.RED))
                .append(Component.literal("]").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY));

        cart.setCustomName(title);
        cart.setCustomNameVisible(true);
    }

    @Unique
    private void pushCartIfCan(@Nullable Player player) {
        MinecartFurnace cart = (MinecartFurnace)(Object)this;

        if (player != null && currentFuel == MAX_FUEL_TO_PUSH) {
            Vec3 playerPos = player.position();
            cart.push = cart.position().subtract(playerPos).horizontal().normalize().scale(0.035);

            // reset fuel
            currentFuel = 0;

            // รถไฟไฟไหม้ omg
            // int random = (int)(Math.random() * 4) + 1;
            // if (random != 1) return;

            // cart.lavaIgnite();
        }
    }
}
