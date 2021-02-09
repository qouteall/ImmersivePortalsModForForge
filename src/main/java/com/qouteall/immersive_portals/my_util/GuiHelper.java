package com.qouteall.immersive_portals.my_util;

import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;

public class GuiHelper {
    // mc does not expose the height
    @NotNull
    public static LayoutFunc layoutButtonHorizontally(Widget widget) {
        return (a, b) -> {
            widget.field_230690_l_ = a;
            widget.func_230991_b_(b - a);
        };
    }
    
    @NotNull
    public static LayoutFunc layoutRectHorizontally(Rect widget) {
        return (a, b) -> {
            widget.xMin = a;
            widget.xMax = b;
        };
    }
    
    public static LayoutFunc layoutButtonVertically(Widget widget) {
        return (a, b) -> {
            widget.field_230691_m_ = a;
        };
    }
    
    public static LayoutFunc layoutRectVertically(Rect widget) {
        return (a, b) -> {
            widget.yMin = a;
            widget.yMax = b;
        };
    }
    
    public static void layout(
        int from, int to,
        LayoutElement... elements
    ) {
        int totalElasticWeight = Arrays.stream(elements)
            .filter(e -> !e.fixedLength)
            .mapToInt(e -> e.length)
            .sum();
        
        int totalFixedLen = Arrays.stream(elements)
            .filter(e -> e.fixedLength)
            .mapToInt(e -> e.length)
            .sum();
        
        int totalEscalateLen = (to - from - totalFixedLen);
        
        int currCoordinate = from;
        for (LayoutElement element : elements) {
            int currLen;
            if (element.fixedLength) {
                currLen = element.length;
            }
            else {
                currLen = element.length * totalEscalateLen / totalElasticWeight;
            }
            element.apply.apply(currCoordinate, currCoordinate + currLen);
            currCoordinate += currLen;
        }
    }
    
    public static LayoutElement blankSpace(int length) {
        return new LayoutElement(true, length, (a, b) -> {});
    }
    
    public static LayoutElement elasticBlankSpace() {
        return new LayoutElement(false, 1, (a, b) -> {});
    }
    
    public static interface LayoutFunc {
        public void apply(int from, int to);
    }
    
    public static LayoutFunc combine(LayoutFunc... args) {
        return (from, to) -> {
            for (LayoutFunc layoutFunc : args) {
                layoutFunc.apply(from, to);
            }
        };
    }
    
    public static class Rect {
        public float xMin;
        public float yMin;
        public float xMax;
        public float yMax;
        
        public Rect(float xMin, float yMin, float xMax, float yMax) {
            this.xMin = xMin;
            this.yMin = yMin;
            this.xMax = xMax;
            this.yMax = yMax;
        }
        
        public Rect() {
            this(0, 0, 0, 0);
        }
        
        public void renderTextCentered(ITextComponent text, MatrixStack matrixStack) {
            FontRenderer textRenderer = Minecraft.getInstance().fontRenderer;
            
            IReorderingProcessor orderedText = text.func_241878_f();
            textRenderer.func_238407_a_(
                matrixStack, orderedText,
                (((xMin + xMax) / 2.0f) - textRenderer.func_243245_a(orderedText) / 2.0f), yMin+5,
                -1
            );
        }
        
        public void renderTextLeft(ITextComponent text, MatrixStack matrixStack) {
            FontRenderer textRenderer = Minecraft.getInstance().fontRenderer;
            textRenderer.func_243246_a(
                matrixStack, text,
                xMin, yMin+5, -1
            );
        }
    }
    
    public static class LayoutElement {
        public boolean fixedLength;
        //if fixed, this length. if not fixed, this is weight
        public int length;
        public LayoutFunc apply;
        
        public LayoutElement(boolean fixedLength, int length, LayoutFunc apply) {
            this.fixedLength = fixedLength;
            this.length = length;
            this.apply = apply;
        }
    }
    
    public static LayoutElement elastic(int weight, LayoutFunc layoutFunc) {
        return new LayoutElement(false, weight, layoutFunc);
    }
    
    public static LayoutElement fixedLength(int length, LayoutFunc layoutFunc) {
        return new LayoutElement(true, length, layoutFunc);
    }
}
