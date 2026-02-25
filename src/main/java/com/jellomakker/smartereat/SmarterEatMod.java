package com.jellomakker.smartereat;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmarterEatMod implements ModInitializer {
	public static final String MOD_ID = "smartereat";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Smarter Eat initialized");
	}
}
