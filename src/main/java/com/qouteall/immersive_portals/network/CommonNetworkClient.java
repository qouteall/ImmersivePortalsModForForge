package com.qouteall.immersive_portals.network;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.my_util.SignalArged;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class CommonNetworkClient {
    
    public static final SignalArged<Portal> clientPortalSpawnSignal = new SignalArged<>();
    
    public static final Minecraft client = Minecraft.getInstance();
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    static boolean isProcessingRedirectedMessage = false;
    
    
    public static void processRedirectedPacket(RegistryKey<World> dimension, IPacket packet) {
        Runnable func = () -> {
            try {
                client.getProfiler().startSection("process_redirected_packet");
                
                ClientWorld packetWorld = ClientWorldLoader.getWorld(dimension);
                
                doProcessRedirectedMessage(packetWorld, packet);
            }
            finally {
                client.getProfiler().endSection();
            }
        };
        
        CHelper.executeOnRenderThread(func);
    }
    
    
    public static void doProcessRedirectedMessage(
        ClientWorld packetWorld,
        IPacket packet
    ) {
        boolean oldIsProcessing = isProcessingRedirectedMessage;
        
        isProcessingRedirectedMessage = true;
        
        ClientPlayNetHandler netHandler = ((IEClientWorld) packetWorld).getNetHandler();
        
        if ((netHandler).getWorld() != packetWorld) {
            ((IEClientPlayNetworkHandler) netHandler).setWorld(packetWorld);
            Helper.err("The world field of client net handler is wrong");
        }
        
        client.getProfiler().startSection(() -> {
            return "handle_redirected_packet" + packetWorld.func_234923_W_() + packet;
        });
        
        try {
            withSwitchedWorld(packetWorld, () -> packet.processPacket(netHandler));
        }
        catch (Throwable e) {
            limitedLogger.throwException(() -> new IllegalStateException(
                "handling packet in " + packetWorld.func_234923_W_(), e
            ));
        }
        finally {
            client.getProfiler().endSection();
            
            isProcessingRedirectedMessage = oldIsProcessing;
        }
    }
    
    
    public static void withSwitchedWorld(ClientWorld newWorld, Runnable runnable) {
        Validate.isTrue(client.isOnExecutionThread());
        
        ClientWorld originalWorld = client.world;
        WorldRenderer originalWorldRenderer = client.worldRenderer;
        
        WorldRenderer newWorldRenderer = ClientWorldLoader.getWorldRenderer(newWorld.func_234923_W_());
        
        Validate.notNull(newWorldRenderer);
        
        client.world = newWorld;
        ((IEParticleManager) client.particles).mySetWorld(newWorld);
        ((IEMinecraftClient) client).setWorldRenderer(newWorldRenderer);
        
        try {
            runnable.run();
        }
        finally {
            if (client.world != newWorld) {
                Helper.err("Respawn packet should not be redirected");
                originalWorld = client.world;
                originalWorldRenderer = client.worldRenderer;
                throw new RuntimeException("Respawn packet should not be redirected");
            }
            
            client.world = originalWorld;
            ((IEMinecraftClient) client).setWorldRenderer(originalWorldRenderer);
            ((IEParticleManager) client.particles).mySetWorld(originalWorld);
        }
    }
    
    public static void processEntitySpawn(String entityTypeString, int entityId, RegistryKey<World> dim, CompoundNBT compoundTag) {
        Optional<EntityType<?>> entityType = EntityType.byKey(entityTypeString);
        if (!entityType.isPresent()) {
            Helper.err("unknown entity type " + entityTypeString);
            return;
        }
        
        CHelper.executeOnRenderThread(() -> {
            client.getProfiler().startSection("ip_spawn_entity");
            
            ClientWorld world = ClientWorldLoader.getWorld(dim);
            
            Entity entity = entityType.get().create(
                world
            );
            entity.read(compoundTag);
            entity.setEntityId(entityId);
            entity.setPacketCoordinates(entity.getPosX(), entity.getPosY(), entity.getPosZ());
            world.addEntity(entityId, entity);
            
            //do not create client world while rendering or gl states will be disturbed
            if (entity instanceof Portal) {
                ClientWorldLoader.getWorld(((Portal) entity).dimensionTo);
                clientPortalSpawnSignal.emit(((Portal) entity));
            }
            
            client.getProfiler().endSection();
        });
    }
}
