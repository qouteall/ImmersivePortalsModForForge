package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.qouteall.hiding_in_the_bushes.IEClientWorld_MA;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld_MA implements IEClientWorld_MA {
    @Shadow
    @Final
    private Int2ObjectMap<Entity> entitiesById;
    
    @Shadow
    protected abstract void removeEntity(Entity entityIn);
    
    @Override
    public void removeEntityWhilstMaintainingCapability(Entity entityToRemove) {
        int eid = entityToRemove.getEntityId();
        Entity entity = entitiesById.remove(eid);
        if (entity != null) {
            //keep the capability
            entity.remove(true);
            this.removeEntity(entity);
        }
    }
}
