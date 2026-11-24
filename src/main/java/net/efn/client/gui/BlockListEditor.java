package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.BlockColorListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
final class BlockListEditor {

    private static final int INLINE_MAX_ROWS = 8;

    private static final float HEADER_H = 40.0F;

    private static final float INLINE_ROW_H = 38.0F;

    private static final float LIST_PAD = 4.0F;

    private static final float EMPTY_H = 44.0F;

    private static final float PICKER_W = 820.0F;

    private static final float PICKER_MAX_H = 760.0F;

    private static final float PICKER_ROW_H = 42.0F;

    private static final float COLUMN_GAP = 28.0F;

    private static final float TAB_BAR_ROOM = 86.0F;

    private static final float ICON_SLOT = 32.0F;

    private static final float COLOR_CONTROLS_W = 88.0F;

    private static final float COLOR_COLUMN_EXTRA = 98.0F;

    private static final float COLOR_WINDOW_GAP = 12.0F;

    private static final float COLOR_WINDOW_W = 290.0F;

    private static final float COLOR_WINDOW_H = 360.0F;

    private static final int ADD_ALL_CONFIRM_OVER = 64;

    private static final long ARM_NANOS = 3000000000L;

    private static final int ARM_RESET = 1;

    private static final int ARM_CLEAR = 2;

    private static final int ARM_ADD = 3;

    private static final int LIST_BG = EfnWidgets.withAlpha(-13226454, 0.5F);

    private static final int ROW_HOVER = 318767103;

    private static final int SCRIM = 1929379840;

    private static final int TRACER_ACCENT = -12276993;

    private static final String SELECT_LABEL = "Select";

    private static final String EMPTY_TEXT = "No blocks yet";

    private static final String PICKER_TITLE = "Select Blocks";

    private static final String AVAILABLE_TITLE = "All blocks";

    private static final String CHOSEN_TITLE = "Selected";

    private static final String NO_MATCHES = "No blocks match";

    private static final String ALL_CHOSEN = "Every block is selected";

    private static final String NONE_CHOSEN = "Nothing selected yet";

    private static final String SEARCH_HINT = "Search blocks...";

    private static final String RESET_LABEL = "Reset";

    private static final String ADD_SHOWN_LABEL = "Add all shown";

    private static final String CLEAR_LABEL = "Clear all";

    private static final String CONFIRM_LABEL = "Confirm";

    private static final int PICKER_FLAGS = 8639;

    private static final int LEV_INSERT = 1;

    private static final int LEV_SUBSTITUTE = 8;

    private static final int LEV_DELETE = 8;

    private static final Reference2ObjectOpenHashMap<Block, BlockListEditor.BlockInfo> INFOS =
            new Reference2ObjectOpenHashMap<>();

    private static final Map<BlockColorListSetting, BlockListEditor.ListState> LISTS =
            new WeakHashMap<>();

    private static String language;

    private static int iconsFrame = -2;

    private static int namesGeneration;

    private static int[] levPrev = new int[64];

    private static int[] levCur = new int[64];

    private final Reference2ObjectOpenHashMap<BlockColorListSetting, float[]> inlineScroll =
            new Reference2ObjectOpenHashMap<>();

    private BlockColorListSetting picker;

    private BlockListEditor.ListState pickerList;

    private String pickerSubtitle;

    private final ImString search = new ImString(64);

    private boolean focusSearch;

    private boolean searchActive;

    private long pickerOpenedAt;

    private boolean pickerJustOpened;

    private final float[] availableScroll = new float[2];

    private final float[] chosenScroll = new float[2];

    private int armedAction;

    private BlockColorListSetting armedFor;

    private long armedAt;

    private BlockColorListSetting colorList;

    private BlockColorListSetting.BlockEntry colorEntry;

    private boolean colorTracer;

    private String colorTitle;

    private final ImBoolean colorOpen = new ImBoolean(false);

    private float colorAtX;

    private float colorAtY;

    private boolean colorFocus;

    BlockListEditor() {}

    float render(ClientModule module, BlockColorListSetting setting, float f, float g, float h) {
        beginFrame();
        BlockListEditor.ListState listState = list(setting);
        refreshChosen(setting, listState);
        boolean bl = !setting.colorsHidden();
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        ImFont imFont = ImGuiManager.getInstance().moduleFont();
        ImFont imFont2 = ImGuiManager.getInstance().smallFont();
        float windowPosX = ImGui.getWindowPosX();
        float f2 = ImGui.getWindowPosY() - ImGui.getScrollY();
        ImGui.setCursorPos(f, h + EfnWidgets.s(10.0F));
        ImGui.textUnformatted(setting.name());
        float g2 = f2 + h + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F;
        countPill(
                windowDrawList,
                imFont2,
                ImGui.getItemRectMaxX() + EfnWidgets.s(8.0F),
                g2,
                listState.countPill);
        float h2 = EfnWidgets.textWidth(imFont, "Select") + EfnWidgets.s(28.0F);
        float px28 = EfnWidgets.s(28.0F);
        float f3 = windowPosX + f + g - h2;
        ImGui.setCursorScreenPos(f3, g2 - px28 * 0.5F);
        if (EfnWidgets.chipButton("##add_blocks", "Select", h2, px28, false)) {
            this.openPicker(module, setting, listState);
        }
        if (bl) {
            float px30 = EfnWidgets.s(30.0F);
            float px20 = EfnWidgets.s(20.0F);
            if (swatch(
                    windowDrawList,
                    "##default_color",
                    setting.defaultColor(),
                    f3 - EfnWidgets.s(10.0F) - px30,
                    g2 - px20 * 0.5F,
                    px30,
                    px20)) {
                this.openColor(setting, null, false);
            }
            if (ImGui.isItemHovered(32768) && !this.editing(setting, null, false)) {
                ClickGuiRenderer.showTooltip("Default colour, for blocks not on the list");
            }
        }
        float g3 = h + EfnWidgets.s(40.0F);
        float px38 = EfnWidgets.s(38.0F);
        int chosenCount2 = listState.chosenCount;
        float h3 =
                chosenCount2 == 0
                        ? EfnWidgets.s(44.0F)
                        : Math.min(chosenCount2, 8) * px38 + EfnWidgets.s(4.0F) * 2.0F;
        float f4 = windowPosX + f;
        float g4 = f2 + g3;
        float px10 = EfnWidgets.s(10.0F);
        windowDrawList.addRectFilled(f4, g4, f4 + g, g4 + h3, EfnWidgets.c(LIST_BG), px10);
        if (chosenCount2 == 0) {
            EfnWidgets.text(
                    windowDrawList,
                    imFont,
                    f4 + EfnWidgets.s(14.0F),
                    g4 + (h3 - EfnWidgets.lineHeight(imFont)) * 0.5F,
                    -4542808,
                    "No blocks yet");
        } else if (chosenCount2 <= 8) {
            this.drawChosenRows(
                    windowDrawList,
                    setting,
                    listState,
                    f,
                    g3 + EfnWidgets.s(4.0F),
                    g,
                    px38,
                    0,
                    chosenCount2,
                    bl,
                    imFont,
                    imFont2);
        } else {
            ImGui.setCursorPos(f, g3 + EfnWidgets.s(4.0F));
            if (ImGui.beginChild("##chosen", g, h3 - EfnWidgets.s(4.0F) * 2.0F, false, 24)) {
                ImGui.setCursorPos(0.0F, 0.0F);
                ImGui.dummy(1.0F, chosenCount2 * px38);
                float scrollY = ImGui.getScrollY();
                int i = Math.max(0, (int) (scrollY / px38));
                int j =
                        Math.min(
                                chosenCount2,
                                (int) Math.ceil((scrollY + ImGui.getWindowHeight()) / px38));
                this.drawChosenRows(
                        ImGui.getWindowDrawList(),
                        setting,
                        listState,
                        0.0F,
                        0.0F,
                        g,
                        px38,
                        i,
                        j,
                        bl,
                        imFont,
                        imFont2);
                ClickGuiRenderer.smoothScroll(
                        this.inlineScroll.computeIfAbsent(setting, value -> new float[2]),
                        frameDelta());
            }
            ImGui.endChild();
        }
        return EfnWidgets.s(40.0F) + h3 + EfnWidgets.s(12.0F);
    }

