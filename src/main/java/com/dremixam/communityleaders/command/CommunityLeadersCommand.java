package com.dremixam.communityleaders.command;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.InvitationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main Community Leaders command with all subcommands.
 * Supports /communityleaders and configurable alias (default: /cl)
 */
public class CommunityLeadersCommand {
    private static InvitationManager invitationManager;
    private static ConfigManager configManager;

    /**
     * Registers the main command and its alias.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
                               CommandManager.RegistrationEnvironment environment, InvitationManager invManager, ConfigManager confManager) {
        invitationManager = invManager;
        configManager = confManager;

        // Register main command
        registerCommunityleadersCommand(dispatcher);

        // Register alias if configured
        String alias = configManager.getCommandAlias();
        if (alias != null && !alias.trim().isEmpty()) {
            registerAliasCommand(dispatcher, alias.trim());
        }
    }

    private static void registerCommunityleadersCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("communityleaders")
                .requires(source -> hasAnyPermission(source))
                .executes(CommunityLeadersCommand::executeHelp)
                .then(CommandManager.literal("invite")
                        .requires(source -> hasPermission(source, "communityleaders.invite"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(CommunityLeadersCommand::executeInvite)))
                .then(CommandManager.literal("uninvite")
                        .requires(source -> hasPermission(source, "communityleaders.uninvite"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(CommunityLeadersCommand::executeUninvite)))
                .then(CommandManager.literal("ban")
                        .requires(source -> hasPermission(source, "communityleaders.ban"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(CommunityLeadersCommand::executeBan)))
                .then(CommandManager.literal("list")
                        .requires(source -> hasPermission(source, "communityleaders.list"))
                        .executes(CommunityLeadersCommand::executeList))
                .then(CommandManager.literal("tree")
                        .requires(source -> hasPermission(source, "communityleaders.tree"))
                        .executes(CommunityLeadersCommand::executeTree)));
    }

    private static void registerAliasCommand(CommandDispatcher<ServerCommandSource> dispatcher, String alias) {
        dispatcher.register(CommandManager.literal(alias)
                .requires(source -> hasAnyPermission(source))
                .executes(CommunityLeadersCommand::executeHelp)
                .then(CommandManager.literal("invite")
                        .requires(source -> hasPermission(source, "communityleaders.invite"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(CommunityLeadersCommand::executeInvite)))
                .then(CommandManager.literal("uninvite")
                        .requires(source -> hasPermission(source, "communityleaders.uninvite"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(CommunityLeadersCommand::executeUninvite)))
                .then(CommandManager.literal("ban")
                        .requires(source -> hasPermission(source, "communityleaders.ban"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(CommunityLeadersCommand::executeBan)))
                .then(CommandManager.literal("list")
                        .requires(source -> hasPermission(source, "communityleaders.list"))
                        .executes(CommunityLeadersCommand::executeList))
                .then(CommandManager.literal("tree")
                        .requires(source -> hasPermission(source, "communityleaders.tree"))
                        .executes(CommunityLeadersCommand::executeTree)));
    }

    private static boolean hasAnyPermission(ServerCommandSource source) {
        // Console et OP peuvent toujours utiliser les commandes
        if (!source.isExecutedByPlayer() || source.hasPermissionLevel(4)) {
            return true;
        }

        return hasPermission(source, "communityleaders.invite") ||
               hasPermission(source, "communityleaders.uninvite") ||
               hasPermission(source, "communityleaders.ban") ||
               hasPermission(source, "communityleaders.list") ||
               hasPermission(source, "communityleaders.tree");
    }

    private static boolean hasPermission(ServerCommandSource source, String permission) {
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
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    // Help command - shows available subcommands
    private static int executeHelp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        if (source.isExecutedByPlayer()) {
            ServerPlayerEntity player = source.getPlayer();
            player.sendMessage(Text.literal("§a" + configManager.getMessage("help_title")), false);

            if (hasPermission(source, "communityleaders.invite")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_invite")), false);
            }
            if (hasPermission(source, "communityleaders.uninvite")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_uninvite")), false);
            }
            if (hasPermission(source, "communityleaders.ban")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_ban")), false);
            }
            if (hasPermission(source, "communityleaders.list")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_list")), false);
            }
            if (hasPermission(source, "communityleaders.tree")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_tree")), false);
            }
        } else {
            // Console
            source.sendFeedback(() -> Text.literal("§a" + configManager.getMessage("help_title")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_invite")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_uninvite")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_ban")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_list")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_tree")), false);
        }

        return 1;
    }

    // Invite subcommand
    private static int executeInvite(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        // Vérifier si la commande est exécutée depuis la console
        if (!source.isExecutedByPlayer()) {
            source.sendFeedback(() -> Text.literal("§c" + configManager.getMessage("console_only_players")), false);
            return 0;
        }

        ServerPlayerEntity streamer = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                streamer.sendMessage(Text.literal("§c" + configManager.getMessage("player_not_found", playerName)), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            if (whitelist.isAllowed(targetProfile)) {
                streamer.sendMessage(Text.literal("§c" + configManager.getMessage("invite_already_whitelisted", playerName)), false);
                return 0;
            }

            // Vérifier la limite d'invitations (sauf si le joueur a la permission d'override)
            boolean hasUnlimitedPermission = hasPermission(source, "communityleaders.unlimited");
            if (!hasUnlimitedPermission) {
                int maxInvitations = configManager.getMaxInvitationsPerLeader();
                if (maxInvitations != -1) { // -1 = illimité, sinon limite appliquée
                    int currentInvitations = invitationManager.getInvitedPlayers(streamer.getUuid()).size();
                    if (currentInvitations >= maxInvitations) {
                        String limitMessage = configManager.getMessage("invite_limit_reached")
                                .replace("%limit%", String.valueOf(maxInvitations));
                        streamer.sendMessage(Text.literal("§c" + limitMessage), false);
                        return 0;
                    }
                }
            }

            WhitelistEntry entry = new WhitelistEntry(targetProfile);
            whitelist.add(entry);

            invitationManager.addInvitation(streamer.getUuid(), targetProfile.getId());

            streamer.sendMessage(Text.literal("§a" + configManager.getMessage("invite_success", playerName)), false);
            return 1;
        } catch (Exception e) {
            streamer.sendMessage(Text.literal("§c" + configManager.getMessage("invite_error").replace("%error%", e.getMessage())), false);
            return 0;
        }
    }

    // Uninvite subcommand
    private static int executeUninvite(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        // Vérifier si la commande est exécutée depuis la console
        if (!source.isExecutedByPlayer()) {
            source.sendFeedback(() -> Text.literal("§c" + configManager.getMessage("console_only_players")), false);
            return 0;
        }

        ServerPlayerEntity streamer = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                streamer.sendMessage(Text.literal("§c" + configManager.getMessage("player_not_found", playerName)), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            if (!invitationManager.hasInvited(streamer.getUuid(), targetProfile.getId())) {
                streamer.sendMessage(Text.literal("§c" + configManager.getMessage("uninvite_only_invited")), false);
                return 0;
            }

            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.networkHandler.disconnect(Text.literal(configManager.getMessage("uninvite_disconnect_message")));
            }

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            whitelist.remove(targetProfile);

            invitationManager.removeInvitation(streamer.getUuid(), targetProfile.getId());

            streamer.sendMessage(Text.literal("§a" + configManager.getMessage("uninvite_success", playerName)), false);
            return 1;
        } catch (Exception e) {
            streamer.sendMessage(Text.literal("§c" + configManager.getMessage("uninvite_error").replace("%error%", e.getMessage())), false);
            return 0;
        }
    }

    // Ban subcommand
    private static int executeBan(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        // Vérifier si la commande est exécutée depuis la console
        if (!source.isExecutedByPlayer()) {
            source.sendFeedback(() -> Text.literal("§c" + configManager.getMessage("console_only_players")), false);
            return 0;
        }

        ServerPlayerEntity streamer = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                streamer.sendMessage(Text.literal("§c" + configManager.getMessage("player_not_found", playerName)), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            if (!invitationManager.hasInvited(streamer.getUuid(), targetProfile.getId())) {
                streamer.sendMessage(Text.literal("§c" + configManager.getMessage("ban_only_invited")), false);
                return 0;
            }

            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.networkHandler.disconnect(Text.literal(configManager.getMessage("ban_disconnect_message")));
            }

            var bannedPlayerList = server.getPlayerManager().getUserBanList();
            var banEntry = new net.minecraft.server.BannedPlayerEntry(targetProfile, null, streamer.getName().getString(), null, configManager.getMessage("ban_reason"));
            bannedPlayerList.add(banEntry);

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            whitelist.remove(targetProfile);

            invitationManager.removeInvitation(streamer.getUuid(), targetProfile.getId());

            streamer.sendMessage(Text.literal("§a" + configManager.getMessage("ban_success", playerName)), false);
            return 1;
        } catch (Exception e) {
            streamer.sendMessage(Text.literal("§c" + configManager.getMessage("ban_error").replace("%error%", e.getMessage())), false);
            return 0;
        }
    }

    // List subcommand
    private static int executeList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        // Vérifier si la commande est exécutée depuis la console
        if (!source.isExecutedByPlayer()) {
            source.sendFeedback(() -> Text.literal("§c" + configManager.getMessage("console_only_players")), false);
            return 0;
        }

        ServerPlayerEntity streamer = source.getPlayer();

        try {
            List<UUID> invitedPlayers = invitationManager.getInvitedPlayers(streamer.getUuid());
            int currentInvitations = invitedPlayers.size();

            // Construire le titre avec le ratio d'invitations
            String title = configManager.getMessage("list_title");

            // Ajouter le ratio si le joueur n'a pas la permission unlimited
            boolean hasUnlimitedPermission = hasPermission(source, "communityleaders.unlimited");
            if (!hasUnlimitedPermission) {
                int maxInvitations = configManager.getMaxInvitationsPerLeader();
                if (maxInvitations != -1) { // -1 = illimité, sinon limite appliquée
                    title += " (" + currentInvitations + "/" + maxInvitations + ")";
                } else {
                    title += " (" + currentInvitations + "/∞)";
                }
            } else {
                title += " (" + currentInvitations + "/∞)";
            }

            if (invitedPlayers.isEmpty()) {
                streamer.sendMessage(Text.literal("§e" + configManager.getMessage("list_empty")), false);
                return 0;
            }

            streamer.sendMessage(Text.literal("§a" + title), false);

            var server = source.getServer();
            var userCache = server.getUserCache();

            for (UUID invitedUuid : invitedPlayers) {
                var profileOpt = userCache.getByUuid(invitedUuid);
                String playerName = profileOpt.map(profile -> profile.getName()).orElse("Unknown Player");

                String entry = configManager.getMessage("list_entry").replace("%player%", playerName);
                streamer.sendMessage(Text.literal("§f" + entry), false);
            }

            return 1;
        } catch (Exception e) {
            streamer.sendMessage(Text.literal("§c" + "Error displaying list: " + e.getMessage()), false);
            return 0;
        }
    }

    // Tree subcommand
    private static int executeTree(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
