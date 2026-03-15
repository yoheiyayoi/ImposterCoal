package com.yoheiyayoi.manager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class ImposterItem {
    //-- Items
    public static ItemStack getHammer() {
        ItemStack hammer = new ItemStack(Items.NETHERITE_HOE);

        // Custom name
        hammer.set(DataComponents.CUSTOM_NAME,
                Component.literal("ค้อนมหาปลัย").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
        );

        return hammer;
    }

    //-- Functions

    public static void giveItemToImposter(ServerPlayer player) {
        ItemStack hammer = getHammer();
        player.getInventory().add(hammer);
    }
}
