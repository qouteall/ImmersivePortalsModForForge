package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class GlobalPortalStorage extends WorldSavedData {
    public List<GlobalTrackedPortal> data;
    public WeakReference<ServerWorld> world;
    private int version = 1;
    private boolean shouldReSync = false;
    
    public static void init() {
        ModMain.postServerTickSignal.connect(() -> {
            McHelper.getServer().getWorlds().forEach(world1 -> {
                GlobalPortalStorage gps = GlobalPortalStorage.get(world1);
                gps.tick();
            });
        });
    }
    
    public GlobalPortalStorage(String string_1, ServerWorld world_) {
        super(string_1);
        world = new WeakReference<>(world_);
        data = new ArrayList<>();
    }
    
    public static void onPlayerLoggedIn(ServerPlayerEntity player) {
        McHelper.getServer().getWorlds().forEach(
            world -> {
                GlobalPortalStorage storage = get(world);
                if (!storage.data.isEmpty()) {
                    player.connection.sendPacket(
                        MyNetwork.createGlobalPortalUpdate(
                            storage
                        )
                    );
                }
            }
        );
        NewChunkTrackingGraph.updateForPlayer(player);
    }
    
    public void onDataChanged() {
        setDirty(true);
        
        shouldReSync = true;
        
        
    }
    
    private void syncToAllPlayers() {
        IPacket packet = MyNetwork.createGlobalPortalUpdate(this);
        McHelper.getCopiedPlayerList().forEach(
            player -> player.connection.sendPacket(packet)
        );
    }
    
    @Override
    public void read(CompoundNBT tag) {
        
        ServerWorld currWorld = world.get();
        Validate.notNull(currWorld);
        List<GlobalTrackedPortal> newData = getPortalsFromTag(tag, currWorld);
        
        if (tag.contains("version")) {
            version = tag.getInt("version");
        }
        
        data = newData;
        
        clearAbnormalPortals();
    }
    
    private static List<GlobalTrackedPortal> getPortalsFromTag(
        CompoundNBT tag,
        World currWorld
    ) {
        /**{@link CompoundTag#getType()}*/
        ListNBT listTag = tag.getList("data", 10);
        
        List<GlobalTrackedPortal> newData = new ArrayList<>();
        
        for (int i = 0; i < listTag.size(); i++) {
            CompoundNBT compoundTag = listTag.getCompound(i);
            GlobalTrackedPortal e = readPortalFromTag(currWorld, compoundTag);
            if (e != null) {
                newData.add(e);
            }
            else {
                Helper.err("error reading portal" + compoundTag);
            }
        }
        return newData;
    }
    
    private static GlobalTrackedPortal readPortalFromTag(World currWorld, CompoundNBT compoundTag) {
        ResourceLocation entityId = new ResourceLocation(compoundTag.getString("entity_type"));
        EntityType<?> entityType = Registry.ENTITY_TYPE.getOrDefault(entityId);
        
        Entity e = entityType.create(currWorld);
        e.read(compoundTag);
        
        if (!(e instanceof GlobalTrackedPortal)) {
            return null;
        }
        
        
        return (GlobalTrackedPortal) e;
    }
    
    @Override
    public CompoundNBT write(CompoundNBT tag) {
        if (data == null) {
            return tag;
        }
        
        ListNBT listTag = new ListNBT();
        ServerWorld currWorld = world.get();
        Validate.notNull(currWorld);
        
        for (GlobalTrackedPortal portal : data) {
            Validate.isTrue(portal.world == currWorld);
            CompoundNBT portalTag = new CompoundNBT();
            portal.writeWithoutTypeId(portalTag);
            portalTag.putString(
                "entity_type",
                EntityType.getKey(portal.getType()).toString()
            );
            listTag.add(portalTag);
        }
        
        tag.put("data", listTag);
        
        tag.putInt("version", version);
        
        return tag;
    }
    
    public static GlobalPortalStorage get(
        ServerWorld world
    ) {
        return world.getSavedData().getOrCreate(
            () -> new GlobalPortalStorage("global_portal", world),
            "global_portal"
        );
    }
    
    public void tick() {
        if (shouldReSync) {
            syncToAllPlayers();
            shouldReSync = false;
        }
        
        if (version <= 1) {
            upgradeData(world.get());
            version = 2;
            setDirty(true);
        }
    }
    
    public void clearAbnormalPortals() {
        data.removeIf(e -> {
            RegistryKey<World> dimensionTo = ((GlobalTrackedPortal) e).dimensionTo;
            if (McHelper.getServer().getWorld(dimensionTo) == null) {
                Helper.err("Missing Dimension for global portal " + dimensionTo.func_240901_a_());
                return true;
            }
            return false;
        });
    }
    
    private static void upgradeData(ServerWorld world) {
        //removed
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void receiveGlobalPortalSync(RegistryKey<World> dimension, CompoundNBT compoundTag) {
        ClientWorld world = CGlobal.clientWorldLoader.getWorld(dimension);
        
        List<GlobalTrackedPortal> oldGlobalPortals = ((IEClientWorld) world).getGlobalPortals();
        if (oldGlobalPortals != null) {
            for (GlobalTrackedPortal p : oldGlobalPortals) {
                p.removed = true;
            }
        }
        
        List<GlobalTrackedPortal> newPortals = getPortalsFromTag(compoundTag, world);
        for (GlobalTrackedPortal p : newPortals) {
            p.removed = false;
        }
        
        ((IEClientWorld) world).setGlobalPortals(newPortals);
        
        Helper.log("Global Portals Updated " + dimension.func_240901_a_());
    }
}
