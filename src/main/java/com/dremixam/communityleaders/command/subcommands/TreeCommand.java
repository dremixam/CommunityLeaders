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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
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

            // Récupérer tous les joueurs whitelistés
            Set<UUID> allWhitelistedPlayers = getAllWhitelistedPlayers(server);

            if (source.isExecutedByPlayer()) {
                ServerPlayerEntity streamer = source.getPlayer();
                if (streamer != null) {
                    streamer.sendMessage(Text.literal("§a" + configManager.getMessage("tree_title")), false);

                    // Identifier tous les joueurs impliqués dans les invitations
                    Set<UUID> allInviters = allInvitations.keySet();
                    Set<UUID> allInvited = allInvitations.values().stream()
                            .flatMap(List::stream)
                            .collect(Collectors.toSet());

                    // Les root players sont ceux qui invitent mais ne sont pas invités
                    Set<UUID> rootPlayersWithInvitations = allInviters.stream()
                            .filter(inviter -> !allInvited.contains(inviter))
                            .collect(Collectors.toSet());

                    // Ajouter les joueurs whitelistés qui n'ont aucune invitation (ni donné ni reçu)
                    Set<UUID> isolatedPlayers = allWhitelistedPlayers.stream()
                            .filter(uuid -> !allInviters.contains(uuid) && !allInvited.contains(uuid))
                            .collect(Collectors.toSet());

                    // Combiner tous les joueurs racine
                    Set<UUID> allRootPlayers = new HashSet<>(rootPlayersWithInvitations);
                    allRootPlayers.addAll(isolatedPlayers);

                    if (allRootPlayers.isEmpty() && allInvitations.isEmpty()) {
                        streamer.sendMessage(Text.literal("§e" + "No whitelisted players found."), false);
                        return 0;
                    }

                    // Trier les joueurs racine par ordre alphabétique
                    List<UUID> sortedRootPlayers = allRootPlayers.stream()
                            .sorted((uuid1, uuid2) -> {
                                var profile1 = userCache.getByUuid(uuid1);
                                var profile2 = userCache.getByUuid(uuid2);
                                String name1 = profile1 != null ? profile1.map(profile -> profile.getName()).orElse("Unknown Player") : "Unknown Player";
                                String name2 = profile2 != null ? profile2.map(profile -> profile.getName()).orElse("Unknown Player") : "Unknown Player";
                                return name1.compareToIgnoreCase(name2);
                            })
                            .toList();

                    for (UUID rootPlayer : sortedRootPlayers) {
                        displayPlayerTree(streamer, userCache, allInvitations, rootPlayer, "");
                    }
                }
            } else {
                // Console version
                source.sendFeedback(() -> Text.literal(configManager.getMessage("tree_title")), false);

                // Identifier tous les joueurs impliqués dans les invitations
                Set<UUID> allInviters = allInvitations.keySet();
                Set<UUID> allInvited = allInvitations.values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toSet());

                // Les root players sont ceux qui invitent mais ne sont pas invités
                Set<UUID> rootPlayersWithInvitations = allInviters.stream()
                        .filter(inviter -> !allInvited.contains(inviter))
                        .collect(Collectors.toSet());

                // Ajouter les joueurs whitelistés qui n'ont aucune invitation (ni donné ni reçu)
                Set<UUID> isolatedPlayers = allWhitelistedPlayers.stream()
                        .filter(uuid -> !allInviters.contains(uuid) && !allInvited.contains(uuid))
                        .collect(Collectors.toSet());

                // Combiner tous les joueurs racine
                Set<UUID> allRootPlayers = new HashSet<>(rootPlayersWithInvitations);
                allRootPlayers.addAll(isolatedPlayers);

                if (allRootPlayers.isEmpty() && allInvitations.isEmpty()) {
                    source.sendFeedback(() -> Text.literal("No whitelisted players found."), false);
                    return 0;
                }

                // Trier les joueurs racine par ordre alphabétique
                List<UUID> sortedRootPlayers = allRootPlayers.stream()
                        .sorted((uuid1, uuid2) -> {
                            var profile1 = userCache.getByUuid(uuid1);
                            var profile2 = userCache.getByUuid(uuid2);
                            String name1 = profile1 != null ? profile1.map(profile -> profile.getName()).orElse("Unknown Player") : "Unknown Player";
                            String name2 = profile2 != null ? profile2.map(profile -> profile.getName()).orElse("Unknown Player") : "Unknown Player";
                            return name1.compareToIgnoreCase(name2);
                        })
                        .toList();

                for (UUID rootPlayer : sortedRootPlayers) {
                    displayConsoleTree(source, userCache, allInvitations, rootPlayer, "");
                }
            }

            return 1;
        } catch (Exception e) {
            if (source.isExecutedByPlayer() && source.getPlayer() != null) {
                source.getPlayer().sendMessage(Text.literal("§c" + "Error displaying tree: " + e.getMessage()), false);
            } else {
                source.sendFeedback(() -> Text.literal("Error displaying tree: " + e.getMessage()), false);
            }
            return 0;
        }
    }

    private static Set<UUID> getAllWhitelistedPlayers(MinecraftServer server) {
        Set<UUID> whitelistedPlayers = new HashSet<>();

        try {
            // Essayer de lire le fichier whitelist.json
            File whitelistFile = new File(server.getRunDirectory(), "whitelist.json");
            if (whitelistFile.exists()) {
                try (FileReader reader = new FileReader(whitelistFile)) {
                    JsonArray whitelistArray = JsonParser.parseReader(reader).getAsJsonArray();
                    for (JsonElement element : whitelistArray) {
                        JsonObject playerObj = element.getAsJsonObject();
                        String uuidString = playerObj.get("uuid").getAsString();
                        whitelistedPlayers.add(UUID.fromString(uuidString));
                    }
                }
            }
        } catch (Exception e) {
            // En cas d'erreur, retourner un set vide ou utiliser une méthode alternative
            System.err.println("Error reading whitelist file: " + e.getMessage());
        }

        return whitelistedPlayers;
    }

    private static void displayPlayerTree(ServerPlayerEntity streamer,
                                        net.minecraft.util.UserCache userCache,
                                        Map<UUID, List<UUID>> allInvitations,
                                        UUID playerUuid,
                                        String prefix) {
        var profileOpt = userCache.getByUuid(playerUuid);
        String playerName = profileOpt != null ? profileOpt.map(profile -> profile.getName()).orElse("Unknown Player") : "Unknown Player";

        // Vérifier les statuts du joueur et appliquer la couleur appropriée
        String displayName = playerName;

        // Vérifier si le joueur est opérateur
        boolean isOperator = isPlayerOperator(streamer.getServer(), playerUuid);

        // Vérifier si le joueur a la permission d'inviter (en ligne ou hors ligne)
        boolean canInvite = hasInvitePermission(streamer.getServer(), playerUuid);

        // Priorité : opérateur > permission d'inviter > modérateur
        if (isOperator) {
            displayName = "§c" + playerName + "§f"; // Rouge pour les opérateurs
        } else if (canInvite) {
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
                String childPlayerName = childProfileOpt != null ? childProfileOpt.map(profile -> profile.getName()).orElse("Unknown Player") : "Unknown Player";

                // Vérifier les statuts de l'enfant (en ligne ou hors ligne)
                String childDisplayName = childPlayerName;
                boolean childIsOperator = isPlayerOperator(streamer.getServer(), invitedUuid);
                boolean childCanInvite = hasInvitePermission(streamer.getServer(), invitedUuid);

                // Priorité : opérateur > permission d'inviter > modérateur
                if (childIsOperator) {
                    childDisplayName = "§c" + childPlayerName + "§f"; // Rouge pour les opérateurs
                } else if (childCanInvite) {
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
                                        nextPrefix + (isLastGrandChild ? "└── " : "├── "));
                    }
                }
            }
        }
    }

    private static void displayConsoleTree(ServerCommandSource source,
                                          net.minecraft.util.UserCache userCache,
                                          Map<UUID, List<UUID>> allInvitations,
                                          UUID playerUuid,
                                          String prefix) {
        var profileOpt = userCache.getByUuid(playerUuid);
        String playerName = profileOpt != null ? profileOpt.map(profile -> profile.getName()).orElse("Unknown Player") : "Unknown Player";

        // Vérifier les statuts du joueur et ajouter les symboles appropriés en console
        String displayName = playerName;

        // Vérifier si le joueur est opérateur
        boolean isOperator = isPlayerOperator(source.getServer(), playerUuid);

        // Vérifier si le joueur a la permission d'inviter (en ligne ou hors ligne)
        boolean canInvite = hasInvitePermission(source.getServer(), playerUuid);

        // Priorité : opérateur > permission d'inviter > modérateur
        if (isOperator) {
            displayName = "♛ " + playerName; // Symbole reine pour les opérateurs
        } else if (canInvite) {
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
                String childPlayerName = childProfileOpt != null ? childProfileOpt.map(profile -> profile.getName()).orElse("Unknown Player") : "Unknown Player";

                // Vérifier les statuts de l'enfant (en ligne ou hors ligne)
                String childDisplayName = childPlayerName;
                boolean childIsOperator = isPlayerOperator(source.getServer(), invitedUuid);
                boolean childCanInvite = hasInvitePermission(source.getServer(), invitedUuid);

                // Priorité : opérateur > permission d'inviter > modérateur
                if (childIsOperator) {
                    childDisplayName = "♛ " + childPlayerName; // Symbole reine pour les opérateurs
                } else if (childCanInvite) {
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
                                        nextPrefix + (isLastGrandChild ? "└── " : "├── "));
                    }
                }
            }
        }
    }

    /**
     * Vérifie si un joueur est opérateur du serveur
     * @param server Le serveur Minecraft
     * @param playerUuid L'UUID du joueur à vérifier
     * @return true si le joueur est opérateur
     */
    private static boolean isPlayerOperator(MinecraftServer server, UUID playerUuid) {
        try {
            var userCache = server.getUserCache();
            var profileOpt = userCache.getByUuid(playerUuid);
            if (profileOpt != null && profileOpt.isPresent()) {
                return server.getPlayerManager().isOperator(profileOpt.get());
            }
            return false;
        } catch (Exception e) {
            return false;
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
