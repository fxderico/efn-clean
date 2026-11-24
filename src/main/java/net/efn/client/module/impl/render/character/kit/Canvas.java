package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class Canvas {

    private final int width;

    private final int height;

    final int[] pixels;

    public Canvas(int width2, int height2) {
        if (width2 >= 1 && height2 >= 1 && width2 <= 2048 && height2 <= 2048) {
            this.width = width2;
            this.height = height2;
            this.pixels = new int[width2 * height2];
        } else {
            throw new IllegalArgumentException("canvas size must be between 1 and 2048 pixels");
        }
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int get(int i, int j) {
        return i >= 0 && j >= 0 && i < this.width && j < this.height
                ? this.pixels[j * this.width + i]
                : 0;
    }

    public int[] pixels() {
        return (int[]) this.pixels.clone();
    }

    public Canvas clear(int i) {
        Arrays.fill(this.pixels, i);
        return this;
    }

    public Canvas fill(Shape2D shape2D, int i) {
        return this.fill(shape2D, Paint2D.solid(i), 0.0F);
    }

    public Canvas fill(Shape2D shape2D, int i, float f) {
        return this.fill(shape2D, Paint2D.solid(i), f);
    }

    public Canvas fill(Shape2D shape2D, Paint2D paint2D) {
        return this.fill(shape2D, paint2D, 0.0F);
    }

    public Canvas fill(Shape2D shape2D, Paint2D paint2D, float f) {
        float g = Math.max(1.0F, f);
        int i = Math.max(0, (int) Math.floor(shape2D.minX - g - 1.0F));
        int j = Math.max(0, (int) Math.floor(shape2D.minY - g - 1.0F));
        int k = Math.min(this.width - 1, (int) Math.ceil(shape2D.maxX + g + 1.0F));
        int m = Math.min(this.height - 1, (int) Math.ceil(shape2D.maxY + g + 1.0F));
        for (int n = j; n <= m; n++) {
            float h = n + 0.5F;
            for (int o = i; o <= k; o++) {
                float f2 = o + 0.5F;
                float g2 = 0.5F - shape2D.distance(f2, h) / g;
                if (!(g2 <= 0.0F)) {
                    if (g2 > 1.0F) {
                        g2 = 1.0F;
                    }
                    this.blend(n * this.width + o, paint2D.argb(f2, h), g2);
                }
            }
        }
        return this;
    }

    public Canvas fillAll(Paint2D paint2D) {
        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                this.blend(i * this.width + j, paint2D.argb(j + 0.5F, i + 0.5F), 1.0F);
            }
        }
        return this;
    }

    public Canvas erase(Shape2D shape2D) {
        return this.erase(shape2D, 0.0F);
    }

    public Canvas erase(Shape2D shape2D, float f) {
        float g = Math.max(1.0F, f);
        int i = Math.max(0, (int) Math.floor(shape2D.minX - g - 1.0F));
        int j = Math.max(0, (int) Math.floor(shape2D.minY - g - 1.0F));
        int k = Math.min(this.width - 1, (int) Math.ceil(shape2D.maxX + g + 1.0F));
        int m = Math.min(this.height - 1, (int) Math.ceil(shape2D.maxY + g + 1.0F));
        for (int n = j; n <= m; n++) {
            for (int o = i; o <= k; o++) {
                float h = 0.5F - shape2D.distance(o + 0.5F, n + 0.5F) / g;
                if (!(h <= 0.0F)) {
                    int p = n * this.width + o;
                    int q = Math.round((this.pixels[p] >>> 24) * (1.0F - Math.min(1.0F, h)));
                    this.pixels[p] = q << 24 | this.pixels[p] & 16777215;
                }
            }
        }
        return this;
    }

    public Canvas draw(Canvas canvas, int i, int j) {
        for (int k = 0; k < canvas.height; k++) {
            int m = k + j;
            if (m >= 0 && m < this.height) {
                for (int n = 0; n < canvas.width; n++) {
                    int o = n + i;
                    if (o >= 0 && o < this.width) {
                        this.blend(m * this.width + o, canvas.pixels[k * canvas.width + n], 1.0F);
                    }
                }
            }
        }
        return this;
    }

    public Canvas mirrored() {
        Canvas canvas = new Canvas(this.width, this.height);
        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                canvas.pixels[i * this.width + j] =
                        this.pixels[i * this.width + this.width - 1 - j];
            }
        }
        return canvas;
    }

    public Canvas copy() {
        Canvas canvas = new Canvas(this.width, this.height);
        System.arraycopy(this.pixels, 0, canvas.pixels, 0, this.pixels.length);
        return canvas;
    }

    private void blend(int i, int j, float f) {
        float g = (j >>> 24) / 255.0F * f;
        if (!(g <= 0.0F)) {
            int pixel = this.pixels[i];
            float h = (pixel >>> 24) / 255.0F;
            float f2 = g + h * (1.0F - g);
            if (f2 <= 0.0F) {
                this.pixels[i] = 0;
            } else {
                float g2 = h * (1.0F - g) / f2;
                float h2 = g / f2;
                int k = Math.round((j >> 16 & 0xFF) * h2 + (pixel >> 16 & 0xFF) * g2);
                int m = Math.round((j >> 8 & 0xFF) * h2 + (pixel >> 8 & 0xFF) * g2);
                int n = Math.round((j & 0xFF) * h2 + (pixel & 0xFF) * g2);
                int o = Math.round(f2 * 255.0F);
                this.pixels[i] =
                        Colors.clamp(o) << 24
                                | Colors.clamp(k) << 16
                                | Colors.clamp(m) << 8
                                | Colors.clamp(n);
            }
        }
    }
}
