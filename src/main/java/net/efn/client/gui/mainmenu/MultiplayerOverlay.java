package net.efn.client.gui.mainmenu;

import com.example.client.mixin.AccessorMenusJoinMultiplayerScreen;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;

import net.efn.client.account.Account;
import net.efn.client.account.Accounts;
import net.efn.client.gui.HudDraw;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.proxy.Proxies;
import net.efn.client.proxy.Proxy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.util.Window;

@Environment(EnvType.CLIENT)
public final class MultiplayerOverlay {

    private static final int HEADER_HEIGHT = 33;

    private static final float NOWHERE = -10000.0F;

    private static final float BUTTON_H = 62.0F;

    private static final float ICON = 34.0F;

    private static final float MARGIN = 16.0F;

    private final MultiplayerScreen screen;

    private final ConnectionsPanel panel;

    private final DuskWidgets ui = new DuskWidgets();

    private final MenuFrame frame = new MenuFrame();

    private long shownAt;

    private boolean overButton;

    public MultiplayerOverlay(MultiplayerScreen screen2) {
        this.screen = screen2;
        MinecraftClient client = MinecraftClient.getInstance();
        this.panel =
                new ConnectionsPanel(
                        () ->
                                client.send(
                                        ((AccessorMenusJoinMultiplayerScreen) screen2)
                                                ::efn$refreshServerList));
    }

    public void show() {
        this.shownAt = System.nanoTime();
        ImGuiManager.getInstance().activate(this::render);
    }

    public void hide() {
        ImGuiManager.getInstance().deactivate();
    }

    public boolean takesPointer() {
        return this.overButton || this.panel.isOpen();
    }

    public boolean keyPressed(int i) {
        return this.panel.keyPressed(i);
    }

    private void render() {
        ImGuiIO iO = ImGui.getIO();
        float displaySizeX = iO.getDisplaySizeX();
        float displaySizeY = iO.getDisplaySizeY();
        float uiScale = ImGuiManager.getInstance().uiScale();
        float f = Math.max(0.001F, iO.getDeltaTime());
        boolean bl = !this.panel.isOpen();
        this.frame.begin(
                ImGui.getBackgroundDrawList(),
                ImGui.getForegroundDrawList(),
                displaySizeX,
                displaySizeY,
                uiScale,
                (float) (System.nanoTime() - this.shownAt) / 1.0E9F,
                f,
                bl ? iO.getMousePosX() : -10000.0F,
                bl ? iO.getMousePosY() : -10000.0F,
                bl && ImGui.isMouseClicked(0));
        this.ui.begin(this.frame);
        try {
            this.header();
        } finally {
            this.ui.end();
        }
        this.panel.render(displaySizeX, displaySizeY, uiScale, f);
    }

    private void header() {
        Window window = MinecraftClient.getInstance().getWindow();
        double scaleFactor = window.getScaleFactor();
        float scale = this.frame.scale();
        float intro = this.frame.intro(0.0F);
        float f = 62.0F * scale;
        float g = 16.0F * scale;
        float h = (float) (33.0 * scaleFactor);
        float f2 = Math.round((h - f) * 0.5F);
        ImFont imFont = this.frame.menuFont();
        float g2 =
                Math.max(
                                this.frame.textWidth(imFont, "Accounts"),
                                this.frame.textWidth(imFont, "Proxies"))
                        + 82.0F * scale;
        Proxy proxy = Proxies.get().enabled();
        boolean button =
                this.button(
                        ConnectionsPanel.Tab.ACCOUNTS,
                        "Accounts",
                        null,
                        g,
                        f2,
                        g2,
                        f,
                        scale,
                        intro);
        this.overButton =
                button
                        | this.button(
                                ConnectionsPanel.Tab.PROXIES,
                                "Proxies",
                                proxy,
                                g + g2 + 8.0F * scale,
                                f2,
                                g2,
                                f,
                                scale,
                                intro);
        float h2 =
                (float)
                        ((window.getScaledWidth()
                                        + MinecraftClient.getInstance()
                                                .textRenderer
                                                .getWidth(this.screen.getTitle()))
                                / 2.0
                                * scaleFactor);
        float f3 = this.frame.width() - g;
        float g3 = f3 - h2 - 3.0F * g;
        if (!(g3 < 40.0F * scale)) {
            ImFont imFont2 = this.frame.menuFont();
            ImFont imFont3 = this.frame.listFont();
            float h3 = 4.0F * scale;
            float f4 = this.frame.textHeight(imFont2) + h3 + this.frame.textHeight(imFont3);
            float g4 = Math.round((h - f4) * 0.5F);
            String fit = DuskWidgets.fit(imFont2, this.frame.displayName(), g3);
            String fit2 =
                    DuskWidgets.fit(
                            imFont3, proxy != null ? proxy.displayAddress() : "No proxy", g3);
            ImDrawList imDrawList = this.frame.bg();
            this.frame.text(
                    imDrawList,
                    imFont2,
                    f3 - this.frame.textWidth(imFont2, fit),
                    g4,
                    -1575946,
                    intro,
                    fit);
            this.frame.text(
                    imDrawList,
                    imFont3,
                    f3 - this.frame.textWidth(imFont3, fit2),
                    g4 + this.frame.textHeight(imFont2) + h3,
                    proxy != null ? -6557705 : -7693914,
                    intro,
                    fit2);
        }
    }

    private boolean button(
            ConnectionsPanel.Tab tab,
            String string,
            Proxy proxy,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2) {
        ImDrawList imDrawList = this.frame.bg();
        boolean hovered = this.ui.hovered(f, g, h, f2);
        if (this.ui.pressed(f, g, h, f2)) {
            this.panel.open(tab);
        }
        float hover = this.ui.hover(tab, hovered);
        imDrawList.addRectFilled(f, g, f + h, g + f2, HudDraw.col(-15461610, 0.88F * h2));
        if (hover > 0.01F) {
            imDrawList.addRectFilled(
                    f, g, f + h, g + f2, HudDraw.col(-1575946, 0.06F * hover * h2));
        }
        imDrawList.addRect(
                f,
                g,
                f + h,
                g + f2,
                HudDraw.col(HudDraw.mix(-1575946, -15408912, hover), (0.18F + 0.4F * hover) * h2),
                0.0F,
                0,
                Math.max(1.0F, g2));
        float f3 = 34.0F * g2;
        float g3 = f + 16.0F * g2;
        float h3 = g + (f2 - f3) * 0.5F;
        if (tab == ConnectionsPanel.Tab.ACCOUNTS) {
            Account account = Accounts.get().active();
            if (account != null) {
                PlayerFaces.draw(imDrawList, account, g3, h3, f3, g2, h2);
            } else {
                PlayerFaces.draw(
                        imDrawList,
                        MinecraftClient.getInstance().getSession().getUuidOrNull(),
                        false,
                        g3,
                        h3,
                        f3,
                        g2,
                        h2);
            }
        } else {
            ConnectionsPanel.proxyDot(
                    imDrawList, g3 + f3 * 0.5F, h3 + f3 * 0.5F, proxy != null, 2.1F * g2, h2);
        }
        ImFont imFont = this.frame.menuFont();
        this.frame.text(
                imDrawList,
                imFont,
                g3 + f3 + 12.0F * g2,
                g + (f2 - this.frame.textHeight(imFont)) * 0.5F,
                HudDraw.mix(-1575946, -6621958, hover),
                h2,
                string);
        return hovered;
    }
}
