package com.dremixam.communityleaders;

import com.dremixam.communityleaders.command.BanCommand;
import com.dremixam.communityleaders.command.InviteCommand;
import com.dremixam.communityleaders.command.UninviteCommand;
import com.dremixam.communityleaders.data.InvitationManager;
import com.dremixam.communityleaders.data.RulesManager;
import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.events.PlayerConnectionHandler;
import com.dremixam.communityleaders.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Communityleaders implements ModInitializer {
    public static final String MOD_ID = "communityleaders";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private ConfigManager configManager;
    private InvitationManager invitationManager;
    private RulesManager rulesManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Community Leaders!");

        configManager = new ConfigManager();
        invitationManager = new InvitationManager(configManager);
        rulesManager = new RulesManager(configManager);

        // Initialize network system
        NetworkHandler.initialize(configManager, rulesManager);

        // Initialize player connection events
        PlayerConnectionHandler.initialize(configManager, rulesManager);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            InviteCommand.register(dispatcher, registryAccess, environment, invitationManager, configManager);
            UninviteCommand.register(dispatcher, registryAccess, environment, invitationManager, configManager);
            BanCommand.register(dispatcher, registryAccess, environment, invitationManager, configManager);
        });

        LOGGER.info("Community Leaders successfully initialized!");
        if (configManager.isRulesEnabled()) {
            LOGGER.info("Rules system enabled");
        }
    }
}
