package com.yoheiyayoi.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.permissions.Permissions;

public class CommandRegister {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, commandBuildContext, commandSelection) -> {
            // impostercoal command (setup, start, finish, end)
            dispatcher.register(Commands.literal("impostercoal")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .then(Commands.literal("setup").executes(GameControlCommand::setupGame))
                    .then(Commands.literal("start").executes(GameControlCommand::startGame))
                    .then(Commands.literal("finish").executes(GameControlCommand::cartReachTheEnd))
                    .then(Commands.literal("end").executes(GameControlCommand::endGame))
            );

            // mute command (for imposter)
            dispatcher.register(Commands.literal("mute")
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(ImposterCommand::mutePlayer)
                    ));
        }));
    }
}
