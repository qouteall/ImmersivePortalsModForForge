package com.qouteall.immersive_portals.mixin_client.altius_world;

import com.google.gson.JsonElement;
import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.altius_world.AltiusScreen;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public class MixinCreateWorldScreen extends Screen {
    private Button altiusButton;
    private AltiusScreen altiusScreen;
    
    protected MixinCreateWorldScreen(ITextComponent title) {
        super(title);
        throw new RuntimeException();
    }
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onConstructEnded(Screen parent, CallbackInfo ci) {
        altiusScreen = new AltiusScreen((Screen) (Object) this);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screen/CreateWorldScreen;init(Lnet/minecraft/client/Minecraft;II)V",
        at = @At("HEAD")
    )
    private void onInitEnded(CallbackInfo ci) {
        
        altiusButton = (Button) this.addButton(new Button(
            this.width / 2 - 75, 187 - 25, 150, 20,
            I18n.format("imm_ptl.altius_screen_button"),
            (buttonWidget) -> {
                openAltiusScreen();
            }
        ));
        altiusButton.visible = true;
    
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screen/CreateWorldScreen;showMoreWorldOptions(Z)V",
        at = @At("RETURN")
    )
    private void onMoreOptionsOpen(boolean moreOptionsOpen, CallbackInfo ci) {
        if (moreOptionsOpen) {
            altiusButton.visible = false;
        }
        else {
            altiusButton.visible = true;
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/gui/screen/CreateWorldScreen;createWorld()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/WorldSettings;setGeneratorOptions(Lcom/google/gson/JsonElement;)Lnet/minecraft/world/WorldSettings;"
        )
    )
    private WorldSettings redirectOnCreateLevel(
        WorldSettings levelInfo, JsonElement generatorOptions
    ) {
        AltiusInfo info = altiusScreen.getAltiusInfo();
        ((IELevelProperties) (Object) levelInfo).setAltiusInfo(info);
    
        return levelInfo.setGeneratorOptions(generatorOptions);
    }
    
    private void openAltiusScreen() {
        Minecraft.getInstance().displayGuiScreen(altiusScreen);
    }
}
