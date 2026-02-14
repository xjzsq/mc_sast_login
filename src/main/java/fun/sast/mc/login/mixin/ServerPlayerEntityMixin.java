package fun.sast.mc.login.mixin;

import fun.sast.mc.login.utils.PlayerAuth;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends EntityMixin implements PlayerAuth {
    @Unique
    private final ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

    @Unique
    private static long lastSendAuthMessage = 0;

    @Unique
    private boolean isAuthenticated = false;

    @Unique
    private final String oauthBaseUrl = "https://mc.01z.cc/oauth?uuid=";

    @Override
    public boolean sastLogin$isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public void sastLogin$setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    @Override
    public void sastLogin$sendAuthMessage() {
        if (System.nanoTime() >= lastSendAuthMessage + 10L * 1000 * 1000000) {
            player.sendMessage(Text.literal("您的 UUID 尚未验证第三方平台 ID，请访问以下链接进行验证：").append(Text.literal(sastLogin$getAuthURL())
                    .styled(style -> style.withClickEvent(
                                    //? if < 1.21.5 {
                                    /*new ClickEvent(ClickEvent.Action.OPEN_URL, sastLogin$getAuthURL()))
                                     *///?} else {
                                    new ClickEvent.OpenUrl(sastLogin$getAuthURI()))
                            //?}
                            .withUnderline(true))), false);
            lastSendAuthMessage = System.nanoTime();
        }

    }

    @Override
    public void sastLogin$sendAuthOKMessage() {
        player.sendMessage(Text.literal("验证成功！Enjoy your game~"));
    }

    public void sastLogin$sendPremiumAuthOKMessage() {
        player.sendMessage(Text.literal("验证并绑定成功！Enjoy your game~"));
        player.sendMessage(Text.literal("可通过 /migrate <离线用户名> 命令迁移离线数据到当前账号"));
    }

    @Inject(method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V", at = @At("RETURN"))
    private void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        PlayerAuth oldPlayerAuth = (PlayerAuth) oldPlayer;
        PlayerAuth newPlayerAuth = (PlayerAuth) player;
        newPlayerAuth.sastLogin$setAuthenticated(oldPlayerAuth.sastLogin$isAuthenticated());
        if (!newPlayerAuth.sastLogin$isAuthenticated()) {
            player.changeGameMode(GameMode.SPECTATOR);
        }
    }

    @Unique
    private String sastLogin$getAuthURL() {
        return oauthBaseUrl + player.getUuid().toString();
    }

    @Unique
    private URI sastLogin$getAuthURI() {
        return URI.create(sastLogin$getAuthURL());
    }
}