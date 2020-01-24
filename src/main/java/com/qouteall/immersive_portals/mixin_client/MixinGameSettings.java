package com.qouteall.immersive_portals.mixin_client;

import net.minecraft.client.GameSettings;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameSettings.class)
public abstract class MixinGameSettings {
//    private static String key = "compatibilityPortalRender";
//
//    @Shadow
//    protected abstract CompoundNBT dataFix(CompoundNBT nbt);
//
//    @Redirect(
//        method = "loadOptions",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/GameSettings;dataFix(Lnet/minecraft/nbt/CompoundNBT;)Lnet/minecraft/nbt/CompoundNBT;"
//        )
//    )
//    CompoundNBT redirectDataFix(GameSettings gameSettings, CompoundNBT nbt) {
//        if (nbt.contains(key)) {
//            boolean useCompatibilityRenderer = nbt.getString(key).equals("true");
//            if (useCompatibilityRenderer) {
//                CGlobal.renderMode = CGlobal.RenderMode.compatibility;
//                Helper.log("Initially Switched to Compatibility Render Mode");
//            }
//        }
//        return dataFix(nbt);
//    }
//
//    @Redirect(
//        method = "saveOptions",
//        at = @At(
//            value = "INVOKE",
//            target = "Ljava/io/PrintWriter;println(Ljava/lang/String;)V",
//            ordinal = 0
//        )
//    )
//    private void onSaveOptions(PrintWriter printWriter, String x) {
//        boolean option = CGlobal.renderMode == CGlobal.RenderMode.normal;
//        printWriter.println(key + ":" + option);
//        printWriter.println(x);
//    }
}
