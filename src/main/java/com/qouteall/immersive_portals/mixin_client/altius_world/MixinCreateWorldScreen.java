package com.qouteall.immersive_portals.mixin_client.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.altius_world.AltiusScreen;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldOptionsScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.server.IDynamicRegistries;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen extends Screen {
    @Shadow
    public abstract void func_231164_f_();
    
    private Button altiusButton;
    private AltiusScreen altiusScreen;
    
    protected MixinCreateWorldScreen(ITextComponent title) {
        super(title);
        throw new RuntimeException();
    }
    
    @Inject(
        method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/gui/screen/WorldOptionsScreen;)V",
        at = @At("RETURN")
    )
    private void onConstructEnded(
        Screen screen,
        WorldOptionsScreen moreOptionsDialog,
        CallbackInfo ci
    ) {
        altiusScreen = new AltiusScreen((CreateWorldScreen) (Object) this);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screen/CreateWorldScreen;func_231160_c_()V",
        at = @At("HEAD")
    )
    private void onInitEnded(CallbackInfo ci) {
        
        altiusButton = (Button) this.func_230480_a_(new Button(
            field_230708_k_ / 2 + 5, 151, 150, 20,
            new TranslationTextComponent("imm_ptl.altius_screen_button"),
            (buttonWidget) -> {
                openAltiusScreen();
            }
        ));
        altiusButton.field_230694_p_ = false;
        
    }
    
    @Inject(
        method = "setMoreOptionsOpen(Z)V",
        at = @At("RETURN")
    )
    private void onMoreOptionsOpen(boolean moreOptionsOpen, CallbackInfo ci) {
        if (moreOptionsOpen) {
            altiusButton.field_230694_p_ = true;
        }
        else {
            altiusButton.field_230694_p_ = false;
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/gui/screen/CreateWorldScreen;createWorld()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;func_238192_a_(Ljava/lang/String;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/server/IDynamicRegistries$Impl;Lnet/minecraft/world/gen/settings/DimensionGeneratorSettings;)V"
        )
    )
    private void redirectOnCreateLevel(
        Minecraft client,
        String string,
        WorldSettings levelInfo,
        IDynamicRegistries.Impl modifiable,
        DimensionGeneratorSettings generatorOptions
    ) {
        AltiusInfo info = altiusScreen.getAltiusInfo();
        ((IELevelProperties) (Object) levelInfo).setAltiusInfo(info);
        
        client.func_238192_a_(string, levelInfo, modifiable, generatorOptions);
    }
    
    private void openAltiusScreen() {
        Minecraft.getInstance().displayGuiScreen(altiusScreen);
    }
}
