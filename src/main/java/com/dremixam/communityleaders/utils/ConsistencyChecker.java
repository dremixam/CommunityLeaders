package com.dremixam.communityleaders.utils;

import com.dremixam.communityleaders.Communityleaders;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Classe utilitaire pour vérifier et maintenir la cohérence des données
 * entre les permissions d'invitation et les statuts de modérateur.
 */
public class ConsistencyChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(Communityleaders.MOD_ID);
    private final InvitationManager invitationManager;
    private final ModeratorManager moderatorManager;
    private final MinecraftServer server;

    public ConsistencyChecker(InvitationManager invitationManager, ModeratorManager moderatorManager, MinecraftServer server) {
        this.invitationManager = invitationManager;
        this.moderatorManager = moderatorManager;
        this.server = server;
    }

    /**
     * Vérifie si un joueur a la permission d'inviter via LuckPerms (directe uniquement)
     */
    private boolean hasInvitePermission(UUID playerUuid) {
        try {
            // Utilise PermissionUtils pour vérifier la permission directe (sans héritage)
            return com.dremixam.communityleaders.command.PermissionUtils.hasPermission(playerUuid, "communityleaders.invite", false);
        } catch (Exception e) {
            LOGGER.warn("Error checking permissions for " + playerUuid, e);
            return false;
        }
    }

    /**
     * Effectue toutes les vérifications de cohérence au démarrage
     */
    public void performStartupConsistencyCheck() {
        LOGGER.info("Starting consistency checks...");

        // 0. Vérifier si le serveur est en mode whitelist
        checkWhitelistMode();

        // 1. Vérifier les modérateurs qui ont des permissions d'inviter
        checkModeratorsWithInvitePermission();

        // 2. Vérifier les invitations de leaders sans permission d'inviter
        checkInvalidInvitations();

        // 3. Vérifier que les modérateurs ont bien été invités par leur leader
        checkModeratorsNotInvitedByLeader();

        LOGGER.info("Consistency checks completed.");
    }

    /**
     * Vérifie si le serveur est en mode whitelist et affiche un warning si ce n'est pas le cas
     */
    private void checkWhitelistMode() {
        if (server != null && server.getPlayerManager() != null) {
            boolean isWhitelistEnabled = server.getPlayerManager().isWhitelistEnabled();

            if (!isWhitelistEnabled) {
                LOGGER.warn("*************************************************");
                LOGGER.warn("* WARNING: Server is not in whitelist mode!    *");
                LOGGER.warn("* Community Leaders plugin works best with     *");
                LOGGER.warn("* whitelist enabled for better player control. *");
                LOGGER.warn("*************************************************");
            } else {
                LOGGER.info("Server whitelist is enabled - Good practice for Community Leaders!");
            }
        }
    }

    /**
     * Vérifie et corrige les modérateurs qui ont des permissions d'inviter
     */
    private void checkModeratorsWithInvitePermission() {
        Map<UUID, List<UUID>> allModerators = moderatorManager.getAllModerators();

        for (Map.Entry<UUID, List<UUID>> entry : allModerators.entrySet()) {
            UUID leader = entry.getKey();
            List<UUID> moderators = new ArrayList<>(entry.getValue());

            for (UUID moderator : moderators) {
                if (hasInvitePermission(moderator)) {
                    LOGGER.info("Removing moderator status from " + moderator + " (has invite permission)");
                    moderatorManager.removeModerator(leader, moderator);
                }
            }
        }
    }

    /**
     * Vérifie et supprime les invitations de leaders sans permission d'inviter
     */
    private void checkInvalidInvitations() {
        Map<UUID, List<UUID>> allInvitations = invitationManager.getAllInvitations();
        List<UUID> leadersToRemove = new ArrayList<>();

        for (UUID leader : allInvitations.keySet()) {
            if (!hasInvitePermission(leader)) {
                LOGGER.info("Removing all invitations from leader " + leader + " (no longer has invite permission)");
                leadersToRemove.add(leader);
            }
        }

        for (UUID leader : leadersToRemove) {
            List<UUID> invited = new ArrayList<>(allInvitations.get(leader));
            for (UUID invitedPlayer : invited) {
                invitationManager.removeInvitation(leader, invitedPlayer);
            }
        }
    }

    /**
     * Vérifie que les modérateurs ont bien été invités par leur leader
     */
    private void checkModeratorsNotInvitedByLeader() {
        Map<UUID, List<UUID>> allModerators = moderatorManager.getAllModerators();

        for (Map.Entry<UUID, List<UUID>> entry : allModerators.entrySet()) {
            UUID leader = entry.getKey();
            List<UUID> moderators = new ArrayList<>(entry.getValue());

            for (UUID moderator : moderators) {
                // Vérifier si le modérateur a été invité par le leader
                if (!invitationManager.hasInvited(leader, moderator)) {
                    LOGGER.info("Removing moderator status from " + moderator + " under leader " + leader + " (not invited by this leader)");
                    moderatorManager.removeModerator(leader, moderator);
                }
            }
        }
    }

    /**
     * Vérifie la cohérence lorsque les permissions d'un joueur changent
     */
    public void checkPermissionChange(UUID playerUuid) {
        if (hasInvitePermission(playerUuid)) {
            handlePlayerGainedInvitePermission(playerUuid);
        }
    }

    /**
     * Gère le cas où un joueur vient d'obtenir la permission d'inviter
     */
    private void handlePlayerGainedInvitePermission(UUID playerUuid) {
        // 1. Retirer le statut de modérateur si applicable
        Map<UUID, List<UUID>> allModerators = moderatorManager.getAllModerators();
        for (Map.Entry<UUID, List<UUID>> entry : allModerators.entrySet()) {
            UUID leader = entry.getKey();
            if (entry.getValue().contains(playerUuid)) {
                LOGGER.info("Removing moderator status from " + playerUuid + " under leader " + leader);
                moderatorManager.removeModerator(leader, playerUuid);
            }
        }

        // 2. Retirer le lien d'invitation si applicable
        UUID inviter = invitationManager.getInviter(playerUuid);
        if (inviter != null) {
            LOGGER.info("Removing invitation link for " + playerUuid + " (invited by " + inviter + ")");
            invitationManager.removeInvitation(inviter, playerUuid);
        }
    }
}
