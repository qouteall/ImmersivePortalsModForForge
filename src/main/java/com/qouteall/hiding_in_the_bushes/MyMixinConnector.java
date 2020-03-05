package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MyMixinConnector implements IMixinConnector {
    @OnlyIn(Dist.CLIENT)
    public static boolean getIsOptifinePresent() {
        try {
            //do not load other optifine classes that loads vanilla classes
            //that would load the class before mixin
            Class.forName("optifine.ZipResourceProvider");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
    
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
            if (getIsOptifinePresent()) {
                Mixins.addConfiguration(
                    "assets/immersive_portals/immersive_portals.mixins_with_optifine.json"
                );
            }
        }
    }
}
