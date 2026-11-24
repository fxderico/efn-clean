package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MenuIcons {

    private MenuIcons() {}

    public static void draw(
            ImDrawList imDrawList,
            MenuAction menuAction,
            float f,
            float g,
            float h,
            int i,
            float f2) {
        switch (menuAction) {
            case LANGUAGE:
                globe(imDrawList, f, g, h, i, f2);
                break;
            case ACCESSIBILITY:
                figure(imDrawList, f, g, h, i, f2);
                break;
            case EFN:
                bolt(imDrawList, f, g, h, i, f2);
                break;
            case MODS:
                folder(imDrawList, f, g, h, i, f2);
                break;
            case ACCOUNTS:
                person(imDrawList, f, g, h, i, f2);
                break;
            case CREDITS:
                scroll(imDrawList, f, g, h, i, f2);
                break;
            default:
                imDrawList.addCircleFilled(f, g, h * 0.25F, i, 12);
        }
    }

    public static void person(ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        imDrawList.addCircle(f, g - h * 0.38F, h * 0.42F, i, 24, f2);
        imDrawList.pathClear();
        imDrawList.pathArcTo(f, g + h * 0.95F, h * 0.82F, 3.1415927F, 6.2831855F, 20);
        imDrawList.pathStroke(i, 0, f2);
    }

    public static void bolt(ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        ellipse(imDrawList, f, g - h * 0.55F, h * 0.36F, h * 0.45F, i, f2);
        imDrawList.addLine(
                f - h * 0.72F, g - h * 0.05F, f + h * 0.72F, g - h * 0.05F, i, f2 * 1.2F);
        imDrawList.addLine(f, g - h * 0.1F, f, g + h * 1.05F, i, f2 * 1.2F);
    }

    public static void globe(ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        imDrawList.addCircle(f, g, h, i, 32, f2);
        ellipse(imDrawList, f, g, h * 0.45F, h, i, f2);
        imDrawList.addLine(f - h, g, f + h, g, i, f2);
    }

    public static void figure(ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        imDrawList.addCircleFilled(f, g - h * 0.8F, h * 0.22F, i, 16);
        imDrawList.addLine(f - h * 0.85F, g - h * 0.35F, f + h * 0.85F, g - h * 0.35F, i, f2);
        imDrawList.addLine(f, g - h * 0.35F, f, g + h * 0.3F, i, f2);
        imDrawList.addLine(f, g + h * 0.3F, f - h * 0.5F, g + h, i, f2);
        imDrawList.addLine(f, g + h * 0.3F, f + h * 0.5F, g + h, i, f2);
    }

    public static void folder(ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        float g2 = f - h;
        float h2 = f + h;
        float f3 = g - h * 0.7F;
        float g3 = g + h * 0.75F;
        imDrawList.pathClear();
        imDrawList.pathLineTo(g2, g3);
        imDrawList.pathLineTo(g2, f3);
        imDrawList.pathLineTo(f - h * 0.2F, f3);
        imDrawList.pathLineTo(f, f3 + h * 0.3F);
        imDrawList.pathLineTo(h2, f3 + h * 0.3F);
        imDrawList.pathLineTo(h2, g3);
        imDrawList.pathStroke(i, 1, f2);
    }

    public static void scroll(ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        float g2 = f - h * 0.62F;
        float h2 = f + h * 0.62F;
        float f3 = g - h * 0.78F;
        float g3 = g + h * 0.78F;
        imDrawList.addRect(g2, f3, h2, g3, i, 0.0F, 0, f2);
        imDrawList.addLine(g2 - h * 0.28F, f3, h2 + h * 0.28F, f3, i, f2 * 1.3F);
        imDrawList.addLine(g2 - h * 0.28F, g3, h2 + h * 0.28F, g3, i, f2 * 1.3F);
        imDrawList.addLine(g2 + h * 0.3F, g - h * 0.22F, h2 - h * 0.3F, g - h * 0.22F, i, f2);
        imDrawList.addLine(g2 + h * 0.3F, g + h * 0.22F, h2 - h * 0.3F, g + h * 0.22F, i, f2);
    }

    public static void ellipse(
            ImDrawList imDrawList, float f, float g, float h, float f2, int i, float g2) {
        imDrawList.pathClear();
        byte b = 28;
        for (int m = 0; m < b; m++) {
            float h2 = (float) (m * 3.141592653589793 * 2.0 / b);
            imDrawList.pathLineTo(f + (float) Math.cos(h2) * h, g + (float) Math.sin(h2) * f2);
        }
        imDrawList.pathStroke(i, 1, g2);
    }
}
