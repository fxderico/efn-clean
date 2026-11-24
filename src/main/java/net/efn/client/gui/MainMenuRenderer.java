package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;

import net.efn.client.gui.mainmenu.CommunityPopup;
import net.efn.client.gui.mainmenu.ConnectionsPanel;
import net.efn.client.gui.mainmenu.CreditsPopup;
import net.efn.client.gui.mainmenu.EfnMenu;
import net.efn.client.gui.mainmenu.MenuContext;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class MainMenuRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final float NOWHERE = -10000.0F;

    private final MenuContext context;

    private final CommunityPopup popup = new CommunityPopup();

    private final ConnectionsPanel accounts = new ConnectionsPanel(() -> {});

    private final CreditsPopup credits = new CreditsPopup();

    private long openedAt = System.nanoTime();

    private boolean failed;

    MainMenuRenderer(Screen screen) {
        this.context =
                new MenuContext(
                        screen,
                        () -> this.accounts.open(ConnectionsPanel.Tab.ACCOUNTS),
                        this.credits::open);
    }

    boolean keyPressed(int i) {
        return this.popup.keyPressed(i)
                || this.credits.keyPressed(i)
                || this.accounts.keyPressed(i);
    }

    void render() {
        MinecraftClient client = MinecraftClient.getInstance();
        long l = System.nanoTime();
        if (client.getOverlay() != null) {
            this.openedAt = l;
        }
        ImGuiIO iO = ImGui.getIO();
        boolean bl = client.getOverlay() == null;
        float f = (float) (l - this.openedAt) / 1.0E9F;
        float mousePosX = iO.getMousePosX();
        float mousePosY = iO.getMousePosY();
        boolean bl2 = bl && ImGui.isMouseClicked(0);
        float displaySizeX = iO.getDisplaySizeX();
        float displaySizeY = iO.getDisplaySizeY();
        float uiScale = ImGuiManager.getInstance().uiScale();
        float g = Math.max(0.001F, iO.getDeltaTime());
        this.popup.update(f, bl && !this.accounts.isOpen() && !this.credits.isOpen());
        boolean bl3 = !this.accounts.isOpen() && !this.credits.isOpen() && !this.popup.isOpen();
        this.context.begin(
                ImGui.getBackgroundDrawList(),
                ImGui.getForegroundDrawList(),
                displaySizeX,
                displaySizeY,
                uiScale,
                f,
                g,
                bl3 ? mousePosX : -10000.0F,
                bl3 ? mousePosY : -10000.0F,
                bl3 && bl2);
        try {
            EfnMenu.render(this.context);
        } catch (RuntimeException exception) {
            if (!this.failed) {
                LOGGER.error("[EFN] Main menu failed to render", (Throwable) exception);
            }
            this.failed = true;
            return;
        }
        if (this.context.pendingTooltip() != null) {
            drawTooltip(this.context);
        }
        this.accounts.render(displaySizeX, displaySizeY, uiScale, g);
        this.credits.render(
                ImGui.getForegroundDrawList(),
                displaySizeX,
                displaySizeY,
                uiScale,
                g,
                mousePosX,
                mousePosY,
                bl2);
        this.popup.render(
                ImGui.getForegroundDrawList(),
                displaySizeX,
                displaySizeY,
                uiScale,
                g,
                mousePosX,
                mousePosY,
                bl2);
    }

    private static void drawTooltip(MenuContext menuContext) {
        ImDrawList imDrawList = menuContext.fg();
        ImFont imFont = menuContext.tagFont();
        String pendingTooltip = menuContext.pendingTooltip();
        float textHeight = menuContext.textHeight(imFont);
        float f = menuContext.textWidth(imFont, pendingTooltip) + menuContext.px(18.0F);
        float g = textHeight + menuContext.px(9.0F);
        float h = menuContext.tooltipX() - f * 0.5F;
        float tooltipY = menuContext.tooltipY();
        imDrawList.addRectFilled(
                h,
                tooltipY,
                h + f,
                tooltipY + g,
                MenuContext.col(-16053492, 0.96F),
                menuContext.px(3.0F));
        imDrawList.addRect(
                h,
                tooltipY,
                h + f,
                tooltipY + g,
                MenuContext.col(-1, 0.25F),
                menuContext.px(3.0F),
                0,
                Math.max(1.0F, menuContext.scale()));
        menuContext.text(
                imDrawList,
                imFont,
                h + menuContext.px(9.0F),
                tooltipY + menuContext.px(4.5F),
                -921103,
                1.0F,
                pendingTooltip);
    }
}
