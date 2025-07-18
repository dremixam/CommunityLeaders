package com.dremixam.communityleaders.config;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("communityleaders");
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("config.yml").toFile();

    private Map<String, Object> config;

    public ConfigManager() {
        createConfigDirectory();
        loadConfig();
    }

    private void createConfigDirectory() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            createDefaultConfig();
        }

        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            Yaml yaml = new Yaml();
            config = yaml.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
            createDefaultConfig();
        }
    }

    private void createDefaultConfig() {
        String defaultConfig = """
                # Community Leaders Configuration
                # Configuration for streamer invitation system and rules

                # Command configuration
                command:
                  alias: "cl"  # Alias for /communityleaders command (set to empty string to disable)

                # Invitation limits
                limits:
                  max_invitations_per_leader: 5  # Maximum number of players a leader can invite (-1 for unlimited)

                # Customizable messages
                messages:
                  # General
                  player_not_found: "Player '%player%' not found."
                  console_only_players: "This command can only be executed by players, not from console."
                  
                  # Invite command
                  invite_success: "Successfully invited '%player%' to the server!"
                  invite_already_whitelisted: "Player '%player%' is already whitelisted."
                  invite_error: "Error inviting player: %error%"
                  invite_limit_reached: "You have reached your invitation limit (%limit% invitations). You cannot invite more players."

                  # Uninvite command
                  uninvite_success: "Successfully uninvited '%player%' from the server."
                  uninvite_only_invited: "You can only uninvite players you invited yourself."
                  uninvite_disconnect_message: "You have been uninvited from the server."
                  uninvite_error: "Error uninviting player: %error%"

                  # Ban command
                  ban_success: "Successfully banned '%player%' from the server."
                  ban_only_invited: "You can only ban players you invited yourself."
                  ban_reason: "Banned by the streamer who invited you."
                  ban_disconnect_message: "You have been banned from the server."
                  ban_error: "Error banning player: %error%"
                  
                  # List command
                  list_title: "Players %player% invited:"
                  list_empty: "You haven't invited anyone yet."
                  list_entry: "- %player%"
                  
                  # Tree command
                  tree_title: "Invitation Tree:"
                  
                  # Moderator commands
                  mod_add_success: "%player% has been added as a moderator."
                  mod_add_already_moderator: "%player% is already your moderator."
                  mod_add_moderator_of_other: "%player% is already a moderator of another leader."
                  mod_add_only_invited: "You can only make moderators from players you have invited."
                  mod_add_cannot_self: "You cannot make yourself a moderator of yourself."
                  mod_add_notification: "You have been made a moderator by %leader%!"
                  mod_add_notification_commands: "You now have access to their leader commands."
                  mod_add_error: "Error adding moderator: %error%"
                  
                  mod_remove_success: "%player% has been removed as a moderator."
                  mod_remove_not_moderator: "%player% is not your moderator."
                  mod_remove_notification: "You are no longer a moderator of %leader%."
                  mod_remove_error: "Error removing moderator: %error%"
                  
                  mod_list_title: "Your moderators:"
                  mod_list_empty: "You don't have any moderators yet."
                  mod_list_entry: "- %player%"
                  mod_list_error: "Error displaying moderators: %error%"
                  
                  # Help messages
                  help_title: "Community Leaders Commands:"
                  help_invite: "/cl invite <player> - Invite a player to the server"
                  help_uninvite: "/cl uninvite <player> - Remove a player you invited"
                  help_ban: "/cl ban <player> - Ban a player you invited"
                  help_list: "/cl list - Show players you have invited"
                  help_tree: "/cl tree - Show the complete invitation tree"
                  help_mod_add: "/cl mod add <player> - Add a moderator"
                  help_mod_remove: "/cl mod remove <player> - Remove a moderator"
                  help_mod_list: "/cl mod list - List your moderators"
                """;

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write(defaultConfig);
            // Recharger après création
            Yaml yaml = new Yaml();
            config = yaml.load(new StringReader(defaultConfig));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMessage(String key) {
        Map<String, Object> messages = (Map<String, Object>) config.get("messages");
        return (String) messages.get(key);
    }

    public String getMessage(String key, String playerName) {
        return getMessage(key).replace("%player%", playerName);
    }

    public String getCommandAlias() {
        Map<String, Object> command = (Map<String, Object>) config.get("command");
        return (String) command.get("alias");
    }

    public int getMaxInvitationsPerLeader() {
        Map<String, Object> limits = (Map<String, Object>) config.get("limits");
        return (Integer) limits.get("max_invitations_per_leader");
    }

    public Path getConfigDirectory() {
        return CONFIG_DIR;
    }
}
