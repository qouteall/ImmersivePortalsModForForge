package com.qouteall.immersive_portals.network;

import com.qouteall.immersive_portals.Helper;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ModDimension;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ClearableRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StcDimensionInfo {
    
    public static class MyEntry {
        public ResourceLocation stringId;
        public boolean skylight;
        public int registryId;
        public ResourceLocation modDimensionName;
        public PacketBuffer extraData;
        
        public MyEntry(DimensionType type) {
            Validate.isTrue(!type.isVanilla());
            
            registryId = type.getId() + 1;
            stringId = type.getRegistryName();
            modDimensionName = type.getModType().getRegistryName();
            skylight = type.hasSkyLight();
            extraData = new PacketBuffer(Unpooled.buffer());
            type.getModType().write(extraData, true);
        }
        
        public MyEntry(PacketBuffer buffer) {
            registryId = buffer.readInt();
            stringId = buffer.readResourceLocation();
            modDimensionName = buffer.readResourceLocation();
            skylight = buffer.readBoolean();
            extraData = new PacketBuffer(Unpooled.wrappedBuffer(buffer.readByteArray()));
        }
        
        public void encode(PacketBuffer buffer) {
            buffer.writeInt(registryId);
            buffer.writeResourceLocation(stringId);
            buffer.writeResourceLocation(modDimensionName);
            buffer.writeBoolean(skylight);
            buffer.writeByteArray(extraData.array());
        }
        
        @Nullable
        private DimensionType makeDummyDimensionType() {
            final ModDimension modDim = ForgeRegistries.MOD_DIMENSIONS.getValue(modDimensionName);
            
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
                modDim,
                extraData
            );
        }
        
        private boolean isConsistent() {
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
        
        public String myToString() {
            return String.format(
                "(%s,%s,%s)",
                stringId, registryId, modDimensionName
            );
        }
    }
    
    public List<MyEntry> data;
    
    public StcDimensionInfo(List<MyEntry> data) {
        this.data = data;
    }
    
    public StcDimensionInfo(PacketBuffer buffer) {
        int num = buffer.readInt();
        data = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            data.add(new MyEntry(buffer));
        }
    }
    
    public void encode(PacketBuffer buffer) {
        buffer.writeInt(data.size());
        data.forEach(myEntry -> myEntry.encode(buffer));
    }
    
    public static void sendDimensionInfo(ServerPlayerEntity player) {
        NetworkMain.sendToPlayer(
            player,
            new StcDimensionInfo(
                Registry.DIMENSION_TYPE.stream()
                    .filter(dimensionType -> !dimensionType.isVanilla())
                    .map(dimensionType -> new MyEntry(dimensionType))
                    .collect(Collectors.toList())
            )
        );
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        Minecraft.getInstance().execute(this::handleClientOnly);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void handleClientOnly() {
        ClearableRegistry<DimensionType> registry =
            (ClearableRegistry<DimensionType>) Registry.DIMENSION_TYPE;
        
        Helper.log("Received Dimension Info Sync Packet\n" +
            Helper.myToString(data.stream())
        );
        Helper.log("Current Registry Status:\n" + getRegistryInfo());
        
        boolean isAllConsistent = data.stream().allMatch(
            myEntry -> myEntry.isConsistent()
        );
        
        if (isAllConsistent) {
            Helper.log("All Dimension Registry is Consistent");
        }
        else {
            Helper.log("Detected Inconsistency. Started Re-registering");
            
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
                DimensionType object = myEntry.makeDummyDimensionType();
                if (object == null) {
                    Helper.err(
                        "Failed to Re-register Mod Dimension " +
                            myEntry.modDimensionName + " . Skipped."
                    );
                    return;
                }
                registry.register(
                    myEntry.registryId,
                    myEntry.stringId,
                    object
                );
            });
            
            Helper.log("Mod Dimensions Registered:\n" + getRegistryInfo());
            
            boolean isAllConsistentNow = data.stream().allMatch(
                myEntry -> myEntry.isConsistent()
            );
            
            if (!isAllConsistentNow) {
                Helper.err("What? Dimension Registry is Inconsistent after Re-registering???");
            }
        }
        
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
}
