package com.qouteall.imm_ptl_peripheral.guide;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.network.CommonNetworkClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public class IPGuide {
    public static class GuideInfo {
        public boolean wikiInformed = false;
        public boolean portalHelperInformed = false;
        
        public GuideInfo() {}
    }
    
    private static GuideInfo readFromFile() {
        File storageFile = getStorageFile();
        
        if (storageFile.exists()) {
            
            GuideInfo result = null;
            try (FileReader fileReader = new FileReader(storageFile)) {
                result = Global.gson.fromJson(fileReader, GuideInfo.class);
            }
            catch (IOException e) {
                e.printStackTrace();
                return new GuideInfo();
            }
            
            if (result == null) {
                return new GuideInfo();
            }
            
            return result;
        }
        
        return new GuideInfo();
    }
    
    private static File getStorageFile() {
        return new File(Minecraft.getInstance().gameDir, "imm_ptl_state.json");
    }
    
    private static void writeToFile(GuideInfo guideInfo) {
        try (FileWriter fileWriter = new FileWriter(getStorageFile())) {
            
            Global.gson.toJson(guideInfo, fileWriter);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static GuideInfo guideInfo = new GuideInfo();
    
    public static void initClient() {
        guideInfo = readFromFile();
        
        CommonNetworkClient.clientPortalSpawnSignal.connect(p -> {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            
            if (!guideInfo.wikiInformed) {
                if (player != null && player.isCreative()) {
                    guideInfo.wikiInformed = true;
                    writeToFile(guideInfo);
                    informWithURL(
                        "https://qouteall.fun/immptl/wiki/Portal-Customization",
                        new TranslationTextComponent("imm_ptl.inform_wiki")
                    );
                }
            }
        });
    }
    
    public static void onClientPlacePortalHelper() {
        if (!guideInfo.portalHelperInformed) {
            guideInfo.portalHelperInformed = true;
            writeToFile(guideInfo);
            
            informWithURL(
                "https://qouteall.fun/immptl/wiki/Portal-Customization#portal-helper-block",
                new TranslationTextComponent("imm_ptl.inform_portal_helper")
            );
        }
    }
    
    private static void informWithURL(String link, TranslationTextComponent text) {
        Minecraft.getInstance().ingameGUI.func_238450_a_(
            ChatType.SYSTEM,
            text.func_230529_a_(
                new StringTextComponent(link).func_240700_a_(
                    style -> style.func_240715_a_(new ClickEvent(
                        ClickEvent.Action.OPEN_URL, link
                    )).func_244282_c(true)
                )
            ),
            Util.field_240973_b_
        );
    }
}
