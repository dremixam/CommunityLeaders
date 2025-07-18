package com.dremixam.communityleaders.command;

import com.dremixam.communityleaders.command.subcommands.*;
import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Main Community Leaders command with all subcommands.
 * Supports /communityleaders and configurable alias (default: /cl)
 */
public class CommunityLeadersCommand {
    private static InvitationManager invitationManager;
    private static ConfigManager configManager;
    private static ModeratorManager moderatorManager;

    /**
     * Registers the main command and its alias.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
                               CommandManager.RegistrationEnvironment environment, InvitationManager invManager, ConfigManager confManager, ModeratorManager modManager) {
        invitationManager = invManager;
        configManager = confManager;
        moderatorManager = modManager;

        // Initialize all subcommand managers
        initializeSubcommands();

        // Register main command
        registerCommunityleadersCommand(dispatcher);

        // Register alias if configured
        String alias = configManager.getCommandAlias();
        if (alias != null && !alias.trim().isEmpty()) {
            registerAliasCommand(dispatcher, alias.trim());
        }
    }

    private static void initializeSubcommands() {
        // Initialize permission utilities
        PermissionUtils.setModeratorManager(moderatorManager);

        // Initialize all subcommands with their managers
        HelpCommand.setManagers(invitationManager, configManager, moderatorManager);
        InviteCommand.setManagers(invitationManager, configManager, moderatorManager);
        UninviteCommand.setManagers(invitationManager, configManager, moderatorManager);
        BanCommand.setManagers(invitationManager, configManager, moderatorManager);
        ListCommand.setManagers(invitationManager, configManager, moderatorManager);
        TreeCommand.setManagers(invitationManager, configManager, moderatorManager);
        ModAddCommand.setManagers(invitationManager, configManager, moderatorManager);
        ModRemoveCommand.setManagers(invitationManager, configManager, moderatorManager);
        ModListCommand.setManagers(invitationManager, configManager, moderatorManager);
    }

    private static void registerCommunityleadersCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("communityleaders")
                .requires(source -> PermissionUtils.hasAnyPermission(source))
                .executes(HelpCommand::execute)
                .then(CommandManager.literal("invite")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.invite"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(InviteCommand::execute)))
                .then(CommandManager.literal("uninvite")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.uninvite"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(UninviteCommand::execute)))
                .then(CommandManager.literal("ban")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.ban"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(BanCommand::execute)))
                .then(CommandManager.literal("list")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.list"))
                        .executes(ListCommand::execute))
                .then(CommandManager.literal("tree")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.tree"))
                        .executes(TreeCommand::execute))
                .then(CommandManager.literal("mod")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.moderator"))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(ModAddCommand::execute)))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(ModRemoveCommand::execute)))
                        .then(CommandManager.literal("list")
                                .executes(ModListCommand::execute))));
    }

    private static void registerAliasCommand(CommandDispatcher<ServerCommandSource> dispatcher, String alias) {
        dispatcher.register(CommandManager.literal(alias)
                .requires(source -> PermissionUtils.hasAnyPermission(source))
                .executes(HelpCommand::execute)
                .then(CommandManager.literal("invite")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.invite"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(InviteCommand::execute)))
                .then(CommandManager.literal("uninvite")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.uninvite"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(UninviteCommand::execute)))
                .then(CommandManager.literal("ban")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.ban"))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(BanCommand::execute)))
                .then(CommandManager.literal("list")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.list"))
                        .executes(ListCommand::execute))
                .then(CommandManager.literal("tree")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.tree"))
                        .executes(TreeCommand::execute))
                .then(CommandManager.literal("mod")
                        .requires(source -> PermissionUtils.hasPermission(source, "communityleaders.moderator", false))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(ModAddCommand::execute)))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(ModRemoveCommand::execute)))
                        .then(CommandManager.literal("list")
                                .executes(ModListCommand::execute))));
    }
}
