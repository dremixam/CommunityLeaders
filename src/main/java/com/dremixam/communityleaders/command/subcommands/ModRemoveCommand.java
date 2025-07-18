package com.dremixam.communityleaders.command.subcommands;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;

public class ModRemoveCommand {
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

        if (!source.isExecutedByPlayer()) {
            source.sendFeedback(() -> Text.literal("§c" + configManager.getMessage("console_only_players")), false);
            return 0;
        }

        ServerPlayerEntity leader = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                leader.sendMessage(Text.literal("§c" + configManager.getMessage("player_not_found", playerName)), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            if (!moderatorManager.isModerator(leader.getUuid(), targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + configManager.getMessage("mod_remove_not_moderator", playerName)), false);
                return 0;
            }

            moderatorManager.removeModerator(leader.getUuid(), targetProfile.getId());
            leader.sendMessage(Text.literal("§a" + configManager.getMessage("mod_remove_success", playerName)), false);

            // Notifier le joueur s'il est en ligne et rafraîchir ses commandes
            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.sendMessage(Text.literal("§e" + configManager.getMessage("mod_remove_notification").replace("%leader%", leader.getName().getString())), false);

                // Forcer le rafraîchissement des commandes côté client
                server.getCommandManager().sendCommandTree(targetPlayer);
            }

            return 1;
        } catch (Exception e) {
            leader.sendMessage(Text.literal("§c" + configManager.getMessage("mod_remove_error").replace("%error%", e.getMessage())), false);
            return 0;
        }
    }
}