    private void drawChosenRows(
            ImDrawList imDrawList,
            BlockColorListSetting setting,
            BlockListEditor.ListState listState,
            float f,
            float g,
            float h,
            float f2,
            int i,
            int j,
            boolean bl,
            ImFont imFont,
            ImFont imFont2) {
        boolean windowHovered = ImGui.isWindowHovered();
        float g2 = BlockIcons.size(EfnWidgets.s(32.0F));
        float px22 = EfnWidgets.s(22.0F);
        float lineHeight = EfnWidgets.lineHeight(imFont);
        float lineHeight2 = EfnWidgets.lineHeight(imFont2);
        BlockListEditor.Row row = null;
        for (int m = i; m < j; m++) {
            BlockListEditor.Row row2 = listState.chosen[m];
            ImGui.setCursorPos(f, g + m * f2);
            float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float cursorScreenPosY = ImGui.getCursorScreenPosY();
            float h2 = cursorScreenPosY + f2 * 0.5F;
            if (windowHovered
                    && ImGui.isMouseHoveringRect(
                            cursorScreenPosX,
                            cursorScreenPosY,
                            cursorScreenPosX + h,
                            cursorScreenPosY + f2)) {
                imDrawList.addRectFilled(
                        cursorScreenPosX + EfnWidgets.s(4.0F),
                        cursorScreenPosY + EfnWidgets.s(1.0F),
                        cursorScreenPosX + h - EfnWidgets.s(4.0F),
                        cursorScreenPosY + f2 - EfnWidgets.s(1.0F),
                        EfnWidgets.c(318767103),
                        EfnWidgets.s(7.0F));
            }
            ImGui.pushID(row2.info.rawId);
            float f3 = cursorScreenPosX + h - EfnWidgets.s(10.0F);
            ImGui.setCursorScreenPos(f3 - px22, h2 - px22 * 0.5F);
            if (ImGui.invisibleButton("##remove", px22, px22)) {
                row = row2;
            }
            boolean itemHovered;
            if (itemHovered = ImGui.isItemHovered()) {
                imDrawList.addCircleFilled(
                        f3 - px22 * 0.5F, h2, px22 * 0.5F, EfnWidgets.c(-12240588), 20);
            }
            EfnWidgets.cross(
                    imDrawList,
                    f3 - px22 * 0.5F,
                    h2,
                    EfnWidgets.s(3.5F),
                    itemHovered ? -10856974 : -4542808);
            f3 -= px22 + EfnWidgets.s(8.0F);
            if (bl) {
                f3 = this.colorControls(imDrawList, setting, row2, f3, h2);
            }
            ImGui.popID();
            float g3 = cursorScreenPosX + EfnWidgets.s(8.0F);
            BlockIcons.draw(imDrawList, row2.info.block, g3, h2 - g2 * 0.5F, g2);
            float h3 = g3 + g2 + EfnWidgets.s(10.0F);
            BlockListEditor.Fit fit = fit(row2.inlineFit, row2.info, imFont, imFont2, f3 - h3);
            EfnWidgets.text(imDrawList, imFont, h3, h2 - lineHeight * 0.5F, -461582, fit.text);
            if (fit.hint != null) {
                EfnWidgets.text(
                        imDrawList,
                        imFont2,
                        h3 + fit.textW + EfnWidgets.s(6.0F),
                        h2 - lineHeight2 * 0.5F,
                        -4542808,
                        fit.hint);
            }
        }
        if (row != null) {
            deselect(setting, row);
        }
    }

    private float colorControls(
            ImDrawList imDrawList,
            BlockColorListSetting setting,
            BlockListEditor.Row row,
            float f,
            float g) {
        float px30 = EfnWidgets.s(30.0F);
        float px18 = EfnWidgets.s(18.0F);
        float px16 = EfnWidgets.s(16.0F);
        float px6 = EfnWidgets.s(6.0F);
        float h = f - px30;
        if (swatch(
                imDrawList,
                "##tracer_color",
                row.entry.tracerColor(),
                h,
                g - px18 * 0.5F,
                px30,
                px18)) {
            this.openColor(setting, row, true);
        }
        if (ImGui.isItemHovered(32768) && !this.editing(setting, row.entry, true)) {
            ClickGuiRenderer.showTooltip("Tracer colour");
        }
        if (checkbox(
                imDrawList,
                "##tracer",
                row.entry.tracerEnabled().get(),
                h = h - (px6 + px16),
                g - px16 * 0.5F,
                px16)) {
            row.entry.tracerEnabled().set(!row.entry.tracerEnabled().get());
            setting.notifyChanged();
        }
        if (ImGui.isItemHovered(32768)) {
            ClickGuiRenderer.showTooltip("Tracer");
        }
        if (swatch(
                imDrawList,
                "##box_color",
                row.entry.color(),
                h = h - (px6 + px30),
                g - px18 * 0.5F,
                px30,
                px18)) {
            this.openColor(setting, row, false);
        }
        if (ImGui.isItemHovered(32768) && !this.editing(setting, row.entry, false)) {
            ClickGuiRenderer.showTooltip("Box colour");
        }
        return h - EfnWidgets.s(10.0F);
    }

