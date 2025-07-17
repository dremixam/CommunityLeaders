package com.dremixam.communityleaders.network;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.CharterManager;
import com.dremixam.communityleaders.events.PlayerConnectionHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class NetworkHandler {
    private static ConfigManager configManager;
    private static CharterManager charterManager;

    public static void initialize(ConfigManager config, CharterManager charter) {
        configManager = config;
        charterManager = charter;

        // Gestionnaire pour l'acceptation de la charte
        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.CHARTER_ACCEPT_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // Vérifier si le joueur était en attente
                if (PlayerConnectionHandler.isAwaitingCharter(player.getUuid())) {
                    // Marquer l'acceptation via PlayerConnectionHandler
                    PlayerConnectionHandler.onCharterAccepted(player.getUuid());

                    // Restaurer le joueur en mode normal (sortir du mode spectateur)
                    player.changeGameMode(GameMode.SURVIVAL);

                    // Message de confirmation
                    player.sendMessage(Text.literal("§aVous avez accepté la charte du serveur. Bienvenue !"), false);
                }
            });
        });

        // Gestionnaire pour le refus de la charte
        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.CHARTER_DECLINE_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // Marquer le refus
                PlayerConnectionHandler.onCharterDeclined(player.getUuid());

                // Déconnecter le joueur avec le message configuré
                player.networkHandler.disconnect(Text.literal(configManager.getCharterDeclineMessage()));
            });
        });
    }
}
