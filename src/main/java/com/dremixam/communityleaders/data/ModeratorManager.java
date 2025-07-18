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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire pour les modérateurs des leaders de communauté.
 * Un modérateur ne peut être nommé que parmi les joueurs invités par le leader.
 */
public class ModeratorManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;
    private static final Type dataType = new TypeToken<ConcurrentHashMap<UUID, List<UUID>>>() {}.getType();

    private Map<UUID, List<UUID>> moderators; // Leader UUID -> List of Moderator UUIDs

    public ModeratorManager(ConfigManager configManager) {
        this.dataFile = configManager.getConfigDirectory().resolve("moderators.json").toFile();
        loadData();
    }

    public void loadData() {
        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                moderators = GSON.fromJson(reader, dataType);
                if (moderators == null) {
                    moderators = new ConcurrentHashMap<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
                moderators = new ConcurrentHashMap<>();
            }
        } else {
            moderators = new ConcurrentHashMap<>();
        }
    }

    public void saveData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            GSON.toJson(moderators, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ajoute un modérateur à un leader.
     * @param leader UUID du leader
     * @param moderator UUID du modérateur
     */
    public void addModerator(UUID leader, UUID moderator) {
        moderators.computeIfAbsent(leader, k -> new ArrayList<>()).add(moderator);
        saveData();
    }

    /**
     * Retire un modérateur d'un leader.
     * @param leader UUID du leader
     * @param moderator UUID du modérateur
     */
    public void removeModerator(UUID leader, UUID moderator) {
        if (moderators.containsKey(leader)) {
            moderators.get(leader).remove(moderator);
            if (moderators.get(leader).isEmpty()) {
                moderators.remove(leader);
            }
            saveData();
        }
    }

    /**
     * Vérifie si un joueur est modérateur d'un leader spécifique.
     * @param leader UUID du leader
     * @param moderator UUID du modérateur potentiel
     * @return true si le joueur est modérateur de ce leader
     */
    public boolean isModerator(UUID leader, UUID moderator) {
        return moderators.containsKey(leader) && moderators.get(leader).contains(moderator);
    }

    /**
     * Récupère la liste des modérateurs d'un leader.
     * @param leader UUID du leader
     * @return Liste des UUIDs des modérateurs
     */
    public List<UUID> getModerators(UUID leader) {
        return new ArrayList<>(moderators.getOrDefault(leader, new ArrayList<>()));
    }

    /**
     * Trouve le leader d'un modérateur.
     * @param moderator UUID du modérateur
     * @return UUID du leader ou null si le joueur n'est modérateur de personne
     */
    public UUID getLeader(UUID moderator) {
        for (Map.Entry<UUID, List<UUID>> entry : moderators.entrySet()) {
            if (entry.getValue().contains(moderator)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Vérifie si un joueur est modérateur de quelqu'un.
     * @param moderator UUID du modérateur potentiel
     * @return true si le joueur est modérateur d'au moins un leader
     */
    public boolean isModeratorOfAnyone(UUID moderator) {
        return getLeader(moderator) != null;
    }

    /**
     * Récupère tous les modérateurs de tous les leaders.
     * @return Map complète des modérateurs
     */
    public Map<UUID, List<UUID>> getAllModerators() {
        return new ConcurrentHashMap<>(moderators);
    }
}
