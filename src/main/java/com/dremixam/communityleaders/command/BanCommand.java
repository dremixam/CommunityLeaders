package com.dremixam.communityleaders.command;

import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.config.ConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.Whitelist;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.BannedPlayerEntry;

import java.util.Optional;
import java.util.Date;

public class BanCommand {
    private static InvitationManager aInvitationManager;
    private static ConfigManager aConfigManager;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment, InvitationManager invitationManager, ConfigManager configManager) {
        aInvitationManager = invitationManager;
        aConfigManager = configManager;
        dispatcher.register(CommandManager.literal("kickban")
                .requires(source -> {
                    if (source.isExecutedByPlayer()) {
                        try {
                            ServerPlayerEntity player = source.getPlayer();
                            LuckPerms luckPerms = LuckPermsProvider.get();
                            User user = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);
                            return user.getCachedData().getPermissionData().checkPermission("communityleaders.ban").asBoolean();
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return false;
                })
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(BanCommand::run)));
    }

    private static int run(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity streamer = context.getSource().getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        Optional<GameProfile> gameProfileOpt = context.getSource().getServer().getUserCache().findByName(playerName);

        if (gameProfileOpt.isEmpty()) {
            context.getSource().sendError(Text.literal(aConfigManager.getMessage("player_not_found", playerName)));
            return 0;
        }

        GameProfile gameProfile = gameProfileOpt.get();

        if (!aInvitationManager.aInvite(streamer.getUuid(), gameProfile.getId())) {
            context.getSource().sendError(Text.literal(aConfigManager.getMessage("not_your_invite")));
            return 0;
        }

        BannedPlayerList bannedPlayerList = context.getSource().getServer().getPlayerManager().getUserBanList();
        if (bannedPlayerList.contains(gameProfile)) {
            context.getSource().sendFeedback(() -> Text.literal(aConfigManager.getMessage("already_banned")), false);
            return 0;
        }

        BannedPlayerEntry banEntry = new BannedPlayerEntry(gameProfile, new Date(), streamer.getName().getString(), null, aConfigManager.getMessage("ban_reason"));
        bannedPlayerList.add(banEntry);

        Whitelist whitelist = context.getSource().getServer().getPlayerManager().getWhitelist();
        if (whitelist.isAllowed(gameProfile)) {
            whitelist.remove(gameProfile);
        }

        aInvitationManager.retirerInvitation(streamer.getUuid(), gameProfile.getId());

        ServerPlayerEntity playerToBan = context.getSource().getServer().getPlayerManager().getPlayer(gameProfile.getId());
        if (playerToBan != null) {
            playerToBan.networkHandler.disconnect(Text.literal(aConfigManager.getMessage("ban_reason")));
        }

        context.getSource().sendFeedback(() -> Text.literal(aConfigManager.getMessage("ban_success", playerName)), true);

        return 1;
    }
}
