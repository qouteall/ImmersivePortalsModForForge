package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.util.List;
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
    
    //NOTE this may not be reliable
    public static DimensionType getOriginalDimension() {
        if (CGlobal.renderer.isRendering()) {
            return MyRenderHelper.originalPlayerDimension;
        }
        else {
            return Minecraft.getInstance().player.dimension;
        }
    }
    
    public static boolean shouldDisableFog() {
        return OFInterface.shouldDisableFog.getAsBoolean();
    }
    
    //do not inline this
    //or it will crash in server
    public static World getClientWorld(DimensionType dimension) {
        return CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension);
    }
    
    public static Stream<Portal> getClientNearbyPortals(double range) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        List<GlobalTrackedPortal> globalPortals = ((IEClientWorld) player.world).getGlobalPortals();
        Stream<Portal> nearbyPortals = McHelper.getEntitiesNearby(
            player,
            Portal.class,
            range
        );
        if (globalPortals == null) {
            return nearbyPortals;
        }
        else {
            return Streams.concat(
                globalPortals.stream().filter(
                    p -> p.getDistanceToNearestPointInPortal(player.getPositionVec()) < range * 2
                ),
                nearbyPortals
            );
        }
    }
    
    public static void initCommandClientOnly(CommandDispatcher<CommandSource> dispatcher) {
        MyCommandClient.register(dispatcher);
    }
    
    public static void printChat(String str) {
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(str));
    }
    
    public static void checkGlError() {
        if (!CGlobal.doCheckGlError) {
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
}
