package com.yoheiyayoi.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yoheiyayoi.Utils;
import com.yoheiyayoi.manager.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

public class ImposterCommand {
    public static int mutePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer user = context.getSource().getPlayer();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        GameManager gameManager = GameManager.getInstance();

        // imposter check
        assert user != null;
        if (user.getUUID() != gameManager.getImposter()) {
            context.getSource().sendSuccess(() ->
                    Component.literal("[!] มีแค่ imposter ในตานี้เท่านั้นที่ใช้คำสั่งนี้ได้ไอควาย").withStyle(ChatFormatting.RED), false
            );

            Utils.sendSoundToPlayer(user, SoundEvents.ENDER_EYE_DEATH, 1.0f, 1.0f);
            return 1;
        }

        if (!gameManager.getSurvivors().contains(target.getUUID())) {
            context.getSource().sendSuccess(() ->
                    Component.literal("[!] ผู้เล่น " + target.getName().getString() + " ไม่สามารถสาปได้ เนื่องจากไม่พบผู้เล่น").withStyle(ChatFormatting.RED), false
            );

            Utils.sendSoundToPlayer(user, SoundEvents.ENDER_EYE_DEATH, 1.0f, 1.0f);
            return 1;
        }

        if (!gameManager.canMutePlayer()) {
            context.getSource().sendSuccess(() ->
                    Component.literal("[!] ยังไม่สามารถสาปผู้เล่นในตอนนี้ได้").withStyle(ChatFormatting.RED), false
            );

            Utils.sendSoundToPlayer(user, SoundEvents.ENDER_EYE_DEATH, 1.0f, 1.0f);
            return 1;
        }

        if (gameManager.isUseMutePower) {
            context.getSource().sendSuccess(() ->
                    Component.literal("[!] คุณได้ใช้พลังสาปผู้เล่นไปแล้วในตานี้!").withStyle(ChatFormatting.RED), false
            );

            Utils.sendSoundToPlayer(user, SoundEvents.ENDER_EYE_DEATH, 1.0f, 1.0f);
            return 1;
        }

        gameManager.isUseMutePower = true;
        target.displayClientMessage(Component.literal("คุณถูกสาปให้เป็นใบ้โดย imposter ในตานี้!").withStyle(ChatFormatting.RED), false);
        target.displayClientMessage(Component.literal("เนื่องจากคำสาป คุณจะไม่ได้รับอนุญาตให้เปิดไมค์ในตานี้อีกตลอดทั้งเกม").withStyle(ChatFormatting.RED), false);
        Utils.sendTitleToPlayer(target,
                Component.literal("คุณถูกสาป!").withStyle(ChatFormatting.RED),
                Component.literal("คุณจะไม่สามารถพูดคุยกับผู้เล่นอื่นได้อีกต่อไป!").withStyle(ChatFormatting.GRAY)
        );
        Utils.sendSoundToPlayer(target, SoundEvents.AMBIENT_CAVE.value(), 1.0f, 1.0f);

        context.getSource().sendSuccess(() ->
                Component.literal("[✔] mute ผู้เล่น " + target.getName().getString() + " แล้ว!").withStyle(ChatFormatting.GREEN), false
        );

        Utils.sendSoundToPlayer(user, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        return 1;
    }
}
