package com.dremixam.communityleaders.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class RulesPacket {

    public static void sendRulesToPlayer(ServerPlayerEntity player, String title, String content,
                                         String acceptButton, String declineButton, String checkboxText, String declineMessage) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(title);
        buf.writeString(content);
        buf.writeString(acceptButton);
        buf.writeString(declineButton);
        buf.writeString(checkboxText);
        buf.writeString(declineMessage);

        ServerPlayNetworking.send(player, NetworkConstants.SHOW_RULES_ID, buf);
    }
}
