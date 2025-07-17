package com.dremixam.communityleaders.command;

import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.config.ConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.Whitelist;
import com.mojang.authlib.GameProfile;
import java.util.Optional;

public class InviteCommand {
    private static InvitationManager aInvitationManager;
    private static ConfigManager aConfigManager;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment, InvitationManager invitationManager, ConfigManager configManager) {
        aInvitationManager = invitationManager;
        aConfigManager = configManager;

        // Commande simple /invite
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
                        .executes(InviteCommand::run)));
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity streamer = context.getSource().getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        Optional<GameProfile> gameProfileOpt = context.getSource().getServer().getUserCache().findByName(playerName);

        if (gameProfileOpt.isEmpty()) {
            context.getSource().sendError(Text.literal(aConfigManager.getMessage("player_not_found", playerName)));
            return 0;
        }

        GameProfile gameProfile = gameProfileOpt.get();
        Whitelist whitelist = context.getSource().getServer().getPlayerManager().getWhitelist();

        if (whitelist.isAllowed(gameProfile)) {
            context.getSource().sendFeedback(() -> Text.literal(aConfigManager.getMessage("invite_already_whitelisted")), false);
            return 0;
        }

        whitelist.add(new WhitelistEntry(gameProfile));
        aInvitationManager.ajouterInvitation(streamer.getUuid(), gameProfile.getId());

        context.getSource().sendFeedback(() -> Text.literal(aConfigManager.getMessage("invite_success", playerName)), true);

        return 1;
    }
}
