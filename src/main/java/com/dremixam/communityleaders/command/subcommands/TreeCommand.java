package com.dremixam.communityleaders.command.subcommands;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
                    source.sendFeedback(() -> Text.literal("§e" + "No invitations found."), false);
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
                source.sendFeedback(() -> Text.literal("§a" + configManager.getMessage("tree_title")), false);

                Set<UUID> allInviters = allInvitations.keySet();
                Set<UUID> allInvited = allInvitations.values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toSet());

                Set<UUID> rootPlayers = allInviters.stream()
                        .filter(inviter -> !allInvited.contains(inviter))
                        .collect(Collectors.toSet());

                if (rootPlayers.isEmpty()) {
                    source.sendFeedback(() -> Text.literal("§e" + "No root players found (circular invitations detected)."), false);
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
                source.sendFeedback(() -> Text.literal("§c" + "Error displaying tree: " + e.getMessage()), false);
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

        streamer.sendMessage(Text.literal("§f" + prefix + playerName), false);

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
                streamer.sendMessage(Text.literal("§f" + childPrefix + childPlayerName), false);

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

        source.sendFeedback(() -> Text.literal("§f" + prefix + playerName), false);

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
                source.sendFeedback(() -> Text.literal("§f" + childPrefix + childPlayerName), false);

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
}
