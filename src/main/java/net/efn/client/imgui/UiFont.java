package net.efn.client.imgui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public enum UiFont {
    GEIST("Geist", "geist-medium.ttf", "geist-semibold.ttf", 1.0F, 0, 0, false, 0.546F, 0.0F),
    MANROPE("Manrope", "manrope-medium.ttf", "manrope-bold.ttf", 1.05F, 0, 0, false, 0.527F, 0.0F),
    RAJDHANI(
            "Rajdhani",
            "rajdhani-semibold.ttf",
            "rajdhani-bold.ttf",
            1.12F,
            0,
            0,
            false,
            0.504F,
            0.0F),
    SILKSCREEN(
            "Silkscreen",
            "silkscreen-regular.ttf",
            "silkscreen-bold.ttf",
            0.874F,
            8,
            2,
            true,
            0.7F,
            0.18F),
    JERSEY(
            "Jersey 10",
            "jersey10-regular.ttf",
            "jersey10-regular.ttf",
            1.09F,
            20,
            1,
            false,
            0.5F,
            0.0F);

    private static final String DIR = "/assets/efn/fonts/";

    public final String label;

    private final String regular;

    private final String strong;

    private final float scale;

    private final int step;

    private final int minSteps;

    private final boolean emSized;

    private final float capRatio;

    private final float lift;

    private UiFont(
            String label2,
            String regular2,
            String strong2,
            float scale2,
            int step2,
            int minSteps2,
            boolean emSized2,
            float capRatio2,
            float lift2) {
        this.label = label2;
        this.regular = regular2;
        this.strong = strong2;
        this.scale = scale2;
        this.step = step2;
        this.minSteps = minSteps2;
        this.emSized = emSized2;
        this.capRatio = capRatio2;
        this.lift = lift2;
    }

    public static String[] labels() {
        return Arrays.stream(values()).map(uiFont -> uiFont.label).toArray(String[]::new);
    }

    public static UiFont of(String string) {
        for (UiFont uiFont : values()) {
            if (uiFont.label.equals(string)) {
                return uiFont;
            }
        }
        return GEIST;
    }

    String resource(boolean bl) {
        return "/assets/efn/fonts/" + (bl ? this.strong : this.regular);
    }

    boolean pixel() {
        return this.step > 0;
    }

    float size(float f) {
        float g = f * this.scale;
        return !this.pixel()
                ? Math.round(g)
                : this.step * Math.max(this.minSteps, Math.round(g / this.step));
    }

    int flags() {
        if (!this.pixel()) {
            return 8;
        } else {
            short s = 129;
            return this.emSized ? s | 512 : s;
        }
    }

    float offsetY(float f) {
        return -Math.round(this.lift * f);
    }

    float fallbackSize(float f) {
        return Math.round(f * this.capRatio / GEIST.capRatio);
    }
}
