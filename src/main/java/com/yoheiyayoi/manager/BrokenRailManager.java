package com.yoheiyayoi.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.joml.Vector3f;

import java.util.*;

public class BrokenRailManager {
    private static BrokenRailManager instance;

    // BlockPos → [currentIron, requiredIron, textDisplayUUID]
    private final Map<BlockPos, BrokenRail> brokenRails = new HashMap<>();

    public static BrokenRailManager getInstance() {
        if (instance == null) instance = new BrokenRailManager();
        return instance;
    }

    public boolean isBroken(BlockPos pos) {
        return brokenRails.containsKey(pos);
    }

    public void breakRail(BlockPos pos, ServerLevel level) {
        if (brokenRails.containsKey(pos)) return;

        int required = 2 + new Random().nextInt(4); // random 2-5

        // spawn text display above rail
        Display.TextDisplay textDisplay = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        textDisplay.setPos(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
        textDisplay.setText(buildText(0, required));
        textDisplay.setBackgroundColor(0x40000000);
        textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        level.addFreshEntity(textDisplay);

        brokenRails.put(pos, new BrokenRail(0, required, textDisplay.getUUID(), level));
    }

    // returns true if fully repaired
    public boolean repair(BlockPos pos, ServerLevel level) {
        BrokenRail rail = brokenRails.get(pos);
        if (rail == null) return false;

        rail.current++;
        updateText(pos, level);

        if (rail.current >= rail.required) {
            removeDisplay(rail, level);
            brokenRails.remove(pos);
            return true;
        }

        return false;
    }

    private void updateText(BlockPos pos, ServerLevel level) {
        BrokenRail rail = brokenRails.get(pos);
        if (rail == null) return;

        level.getEntity(rail.displayUUID);
        if (level.getEntity(rail.displayUUID) instanceof Display.TextDisplay display) {
            display.setText(buildText(rail.current, rail.required));
        }
    }

    private void removeDisplay(BrokenRail rail, ServerLevel level) {
        if (level.getEntity(rail.displayUUID) instanceof Display.TextDisplay display) {
            display.discard();
        }
    }

    public void reset() {
        // remove all broken rail
        for (BrokenRail rail : brokenRails.values()) {
            removeDisplay(rail, rail.level);
        }

        brokenRails.clear();
    }

    private Component buildText(int current, int required) {
        return Component.literal(" รางรถไฟพัง \n")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(
                        Component.literal(" [" + current + "/" + required + "] ").withStyle(ChatFormatting.WHITE)
                );
    }

    public static class BrokenRail {
        public int current;
        public final int required;
        public final UUID displayUUID;
        public final ServerLevel level;

        public BrokenRail(int current, int required, UUID displayUUID, ServerLevel level) {
            this.current = current;
            this.required = required;
            this.displayUUID = displayUUID;
            this.level = level;
        }
    }
}