package com.dremixam.communityleaders.events;

import com.dremixam.communityleaders.Communityleaders;
import com.dremixam.communityleaders.utils.ConsistencyChecker;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Écouteur d'événements LuckPerms pour détecter les changements de permissions
 */
public class PermissionEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Communityleaders.MOD_ID);
    private final ConsistencyChecker consistencyChecker;

    public PermissionEventListener(ConsistencyChecker consistencyChecker) {
        this.consistencyChecker = consistencyChecker;
    }

    /**
     * Enregistre les écouteurs d'événements LuckPerms
     */
    public void registerEvents(LuckPerms luckPerms) {
        EventBus eventBus = luckPerms.getEventBus();

        // Écouter les recalculs de données utilisateur (changements de permissions)
        eventBus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);

        LOGGER.info("LuckPerms event listeners registered");
    }

    /**
     * Gère les événements de recalcul des données utilisateur
     */
    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        // Vérifier la cohérence pour ce joueur après le changement de permissions
        consistencyChecker.checkPermissionChange(event.getUser().getUniqueId());
    }
}
