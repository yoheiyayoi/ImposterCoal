package com.yoheiyayoi.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.permissions.Permissions;

public class CommandRegister {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, commandBuildContext, commandSelection) -> {
            // setup game (set gamerule and etc)
            dispatcher.register(Commands.literal("setup_imposter_coal")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .executes(GameControlCommand::setupGame));

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

            // mute command (for imposter)
            dispatcher.register(Commands.literal("mute")
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(ImposterCommand::mutePlayer)
                    ));
        }));
    }
}
