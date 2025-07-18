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
import net.minecraft.server.Whitelist;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

public class BanCommand {
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
}
