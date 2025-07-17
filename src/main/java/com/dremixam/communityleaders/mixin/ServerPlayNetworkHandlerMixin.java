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
     * Intercepts packet sending to block world data if rules are not accepted
     */
    @Inject(method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void interceptPacketSending(Packet<?> packet, CallbackInfo ci) {
        if (this.player != null && PlayerConnectionHandler.isAwaitingRules(this.player.getUuid())) {

            // Block ONLY packets that allow player interaction
            // Let world data pass through so it loads correctly
            if (packet instanceof PlayerPositionLookS2CPacket ||
                packet instanceof UpdateSelectedSlotS2CPacket ||
                packet instanceof HealthUpdateS2CPacket ||
                packet instanceof PlayerAbilitiesS2CPacket) {

                // Block these interaction packets
                ci.cancel();
                return;
            }

            // Allow everything else (chunks, entities, inventory, etc.)
            // Player will see the world loading but cannot interact
        }
    }
}
