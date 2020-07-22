package com.qouteall.immersive_portals.altius_world;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AltiusScreen extends Screen {
    CreateWorldScreen parent;
    private Button backButton;
    private Button toggleButton;
    private Button addDimensionButton;
    private Button removeDimensionButton;
    
    private int titleY;
    
    public boolean isEnabled = false;
    private DimListWidget dimListWidget;
    
    public AltiusScreen(CreateWorldScreen parent) {
        super(new TranslationTextComponent("imm_ptl.altius_screen"));
        this.parent = parent;
        
        toggleButton = new Button(
            0, 0, 150, 20,
            new TranslationTextComponent("imm_ptl.toggle_altius"),
            (buttonWidget) -> {
                setEnabled(!isEnabled);
            }
        );
        
        backButton = new Button(
            0, 0, 72, 20,
            new TranslationTextComponent("imm_ptl.back_to_create_world"),
            (buttonWidget) -> {
                Minecraft.getInstance().displayGuiScreen(parent);
            }
        );
        addDimensionButton = new Button(
            0, 0, 72, 20,
            new TranslationTextComponent("imm_ptl.add_dimension"),
            (buttonWidget) -> {
                onAddDimension();
            }
        );
        removeDimensionButton = new Button(
            0, 0, 72, 20,
            new TranslationTextComponent("imm_ptl.remove_dimension"),
            (buttonWidget) -> {
                onRemoveDimension();
            }
        );
        
        dimListWidget = new DimListWidget(
            field_230708_k_,
            field_230709_l_,
            100,
            200,
            15,
            this
        );
        
        Consumer<DimTermWidget> callback = getElementSelectCallback();
        dimListWidget.terms.add(
            new DimTermWidget(ModMain.alternate4, dimListWidget, callback)
        );
        dimListWidget.terms.add(
            new DimTermWidget(ModMain.alternate2, dimListWidget, callback)
        );
        dimListWidget.terms.add(
            new DimTermWidget(World.field_234918_g_, dimListWidget, callback)
        );
        dimListWidget.terms.add(
            new DimTermWidget(World.field_234919_h_, dimListWidget, callback)
        );
    }
    
    //nullable
    public AltiusInfo getAltiusInfo() {
        if (isEnabled) {
            return new AltiusInfo(
                dimListWidget.terms.stream().map(
                    w -> w.dimension
                ).collect(Collectors.toList())
            );
        }
        else {
            return null;
        }
    }
    
    @Override
    protected void func_231160_c_() {
        
        func_230480_a_(toggleButton);
        func_230480_a_(backButton);
        func_230480_a_(addDimensionButton);
        func_230480_a_(removeDimensionButton);
        
        setEnabled(isEnabled);
        
        field_230705_e_.add(dimListWidget);
        
        dimListWidget.update();
        
        CHelper.layout(
            0, field_230709_l_,
            CHelper.LayoutElement.blankSpace(15),
            new CHelper.LayoutElement(true, 20, (from, to) -> {
                titleY = (from + to) / 2;
            }),
            CHelper.LayoutElement.blankSpace(10),
            new CHelper.LayoutElement(true, 20, (a, b) -> {
                toggleButton.field_230690_l_ = 20;
                toggleButton.field_230691_m_ = a;
            }),
            CHelper.LayoutElement.blankSpace(10),
            new CHelper.LayoutElement(false, 1, (from, to) -> {
                dimListWidget.func_230940_a_(
                    field_230708_k_, field_230709_l_,
                    from, to
                );
            }),
            CHelper.LayoutElement.blankSpace(15),
            new CHelper.LayoutElement(true, 20, (from, to) -> {
                backButton.field_230691_m_ = from;
                addDimensionButton.field_230691_m_ = from;
                removeDimensionButton.field_230691_m_ = from;
                CHelper.layout(
                    0, field_230708_k_,
                    CHelper.LayoutElement.blankSpace(20),
                    CHelper.LayoutElement.layoutX(backButton, 1),
                    CHelper.LayoutElement.blankSpace(10),
                    CHelper.LayoutElement.layoutX(addDimensionButton, 1),
                    CHelper.LayoutElement.blankSpace(10),
                    CHelper.LayoutElement.layoutX(removeDimensionButton, 1),
                    CHelper.LayoutElement.blankSpace(20)
                );
            }),
            CHelper.LayoutElement.blankSpace(15)
        );
    }
    
    private Consumer<DimTermWidget> getElementSelectCallback() {
        return w -> dimListWidget.func_241215_a_(w);
    }
    
    @Override
    public void func_230430_a_(MatrixStack matrixStack, int mouseY, int i, float f) {
        this.func_230446_a_(matrixStack);
        
        
        if (isEnabled) {
            dimListWidget.func_230430_a_(matrixStack, mouseY, i, f);
        }
        
        super.func_230430_a_(matrixStack, mouseY, i, f);
    
        FontRenderer textRenderer = Minecraft.getInstance().fontRenderer;
        textRenderer.func_238407_a_(
            matrixStack, this.field_230704_d_,
            20, 20, -1
        );
        
    }
    
    private void setEnabled(boolean cond) {
        isEnabled = cond;
        if (isEnabled) {
            toggleButton.func_238482_a_(new TranslationTextComponent("imm_ptl.altius_toggle_true"));
        }
        else {
            toggleButton.func_238482_a_(new TranslationTextComponent("imm_ptl.altius_toggle_false"));
        }
        addDimensionButton.field_230694_p_ = isEnabled;
        removeDimensionButton.field_230694_p_ = isEnabled;
    }
    
    private void onAddDimension() {
        DimTermWidget selected = dimListWidget.func_230958_g_();
        if (selected == null) {
            return;
        }
        
        int position = dimListWidget.terms.indexOf(selected);
        
        if (position < 0 || position > dimListWidget.terms.size()) {
            position = -1;
        }
        
        int insertingPosition = position + 1;
        
        Minecraft.getInstance().displayGuiScreen(
            new SelectDimensionScreen(
                this,
                dimensionType -> {
                    dimListWidget.terms.add(
                        insertingPosition,
                        new DimTermWidget(
                            dimensionType,
                            dimListWidget,
                            getElementSelectCallback()
                        )
                    );
                    removeDuplicate(insertingPosition);
                    dimListWidget.update();
                }
            )
        );
    }
    
    private void onRemoveDimension() {
        DimTermWidget selected = dimListWidget.func_230958_g_();
        if (selected == null) {
            return;
        }
        
        int position = dimListWidget.terms.indexOf(selected);
        
        if (position == -1) {
            return;
        }
        
        dimListWidget.terms.remove(position);
        dimListWidget.update();
    }
    
    private void removeDuplicate(int insertedIndex) {
        RegistryKey<World> inserted = dimListWidget.terms.get(insertedIndex).dimension;
        for (int i = dimListWidget.terms.size() - 1; i >= 0; i--) {
            if (dimListWidget.terms.get(i).dimension == inserted) {
                if (i != insertedIndex) {
                    dimListWidget.terms.remove(i);
                }
            }
        }
    }
}
