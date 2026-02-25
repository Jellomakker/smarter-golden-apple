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

		protected SmarterEatConfigScreen(Screen parent) {
			super(Text.literal("Smarter Eat Config"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			addDrawableChild(ButtonWidget.builder(
					getToggleText(),
					button -> {
						SmarterEatConfig.toggle();
						button.setMessage(getToggleText());
					})
					.dimensions(width / 2 - 100, height / 2 - 20, 200, 20)
					.build());

			addDrawableChild(ButtonWidget.builder(
					Text.literal("Done"),
					button -> close())
					.dimensions(width / 2 - 100, height / 2 + 10, 200, 20)
					.build());
		}

		private Text getToggleText() {
			return Text.literal("Smarter Eat: " + (SmarterEatConfig.isEnabled() ? "ON" : "OFF"));
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, float delta) {
			super.render(context, mouseX, mouseY, delta);
			context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 40, 0xFFFFFF);
		}

		@Override
		public void close() {
			client.setScreen(parent);
		}
	}
}
