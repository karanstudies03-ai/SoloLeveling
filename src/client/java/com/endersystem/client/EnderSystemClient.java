package com.endersystem.client;

import com.endersystem.EnderSystem;
import com.endersystem.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class EnderSystemClient implements ClientModInitializer {
    private static KeyBinding openSystemKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category =
                KeyBinding.Category.create(Identifier.of(EnderSystem.MOD_ID, "main"));
        openSystemKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.endersystem.open_system",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSystemKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new StatusScreen());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.LevelUpPayload.ID, (payload, context) -> {
            LevelUpOverlay.trigger(payload.level(), payload.pointsGained());
            context.player().playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        });

        LevelUpOverlay.init();
    }
}
