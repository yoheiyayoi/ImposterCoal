package com.yoheiyayoi;

import com.yoheiyayoi.event.CancelRailBreak;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImposterCoal implements ModInitializer {
	public static final String MOD_ID = "imposter-coal";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CancelRailBreak.register();
	}
}