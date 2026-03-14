package com.yoheiyayoi;

import com.yoheiyayoi.command.CommandRegister;
import com.yoheiyayoi.event.CancelRailBreak;
import com.yoheiyayoi.manager.GameManager;
import com.yoheiyayoi.sound.ModSounds;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImposterCoal implements ModInitializer {
	public static final String MOD_ID = "imposter-coal";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Init
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			GameManager.getInstance().init(server);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			GameManager.getInstance().tick();
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayer player) {
				GameManager.getInstance().onPlayerDeath(player);
			}
		});

		ModSounds.registerSound();

		// Events
		CancelRailBreak.register();

		// Commands
		CommandRegister.register();
	}
}