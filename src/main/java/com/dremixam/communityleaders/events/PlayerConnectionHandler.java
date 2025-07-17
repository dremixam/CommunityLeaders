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

    // Joueurs en attente d'acceptation de la charte
    private static final Set<UUID> playersAwaitingCharter = ConcurrentHashMap.newKeySet();

    public static void initialize(ConfigManager config, CharterManager charter) {
        configManager = config;
        charterManager = charter;

        // Quand un joueur se connecte, vérifier s'il doit voir la charte
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerId = player.getUuid();

            // Vérifier si le joueur doit accepter la charte
            if (shouldShowCharter(playerId)) {
                // Marquer comme en attente - le mixin bloquera seulement l'interaction
                playersAwaitingCharter.add(playerId);

                // Mettre en mode spectateur temporaire pour que les autres joueurs ne le voient pas
                player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);

                // Envoyer la charte immédiatement, pendant que le chargement est en cours
                sendCharterToPlayer(player);
            }
        });
    }

    // Vérifier si un joueur doit voir la charte
    public static boolean shouldShowCharter(UUID playerId) {
        if (!configManager.isCharterEnabled()) {
            return false;
        }

        return !charterManager.aAccepteCharte(playerId);
    }

    // Envoyer la charte à un joueur
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
                System.err.println("Erreur lors de l'envoi de la charte : " + e.getMessage());
            }
        });
    }

    // Méthode appelée quand un joueur accepte la charte
    public static void onCharterAccepted(UUID playerId) {
        if (playersAwaitingCharter.remove(playerId)) {
            // Marquer la charte comme acceptée
            charterManager.marquerCharteAcceptee(playerId);

            // Le mode de jeu sera restauré automatiquement par le NetworkHandler
            // qui a accès direct au joueur
        }
    }

    // Méthode appelée quand un joueur refuse la charte
    public static void onCharterDeclined(UUID playerId) {
        playersAwaitingCharter.remove(playerId);
        // La déconnexion sera gérée par le NetworkHandler
    }

    // Vérifier si un joueur est en attente de la charte (utilisé par le mixin)
    public static boolean isAwaitingCharter(UUID playerId) {
        return playersAwaitingCharter.contains(playerId);
    }
}
