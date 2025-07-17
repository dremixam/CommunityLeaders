package com.dremixam.communityleaders.events;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.RulesManager;
import com.dremixam.communityleaders.network.RulesPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerConnectionHandler {
    private static ConfigManager configManager;
    private static RulesManager rulesManager;

    // Players waiting for rules acceptance
    private static final Set<UUID> playersAwaitingRules = ConcurrentHashMap.newKeySet();

    public static void initialize(ConfigManager config, RulesManager rules) {
        configManager = config;
        rulesManager = rules;

        // When a player connects, check if they need to see the rules
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerId = player.getUuid();

            // Check if the player needs to accept the rules
            if (shouldShowRules(playerId)) {
                // Mark as waiting - the mixin will only block interaction
                playersAwaitingRules.add(playerId);

                // Put in temporary spectator mode so other players don't see them
                player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);

                // Send rules immediately, while loading is in progress
                sendRulesToPlayer(player);
            }
        });
    }

    // Check if a player should see the rules
    public static boolean shouldShowRules(UUID playerId) {
        if (!configManager.isRulesEnabled()) {
            return false;
        }

        return !rulesManager.hasAcceptedRules(playerId);
    }

    // Send rules to a player
    private static void sendRulesToPlayer(ServerPlayerEntity player) {
        if (player == null || player.networkHandler == null) {
            return;
        }

        player.server.execute(() -> {
            try {
                RulesPacket.sendRulesToPlayer(
                    player,
                    configManager.getRulesTitle(),
                    configManager.getRulesContent(),
                    configManager.getRulesAcceptButton(),
                    configManager.getRulesDeclineButton(),
                    configManager.getRulesCheckboxText(),
                    configManager.getRulesDeclineMessage()
                );
            } catch (Exception e) {
                System.err.println("Error sending rules: " + e.getMessage());
            }
        });
    }

    // Method called when a player accepts the rules
    public static void onRulesAccepted(UUID playerId) {
        if (playersAwaitingRules.remove(playerId)) {
            // Mark rules as accepted
            rulesManager.markRulesAccepted(playerId);

            // Game mode will be restored automatically by the NetworkHandler
            // which has direct access to the player
        }
    }

    // Method called when a player declines the rules
    public static void onRulesDeclined(UUID playerId) {
        playersAwaitingRules.remove(playerId);
        // Disconnection will be handled by the NetworkHandler
    }

    // Check if a player is waiting for the rules (used by mixin)
    public static boolean isAwaitingRules(UUID playerId) {
        return playersAwaitingRules.contains(playerId);
    }
}
