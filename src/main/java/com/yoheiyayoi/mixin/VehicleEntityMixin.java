package com.yoheiyayoi.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public class VehicleEntityMixin {
    // Custom hurt
    // because player should not attack the minecart
    // if minecart is dead then how we gonna play bro
    @Inject(method="hurtServer", at = @At("HEAD"), cancellable = true)
    public void customHurt(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof MinecartFurnace) {
            if (damageSource.getEntity() instanceof Player player && player.isCreative()) {
                return;
            }

            cir.setReturnValue(false);
        }
    }
}
