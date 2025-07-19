package com.dremixam.communityleaders;

import com.dremixam.communityleaders.command.CommunityLeadersCommand;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.utils.ConsistencyChecker;
import com.dremixam.communityleaders.events.PermissionEventListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Communityleaders implements ModInitializer {
    public static final String MOD_ID = "CommunityLeaders";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private ConfigManager configManager;
    private InvitationManager invitationManager;
    private ModeratorManager moderatorManager;
    private ConsistencyChecker consistencyChecker;
    private PermissionEventListener permissionEventListener;

    @Override
    public void onInitialize() {
        // Vérifier si on est côté serveur (pas en solo)
        if (!FabricLoader.getInstance().getEnvironmentType().name().equals("SERVER")) {
            LOGGER.info("Community Leaders disabled in single-player mode");
            return;
        }

        // Vérifier si LuckPerms est disponible
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("LuckPerms not found, Community Leaders will not function properly. Disabling mod.");
            return;
        }

        LOGGER.info("Initializing Community Leaders!");

        configManager = new ConfigManager();
        invitationManager = new InvitationManager(configManager);
        moderatorManager = new ModeratorManager(configManager);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CommunityLeadersCommand.register(dispatcher, registryAccess, environment, invitationManager, configManager, moderatorManager);
        });

        // Enregistrer l'événement de démarrage du serveur pour les vérifications de cohérence
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started, initializing consistency checks...");

            // Initialiser le vérificateur de cohérence
            consistencyChecker = new ConsistencyChecker(invitationManager, moderatorManager, server);

            // Effectuer les vérifications de cohérence au démarrage
            consistencyChecker.performStartupConsistencyCheck();

            // Enregistrer les écouteurs d'événements LuckPerms
            try {
                permissionEventListener = new PermissionEventListener(consistencyChecker);
                permissionEventListener.registerEvents(LuckPermsProvider.get());
                LOGGER.info("Consistency check system initialized successfully");
            } catch (Exception e) {
                LOGGER.error("Error during LuckPerms listeners initialization", e);
            }
        });

        LOGGER.info("Community Leaders successfully initialized!");
    }
}
