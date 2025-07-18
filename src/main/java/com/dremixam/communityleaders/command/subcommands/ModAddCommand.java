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
            source.sendFeedback(() -> Text.literal("§cCette commande ne peut être utilisée que par des joueurs."), false);
            return 0;
        }

        ServerPlayerEntity leader = source.getPlayer();
        String playerName = StringArgumentType.getString(context, "player");

        try {
            var server = source.getServer();
            var userCache = server.getUserCache();

            var profileOpt = userCache.findByName(playerName);
            if (profileOpt.isEmpty()) {
                leader.sendMessage(Text.literal("§cJoueur non trouvé: " + playerName), false);
                return 0;
            }

            GameProfile targetProfile = profileOpt.get();

            // Vérifier que le joueur ne peut pas se nommer lui-même modérateur
            if (targetProfile.getId().equals(leader.getUuid())) {
                leader.sendMessage(Text.literal("§cVous ne pouvez pas vous nommer modérateur de vous-même."), false);
                return 0;
            }

            // RÈGLE IMPORTANTE: Seuls les joueurs invités par ce leader peuvent devenir modérateurs
            if (!invitationManager.hasInvited(leader.getUuid(), targetProfile.getId())) {
                leader.sendMessage(Text.literal("§cVous ne pouvez nommer modérateur que les joueurs que vous avez invités."), false);
                return 0;
            }

            // Vérifier si le joueur est déjà modérateur de ce leader
            if (moderatorManager.isModerator(leader.getUuid(), targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + playerName + " est déjà votre modérateur."), false);
                return 0;
            }

            // Vérifier si le joueur est modérateur de quelqu'un d'autre
            if (moderatorManager.isModeratorOfAnyone(targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + playerName + " est déjà modérateur d'un autre leader."), false);
                return 0;
            }

            moderatorManager.addModerator(leader.getUuid(), targetProfile.getId());
            leader.sendMessage(Text.literal("§a" + playerName + " a été ajouté comme modérateur."), false);

            // Notifier le joueur s'il est en ligne et rafraîchir ses commandes
            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.sendMessage(Text.literal("§aVous avez été nommé modérateur par " + leader.getName().getString() + " !"), false);
                targetPlayer.sendMessage(Text.literal("§eVous avez maintenant accès à ses commandes de leader."), false);

                // Forcer le rafraîchissement des commandes côté client
                server.getCommandManager().sendCommandTree(targetPlayer);
            }

            return 1;
        } catch (Exception e) {
            leader.sendMessage(Text.literal("§cErreur lors de l'ajout du modérateur: " + e.getMessage()), false);
            return 0;
        }
    }
}
