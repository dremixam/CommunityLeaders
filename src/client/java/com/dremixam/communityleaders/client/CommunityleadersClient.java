package com.dremixam.communityleaders.client;

import com.dremixam.communityleaders.client.network.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

public class CommunityleadersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialiser le gestionnaire de réseau côté client
        ClientNetworkHandler.initialize();
    }
}
