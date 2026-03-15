package com.yoheiyayoi.manager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

        // add item to inventory but not main hand
        // ปาย said: แม่งเคียวมาโผล่ในมือเลย
        giveItemNotMainHand(player, hammer);
    }

    //-- Utils
    private static void giveItemNotMainHand(ServerPlayer player, ItemStack item) {
        for (int slot = 1; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setItem(slot, item);
                return;
            }
        }

        // inventory full, drop the item
        player.drop(item, false);
    }
}
