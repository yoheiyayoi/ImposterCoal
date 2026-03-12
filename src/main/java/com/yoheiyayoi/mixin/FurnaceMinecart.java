package com.yoheiyayoi.mixin;

import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(MinecartFurnace.class)
public class FurnaceMinecart {
    //-- Local
    private int currentFuel = 0;
    private int MAX_FUEL_TO_PUSH = 10;

    //-- Functions
    // TODO: เดี๋ยวมาทำ
}
