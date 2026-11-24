package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;

import net.efn.client.gui.ImGuiScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.util.Util;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class MenuContext extends MenuFrame {

    private static final float LOOK_EASE = 2.5F;

    private final Screen screen;

    private final Runnable openAccounts;

    private final Runnable openCredits;

    private final float[] hover = new float[MenuAction.values().length];

    private float lookX;

    private float lookY;

    public MenuContext(Screen screen2, Runnable openAccounts2, Runnable openCredits2) {
        this.screen = screen2;
        this.openAccounts = openAccounts2;
        this.openCredits = openCredits2;
    }

    @Override
    public void begin(
            ImDrawList imDrawList,
            ImDrawList imDrawList2,
            float f,
            float g,
            float h,
            float i,
            float j,
            float k,
            float f2,
            boolean bl) {
        super.begin(imDrawList, imDrawList2, f, g, h, i, j, k, f2, bl);
        boolean bl2 = k >= 0.0F && f2 >= 0.0F && k <= f && f2 <= g;
        float g2 = bl2 ? k / f * 2.0F - 1.0F : 0.0F;
        float h2 = bl2 ? f2 / g * 2.0F - 1.0F : 0.0F;
        float f3 = Math.min(1.0F, j * 2.5F);
        this.lookX = this.lookX + (g2 - this.lookX) * f3;
        this.lookY = this.lookY + (h2 - this.lookY) * f3;
    }

    public boolean accountsAvailable() {
        return this.openAccounts != null;
    }

    public float lookX() {
        return this.lookX;
    }

    public float lookY() {
        return this.lookY;
    }

    public boolean enabled(MenuAction menuAction) {
        return menuAction != MenuAction.MULTIPLAYER && menuAction != MenuAction.REALMS
                || MinecraftClient.getInstance().isMultiplayerEnabled();
    }

    public float control(MenuAction menuAction, float f, float g, float h, float f2) {
        boolean inside = this.inside(f, g, h, f2);
        if (!this.enabled(menuAction)) {
            if (inside) {
                this.tooltip("Multiplayer is disabled", this.mouseX(), g + f2 + this.px(6.0F));
            }
            return this.hover(menuAction, false);
        } else {
            if (inside && this.clicked()) {
                this.activate(menuAction);
            }
            return this.hover(menuAction, inside);
        }
    }

    public float hover(MenuAction menuAction, boolean bl) {
        float f = this.hover[menuAction.ordinal()];
        f += ((bl ? 1.0F : 0.0F) - f) * Math.min(1.0F, this.dt() * 12.0F);
        this.hover[menuAction.ordinal()] = f;
        return f;
    }

    public void activate(MenuAction menuAction) {
        if (this.enabled(menuAction)) {
            MinecraftClient client = MinecraftClient.getInstance();
            Supplier supplier =
                    switch (menuAction) {
                        case SINGLEPLAYER -> () -> new SelectWorldScreen(this.screen);
                        case MULTIPLAYER ->
                                () ->
                                        (Screen)
                                                (client.options.skipMultiplayerWarning
                                                        ? new MultiplayerScreen(this.screen)
                                                        : new MultiplayerWarningScreen(
                                                                this.screen));
                        case REALMS -> () -> new RealmsMainScreen(this.screen);
                        case OPTIONS -> () -> new OptionsScreen(this.screen, client.options);
                        case LANGUAGE ->
                                () ->
                                        new LanguageOptionsScreen(
                                                this.screen,
                                                client.options,
                                                client.getLanguageManager());
                        case ACCESSIBILITY ->
                                () -> new AccessibilityOptionsScreen(this.screen, client.options);
                        case EFN -> ImGuiScreen::new;
                        case QUIT, MODS, ACCOUNTS, CREDITS -> null;
                    };
            if (menuAction == MenuAction.QUIT) {
                client.execute(() -> client.scheduleStop());
            } else if (menuAction == MenuAction.ACCOUNTS) {
                if (this.openAccounts != null) {
                    this.openAccounts.run();
                }
            } else if (menuAction == MenuAction.CREDITS) {
                this.openCredits.run();
            } else if (menuAction == MenuAction.MODS) {
                client.execute(
                        () ->
                                Util.getOperatingSystem()
                                        .open(
                                                FabricLoader.getInstance()
                                                        .getGameDir()
                                                        .resolve("mods")));
            } else if (supplier != null) {
                client.send(() -> client.setScreen((Screen) supplier.get()));
            }
        }
    }
}
