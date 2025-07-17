package com.dremixam.communityleaders.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.dremixam.communityleaders.config.ConfigManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages rules acceptance data for players.
 * Stores which players have accepted the server rules.
 */
public class RulesManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;
    private static final Type DATA_TYPE = new TypeToken<ConcurrentHashMap<UUID, Boolean>>() {}.getType();

    private Set<UUID> playersWhoAcceptedRules;

    public RulesManager(ConfigManager configManager) {
        this.dataFile = configManager.getConfigDirectory().resolve("rules_accepted.json").toFile();
        loadData();
    }

    /**
     * Loads rules acceptance data from the file.
     */
    public void loadData() {
        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                ConcurrentHashMap<UUID, Boolean> data = GSON.fromJson(reader, DATA_TYPE);
                if (data != null) {
                    // Create a new Set with the UUIDs instead of using keySet() directly
                    playersWhoAcceptedRules = ConcurrentHashMap.newKeySet();
                    playersWhoAcceptedRules.addAll(data.keySet());
                } else {
                    playersWhoAcceptedRules = ConcurrentHashMap.newKeySet();
                }
            } catch (IOException e) {
                e.printStackTrace();
                playersWhoAcceptedRules = ConcurrentHashMap.newKeySet();
            }
        } else {
            playersWhoAcceptedRules = ConcurrentHashMap.newKeySet();
        }
    }

    /**
     * Saves the current rules acceptance data to the file.
     */
    public void saveData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            ConcurrentHashMap<UUID, Boolean> data = new ConcurrentHashMap<>();
            for (UUID uuid : playersWhoAcceptedRules) {
                data.put(uuid, true);
            }
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if a player has accepted the rules.
     * @param playerId The UUID of the player to check.
     * @return true if the player has accepted, false otherwise.
     */
    public boolean hasAcceptedRules(UUID playerId) {
        // Check that the UUID is not null before searching in the Set
        if (playerId == null) {
            return false;
        }
        return playersWhoAcceptedRules.contains(playerId);
    }

    /**
     * Marks a player as having accepted the rules.
     * @param playerId The UUID of the player.
     */
    public void markRulesAccepted(UUID playerId) {
        playersWhoAcceptedRules.add(playerId);
        saveData();
    }

    /**
     * Removes a player's rules acceptance status.
     * @param playerId The UUID of the player.
     */
    public void removeRulesAcceptance(UUID playerId) {
        playersWhoAcceptedRules.remove(playerId);
        saveData();
    }
}
