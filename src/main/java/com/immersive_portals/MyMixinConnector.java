package com.immersive_portals;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MyMixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration("assets/immersive_portals/immersive_portals.mixins.json");
        Mixins.addConfiguration("assets/immersive_portals/immersive_portals.mixins_client.json");
    }
}
