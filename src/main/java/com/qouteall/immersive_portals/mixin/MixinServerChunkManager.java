package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.ducks.IEServerChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerChunkProvider.class)
public abstract class MixinServerChunkManager implements IEServerChunkManager {

}
