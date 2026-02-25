package com.jellomakker.smartereat;

import com.jellomakker.smartereat.config.SmarterEatConfig;
import com.jellomakker.smartereat.mixin.KeyBindingAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/**
 * Smarter Eat – tap right-click N times to queue eating N food items.
 *
 * How it works:
 * - Uses START_CLIENT_TICK which fires BEFORE handleInputEvents in the game loop.
 * - On first tap: lets the game start eating naturally (leaves 1 press for handleInputEvents).
 * - Forces the use key pressed so the game thinks the player is holding right-click.
 * - When one eat finishes, injects another press to start the next queued eat.
 * - On reset, restores the real physical key state via GLFW to prevent stuck keys.
 * - No custom packets – only standard game interaction paths.
 */
public class SmarterEatClient implements ClientModInitializer {
	private int queuedEats = 0;
	private ItemStack trackedFood = ItemStack.EMPTY;
	private Hand trackedHand = null;
	private boolean forceHolding = false;
	private boolean wasEatingLastTick = false;
	private int lastSelectedSlot = -1;
	private int idleTickCount = 0;

	@Override
	public void onInitializeClient() {
		SmarterEatConfig.load();
		// START fires before handleInputEvents, so we can override key state in time
		ClientTickEvents.START_CLIENT_TICK.register(this::onStartTick);
	}

	private void onStartTick(MinecraftClient client) {
		if (!SmarterEatConfig.isEnabled() || client.player == null || client.interactionManager == null || client.options == null) {
			if (forceHolding) fullReset(client);
			return;
		}

		PlayerEntity player = client.player;
		KeyBinding useKey = client.options.useKey;
		KeyBindingAccessor useKeyAccess = (KeyBindingAccessor) useKey;

		// ── Cancel conditions ──

		// Hotbar slot changed → cancel queue
		int currentSlot = player.getInventory().selectedSlot;
		if (lastSelectedSlot != -1 && currentSlot != lastSelectedSlot && forceHolding) {
			fullReset(client);
		}
		lastSelectedSlot = currentSlot;

		// Left-click (attack) → cancel queue AND ensure the attack/swing goes through
		KeyBinding attackKey = client.options.attackKey;
		KeyBindingAccessor attackKeyAccess = (KeyBindingAccessor) attackKey;
		if (attackKey.isPressed() && forceHolding) {
			// Stop eating via the standard interaction manager method (same path the
			// game uses when the use key is released – NOT a direct packet)
			if (player.isUsingItem()) {
				client.interactionManager.stopUsingItem(player);
			}
			fullReset(client);
			// Ensure the attack press is queued so handleInputEvents → doAttack fires.
			// Now that isUsingItem() is false, the game will process the swing normally.
			if (attackKeyAccess.getTimesPressed() < 1) {
				attackKeyAccess.setTimesPressed(1);
			}
			attackKey.setPressed(true);
		}

		// ── Read state ──

		int newPresses = useKeyAccess.getTimesPressed();
		boolean isEating = player.isUsingItem();

		// ── Queue management ──

		if (forceHolding) {
			// Already in force-hold mode
			if (newPresses > 0) {
				// Additional clicks while eating → add to queue
				Hand foodHand = getFoodHand(player);
				if (foodHand != null && foodHand == trackedHand
						&& isSameFoodType(player.getStackInHand(foodHand), trackedFood)) {
					queuedEats += newPresses;
				}
				// Consume presses so handleInputEvents doesn't start extra interactions
				useKeyAccess.setTimesPressed(0);
			}

			// Keep the use key forced so the game doesn't send stop-using packet
			useKey.setPressed(true);

			// Safety: if force-holding but player isn't eating for several ticks, abort
			if (!isEating) {
				idleTickCount++;
				if (idleTickCount > 10) {
					fullReset(client);
					wasEatingLastTick = false;
					return;
				}
			} else {
				idleTickCount = 0;
			}

		} else if (newPresses > 0) {
			// Not force-holding yet: first click(s) detected
			Hand foodHand = getFoodHand(player);
			if (foodHand != null) {
				ItemStack food = player.getStackInHand(foodHand);
				trackedHand = foodHand;
				trackedFood = food.copy();
				// First eat is handled naturally by the game (we leave 1 press)
				queuedEats = newPresses - 1;
				forceHolding = true;
				idleTickCount = 0;
				// Leave exactly 1 press for handleInputEvents → doItemUse
				useKeyAccess.setTimesPressed(1);
				// Force key held so that eating continues after user releases the button
				useKey.setPressed(true);
			}
			// If not food, don't interfere – game handles normally
		}

		// ── Detect eating completion ──

		if (wasEatingLastTick && !isEating && forceHolding) {
			if (queuedEats > 0) {
				// Validate same food is still in hand
				ItemStack current = player.getStackInHand(trackedHand);
				if (isSameFoodType(current, trackedFood)) {
					queuedEats--;
					idleTickCount = 0;
					// Inject a press so handleInputEvents → doItemUse starts next eat
					useKeyAccess.setTimesPressed(useKeyAccess.getTimesPressed() + 1);
					useKey.setPressed(true);
				} else {
					fullReset(client);
				}
			} else {
				// All queued eats done
				fullReset(client);
			}
		}
		wasEatingLastTick = isEating;

		// ── Extra validation ──
		if (forceHolding && trackedHand != null && !isEating) {
			ItemStack current = player.getStackInHand(trackedHand);
			if (current.isEmpty()) {
				fullReset(client);
			}
		}
	}

