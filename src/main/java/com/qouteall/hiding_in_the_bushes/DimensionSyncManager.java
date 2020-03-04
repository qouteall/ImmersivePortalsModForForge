package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEDimensionType;
import com.qouteall.immersive_portals.ducks.IEMinecraftServer;
import com.qouteall.hiding_in_the_bushes.network.NetworkMain;
import com.qouteall.hiding_in_the_bushes.network.StcDimensionInfo;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.FuzzedBiomeMagnifier;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.ModDimension;
import net.minecraftforge.registries.ClearableRegistry;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//Client and server may have different integer ids for the same dimension
//So we need to sync them
public class DimensionSyncManager {
    
    @Nullable
    private static DimensionType createDummyDimensionType(
        PacketBuffer extraData,
        ResourceLocation modDimensionName,
        int registryId,
        boolean skylight
    ) {
        ModDimension modDim = ForgeRegistries.MOD_DIMENSIONS.getValue(modDimensionName);
        
        if (modDim == null) {
            return null;
        }
        modDim.read(extraData, true);
        return new DimensionType(
            registryId,
            "dummy",
            "dummy",
            modDim.getFactory(),
            skylight,
            FuzzedBiomeMagnifier.INSTANCE,
            modDim,
            extraData
        );
    }
    
    private static boolean isConsistent(int registryId, ResourceLocation stringId) {
        ClearableRegistry<DimensionType> registry =
            (ClearableRegistry<DimensionType>) Registry.DIMENSION_TYPE;
        
        DimensionType object = registry.getByValue(registryId);
        if (object == null) {
            return false;
        }
        if (!object.getRegistryName().equals(stringId)) {
            return false;
        }
        if (object.getId() + 1 != registryId) {
            return false;
        }
        Optional<DimensionType> object1 = registry.getValue(stringId);
        if (!object1.isPresent()) {
            return false;
        }
        if (object1.get() != object) {
            return false;
        }
        
        return true;
    }
    
    public static void sendDimensionInfo(ServerPlayerEntity player) {
        NetworkMain.sendToPlayer(
            player,
            new StcDimensionInfo(
                Registry.DIMENSION_TYPE.stream()
                    .filter(dimensionType -> !dimensionType.isVanilla())
                    .map(dimensionType -> new StcDimensionInfo.MyEntry(dimensionType))
                    .collect(Collectors.toList())
            )
        );
    }
    
    public static void handleSyncData(List<StcDimensionInfo.MyEntry> data) {
        Helper.log("Really Handling");
        
        ClearableRegistry<DimensionType> registry =
            (ClearableRegistry<DimensionType>) Registry.DIMENSION_TYPE;
        
        Helper.log("Received Dimension Info Sync Packet\n" +
            Helper.myToString(data.stream())
        );
        Helper.log("Current Registry Status:\n" + getRegistryInfo());
        
        boolean isAllConsistent = data.stream().allMatch(
            myEntry -> isConsistent(myEntry.registryId, myEntry.stringId)
        );
        
        if (isAllConsistent) {
            Helper.log("All Dimension Registry is Consistent");
        }
        else {
            Helper.log("Detected Inconsistency. Re-registering is needed");
            
            reRegisterDimensions(data, registry);
            
            boolean isAllConsistentNow = data.stream().allMatch(
                myEntry -> isConsistent(myEntry.registryId, myEntry.stringId)
            );
            
            if (!isAllConsistentNow) {
                Helper.err("What? Dimension Registry is Inconsistent after Re-registering???");
            }
        }
        
    }
    
