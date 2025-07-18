package com.dremixam.communityleaders.command.subcommands;

import com.dremixam.communityleaders.command.PermissionUtils;
import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.*;
import java.util.stream.Collectors;

public class TreeCommand {
    private static InvitationManager invitationManager;
    private static ConfigManager configManager;
    private static ModeratorManager moderatorManager;

    public static void setManagers(InvitationManager invManager, ConfigManager confManager, ModeratorManager modManager) {
        invitationManager = invManager;
        configManager = confManager;
        moderatorManager = modManager;
    }

    public static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();
            Map<UUID, List<UUID>> allInvitations = invitationManager.getAllInvitations();

            if (allInvitations.isEmpty()) {
                if (source.isExecutedByPlayer()) {
                    source.getPlayer().sendMessage(Text.literal("§e" + "No invitations found."), false);
                } else {
                    source.sendFeedback(() -> Text.literal("No invitations found."), false);
                }
                return 0;
            }

            if (source.isExecutedByPlayer()) {
                ServerPlayerEntity streamer = source.getPlayer();
                streamer.sendMessage(Text.literal("§a" + configManager.getMessage("tree_title")), false);

                Set<UUID> allInviters = allInvitations.keySet();
                Set<UUID> allInvited = allInvitations.values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toSet());

                Set<UUID> rootPlayers = allInviters.stream()
                        .filter(inviter -> !allInvited.contains(inviter))
                        .collect(Collectors.toSet());

                if (rootPlayers.isEmpty()) {
                    streamer.sendMessage(Text.literal("§e" + "No root players found (circular invitations detected)."), false);
                    return 0;
                }

