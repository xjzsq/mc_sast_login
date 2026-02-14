package fun.sast.mc.login.mixin;

import com.mojang.brigadier.ParseResults;
import fun.sast.mc.login.utils.PlayerAuth;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    //? if >= 1.20.3 {
    @Inject(method = "execute(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void checkCanUseCommands(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "execute(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void checkCanUseCommands(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfoReturnable<Integer> cir) {
    *///?}
        PlayerAuth player = (PlayerAuth) parseResults.getContext().getSource().getPlayer();
        if (player != null && !player.sastLogin$isAuthenticated()) {
            if (!command.startsWith("bind ")) {
                player.sastLogin$sendAuthMessage();
                //? if >= 1.20.3 {
                ci.cancel();
                //?} else {
                /*cir.setReturnValue(0);
                 *///?}
            }
        }
    }
}
