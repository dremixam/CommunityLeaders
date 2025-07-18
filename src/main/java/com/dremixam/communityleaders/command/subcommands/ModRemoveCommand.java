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

            if (!moderatorManager.isModerator(leader.getUuid(), targetProfile.getId())) {
                leader.sendMessage(Text.literal("§c" + playerName + " n'est pas votre modérateur."), false);
                return 0;
            }

            moderatorManager.removeModerator(leader.getUuid(), targetProfile.getId());
            leader.sendMessage(Text.literal("§a" + playerName + " a été retiré de vos modérateurs."), false);

            // Notifier le joueur s'il est en ligne et rafraîchir ses commandes
            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetProfile.getId());
            if (targetPlayer != null) {
                targetPlayer.sendMessage(Text.literal("§eVous n'êtes plus modérateur de " + leader.getName().getString() + "."), false);
                targetPlayer.sendMessage(Text.literal("§eVos permissions de modérateur ont été révoquées."), false);

                // Forcer le rafraîchissement des commandes côté client
                server.getCommandManager().sendCommandTree(targetPlayer);
            }

            return 1;
        } catch (Exception e) {
            leader.sendMessage(Text.literal("§cErreur lors de la suppression du modérateur: " + e.getMessage()), false);
            return 0;
        }
    }
}
