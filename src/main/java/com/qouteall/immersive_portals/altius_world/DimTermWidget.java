package com.qouteall.immersive_portals.altius_world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.function.Consumer;

public class DimTermWidget extends AbstractList.AbstractListEntry<DimTermWidget> {
    
    public RegistryKey<World> dimension;
    public final DimListWidget parent;
    private Consumer<DimTermWidget> selectCallback;
    
    public DimTermWidget(
        RegistryKey<World> dimension,
        DimListWidget parent,
        Consumer<DimTermWidget> selectCallback
    ) {
        this.dimension = dimension;
        this.parent = parent;
        this.selectCallback = selectCallback;
    }
    
    @Override
    public void func_230432_a_(
        MatrixStack matrixStack,
        int y,
        int x,
        int width,
        int height,
        int mouseX,
        int mouseY,
        int i,
        boolean bl,
        float f
    ) {
        Minecraft.getInstance().fontRenderer.func_238421_b_(
            matrixStack, dimension.func_240901_a_().toString(), width + 32 + 3, (float) (x), 0xFFFFFFFF
        );
    }
    
    @Override
    public boolean func_231044_a_(double mouseX, double mouseY, int button) {
        selectCallback.accept(this);
        return super.func_231044_a_(mouseX, mouseY, button);
    }
}
