package com.yoheiyayoi.mixin;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public class AbstractMinecartMixin {
    // make minecart furnace not pushable
    // cuz why not? skibidi
    @Inject(method="isPushable", at = @At("HEAD"), cancellable = true)
    public void disablePush(CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof MinecartFurnace) {
            cir.setReturnValue(false);
        }
    }
}
