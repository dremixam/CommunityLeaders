package com.dremixam.communityleaders.network;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.CharterManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class NetworkHandler {
    private static ConfigManager configManager;
    private static CharterManager charterManager;

    public static void initialize(ConfigManager config, CharterManager charter) {
        configManager = config;
        charterManager = charter;

        // Gestionnaire pour l'acceptation de la charte
        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.CHARTER_ACCEPT_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // Marquer que le joueur a accepté la charte
                charterManager.marquerCharteAcceptee(player.getUuid());

                // Optionnel : envoyer un message de confirmation
                player.sendMessage(Text.literal("§aVous avez accepté la charte du serveur. Bienvenue !"), false);
            });
        });

        // Gestionnaire pour le refus de la charte
        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.CHARTER_DECLINE_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // Déconnecter le joueur avec le message configuré
                player.networkHandler.disconnect(Text.literal(configManager.getCharterDeclineMessage()));
            });
        });
    }
}