    void renderWindows() {
        if (this.picker != null) {
            this.renderPicker();
        }
        if (this.colorList != null) {
            this.renderColorWindow();
        }
    }

    void close() {
        this.closePicker();
        this.closeColor();
    }

    boolean closePicker() {
        if (this.picker == null) {
            return false;
        } else {
            this.picker = null;
            this.pickerList = null;
            this.disarm();
            return true;
        }
    }

    private void openPicker(
            ClientModule module,
            BlockColorListSetting picker2,
            BlockListEditor.ListState pickerList2) {
        this.closeColor();
        this.picker = picker2;
        this.pickerList = pickerList2;
        this.pickerSubtitle = module.name() + " / " + picker2.name();
        this.search.set("");
        this.focusSearch = true;
        this.searchActive = false;
        this.pickerOpenedAt = System.nanoTime();
        this.pickerJustOpened = true;
        this.chosenScroll[1] = 0.0F;
        this.chosenScroll[0] = 0.0F;
        this.availableScroll[1] = 0.0F;
        this.availableScroll[0] = 0.0F;
        this.disarm();
    }

    private void renderPicker() {
        beginFrame();
        BlockColorListSetting picker2 = this.picker;
        BlockListEditor.ListState pickerList2 = this.pickerList;
        refreshChosen(picker2, pickerList2);
        boolean bl = !picker2.colorsHidden();
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float px16 = EfnWidgets.s(16.0F);
        float f = bl ? EfnWidgets.s(98.0F) : 0.0F;
        float g = Math.min(displaySizeX - px16 * 2.0F, EfnWidgets.s(820.0F) + f);
        float h =
                Math.min(
                        displaySizeY - EfnWidgets.s(86.0F) - EfnWidgets.s(32.0F),
                        EfnWidgets.s(760.0F));
        float easeOut =
                ClickGuiRenderer.easeOut(ClickGuiRenderer.elapsed(this.pickerOpenedAt, 200.0F));
        float f2 = Math.round((displaySizeX - g) * 0.5F);
        float g2 =
                Math.round(
                        Math.max(
                                        px16,
                                        (displaySizeY - EfnWidgets.s(86.0F) - h) * 0.5F
                                                + EfnWidgets.s(8.0F))
                                + EfnWidgets.s(14.0F) * (1.0F - easeOut));
        float cardRounding = ClickGuiRenderer.cardRounding();
        ImGui.setNextWindowPos(0.0F, 0.0F, 1);
        ImGui.setNextWindowSize(displaySizeX, displaySizeY, 1);
        if (this.pickerJustOpened) {
            ImGui.setNextWindowFocus();
        }
        ImGui.pushStyleVar(0, Math.max(0.01F, easeOut));
        ImGui.pushStyleVar(2, 0.0F, 0.0F);
        ImGui.pushStyleVar(14, 0.0F, 0.0F);
        ImGui.pushStyleVar(4, 0.0F);
        boolean bl2 = false;
        boolean begin = ImGui.begin("##block_picker", 8639);
        if (this.pickerJustOpened) {
            imgui.internal.ImGui.bringWindowToDisplayFront(imgui.internal.ImGui.getCurrentWindow());
        }
        if (begin) {
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            windowDrawList.addRectFilled(
                    0.0F, 0.0F, displaySizeX, displaySizeY, EfnWidgets.c(1929379840));
            shadow(windowDrawList, f2, g2, g, h, cardRounding);
            windowDrawList.addRectFilled(
                    f2,
                    g2,
                    f2 + g,
                    g2 + h,
                    EfnWidgets.c(GuiPalette.abgr(ThemeStore.get().cards.color.argbWithAlpha(255))),
                    cardRounding);
            windowDrawList.addRect(
                    f2, g2, f2 + g, g2 + h, EfnWidgets.c(318767103), cardRounding, 0, 1.0F);
            ImFont imFont = ImGuiManager.getInstance().moduleFont();
            ImFont imFont2 = ImGuiManager.getInstance().smallFont();
            float px24 = EfnWidgets.s(24.0F);
            float h2 = g2 + EfnWidgets.s(22.0F);
            float px28 = EfnWidgets.s(28.0F);
            ClickGuiRenderer.drawCardTitle("Select Blocks", f2 + px24, h2 + px28 * 0.5F);
            ImGui.setCursorPos(f2 + g - px24 - px28, h2);
            if (EfnWidgets.iconButton("##close", px28)) {
                bl2 = true;
            }
            EfnWidgets.cross(
                    windowDrawList,
                    f2 + g - px24 - px28 * 0.5F,
                    h2 + px28 * 0.5F,
                    EfnWidgets.s(4.5F),
                    -461582);
            if (chip(
                            "##reset_blocks",
                            "Reset",
                            this.armed(1, picker2),
                            picker2.isDefault(),
                            f2 + g - px24 - px28 - EfnWidgets.s(10.0F),
                            h2 + px28 * 0.5F,
                            imFont2,
                            "Back to the default blocks")
                    && this.confirm(1, picker2)) {
                picker2.resetToDefault();
            }
            float f3 = h2 + px28 + EfnWidgets.s(2.0F);
            EfnWidgets.text(windowDrawList, imFont2, f2 + px24, f3, -4542808, this.pickerSubtitle);
            ImGui.setCursorPos(
                    f2 + px24, f3 + EfnWidgets.lineHeight(imFont2) + EfnWidgets.s(14.0F));
            ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
            ImGui.setNextItemWidth(g - px24 * 2.0F);
            if (this.focusSearch) {
                ImGui.setKeyboardFocusHere();
                this.focusSearch = false;
            }
            boolean bl3 =
                    this.searchActive
                            && (ImGui.isKeyPressed(525, false) || ImGui.isKeyPressed(627, false));
            ImGui.inputTextWithHint("##block_search", "Search blocks...", this.search);
            this.searchActive = ImGui.isItemActive();
            ImGui.popStyleVar();
            float g3 = ImGui.getItemRectMaxY() + EfnWidgets.s(18.0F);
            String searchKey2 = pickerList2.searchKey;
            refreshSearch(pickerList2, this.search.get());
            if (pickerList2.searchKey != searchKey2) {
                this.chosenScroll[1] = 0.0F;
                this.chosenScroll[0] = 0.0F;
                this.availableScroll[1] = 0.0F;
                this.availableScroll[0] = 0.0F;
                if (this.armedAction == 3) {
                    this.disarm();
                }
            }
            refreshColumns(picker2, pickerList2);
            if (bl3 && !pickerList2.searchKey.isBlank()) {
                if (pickerList2.availableCount > 0) {
                    select(picker2, pickerList2.available[0]);
                }
                this.focusSearch = true;
            }
            float h3 = (g - px24 * 2.0F - EfnWidgets.s(28.0F) - f) * 0.5F;
            float f4 = h3 + f;
            float g4 = f2 + px24;
            float h4 = g4 + h3 + EfnWidgets.s(28.0F);
            float px282 = EfnWidgets.s(28.0F);
            float f5 = g3 + px282 + EfnWidgets.s(10.0F);
            float g5 = g2 + h - EfnWidgets.s(18.0F);
            if (pickerList2.availableLabelFor != pickerList2.availableCount) {
                pickerList2.availableLabelFor = pickerList2.availableCount;
                pickerList2.availableLabel = Integer.toString(pickerList2.availableCount);
            }
            if (pickerList2.shownLabelFor != pickerList2.shownCount) {
                pickerList2.shownLabelFor = pickerList2.shownCount;
                pickerList2.shownLabel = Integer.toString(pickerList2.shownCount);
            }
            columnHeader(
                    windowDrawList,
                    imFont,
                    imFont2,
                    g4,
                    g3,
                    px282,
                    h3,
                    "All blocks",
                    pickerList2.availableLabel);
            columnHeader(
                    windowDrawList,
                    imFont,
                    imFont2,
                    h4,
                    g3,
                    px282,
                    f4,
                    "Selected",
                    pickerList2.shownLabel);
            float h5 = g3 + px282 * 0.5F;
            boolean chip =
                    chip(
                            "##add_shown",
                            "Add all shown",
                            this.armed(3, picker2),
                            pickerList2.availableCount == 0,
                            g4 + h3 - EfnWidgets.s(6.0F),
                            h5,
                            imFont2,
                            "Adds every block this column lists");
            boolean chip2 =
                    chip(
                            "##clear_all",
                            "Clear all",
                            this.armed(2, picker2),
                            pickerList2.chosenCount == 0,
                            h4 + f4 - EfnWidgets.s(6.0F),
                            h5,
                            imFont2,
                            "Takes every block off the list");
            float f6 = Math.round(g4 + h3 + EfnWidgets.s(28.0F) * 0.5F);
            windowDrawList.addLine(f6, g3, f6, g5, EfnWidgets.c(268435455), 1.0F);
            float g6 = Math.max(1.0F, g5 - f5);
            String string =
                    pickerList2.searchKey.isBlank() ? "Every block is selected" : "No blocks match";
            String string2 =
                    pickerList2.searchKey.isBlank() ? "Nothing selected yet" : "No blocks match";
            BlockListEditor.Row row =
                    this.column(
                            "##available",
                            pickerList2.available,
                            pickerList2.availableCount,
                            true,
                            null,
                            g4,
                            f5,
                            h3,
                            g6,
                            this.availableScroll,
                            string,
                            imFont,
                            imFont2);
            BlockListEditor.Row row2 =
                    this.column(
                            "##chosen",
                            pickerList2.shown,
                            pickerList2.shownCount,
                            false,
                            bl ? picker2 : null,
                            h4,
                            f5,
                            f4,
                            g6,
                            this.chosenScroll,
                            string2,
                            imFont,
                            imFont2);
            if (row != null) {
                select(picker2, row);
            }
            if (row2 != null) {
                deselect(picker2, row2);
            }
            if (chip) {
                this.addAllShown(picker2, pickerList2);
            }
            if (chip2) {
                this.clearAll(picker2);
            }
            if (!this.pickerJustOpened
                    && ImGui.isMouseClicked(0)
                    && ImGui.isWindowHovered()
                    && !ImGui.isMouseHoveringRect(f2, g2, f2 + g, g2 + h)) {
                bl2 = true;
            }
        }
        ImGui.end();
        ImGui.popStyleVar(4);
        this.pickerJustOpened = false;
        if (bl2) {
            this.closePicker();
        }
    }

