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
                  no_permission: "You don't have permission to use this command."
                  
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
                  list_title: "Players you have invited:"
                  list_empty: "You haven't invited anyone yet."
                  list_entry: "- %player%"
                  
                  # Tree command
                  tree_title: "Invitation Tree:"
                  tree_root: "Root players (not invited by anyone):"
                  tree_branch: "├── %player%"
                  tree_last_branch: "└── %player%"
                  tree_indent: "    "
                  
                  # Help messages
                  help_title: "Community Leaders Commands:"
                  help_invite: "/cl invite <player> - Invite a player to the server"
                  help_uninvite: "/cl uninvite <player> - Remove a player you invited"
                  help_ban: "/cl ban <player> - Ban a player you invited"
                  help_list: "/cl list - Show players you have invited"
                  help_tree: "/cl tree - Show the complete invitation tree"
                  
                  # Console messages
                  console_only_players: "This command can only be executed by players, not from console."
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
