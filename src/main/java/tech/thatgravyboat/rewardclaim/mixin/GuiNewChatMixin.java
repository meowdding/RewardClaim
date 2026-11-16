package tech.thatgravyboat.rewardclaim.mixin;

import net.minecraft.client.gui.chat.ChatGui;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.thatgravyboat.rewardclaim.Initializer;
import tech.thatgravyboat.rewardclaim.remote.RemoteData;

@Mixin(ChatGui.class)
public abstract class GuiNewChatMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void rewardclaim$addChatMessage(Text message, CallbackInfo ci) {
        if (RemoteData.disabled) return;

        if (Initializer.INSTANCE.onMessage(message.getString())) {
            ci.cancel();
        }
    }
}