    private static boolean chip(
            String string,
            String string2,
            boolean bl,
            boolean bl2,
            float f,
            float g,
            ImFont imFont,
            String string3) {
        boolean pushFont = ClickGuiRenderer.pushFont(imFont);
        float h =
                Math.max(
                                EfnWidgets.textWidth(imFont, string2),
                                EfnWidgets.textWidth(imFont, "Confirm"))
                        + EfnWidgets.s(20.0F);
        float f2 = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(12.0F);
        ImGui.setCursorPos(f - h, g - f2 * 0.5F);
        ImGui.beginDisabled(bl2);
        boolean chipButton = EfnWidgets.chipButton(string, bl ? "Confirm" : string2, h, f2, bl);
        ImGui.endDisabled();
        if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip(string3);
        }
        if (pushFont) {
            ImGui.popFont();
        }
        return chipButton;
    }

    private boolean confirm(int armedAction2, BlockColorListSetting armedFor2) {
        if (this.armed(armedAction2, armedFor2)) {
            this.disarm();
            return true;
        } else {
            this.armedAction = armedAction2;
            this.armedFor = armedFor2;
            this.armedAt = System.nanoTime();
            return false;
        }
    }

    private boolean armed(int i, BlockColorListSetting setting) {
        return this.armedAction == i
                && this.armedFor == setting
                && System.nanoTime() - this.armedAt < 3000000000L;
    }

    private void disarm() {
        this.armedAction = 0;
        this.armedFor = null;
    }

    private static void columnHeader(
            ImDrawList imDrawList,
            ImFont imFont,
            ImFont imFont2,
            float f,
            float g,
            float h,
            float f2,
            String string,
            String string2) {
        float g2 = g + h * 0.5F;
        EfnWidgets.text(
                imDrawList,
                imFont,
                f + EfnWidgets.s(4.0F),
                g2 - EfnWidgets.lineHeight(imFont) * 0.5F,
                -461582,
                string);
        countPill(
                imDrawList,
                imFont2,
                f + EfnWidgets.s(4.0F) + EfnWidgets.textWidth(imFont, string) + EfnWidgets.s(8.0F),
                g2,
                string2);
        imDrawList.addLine(
                f,
                g + h + EfnWidgets.s(4.0F),
                f + f2,
                g + h + EfnWidgets.s(4.0F),
                EfnWidgets.c(268435455),
                1.0F);
    }

    private BlockListEditor.Row column(
            String string,
            BlockListEditor.Row[] rows,
            int i,
            boolean bl,
            BlockColorListSetting setting,
            float f,
            float g,
            float h,
            float f2,
            float[] floats,
            String string2,
            ImFont imFont,
            ImFont imFont2) {
        BlockListEditor.Row row = null;
        ImGui.setCursorPos(f, g);
        if (ImGui.beginChild(string, h, f2, false, 24)) {
            float px42 = EfnWidgets.s(42.0F);
            float g2 = h - EfnWidgets.s(6.0F);
            float h2 = px42 - EfnWidgets.s(2.0F);
            ImGui.setCursorPos(0.0F, 0.0F);
            ImGui.dummy(1.0F, i * px42 + EfnWidgets.s(6.0F));
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            if (i == 0) {
                EfnWidgets.text(
                        windowDrawList,
                        imFont,
                        ImGui.getWindowPosX() + EfnWidgets.s(8.0F),
                        ImGui.getWindowPosY() + EfnWidgets.s(12.0F),
                        -4542808,
                        string2);
            } else {
                float lineHeight = EfnWidgets.lineHeight(imFont);
                float lineHeight2 = EfnWidgets.lineHeight(imFont2);
                float f3 = BlockIcons.size(EfnWidgets.s(32.0F));
                float px11 = EfnWidgets.s(11.0F);
                float scrollY = ImGui.getScrollY();
                int j = Math.max(0, (int) (scrollY / px42));
                int k = Math.min(i, (int) Math.ceil((scrollY + ImGui.getWindowHeight()) / px42));
                int accent = GuiPalette.accent();
                boolean windowHovered = ImGui.isWindowHovered();
                for (int o = j; o < k; o++) {
                    BlockListEditor.Row row2 = rows[o];
                    ImGui.setCursorPos(0.0F, o * px42);
                    float cursorScreenPosX = ImGui.getCursorScreenPosX();
                    float cursorScreenPosY = ImGui.getCursorScreenPosY();
                    float g3 = cursorScreenPosY + h2 * 0.5F;
                    float h3 = cursorScreenPosX + g2 - EfnWidgets.s(8.0F) - px11;
                    ImGui.pushID(row2.info.rawId);
                    float f4;
                    boolean itemHovered;
                    if (setting == null) {
                        if (ImGui.invisibleButton("##row", g2, h2)) {
                            row = row2;
                        }
                        if (itemHovered = ImGui.isItemHovered()) {
                            windowDrawList.addRectFilled(
                                    cursorScreenPosX,
                                    cursorScreenPosY,
                                    cursorScreenPosX + g2,
                                    cursorScreenPosY + h2,
                                    EfnWidgets.c(318767103),
                                    EfnWidgets.s(8.0F));
                        }
                        f4 = h3 - px11 - EfnWidgets.s(10.0F);
                    } else {
                        if (windowHovered
                                && ImGui.isMouseHoveringRect(
                                        cursorScreenPosX,
                                        cursorScreenPosY,
                                        cursorScreenPosX + g2,
                                        cursorScreenPosY + h2)) {
                            windowDrawList.addRectFilled(
                                    cursorScreenPosX,
                                    cursorScreenPosY,
                                    cursorScreenPosX + g2,
                                    cursorScreenPosY + h2,
                                    EfnWidgets.c(318767103),
                                    EfnWidgets.s(8.0F));
                        }
                        float g4 = h3 - px11 - EfnWidgets.s(6.0F);
                        f4 =
                                this.colorControls(
                                        windowDrawList, setting, row2, g4 - EfnWidgets.s(4.0F), g3);
                        ImGui.setCursorScreenPos(g4, cursorScreenPosY);
                        if (ImGui.invisibleButton("##minus", cursorScreenPosX + g2 - g4, h2)) {
                            row = row2;
                        }
                        itemHovered = ImGui.isItemHovered();
                        ImGui.setCursorScreenPos(cursorScreenPosX, cursorScreenPosY);
                        if (ImGui.invisibleButton(
                                "##row",
                                Math.max(1.0F, f4 + EfnWidgets.s(4.0F) - cursorScreenPosX),
                                h2)) {
                            row = row2;
                        }
                        itemHovered |= ImGui.isItemHovered();
                    }
                    ImGui.popID();
                    float h4 = cursorScreenPosX + EfnWidgets.s(6.0F);
                    BlockIcons.draw(windowDrawList, row2.info.block, h4, g3 - f3 * 0.5F, f3);
                    float f5 = h4 + f3 + EfnWidgets.s(12.0F);
                    BlockListEditor.Fit fit =
                            fit(row2.pickerFit, row2.info, imFont, imFont2, f4 - f5);
                    EfnWidgets.text(
                            windowDrawList,
                            imFont,
                            f5,
                            g3 - lineHeight * 0.5F,
                            itemHovered ? -461582 : -2042416,
                            fit.text);
                    if (fit.hint != null) {
                        EfnWidgets.text(
                                windowDrawList,
                                imFont2,
                                f5 + fit.textW + EfnWidgets.s(6.0F),
                                g3 - lineHeight2 * 0.5F,
                                -4542808,
                                fit.hint);
                    }
                    int m = !itemHovered ? -13226454 : (bl ? accent : -10856974);
                    int n = itemHovered ? -1 : -4542808;
                    windowDrawList.addCircleFilled(h3, g3, px11, EfnWidgets.c(m), 24);
                    windowDrawList.addLine(
                            h3 - px11 * 0.45F,
                            g3,
                            h3 + px11 * 0.45F,
                            g3,
                            EfnWidgets.c(n),
                            EfnWidgets.s(1.6F));
                    if (bl) {
                        windowDrawList.addLine(
                                h3,
                                g3 - px11 * 0.45F,
                                h3,
                                g3 + px11 * 0.45F,
                                EfnWidgets.c(n),
                                EfnWidgets.s(1.6F));
                    }
                }
            }
            ClickGuiRenderer.smoothScroll(floats, frameDelta());
        }
        ImGui.endChild();
        return row;
    }

    private void openColor(
            BlockColorListSetting colorList2, BlockListEditor.Row row, boolean colorTracer2) {
        this.closeColor();
        this.colorList = colorList2;
        this.colorEntry = row == null ? null : row.entry;
        this.colorTracer = colorTracer2;
        String string =
                row == null
                        ? "Default Color"
                        : (colorTracer2 ? row.info.name + " Tracer" : row.info.name);
        this.colorTitle = "Block Color - " + string + "###block_color";
        this.colorOpen.set(true);
        if (ImGui.isMousePosValid()) {
            this.colorAtX = ImGui.getIO().getMousePosX();
            this.colorAtY = ImGui.getIO().getMousePosY();
        } else {
            this.colorAtX = ImGui.getIO().getDisplaySizeX() * 0.5F;
            this.colorAtY = ImGui.getIO().getDisplaySizeY() * 0.5F;
        }
        this.colorFocus = true;
    }

    private void renderColorWindow() {
        float[] floats =
                this.colorEntry == null
                        ? this.colorList.defaultColor()
                        : (this.colorTracer
                                ? this.colorEntry.tracerColor()
                                : this.colorEntry.color());
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        boolean bl = this.colorAtX + 12.0F + 290.0F > displaySizeX;
        boolean bl2 = this.colorAtY + 12.0F + 360.0F > displaySizeY;
        ImGui.setNextWindowPos(
                this.colorAtX + (bl ? -12.0F : 12.0F),
                this.colorAtY + (bl2 ? -12.0F : 12.0F),
                8,
                bl ? 1.0F : 0.0F,
                bl2 ? 1.0F : 0.0F);
        ImGui.setNextWindowSize(270.0F, 330.0F, 4);
        if (this.colorFocus) {
            ImGui.setNextWindowFocus();
            this.colorFocus = false;
        }
        short s = 352;
        if (ImGui.begin(this.colorTitle, this.colorOpen, 352)) {
            int i = 135593984;
            if (ImGui.colorPicker4("##bcpicker", floats, i)) {
                this.colorList.notifyChanged();
            }
            ImGui.setNextItemWidth(240.0F);
            if (ImGui.colorEdit4("RGBA##bcce", floats, i)) {
                this.colorList.notifyChanged();
            }
            if (ImGui.button("Close##bccp", 80.0F, 22.0F)) {
                this.colorOpen.set(false);
            }
        }
        ImGui.end();
        if (!this.colorOpen.get()) {
            this.closeColor();
        }
    }

    private boolean editing(
            BlockColorListSetting setting,
            BlockColorListSetting.BlockEntry blockEntry,
            boolean bl) {
        return this.colorList == setting && this.colorEntry == blockEntry && this.colorTracer == bl;
    }

    private void closeColor() {
        if (this.colorList != null) {
            this.colorList.notifyChanged();
            this.colorList = null;
            this.colorEntry = null;
            this.colorOpen.set(false);
        }
    }

    private static void select(BlockColorListSetting setting, BlockListEditor.Row row) {
        setting.select(row.entry);
        if (row.follower != null) {
            setting.select(row.follower.entry);
        }
    }

    private static void deselect(BlockColorListSetting setting, BlockListEditor.Row row) {
        setting.deselect(row.entry);
        if (row.follower != null) {
            setting.deselect(row.follower.entry);
        }
    }

    private void addAllShown(BlockColorListSetting setting, BlockListEditor.ListState listState) {
        int availableCount2 = listState.availableCount;
        if (availableCount2 != 0 && (availableCount2 <= 64 || this.confirm(3, setting))) {
            ArrayList list = new ArrayList(availableCount2 + 8);
            for (int j = 0; j < availableCount2; j++) {
                BlockListEditor.Row row = listState.available[j];
                list.add(row.entry);
                if (row.follower != null) {
                    list.add(row.follower.entry);
                }
            }
            setting.selectAll(list);
        }
    }

    private void clearAll(BlockColorListSetting setting) {
        if (this.confirm(2, setting)) {
            setting.deselectAll();
        }
    }

    private static void beginFrame() {
        refreshNames();
        int frameCount = ImGuiManager.getInstance().frameCount();
        if (frameCount > iconsFrame + 1) {
            BlockIcons.forget();
        }
        iconsFrame = frameCount;
        BlockIcons.validate();
    }

    private static void refreshNames() {
        String language2 = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        if (!language2.equals(language)) {
            language = language2;
            if (INFOS.isEmpty()) {
                for (Block block2 : Registries.BLOCK) {
                    INFOS.put(block2, new BlockListEditor.BlockInfo(block2));
                }
            }
            Object2IntOpenHashMap object2IntOpenHashMap = new Object2IntOpenHashMap();
            for (BlockListEditor.BlockInfo blockInfo : INFOS.values()) {
                blockInfo.name = blockInfo.block.getName().getString();
                blockInfo.nameLower = blockInfo.name.toLowerCase(Locale.ROOT);
                if (!blockInfo.wallBanner) {
                    object2IntOpenHashMap.addTo(blockInfo.name, 1);
                }
            }
            for (BlockListEditor.BlockInfo blockInfo2 : INFOS.values()) {
                blockInfo2.hint =
                        !blockInfo2.wallBanner && object2IntOpenHashMap.getInt(blockInfo2.name) <= 1
                                ? null
                                : blockInfo2.path;
            }
            namesGeneration++;
        }
    }

    private static BlockListEditor.ListState list(BlockColorListSetting setting) {
        BlockListEditor.ListState listState = LISTS.get(setting);
        if (listState == null) {
            listState = new BlockListEditor.ListState(setting);
            LISTS.put(setting, listState);
        }
        return listState;
    }

    private static void refreshChosen(
            BlockColorListSetting setting, BlockListEditor.ListState listState) {
        if (listState.chosenRevision != setting.revision()) {
            listState.chosenRevision = setting.revision();
            List list = setting.selected();
            if (listState.chosen.length < list.size()) {
                listState.chosen = new BlockListEditor.Row[list.size()];
            }
            int i = 0;
            for (int j = 0; j < list.size(); j++) {
                BlockListEditor.Row row = listState.byEntry.get(list.get(j));
                if (row != null && (row.leader == null || !row.leader.entry.enabled().get())) {
                    listState.chosen[i++] = row;
                }
            }
            listState.chosenCount = i;
            if (listState.labelsFor != i) {
                listState.labelsFor = i;
                listState.countPill = Integer.toString(i);
            }
        }
    }

    private static void refreshSearch(BlockListEditor.ListState listState, String string) {
        if (string != listState.searchKey || listState.searchGeneration != namesGeneration) {
            listState.searchKey = string;
            listState.searchGeneration = namesGeneration;
            BlockListEditor.Row[] pickable2 = listState.pickable;
            String string2 = string.trim().toLowerCase(Locale.ROOT);
            if (string2.isEmpty()) {
                int i = 0;
                while (i < pickable2.length) {
                    listState.results[i] = i++;
                }
                listState.resultCount = pickable2.length;
            } else {
                String[] strings = string2.split(" ");
                long[] sortKeys2 = listState.sortKeys;
                int j = 0;
                for (int k = 0; k < pickable2.length; k++) {
                    int matchDistance = matchDistance(pickable2[k].info, string2, strings);
                    if (matchDistance >= 0) {
                        sortKeys2[j++] = (long) matchDistance << 32 | k;
                    }
                }
                Arrays.sort(sortKeys2, 0, j);
                for (int n = 0; n < j; n++) {
                    listState.results[n] = (int) sortKeys2[n];
                }
                listState.resultCount = j;
            }
        }
    }

    private static void refreshColumns(
            BlockColorListSetting setting, BlockListEditor.ListState listState) {
        if (listState.columnsRevision != setting.revision()
                || listState.columnsKey != listState.searchKey
                || listState.columnsGeneration != namesGeneration) {
            listState.columnsRevision = setting.revision();
            listState.columnsKey = listState.searchKey;
            listState.columnsGeneration = namesGeneration;
            int i = 0;
            for (int j = 0; j < listState.resultCount; j++) {
                BlockListEditor.Row row = listState.pickable[listState.results[j]];
                if (!row.entry.enabled().get()) {
                    listState.available[i++] = row;
                }
            }
            listState.availableCount = i;
            if (listState.shown.length < listState.chosen.length) {
                listState.shown = new BlockListEditor.Row[listState.chosen.length];
                listState.shownKeys = new long[listState.chosen.length];
            }
            String string;
            if ((string = listState.searchKey.trim().toLowerCase(Locale.ROOT)).isEmpty()) {
                System.arraycopy(listState.chosen, 0, listState.shown, 0, listState.chosenCount);
                listState.shownCount = listState.chosenCount;
            } else {
                String[] strings = string.split(" ");
                long[] shownKeys2 = listState.shownKeys;
                int k = 0;
                for (int m = 0; m < listState.chosenCount; m++) {
                    int matchDistance = matchDistance(listState.chosen[m].info, string, strings);
                    if (matchDistance >= 0) {
                        shownKeys2[k++] = (long) matchDistance << 32 | m;
                    }
                }
                Arrays.sort(shownKeys2, 0, k);
                for (int o = 0; o < k; o++) {
                    listState.shown[o] = listState.chosen[(int) shownKeys2[o]];
                }
                listState.shownCount = k;
            }
        }
    }

    private static int matchDistance(
            BlockListEditor.BlockInfo blockInfo, String string, String[] strings) {
        int i =
                Math.max(
                        wordsFound(blockInfo.nameLower, strings),
                        wordsFound(blockInfo.idLower, strings));
        int levenshtein = levenshtein(string, blockInfo.nameLower);
        int levenshtein2 = levenshtein(string, blockInfo.idLower);
        float f =
                Math.max(
                        1.0F - (float) levenshtein / blockInfo.nameLower.length(),
                        1.0F - (float) levenshtein2 / blockInfo.idLower.length());
        return i <= 0 && !(f >= 0.5F) ? -1 : Math.min(levenshtein, levenshtein2);
    }

    private static int wordsFound(String string, String[] strings) {
        int i = 0;
        for (String string2 : strings) {
            if (!string.contains(string2)) {
                return 0;
            }
            if (!string2.isEmpty()) {
                for (int j = string.indexOf(string2);
                        j >= 0;
                        j = string.indexOf(string2, j + string2.length())) {
                    i++;
                }
            }
        }
        return i;
    }

    private static int levenshtein(String string, String string2) {
        int i = string.length();
        int j = string2.length();
        if (i == 0) {
            return j * 1;
        } else if (j == 0) {
            return i * 8;
        } else {
            if (levPrev.length <= j) {
                levPrev = new int[j + 1];
                levCur = new int[j + 1];
            }
            int[] ints = levPrev;
            int[] ints2 = levCur;
            for (int k = 0; k <= j; k++) {
                ints[k] = k * 1;
            }
            for (int m = 1; m <= i; m++) {
                ints2[0] = m * 8;
                char c = string.charAt(m - 1);
                for (int n = 1; n <= j; n++) {
                    int o = ints[n - 1] + (c == string2.charAt(n - 1) ? 0 : 8);
                    ints2[n] = Math.min(Math.min(ints[n] + 8, ints2[n - 1] + 1), o);
                }
                int[] ints3 = ints;
                ints = ints2;
                ints2 = ints3;
            }
            return ints[j];
        }
    }

    private static BlockListEditor.Fit fit(
            BlockListEditor.Fit fit,
            BlockListEditor.BlockInfo blockInfo,
            ImFont imFont,
            ImFont imFont2,
            float f) {
        if (fit.width == f && fit.generation == namesGeneration) {
            return fit;
        } else {
            fit.width = f;
            fit.generation = namesGeneration;
            fit.text = EfnWidgets.ellipsize(imFont, blockInfo.name, f);
            fit.textW = EfnWidgets.textWidth(imFont, fit.text);
            float g = f - fit.textW - EfnWidgets.s(6.0F);
            fit.hint =
                    blockInfo.hint != null && g >= EfnWidgets.s(40.0F)
                            ? EfnWidgets.ellipsize(imFont2, blockInfo.hint, g)
                            : null;
            return fit;
        }
    }

    private static void countPill(
            ImDrawList imDrawList, ImFont imFont, float f, float g, String string) {
        float h = EfnWidgets.textWidth(imFont, string) + EfnWidgets.s(14.0F);
        float f2 = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(6.0F);
        float g2 = g - f2 * 0.5F;
        imDrawList.addRectFilled(f, g2, f + h, g2 + f2, EfnWidgets.c(-13226454), f2 * 0.5F);
        EfnWidgets.text(
                imDrawList,
                imFont,
                f + EfnWidgets.s(7.0F),
                g2 + EfnWidgets.s(3.0F),
                -4542808,
                string);
    }

    private static void shadow(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2) {
        for (int k = 1; k <= 6; k++) {
            float h2 = EfnWidgets.s(2.5F) * k;
            imDrawList.addRectFilled(
                    f - h2,
                    g - h2 + EfnWidgets.s(4.0F),
                    f + h + h2,
                    g + f2 + h2 + EfnWidgets.s(4.0F),
                    EfnWidgets.c(335544320),
                    g2 + h2);
        }
    }

    private static boolean swatch(
            ImDrawList imDrawList,
            String string,
            float[] floats,
            float f,
            float g,
            float h,
            float f2) {
        ImGui.setCursorScreenPos(f, g);
        boolean invisibleButton = ImGui.invisibleButton(string, h, f2);
        boolean itemHovered = ImGui.isItemHovered();
        float px5 = EfnWidgets.s(5.0F);
        imDrawList.addRectFilled(f, g, f + h, g + f2, EfnWidgets.c(-13226454), px5);
        imDrawList.addRectFilled(f, g, f + h, g + f2, EfnWidgets.c(abgr(floats)), px5);
        imDrawList.addRect(
                f,
                g,
                f + h,
                g + f2,
                EfnWidgets.c(itemHovered ? 1728053247 : 654311423),
                px5,
                0,
                1.0F);
        return invisibleButton;
    }

    private static boolean checkbox(
            ImDrawList imDrawList, String string, boolean bl, float f, float g, float h) {
        ImGui.setCursorScreenPos(f, g);
        boolean invisibleButton = ImGui.invisibleButton(string, h, h);
        int i = bl ? -12276993 : (ImGui.isItemHovered() ? 1090519039 : 452984831);
        float px4 = EfnWidgets.s(4.0F);
        imDrawList.addRectFilled(f, g, f + h, g + h, EfnWidgets.c(-13226454), px4);
        imDrawList.addRect(
                f + 0.5F, g + 0.5F, f + h - 0.5F, g + h - 0.5F, EfnWidgets.c(i), px4, 0, 1.0F);
        if (bl) {
            EfnWidgets.check(imDrawList, f + h * 0.5F, g + h * 0.5F, h * 0.28F, -12276993);
        }
        return invisibleButton;
    }

    private static int abgr(float[] floats) {
        return unit(floats[3]) << 24
                | unit(floats[2]) << 16
                | unit(floats[1]) << 8
                | unit(floats[0]);
    }

    private static int unit(float f) {
        return Math.round(Math.max(0.0F, Math.min(1.0F, f)) * 255.0F);
    }

    private static float frameDelta() {
        return ClickGuiRenderer.animationDelta(Math.max(ImGui.getIO().getDeltaTime(), 0.001F));
    }

    @Environment(EnvType.CLIENT)
    private static final class BlockInfo {

        final Block block;

        final int rawId;

        final String id;

        final String idLower;

        final String path;

        final boolean wallBanner;

        final String wallBannerId;

        String name;

        String nameLower;

        String hint;

        BlockInfo(Block block2) {
            this.block = block2;
            this.rawId = Registries.BLOCK.getRawId(block2);
            Identifier id2 = Registries.BLOCK.getId(block2);
            this.id = id2.toString();
            this.idLower = this.id.toLowerCase(Locale.ROOT);
            this.path = id2.getPath();
            this.wallBanner = this.path.endsWith("_wall_banner");
            this.wallBannerId =
                    !this.wallBanner && this.path.endsWith("_banner")
                            ? id2.getNamespace()
                                    + ":"
                                    + this.path.substring(0, this.path.length() - "banner".length())
                                    + "wall_banner"
                            : null;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Fit {

        String text;

        float textW;

        String hint;

        float width = -1.0F;

        int generation = -1;

        private Fit() {}
    }

    @Environment(EnvType.CLIENT)
    private static final class ListState {

        final BlockListEditor.Row[] pickable;

        final Reference2ObjectOpenHashMap<BlockColorListSetting.BlockEntry, BlockListEditor.Row>
                byEntry = new Reference2ObjectOpenHashMap<>();

        BlockListEditor.Row[] chosen = new BlockListEditor.Row[0];

        int chosenCount;

        long chosenRevision = -1L;

        int labelsFor = -1;

        String countPill;

        final int[] results;

        final long[] sortKeys;

        int resultCount;

        String searchKey;

        int searchGeneration = -1;

        final BlockListEditor.Row[] available;

        int availableCount;

        BlockListEditor.Row[] shown = new BlockListEditor.Row[0];

        long[] shownKeys = new long[0];

        int shownCount;

        long columnsRevision = -1L;

        String columnsKey;

        int columnsGeneration = -1;

        int availableLabelFor = -1;

        int shownLabelFor = -1;

        String availableLabel;

        String shownLabel;

        ListState(BlockColorListSetting setting) {
            List list = setting.entries();
            ArrayList list2 = new ArrayList(list.size());
            for (BlockColorListSetting.BlockEntry blockEntry :
                    (Iterable<BlockColorListSetting.BlockEntry>) list) {
                Identifier id2 = Identifier.tryParse(blockEntry.id());
                Block block2 =
                        id2 == null ? null : Registries.BLOCK.getOptionalValue(id2).orElse(null);
                BlockListEditor.BlockInfo blockInfo =
                        block2 == null ? null : BlockListEditor.INFOS.get(block2);
                if (blockInfo != null) {
                    BlockListEditor.Row row = new BlockListEditor.Row(blockEntry, blockInfo);
                    list2.add(row);
                    this.byEntry.put(blockEntry, row);
                }
            }
            ArrayList list3 = new ArrayList(list2.size());
            for (BlockListEditor.Row row2 : (Iterable<BlockListEditor.Row>) list2) {
                if (row2.info.wallBannerId != null) {
                    BlockColorListSetting.BlockEntry blockEntry2 =
                            setting.entry(row2.info.wallBannerId);
                    BlockListEditor.Row row3 =
                            row2.follower =
                                    blockEntry2 == null ? null : this.byEntry.get(blockEntry2);
                    if (row2.follower != null) {
                        row2.follower.leader = row2;
                    }
                }
                if (!row2.info.wallBanner) {
                    list3.add(row2);
                }
            }
            this.pickable = ((BlockListEditor.Row[]) list3.toArray(new BlockListEditor.Row[0]));
            this.results = new int[this.pickable.length];
            this.sortKeys = new long[this.pickable.length];
            this.available = new BlockListEditor.Row[this.pickable.length];
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Row {

        final BlockColorListSetting.BlockEntry entry;

        final BlockListEditor.BlockInfo info;

        BlockListEditor.Row follower;

        BlockListEditor.Row leader;

        final BlockListEditor.Fit inlineFit = new BlockListEditor.Fit();

        final BlockListEditor.Fit pickerFit = new BlockListEditor.Fit();

        Row(BlockColorListSetting.BlockEntry entry2, BlockListEditor.BlockInfo info2) {
            this.entry = entry2;
            this.info = info2;
        }
    }
}
