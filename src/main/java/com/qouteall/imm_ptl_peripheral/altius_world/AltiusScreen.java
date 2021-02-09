package com.qouteall.imm_ptl_peripheral.altius_world;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.imm_ptl_peripheral.alternate_dimension.AlternateDimensions;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.GuiHelper;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class AltiusScreen extends Screen {
    CreateWorldScreen parent;
    private final Button backButton;
    private final Button toggleButton;
    private final Button addDimensionButton;
    private final Button removeDimensionButton;
    private final Button editButton;
    
    private final Button helpButton;
    
    private int titleY;
    
    public boolean isEnabled = false;
    public final DimListWidget dimListWidget;
    private final Supplier<DimensionGeneratorSettings> generatorOptionsSupplier1;
    
    public boolean loopEnabled = false;
    
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
            new TranslationTextComponent("imm_ptl.back"),
            (buttonWidget) -> {
                Minecraft.getInstance().displayGuiScreen(parent);
            }
        );
        addDimensionButton = new Button(
            0, 0, 72, 20,
            new TranslationTextComponent("imm_ptl.dim_stack_add"),
            (buttonWidget) -> {
                onAddEntry();
            }
        );
        removeDimensionButton = new Button(
            0, 0, 72, 20,
            new TranslationTextComponent("imm_ptl.dim_stack_remove"),
            (buttonWidget) -> {
                onRemoveEntry();
            }
        );
        
        editButton = new Button(
            0, 0, 72, 20,
            new TranslationTextComponent("imm_ptl.dim_stack_edit"),
            (buttonWidget) -> {
                onEditEntry();
            }
        );
        
        dimListWidget = new DimListWidget(
            field_230708_k_,
            field_230709_l_,
            100,
            200,
            DimEntryWidget.widgetHeight,
            this,
            DimListWidget.Type.mainDimensionList
        );
        
        Consumer<DimEntryWidget> callback = getElementSelectCallback();
        if (Global.enableAlternateDimensions) {
            dimListWidget.entryWidgets.add(createDimEntryWidget(AlternateDimensions.alternate5));
            dimListWidget.entryWidgets.add(createDimEntryWidget(AlternateDimensions.alternate1));
        }
        dimListWidget.entryWidgets.add(createDimEntryWidget(World.field_234918_g_));
        dimListWidget.entryWidgets.add(createDimEntryWidget(World.field_234919_h_));
        
        generatorOptionsSupplier1 = Helper.cached(() -> {
            DimensionGeneratorSettings rawGeneratorOptions =
                this.parent.field_238934_c_.func_239054_a_(false);
            return WorldCreationDimensionHelper.getPopulatedGeneratorOptions(
                this.parent, rawGeneratorOptions
            );
        });
        
        helpButton = createHelpButton(this);
    }
    
    public static Button createHelpButton(Screen parent) {
        return new Button(
            0, 0, 30, 20,
            new StringTextComponent("?"),
            button -> {
                CHelper.openLinkConfirmScreen(
                    parent, "https://qouteall.fun/immptl/wiki/Dimension-Stack"
                );
            }
        );
    }
    
    @NotNull
    private DimEntryWidget createDimEntryWidget(RegistryKey<World> dimension) {
        return new DimEntryWidget(dimension, dimListWidget, getElementSelectCallback(), DimEntryWidget.Type.withAdvancedOptions);
    }
    
    @Nullable
    public AltiusInfo getAltiusInfo() {
        if (isEnabled) {
            return new AltiusInfo(
                dimListWidget.entryWidgets.stream().map(
                    dimEntryWidget -> dimEntryWidget.entry
                ).collect(Collectors.toList()), loopEnabled
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
        
        func_230480_a_(editButton);
        
        func_230480_a_(helpButton);
        
        setEnabled(isEnabled);
        
        field_230705_e_.add(dimListWidget);
        
        dimListWidget.update();
        
        GuiHelper.layout(
            0, field_230709_l_,
            GuiHelper.blankSpace(5),
            new GuiHelper.LayoutElement(true, 20, (from, to) -> {
                helpButton.field_230690_l_ = field_230708_k_ - 50;
                helpButton.field_230691_m_ = from;
                helpButton.func_230991_b_(30);
            }),
            new GuiHelper.LayoutElement(true, 20, (a, b) -> {
                toggleButton.field_230690_l_ = 10;
                toggleButton.field_230691_m_ = a;
            }),
            GuiHelper.blankSpace(5),
            new GuiHelper.LayoutElement(false, 1, (from, to) -> {
                dimListWidget.func_230940_a_(
                    field_230708_k_, field_230709_l_,
                    from, to
                );
            }),
            GuiHelper.blankSpace(5),
            new GuiHelper.LayoutElement(true, 20, (from, to) -> {
                backButton.field_230691_m_ = from;
                addDimensionButton.field_230691_m_ = from;
                removeDimensionButton.field_230691_m_ = from;
                editButton.field_230691_m_ = from;
                GuiHelper.layout(
                    0, field_230708_k_,
                    GuiHelper.blankSpace(10),
                    new GuiHelper.LayoutElement(
                        false, 1,
                        GuiHelper.layoutButtonHorizontally(backButton)
                    ),
                    GuiHelper.blankSpace(5),
                    new GuiHelper.LayoutElement(
                        false, 1,
                        GuiHelper.layoutButtonHorizontally(addDimensionButton)
                    ),
                    GuiHelper.blankSpace(5),
                    new GuiHelper.LayoutElement(
                        false, 1,
                        GuiHelper.layoutButtonHorizontally(removeDimensionButton)
                    ),
                    GuiHelper.blankSpace(5),
                    new GuiHelper.LayoutElement(
                        false, 1,
                        GuiHelper.layoutButtonHorizontally(editButton)
                    ),
                    GuiHelper.blankSpace(10)
                );
            }),
            GuiHelper.blankSpace(5)
        );
    }
    
    @Override
    public void func_231175_as__() {
        // When `esc` is pressed return to the parent screen rather than setting screen to `null` which returns to the main menu.
        this.field_230706_i_.displayGuiScreen(this.parent);
    }
    
    private Consumer<DimEntryWidget> getElementSelectCallback() {
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
        textRenderer.func_243246_a(
            matrixStack, this.field_230704_d_,
            20, 10, -1
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
        
        editButton.field_230694_p_ = isEnabled;
    }
    
    private void onAddEntry() {
        DimEntryWidget selected = dimListWidget.func_230958_g_();
        
        int position;
        if (selected == null) {
            position = 0;
        }
        else {
            position = dimListWidget.entryWidgets.indexOf(selected);
        }
        
        if (position < 0 || position > dimListWidget.entryWidgets.size()) {
            position = -1;
        }
        
        int insertingPosition = position + 1;
        
        Minecraft.getInstance().displayGuiScreen(
            new DirtMessageScreen(new TranslationTextComponent("imm_ptl.loading_datapack_dimensions"))
        );
        
        ModMain.preTotalRenderTaskList.addTask(MyTaskList.withDelay(1, () -> {
            Minecraft.getInstance().displayGuiScreen(
                new SelectDimensionScreen(
                    this,
                    dimensionType -> {
                        dimListWidget.entryWidgets.add(
                            insertingPosition,
                            createDimEntryWidget(dimensionType)
                        );
                        removeDuplicate(insertingPosition);
                        dimListWidget.update();
                    }, generatorOptionsSupplier1
                )
            );
            return true;
        }));
    }
    
    private void onRemoveEntry() {
        DimEntryWidget selected = dimListWidget.func_230958_g_();
        if (selected == null) {
            return;
        }
        
        int position = dimListWidget.entryWidgets.indexOf(selected);
        
        if (position == -1) {
            return;
        }
        
        dimListWidget.entryWidgets.remove(position);
        dimListWidget.update();
    }
    
    private void onEditEntry() {
        DimEntryWidget selected = dimListWidget.func_230958_g_();
        if (selected == null) {
            return;
        }
        
        Minecraft.getInstance().displayGuiScreen(new AltiusEditScreen(
            this, selected
        ));
    }
    
    private void removeDuplicate(int insertedIndex) {
        RegistryKey<World> inserted = dimListWidget.entryWidgets.get(insertedIndex).dimension;
        for (int i = dimListWidget.entryWidgets.size() - 1; i >= 0; i--) {
            if (dimListWidget.entryWidgets.get(i).dimension == inserted) {
                if (i != insertedIndex) {
                    dimListWidget.entryWidgets.remove(i);
                }
            }
        }
    }
    
}
