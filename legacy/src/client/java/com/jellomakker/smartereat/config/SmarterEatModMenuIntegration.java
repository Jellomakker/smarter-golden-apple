package com.jellomakker.smartereat.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class SmarterEatModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return SmarterEatConfigScreen::new;
	}

	private static class SmarterEatConfigScreen extends Screen {
		private final Screen parent;
		private ButtonWidget cooldownButton;

		protected SmarterEatConfigScreen(Screen parent) {
			super(Text.literal("Smarter Eat Config"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			int centerX = this.width / 2;
			int y = this.height / 4 + 24;

			// Enable/Disable toggle
			addDrawableChild(ButtonWidget.builder(
					getToggleText(),
					button -> {
						SmarterEatConfig.toggle();
						button.setMessage(getToggleText());
					})
					.dimensions(centerX - 100, y, 200, 20)
					.build());

			y += 30;

			// Cooldown display button (shows current value, click to reset to default)
			cooldownButton = ButtonWidget.builder(
					getCooldownText(),
					button -> {
						SmarterEatConfig.setPotionCooldownMs(50);
						updateCooldownButton();
					})
					.dimensions(centerX - 100, y, 200, 20)
					.build();
			addDrawableChild(cooldownButton);

			y += 24;

			// -50ms button
			addDrawableChild(ButtonWidget.builder(
					Text.literal("-50"),
					button -> {
						SmarterEatConfig.setPotionCooldownMs(SmarterEatConfig.getPotionCooldownMs() - 50);
						updateCooldownButton();
					})
					.dimensions(centerX - 100, y, 45, 20)
					.build());

			// -10ms button
			addDrawableChild(ButtonWidget.builder(
					Text.literal("-10"),
					button -> {
						SmarterEatConfig.setPotionCooldownMs(SmarterEatConfig.getPotionCooldownMs() - 10);
						updateCooldownButton();
					})
					.dimensions(centerX - 50, y, 45, 20)
					.build());

			// +10ms button
			addDrawableChild(ButtonWidget.builder(
					Text.literal("+10"),
					button -> {
						SmarterEatConfig.setPotionCooldownMs(SmarterEatConfig.getPotionCooldownMs() + 10);
						updateCooldownButton();
					})
					.dimensions(centerX + 5, y, 45, 20)
					.build());

			// +50ms button
			addDrawableChild(ButtonWidget.builder(
					Text.literal("+50"),
					button -> {
						SmarterEatConfig.setPotionCooldownMs(SmarterEatConfig.getPotionCooldownMs() + 50);
						updateCooldownButton();
					})
					.dimensions(centerX + 55, y, 45, 20)
					.build());

			y += 36;

			// Done button
			addDrawableChild(ButtonWidget.builder(
					Text.literal("Done"),
					button -> close())
					.dimensions(centerX - 100, y, 200, 20)
					.build());
		}

		private void updateCooldownButton() {
			if (cooldownButton != null) {
				cooldownButton.setMessage(getCooldownText());
			}
		}

		private Text getToggleText() {
			return Text.literal("Smarter Eat: " + (SmarterEatConfig.isEnabled() ? "ON" : "OFF"));
		}

		private Text getCooldownText() {
			return Text.literal("Potion Cooldown: " + SmarterEatConfig.getPotionCooldownMs() + "ms (click = reset)");
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, float delta) {
			super.render(context, mouseX, mouseY, delta);
			context.drawCenteredTextWithShadow(textRenderer, title, width / 2, this.height / 4 + 8, 0xFFFFFF);
		}

		@Override
		public void close() {
			client.setScreen(parent);
		}
	}
}
