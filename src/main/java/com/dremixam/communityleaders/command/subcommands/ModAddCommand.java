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

public class ModAddCommand {
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

            // Vérifier que le joueur ne peut pas se nommer lui-même modérateur
            if (targetProfile.getId().equals(leader.getUuid())) {
                leader.sendMessage(Text.literal("§c" + configManager.getMessage("mod_add_cannot_self")), false);
                return 0;
            }

            // RÈGLE IMPORTANTE: Seuls les joueurs invités par ce leader peuvent devenir modérateurs
            if (!invitationManager.hasInvited(leader.getUuid(), targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + configManager.getMessage("mod_add_only_invited")), false);
                return 0;
            }

            // Vérifier si le joueur est déjà modérateur de ce leader
            if (moderatorManager.isModerator(leader.getUuid(), targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + configManager.getMessage("mod_add_already_moderator", playerName)), false);
                return 0;
            }

            // Vérifier si le joueur est modérateur de quelqu'un d'autre
            if (moderatorManager.isModeratorOfAnyone(targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + configManager.getMessage("mod_add_moderator_of_other", playerName)), false);
                return 0;
            }

            moderatorManager.addModerator(leader.getUuid(), targetProfile.getId());
            leader.sendMessage(Text.literal("§a" + configManager.getMessage("mod_add_success", playerName)), false);

            // Notifier le joueur s'il est en ligne et rafraîchir ses commandes
            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.sendMessage(Text.literal("§a" + configManager.getMessage("mod_add_notification").replace("%leader%", leader.getName().getString())), false);
                targetPlayer.sendMessage(Text.literal("§e" + configManager.getMessage("mod_add_notification_commands")), false);

                // Forcer le rafraîchissement des commandes côté client
                server.getCommandManager().sendCommandTree(targetPlayer);
            }

            return 1;
        } catch (Exception e) {
            leader.sendMessage(Text.literal("§c" + configManager.getMessage("mod_add_error").replace("%error%", e.getMessage())), false);
            return 0;
        }
    }
}
