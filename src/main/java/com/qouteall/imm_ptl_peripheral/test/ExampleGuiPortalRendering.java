package com.qouteall.imm_ptl_peripheral.test;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.my_util.DQuaternion;
import com.qouteall.immersive_portals.render.GuiPortalRendering;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import com.qouteall.immersive_portals.render.context_management.WorldRenderInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExampleGuiPortalRendering {
    private static Framebuffer frameBuffer;
    private static final Minecraft client = Minecraft.getInstance();
    
    public static void open() {
        if (frameBuffer == null) {
            frameBuffer = new Framebuffer(1000, 1000, true, true);
        }
        
        client.displayGuiScreen(new TestScreen());
    }
    
    public static class TestScreen extends Screen {
        
        public TestScreen() {
            super(new StringTextComponent("GUI Portal Test"));
        }
        
        @Override
        public void func_230430_a_(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.func_230430_a_(matrices, mouseX, mouseY, delta);
            
            Matrix4f cameraTransformation = new Matrix4f();
            cameraTransformation.setIdentity();
            cameraTransformation.mul(
                DQuaternion.rotationByDegrees(
                    new Vector3d(1, 0, 0),
                    ((double) ((field_230706_i_.world.getGameTime() % 100) / 100.0) + RenderStates.tickDelta / 100.0) * 360
                ).toMcQuaternion()
            );
            
            WorldRenderInfo worldRenderInfo = new WorldRenderInfo(
                ClientWorldLoader.getWorld(World.field_234918_g_),
                field_230706_i_.player.getPositionVec().add(0, 5, 0),
                cameraTransformation, null,
                field_230706_i_.gameSettings.renderDistanceChunks, true
            );
            GuiPortalRendering.submitNextFrameRendering(worldRenderInfo, frameBuffer);
            
            int windowHeight = field_230706_i_.getMainWindow().getFramebufferHeight();
            int windowWidth = field_230706_i_.getMainWindow().getFramebufferWidth();
            float sideLen = windowHeight * 0.6f;
            
            MyRenderHelper.drawFramebuffer(
                frameBuffer,
                false, false,
                (windowWidth - sideLen) / 2,
                (windowWidth - sideLen) / 2 + sideLen,
                windowHeight * 0.2f,
                windowHeight * 0.2f + sideLen
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