    private static void reRegisterDimensions(
        List<StcDimensionInfo.MyEntry> data,
        ClearableRegistry<DimensionType> registry
    ) {
        List<DimensionType> capturedTypeObjects = registry.stream()
            .filter(dimensionType -> !dimensionType.isVanilla())
            .collect(Collectors.toList());
        
        Helper.log(
            "Captured DimensionType Objects:\n" +
                Helper.myToString(capturedTypeObjects.stream())
        );
        
        Helper.log("Started Re-registering");
        
        registry.clear();
        
        registry.register(
            DimensionType.OVERWORLD.getId() + 1,
            DimensionType.OVERWORLD.getRegistryName(),
            DimensionType.OVERWORLD
        );
        
        registry.register(
            DimensionType.THE_NETHER.getId() + 1,
            DimensionType.THE_NETHER.getRegistryName(),
            DimensionType.THE_NETHER
        );
        
        registry.register(
            DimensionType.THE_END.getId() + 1,
            DimensionType.THE_END.getRegistryName(),
            DimensionType.THE_END
        );
        
        Helper.log("Vanilla Dimensions Registered:\n" + getRegistryInfo());
        
        data.forEach(myEntry -> {
            DimensionType foundExistingObject = capturedTypeObjects.stream().filter(
                dimensionType -> dimensionType.getRegistryName().equals(myEntry.stringId)
            ).findAny().orElse(null);
            
            DimensionType object;
            
            if (foundExistingObject == null) {
                object = createDummyDimensionType(
                    myEntry.extraData,
                    myEntry.modDimensionName,
                    myEntry.registryId,
                    myEntry.skylight
                );
                
                Helper.log("Cannot Find Existing DimensionType Object " +
                    myEntry.stringId + ". Created New DimensionType Object."
                );
            }
            else {
                object = foundExistingObject;
                Helper.log("Use Captured Existing DimensionType Object");
            }
            
            if (object == null) {
                Helper.err(
                    "Failed to Re-register Mod Dimension Type " +
                        myEntry.modDimensionName + " . Skipped."
                );
                return;
            }
            registry.register(
                myEntry.registryId,
                myEntry.stringId,
                object
            );
            ((IEDimensionType) object).setRegistryIntegerId(myEntry.registryId);
        });
        
        Helper.log("Mod Dimensions Registered:\n" + getRegistryInfo());
    }
    
    private static String getRegistryInfo() {
        ClearableRegistry<DimensionType> registry =
            (ClearableRegistry<DimensionType>) Registry.DIMENSION_TYPE;
        
        return Helper.myToString(
            registry.stream().map(dimensionType -> String.format(
                "(%s,%s,%s)",
                dimensionType.getRegistryName(),
                dimensionType.getId() + 1,
                dimensionType.getId()
            ))
        );
    }
    
    public static void onDimensionRegisteredAtRuntimeAtServer(
        DimensionType dimensionType
    ) {
        MinecraftServer server = McHelper.getServer();
        
        if (server == null) {
            return;
        }
        
        if (((IEMinecraftServer) server).portal_getAreAllWorldsLoaded()) {
            Helper.log("Noticed New Dimension Being Registered When Game is Running");
            
            McHelper.getCopiedPlayerList().forEach(
                DimensionSyncManager::sendDimensionInfo
            );
            
            Helper.log("Dimension Info Updating Packet Sent");
        }
    }
    
    private static Set<DimensionType> serverSideDimensionTypesBefore;
    
    public static void beforeServerReadDimensionRegistry() {
        serverSideDimensionTypesBefore =
            Registry.DIMENSION_TYPE.stream().collect(Collectors.toSet());
        Helper.log("Collected Registered Dimension Types Before Server Reading Dimension Registry\n" +
            Helper.myToString(serverSideDimensionTypesBefore.stream()));
    }
    
    public static void afterServerReadDimensionRegistry() {
        serverSideDimensionTypesBefore.forEach(dimensionType -> {
            if (dimensionType.isVanilla()) {
                return;
            }
            
            ClearableRegistry<DimensionType> registry =
                (ClearableRegistry<DimensionType>) Registry.DIMENSION_TYPE;
            
            ResourceLocation preCaptureName = dimensionType.getRegistryName();
            
            if (preCaptureName == null) {
                Helper.log("Found Dimension Type with No Pre-Capture Name " + dimensionType.getId());
                return;
            }
            
            if (!registry.containsKey(preCaptureName)) {
                Helper.log("Lost Dimension Type " + preCaptureName);
                int intId = getAvailableIntegerId(registry);
                Helper.log("Got available id " + intId);
                ((IEDimensionType) dimensionType).setRegistryIntegerId(intId);
                registry.register(intId, preCaptureName, dimensionType);
                Helper.log("Registered Lost Dimension Type\n" + getRegistryInfo());
            }
        });
        
        serverSideDimensionTypesBefore = null;
    }
    
    private static int getAvailableIntegerId(ClearableRegistry<DimensionType> registry) {
        OptionalInt first = IntStream.iterate(1, i -> i + 1)
            .filter(i -> registry.getByValue(i) == null)
            .findFirst();
        if (first.isPresent()) {
            return first.getAsInt();
        }
        else {
            throw new IllegalStateException(">_>");
        }
    }
}
