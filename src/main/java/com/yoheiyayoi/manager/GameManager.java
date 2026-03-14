package com.yoheiyayoi.manager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class GameManager {
    //-- Global
    private static GameManager instance;

    //-- Local
    private boolean isGameStart = false;
    private MinecraftServer server;

    //-- Functions
    private GameManager() {}

    public static GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }

    public void init(MinecraftServer server) {
        this.server = server;
    }

    //-- Game Logic
    public void startGame() {
        isGameStart = true;

        boardCast(
                Component.literal("เกมเริ่มแล้ว!").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN), false
        );
    }

    // force end game
    public void endGame() {
        isGameStart = false;

        boardCast(
                Component.literal("เกมถูกยุติแล้ว!").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), false
        );
    }

    // survivor won
    public void cartReachTheEnd() {
        isGameStart = false;

        boardCast(
                Component.literal("เกมจบแล้ว คนดีเป็นฝ่ายชนะ!").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN), false
        );
    }

    public boolean isStart() {
        return isGameStart;
    }

    //-- Utils
    private void boardCast(Component component, boolean bl) {
        server.getPlayerList().broadcastSystemMessage(
                component, bl
        );
    }
}
