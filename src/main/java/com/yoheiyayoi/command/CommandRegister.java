package com.yoheiyayoi.command;

import com.yoheiyayoi.manager.GameManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public class CommandRegister {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, commandBuildContext, commandSelection) -> {
            // start game
            dispatcher.register(Commands.literal("start_game")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .executes(GameControlCommand::startGame));

            // force end game
            dispatcher.register(Commands.literal("end_game")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .executes(GameControlCommand::endGame));

            // cart reach the end
            dispatcher.register(Commands.literal("cart_reach_the_end")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .executes(GameControlCommand::cartReachTheEnd));
        }));
    }
}
