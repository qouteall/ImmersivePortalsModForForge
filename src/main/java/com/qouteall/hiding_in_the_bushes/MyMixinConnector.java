package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
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
        Mixins.addConfiguration(
            "assets/immersive_portals/immersive_portals.mixins_ma.json"
        );
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Mixins.addConfiguration(
                "assets/immersive_portals/immersive_portals.mixins_client.json"
            );
            Mixins.addConfiguration(
                "assets/immersive_portals/immersive_portals.mixins_ma_client.json"
            );
            boolean result;
            try {
                //do not load other optifine classes that loads vanilla classes
                //that would load the class before mixin
                Class.forName("optifine.ZipResourceProvider");
                result = true;
            }
            catch (ClassNotFoundException e) {
                result = false;
            }
            if (result) {
                Mixins.addConfiguration(
                    "assets/immersive_portals/immersive_portals.mixins_with_optifine.json"
                );
            }
        }
    }
}
