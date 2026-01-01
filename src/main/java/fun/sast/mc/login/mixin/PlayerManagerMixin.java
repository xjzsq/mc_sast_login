package fun.sast.mc.login.mixin;

import com.mojang.authlib.GameProfile;
import fun.sast.mc.login.utils.PlayerAuth;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Inject(method = "sendToAll", at = @At("HEAD"))
    private void onSendToAll(Packet<?> packet, CallbackInfo ci) {
//        if (packet instanceof PlayerListS2CPacket) {
//            PlayerManager playerManager = (PlayerManager) (Object) this;
//            for (ServerPlayerEntity player : playerManager.getPlayerList()) {
//                if (((PlayerAuth) player).sastLogin$isAuthenticated()) {
//                    player.networkHandler.sendPacket(packet);
//                }
//            }
//            ci.cancel();
//        }
    }
}