                for (UUID rootPlayer : rootPlayers) {
                    displayPlayerTree(streamer, userCache, allInvitations, rootPlayer, "", true);
                }
            } else {
                // Console version
                source.sendFeedback(() -> Text.literal(configManager.getMessage("tree_title")), false);

                Set<UUID> allInviters = allInvitations.keySet();
                Set<UUID> allInvited = allInvitations.values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toSet());

                Set<UUID> rootPlayers = allInviters.stream()
                        .filter(inviter -> !allInvited.contains(inviter))
                        .collect(Collectors.toSet());

                if (rootPlayers.isEmpty()) {
                    source.sendFeedback(() -> Text.literal("No root players found (circular invitations detected)."), false);
                    return 0;
                }

                for (UUID rootPlayer : rootPlayers) {
                    displayConsoleTree(source, userCache, allInvitations, rootPlayer, "", true);
                }
            }

            return 1;
        } catch (Exception e) {
            if (source.isExecutedByPlayer()) {
                source.getPlayer().sendMessage(Text.literal("§c" + "Error displaying tree: " + e.getMessage()), false);
            } else {
                source.sendFeedback(() -> Text.literal("Error displaying tree: " + e.getMessage()), false);
            }
            return 0;
        }
    }

    private static void displayPlayerTree(ServerPlayerEntity streamer,
                                        net.minecraft.util.UserCache userCache,
                                        Map<UUID, List<UUID>> allInvitations,
                                        UUID playerUuid,
                                        String prefix,
                                        boolean isRoot) {
        var profileOpt = userCache.getByUuid(playerUuid);
        String playerName = profileOpt.map(profile -> profile.getName()).orElse("Unknown Player");

        // Vérifier les statuts du joueur et appliquer la couleur appropriée
        String displayName = playerName;

        // Vérifier si le joueur a la permission d'inviter (en ligne ou hors ligne)
        boolean canInvite = hasInvitePermission(streamer.getServer(), playerUuid);

        // Priorité : permission d'inviter > modérateur
        if (canInvite) {
            displayName = "§d" + playerName + "§f"; // Violet pour ceux qui peuvent inviter
        } else if (moderatorManager.isModeratorOfAnyone(playerUuid)) {
            displayName = "§e" + playerName + "§f"; // Jaune pour les modérateurs
        }

        streamer.sendMessage(Text.literal("§f" + prefix + displayName), false);

        List<UUID> invitedPlayers = allInvitations.getOrDefault(playerUuid, new ArrayList<>());

        if (!invitedPlayers.isEmpty()) {
            for (int i = 0; i < invitedPlayers.size(); i++) {
                UUID invitedUuid = invitedPlayers.get(i);
                boolean isLast = (i == invitedPlayers.size() - 1);

                String childPrefix = prefix + (isLast ? "└── " : "├── ");
                String nextPrefix = prefix + (isLast ? "    " : "│   ");

                // Afficher le joueur avec le bon symbole
                var childProfileOpt = userCache.getByUuid(invitedUuid);
                String childPlayerName = childProfileOpt.map(profile -> profile.getName()).orElse("Unknown Player");

                // Vérifier les statuts de l'enfant (en ligne ou hors ligne)
                String childDisplayName = childPlayerName;
                boolean childCanInvite = hasInvitePermission(streamer.getServer(), invitedUuid);

                // Priorité : permission d'inviter > modérateur
                if (childCanInvite) {
                    childDisplayName = "§d" + childPlayerName + "§f"; // Violet pour ceux qui peuvent inviter
                } else if (moderatorManager.isModeratorOfAnyone(invitedUuid)) {
                    childDisplayName = "§e" + childPlayerName + "§f"; // Jaune pour les modérateurs
                }

                streamer.sendMessage(Text.literal("§f" + childPrefix + childDisplayName), false);

                // Continuer la récursion avec le bon préfixe d'indentation
                List<UUID> grandChildren = allInvitations.getOrDefault(invitedUuid, new ArrayList<>());
                if (!grandChildren.isEmpty()) {
                    for (int j = 0; j < grandChildren.size(); j++) {
                        UUID grandChildUuid = grandChildren.get(j);
                        boolean isLastGrandChild = (j == grandChildren.size() - 1);

                        displayPlayerTree(streamer, userCache, allInvitations, grandChildUuid,
                                        nextPrefix + (isLastGrandChild ? "└── " : "├── "), false);
                    }
                }
            }
        }
    }

    private static void displayConsoleTree(ServerCommandSource source,
                                          net.minecraft.util.UserCache userCache,
                                          Map<UUID, List<UUID>> allInvitations,
                                          UUID playerUuid,
                                          String prefix,
                                          boolean isRoot) {
        var profileOpt = userCache.getByUuid(playerUuid);
        String playerName = profileOpt.map(profile -> profile.getName()).orElse("Unknown Player");

        // Vérifier les statuts du joueur et ajouter les symboles appropriés en console
        String displayName = playerName;

        // Vérifier si le joueur a la permission d'inviter (en ligne ou hors ligne)
        boolean canInvite = hasInvitePermission(source.getServer(), playerUuid);

        // Priorité : permission d'inviter > modérateur
        if (canInvite) {
            displayName = "♚ " + playerName; // Symbole roi pour ceux qui peuvent inviter
        } else if (moderatorManager.isModeratorOfAnyone(playerUuid)) {
            displayName = "♟ " + playerName; // Symbole pion pour les modérateurs
        }

        final String finalDisplayName = displayName;
        source.sendFeedback(() -> Text.literal(prefix + finalDisplayName), false);

        List<UUID> invitedPlayers = allInvitations.getOrDefault(playerUuid, new ArrayList<>());

        if (!invitedPlayers.isEmpty()) {
            for (int i = 0; i < invitedPlayers.size(); i++) {
                UUID invitedUuid = invitedPlayers.get(i);
                boolean isLast = (i == invitedPlayers.size() - 1);

                String childPrefix = prefix + (isLast ? "└── " : "├── ");
                String nextPrefix = prefix + (isLast ? "    " : "│   ");

                // Afficher le joueur avec le bon symbole
                var childProfileOpt = userCache.getByUuid(invitedUuid);
                String childPlayerName = childProfileOpt.map(profile -> profile.getName()).orElse("Unknown Player");

                // Vérifier les statuts de l'enfant (en ligne ou hors ligne)
                String childDisplayName = childPlayerName;
                boolean childCanInvite = hasInvitePermission(source.getServer(), invitedUuid);

                // Priorité : permission d'inviter > modérateur
                if (childCanInvite) {
                    childDisplayName = "♚ " + childPlayerName; // Symbole roi pour ceux qui peuvent inviter
                } else if (moderatorManager.isModeratorOfAnyone(invitedUuid)) {
                    childDisplayName = "♟ " + childPlayerName; // Symbole pion pour les modérateurs
                }

                final String finalChildDisplayName = childDisplayName;
                source.sendFeedback(() -> Text.literal(childPrefix + finalChildDisplayName), false);

                // Continuer la récursion avec le bon préfixe d'indentation
                List<UUID> grandChildren = allInvitations.getOrDefault(invitedUuid, new ArrayList<>());
                if (!grandChildren.isEmpty()) {
                    for (int j = 0; j < grandChildren.size(); j++) {
                        UUID grandChildUuid = grandChildren.get(j);
                        boolean isLastGrandChild = (j == grandChildren.size() - 1);

                        displayConsoleTree(source, userCache, allInvitations, grandChildUuid,
                                        nextPrefix + (isLastGrandChild ? "└── " : "├── "), false);
                    }
                }
            }
        }
    }

    /**
     * Vérifie si un joueur a la permission d'inviter, même s'il n'est pas connecté
     * @param server Le serveur Minecraft
     * @param playerUuid L'UUID du joueur à vérifier
     * @return true si le joueur a la permission communityleaders.invite
     */
    private static boolean hasInvitePermission(MinecraftServer server, UUID playerUuid) {
        try {
            // D'abord vérifier si le joueur est en ligne
            ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(playerUuid);
            if (onlinePlayer != null) {
                // Si le joueur est en ligne, utiliser la méthode normale
                return PermissionUtils.hasPermission(onlinePlayer.getCommandSource(), "communityleaders.invite");
            }

            // Si le joueur est hors ligne, utiliser LuckPerms directement
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().loadUser(playerUuid).get();
            if (user != null) {
                return user.getCachedData().getPermissionData().checkPermission("communityleaders.invite").asBoolean();
            }

            return false;
        } catch (Exception e) {
            // En cas d'erreur, considérer que le joueur n'a pas la permission
            return false;
        }
    }
}
