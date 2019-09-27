package com.qouteall.immersive_portals.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.qouteall.immersive_portals.MyCommandClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class MixinCommandManager {
    @Shadow
    @Final
    private CommandDispatcher<CommandSource> dispatcher;
    
    @Inject(
        method = "Lnet/minecraft/server/command/CommandManager;<init>(Z)V",
        at = @At("RETURN")
    )
    private void initCommands(boolean isOnServer, CallbackInfo ci) {
        if (!isOnServer) {
            MyCommandClient.register(dispatcher);
        }
    }
    
}
