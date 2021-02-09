package com.qouteall.imm_ptl_peripheral.altius_world;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.util.text.TranslationTextComponent;
import java.util.ArrayList;
import java.util.List;

public class DimListWidget extends AbstractList<DimEntryWidget> {
    public final List<DimEntryWidget> entryWidgets = new ArrayList<>();
    public final Screen parent;
    private final Type type;
    
    private Button extraLoopButton;
    
    public static enum Type {
        mainDimensionList, addDimensionList
    }
    
    public DimListWidget(
        int width,
        int height,
        int top,
        int bottom,
        int itemHeight,
        Screen parent,
        Type type
    ) {
        super(Minecraft.getInstance(), width, height, top, bottom, itemHeight);
        this.parent = parent;
        this.type = type;
        
        if (type == Type.mainDimensionList) {
            AltiusScreen parent1 = (AltiusScreen) parent;
            
            extraLoopButton = new Button(
                0, 0, 100, 20,
                new TranslationTextComponent(parent1.loopEnabled ?
                    "imm_ptl.enabled" : "imm_ptl.disabled"),
                button -> {
                    parent1.loopEnabled = !parent1.loopEnabled;
                    button.func_238482_a_(
                        new TranslationTextComponent(parent1.loopEnabled ?
                            "imm_ptl.enabled" : "imm_ptl.disabled")
                    );
                }
            );
        }
    }
    
    public void update() {
        this.func_230963_j_();
        this.entryWidgets.forEach(this::func_230513_b_);
    }
    
    @Override
    public boolean func_231045_a_(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (type == Type.mainDimensionList) {
            DimEntryWidget selected = func_230958_g_();
            
            if (selected != null) {
                DimEntryWidget mouseOn = func_230933_a_(mouseX, mouseY);
                if (mouseOn != null) {
                    if (mouseOn != selected) {
                        switchEntries(selected, mouseOn);
                    }
                }
            }
        }
        
        return super.func_231045_a_(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    private void switchEntries(DimEntryWidget a, DimEntryWidget b) {
        int i1 = entryWidgets.indexOf(a);
        int i2 = entryWidgets.indexOf(b);
        if (i1 == -1 || i2 == -1) {
            Helper.err("Dimension Stack GUI Abnormal");
            return;
        }
        
        DimEntryWidget temp = entryWidgets.get(i1);
        entryWidgets.set(i1, entryWidgets.get(i2));
        entryWidgets.set(i2, temp);
        
        update();
    }
    
    @Override
    protected int func_230945_b_() {
        if (type == Type.mainDimensionList) {
            return super.func_230945_b_() + field_230669_c_;
        }
        
        return super.func_230945_b_();
    }
    
    @Override
    protected void func_238478_a_(MatrixStack matrices, int x, int y, int mouseX, int mouseY, float delta) {
        super.func_238478_a_(matrices, x, y, mouseX, mouseY, delta);
        
        if (type == Type.mainDimensionList) {
            renderLoopButton(matrices, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public void func_230430_a_(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.func_230430_a_(matrices, mouseX, mouseY, delta);
    }
    
    private void renderLoopButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int localOffset = func_230945_b_() - 35;
        int currY = field_230672_i_ + 4 - (int) func_230966_l_() + localOffset;
        extraLoopButton.field_230691_m_ = currY;
        extraLoopButton.field_230690_l_ = func_230968_n_() + 100;
        
        extraLoopButton.func_230430_a_(matrices, mouseX, mouseY, delta);
        
        new GuiHelper.Rect(
            func_230968_n_() + 30, currY, 200, currY + 100
        ).renderTextLeft(new TranslationTextComponent("imm_ptl.loop"), matrices);
    }
    
    @Override
    public boolean func_231044_a_(double mouseX, double mouseY, int button) {
        if (type == Type.mainDimensionList) {
            extraLoopButton.func_231044_a_(mouseX, mouseY, button);
        }
        
        return super.func_231044_a_(mouseX, mouseY, button);
    }
}
