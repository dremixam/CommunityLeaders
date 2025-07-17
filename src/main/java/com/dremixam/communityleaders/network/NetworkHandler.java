package com.dremixam.communityleaders.network;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.RulesManager;
import com.dremixam.communityleaders.events.PlayerConnectionHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class NetworkHandler {
    private static ConfigManager configManager;
    private static RulesManager rulesManager;

    public static void initialize(ConfigManager config, RulesManager rules) {
        configManager = config;
        rulesManager = rules;

        // Handler for rules acceptance
        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.RULES_ACCEPT_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // Check if the player was waiting
                if (PlayerConnectionHandler.isAwaitingRules(player.getUuid())) {
                    // Mark acceptance via PlayerConnectionHandler
                    PlayerConnectionHandler.onRulesAccepted(player.getUuid());

                    // Restore player to normal mode (exit spectator mode)
                    player.changeGameMode(GameMode.SURVIVAL);

                    // Confirmation message from configuration
                    String acceptedMessage = configManager.getRulesAcceptedMessage();
                    player.sendMessage(Text.literal("§a" + acceptedMessage), false);
                }
            });
        });

        // Handler for rules decline
        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.RULES_DECLINE_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // Mark decline
                PlayerConnectionHandler.onRulesDeclined(player.getUuid());

                // Disconnect player with configured message
                player.networkHandler.disconnect(Text.literal(configManager.getRulesDeclineMessage()));
            });
        });
    }
}
