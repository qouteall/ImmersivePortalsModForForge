package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.ShaderPackDefault;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Shaders.class)
public class MixinShaders_DimensionRedirect {
    
    @Shadow(remap = false)
    private static ClientWorld currentWorld;
    
    @Shadow(remap = false)
    private static IShaderPack shaderPack;
    
    @Inject(method = "init", at = @At("HEAD"), remap = false)
    private static void onInit(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        RegistryKey<World> currDimension = client.world.func_234923_W_();
        
        Helper.log("Shader init " + currDimension);
        
        if (RenderDimensionRedirect.isNoShader(currentWorld.func_234923_W_())) {
            shaderPack = new ShaderPackDefault();
            Helper.log("Set to internal shader");
        }
    }
    
    //redirect dimension for shadow camera
    @Redirect(
        method = "setCameraShadow",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;world:Lnet/minecraft/client/world/ClientWorld;",
            remap = true
        ),
        remap = false
    )
    private static ClientWorld redirectWorldForShadowCamera(Minecraft client) {
        return ClientWorldLoader.getWorld(RenderDimensionRedirect.getRedirectedDimension(
                client.world.func_234923_W_()
            ));
    }
    
    @Redirect(
        method = "beginRender",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;world:Lnet/minecraft/client/world/ClientWorld;",
            ordinal = 1,
            remap = true
        ),
        remap = false
    )
    private static ClientWorld redirectWorldInBeginRender(Minecraft client) {
        return ClientWorldLoader.getWorld(RenderDimensionRedirect.getRedirectedDimension(
                client.world.func_234923_W_()
            ));
    }
    
}
