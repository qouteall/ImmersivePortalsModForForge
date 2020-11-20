package com.qouteall.imm_ptl_peripheral.altius_world;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.imm_ptl_peripheral.alternate_dimension.AlternateDimensions;
import com.qouteall.immersive_portals.Global;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Dimension;
import net.minecraft.world.World;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SelectDimensionScreen extends Screen {
    public final AltiusScreen parent;
    private DimListWidget dimListWidget;
    private Button confirmButton;
    private Consumer<RegistryKey<World>> outerCallback;
    private Supplier<DimensionGeneratorSettings> generatorOptionsSupplier;
    
    protected SelectDimensionScreen(AltiusScreen parent, Consumer<RegistryKey<World>> callback, Supplier<DimensionGeneratorSettings> generatorOptionsSupplier1) {
        super(new TranslationTextComponent("imm_ptl.select_dimension"));
        this.parent = parent;
        this.outerCallback = callback;
        
        generatorOptionsSupplier = generatorOptionsSupplier1;
    }
    
    public static List<RegistryKey<World>> getDimensionList(
        Supplier<DimensionGeneratorSettings> generatorOptionsSupplier,
        DynamicRegistries.Impl dynamicRegistryManager
    ) {
        
        final DimensionGeneratorSettings generatorOptions = generatorOptionsSupplier.get();
        SimpleRegistry<Dimension> dimensionMap = generatorOptions.func_236224_e_();
        
        // Alternate dimensions are added in a special way
        if (Global.enableAlternateDimensions) {
            AlternateDimensions.addAlternateDimensions(
                dimensionMap,
                dynamicRegistryManager,
                generatorOptions.func_236221_b_()
            );
        }
        
        ArrayList<RegistryKey<World>> dimList = new ArrayList<>();
        
        for (Map.Entry<RegistryKey<Dimension>, Dimension> entry : dimensionMap.func_239659_c_()) {
            dimList.add(RegistryKey.func_240903_a_(Registry.field_239699_ae_, entry.getKey().func_240901_a_()));
        }
        
        return dimList;
    }
    
    @Override
    protected void func_231160_c_() {
        dimListWidget = new DimListWidget(
            field_230708_k_,
            field_230709_l_,
            48,
            field_230709_l_ - 64,
            15,
            this
        );
        field_230705_e_.add(dimListWidget);
        
        Consumer<DimTermWidget> callback = w -> dimListWidget.func_241215_a_(w);
        
        for (RegistryKey<World> dim : getDimensionList(this.generatorOptionsSupplier, this.parent.parent.field_238934_c_.func_239055_b_())) {
            dimListWidget.terms.add(new DimTermWidget(dim, dimListWidget, callback));
        }
        
        dimListWidget.update();
        
        confirmButton = (Button) this.func_230480_a_(new Button(
            this.field_230708_k_ / 2 - 75, this.field_230709_l_ - 28, 150, 20,
            new TranslationTextComponent("imm_ptl.confirm_select_dimension"),
            (buttonWidget) -> {
                DimTermWidget selected = dimListWidget.func_230958_g_();
                if (selected == null) {
                    return;
                }
                outerCallback.accept(selected.dimension);
                Minecraft.getInstance().displayGuiScreen(parent);
            }
        ));
        
    }
    
    @Override
    public void func_231175_as__() {
        // When `esc` is pressed return to the parent screen rather than setting screen to `null` which returns to the main menu.
        this.field_230706_i_.displayGuiScreen(this.parent);
    }
    
    @Override
    public void func_230430_a_(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        this.func_230446_a_(matrixStack);
        
        dimListWidget.func_230430_a_(matrixStack, mouseX, mouseY, delta);
        
        super.func_230430_a_(matrixStack, mouseX, mouseY, delta);
        
        this.func_238471_a_(
            matrixStack, this.field_230712_o_, this.field_230704_d_.getUnformattedComponentText(), this.field_230708_k_ / 2, 20, -1
        );
    }
}
