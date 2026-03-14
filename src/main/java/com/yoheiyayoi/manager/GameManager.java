package com.yoheiyayoi.manager;

import com.yoheiyayoi.Utils;
import com.yoheiyayoi.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GameManager {
    //*
    // ระบบออกแบบมาสำหรับมีฆาตกร (imposter) แค่คนเดียว
    // สำหรับใครที่อยากเอาไปเล่นและอยากให้มีฆาตกรมากกว่า 1 คน ก็ fork โค้ดไปแก้และ build เอาเองเด้อ
    //*

    //-- Global
    private static GameManager instance;
    private static final int TIME_TIL_RANDOM_IMPOSTER = Utils.convertSecondToTick(20); // 5 mins
    private static final int TIME_TIL_GAME_END = Utils.convertSecondToTick(40); // 60 mins
    private static final int COUNTDOWN_DURATION = Utils.convertSecondToTick(10);

    //-- Local
    private boolean isGameStart = false;
    private MinecraftServer server;

    // Roles
    private UUID imposterUUID = null;
    private final Set<UUID> survivorUUIDs = new HashSet<>();

    // Timers
    private int tickCounter = 0;
    private boolean imposterAssigned = false;

    //-- Functions
    private GameManager() {}

    public static GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }

    public void init(MinecraftServer newServer) {
        server = newServer;
    }

    //-- Tick
    public void tick() {
        if (!isGameStart) return;

        tickCounter++;

        handleImposterRandom();
        handleGameSequence();
    }

    private void handleImposterRandom() {
        if (imposterAssigned) return;

        int remaining = TIME_TIL_RANDOM_IMPOSTER - tickCounter;

        // 1 mins warn
        if (remaining == Utils.convertMinuteToTick(1)) {
            sendRandomImposterCountdown("1", "นาที");
        }

        // second warn
        if (isCountdownActive(remaining)) {
            int seconds = remaining / 20;
            sendRandomImposterCountdown(String.valueOf(seconds), "วินาที");
        }

        // random imposterrrrrrrrrr after 5 mins ofc
        if (tickCounter >= TIME_TIL_RANDOM_IMPOSTER) {
            assignImposter();
        }
    }

    private void handleGameSequence() {
        int remaining = TIME_TIL_GAME_END - tickCounter;

        // 20 mins warn
        if (remaining == Utils.convertMinuteToTick(20)) {
            sendTimeUpCountdown("20", "นาที");
        }

        // second warn
        if (isCountdownActive(remaining)) {
            int seconds = remaining / 20;
            sendTimeUpCountdown(String.valueOf(seconds), "วินาที");
        }

        // time up after 60 mins sigma boy
        if (tickCounter >= TIME_TIL_GAME_END) {
            imposterWin();
        }
    }

    //-- Game Logic
    public void startGame() {
        isGameStart = true;
        tickCounter = 0;
        imposterAssigned = false;
        imposterUUID = null;
        survivorUUIDs.clear();

        // register all current players as survivors for now cuz we random imposter later
        server.getPlayerList().getPlayers().forEach(player -> survivorUUIDs.add(player.getUUID()));

        boardCast(Component.literal("เกมเริ่มแล้ว!").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN), false);
        sendRandomImposterCountdown("5", "นาที");

        sendTitleToAll(
                Component.literal("Game Start!").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW),
                Component.literal("ขอให้โชคดี!").withStyle(ChatFormatting.GREEN)
        );

        playServerSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }

    private void assignImposter() {
        imposterAssigned = true;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        // pick random imposter
        ServerPlayer imposter = players.get(new Random().nextInt(players.size()));
        imposterUUID = imposter.getUUID();
        survivorUUIDs.remove(imposterUUID);

        // tell imposter
        imposter.sendSystemMessage(
                Component.literal("⚠ คุณคือฆาตกร! กำจัดผู้รอดชีวิตให้หมดหรือขัดขวางไม่ให้รถไฟไปถึงจุดหมายให้ได้!").withStyle(ChatFormatting.BOLD, ChatFormatting.RED)
        );

        sendTitle(imposter,
                Component.literal("คุณคือ ฆาตกร!").withStyle(ChatFormatting.BOLD, ChatFormatting.RED),
                Component.literal("กำจัดทุกคน!").withStyle(ChatFormatting.DARK_RED)
        );

        // tell survivors
        players.forEach(player -> {
            if (!player.getUUID().equals(imposterUUID)) {
                player.sendSystemMessage(
                        Component.literal("✔ คุณคือผู้รอดชีวิต! ระวังฆาตกรและหาถ่านไปใส่รถไฟให้ได้!").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN)
                );

                sendTitle(player,
                        Component.literal("คุณคือ ผู้รอดชีวิต!").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN),
                        Component.literal("เอาตัวรอดให้ได้!").withStyle(ChatFormatting.YELLOW)
                );
            }
        });

        playServerSound(SoundEvents.AMBIENT_CAVE.value(), 1.0f, 1.0f);
    }

    public void onPlayerDeath(ServerPlayer player) {
        if (!isGameStart || !imposterAssigned) return;

        UUID died = player.getUUID();

        // imposter died = survivors win
        if (died.equals(imposterUUID)) {
            boardCast(Component.literal("ฆาตกร " + player.getName().getString() + " ตายแล้ว!").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN), false);
            survivorWin();
            return;
        }

        // survivor died = remove from set
        survivorUUIDs.remove(died);

        // all survivors dead = imposter wins
        if (survivorUUIDs.isEmpty()) {
            imposterWin();
        }
    }

    private void resetGame() {
        isGameStart = false;
        tickCounter = 0;
        imposterAssigned = false;
        imposterUUID = null;
        survivorUUIDs.clear();
    }

    public void endGame() {
        boardCast(Component.literal("เกมถูกยุติแล้ว!").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), false);

        sendTitleToAll(
                Component.literal("Game End!").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD),
                Component.literal("เกมถูกยุติแล้ว!").withStyle(ChatFormatting.YELLOW)
        );

        resetGame();
    }

    public void survivorWin() {
        boardCast(Component.literal("เกมจบแล้ว คนดีเป็นฝ่ายชนะ!").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN), false);

        sendTitleToAll(
                Component.literal("คนดีเป็นฝ่ายชนะ!").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN),
                Component.literal("Game End!").withStyle(ChatFormatting.GOLD)
        );

        playServerSound(ModSounds.SURVIVOR_WIN, 1.0f, 1.0f);

        resetGame();
    }

    public void imposterWin() {
        boardCast(Component.literal("เกมจบแล้ว ฆาตกรเป็นฝ่ายชนะ!").withStyle(ChatFormatting.BOLD, ChatFormatting.RED), false);

        sendTitleToAll(
                Component.literal("ฆาตกรเป็นฝ่ายชนะ!").withStyle(ChatFormatting.BOLD, ChatFormatting.RED),
                Component.literal("Game End!").withStyle(ChatFormatting.GOLD)
        );

        playServerSound(ModSounds.IMPOSTER_WIN, 1.0f, 1.0f);

        resetGame();
    }

    public boolean isStart() {
        return isGameStart;
    }

    public boolean isImposter(UUID uuid) {
        return uuid.equals(imposterUUID);
    }

    public UUID getImposter() {
        return imposterUUID;

    }
    public Set<UUID> getSurvivors() {
        return survivorUUIDs;
    }

    //-- Utils
    private void boardCast(Component component, boolean actionBar) {
        server.getPlayerList().broadcastSystemMessage(component, actionBar);
    }

    private void playServerSound(SoundEvent sound, float volume, float pitch) {
        server.getPlayerList().getPlayers().forEach(player -> {
            player.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                    SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(),
                    volume * 100f, pitch, 0L
            ));
        });
    }

    private void sendTitle(ServerPlayer player, Component title, @Nullable Component subTitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));

        if (subTitle != null) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(subTitle));
        }
    }

    private void sendTitleToAll(Component title, @Nullable Component subTitle) {
        server.getPlayerList().getPlayers().forEach(player -> sendTitle(player, title, subTitle));
    }

    private void sendCountdownToAll(Component title) {
        boardCast(title, false);
        playServerSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }

    private void sendRandomImposterCountdown(String content, String tileUnit) {
        sendCountdownToAll(Component.literal("⚠ จะสุ่มบทบาทในอีก ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(content).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD))
                .append(Component.literal(" " + tileUnit).withStyle(ChatFormatting.YELLOW))
        );
    }

    private void sendTimeUpCountdown(String content, String tileUnit) {
        sendCountdownToAll(Component.literal("⏰ เกมจะจบในอีก ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(content).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD))
                .append(Component.literal(" " + tileUnit).withStyle(ChatFormatting.YELLOW))
        );
    }

    // Calculate if countdown is 1-10 or something but yes this works
    private boolean isCountdownActive(int remainingTicks) {
        return remainingTicks <= COUNTDOWN_DURATION && remainingTicks > 0 && remainingTicks % 20 == 0;
    }
}