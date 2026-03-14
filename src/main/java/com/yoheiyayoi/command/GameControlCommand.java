package com.yoheiyayoi.command;

import com.mojang.brigadier.context.CommandContext;
import com.yoheiyayoi.manager.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class GameControlCommand {
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
