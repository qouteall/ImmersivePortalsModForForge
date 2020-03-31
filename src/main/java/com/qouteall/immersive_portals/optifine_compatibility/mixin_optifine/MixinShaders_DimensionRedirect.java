package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType;
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
        Minecraft mc = Minecraft.getInstance();
        DimensionType currDimension = mc.world.dimension.getType();
        
        Helper.log("Shader init " + currDimension);
        
        if (RenderDimensionRedirect.isNoShader(currentWorld.dimension.getType())) {
            shaderPack = new ShaderPackDefault();
            Helper.log("Set to internal shader");
        }
    }
    
    @Redirect(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/dimension/DimensionType;getId()I",
            remap = true
        ),
        remap = false
    )
    private static int redirectGetDimensionRawId(DimensionType dimensionType) {
        return RenderDimensionRedirect.getRedirectedDimension(dimensionType).getId();
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
        return CGlobal.clientWorldLoader.getWorld(
            RenderDimensionRedirect.getRedirectedDimension(
                client.world.getDimension().getType()
            )
        );
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
        return CGlobal.clientWorldLoader.getWorld(
            RenderDimensionRedirect.getRedirectedDimension(
                client.world.getDimension().getType()
            )
        );
    }
}
