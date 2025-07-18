package com.dremixam.communityleaders.command;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
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
    private static ModeratorManager moderatorManager;

    /**
     * Registers the main command and its alias.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
                               CommandManager.RegistrationEnvironment environment, InvitationManager invManager, ConfigManager confManager, ModeratorManager modManager) {
        invitationManager = invManager;
        configManager = confManager;
        moderatorManager = modManager;

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
                        .executes(CommunityLeadersCommand::executeTree))
                .then(CommandManager.literal("mod")
                        .requires(source -> hasPermission(source, "communityleaders.moderator"))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(CommunityLeadersCommand::executeModAdd)))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(CommunityLeadersCommand::executeModRemove)))
                        .then(CommandManager.literal("list")
                                .executes(CommunityLeadersCommand::executeModList))));
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
                        .executes(CommunityLeadersCommand::executeTree))
                .then(CommandManager.literal("mod")
                        .requires(source -> hasPermission(source, "communityleaders.moderator", false))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(CommunityLeadersCommand::executeModAdd)))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(CommunityLeadersCommand::executeModRemove)))
                        .then(CommandManager.literal("list")
                                .executes(CommunityLeadersCommand::executeModList))));
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
        return hasPermission(source, permission, true);
    }

    private static boolean hasPermission(ServerCommandSource source, String permission, boolean inheritPermissions) {
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

        ServerPlayerEntity player = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            // Déterminer qui est le vrai leader (le joueur ou son leader si c'est un modérateur)
            UUID actualLeaderUuid = player.getUuid();
            String leaderName = player.getName().getString();

            UUID leaderUuid = moderatorManager.getLeader(player.getUuid());
            if (leaderUuid != null) {
                // Le joueur est modérateur, utiliser son leader
                actualLeaderUuid = leaderUuid;
                var leaderProfileOpt = userCache.getByUuid(leaderUuid);
                leaderName = leaderProfileOpt.map(profile -> profile.getName()).orElse("Leader");
            }

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                player.sendMessage(Text.literal("§c" + configManager.getMessage("player_not_found", playerName)), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            if (whitelist.isAllowed(targetProfile)) {
                player.sendMessage(Text.literal("§c" + configManager.getMessage("invite_already_whitelisted", playerName)), false);
                return 0;
            }

            // Vérifier la limite d'invitations en utilisant le vrai leader
            boolean hasUnlimitedPermission = hasPermission(source, "communityleaders.unlimited");
            if (!hasUnlimitedPermission) {
                int maxInvitations = configManager.getMaxInvitationsPerLeader();
                if (maxInvitations != -1) { // -1 = illimité, sinon limite appliquée
                    int currentInvitations = invitationManager.getInvitedPlayers(actualLeaderUuid).size();
                    if (currentInvitations >= maxInvitations) {
                        String limitMessage = configManager.getMessage("invite_limit_reached")
                                .replace("%limit%", String.valueOf(maxInvitations));
                        player.sendMessage(Text.literal("§c" + limitMessage), false);
                        return 0;
                    }
                }
            }

            WhitelistEntry entry = new WhitelistEntry(targetProfile);
            whitelist.add(entry);

            invitationManager.addInvitation(actualLeaderUuid, targetProfile.getId());

            player.sendMessage(Text.literal("§a" + configManager.getMessage("invite_success", playerName)), false);
            return 1;
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c" + configManager.getMessage("invite_error").replace("%error%", e.getMessage())), false);
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

        ServerPlayerEntity player = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            // Déterminer qui est le vrai leader (le joueur ou son leader si c'est un modérateur)
            UUID actualLeaderUuid = player.getUuid();

            UUID leaderUuid = moderatorManager.getLeader(player.getUuid());
            if (leaderUuid != null) {
                // Le joueur est modérateur, utiliser son leader
                actualLeaderUuid = leaderUuid;
            }

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                player.sendMessage(Text.literal("§c" + configManager.getMessage("player_not_found", playerName)), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            if (!invitationManager.hasInvited(actualLeaderUuid, targetProfile.getId())) {
                player.sendMessage(Text.literal("§c" + configManager.getMessage("uninvite_only_invited")), false);
                return 0;
            }

            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.networkHandler.disconnect(Text.literal(configManager.getMessage("uninvite_disconnect_message")));
            }

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            whitelist.remove(targetProfile);

            invitationManager.removeInvitation(actualLeaderUuid, targetProfile.getId());

            player.sendMessage(Text.literal("§a" + configManager.getMessage("uninvite_success", playerName)), false);
            return 1;
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c" + configManager.getMessage("uninvite_error").replace("%error%", e.getMessage())), false);
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

        ServerPlayerEntity player = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            // Déterminer qui est le vrai leader (le joueur ou son leader si c'est un modérateur)
            UUID actualLeaderUuid = player.getUuid();

            UUID leaderUuid = moderatorManager.getLeader(player.getUuid());
            if (leaderUuid != null) {
                // Le joueur est modérateur, utiliser son leader
                actualLeaderUuid = leaderUuid;
            }

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                player.sendMessage(Text.literal("§c" + configManager.getMessage("player_not_found", playerName)), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            if (!invitationManager.hasInvited(actualLeaderUuid, targetProfile.getId())) {
                player.sendMessage(Text.literal("§c" + configManager.getMessage("ban_only_invited")), false);
                return 0;
            }

            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.networkHandler.disconnect(Text.literal(configManager.getMessage("ban_disconnect_message")));
            }

            var bannedPlayerList = server.getPlayerManager().getUserBanList();
            var banEntry = new net.minecraft.server.BannedPlayerEntry(targetProfile, null, player.getName().getString(), null, configManager.getMessage("ban_reason"));
            bannedPlayerList.add(banEntry);

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            whitelist.remove(targetProfile);

            invitationManager.removeInvitation(actualLeaderUuid, targetProfile.getId());

            player.sendMessage(Text.literal("§a" + configManager.getMessage("ban_success", playerName)), false);
            return 1;
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c" + configManager.getMessage("ban_error").replace("%error%", e.getMessage())), false);
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

        ServerPlayerEntity player = source.getPlayer();

        try {
            // Déterminer qui est le vrai leader (le joueur ou son leader si c'est un modérateur)
            UUID actualLeaderUuid = player.getUuid();
            String leaderName = player.getName().getString();

            UUID leaderUuid = moderatorManager.getLeader(player.getUuid());
            if (leaderUuid != null) {
                // Le joueur est modérateur, utiliser son leader
                actualLeaderUuid = leaderUuid;
                var server = source.getServer();
                var userCache = server.getUserCache();
                var leaderProfileOpt = userCache.getByUuid(leaderUuid);
                leaderName = leaderProfileOpt.map(profile -> profile.getName()).orElse("Leader");
            }

            List<UUID> invitedPlayers = invitationManager.getInvitedPlayers(actualLeaderUuid);
            int currentInvitations = invitedPlayers.size();

            // Construire le titre avec le ratio d'invitations
            String title = configManager.getMessage("list_title", leaderName);

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
                player.sendMessage(Text.literal("§e" + configManager.getMessage("list_empty")), false);
                return 0;
            }

            player.sendMessage(Text.literal("§a" + title), false);

            var server = source.getServer();
            var userCache = server.getUserCache();

            for (UUID invitedUuid : invitedPlayers) {
                var profileOpt = userCache.getByUuid(invitedUuid);
                String playerName = profileOpt.map(profile -> profile.getName()).orElse("Unknown Player");

                String entry = configManager.getMessage("list_entry", playerName);
                player.sendMessage(Text.literal("§f" + entry), false);
            }

            return 1;
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c" + "Error displaying list: " + e.getMessage()), false);
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

    // Moderator add subcommand - Ajoute un modérateur (seulement parmi les joueurs invités)
    private static int executeModAdd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendFeedback(() -> Text.literal("§cCette commande ne peut être utilisée que par des joueurs."), false);
            return 0;
        }

        ServerPlayerEntity leader = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                leader.sendMessage(Text.literal("§cJoueur non trouvé: " + playerName), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            // Vérifier que le joueur ne peut pas se nommer lui-même modérateur
            if (targetProfile.getId().equals(leader.getUuid())) {
                leader.sendMessage(Text.literal("§cVous ne pouvez pas vous nommer modérateur de vous-même."), false);
                return 0;
            }

            // RÈGLE IMPORTANTE: Seuls les joueurs invités par ce leader peuvent devenir modérateurs
            if (!invitationManager.hasInvited(leader.getUuid(), targetProfile.getId())) {
                leader.sendMessage(Text.literal("§cVous ne pouvez nommer modérateur que les joueurs que vous avez invités."), false);
                return 0;
            }

            // Vérifier si le joueur est déjà modérateur de ce leader
            if (moderatorManager.isModerator(leader.getUuid(), targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + playerName + " est déjà votre modérateur."), false);
                return 0;
            }

            // Vérifier si le joueur est modérateur de quelqu'un d'autre
            if (moderatorManager.isModeratorOfAnyone(targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + playerName + " est déjà modérateur d'un autre leader."), false);
                return 0;
            }

            moderatorManager.addModerator(leader.getUuid(), targetProfile.getId());
            leader.sendMessage(Text.literal("§a" + playerName + " a été ajouté comme modérateur."), false);

            // Notifier le joueur s'il est en ligne et rafraîchir ses commandes
            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.sendMessage(Text.literal("§aVous avez été nommé modérateur par " + leader.getName().getString() + " !"), false);
                targetPlayer.sendMessage(Text.literal("§eVous avez maintenant accès à ses commandes de leader."), false);

                // Forcer le rafraîchissement des commandes côté client
                server.getCommandManager().sendCommandTree(targetPlayer);
            }

            return 1;
        } catch (Exception e) {
            leader.sendMessage(Text.literal("§cErreur lors de l'ajout du modérateur: " + e.getMessage()), false);
            return 0;
        }
    }

    // Moderator remove subcommand
    private static int executeModRemove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendFeedback(() -> Text.literal("§cCette commande ne peut être utilisée que par des joueurs."), false);
            return 0;
        }

        ServerPlayerEntity leader = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                leader.sendMessage(Text.literal("§cJoueur non trouvé: " + playerName), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            if (!moderatorManager.isModerator(leader.getUuid(), targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + playerName + " n'est pas votre modérateur."), false);
                return 0;
            }

            moderatorManager.removeModerator(leader.getUuid(), targetProfile.getId());
            leader.sendMessage(Text.literal("§a" + playerName + " a été retiré de vos modérateurs."), false);

            // Notifier le joueur s'il est en ligne et rafraîchir ses commandes
            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.sendMessage(Text.literal("§eVous n'êtes plus modérateur de " + leader.getName().getString() + "."), false);
                targetPlayer.sendMessage(Text.literal("§eVos permissions de modérateur ont été révoquées."), false);

                // Forcer le rafraîchissement des commandes côté client
                server.getCommandManager().sendCommandTree(targetPlayer);
            }

            return 1;
        } catch (Exception e) {
            leader.sendMessage(Text.literal("§cErreur lors de la suppression du modérateur: " + e.getMessage()), false);
            return 0;
        }
    }

    // Moderator list subcommand
    private static int executeModList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendFeedback(() -> Text.literal("§cCette commande ne peut être utilisée que par des joueurs."), false);
            return 0;
        }

        ServerPlayerEntity leader = source.getPlayer();

        try {
            List<UUID> moderators = moderatorManager.getModerators(leader.getUuid());

            if (moderators.isEmpty()) {
                leader.sendMessage(Text.literal("§eVous n'avez aucun modérateur."), false);
                return 0;
            }

            leader.sendMessage(Text.literal("§aVos modérateurs (" + moderators.size() + "):"), false);

            var server = source.getServer();
            var userCache = server.getUserCache();

            for (UUID modUuid : moderators) {
                var profileOpt = userCache.getByUuid(modUuid);
                String modName = profileOpt.map(profile -> profile.getName()).orElse("Unknown Player");
                leader.sendMessage(Text.literal("- " + modName), false);
            }

            return 1;
        } catch (Exception e) {
            leader.sendMessage(Text.literal("§cErreur lors de l'affichage des modérateurs: " + e.getMessage()), false);
            return 0;
        }
    }
}
