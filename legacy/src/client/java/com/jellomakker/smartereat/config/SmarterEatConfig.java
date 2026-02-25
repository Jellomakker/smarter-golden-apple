package com.jellomakker.smartereat.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Simple file-based config for Smarter Eat.
 * Stores a single boolean: whether the mod is enabled.
 */
public class SmarterEatConfig {
	private static final Path CONFIG_PATH = Paths.get("config", "smartereat.properties");
	private static boolean enabled = true;

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
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
			}
		} catch (IOException e) {
			enabled = true;
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Properties props = new Properties();
			props.setProperty("enabled", Boolean.toString(enabled));
			try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
				props.store(out, "Smarter Eat Configuration");
			}
		} catch (IOException e) {
			// Silently fail
		}
	}
}
