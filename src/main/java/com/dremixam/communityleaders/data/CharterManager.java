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

public class CharterManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File aDonnees;
    private static final Type aTypeDonnees = new TypeToken<ConcurrentHashMap<UUID, Boolean>>() {}.getType();

    private Set<UUID> aJoueursAyantAccepte;

    public CharterManager(ConfigManager configManager) {
        this.aDonnees = configManager.getConfigDirectory().resolve("charter_accepted.json").toFile();
        chargerDonnees();
    }

    public void chargerDonnees() {
        if (aDonnees.exists()) {
            try (FileReader reader = new FileReader(aDonnees)) {
                ConcurrentHashMap<UUID, Boolean> data = GSON.fromJson(reader, aTypeDonnees);
                if (data != null) {
                    // Créer un nouveau Set avec les UUID au lieu d'utiliser keySet() directement
                    aJoueursAyantAccepte = ConcurrentHashMap.newKeySet();
                    aJoueursAyantAccepte.addAll(data.keySet());
                } else {
                    aJoueursAyantAccepte = ConcurrentHashMap.newKeySet();
                }
            } catch (IOException e) {
                e.printStackTrace();
                aJoueursAyantAccepte = ConcurrentHashMap.newKeySet();
            }
        } else {
            aJoueursAyantAccepte = ConcurrentHashMap.newKeySet();
        }
    }

    public void sauvegarderDonnees() {
        try (FileWriter writer = new FileWriter(aDonnees)) {
            ConcurrentHashMap<UUID, Boolean> data = new ConcurrentHashMap<>();
            for (UUID uuid : aJoueursAyantAccepte) {
                data.put(uuid, true);
            }
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean aAccepteCharte(UUID joueur) {
        // Vérifier que l'UUID n'est pas null avant de chercher dans le Set
        if (joueur == null) {
            return false;
        }
        return aJoueursAyantAccepte.contains(joueur);
    }

    public void marquerCharteAcceptee(UUID joueur) {
        aJoueursAyantAccepte.add(joueur);
        sauvegarderDonnees();
    }

    public void retirerAcceptationCharte(UUID joueur) {
        aJoueursAyantAccepte.remove(joueur);
        sauvegarderDonnees();
    }
}
