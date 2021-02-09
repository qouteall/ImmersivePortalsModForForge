package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;

@OnlyIn(Dist.CLIENT)
public class CHelper {
    
    private static int reportedErrorNum = 0;
    
    public static NetworkPlayerInfo getClientPlayerListEntry() {
        return Minecraft.getInstance().getConnection().getPlayerInfo(
            Minecraft.getInstance().player.getGameProfile().getId()
        );
    }
    
    public static boolean shouldDisableFog() {
        return OFInterface.shouldDisableFog.getAsBoolean();
    }
    
    //do not inline this
    //or it will crash in server
    public static World getClientWorld(RegistryKey<World> dimension) {
        return ClientWorldLoader.getWorld(dimension);
    }
    
    public static List<Portal> getClientGlobalPortal(World world) {
        if (world instanceof ClientWorld) {
            return ((IEClientWorld) world).getGlobalPortals();
        }
        else {
            return null;
        }
    }
    
    public static Stream<Portal> getClientNearbyPortals(double range) {
        return McHelper.getNearbyPortals(Minecraft.getInstance().player, range);
    }
    
    public static void checkGlError() {
        if (!Global.doCheckGlError) {
            return;
        }
        if (reportedErrorNum > 100) {
            return;
        }
        int errorCode = GL11.glGetError();
        if (errorCode != GL_NO_ERROR) {
            Helper.err("OpenGL Error" + errorCode);
            new Throwable().printStackTrace();
            reportedErrorNum++;
        }
    }
    
    public static void printChat(String str) {
        printChat(new StringTextComponent(str));
    }
    
    public static void printChat(StringTextComponent text) {
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(text);
    }
    
    public static void openLinkConfirmScreen(
        Screen parent,
        String link
    ) {
        Minecraft client = Minecraft.getInstance();
        client.displayGuiScreen(new ConfirmOpenLinkScreen(
            (result) -> {
                if (result) {
                    try {
                        Util.getOSType().openURI(new URI(link));
                    }
                    catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
                client.displayGuiScreen(parent);
            },
            link, true
        ));
    }
    
    public static Vector3d getCurrentCameraPos() {
        return Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
    }
    
    public static <T> T withWorldSwitched(Entity entity, Portal portal, Supplier<T> func) {
        
        World oldWorld = entity.world;
        Vector3d eyePos = McHelper.getEyePos(entity);
        Vector3d lastTickEyePos = McHelper.getLastTickEyePos(entity);
        
        entity.world = portal.getDestinationWorld();
        McHelper.setEyePos(
            entity,
            portal.transformPoint(eyePos),
            portal.transformPoint(lastTickEyePos)
        );
        
        try {
            T result = func.get();
            return result;
        }
        finally {
            entity.world = oldWorld;
            McHelper.setEyePos(entity, eyePos, lastTickEyePos);
        }
    }
    
    public static Iterable<Entity> getWorldEntityList(World world) {
        if (!(world instanceof ClientWorld)) {
            return (Iterable<Entity>) Collections.emptyList().iterator();
        }
        
        ClientWorld clientWorld = (ClientWorld) world;
        return clientWorld.getAllEntities();
    }
    
    /**
     * {@link ReentrantThreadExecutor#shouldExecuteAsync()}
     * The execution may get deferred on the render thread
     */
    public static void executeOnRenderThread(Runnable runnable) {
        Minecraft client = Minecraft.getInstance();
        
        if (client.isOnExecutionThread()) {
            runnable.run();
        }
        else {
            client.execute(runnable);
        }
    }
    
    public static double getSmoothCycles(long unitTicks) {
        int playerAge = Minecraft.getInstance().player.ticksExisted;
        return (playerAge % unitTicks + RenderStates.tickDelta) / (double) unitTicks;
    }
}
