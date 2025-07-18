package com.dremixam.communityleaders;

import com.dremixam.communityleaders.command.CommunityLeadersCommand;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.ModeratorManager;
import com.dremixam.communityleaders.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Communityleaders implements ModInitializer {
    public static final String MOD_ID = "communityleaders";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private ConfigManager configManager;
    private InvitationManager invitationManager;
    private ModeratorManager moderatorManager;

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

        LOGGER.info("Community Leaders successfully initialized!");
    }
}
