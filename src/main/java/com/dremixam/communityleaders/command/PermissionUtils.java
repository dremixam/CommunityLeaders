package com.dremixam.communityleaders.command;

import com.dremixam.communityleaders.data.ModeratorManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class PermissionUtils {
    private static ModeratorManager moderatorManager;

    public static void setModeratorManager(ModeratorManager modManager) {
        moderatorManager = modManager;
    }

    public static boolean hasAnyPermission(ServerCommandSource source) {
        // Console et OP peuvent toujours utiliser les commandes
        if (!source.isExecutedByPlayer() || source.hasPermissionLevel(4)) {
            return true;
        }

        return hasPermission(source, "communityleaders.invite") ||
               hasPermission(source, "communityleaders.ban") ||
               hasPermission(source, "communityleaders.tree");
    }

    public static boolean hasPermission(ServerCommandSource source, String permission) {
        return hasPermission(source, permission, true);
    }

    public static boolean hasPermission(ServerCommandSource source, String permission, boolean inheritPermissions) {
        // Console peut toujours utiliser les commandes
        if (!source.isExecutedByPlayer()) {
            return true;
        }

        // Les OPs (niveau 4) peuvent toujours utiliser les commandes
        if (source.hasPermissionLevel(4)) {
            return true;
        }

        // Vérification des permissions LuckPerms pour les joueurs normaux
        try {
            ServerPlayerEntity player = source.getPlayer();
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);

            // Vérifier si le joueur a directement la permission
            if (user.getCachedData().getPermissionData().checkPermission(permission).asBoolean()) {
                return true;
            }

            if (inheritPermissions) {
                // Vérifier si le joueur est modérateur et si son leader a la permission
                UUID leaderUuid = moderatorManager.getLeader(player.getUuid());

                if (leaderUuid != null) {
                    var server = source.getServer();
                    var userCache = server.getUserCache();
                    var leaderProfileOpt = userCache.getByUuid(leaderUuid);

                    if (leaderProfileOpt.isPresent()) {
                        // Chercher le leader en ligne pour vérifier ses permissions
                        ServerPlayerEntity leaderPlayer = server.getPlayerManager().getPlayer(leaderUuid);
                        if (leaderPlayer != null) {
                            // Le leader est en ligne, vérifier ses permissions directement
                            User leaderUser = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(leaderPlayer);
                            return leaderUser.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
                        } else {
                            // Le leader n'est pas en ligne, charger ses données depuis LuckPerms
                            try {
                                User leaderUser = luckPerms.getUserManager().loadUser(leaderUuid).get();
                                if (leaderUser != null) {
                                    return leaderUser.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
                                }
                            } catch (Exception e) {
                                // En cas d'erreur, on refuse l'accès par sécurité
                                return false;
                            }
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Vérifie si un joueur (par UUID) a une permission LuckPerms, avec ou sans héritage (leader)
     */
    public static boolean hasPermission(UUID playerUuid, String permission, boolean inheritPermissions) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user == null) {
                user = luckPerms.getUserManager().loadUser(playerUuid).get();
            }
            if (user == null) return false;

            // Vérifier la permission directe
            if (user.getCachedData().getPermissionData().checkPermission(permission).asBoolean()) {
                return true;
            }

            if (inheritPermissions && moderatorManager != null) {
                UUID leaderUuid = moderatorManager.getLeader(playerUuid);
                if (leaderUuid != null) {
                    User leaderUser = luckPerms.getUserManager().getUser(leaderUuid);
                    if (leaderUser == null) {
                        leaderUser = luckPerms.getUserManager().loadUser(leaderUuid).get();
                    }
                    if (leaderUser != null) {
                        return leaderUser.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
