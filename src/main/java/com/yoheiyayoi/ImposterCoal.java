package com.yoheiyayoi;

import com.yoheiyayoi.command.CommandRegister;
import com.yoheiyayoi.event.CancelRailBreak;
import com.yoheiyayoi.manager.GameManager;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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

		// Events
		CancelRailBreak.register();

		// Commands
		CommandRegister.register();
	}
}