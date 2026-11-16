package tech.thatgravyboat.rewardclaim.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.thatgravyboat.rewardclaim.Initializer;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Accessor("screen")
    public abstract Screen rewardclaim$getScreen();

    @Inject(method = "openScreen", at = @At("HEAD"), cancellable = true)
    private void rewardclaim$openScreen(Screen screen, CallbackInfo ci) {
        if (Initializer.INSTANCE.shouldCancelScreen(((MinecraftMixin) (Object) this).rewardclaim$getScreen(), screen)) {
            ci.cancel();
        }
    }
}
