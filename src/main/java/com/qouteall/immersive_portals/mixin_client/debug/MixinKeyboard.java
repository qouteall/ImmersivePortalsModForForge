package com.qouteall.immersive_portals.mixin_client.debug;

import net.minecraft.client.KeyboardListener;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(KeyboardListener.class)
public class MixinKeyboard {
    //fix cannot output when taking screenshot
    void method_1464(ITextComponent text) {
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(text);
    }
}
