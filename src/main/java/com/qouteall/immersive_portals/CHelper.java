package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
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
import java.util.Arrays;
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
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(
            new StringTextComponent(str)
        );
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
    
    public static class Rect {
        public float xMin;
        public float yMin;
        public float xMax;
        public float yMax;
        
        public Rect(float xMin, float yMin, float xMax, float yMax) {
            this.xMin = xMin;
            this.yMin = yMin;
            this.xMax = xMax;
            this.yMax = yMax;
        }
        
        public void grow(float delta) {
            xMin -= delta;
            xMax += delta;
            yMin -= delta;
            yMax += delta;
        }
        
        public static Rect of(Screen screen) {
            return new Rect(
                0, 0,
                screen.field_230708_k_, screen.field_230709_l_
            );
        }

//        public static Rect of(AbstractButtonWidget widget) {
//            return new Rect(
//                widget.x,widget.y,
//                widget.x+widget.getWidth(),
//                widget.y+widget.
//            )
//        }
    }
    
    public static interface LayoutFunc {
        public void apply(int from, int to);
    }
    
    public static class LayoutElement {
        public boolean fixedLength;
        //if fixed, this length. if not fixed, this is weight
        public int length;
        public LayoutFunc apply;
        
        public LayoutElement(boolean fixedLength, int length, LayoutFunc apply) {
            this.fixedLength = fixedLength;
            this.length = length;
            this.apply = apply;
        }
        
        public static LayoutElement blankSpace(int length) {
            return new LayoutElement(true, length, (a, b) -> {
            });
        }
        
        public static LayoutElement layoutX(Button widget, int widthRatio) {
            return new LayoutElement(
                false,
                widthRatio,
                (a, b) -> {
                    widget.field_230690_l_ = a;
                    widget.func_230991_b_(b - a);
                }
            );
        }
        
        public static LayoutElement layoutY(Button widget, int height) {
            return new LayoutElement(
                true,
                height,
                (a, b) -> {
                    widget.field_230691_m_ = a;
                }
            );
        }
    }
    
    public static void layout(
        int from, int to,
        LayoutElement... elements
    ) {
        int totalElasticWeight = Arrays.stream(elements)
            .filter(e -> !e.fixedLength)
            .mapToInt(e -> e.length)
            .sum();
        
        int totalFixedLen = Arrays.stream(elements)
            .filter(e -> e.fixedLength)
            .mapToInt(e -> e.length)
            .sum();
        
        int totalEscalateLen = (to - from - totalFixedLen);
        
        int currCoordinate = from;
        for (LayoutElement element : elements) {
            int currLen;
            if (element.fixedLength) {
                currLen = element.length;
            }
            else {
                currLen = element.length * totalEscalateLen / totalElasticWeight;
            }
            element.apply.apply(currCoordinate, currCoordinate + currLen);
            currCoordinate += currLen;
        }
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
}
