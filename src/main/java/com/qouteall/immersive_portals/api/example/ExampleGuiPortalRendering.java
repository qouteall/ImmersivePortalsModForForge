package com.qouteall.immersive_portals.api.example;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.api.PortalAPI;
import com.qouteall.immersive_portals.chunk_loading.ChunkLoader;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.my_util.DQuaternion;
import com.qouteall.immersive_portals.network.McRemoteProcedureCall;
import com.qouteall.immersive_portals.render.GuiPortalRendering;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.context_management.WorldRenderInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.WeakHashMap;

/**
 * <p>
 * Example code about how to use
 * </p>
 * <ul>
 *     <li>GUI Portal API</li>
 *     <li>Chunk Loading API</li>
 *     <li>Remote Procedure Call Utility API</li>
 * </ul>
 *
 * <p>
 *     To test this, use command /portal debug gui_portal minecraft:the_end 0 80 0
 * </p>
 * <p>It involves:</p>
 *
 * <ul>
 *     <li>The server sending the dimension and position to client</li>
 *     <li>The server adding an additional per-player chunk loader so that the chunks near that
 *     position is being generated and synchronized to that player's client</li>
 *     <li>Client opening the GUI portal screen and render the GUI portal.
 *     It controls the camera rotation transformation and camera position.</li>
 *     <li>If the client closes the screen, the server removes the additional chunk loader</li>
 * </ul>
 */
public class ExampleGuiPortalRendering {
    /**
     * The Framebuffer that the GUI portal is going to render onto
     */
    @OnlyIn(Dist.CLIENT)
    private static Framebuffer frameBuffer;
    
    /**
     * A weak hash map storing ChunkLoader objects for each players
     */
    private static final WeakHashMap<ServerPlayerEntity, ChunkLoader>
        chunkLoaderMap = new WeakHashMap<>();
    
    /**
     * Remove the GUI portal chunk loader for a player
     */
    private static void removeChunkLoaderFor(ServerPlayerEntity player) {
        ChunkLoader chunkLoader = chunkLoaderMap.remove(player);
        if (chunkLoader != null) {
            PortalAPI.removeChunkLoaderForPlayer(player, chunkLoader);
        }
    }
    
    public static void onCommandExecuted(ServerPlayerEntity player, ServerWorld world, Vector3d pos) {
        removeChunkLoaderFor(player);
        
        ChunkLoader chunkLoader = new ChunkLoader(
            new DimensionalChunkPos(
                world.func_234923_W_(), new ChunkPos(new BlockPos(pos))
            ),
            8
        );
        
        // Add the per-player additional chunk loader
        PortalAPI.addChunkLoaderForPlayer(player, chunkLoader);
        chunkLoaderMap.put(player, chunkLoader);
        
        // Tell the client to open the screen
        McRemoteProcedureCall.tellClientToInvoke(
            player,
            "com.qouteall.immersive_portals.api.example.ExampleGuiPortalRendering.RemoteCallables.clientActivateExampleGuiPortal",
            world.func_234923_W_(),
            pos
        );
    }
    
    public static class RemoteCallables {
        @OnlyIn(Dist.CLIENT)
        public static void clientActivateExampleGuiPortal(
            RegistryKey<World> dimension,
            Vector3d position
        ) {
            if (frameBuffer == null) {
                frameBuffer = new Framebuffer(1000, 1000, true, true);
            }
            
            Minecraft.getInstance().displayGuiScreen(new GuiPortalScreen(dimension, position));
        }
        
        public static void serverRemoveChunkLoader(ServerPlayerEntity player) {
            removeChunkLoaderFor(player);
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    public static class GuiPortalScreen extends Screen {
        
        private final RegistryKey<World> viewingDimension;
        
        private final Vector3d viewingPosition;
        
        public GuiPortalScreen(RegistryKey<World> viewingDimension, Vector3d viewingPosition) {
            super(new StringTextComponent("GUI Portal Example"));
            this.viewingDimension = viewingDimension;
            this.viewingPosition = viewingPosition;
        }
        
        @Override
        public void func_231175_as__() {
            super.func_231175_as__();
            
            // Tell the server to remove the additional chunk loader
            McRemoteProcedureCall.tellServerToInvoke(
                "com.qouteall.immersive_portals.api.example.ExampleGuiPortalRendering.RemoteCallables.serverRemoveChunkLoader"
            );
        }
        
        @Override
        public void func_230430_a_(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.func_230430_a_(matrices, mouseX, mouseY, delta);
            
            double t1 = CHelper.getSmoothCycles(503);
            double t2 = CHelper.getSmoothCycles(197);
            
            // Determine the camera transformation
            Matrix4f cameraTransformation = new Matrix4f();
            cameraTransformation.setIdentity();
            cameraTransformation.mul(
                DQuaternion.rotationByDegrees(
                    new Vector3d(1, 1, 1).normalize(),
                    t1 * 360
                ).toMcQuaternion()
            );
            
            // Determine the camera position
            Vector3d cameraPosition = this.viewingPosition.add(
                new Vector3d(Math.cos(t2 * 2 * Math.PI), 0, Math.sin(t2 * 2 * Math.PI)).scale(30)
            );
            
            // Create the world render info
            WorldRenderInfo worldRenderInfo = new WorldRenderInfo(
                ClientWorldLoader.getWorld(viewingDimension),// the world that it renders
                cameraPosition,// the camera position
                cameraTransformation,// the camera transformation
                true,// does not apply this transformation to the existing player camera
                null,
                field_230706_i_.gameSettings.renderDistanceChunks// render distance
            );
            
            // Ask it to render the world into the framebuffer the next frame
            GuiPortalRendering.submitNextFrameRendering(worldRenderInfo, frameBuffer);
            
            // Draw the framebuffer
            int h = field_230706_i_.getMainWindow().getFramebufferHeight();
            int w = field_230706_i_.getMainWindow().getFramebufferWidth();
            MyRenderHelper.drawFramebuffer(
                frameBuffer,
                false, false,
                w * 0.2f, w * 0.8f,
                h * 0.2f, h * 0.8f
            );
            
            func_238472_a_(
                matrices, this.field_230712_o_, this.field_230704_d_, this.field_230708_k_ / 2, 70, 16777215
            );
        }
        
        @Override
        public boolean func_231177_au__() {
            return false;
        }
    }
}
