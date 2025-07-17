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
import java.util.Optional;

public class UninviteCommand {
    private static InvitationManager invitationManager;
    private static ConfigManager configManager;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment, InvitationManager invManager, ConfigManager confManager) {
        invitationManager = invManager;
        configManager = confManager;
        dispatcher.register(CommandManager.literal("uninvite")
                .requires(source -> {
                    if (source.isExecutedByPlayer()) {
                        try {
                            ServerPlayerEntity player = source.getPlayer();
                            LuckPerms luckPerms = LuckPermsProvider.get();
                            User user = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);
                            return user.getCachedData().getPermissionData().checkPermission("communityleaders.uninvite").asBoolean();
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return false;
                })
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(UninviteCommand::run)));
    }

    private static int run(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity streamer = context.getSource().getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        Optional<GameProfile> gameProfileOpt = context.getSource().getServer().getUserCache().findByName(playerName);

        if (gameProfileOpt.isEmpty()) {
            context.getSource().sendError(Text.literal(configManager.getMessage("player_not_found", playerName)));
            return 0;
        }

        GameProfile gameProfile = gameProfileOpt.get();

        if (!invitationManager.hasInvited(streamer.getUuid(), gameProfile.getId())) {
            context.getSource().sendError(Text.literal(configManager.getMessage("not_your_invite")));
            return 0;
        }

        Whitelist whitelist = context.getSource().getServer().getPlayerManager().getWhitelist();
        if (whitelist.isAllowed(gameProfile)) {
            whitelist.remove(gameProfile);
        }

        invitationManager.removeInvitation(streamer.getUuid(), gameProfile.getId());

        context.getSource().sendFeedback(() -> Text.literal(configManager.getMessage("uninvite_success", playerName)), true);

        return 1;
    }
}
