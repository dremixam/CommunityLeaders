package com.dremixam.communityleaders.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public class RulesResponsePacket {

    public static void sendAcceptPacket() {
        ClientPlayNetworking.send(NetworkConstants.RULES_ACCEPT_ID, PacketByteBufs.create());
    }

    public static void sendDeclinePacket() {
        ClientPlayNetworking.send(NetworkConstants.RULES_DECLINE_ID, PacketByteBufs.create());
    }
}
