package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.OFInterface;
import net.minecraft.client.shader.Framebuffer;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;

public class OFHelper {
    
    //some video cards does not support format conversion in copying depth component
    //optifine shader fb uses 32 bit depth and RenderMixed use 24 bit depth 8 bit stencil
    private static enum DepthFormatConvertCapability {
        unknown,
        supported,
        notSupported
    }
    
    
    public static void copyFromShaderFbTo(Framebuffer destFb, int copyComponent) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OFGlobal.getDfb.get());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destFb.framebufferObject);
        
        GL30.glBlitFramebuffer(
            0, 0, Shaders.renderWidth, Shaders.renderHeight,
            0, 0, destFb.framebufferWidth, destFb.framebufferHeight,
            copyComponent, GL_NEAREST
        );
        
        int errorCode = GL11.glGetError();
        if (errorCode != GL_NO_ERROR) {
            Helper.err("OpenGL Error" + errorCode);
            IntStream.range(0, 100).forEach(i -> Helper.log(
                "Detected Video Card's Incapability of Depth Format Conversion." +
                    "Switch to Compatibility Renderer"
            ));
            CGlobal.shaderCompatibilityMode = true;
        }
        
        OFInterface.bindToShaderFrameBuffer.run();
    }
}
