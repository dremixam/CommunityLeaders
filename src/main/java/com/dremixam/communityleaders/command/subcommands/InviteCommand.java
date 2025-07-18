package com.dremixam.communityleaders.command.subcommands;

import com.dremixam.communityleaders.command.PermissionUtils;
import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

public class InviteCommand {
    private static InvitationManager invitationManager;
    private static ConfigManager configManager;
    private static ModeratorManager moderatorManager;

    public static void setManagers(InvitationManager invManager, ConfigManager confManager, ModeratorManager modManager) {
        invitationManager = invManager;
        configManager = confManager;
        moderatorManager = modManager;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, String commandName) {
        // Cette méthode sera appelée depuis CommunityLeadersCommand pour enregistrer la sous-commande
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
            boolean hasUnlimitedPermission = PermissionUtils.hasPermission(source, "communityleaders.unlimited");
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
}
