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
                # Configuration du mod Community Leaders
                
                # Charte du serveur
                charter:
                  enabled: true
                  title: "Charte du Serveur"
                  content: |
                    Bienvenue sur notre serveur Minecraft !
                    
                    En rejoignant ce serveur, vous acceptez de respecter les règles suivantes :
                    
                    1. Respectez les autres joueurs et leurs constructions
                    2. Pas de grief, de vol ou de destruction intentionnelle
                    3. Pas de langage offensant ou de harcèlement
                    4. Respectez les zones protégées et les propriétés privées
                    5. Pas de triche, hack ou exploitation de bugs
                    6. Écoutez et respectez les modérateurs et administrateurs
                    
                    Le non-respect de ces règles peut entraîner un avertissement,
                    une suspension temporaire ou un bannissement permanent.
                    
                    Merci de contribuer à maintenir une communauté amicale !
                  accept_button: "J'accepte"
                  decline_button: "Je refuse"
                  checkbox_text: "Je comprends et j'accepte la charte"
                  decline_message: "Vous devez accepter la charte pour jouer sur ce serveur."
                
                # Messages personnalisables
                messages:
                  invite_success: "%player% a été invité et ajouté à la liste blanche."
                  invite_already_whitelisted: "Ce joueur est déjà sur la liste blanche."
                  uninvite_success: "%player% n'est plus invité et a été retiré de la liste blanche."
                  ban_success: "%player% a été banni et retiré de la liste blanche."
                  player_not_found: "Le joueur %player% n'a pas été trouvé."
                  no_permission: "Vous n'avez pas la permission d'utiliser cette commande."
                  not_your_invite: "Vous ne pouvez pas agir sur ce joueur car vous ne l'avez pas invité."
                  already_banned: "Ce joueur est déjà banni."
                  ban_reason: "Banni par le streamer qui vous a invité."
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
