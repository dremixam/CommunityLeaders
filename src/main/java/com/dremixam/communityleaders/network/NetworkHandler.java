package com.dremixam.communityleaders.network;

import com.dremixam.communityleaders.config.ConfigManager;
import com.dremixam.communityleaders.data.CharterManager;
import com.dremixam.communityleaders.events.PlayerConnectionHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class NetworkHandler {
    private static ConfigManager configManager;
    private static CharterManager charterManager;

    public static void initialize(ConfigManager config, CharterManager charter) {
        configManager = config;
        charterManager = charter;

        // Handler for charter acceptance
        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.CHARTER_ACCEPT_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // Check if the player was waiting
                if (PlayerConnectionHandler.isAwaitingCharter(player.getUuid())) {
                    // Mark acceptance via PlayerConnectionHandler
                    PlayerConnectionHandler.onCharterAccepted(player.getUuid());

                    // Restore player to normal mode (exit spectator mode)
                    player.changeGameMode(GameMode.SURVIVAL);

                    // Confirmation message
                    player.sendMessage(Text.literal("§aYou have accepted the server charter. Welcome!"), false);
                }
            });
        });

        // Handler for charter decline
        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.CHARTER_DECLINE_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // Mark decline
                PlayerConnectionHandler.onCharterDeclined(player.getUuid());

                // Disconnect player with configured message
                player.networkHandler.disconnect(Text.literal(configManager.getCharterDeclineMessage()));
            });
        });
    }
}
