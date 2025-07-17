package com.dremixam.communityleaders.client.network;

import com.dremixam.communityleaders.client.gui.CharterScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class ClientNetworkHandler {

    public static void initialize() {
        // Gestionnaire pour recevoir la charte du serveur
        ClientPlayNetworking.registerGlobalReceiver(
            com.dremixam.communityleaders.network.NetworkConstants.SHOW_CHARTER_ID,
            (client, handler, buf, responseSender) -> {
                // Lire les données du paquet
                String title = buf.readString();
                String content = buf.readString();
                String acceptButton = buf.readString();
                String declineButton = buf.readString();
                String checkboxText = buf.readString();
                String declineMessage = buf.readString();

                // Programmer l'affichage de l'écran sur le thread principal
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
