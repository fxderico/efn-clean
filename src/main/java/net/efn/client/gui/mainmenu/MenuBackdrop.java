package net.efn.client.gui.mainmenu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class MenuBackdrop {

    public static final Identifier TEXTURE = Identifier.of("efn", "textures/gui/menu_backdrop.png");

    private static final float ASPECT = 1.7777778F;

    private MenuBackdrop() {}

    public static void draw(DrawContext context, int i, int j) {
        if (i > 0 && j > 0) {
            float f = (float) i / j;
            float g = f < 1.7777778F ? (1.0F - f / 1.7777778F) * 0.5F : 0.0F;
            float h = f > 1.7777778F ? (1.0F - 1.7777778F / f) * 0.5F : 0.0F;
            context.drawTexturedQuad(TEXTURE, 0, 0, i, j, g, 1.0F - g, h, 1.0F - h);
        }
    }
}
