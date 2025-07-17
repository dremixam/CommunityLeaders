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
                
                # Rules system settings
                rules:
                  enabled: true
                  title: "§6§lServer Rules"
                  content: |
                    §b§lWelcome to our community server!
                    
                    §fBy playing on this server, you agree to follow these rules:
                    
                    §a1. §fRespect other players and their builds
                    §a2. §fNo griefing, stealing or intentional destruction
                    §a3. §fNo offensive language or harassment
                    §a4. §fRespect protected areas and private properties
                    §a5. §fNo cheating, hacking or bug exploitation
                    §a6. §fListen to and respect moderators and administrators
                    
                    §c§lBreaking these rules may result in a warning,
                    §c§ltemporary suspension or permanent ban.
                    
                    §e§lThank you for helping maintain a friendly community!
                  accept_button: "I Accept"
                  decline_button: "I Decline"
                  checkbox_text: "I understand and accept the rules"
                  decline_message: "You must accept the rules to play on this server."
                
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
                  
                  # Rules system
                  rules_accepted: "You have accepted the server rules. Welcome!"
                  rules_sending_error: "Error sending rules: %error%"
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

    public boolean isRulesEnabled() {
        // Try rules first, fallback to charter for backward compatibility
        if (config.containsKey("rules")) {
            Map<String, Object> rules = (Map<String, Object>) config.get("rules");
            return (Boolean) rules.get("enabled");
        } else if (config.containsKey("charter")) {
            Map<String, Object> charter = (Map<String, Object>) config.get("charter");
            return (Boolean) charter.get("enabled");
        }
        return false;
    }

    public String getRulesTitle() {
        if (config.containsKey("rules")) {
            Map<String, Object> rules = (Map<String, Object>) config.get("rules");
            return (String) rules.get("title");
        } else if (config.containsKey("charter")) {
            Map<String, Object> charter = (Map<String, Object>) config.get("charter");
            return (String) charter.get("title");
        }
        return "Server Rules";
    }

    public String getRulesContent() {
        if (config.containsKey("rules")) {
            Map<String, Object> rules = (Map<String, Object>) config.get("rules");
            return (String) rules.get("content");
        } else if (config.containsKey("charter")) {
            Map<String, Object> charter = (Map<String, Object>) config.get("charter");
            return (String) charter.get("content");
        }
        return "Please accept the server rules.";
    }

    public String getRulesAcceptButton() {
        if (config.containsKey("rules")) {
            Map<String, Object> rules = (Map<String, Object>) config.get("rules");
            return (String) rules.get("accept_button");
        } else if (config.containsKey("charter")) {
            Map<String, Object> charter = (Map<String, Object>) config.get("charter");
            return (String) charter.get("accept_button");
        }
        return "I Accept";
    }

    public String getRulesDeclineButton() {
        if (config.containsKey("rules")) {
            Map<String, Object> rules = (Map<String, Object>) config.get("rules");
            return (String) rules.get("decline_button");
        } else if (config.containsKey("charter")) {
            Map<String, Object> charter = (Map<String, Object>) config.get("charter");
            return (String) charter.get("decline_button");
        }
        return "I Decline";
    }

    public String getRulesCheckboxText() {
        if (config.containsKey("rules")) {
            Map<String, Object> rules = (Map<String, Object>) config.get("rules");
            return (String) rules.get("checkbox_text");
        } else if (config.containsKey("charter")) {
            Map<String, Object> charter = (Map<String, Object>) config.get("charter");
            return (String) charter.get("checkbox_text");
        }
        return "I understand and accept the rules";
    }

    public String getRulesDeclineMessage() {
        if (config.containsKey("rules")) {
            Map<String, Object> rules = (Map<String, Object>) config.get("rules");
            return (String) rules.get("decline_message");
        } else if (config.containsKey("charter")) {
            Map<String, Object> charter = (Map<String, Object>) config.get("charter");
            return (String) charter.get("decline_message");
        }
        return "You must accept the rules to play on this server.";
    }

    public String getRulesAcceptedMessage() {
        // Check if using new message system
        if (config.containsKey("messages")) {
            Map<String, Object> messages = (Map<String, Object>) config.get("messages");
            if (messages.containsKey("rules_accepted")) {
                return (String) messages.get("rules_accepted");
            }
        }

        // Fallback to old charter system for backward compatibility
        if (config.containsKey("charter")) {
            Map<String, Object> charter = (Map<String, Object>) config.get("charter");
            if (charter.containsKey("accepted")) {
                return (String) charter.get("accepted");
            }
        }

        return "You have accepted the server rules. Welcome!";
    }

    // Legacy methods for backward compatibility - delegate to Rules methods
    public boolean isCharterEnabled() {
        return isRulesEnabled();
    }

    public String getCharterTitle() {
        return getRulesTitle();
    }

    public String getCharterContent() {
        return getRulesContent();
    }

    public String getCharterAcceptButton() {
        return getRulesAcceptButton();
    }

    public String getCharterDeclineButton() {
        return getRulesDeclineButton();
    }

    public String getCharterCheckboxText() {
        return getRulesCheckboxText();
    }

    public String getCharterDeclineMessage() {
        return getRulesDeclineMessage();
    }

    public Path getConfigDirectory() {
        return CONFIG_DIR;
    }
}
