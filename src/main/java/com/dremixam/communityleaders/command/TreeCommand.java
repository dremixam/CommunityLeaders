package com.dremixam.communityleaders.command;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.InvitationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to display the invitation tree.
 * Shows the hierarchy of who invited whom.
 * Only streamers with the correct permission can use this command.
 */
public class TreeCommand {
    private static InvitationManager invitationManager;
    private static ConfigManager configManager;

    /**
     * Registers the tree command.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
                               CommandManager.RegistrationEnvironment environment, InvitationManager invManager, ConfigManager confManager) {
        invitationManager = invManager;
        configManager = confManager;

        dispatcher.register(CommandManager.literal("tree")
                .requires(source -> {
                    if (source.isExecutedByPlayer()) {
                        try {
                            ServerPlayerEntity player = source.getPlayer();
                            LuckPerms luckPerms = LuckPermsProvider.get();
                            User user = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);
                            return user.getCachedData().getPermissionData().checkPermission("communityleaders.tree").asBoolean();
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return false;
                })
                .executes(TreeCommand::execute));
    }

    /**
     * Executes the tree command logic.
     */
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity streamer = context.getSource().getPlayer();

        try {
            var server = context.getSource().getServer();
            var userCache = server.getUserCache();
            Map<UUID, List<UUID>> allInvitations = invitationManager.getAllInvitations();

            if (allInvitations.isEmpty()) {
                streamer.sendMessage(Text.literal("§e" + "No invitations found."), false);
                return 0;
            }

            // Title
            streamer.sendMessage(Text.literal("§a" + configManager.getMessage("tree_title")), false);
            streamer.sendMessage(Text.literal(""), false); // Empty line

            // Find root players (those who invited others but weren't invited themselves)
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

            // Display root players and their trees
            streamer.sendMessage(Text.literal("§6" + configManager.getMessage("tree_root")), false);

            for (UUID rootPlayer : rootPlayers) {
                displayPlayerTree(streamer, userCache, allInvitations, rootPlayer, "", true);
            }

            return 1;
        } catch (Exception e) {
            streamer.sendMessage(Text.literal("§c" + "Error displaying tree: " + e.getMessage()), false);
            return 0;
        }
    }

    /**
     * Recursively displays a player and their invited players in tree format.
     */
    private static void displayPlayerTree(ServerPlayerEntity streamer,
                                        net.minecraft.util.UserCache userCache,
                                        Map<UUID, List<UUID>> allInvitations,
                                        UUID playerUuid,
                                        String prefix,
                                        boolean isRoot) {
        // Get player name
        var profileOpt = userCache.getByUuid(playerUuid);
        String playerName = profileOpt.map(profile -> profile.getName()).orElse("Unknown Player");

        // Display current player
        if (isRoot) {
            streamer.sendMessage(Text.literal("§f" + playerName), false);
        } else {
            streamer.sendMessage(Text.literal("§f" + prefix + playerName), false);
        }

        // Get invited players
        List<UUID> invitedPlayers = allInvitations.getOrDefault(playerUuid, new ArrayList<>());

        if (!invitedPlayers.isEmpty()) {
            // Display each invited player recursively
            for (int i = 0; i < invitedPlayers.size(); i++) {
                UUID invitedUuid = invitedPlayers.get(i);
                boolean isLast = (i == invitedPlayers.size() - 1);

                String branch = isLast ? "└── " : "├── ";
                String newPrefix = prefix + (isLast ? "    " : "│   ");

                // Recursively display this invited player and their invitations
                displayPlayerTree(streamer, userCache, allInvitations, invitedUuid,
                        newPrefix + branch, false);
            }
        }
    }
}
