package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMainClient;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.resources.IReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = EntityRendererManager.class)
public class MixinEntityRenderManager {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onEntityRenderDispatcherInit(
        TextureManager p_i50971_1_,
        ItemRenderer p_i50971_2_,
        IReloadableResourceManager p_i50971_3_,
        CallbackInfo ci
    ) {
        ModMainClient.initRenderers((EntityRendererManager) (Object) this);
    }
    
    @Inject(
        method = "shouldRender",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onShouldRenderEntity(
        Entity entity_1,
        ICamera visibleRegion_1,
        double double_1,
        double double_2,
        double double_3,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!CGlobal.renderer.shouldRenderEntityNow(entity_1)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
    
    @Inject(
        method = "renderEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;doRender(Lnet/minecraft/entity/Entity;DDDFF)V"
        )
    )
    private void onBeforeReallyRenderEntity(
        Entity entityIn,
        double x,
        double y,
        double z,
        float yaw,
        float partialTicks,
        boolean p_188391_10_,
        CallbackInfo ci
    ) {
        if (MyRenderHelper.isRenderingMirror()) {
            GlStateManager.disableCull();
        }
    }
}
