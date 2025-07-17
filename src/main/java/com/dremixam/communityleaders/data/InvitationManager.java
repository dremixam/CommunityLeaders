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

public class InvitationManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;
    private static final Type dataType = new TypeToken<ConcurrentHashMap<UUID, List<UUID>>>() {}.getType();

    private Map<UUID, List<UUID>> invitations;

    public InvitationManager(ConfigManager configManager) {
        this.dataFile = configManager.getConfigDirectory().resolve("invites.json").toFile();
        loadData();
    }

    public void loadData() {
        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                invitations = GSON.fromJson(reader, dataType);
            } catch (IOException e) {
                e.printStackTrace();
                invitations = new ConcurrentHashMap<>();
            }
        } else {
            invitations = new ConcurrentHashMap<>();
        }
    }

    public void saveData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            GSON.toJson(invitations, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addInvitation(UUID inviter, UUID invited) {
        invitations.computeIfAbsent(inviter, k -> new ArrayList<>()).add(invited);
        saveData();
    }

    public void removeInvitation(UUID inviter, UUID invited) {
        if (invitations.containsKey(inviter)) {
            invitations.get(inviter).remove(invited);
            if (invitations.get(inviter).isEmpty()) {
                invitations.remove(inviter);
            }
            saveData();
        }
    }

    public boolean hasInvited(UUID inviter, UUID invited) {
        return invitations.containsKey(inviter) && invitations.get(inviter).contains(invited);
    }

    public List<UUID> getInvitedPlayers(UUID inviter) {
        return invitations.getOrDefault(inviter, new ArrayList<>());
    }
}