	/**
	 * Clears all queue state and restores the real physical key state via GLFW
	 * so the use key doesn't get stuck after we stop forcing it.
	 */
	private void fullReset(MinecraftClient client) {
		boolean wasForcing = forceHolding;
		queuedEats = 0;
		trackedFood = ItemStack.EMPTY;
		trackedHand = null;
		forceHolding = false;
		wasEatingLastTick = false;
		idleTickCount = 0;

		if (wasForcing && client != null && client.options != null && client.getWindow() != null) {
			restoreRealKeyState(client, client.options.useKey);
		}
	}

	/**
	 * Queries GLFW for the real hardware state of the bound key/mouse button
	 * and restores KeyBinding.pressed to match, preventing stuck keys.
	 */
	private void restoreRealKeyState(MinecraftClient client, KeyBinding key) {
		try {
			InputUtil.Key boundKey = ((KeyBindingAccessor) key).getBoundKey();
			long window = client.getWindow().getHandle();
			boolean reallyPressed;
			if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
				reallyPressed = GLFW.glfwGetMouseButton(window, boundKey.getCode()) == GLFW.GLFW_PRESS;
			} else {
				reallyPressed = GLFW.glfwGetKey(window, boundKey.getCode()) == GLFW.GLFW_PRESS;
			}
			key.setPressed(reallyPressed);
		} catch (Exception ignored) {
			// Fallback: just release the key
			key.setPressed(false);
		}
	}

	private Hand getFoodHand(PlayerEntity player) {
		ItemStack mainHand = player.getMainHandStack();
		if (mainHand.get(DataComponentTypes.FOOD) != null) {
			return Hand.MAIN_HAND;
		}
		ItemStack offHand = player.getOffHandStack();
		if (offHand.get(DataComponentTypes.FOOD) != null) {
			return Hand.OFF_HAND;
		}
		return null;
	}

	private boolean isSameFoodType(ItemStack a, ItemStack b) {
		if (a.isEmpty() || b.isEmpty()) return false;
		if (a.getItem() != b.getItem()) return false;
		ItemStack aCopy = a.copy();
		ItemStack bCopy = b.copy();
		aCopy.setCount(1);
		bCopy.setCount(1);
		return ItemStack.areItemsAndComponentsEqual(aCopy, bCopy);
	}
}
