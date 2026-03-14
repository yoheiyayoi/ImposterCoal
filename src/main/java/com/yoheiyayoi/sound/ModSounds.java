package com.yoheiyayoi.sound;

import com.yoheiyayoi.ImposterCoal;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent SURVIVOR_WIN = registerSoundEvent("survivor_win");
    public static final SoundEvent IMPOSTER_WIN = registerSoundEvent("imposter_win");

    public static SoundEvent registerSoundEvent(String name) {
        // เค้าจะเปลี่ยนให้มันยาวขึ้นจาก 1.21.1 ทำพระแสงอะไรฟระ
        Identifier id = Identifier.fromNamespaceAndPath(ImposterCoal.MOD_ID, name);
        return Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                id,
                SoundEvent.createVariableRangeEvent(id)
        );
    }

    public static void registerSound() {
        ImposterCoal.LOGGER.info("Registering Mod Sounds for " + ImposterCoal.MOD_ID);
    }
}
