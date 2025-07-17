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
    private final File aDonnees;
    private static final Type aTypeDonnees = new TypeToken<ConcurrentHashMap<UUID, List<UUID>>>() {}.getType();

    private Map<UUID, List<UUID>> aInvitations;

    public InvitationManager(ConfigManager configManager) {
        this.aDonnees = configManager.getConfigDirectory().resolve("invites.json").toFile();
        chargerDonnees();
    }

    public void chargerDonnees() {
        if (aDonnees.exists()) {
            try (FileReader reader = new FileReader(aDonnees)) {
                aInvitations = GSON.fromJson(reader, aTypeDonnees);
            } catch (IOException e) {
                e.printStackTrace();
                aInvitations = new ConcurrentHashMap<>();
            }
        } else {
            aInvitations = new ConcurrentHashMap<>();
        }
    }

    public void sauvegarderDonnees() {
        try (FileWriter writer = new FileWriter(aDonnees)) {
            GSON.toJson(aInvitations, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ajouterInvitation(UUID inviteur, UUID invite) {
        aInvitations.computeIfAbsent(inviteur, k -> new ArrayList<>()).add(invite);
        sauvegarderDonnees();
    }

    public void retirerInvitation(UUID inviteur, UUID invite) {
        if (aInvitations.containsKey(inviteur)) {
            aInvitations.get(inviteur).remove(invite);
            if (aInvitations.get(inviteur).isEmpty()) {
                aInvitations.remove(inviteur);
            }
            sauvegarderDonnees();
        }
    }

    public boolean aInvite(UUID inviteur, UUID invite) {
        return aInvitations.containsKey(inviteur) && aInvitations.get(inviteur).contains(invite);
    }

    public List<UUID> obtenirInvites(UUID inviteur) {
        return aInvitations.getOrDefault(inviteur, new ArrayList<>());
    }
}
