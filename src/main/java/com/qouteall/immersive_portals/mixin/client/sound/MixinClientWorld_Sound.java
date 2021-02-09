package com.qouteall.immersive_portals.mixin.client.sound;

import com.qouteall.immersive_portals.ClientWorldLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.EntityTickableSound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class MixinClientWorld_Sound {
    
    @Shadow
    @Final
    private Minecraft mc;
    
    @Inject(
        method = "Lnet/minecraft/client/world/ClientWorld;playSound(DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FFZ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onPlaySound(
        double x, double y, double z,
        SoundEvent sound, SoundCategory category, float volume, float pitch, boolean bl,
        CallbackInfo ci
    ) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        Vector3d soundPos = new Vector3d(x, y, z);
        
        if (!portal_isPosNearPlayer(soundPos)) {
            Vector3d transformedSoundPosition =
                ClientWorldLoader.getTransformedSoundPosition(this_, soundPos);
            if (transformedSoundPosition != null) {
                portal_playSound(
                    transformedSoundPosition.x, transformedSoundPosition.y, transformedSoundPosition.z,
                    sound, category, volume, pitch, bl
                );
            }
            ci.cancel();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/world/ClientWorld;playMovingSound(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onPlaySoundFromEntity(
        PlayerEntity player, Entity entity,
        SoundEvent sound, SoundCategory category, float volume, float pitch, CallbackInfo ci
    ) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        
        if (!portal_isPosNearPlayer(entity.getPositionVec())) {
            Vector3d entityPos = entity.getPositionVec();
            Vector3d transformedSoundPosition = ClientWorldLoader.getTransformedSoundPosition(
                this_, entityPos
            );
            
            if (transformedSoundPosition != null) {
                entity.setRawPosition(transformedSoundPosition.x, transformedSoundPosition.y, transformedSoundPosition.z);
                mc.getSoundHandler().play(new EntityTickableSound(sound, category, entity));
                entity.setRawPosition(entityPos.x, entityPos.y, entityPos.z);
            }
            
            ci.cancel();
        }
    }
    
    private void portal_playSound(
        double x, double y, double z,
        SoundEvent sound, SoundCategory category, float volume, float pitch, boolean bl
    ) {
        double d = mc.gameRenderer.getActiveRenderInfo().getProjectedView().squareDistanceTo(x, y, z);
        SimpleSound positionedSoundInstance = new SimpleSound(sound, category, volume, pitch, x, y, z);
        if (bl && d > 100.0D) {
            double e = Math.sqrt(d) / 40.0D;
            mc.getSoundHandler().playDelayed(positionedSoundInstance, (int) (e * 20.0D));
        }
        else {
            mc.getSoundHandler().play(positionedSoundInstance);
        }
    }
    
    private boolean portal_isPosNearPlayer(Vector3d pos) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        
        ClientPlayerEntity player = mc.player;
        
        if (this_ != player.world) {
            return false;
        }
        
        return pos.squareDistanceTo(player.getPositionVec()) < 30 * 30;
    }
    
}
