package com.dremixam.communityleaders.mixin;

import com.dremixam.communityleaders.events.PlayerConnectionHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    /**
     * Intercepte l'envoi de paquets pour bloquer les données du monde si la charte n'est pas acceptée
     */
    @Inject(method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void interceptPacketSending(Packet<?> packet, CallbackInfo ci) {
        if (this.player != null && PlayerConnectionHandler.isAwaitingCharter(this.player.getUuid())) {

            // Bloquer SEULEMENT les paquets qui permettent au joueur d'interagir
            // Laisser passer les données du monde pour qu'il se charge correctement
            if (packet instanceof PlayerPositionLookS2CPacket ||
                packet instanceof UpdateSelectedSlotS2CPacket ||
                packet instanceof HealthUpdateS2CPacket ||
                packet instanceof PlayerAbilitiesS2CPacket) {

                // Bloquer ces paquets d'interaction
                ci.cancel();
                return;
            }

            // Autoriser tout le reste (chunks, entités, inventaire, etc.)
            // Le joueur verra le monde se charger mais ne pourra pas interagir
        }
    }
}
