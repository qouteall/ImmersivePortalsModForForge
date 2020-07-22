package com.qouteall.immersive_portals.portal;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.List;
import java.util.Random;

public class LoadingIndicatorEntity extends Entity {
    public static EntityType<LoadingIndicatorEntity> entityType;
    
    private static final DataParameter<ITextComponent> text = EntityDataManager.createKey(
        LoadingIndicatorEntity.class, DataSerializers.TEXT_COMPONENT
    );
    
    public boolean isValid = false;
    
    public BlockPortalShape portalShape;
    
    public LoadingIndicatorEntity(EntityType type, World world) {
        super(type, world);
    }
    
    @Override
    public Iterable<ItemStack> getArmorInventoryList() {
        return null;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (world.isRemote()) {
            tickClient();
        }
        else {
            // remove after quitting server and restarting
            if (!isValid) {
                remove();
            }
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private void tickClient() {
        addParticles();
        
        if (ticksExisted > 40) {
            showMessageClient();
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private void addParticles() {
        int num = ticksExisted < 100 ? 50 : 20;
        
        if (portalShape != null) {
            IntBox box = portalShape.innerAreaBox;
            BlockPos size = box.getSize();
            Random random = world.getRandom();
            
            for (int i = 0; i < num; i++) {
                Vector3d p = new Vector3d(
                    random.nextDouble(), random.nextDouble(), random.nextDouble()
                ).mul(Vector3d.func_237491_b_(size)).add(Vector3d.func_237491_b_(box.l));
                
                double speedMultiplier = 20;
                
                double vx = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vy = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vz = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                
                world.addParticle(
                    ParticleTypes.PORTAL,
                    p.x, p.y, p.z,
                    vx, vy, vz
                );
            }
        }
    }
    
    @Override
    protected void registerData() {
        getDataManager().register(text, new StringTextComponent("Loading..."));
    }
    
    @Override
    protected void readAdditional(CompoundNBT tag) {
        if (tag.contains("shape")) {
            portalShape = new BlockPortalShape(tag.getCompound("shape"));
        }
    }
    
    @Override
    protected void writeAdditional(CompoundNBT tag) {
        if (portalShape != null) {
            tag.put("shape", portalShape.toTag());
        }
    }
    
    @Override
    public IPacket<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(this);
    }
    
    public void inform(ITextComponent str) {
        setText(str);
    }
    
    public void setText(ITextComponent str) {
        getDataManager().set(text, str);
    }
    
    public ITextComponent getText() {
        return getDataManager().get(text);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void showMessageClient() {
        IngameGui inGameHud = Minecraft.getInstance().ingameGUI;
        inGameHud.func_238450_a_(
            ChatType.GAME_INFO,
            getText(),
            Util.field_240973_b_
        );
    }
}
