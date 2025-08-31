package fun.sast.mc.login.mixin;

import fun.sast.mc.login.utils.PlayerAuth;
import net.fabricmc.fabric.mixin.dimension.EntityMixin;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends EntityMixin implements PlayerAuth {
    @Unique
    private final ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

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
        player.sendMessage(Text.literal("您的 UUID 尚未绑定第三方平台 ID。请访问以下链接进行绑定：").append(Text.literal(oauthBaseUrl + player.getUuid().toString())
                .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, oauthBaseUrl + player.getUuid().toString()))
                        .withUnderline(true))), false);
    }

    @Override
    public void sastLogin$sendAuthOKMessage() {
        player.sendMessage(Text.literal("绑定成功！Enjoy it~"));
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
}