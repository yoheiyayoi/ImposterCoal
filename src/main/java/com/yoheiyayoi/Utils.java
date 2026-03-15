package com.yoheiyayoi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

public class Utils {
    public static int convertSecondToTick(int second) {
        return second * 20;
    }

    public static int convertMinuteToTick(int minute) {
        return convertSecondToTick(minute * 60);
    }

    public static void sendTitleToPlayer(ServerPlayer player, Component title, @Nullable Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));

        if (subtitle != null) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    public static void sendSoundToPlayer(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SoundSource.MASTER,
                player.getX(), player.getY(), player.getZ(),
                volume, pitch, 0L
        ));
    }
}
