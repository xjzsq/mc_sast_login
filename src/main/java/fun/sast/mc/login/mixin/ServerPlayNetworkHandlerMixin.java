package fun.sast.mc.login.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.sast.mc.login.utils.PlayerAuth;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fun.sast.mc.login.utils.PremiumChecker.isPremium;
import static net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    @Final
    private static Logger LOGGER;
    @Unique
    private static long lastAcceptedPacket = 0;

    @Inject(
            method = "onChatMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.20.5 {
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;validateAcknowledgment(Lnet/minecraft/network/message/LastSeenMessageList$Acknowledgment;)Ljava/util/Optional;",
                    //?} else if >= 1.20.3 {
                    //target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;validateMessage(Lnet/minecraft/network/message/LastSeenMessageList$Acknowledgment;)Ljava/util/Optional;",
                    //?} else {
                    /*target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;validateMessage(Ljava/lang/String;Ljava/time/Instant;Lnet/minecraft/network/message/LastSeenMessageList$Acknowledgment;)Ljava/util/Optional;",
                    *///?}
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void onPlayerChat(ChatMessageC2SPacket packet, CallbackInfo ci) {
        if (!((PlayerAuth) player).sastLogin$isAuthenticated()) {
            ((PlayerAuth) player).sastLogin$sendAuthMessage();
            ci.cancel();
        }
    }

    @Inject(
            method = "onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    // Thanks to Liach for helping me out!
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (!((PlayerAuth) player).sastLogin$isAuthenticated()) {
            if (System.nanoTime() >= lastAcceptedPacket + 20 * 1000000) {
                ((PlayerAuth) player).sastLogin$sendAuthMessage();
                player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
                lastAcceptedPacket = System.nanoTime();
            }
            ci.cancel();
        }
    }

    @Inject(
            method = "onSpectatorTeleport(Lnet/minecraft/network/packet/c2s/play/SpectatorTeleportC2SPacket;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSpectatorTeleport(SpectatorTeleportC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.player;

        if (!((PlayerAuth) player).sastLogin$isAuthenticated()) {
            ((PlayerAuth) player).sastLogin$sendAuthMessage();
            ci.cancel();
        }
    }
    //? if >= 1.20.2 {
    @WrapMethod(method = "onPlayerSession(Lnet/minecraft/network/packet/c2s/play/PlayerSessionC2SPacket;)V")
    private void onPlayerSessionWrap(PlayerSessionC2SPacket packet, Operation<Void> original) {
        boolean isOnline = false;
        try {
            isOnline = isPremium(this.player);
        } catch (Exception e) {
            LOGGER.error("onPlayerSessionWrap: Failed to check premium status for player {}", player.getName().getString(), e);
        }
        if (!isOnline) {
            original.call(packet);
        }
    }
    //?}
}
