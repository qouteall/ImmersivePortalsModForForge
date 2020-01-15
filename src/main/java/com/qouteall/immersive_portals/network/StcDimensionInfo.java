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

import java.util.Optional;
import java.util.function.Supplier;

public class StcDimensionInfo {
    
    public ResourceLocation stringId;
    public boolean skylight;
    public int registryId;
    public ResourceLocation modDimensionName;
    public PacketBuffer extraData;
    
    public StcDimensionInfo(DimensionType type) {
        registryId = type.getId() + 1;
        stringId = type.getRegistryName();
        modDimensionName = type.getModType().getRegistryName();
        skylight = type.hasSkyLight();
        extraData = new PacketBuffer(Unpooled.buffer());
        type.getModType().write(extraData, true);
    }
    
    public StcDimensionInfo(PacketBuffer buffer) {
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
    
    public static void sendDimensionInfo(ServerPlayerEntity player) {
        Registry.DIMENSION_TYPE.stream()
            .filter(dimensionType -> !dimensionType.isVanilla())
            .forEach(dimensionType -> {
                NetworkMain.sendToPlayer(player, new StcDimensionInfo(dimensionType));
            });
    }
    
    private DimensionType makeDummyDimensionType() {
        final ModDimension modDim = ForgeRegistries.MOD_DIMENSIONS.getValue(modDimensionName);
        
        if (modDim == null) {
            throw new IllegalStateException(
                "Mod Dimension Not Registered " + modDimensionName
            );
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
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        Minecraft.getInstance().execute(this::handleClientOnly);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void handleClientOnly() {
        ClearableRegistry<DimensionType> registry =
            (ClearableRegistry<DimensionType>) Registry.DIMENSION_TYPE;
        
        if (isConsistent()) {
            Helper.log(String.format(
                "Dimension Type is Consistent %s %s",
                stringId,
                registryId
            ));
        }
        else {
            Helper.log(String.format("Found Inconsistency %s %s", stringId, registryId));
            Helper.log("Current Registry:\n" + getRegistryInfo());
            registry.register(registryId, stringId, makeDummyDimensionType());
            Helper.log("After Correction:\n" + getRegistryInfo());
        }
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
