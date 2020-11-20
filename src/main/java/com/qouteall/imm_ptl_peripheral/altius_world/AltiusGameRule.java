package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.world.GameRules;

public class AltiusGameRule {
    public static GameRules.RuleKey<GameRules.BooleanValue> dimensionStackKey;
    
    
    private static boolean doUpgradeOldDimensionStack = false;
    
    public static void init() {
        dimensionStackKey = GameRules.func_234903_a_(
            "ipDimensionStack",
            GameRules.Category.MISC,
            GameRules.BooleanValue.create(false)
        );
        
        ModMain.postServerTickSignal.connect(AltiusGameRule::serverTick);
    }
    
    private static void serverTick() {
        if (doUpgradeOldDimensionStack) {
            setIsDimensionStack(true);
            AltiusInfo.removeAltius();
            doUpgradeOldDimensionStack = false;
            Helper.log("Upgraded old dimension stack info");
        }
    }
    
    public static boolean getIsDimensionStack() {
        return McHelper.getServer().getGameRules().get(dimensionStackKey).get();
    }
    
    public static void setIsDimensionStack(boolean cond) {
        McHelper.getServer().getGameRules()
            .get(dimensionStackKey).set(cond, McHelper.getServer());
    }
    
    public static void upgradeOldDimensionStack() {
        doUpgradeOldDimensionStack = true;
    }
}
