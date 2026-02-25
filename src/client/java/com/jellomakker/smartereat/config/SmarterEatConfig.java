package com.jellomakker.smartereat.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Simple file-based config for Smarter Eat.
 * Stores whether the mod is enabled and the potion cooldown duration in ms.
 */
public class SmarterEatConfig {
	private static final Path CONFIG_PATH = Paths.get("config", "smartereat.properties");
	private static boolean enabled = true;
	private static int potionCooldownMs = 50;

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
		save();
	}

	public static int getPotionCooldownMs() {
		return potionCooldownMs;
	}

	public static void setPotionCooldownMs(int ms) {
		if (ms < 0) ms = 0;
		if (ms > 1000) ms = 1000;
		potionCooldownMs = ms;
		save();
	}

	public static void toggle() {
		setEnabled(!enabled);
	}

	public static void load() {
		try {
			if (Files.exists(CONFIG_PATH)) {
				Properties props = new Properties();
				try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
					props.load(in);
				}
				enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
				potionCooldownMs = Integer.parseInt(props.getProperty("potionCooldownMs", "50"));
				if (potionCooldownMs < 0) potionCooldownMs = 50;
				if (potionCooldownMs > 1000) potionCooldownMs = 50;
			}
		} catch (IOException | NumberFormatException e) {
			enabled = true;
			potionCooldownMs = 50;
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Properties props = new Properties();
			props.setProperty("enabled", Boolean.toString(enabled));
			props.setProperty("potionCooldownMs", Integer.toString(potionCooldownMs));
			try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
				props.store(out, "Smarter Eat Configuration");
			}
		} catch (IOException e) {
			// Silently fail
		}
	}
}
