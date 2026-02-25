package com.jellomakker.smartereat.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class SmarterEatModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return SmarterEatConfigScreen::new;
	}

	private static class SmarterEatConfigScreen extends Screen {
		private final Screen parent;
		private TextFieldWidget cooldownField;

		protected SmarterEatConfigScreen(Screen parent) {
			super(Text.literal("Smarter Eat Config"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			// Enable/Disable toggle
			addDrawableChild(ButtonWidget.builder(
					getToggleText(),
					button -> {
						SmarterEatConfig.toggle();
						button.setMessage(getToggleText());
					})
					.dimensions(width / 2 - 100, height / 2 - 40, 200, 20)
					.build());

			// Potion cooldown input
			cooldownField = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2, 200, 20, Text.literal("Potion Cooldown (ms)"));
			cooldownField.setText(String.valueOf(SmarterEatConfig.getPotionCooldownMs()));
			addDrawableChild(cooldownField);
			setInitialFocus(cooldownField);

			// Save button
			addDrawableChild(ButtonWidget.builder(
					Text.literal("Save"),
					button -> {
						try {
							int ms = Integer.parseInt(cooldownField.getText());
							SmarterEatConfig.setPotionCooldownMs(ms);
						} catch (NumberFormatException ignored) {
							// Invalid input, revert to current value
							cooldownField.setText(String.valueOf(SmarterEatConfig.getPotionCooldownMs()));
						}
					})
					.dimensions(width / 2 - 100, height / 2 + 30, 95, 20)
					.build());

			// Done button
			addDrawableChild(ButtonWidget.builder(
					Text.literal("Done"),
					button -> close())
					.dimensions(width / 2 + 5, height / 2 + 30, 95, 20)
					.build());
		}

		private Text getToggleText() {
			return Text.literal("Smarter Eat: " + (SmarterEatConfig.isEnabled() ? "ON" : "OFF"));
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, float delta) {
			super.render(context, mouseX, mouseY, delta);
			context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 60, 0xFFFFFF);
			context.drawTextWithShadow(textRenderer, Text.literal("Potion Cooldown (ms):"), width / 2 - 150, height / 2 + 4, 0xAAAAAA);
		}

		@Override
		public void close() {
			client.setScreen(parent);
		}
	}
}
