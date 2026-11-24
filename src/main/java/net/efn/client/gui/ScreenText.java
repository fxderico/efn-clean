package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public final class ScreenText {

    private static final float IN = 0.4F;

    private static final float OUT = 0.8F;

    private final int rgb;

    private final float stay;

    private Text title;

    private Text subtitle;

    private long start;

    public ScreenText(int i, float stay2) {
        this.rgb = i & 16777215;
        this.stay = stay2;
    }

    public void show(String string, String string2) {
        this.title = Text.literal(string);
        this.subtitle = Text.literal(string2);
        this.start = 0L;
    }

    public void render(DrawContext context) {
        if (this.title != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            long l = System.nanoTime();
            if (this.start == 0L) {
                if (client.currentScreen != null || client.world == null) {
                    return;
                }
                this.start = l;
            }
            float f;
            if ((f = (float) (l - this.start) / 1.0E9F) >= 0.4F + this.stay + 0.8F) {
                this.title = null;
            } else {
                float g =
                        f < 0.4F ? f / 0.4F : Math.min(1.0F, (0.4F + this.stay + 0.8F - f) / 0.8F);
                int i = Math.round(g * 255.0F);
                if (i > 8) {
                    TextRenderer textRenderer = client.textRenderer;
                    int withAlpha = ColorHelper.withAlpha(i, this.rgb);
                    context.getMatrices().pushMatrix();
                    context.getMatrices()
                            .translate(
                                    context.getScaledWindowWidth() / 2.0F,
                                    context.getScaledWindowHeight() / 2.0F - 30.0F);
                    context.getMatrices().scale(3.0F, 3.0F);
                    int j = -textRenderer.getWidth(this.title) / 2;
                    Objects.requireNonNull(textRenderer);
                    context.drawText(textRenderer, this.title, j, -4, withAlpha, true);
                    context.getMatrices().popMatrix();
                    context.getMatrices().pushMatrix();
                    context.getMatrices()
                            .translate(
                                    context.getScaledWindowWidth() / 2.0F,
                                    context.getScaledWindowHeight() / 2.0F);
                    context.getMatrices().scale(1.5F, 1.5F);
                    int k = -textRenderer.getWidth(this.subtitle) / 2;
                    Objects.requireNonNull(textRenderer);
                    context.drawText(textRenderer, this.subtitle, k, -4, withAlpha, true);
                    context.getMatrices().popMatrix();
                }
            }
        }
    }
}
