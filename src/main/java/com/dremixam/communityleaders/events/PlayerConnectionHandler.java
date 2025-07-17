package com.dremixam.communityleaders.events;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.CharterManager;
import com.dremixam.communityleaders.network.CharterPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerConnectionHandler {
    private static ConfigManager configManager;
    private static CharterManager charterManager;

    // Players waiting for charter acceptance
    private static final Set<UUID> playersAwaitingCharter = ConcurrentHashMap.newKeySet();

    public static void initialize(ConfigManager config, CharterManager charter) {
        configManager = config;
        charterManager = charter;

        // When a player connects, check if they need to see the charter
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerId = player.getUuid();

            // Check if the player needs to accept the charter
            if (shouldShowCharter(playerId)) {
                // Mark as waiting - the mixin will only block interaction
                playersAwaitingCharter.add(playerId);

                // Put in temporary spectator mode so other players don't see them
                player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);

                // Send charter immediately, while loading is in progress
                sendCharterToPlayer(player);
            }
        });
    }

    // Check if a player should see the charter
    public static boolean shouldShowCharter(UUID playerId) {
        if (!configManager.isCharterEnabled()) {
            return false;
        }

        return !charterManager.hasAcceptedCharter(playerId);
    }

    // Send charter to a player
    private static void sendCharterToPlayer(ServerPlayerEntity player) {
        if (player == null || player.networkHandler == null) {
            return;
        }

        player.server.execute(() -> {
            try {
                CharterPacket.sendCharterToPlayer(
                    player,
                    configManager.getCharterTitle(),
                    configManager.getCharterContent(),
                    configManager.getCharterAcceptButton(),
                    configManager.getCharterDeclineButton(),
                    configManager.getCharterCheckboxText(),
                    configManager.getCharterDeclineMessage()
                );
            } catch (Exception e) {
                System.err.println("Error sending charter: " + e.getMessage());
            }
        });
    }

    // Method called when a player accepts the charter
    public static void onCharterAccepted(UUID playerId) {
        if (playersAwaitingCharter.remove(playerId)) {
            // Mark charter as accepted
            charterManager.markCharterAccepted(playerId);

            // Game mode will be restored automatically by the NetworkHandler
            // which has direct access to the player
        }
    }

    // Method called when a player declines the charter
    public static void onCharterDeclined(UUID playerId) {
        playersAwaitingCharter.remove(playerId);
        // Disconnection will be handled by the NetworkHandler
    }

    // Check if a player is waiting for the charter (used by mixin)
    public static boolean isAwaitingCharter(UUID playerId) {
        return playersAwaitingCharter.contains(playerId);
    }
}
