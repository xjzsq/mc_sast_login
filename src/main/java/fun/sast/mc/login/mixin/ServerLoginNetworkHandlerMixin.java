package fun.sast.mc.login.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Uuids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fun.sast.mc.login.Sast_login.MOD_ID;
import static fun.sast.mc.login.integrations.MojangApi.getUuid;


@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Shadow
    public GameProfile profile;

    @Shadow
    private ServerLoginNetworkHandler.State state;

    @Final
    @Shadow
    MinecraftServer server;

    @Unique
    private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

    @Inject(
            method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.20.2 {
                    target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"
                    //?} else {
                    /*target = "Lcom/mojang/authlib/GameProfile;<init>(Ljava/util/UUID;Ljava/lang/String;)V",
                    shift = At.Shift.AFTER,
                    remap = false
                    *///?}
            ),
            cancellable = true
    )
    private void checkPremium(LoginHelloC2SPacket packet, CallbackInfo ci) {
        String username = packet.name();

        LOGGER.info("UUID of player {} is {}", username, packet.profileId());

        try {
            Matcher matcher = pattern.matcher(username);
            if (!matcher.matches()) {
                LOGGER.info("Player {} doesn't have a valid username for Mojang account", username);
                state = getReadyState();

                this.profile = getGameProfile(packet.name());

                ci.cancel();
            } else {
                UUID onlineUuid = getUuid(username);
                if (checkUuid(packet.profileId(), onlineUuid)) {
                    LOGGER.info("Player {} is already online", username);
                } else {
                    if (onlineUuid == null) {
                        LOGGER.info("Player {} doesn't have a Mojang account", username);
                    } else {
                        LOGGER.info("Player {} has a Mojang account, but UUID mismatch: expected {}, got {}", username, onlineUuid, packet.profileId());
                        LOGGER.info("UUID of player {} is {}", username, UUID.nameUUIDFromBytes((username).getBytes()));
                    }
                    state = getReadyState();

                    this.profile = getGameProfile(packet.name());

                    ci.cancel();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to check mojang account for Mojang account", e);
        }

    }

    @Unique
    private GameProfile getGameProfile(String name) {
        //? if < 1.20.2 {
        /*return new GameProfile(null, name);
        *///? } else {
         return new GameProfile(Uuids.getOfflinePlayerUuid(name), name);
         //?}
    }

    @Unique
    private ServerLoginNetworkHandler.State getReadyState() {
        //? if < 1.20.2 {
        /*return ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
        *///? } else {
         return ServerLoginNetworkHandler.State.VERIFYING;
         //?}
    }

    @Unique
    private boolean checkUuid(UUID uuid, UUID onlineUuid) {
        return uuid.equals(onlineUuid);
    }

    @Unique
    private boolean checkUuid(Optional<UUID> uuid, UUID onlineUuid) {
        return uuid.isPresent() && uuid.get().equals(onlineUuid);
    }
}