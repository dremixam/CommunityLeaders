package com.dremixam.communityleaders.command.subcommands;

import com.dremixam.communityleaders.command.PermissionUtils;
import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public class ListCommand {
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

        try {
            // Déterminer qui est le vrai leader (le joueur ou son leader si c'est un modérateur)
            UUID actualLeaderUuid = player.getUuid();
            String leaderName = player.getName().getString();

            UUID leaderUuid = moderatorManager.getLeader(player.getUuid());
            if (leaderUuid != null) {
                // Le joueur est modérateur, utiliser son leader
                actualLeaderUuid = leaderUuid;
                var server = source.getServer();
                var userCache = server.getUserCache();
                var leaderProfileOpt = userCache.getByUuid(leaderUuid);
                leaderName = leaderProfileOpt.map(profile -> profile.getName()).orElse("Leader");
            }

            List<UUID> invitedPlayers = invitationManager.getInvitedPlayers(actualLeaderUuid);
            int currentInvitations = invitedPlayers.size();

            // Construire le titre avec le ratio d'invitations
            String title = configManager.getMessage("list_title", leaderName);

            // Ajouter le ratio si le joueur n'a pas la permission unlimited
            boolean hasUnlimitedPermission = PermissionUtils.hasPermission(source, "communityleaders.unlimited");
            if (!hasUnlimitedPermission) {
                int maxInvitations = configManager.getMaxInvitationsPerLeader();
                if (maxInvitations != -1) { // -1 = illimité, sinon limite appliquée
                    title += " (" + currentInvitations + "/" + maxInvitations + ")";
                } else {
                    title += " (" + currentInvitations + "/∞)";
                }
            } else {
                title += " (" + currentInvitations + "/∞)";
            }

            if (invitedPlayers.isEmpty()) {
                player.sendMessage(Text.literal("§e" + configManager.getMessage("list_empty")), false);
                return 0;
            }

            player.sendMessage(Text.literal("§a" + title), false);

            var server = source.getServer();
            var userCache = server.getUserCache();

            for (UUID invitedUuid : invitedPlayers) {
                var profileOpt = userCache.getByUuid(invitedUuid);
                String playerName = profileOpt.map(profile -> profile.getName()).orElse("Unknown Player");

                String entry = configManager.getMessage("list_entry", playerName);

                // Vérifier si le joueur invité est modérateur du leader actuel
                boolean isModerator = moderatorManager.isModerator(actualLeaderUuid, invitedUuid);

                if (isModerator) {
                    // Afficher en jaune si c'est un modérateur
                    player.sendMessage(Text.literal("§e" + entry), false);
                } else {
                    // Afficher en blanc si c'est un invité normal
                    player.sendMessage(Text.literal("§f" + entry), false);
                }
            }

            return 1;
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c" + "Error displaying list: " + e.getMessage()), false);
            return 0;
        }
    }
}
