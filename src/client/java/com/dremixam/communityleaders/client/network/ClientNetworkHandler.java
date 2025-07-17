package com.dremixam.communityleaders.client.network;

import com.dremixam.communityleaders.client.gui.CharterScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * Handles client-side network events, such as receiving the charter from the server.
 */
public class ClientNetworkHandler {

    /**
     * Initializes the client-side network handlers.
     */
    public static void initialize() {
        // Handler to receive the charter from the server
        ClientPlayNetworking.registerGlobalReceiver(
            com.dremixam.communityleaders.network.NetworkConstants.SHOW_CHARTER_ID,
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
                    CharterScreen charterScreen = new CharterScreen(
                        title, content, acceptButton, declineButton, checkboxText, declineMessage
                    );
                    MinecraftClient.getInstance().setScreen(charterScreen);
                });
            }
        );
    }
}
