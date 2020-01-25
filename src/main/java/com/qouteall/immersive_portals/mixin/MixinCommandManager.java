package com.qouteall.immersive_portals.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.commands.MyCommandServer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Commands.class)
public class MixinCommandManager {
    @Shadow
    @Final
    private CommandDispatcher<CommandSource> dispatcher;
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void initCommands(boolean isOnServer, CallbackInfo ci) {
        if (!isOnServer) {
            McHelper.initCommandClientOnly(dispatcher);
        }
        MyCommandServer.register(dispatcher);
    }
    
    static {
        SGlobal.isServerMixinApplied = true;
        
        Helper.log("Mixin Applied");
    }
}
