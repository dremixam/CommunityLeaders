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

import java.util.List;
import java.util.UUID;

/**
 * Command to list players that the streamer has invited.
 * Only streamers with the correct permission can use this command.
 */
public class ListCommand {
    private static InvitationManager invitationManager;
    private static ConfigManager configManager;

    /**
     * Registers the list command.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
                               CommandManager.RegistrationEnvironment environment, InvitationManager invManager, ConfigManager confManager) {
        invitationManager = invManager;
        configManager = confManager;

        dispatcher.register(CommandManager.literal("list")
                .requires(source -> {
                    if (source.isExecutedByPlayer()) {
                        try {
                            ServerPlayerEntity player = source.getPlayer();
                            LuckPerms luckPerms = LuckPermsProvider.get();
                            User user = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);
                            return user.getCachedData().getPermissionData().checkPermission("communityleaders.list").asBoolean();
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return false;
                })
                .executes(ListCommand::execute));
    }

    /**
     * Executes the list command logic.
     */
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity streamer = context.getSource().getPlayer();

        try {
            List<UUID> invitedPlayers = invitationManager.getInvitedPlayers(streamer.getUuid());

            if (invitedPlayers.isEmpty()) {
                streamer.sendMessage(Text.literal("§e" + configManager.getMessage("list_empty")), false);
                return 0;
            }

            // Title
            streamer.sendMessage(Text.literal("§a" + configManager.getMessage("list_title")), false);

            // Get server for user cache
            var server = context.getSource().getServer();
            var userCache = server.getUserCache();

            // List each invited player
            for (UUID invitedUuid : invitedPlayers) {
                var profileOpt = userCache.getByUuid(invitedUuid);
                String playerName = profileOpt.map(profile -> profile.getName()).orElse("Unknown Player");

                String entry = configManager.getMessage("list_entry", playerName);
                streamer.sendMessage(Text.literal("§f" + entry), false);
            }

            return 1;
        } catch (Exception e) {
            streamer.sendMessage(Text.literal("§c" + "Error displaying list: " + e.getMessage()), false);
            return 0;
        }
    }
}
