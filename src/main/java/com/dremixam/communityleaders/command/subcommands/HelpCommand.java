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

public class HelpCommand {
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

        if (source.isExecutedByPlayer()) {
            ServerPlayerEntity player = source.getPlayer();
            player.sendMessage(Text.literal("§a" + configManager.getMessage("help_title")), false);

            if (PermissionUtils.hasPermission(source, "communityleaders.invite")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_invite")), false);
            }
            if (PermissionUtils.hasPermission(source, "communityleaders.uninvite")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_uninvite")), false);
            }
            if (PermissionUtils.hasPermission(source, "communityleaders.ban")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_ban")), false);
            }
            if (PermissionUtils.hasPermission(source, "communityleaders.list")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_list")), false);
            }
            if (PermissionUtils.hasPermission(source, "communityleaders.tree")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_tree")), false);
            }
            if (PermissionUtils.hasPermission(source, "communityleaders.moderator")) {
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_mod_add")), false);
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_mod_remove")), false);
                player.sendMessage(Text.literal("§f" + configManager.getMessage("help_mod_list")), false);
            }
        } else {
            // Console
            source.sendFeedback(() -> Text.literal("§a" + configManager.getMessage("help_title")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_invite")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_uninvite")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_ban")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_list")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_tree")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_mod_add")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_mod_remove")), false);
            source.sendFeedback(() -> Text.literal("§f" + configManager.getMessage("help_mod_list")), false);
        }

        return 1;
    }
}
