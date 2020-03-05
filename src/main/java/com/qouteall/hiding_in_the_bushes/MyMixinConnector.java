package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMainClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MyMixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        Helper.log("Invoking Mixin Connector");
        Mixins.addConfiguration(
            "assets/immersive_portals/immersive_portals.mixins.json"
        );
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Mixins.addConfiguration(
                "assets/immersive_portals/immersive_portals.mixins_client.json"
            );
            if (ModMainForge.getIsOptifinePresent()) {
                Mixins.addConfiguration(
                    "assets/immersive_portals/immersive_portals.mixins_with_optifine.json"
                );
            }
        }
    }
}
