package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.NetworkMain;
import com.qouteall.immersive_portals.StcUpdateGlobalPortals;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import org.apache.commons.lang3.Validate;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class GlobalPortalStorage extends WorldSavedData {
    public List<GlobalTrackedPortal> data;
    public WeakReference<ServerWorld> world;
    
    public GlobalPortalStorage(String string_1, ServerWorld world_) {
        super(string_1);
        world = new WeakReference<>(world_);
        data = new ArrayList<>();
    }
    
    public static void onPlayerLoggedIn(ServerPlayerEntity player) {
        Helper.getServer().getWorlds().forEach(world -> {
            NetworkMain.sendToPlayer(
                player,
                new StcUpdateGlobalPortals(
                    get(world).write(new CompoundNBT()),
                    world.dimension.getType()
                )
            );
        });
    }
    
    public void onDataChanged() {
        setDirty(true);
        
        StcUpdateGlobalPortals packet = new StcUpdateGlobalPortals(
            get(world.get()).write(new CompoundNBT()),
            world.get().dimension.getType()
        );
        
        Helper.getCopiedPlayerList().forEach(
            player -> NetworkMain.sendToPlayer(player, packet)
        );
    }
    
    public static GlobalPortalStorage get(
        ServerWorld world
    ) {
        return world.getSavedData().getOrCreate(
            () -> new GlobalPortalStorage("global_portal", world),
            "global_portal"
        );
    }
    
    @Override
    public void read(CompoundNBT nbt) {
        ServerWorld currWorld = world.get();
        Validate.notNull(currWorld);
        List<GlobalTrackedPortal> newData1 = getPortalsFromTag(nbt, currWorld);
        
        data = newData1;
    }
    
    public static List<GlobalTrackedPortal> getPortalsFromTag(CompoundNBT nbt, World currWorld) {
        /**{@link CompoundNBT#getType()}*/
        ListNBT listTag = nbt.getList("data", 10);
        
        List<GlobalTrackedPortal> newData1 = new ArrayList<>();
        
        for (int i = 0; i < listTag.size(); i++) {
            CompoundNBT tag = listTag.getCompound(i);
            Entity e = McHelper.readEntity(tag, currWorld);
            if (e instanceof GlobalTrackedPortal) {
                newData1.add(((GlobalTrackedPortal) e));
            }
            else {
                Helper.err("error reading portal" + tag);
            }
        }
        return newData1;
    }
    
    @Override
    public CompoundNBT write(CompoundNBT var1) {
        if (data == null) {
            return var1;
        }
        
        ListNBT listTag = new ListNBT();
        ServerWorld currWorld = world.get();
        Validate.notNull(currWorld);
        
        for (GlobalTrackedPortal portal : data) {
            Validate.isTrue(portal.world == currWorld);
            CompoundNBT e = McHelper.writeEntityWithId(portal);
            listTag.add(e);
        }
        
        var1.put("data", listTag);
        
        return var1;
    }
}
