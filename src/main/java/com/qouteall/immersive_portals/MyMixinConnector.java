package com.qouteall.immersive_portals;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MyMixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration(
            "assets/immersive_portals/immersive_portals.mixins.json"
        );
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Mixins.addConfiguration(
                "assets/immersive_portals/immersive_portals.mixins_client.json"
            );
            if (ModMainClient.getIsOptifinePresent()) {
                Mixins.addConfiguration(
                    "assets/immersive_portals/immersive_portals.mixins_with_optifine.json"
                );
            }
        }
    }
}
