package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.exposer.IEPlayerListEntry;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.world.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NetworkPlayerInfo.class, remap = false)
public class MixinNetworkPlayerInfo implements IEPlayerListEntry {
    @Shadow
    private GameType gameType;
    
    @Override
    public void setGameMode(GameType mode) {
        gameType = mode;
    }
}
