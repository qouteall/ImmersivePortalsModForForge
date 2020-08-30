package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MyMixinConnector implements IMixinConnector {
    
    // do not use O_O.detectOptiFine because it will load classes too early
    public static boolean detectOptiFine() {
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
            "imm_ptl_mixins.json"
        );
        Mixins.addConfiguration(
            "imm_ptl_mixins_forge.json"
        );
        Mixins.addConfiguration(
            "imm_ptl_peripheral_mixins.json"
        );
        if (FMLEnvironment.dist == Dist.CLIENT) {
            boolean result = detectOptiFine();
            if (result) {
                Mixins.addConfiguration(
                    "imm_ptl_mixins_optifine.json"
                );
            }
        }
    }
}
