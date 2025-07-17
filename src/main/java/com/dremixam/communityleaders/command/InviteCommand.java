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
import java.util.Optional;

/**
 * Command to invite players to the server.
 * Only streamers with the correct permission can use this command.
 */
public class InviteCommand {
    private static InvitationManager invitationManager;
    private static ConfigManager configManager;

    /**
     * Registers the invite command.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
                               CommandManager.RegistrationEnvironment environment, InvitationManager invManager, ConfigManager confManager) {
        invitationManager = invManager;
        configManager = confManager;

        dispatcher.register(CommandManager.literal("invite")
                .requires(source -> {
                    if (source.isExecutedByPlayer()) {
                        try {
                            ServerPlayerEntity player = source.getPlayer();
                            LuckPerms luckPerms = LuckPermsProvider.get();
                            User user = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);
                            return user.getCachedData().getPermissionData().checkPermission("communityleaders.invite").asBoolean();
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return false;
                })
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(InviteCommand::execute)));
    }

    /**
     * Executes the invite command logic.
     */
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity streamer = context.getSource().getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            // Get the server and user cache
            var server = context.getSource().getServer();
            var userCache = server.getUserCache();

            // Try to find the player profile
            Optional<GameProfile> profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                streamer.sendMessage(Text.literal("§c" + configManager.getMessage("player_not_found", playerName)), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            // Check if player is already whitelisted
            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            if (whitelist.isAllowed(targetProfile)) {
                streamer.sendMessage(Text.literal("§c" + configManager.getMessage("invite_already_whitelisted", playerName)), false);
                return 0;
            }

            // Add to whitelist
            WhitelistEntry entry = new WhitelistEntry(targetProfile);
            whitelist.add(entry);

            // Record the invitation relationship
            invitationManager.addInvitation(streamer.getUuid(), targetProfile.getId());

            // Success message
            streamer.sendMessage(Text.literal("§a" + configManager.getMessage("invite_success", playerName)), false);

            return 1;
        } catch (Exception e) {
            streamer.sendMessage(Text.literal("§c" + configManager.getMessage("invite_error").replace("%error%", e.getMessage())), false);
            return 0;
        }
    }
}
