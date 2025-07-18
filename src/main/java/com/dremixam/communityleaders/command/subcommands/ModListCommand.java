package com.dremixam.communityleaders.command.subcommands;

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

public class ModListCommand {
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

        try {
            List<UUID> moderators = moderatorManager.getModerators(leader.getUuid());

            if (moderators.isEmpty()) {
                leader.sendMessage(Text.literal("§eVous n'avez aucun modérateur."), false);
                return 0;
            }

            leader.sendMessage(Text.literal("§aVos modérateurs (" + moderators.size() + "):"), false);

            var server = source.getServer();
            var userCache = server.getUserCache();

            for (UUID modUuid : moderators) {
                var profileOpt = userCache.getByUuid(modUuid);
                String modName = profileOpt.map(profile -> profile.getName()).orElse("Unknown Player");
                leader.sendMessage(Text.literal("- " + modName), false);
            }

            return 1;
        } catch (Exception e) {
            leader.sendMessage(Text.literal("§cErreur lors de l'affichage des modérateurs: " + e.getMessage()), false);
            return 0;
        }
    }
}
