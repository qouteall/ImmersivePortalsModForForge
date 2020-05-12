package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.CHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.net.URI;
import java.net.URISyntaxException;

@OnlyIn(Dist.CLIENT)
public class MissingMixinScreen extends Screen {
    private Button downloadLinkButton;
    private Button quitGameButton;
    
    public MissingMixinScreen() {
        super(new TranslationTextComponent("imm_ptl.missing_mixin"));
        
        downloadLinkButton = new Button(
            0, 0, 72, 20,
            I18n.format("imm_ptl.download_mixinbootstrap"),
            (buttonWidget) -> {
                try {
                    Util.getOSType().openURI(
                        new URI("https://www.curseforge.com/minecraft/mc-mods/mixinbootstrap")
                    );
                }
                catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        );
        quitGameButton = new Button(
            0, 0, 72, 20,
            I18n.format("menu.quit"),
            (buttonWidget) -> {
                Minecraft.getInstance().shutdown();
            }
        );
    }
    
    @Override
    protected void init() {
        addButton(downloadLinkButton);
        addButton(quitGameButton);
        
        downloadLinkButton.y = height - 40;
        quitGameButton.y = height - 40;
        
        CHelper.layout(
            0, width,
            CHelper.LayoutElement.blankSpace(10),
            CHelper.LayoutElement.layoutX(downloadLinkButton, 1),
            CHelper.LayoutElement.blankSpace(10),
            CHelper.LayoutElement.layoutX(quitGameButton, 1),
            CHelper.LayoutElement.blankSpace(10)
        );
    }
    
    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        
        this.drawCenteredString(
            this.font, this.title.getFormattedText(),
            this.width / 2, 100, -1
        );
        
        super.render(mouseX, mouseY, delta);
    }
}
