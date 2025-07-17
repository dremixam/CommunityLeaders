package com.dremixam.communityleaders.client.network;

import com.dremixam.communityleaders.client.gui.RulesScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * Handles client-side network events, such as receiving the rules from the server.
 */
public class ClientNetworkHandler {

    /**
     * Initializes the client-side network handlers.
     */
    public static void initialize() {
        // Handler to receive the rules from the server
        ClientPlayNetworking.registerGlobalReceiver(
            com.dremixam.communityleaders.network.NetworkConstants.SHOW_RULES_ID,
            (client, handler, buf, responseSender) -> {
                // Read data from the packet
                String title = buf.readString();
                String content = buf.readString();
                String acceptButton = buf.readString();
                String declineButton = buf.readString();
                String checkboxText = buf.readString();
                String declineMessage = buf.readString();

                // Schedule the screen display on the main thread
                client.execute(() -> {
                    RulesScreen rulesScreen = new RulesScreen(
                        title, content, acceptButton, declineButton, checkboxText, declineMessage
                    );
                    MinecraftClient.getInstance().setScreen(rulesScreen);
                });
            }
        );
    }
}
