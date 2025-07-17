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
                # Configuration for streamer invitation system and charter
                
                # Charter system settings
                charter:
                  enabled: true
                  title: "Server Charter"
                  content: |
                    Welcome to our community server!
                    
                    By playing on this server, you agree to follow these rules:
                    
                    1. Respect other players and their builds
                    2. No griefing, stealing or intentional destruction
                    3. No offensive language or harassment
                    4. Respect protected areas and private properties
                    5. No cheating, hacking or bug exploitation
                    6. Listen to and respect moderators and administrators
                    
                    Breaking these rules may result in a warning,
                    temporary suspension or permanent ban.
                    
                    Thank you for helping maintain a friendly community!
                  accept_button: "I Accept"
                  decline_button: "I Decline"
                  checkbox_text: "I understand and accept the charter"
                  decline_message: "You must accept the charter to play on this server."
                
                # Customizable messages
                messages:
                  # General
                  player_not_found: "Player '%player%' not found."
                  no_permission: "You don't have permission to use this command."
                  
                  # Invite command
                  invite_success: "Successfully invited '%player%' to the server!"
                  invite_already_whitelisted: "Player '%player%' is already whitelisted."
                  invite_error: "Error inviting player: %error%"

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
                  
                  # Charter system
                  charter_accepted: "You have accepted the server charter. Welcome!"
                  charter_sending_error: "Error sending charter: %error%"
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

    public boolean isCharterEnabled() {
        Map<String, Object> charter = (Map<String, Object>) config.get("charter");
        return (Boolean) charter.get("enabled");
    }

    public String getCharterTitle() {
        Map<String, Object> charter = (Map<String, Object>) config.get("charter");
        return (String) charter.get("title");
    }

    public String getCharterContent() {
        Map<String, Object> charter = (Map<String, Object>) config.get("charter");
        return (String) charter.get("content");
    }

    public String getCharterAcceptButton() {
        Map<String, Object> charter = (Map<String, Object>) config.get("charter");
        return (String) charter.get("accept_button");
    }

    public String getCharterDeclineButton() {
        Map<String, Object> charter = (Map<String, Object>) config.get("charter");
        return (String) charter.get("decline_button");
    }

    public String getCharterCheckboxText() {
        Map<String, Object> charter = (Map<String, Object>) config.get("charter");
        return (String) charter.get("checkbox_text");
    }

    public String getCharterDeclineMessage() {
        Map<String, Object> charter = (Map<String, Object>) config.get("charter");
        return (String) charter.get("decline_message");
    }

    public Path getConfigDirectory() {
        return CONFIG_DIR;
    }
}
