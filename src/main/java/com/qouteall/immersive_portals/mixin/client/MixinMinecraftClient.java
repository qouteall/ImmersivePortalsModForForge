package com.qouteall.immersive_portals.mixin.client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.network.CommonNetwork;
import com.qouteall.immersive_portals.render.CrossPortalEntityRenderer;
import com.qouteall.immersive_portals.render.FPSMonitor;
import com.qouteall.immersive_portals.render.PortalPresentation;
import com.qouteall.immersive_portals.render.context_management.CloudContext;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.profiler.IProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient implements IEMinecraftClient {
    @Final
    @Shadow
    @Mutable
    private Framebuffer framebuffer;
    
    @Shadow
    public Screen currentScreen;
    
    @Mutable
    @Shadow
    @Final
    public WorldRenderer worldRenderer;
    
    @Shadow
    private static int debugFPS;
    
    @Shadow
    public abstract IProfiler getProfiler();
    
    @Shadow
    @Nullable
    public ClientWorld world;
    
    @Mutable
    @Shadow
    @Final
    private RenderTypeBuffers renderTypeBuffers;
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;runTick()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;tick(Ljava/util/function/BooleanSupplier;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterClientTick(CallbackInfo ci) {
        ModMain.postClientTickSignal.emit();
        
        CGlobal.clientTeleportationManager.manageTeleportation(0);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;runGameLoop(Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/profiler/Snooper;addMemoryStatsToSnooper()V"
        )
    )
    private void onSnooperUpdate(boolean tick, CallbackInfo ci) {
        FPSMonitor.updateEverySecond(debugFPS);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;updateWorldRenderer(Lnet/minecraft/client/world/ClientWorld;)V",
        at = @At("HEAD")
    )
    private void onSetWorld(ClientWorld clientWorld_1, CallbackInfo ci) {
        CGlobal.clientWorldLoader.cleanUp();
        CGlobal.clientTeleportationManager.disableTeleportFor(40);
        CrossPortalEntityRenderer.cleanUp();
        PortalPresentation.cleanup();
        CloudContext.cleanup();
    }
    
    //avoid messing up rendering states in fabulous
    @Inject(method = "Lnet/minecraft/client/Minecraft;func_238218_y_()Z", at = @At("HEAD"), cancellable = true)
    private static void onIsFabulousGraphicsOrBetter(CallbackInfoReturnable<Boolean> cir) {
        if (RenderInfo.isRendering()) {
            cir.setReturnValue(false);
        }
    }
    
    // when processing redirected message, a mod packet processing may call execute()
    // then the task gets delayed. keep the hacky redirect after delaying
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;wrapTask(Ljava/lang/Runnable;)Ljava/lang/Runnable;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCreateTask(Runnable runnable, CallbackInfoReturnable<Runnable> cir) {
        Minecraft this_ = (Minecraft) (Object) this;
        if (this_.isOnExecutionThread()) {
            if (CommonNetwork.getIsProcessingRedirectedMessage()) {
                ClientWorld currWorld = this_.world;
                Runnable newRunnable = () -> {
                    CommonNetwork.withSwitchedWorld(currWorld, runnable);
                };
                cir.setReturnValue(newRunnable);
            }
        }
    }

//    @Inject(
//        method = "render",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/MinecraftClient;runTasks()V",
//            shift = At.Shift.AFTER
//        )
//    )
//    private void onRunTasks(boolean tick, CallbackInfo ci) {
//        getProfiler().push("portal_networking");
//        ClientNetworkingTaskList.processClientNetworkingTasks();
//        getProfiler().pop();
//    }
    
    @Override
    public void setFrameBuffer(Framebuffer buffer) {
        framebuffer = buffer;
    }
    
    @Override
    public Screen getCurrentScreen() {
        return currentScreen;
    }
    
    @Override
    public void setWorldRenderer(WorldRenderer r) {
        worldRenderer = r;
    }
    
    @Override
    public void setBufferBuilderStorage(RenderTypeBuffers arg) {
        renderTypeBuffers = arg;
    }
}
