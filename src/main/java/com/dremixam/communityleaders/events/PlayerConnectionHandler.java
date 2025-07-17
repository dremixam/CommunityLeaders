package com.dremixam.communityleaders.events;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.CharterManager;
import com.dremixam.communityleaders.network.CharterPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerConnectionHandler {
    private static ConfigManager configManager;
    private static CharterManager charterManager;

    public static void initialize(ConfigManager config, CharterManager charter) {
        configManager = config;
        charterManager = charter;

        // Événement quand un joueur rejoint le serveur
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Vérifier si la charte est activée
            if (!configManager.isCharterEnabled()) {
                return;
            }

            // Vérifier si le joueur a déjà accepté la charte
            if (charterManager.aAccepteCharte(player.getUuid())) {
                return;
            }

            // Délai pour s'assurer que le client est prêt
            server.execute(() -> {
                try {
                    Thread.sleep(1000); // Attendre 1 seconde

                    // Envoyer la charte au client
                    CharterPacket.sendCharterToPlayer(
                        player,
                        configManager.getCharterTitle(),
                        configManager.getCharterContent(),
                        configManager.getCharterAcceptButton(),
                        configManager.getCharterDeclineButton(),
                        configManager.getCharterCheckboxText(),
                        configManager.getCharterDeclineMessage()
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });
    }
}
