package com.yoheiyayoi.command;

import com.mojang.brigadier.context.CommandContext;
import com.yoheiyayoi.manager.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.gamerules.GameRules;

public class GameControlCommand {
    // setup game
    public static int setupGame(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        ServerLevel level = server.overworld();
        GameRules rules = level.getGameRules();

        rules.set(GameRules.LOCATOR_BAR, false, server);
        rules.set(GameRules.SPAWN_MOBS, false, server); // 15 นาทีแรกมอนไม่เกิด
        rules.set(GameRules.SHOW_DEATH_MESSAGES, false, server);
        rules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server);

        context.getSource().sendSuccess(() ->
                Component.literal("[✔] setup เรียบร้อยแล้ว").withStyle(ChatFormatting.GREEN), false
        );

        return 1;
    }

    // start game
    public static int startGame(CommandContext<CommandSourceStack> context) {
        GameManager manager = GameManager.getInstance();

        // return if already start
        if (manager.isStart()) {
            context.getSource().sendSuccess(() ->
                    Component.literal("[!] เกมเริ่มไปแล้ว").withStyle(ChatFormatting.RED), false
            );
            return 1;
        }

        // start the game
        manager.startGame();
        return 1;
    }

    // force end game
    public static int endGame(CommandContext<CommandSourceStack> context) {
        GameManager manager = GameManager.getInstance();

        // return if it doesn't start yet
        if (!manager.isStart()) {
            context.getSource().sendSuccess(() ->
                    Component.literal("[!] หยุดเกมไม่ได้ เกมยังไม่ได้เริ่ม").withStyle(ChatFormatting.RED), false
            );
            return 1;
        }

        // force end game
        manager.endGame();
        return 1;
    }

    // cart reach, suvivor won!
    public static int cartReachTheEnd(CommandContext<CommandSourceStack> context) {
        GameManager manager = GameManager.getInstance();

        if (!manager.isStart()) return 1;

        manager.survivorWin();
        return 1;
    }
}
