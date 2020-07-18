package com.qouteall.immersive_portals.dimension_sync;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.fabricmc.fabric.impl.registry.sync.RemappableRegistry;
import net.fabricmc.fabric.mixin.registry.sync.MixinLevelStorageSession;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.storage.SaveFormat;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class DimensionIdManagement {
    private static Field fabric_activeTag_field;
    
    public static void onServerStarted() {
        DimensionIdRecord ipRecord = readIPDimensionRegistry();
        if (ipRecord == null) {
            Helper.log("Immersive Portals' dimension id record is missing");
            
            DimensionIdRecord fabricRecord = getFabricRecord();
            
            if (fabricRecord != null) {
                Helper.log("Found Fabric's dimension id record");
                Helper.log("\n" + fabricRecord);
                
                DimensionIdRecord.serverRecord = fabricRecord;
                fabricRecord = null;
            }
            else {
                Helper.log("Cannot retrieve Fabric's dimension id record.");
                Helper.log("If this is not a newly created world," +
                    " existing portal data may be corrupted!!!"
                );
                DimensionIdRecord.serverRecord = null;
            }
        }
        else {
            DimensionIdRecord.serverRecord = ipRecord;
            Helper.log("Successfully read IP's dimension id record");
        }
        
        completeServerIdRecord();
        
        try {
            File file = getIPDimIdFile();
            
            FileOutputStream fileInputStream = new FileOutputStream(file);
            
            CompoundNBT tag = DimensionIdRecord.recordToTag(DimensionIdRecord.serverRecord);
            
            CompressedStreamTools.writeCompressed(tag, fileInputStream);
            
            Helper.log("Dimension Id Info Saved to File");
        }
        catch (IOException e) {
            throw new RuntimeException(
                "Cannot Save Immersive Portals Dimension Id Info", e
            );
        }
    }
    
    //return null for failed
    private static DimensionIdRecord readIPDimensionRegistry() {
        File dataFile = getIPDimIdFile();
        
        if (!dataFile.exists()) {
            Helper.log("Immersive Portals' Dimension Id Record File Does Not Exist");
            return null;
        }
        
        try {
            FileInputStream fileInputStream = new FileInputStream(dataFile);
            CompoundNBT tag = CompressedStreamTools.readCompressed(fileInputStream);
            fileInputStream.close();
            
            return DimensionIdRecord.tagToRecord(tag);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static File getIPDimIdFile() {
        MinecraftServer server = McHelper.getServer();
        Validate.notNull(server);
        Path saveDir = server.anvilConverterForAnvilFile.field_237279_c_;
        return new File(new File(saveDir.toFile(), "data"), "imm_ptl_dim_reg.dat");
    }
    
    private static void completeServerIdRecord() {
        if (DimensionIdRecord.serverRecord == null) {
            Helper.log("Dimension Id Record is Missing");
            DimensionIdRecord.serverRecord = new DimensionIdRecord(HashBiMap.create());
        }
        
        Helper.log("Start Completing Dimension Id Record");
        Helper.log("Before:\n" + DimensionIdRecord.serverRecord);
        
        Set<RegistryKey<World>> keys = McHelper.getServer().func_240770_D_();
        
        BiMap<RegistryKey<World>, Integer> bimap = DimensionIdRecord.serverRecord.idMap;
        
        if (!bimap.containsKey(World.field_234918_g_)) {
            bimap.put(World.field_234918_g_, 0);
        }
        if (!bimap.containsKey(World.field_234919_h_)) {
            bimap.put(World.field_234919_h_, -1);
        }
        if (!bimap.containsKey(World.field_234920_i_)) {
            bimap.put(World.field_234920_i_, 1);
        }
        
        List<RegistryKey<World>> keysList = new ArrayList<>(keys);
        keysList.sort(Comparator.comparing(RegistryKey::toString));
        
        Helper.log("Server Loaded Dimensions:\n" + Helper.myToString(keysList.stream()));
        
        keysList.forEach(dim -> {
            if (!bimap.containsKey(dim)) {
                int newid = bimap.values().stream().mapToInt(i -> i).max().orElse(1) + 1;
                bimap.put(dim, newid);
            }
        });
        
        Helper.log("After:\n" + DimensionIdRecord.serverRecord);
    }
    
    /**
     * {@link MixinLevelStorageSession}
     */
    @Nullable
    private static DimensionIdRecord getFabricRecord() {
        if (O_O.isForge()) {
            return null;
        }
        
        try {
            if (fabric_activeTag_field == null) {
                fabric_activeTag_field = SaveFormat.LevelSave.class.getDeclaredField("fabric_activeTag");
                fabric_activeTag_field.setAccessible(true);
            }
            
            SaveFormat.LevelSave session = McHelper.getServer().anvilConverterForAnvilFile;
            
            CompoundNBT tag = (CompoundNBT) fabric_activeTag_field.get(session);
            
            if (tag == null) {
                return null;
            }
            
            return readIdsFromFabricRegistryRecord(tag);
        }
        catch (Throwable e) {
            throw new RuntimeException(
                "Cannot get Fabric registry info", e
            );
        }
    }
    
    /**
     * {@link RegistrySyncManager#apply(CompoundTag, RemappableRegistry.RemapMode)}
     */
    private static DimensionIdRecord readIdsFromFabricRegistryRecord(CompoundNBT fabricRegistryRecord) {
        CompoundNBT dimensionTypeTag = fabricRegistryRecord.getCompound("minecraft:dimension_type");
        
        if (dimensionTypeTag.isEmpty()) {
            Helper.err("Missing 'minecraft:dimension_type' " + fabricRegistryRecord);
            return null;
        }
        
        HashBiMap<RegistryKey<World>, Integer> bimap = HashBiMap.create();
        
        dimensionTypeTag.keySet().forEach(dim -> {
            INBT t = dimensionTypeTag.get(dim);
            if (t instanceof IntNBT) {
                int data = ((IntNBT) t).getInt();
                bimap.put(DimId.idToKey(dim), data - 1);
            }
            else {
                Helper.err(String.format(
                    "Non-int tag in fabric registry data %s %s %s", t, dim, fabricRegistryRecord
                ));
            }
        });
        
        return new DimensionIdRecord(bimap);
    }
}
