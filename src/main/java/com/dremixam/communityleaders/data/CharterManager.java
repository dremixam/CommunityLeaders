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
 * Manages charter acceptance data for players.
 * Stores which players have accepted the server charter.
 */
public class CharterManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;
    private static final Type DATA_TYPE = new TypeToken<ConcurrentHashMap<UUID, Boolean>>() {}.getType();

    private Set<UUID> playersWhoAcceptedCharter;

    public CharterManager(ConfigManager configManager) {
        this.dataFile = configManager.getConfigDirectory().resolve("charter_accepted.json").toFile();
        loadData();
    }

    /**
     * Loads charter acceptance data from the file.
     */
    public void loadData() {
        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                ConcurrentHashMap<UUID, Boolean> data = GSON.fromJson(reader, DATA_TYPE);
                if (data != null) {
                    // Create a new Set with the UUIDs instead of using keySet() directly
                    playersWhoAcceptedCharter = ConcurrentHashMap.newKeySet();
                    playersWhoAcceptedCharter.addAll(data.keySet());
                } else {
                    playersWhoAcceptedCharter = ConcurrentHashMap.newKeySet();
                }
            } catch (IOException e) {
                e.printStackTrace();
                playersWhoAcceptedCharter = ConcurrentHashMap.newKeySet();
            }
        } else {
            playersWhoAcceptedCharter = ConcurrentHashMap.newKeySet();
        }
    }

    /**
     * Saves the current charter acceptance data to the file.
     */
    public void saveData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            ConcurrentHashMap<UUID, Boolean> data = new ConcurrentHashMap<>();
            for (UUID uuid : playersWhoAcceptedCharter) {
                data.put(uuid, true);
            }
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if a player has accepted the charter.
     * @param playerId The UUID of the player to check.
     * @return true if the player has accepted, false otherwise.
     */
    public boolean hasAcceptedCharter(UUID playerId) {
        // Check that the UUID is not null before searching in the Set
        if (playerId == null) {
            return false;
        }
        return playersWhoAcceptedCharter.contains(playerId);
    }

    /**
     * Marks a player as having accepted the charter.
     * @param playerId The UUID of the player.
     */
    public void markCharterAccepted(UUID playerId) {
        playersWhoAcceptedCharter.add(playerId);
        saveData();
    }

    /**
     * Removes a player's charter acceptance status.
     * @param playerId The UUID of the player.
     */
    public void removeCharterAcceptance(UUID playerId) {
        playersWhoAcceptedCharter.remove(playerId);
        saveData();
    }
}
