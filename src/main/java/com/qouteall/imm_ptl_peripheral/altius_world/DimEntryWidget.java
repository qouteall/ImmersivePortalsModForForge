package com.qouteall.imm_ptl_peripheral.altius_world;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.list.AbstractOptionList;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// extending EntryListWidget.Entry is also fine
public class DimEntryWidget extends AbstractOptionList.Entry<DimEntryWidget> {
    
    public final RegistryKey<World> dimension;
    public final DimListWidget parent;
    private final Consumer<DimEntryWidget> selectCallback;
    private final ResourceLocation dimIconPath;
    private final ITextComponent dimensionName;
    private boolean dimensionIconPresent = true;
    private final Type type;
    public final AltiusEntry entry;
    
    public final static int widgetHeight = 50;
    
    public static enum Type {
        simple, withAdvancedOptions
    }
    
    public DimEntryWidget(
        RegistryKey<World> dimension,
        DimListWidget parent,
        Consumer<DimEntryWidget> selectCallback,
        Type type
    ) {
        this.dimension = dimension;
        this.parent = parent;
        this.selectCallback = selectCallback;
        this.type = type;
        
        this.dimIconPath = getDimensionIconPath(this.dimension);
        
        this.dimensionName = getDimensionName(dimension);
        
        try {
            Minecraft.getInstance().getResourceManager().getResource(dimIconPath);
        }
        catch (IOException e) {
            Helper.err("Cannot load texture " + dimIconPath);
            dimensionIconPresent = false;
        }
        
        entry = new AltiusEntry(dimension);
    }
    
    private final List<IGuiEventListener> children = new ArrayList<>();
    
    @Override
    public List<? extends IGuiEventListener> func_231039_at__() {
        return children;
    }
    
    @Override
    public void func_230432_a_(
        MatrixStack matrixStack,
        int index,
        int y,
        int x,
        int rowWidth,
        int itemHeight,
        int mouseX,
        int mouseY,
        boolean bl,
        float delta
    ) {
        Minecraft client = Minecraft.getInstance();
        
        client.fontRenderer.func_238421_b_(
            matrixStack, dimensionName.getString(),
            x + widgetHeight + 3, (float) (y),
            0xFFFFFFFF
        );
        
        client.fontRenderer.func_238421_b_(
            matrixStack, dimension.func_240901_a_().toString(),
            x + widgetHeight + 3, (float) (y + 10),
            0xFF999999
        );
        
        if (dimensionIconPresent) {
            client.getTextureManager().bindTexture(dimIconPath);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            
            AbstractGui.func_238463_a_(
                matrixStack,
                x, y, 0, (float) 0,
                widgetHeight - 4, widgetHeight - 4,
                widgetHeight - 4, widgetHeight - 4
            );
        }
        
        if (type == Type.withAdvancedOptions) {
            client.fontRenderer.func_243248_b(
                matrixStack, getText1(),
                x + widgetHeight + 3, (float) (y + 20),
                0xFF999999
            );
            client.fontRenderer.func_243248_b(
                matrixStack, getText2(),
                x + widgetHeight + 3, (float) (y + 30),
                0xFF999999
            );
        }
    }
    
    private ITextComponent getText1() {
        IFormattableTextComponent scaleText = entry.scale != 1.0 ?
            new TranslationTextComponent("imm_ptl.scale")
                .func_230529_a_(new StringTextComponent(":" + Double.toString(entry.scale)))
            : new StringTextComponent("");
        
        return scaleText;
    }
    
    private ITextComponent getText2() {
        IFormattableTextComponent horizontalRotationText = entry.horizontalRotation != 0 ?
            new TranslationTextComponent("imm_ptl.horizontal_rotation")
                .func_230529_a_(new StringTextComponent(":" + Double.toString(entry.horizontalRotation)))
                .func_230529_a_(new StringTextComponent(" "))
            : new StringTextComponent("");
        
        IFormattableTextComponent flippedText = entry.flipped ?
            new TranslationTextComponent("imm_ptl.flipped")
            : new StringTextComponent("");
        
        return horizontalRotationText.func_230529_a_(flippedText);
    }
    
    @Override
    public boolean func_231044_a_(double mouseX, double mouseY, int button) {
        selectCallback.accept(this);
        super.func_231044_a_(mouseX, mouseY, button);
        return true;//allow outer dragging
        /**
         * {@link EntryListWidget#mouseClicked(double, double, int)}
         */
    }
    
    public static ResourceLocation getDimensionIconPath(RegistryKey<World> dimension) {
        ResourceLocation id = dimension.func_240901_a_();
        return new ResourceLocation(
            id.getNamespace(),
            "textures/dimension/" + id.getPath() + ".png"
        );
    }
    
    private static TranslationTextComponent getDimensionName(RegistryKey<World> dimension) {
        return new TranslationTextComponent(
            "dimension." + dimension.func_240901_a_().getNamespace() + "."
                + dimension.func_240901_a_().getPath()
        );
    }
}
