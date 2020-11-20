package com.qouteall.imm_ptl_peripheral.ducks;

import net.minecraft.resources.ResourcePackList;
import net.minecraft.util.datafix.codec.DatapackCodec;

public interface IECreateWorldScreen {
    ResourcePackList portal_getResourcePackManager();
    
    DatapackCodec portal_getDataPackSettings();
}
