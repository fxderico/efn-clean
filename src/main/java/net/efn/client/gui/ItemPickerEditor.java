package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImString;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.StringListSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.ItemEnchants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
final class ItemPickerEditor {

    private static final int INLINE_MAX_ROWS = 8;

    private static final float HEADER_H = 40.0F;

    private static final float INLINE_ROW_H = 38.0F;

    private static final float LIST_PAD = 4.0F;

    private static final float EMPTY_H = 44.0F;

    private static final float LIST_W = 820.0F;

    private static final float SINGLE_W = 560.0F;

    private static final float ENCHANT_W = 560.0F;

    private static final float PICKER_MAX_H = 760.0F;

    private static final float ENCHANT_MAX_H = 640.0F;

    private static final float PICKER_ROW_H = 42.0F;

    private static final float COLUMN_GAP = 28.0F;

    private static final float TAB_BAR_ROOM = 86.0F;

    private static final float ICON_SLOT = 32.0F;

    private static final float PRESET_ICON = 48.0F;

    private static final float PRESET_GAP = 8.0F;

    private static final int LIST_BG = EfnWidgets.withAlpha(-13226454, 0.5F);

    private static final int ROW_HOVER = 318767103;

    private static final int SCRIM = 1929379840;

    private static final String SELECT_LABEL = "Select";

    private static final String EDIT_LABEL = "Edit";

    private static final String NO_ITEMS = "No items yet";

    private static final String NO_ITEM = "No item selected";

    private static final String ANY_ENCHANTS = "Any enchantments";

    private static final String LIST_TITLE = "Select Items";

    private static final String SINGLE_TITLE = "Select Item";

    private static final String ENCHANT_TITLE = "Enchantments";

    private static final String AVAILABLE_TITLE = "All items";

    private static final String CHOSEN_TITLE = "Selected";

    private static final String NO_MATCHES = "No items match";

    private static final String NONE_CHOSEN = "Nothing selected yet";

    private static final String SEARCH_HINT = "Search items...";

    private static final String CLEAR_LABEL = "Clear";

    private static final String DONE_LABEL = "Done";

    private static final String TEXT_HINT = "text match";

    private static final String UNKNOWN_HINT = "unknown item";

    private static final String ENCHANT_HELP =
            "Pick what the item must have. Clashing enchantments turn off on their own.";

    private static final String NO_WORLD = "Join a world to load enchantments.";

    private static final String NO_ENCHANTS = "This item takes no enchantments.";

    private static final String FULL_LIST = "The list is full";

    private static final String OTHER_ITEMS = "Other items";

    private static final int PICKER_FLAGS = 8639;

    private static final int LEV_INSERT = 1;

    private static final int LEV_SUBSTITUTE = 8;

    private static final int LEV_DELETE = 8;

    private static final List<ItemPickerEditor.ItemInfo> ITEMS = new ArrayList<>();

    private static final Reference2ObjectOpenHashMap<Item, ItemPickerEditor.ItemInfo> BY_ITEM =
            new Reference2ObjectOpenHashMap<>();

    private static final Map<StringSetting.Preset, ItemStack> PRESET_ICONS = new HashMap<>();

    private static String language;

    private static int namesGeneration;

    private static int[] levPrev = new int[64];

    private static int[] levCur = new int[64];

    private final Reference2ObjectOpenHashMap<StringListSetting, float[]> inlineScroll =
            new Reference2ObjectOpenHashMap<>();

    private final Reference2ObjectOpenHashMap<StringSetting, ItemPickerEditor.Summary> summaries =
            new Reference2ObjectOpenHashMap<>();

    private ClientModule pickerModule;

    private StringListSetting listTarget;

    private StringSetting itemTarget;

    private String pickerSubtitle;

    private final ImString search = new ImString(64);

    private boolean focusSearch;

    private boolean searchActive;

    private boolean pickerJustOpened;

    private long pickerOpenedAt;

    private final float[] availableScroll = new float[2];

    private final float[] chosenScroll = new float[2];

    private ItemPickerEditor.ItemInfo[] available = new ItemPickerEditor.ItemInfo[0];

    private ItemPickerEditor.ItemInfo[] chosen = new ItemPickerEditor.ItemInfo[0];

    private long[] sortKeys = new long[0];

    private int availableCount;

    private int chosenCount;

    private Object columnsTarget;

    private long columnsRevision = -1L;

    private String columnsKey;

    private int columnsGeneration = -1;

    private StringSetting enchantTarget;

    private Item enchantItem;

    private String enchantSubtitle;

    private final List<ItemPickerEditor.EnchantRow> enchantRows = new ArrayList<>();

    private boolean enchantsLoaded;

    private long enchantOpenedAt;

    private boolean enchantJustOpened;

    private final float[] enchantScroll = new float[2];

    ItemPickerEditor() {}

    float renderList(ClientModule module, StringListSetting setting, float f, float g, float h) {
        beginFrame();
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        ImFont imFont = ImGuiManager.getInstance().moduleFont();
        ImFont imFont2 = ImGuiManager.getInstance().smallFont();
        float windowPosX = ImGui.getWindowPosX();
        float f2 = ImGui.getWindowPosY() - ImGui.getScrollY();
        List setting2 = setting.get();
        ImGui.setCursorPos(f, h + EfnWidgets.s(10.0F));
        ImGui.textUnformatted(setting.name());
        float g2 = f2 + h + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F;
        countPill(
                windowDrawList,
                imFont2,
                ImGui.getItemRectMaxX() + EfnWidgets.s(8.0F),
                g2,
                Integer.toString(setting2.size()));
        if (headerChip("##pick_items", "Select", windowPosX + f + g, g2, imFont)) {
            this.openList(module, setting);
        }
        float h2 = h + EfnWidgets.s(40.0F);
        float px38 = EfnWidgets.s(38.0F);
        int i = setting2.size();
        float f3 = i == 0 ? EfnWidgets.s(44.0F) : Math.min(i, 8) * px38 + EfnWidgets.s(4.0F) * 2.0F;
        float g3 = windowPosX + f;
        float h3 = f2 + h2;
        windowDrawList.addRectFilled(
                g3, h3, g3 + g, h3 + f3, EfnWidgets.c(LIST_BG), EfnWidgets.s(10.0F));
        String string = null;
        if (i == 0) {
            EfnWidgets.text(
                    windowDrawList,
                    imFont,
                    g3 + EfnWidgets.s(14.0F),
                    h3 + (f3 - EfnWidgets.lineHeight(imFont)) * 0.5F,
                    -4542808,
                    "No items yet");
        } else if (i <= 8) {
            string =
                    this.drawEntries(
                            windowDrawList,
                            setting2,
                            f,
                            h2 + EfnWidgets.s(4.0F),
                            g,
                            px38,
                            0,
                            i,
                            imFont,
                            imFont2);
        } else {
            ImGui.setCursorPos(f, h2 + EfnWidgets.s(4.0F));
            if (ImGui.beginChild("##chosen_items", g, f3 - EfnWidgets.s(4.0F) * 2.0F, false, 24)) {
                ImGui.setCursorPos(0.0F, 0.0F);
                ImGui.dummy(1.0F, i * px38);
                float scrollY = ImGui.getScrollY();
                int j = Math.max(0, (int) (scrollY / px38));
                int k = Math.min(i, (int) Math.ceil((scrollY + ImGui.getWindowHeight()) / px38));
                string =
                        this.drawEntries(
                                ImGui.getWindowDrawList(),
                                setting2,
                                0.0F,
                                0.0F,
                                g,
                                px38,
                                j,
                                k,
                                imFont,
                                imFont2);
                ClickGuiRenderer.smoothScroll(
                        this.inlineScroll.computeIfAbsent(setting, value -> new float[2]),
                        frameDelta());
            }
            ImGui.endChild();
        }
        if (string != null) {
            setting.removeValue(string);
        }
        return EfnWidgets.s(40.0F) + f3 + EfnWidgets.s(12.0F);
    }

    float renderItem(ClientModule module, StringSetting setting, float f, float g, float h) {
        beginFrame();
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        ImFont imFont = ImGuiManager.getInstance().moduleFont();
        ImFont imFont2 = ImGuiManager.getInstance().smallFont();
        float windowPosX = ImGui.getWindowPosX();
        float f2 = ImGui.getWindowPosY() - ImGui.getScrollY();
        ImGui.setCursorPos(f, h + EfnWidgets.s(10.0F));
        ImGui.textUnformatted(setting.name());
        float g2 = f2 + h + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F;
        boolean headerChip = headerChip("##pick_item", "Select", windowPosX + f + g, g2, imFont);
        float h2 = h + EfnWidgets.s(40.0F);
        float f3 = EfnWidgets.s(38.0F) + EfnWidgets.s(4.0F) * 2.0F;
        float g3 = windowPosX + f;
        float h3 = f2 + h2;
        float f4 = h3 + f3 * 0.5F;
        float lineHeight = EfnWidgets.lineHeight(imFont);
        float lineHeight2 = EfnWidgets.lineHeight(imFont2);
        String string = setting.get().trim();
        float px22 = EfnWidgets.s(22.0F);
        float g4 = g3 + g - EfnWidgets.s(10.0F);
        boolean bl = false;
        boolean bl2 = false;
        if (!string.isEmpty()) {
            ImGui.setCursorScreenPos(g4 - px22, f4 - px22 * 0.5F);
            bl = ImGui.invisibleButton("##clear_item", px22, px22);
            bl2 = ImGui.isItemHovered();
        }
        float h4 = string.isEmpty() ? g : Math.max(1.0F, g - px22 - EfnWidgets.s(14.0F));
        ImGui.setCursorScreenPos(g3, h3);
        if (ImGui.invisibleButton("##item_box", h4, f3)) {
            headerChip = true;
        }
        windowDrawList.addRectFilled(
                g3,
                h3,
                g3 + g,
                h3 + f3,
                EfnWidgets.c(
                        ImGui.isItemHovered() ? EfnWidgets.withAlpha(-13226454, 0.8F) : LIST_BG),
                EfnWidgets.s(10.0F));
        if (string.isEmpty()) {
            EfnWidgets.text(
                    windowDrawList,
                    imFont,
                    g3 + EfnWidgets.s(14.0F),
                    f4 - lineHeight * 0.5F,
                    -4542808,
                    "No item selected");
        } else {
            if (bl2) {
                windowDrawList.addCircleFilled(
                        g4 - px22 * 0.5F, f4, px22 * 0.5F, EfnWidgets.c(-12240588), 20);
            }
            EfnWidgets.cross(
                    windowDrawList,
                    g4 - px22 * 0.5F,
                    f4,
                    EfnWidgets.s(3.5F),
                    bl2 ? -10856974 : -4542808);
            float f5 = g4 - px22 - EfnWidgets.s(8.0F);
            Item item2 = ItemEnchants.resolve(string);
            float g5 = g3 + EfnWidgets.s(8.0F);
            if (item2 != null) {
                ItemPickerEditor.ItemInfo itemInfo = info(item2);
                float pixels = ItemIcons.pixels(EfnWidgets.s(32.0F));
                ItemIcons.draw(windowDrawList, item2, g5, f4 - pixels * 0.5F, pixels, 1.0F);
                float h5 = g5 + pixels + EfnWidgets.s(10.0F);
                drawLabel(
                        windowDrawList,
                        imFont,
                        imFont2,
                        h5,
                        f4,
                        f5 - h5,
                        itemInfo.name,
                        itemInfo.stored,
                        -461582,
                        lineHeight,
                        lineHeight2);
            } else {
                drawLabel(
                        windowDrawList,
                        imFont,
                        imFont2,
                        g5 + EfnWidgets.s(6.0F),
                        f4,
                        f5 - g5 - EfnWidgets.s(6.0F),
                        string,
                        "unknown item",
                        -10856974,
                        lineHeight,
                        lineHeight2);
            }
        }
        if (bl) {
            setting.set("");
            if (setting.enchantSetting() != null) {
                setting.enchantSetting().set("");
            }
        } else if (headerChip) {
            this.openItem(module, setting);
        }
        return EfnWidgets.s(40.0F) + f3 + EfnWidgets.s(12.0F);
    }

    float renderPresets(ClientModule module, StringSetting setting, float f, float g, float h) {
        beginFrame();
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        ImFont imFont = ImGuiManager.getInstance().moduleFont();
        ImFont imFont2 = ImGuiManager.getInstance().smallFont();
        float windowPosX = ImGui.getWindowPosX();
        float f2 = ImGui.getWindowPosY() - ImGui.getScrollY();
        ImGui.setCursorPos(f, h + EfnWidgets.s(10.0F));
        ImGui.textUnformatted(setting.name());
        List list = setting.presets();
        String string = setting.get().trim();
        String string2 = string.toLowerCase(Locale.ROOT).replace(' ', '_');
        StringSetting.Preset preset = null;
        for (StringSetting.Preset preset2 : (Iterable<StringSetting.Preset>) list) {
            if (preset2.value().equalsIgnoreCase(string2)) {
                preset = preset2;
            }
        }
        int accent = GuiPalette.accent();
        float px8 = EfnWidgets.s(8.0F);
        float lineHeight = EfnWidgets.lineHeight(imFont);
        float lineHeight2 = EfnWidgets.lineHeight(imFont2);
        float pixels = ItemIcons.pixels(EfnWidgets.s(48.0F));
        float g2 = (g - px8 * (list.size() - 1)) / list.size();
        float h2 = pixels + EfnWidgets.s(30.0F) + lineHeight2;
        float f3 = f2 + h + EfnWidgets.s(40.0F);
        for (int j = 0; j < list.size(); j++) {
            StringSetting.Preset preset3 = (StringSetting.Preset) list.get(j);
            boolean bl = preset3 == preset;
            float g3 = windowPosX + f + j * (g2 + px8);
            ImGui.setCursorScreenPos(g3, f3);
            ImGui.pushID(j);
            boolean invisibleButton = ImGui.invisibleButton("##preset", g2, h2);
            boolean itemHovered = ImGui.isItemHovered();
            ImGui.popID();
            presetTile(windowDrawList, g3, f3, g2, h2, bl, itemHovered, accent);
            ItemIcons.draw(
                    windowDrawList,
                    presetIcon(preset3),
                    g3 + (g2 - pixels) * 0.5F,
                    f3 + EfnWidgets.s(12.0F),
                    pixels,
                    1.0F);
            String ellipsize =
                    EfnWidgets.ellipsize(imFont2, preset3.label(), g2 - EfnWidgets.s(10.0F));
            float textWidth = EfnWidgets.textWidth(imFont2, ellipsize);
            EfnWidgets.text(
                    windowDrawList,
                    imFont2,
                    g3 + (g2 - textWidth) * 0.5F,
                    f3 + h2 - EfnWidgets.s(12.0F) - lineHeight2,
                    !bl && !itemHovered ? -2042416 : -461582,
                    ellipsize);
            if (itemHovered && !ellipsize.equals(preset3.label())) {
                ClickGuiRenderer.showTooltip(preset3.label());
            }
            if (invisibleButton) {
                setting.set(preset3.value());
            }
        }
        boolean bl2 = preset == null && !string.isEmpty();
        Item item2 = bl2 ? ItemEnchants.resolve(string) : null;
        float h3 = f3 + h2 + px8;
        float f4 = EfnWidgets.s(38.0F) + EfnWidgets.s(4.0F) * 2.0F;
        float g4 = windowPosX + f;
        float h4 = h3 + f4 * 0.5F;
        ImGui.setCursorScreenPos(g4, h3);
        boolean invisibleButton2 = ImGui.invisibleButton("##other_items", g, f4);
        boolean itemHovered2 = ImGui.isItemHovered();
        presetTile(windowDrawList, g4, h3, g, f4, bl2, itemHovered2, accent);
        float pixels2 = ItemIcons.pixels(EfnWidgets.s(32.0F));
        float f5 = g4 + EfnWidgets.s(10.0F);
        float g5 = f5 + pixels2 + EfnWidgets.s(10.0F);
        float h5 = g4 + g - EfnWidgets.s(14.0F) - g5;
        if (item2 != null) {
            ItemIcons.draw(windowDrawList, item2, f5, h4 - pixels2 * 0.5F, pixels2, 1.0F);
            drawLabel(
                    windowDrawList,
                    imFont,
                    imFont2,
                    g5,
                    h4,
                    h5,
                    info(item2).name,
                    "Other items",
                    -461582,
                    lineHeight,
                    lineHeight2);
        } else {
            float f6 = f5 + pixels2 * 0.5F;
            float g6 = pixels2 * 0.34F;
            int i = EfnWidgets.c(itemHovered2 ? -461582 : -4542808);
            windowDrawList.addCircle(f6, h4, g6, i, 24, EfnWidgets.s(1.5F));
            windowDrawList.addLine(f6 - g6 * 0.5F, h4, f6 + g6 * 0.5F, h4, i, EfnWidgets.s(1.5F));
            windowDrawList.addLine(f6, h4 - g6 * 0.5F, f6, h4 + g6 * 0.5F, i, EfnWidgets.s(1.5F));
            drawLabel(
                    windowDrawList,
                    imFont,
                    imFont2,
                    g5,
                    h4,
                    h5,
                    bl2 ? string : "Other items",
                    bl2 ? "Other items" : null,
                    !itemHovered2 && !bl2 ? -2042416 : -461582,
                    lineHeight,
                    lineHeight2);
        }
        if (invisibleButton2) {
            this.openItem(module, setting);
        }
        return EfnWidgets.s(40.0F) + h2 + px8 + f4 + EfnWidgets.s(12.0F);
    }

    private static void presetTile(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            boolean bl,
            boolean bl2,
            int i) {
        int j =
                bl
                        ? EfnWidgets.withAlpha(i, 0.18F)
                        : (bl2 ? EfnWidgets.withAlpha(-13226454, 0.8F) : LIST_BG);
        imDrawList.addRectFilled(f, g, f + h, g + f2, EfnWidgets.c(j), EfnWidgets.s(10.0F));
        if (bl) {
            imDrawList.addRect(
                    f,
                    g,
                    f + h,
                    g + f2,
                    EfnWidgets.c(EfnWidgets.withAlpha(i, 0.9F)),
                    EfnWidgets.s(10.0F),
                    0,
                    EfnWidgets.s(1.5F));
        }
    }

    private static ItemStack presetIcon(StringSetting.Preset preset) {
        ItemStack stack = PRESET_ICONS.get(preset);
        if (stack != null) {
            return stack;
        } else {
            Item item2 = ItemEnchants.resolve(preset.icon());
            stack = new ItemStack(item2 == null ? Items.BARRIER : item2);
            if (preset.glint()) {
                stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
            PRESET_ICONS.put(preset, stack);
            return stack;
        }
    }

    float renderEnchants(ClientModule module, StringSetting setting, float f, float g, float h) {
        beginFrame();
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        ImFont imFont = ImGuiManager.getInstance().moduleFont();
        float windowPosX = ImGui.getWindowPosX();
        float f2 = ImGui.getWindowPosY() - ImGui.getScrollY();
        ImGui.setCursorPos(f, h + EfnWidgets.s(10.0F));
        ImGui.textUnformatted(setting.name());
        float g2 = f2 + h + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F;
        boolean headerChip = headerChip("##edit_enchants", "Edit", windowPosX + f + g, g2, imFont);
        float h2 = h + EfnWidgets.s(40.0F);
        float f3 = EfnWidgets.s(38.0F) + EfnWidgets.s(4.0F) * 2.0F;
        float g3 = windowPosX + f;
        float h3 = f2 + h2;
        ImGui.setCursorScreenPos(g3, h3);
        if (ImGui.invisibleButton("##enchant_box", g, f3)) {
            headerChip = true;
        }
        boolean itemHovered;
        windowDrawList.addRectFilled(
                g3,
                h3,
                g3 + g,
                h3 + f3,
                EfnWidgets.c(
                        (itemHovered = ImGui.isItemHovered())
                                ? EfnWidgets.withAlpha(-13226454, 0.8F)
                                : LIST_BG),
                EfnWidgets.s(10.0F));
        String summary = this.summary(setting);
        float lineHeight = EfnWidgets.lineHeight(imFont);
        float f4 = g3 + EfnWidgets.s(14.0F);
        float g4 = g - EfnWidgets.s(28.0F);
        if (summary.isEmpty()) {
            EfnWidgets.text(
                    windowDrawList,
                    imFont,
                    f4,
                    h3 + (f3 - lineHeight) * 0.5F,
                    -4542808,
                    "Any enchantments");
        } else {
            String ellipsize = EfnWidgets.ellipsize(imFont, summary, g4);
            EfnWidgets.text(
                    windowDrawList, imFont, f4, h3 + (f3 - lineHeight) * 0.5F, -461582, ellipsize);
            if (itemHovered && !ellipsize.equals(summary)) {
                ClickGuiRenderer.showTooltip(summary);
            }
        }
        if (headerChip) {
            this.openEnchants(module, setting);
        }
        return EfnWidgets.s(40.0F) + f3 + EfnWidgets.s(12.0F);
    }

    void renderWindows() {
        if (this.listTarget != null || this.itemTarget != null) {
            this.renderPicker();
        } else if (this.enchantTarget != null) {
            this.renderEnchantWindow();
        }
    }

    void close() {
        this.closePicker();
        this.closeEnchants();
    }

    boolean closeWindow() {
        if (this.listTarget != null || this.itemTarget != null) {
            this.closePicker();
            return true;
        } else if (this.enchantTarget != null) {
            this.closeEnchants();
            return true;
        } else {
            return false;
        }
    }

    private String drawEntries(
            ImDrawList imDrawList,
            List<String> list,
            float f,
            float g,
            float h,
            float f2,
            int i,
            int j,
            ImFont imFont,
            ImFont imFont2) {
        boolean windowHovered = ImGui.isWindowHovered();
        float pixels = ItemIcons.pixels(EfnWidgets.s(32.0F));
        float px22 = EfnWidgets.s(22.0F);
        float lineHeight = EfnWidgets.lineHeight(imFont);
        float lineHeight2 = EfnWidgets.lineHeight(imFont2);
        String string = null;
        for (int m = i; m < j; m++) {
            String string2 = (String) list.get(m);
            ImGui.setCursorPos(f, g + m * f2);
            float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float cursorScreenPosY = ImGui.getCursorScreenPosY();
            float g2 = cursorScreenPosY + f2 * 0.5F;
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
            ImGui.pushID(m);
            float h2 = cursorScreenPosX + h - EfnWidgets.s(10.0F);
            ImGui.setCursorScreenPos(h2 - px22, g2 - px22 * 0.5F);
            if (ImGui.invisibleButton("##remove", px22, px22)) {
                string = string2;
            }
            boolean itemHovered;
            if (itemHovered = ImGui.isItemHovered()) {
                imDrawList.addCircleFilled(
                        h2 - px22 * 0.5F, g2, px22 * 0.5F, EfnWidgets.c(-12240588), 20);
            }
            EfnWidgets.cross(
                    imDrawList,
                    h2 - px22 * 0.5F,
                    g2,
                    EfnWidgets.s(3.5F),
                    itemHovered ? -10856974 : -4542808);
            ImGui.popID();
            h2 -= px22 + EfnWidgets.s(8.0F);
            float f3 = cursorScreenPosX + EfnWidgets.s(8.0F);
            Item item2 = ItemEnchants.resolve(string2);
            if (item2 != null) {
                ItemPickerEditor.ItemInfo itemInfo = info(item2);
                ItemIcons.draw(imDrawList, item2, f3, g2 - pixels * 0.5F, pixels, 1.0F);
                float g3 = f3 + pixels + EfnWidgets.s(10.0F);
                drawLabel(
                        imDrawList,
                        imFont,
                        imFont2,
                        g3,
                        g2,
                        h2 - g3,
                        itemInfo.name,
                        itemInfo.stored,
                        -461582,
                        lineHeight,
                        lineHeight2);
            } else {
                float h3 = f3 + EfnWidgets.s(6.0F);
                drawLabel(
                        imDrawList,
                        imFont,
                        imFont2,
                        h3,
                        g2,
                        h2 - h3,
                        string2,
                        "text match",
                        -2042416,
                        lineHeight,
                        lineHeight2);
            }
        }
        return string;
    }

    private static void drawLabel(
            ImDrawList imDrawList,
            ImFont imFont,
            ImFont imFont2,
            float f,
            float g,
            float h,
            String string,
            String string2,
            int i,
            float f2,
            float g2) {
        if (!(h <= 0.0F)) {
            String ellipsize = EfnWidgets.ellipsize(imFont, string, h);
            EfnWidgets.text(imDrawList, imFont, f, g - f2 * 0.5F, i, ellipsize);
            float textWidth = EfnWidgets.textWidth(imFont, ellipsize);
            float h2 = h - textWidth - EfnWidgets.s(6.0F);
            if (string2 != null && h2 >= EfnWidgets.s(40.0F)) {
                EfnWidgets.text(
                        imDrawList,
                        imFont2,
                        f + textWidth + EfnWidgets.s(6.0F),
                        g - g2 * 0.5F,
                        -4542808,
                        EfnWidgets.ellipsize(imFont2, string2, h2));
            }
        }
    }

    private void openList(ClientModule module, StringListSetting listTarget2) {
        this.closeEnchants();
        this.listTarget = listTarget2;
        this.itemTarget = null;
        this.startPicker(module, listTarget2.name());
    }

    private void openItem(ClientModule module, StringSetting itemTarget2) {
        this.closeEnchants();
        this.itemTarget = itemTarget2;
        this.listTarget = null;
        this.startPicker(module, itemTarget2.name());
    }

    private void startPicker(ClientModule pickerModule2, String string) {
        this.pickerModule = pickerModule2;
        this.pickerSubtitle = pickerModule2.name() + " / " + string;
        this.search.set("");
        this.focusSearch = true;
        this.searchActive = false;
        this.pickerOpenedAt = System.nanoTime();
        this.pickerJustOpened = true;
        this.chosenScroll[1] = 0.0F;
        this.chosenScroll[0] = 0.0F;
        this.availableScroll[1] = 0.0F;
        this.availableScroll[0] = 0.0F;
        this.columnsTarget = null;
    }

    private void closePicker() {
        this.listTarget = null;
        this.itemTarget = null;
        this.columnsTarget = null;
    }

    private void renderPicker() {
        beginFrame();
        boolean bl = this.listTarget != null;
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float px16 = EfnWidgets.s(16.0F);
        float f = Math.min(displaySizeX - px16 * 2.0F, EfnWidgets.s(bl ? 820.0F : 560.0F));
        float g =
                Math.min(
                        displaySizeY - EfnWidgets.s(86.0F) - EfnWidgets.s(32.0F),
                        EfnWidgets.s(760.0F));
        float easeOut =
                ClickGuiRenderer.easeOut(ClickGuiRenderer.elapsed(this.pickerOpenedAt, 200.0F));
        float h = Math.round((displaySizeX - f) * 0.5F);
        float f2 =
                Math.round(
                        Math.max(
                                        px16,
                                        (displaySizeY - EfnWidgets.s(86.0F) - g) * 0.5F
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
        ItemPickerEditor.ItemInfo itemInfo = null;
        ItemPickerEditor.ItemInfo itemInfo2 = null;
        boolean begin = ImGui.begin("##item_picker", 8639);
        if (this.pickerJustOpened) {
            imgui.internal.ImGui.bringWindowToDisplayFront(imgui.internal.ImGui.getCurrentWindow());
        }
        if (begin) {
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            windowDrawList.addRectFilled(
                    0.0F, 0.0F, displaySizeX, displaySizeY, EfnWidgets.c(1929379840));
            shadow(windowDrawList, h, f2, f, g, cardRounding);
            windowDrawList.addRectFilled(
                    h,
                    f2,
                    h + f,
                    f2 + g,
                    EfnWidgets.c(GuiPalette.abgr(ThemeStore.get().cards.color.argbWithAlpha(255))),
                    cardRounding);
            windowDrawList.addRect(
                    h, f2, h + f, f2 + g, EfnWidgets.c(318767103), cardRounding, 0, 1.0F);
            ImFont imFont = ImGuiManager.getInstance().moduleFont();
            ImFont imFont2 = ImGuiManager.getInstance().smallFont();
            float px24 = EfnWidgets.s(24.0F);
            float g2 = f2 + EfnWidgets.s(22.0F);
            float px28 = EfnWidgets.s(28.0F);
            ClickGuiRenderer.drawCardTitle(
                    bl ? "Select Items" : "Select Item", h + px24, g2 + px28 * 0.5F);
            ImGui.setCursorPos(h + f - px24 - px28, g2);
            if (EfnWidgets.iconButton("##close", px28)) {
                bl2 = true;
            }
            EfnWidgets.cross(
                    windowDrawList,
                    h + f - px24 - px28 * 0.5F,
                    g2 + px28 * 0.5F,
                    EfnWidgets.s(4.5F),
                    -461582);
            if (bl
                    && chip(
                            "##clear_items",
                            "Clear",
                            this.listTarget.get().isEmpty(),
                            h + f - px24 - px28 - EfnWidgets.s(10.0F),
                            g2 + px28 * 0.5F,
                            imFont2,
                            "Takes every item off the list")) {
                this.listTarget.clear();
            }
            float h2 = g2 + px28 + EfnWidgets.s(2.0F);
            EfnWidgets.text(windowDrawList, imFont2, h + px24, h2, -4542808, this.pickerSubtitle);
            ImGui.setCursorPos(h + px24, h2 + EfnWidgets.lineHeight(imFont2) + EfnWidgets.s(14.0F));
            ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
            ImGui.setNextItemWidth(f - px24 * 2.0F);
            if (this.focusSearch) {
                ImGui.setKeyboardFocusHere();
                this.focusSearch = false;
            }
            boolean bl3 =
                    this.searchActive
                            && (ImGui.isKeyPressed(525, false) || ImGui.isKeyPressed(627, false));
            ImGui.inputTextWithHint("##item_search", "Search items...", this.search);
            this.searchActive = ImGui.isItemActive();
            ImGui.popStyleVar();
            float f3 = ImGui.getItemRectMaxY() + EfnWidgets.s(18.0F);
            String columnsKey2 = this.columnsKey;
            this.refreshColumns();
            if (columnsKey2 != null && !columnsKey2.equals(this.columnsKey)) {
                this.chosenScroll[1] = 0.0F;
                this.chosenScroll[0] = 0.0F;
                this.availableScroll[1] = 0.0F;
                this.availableScroll[0] = 0.0F;
            }
            if (bl3 && !this.search.get().isBlank() && this.availableCount > 0) {
                itemInfo = this.available[0];
                this.focusSearch = bl;
            }
            float px282 = EfnWidgets.s(28.0F);
            float g3 = f3 + px282 + EfnWidgets.s(10.0F);
            float h3 = f2 + g - EfnWidgets.s(18.0F);
            float f4 = Math.max(1.0F, h3 - g3);
            String string = this.search.get().isBlank() ? "Nothing selected yet" : "No items match";
            if (bl) {
                float g4 = (f - px24 * 2.0F - EfnWidgets.s(28.0F)) * 0.5F;
                float h4 = h + px24;
                float f5 = h4 + g4 + EfnWidgets.s(28.0F);
                columnHeader(
                        windowDrawList,
                        imFont,
                        imFont2,
                        h4,
                        f3,
                        px282,
                        g4,
                        "All items",
                        this.listTarget.canAdd()
                                ? Integer.toString(this.availableCount)
                                : "The list is full");
                columnHeader(
                        windowDrawList,
                        imFont,
                        imFont2,
                        f5,
                        f3,
                        px282,
                        g4,
                        "Selected",
                        Integer.toString(this.chosenCount));
                float g5 = Math.round(h4 + g4 + EfnWidgets.s(28.0F) * 0.5F);
                windowDrawList.addLine(g5, f3, g5, h3, EfnWidgets.c(268435455), 1.0F);
                ItemPickerEditor.ItemInfo itemInfo3 =
                        this.column(
                                "##available",
                                this.available,
                                this.availableCount,
                                true,
                                null,
                                h4,
                                g3,
                                g4,
                                f4,
                                this.availableScroll,
                                "No items match",
                                imFont,
                                imFont2);
                ItemPickerEditor.ItemInfo itemInfo4 =
                        this.column(
                                "##chosen",
                                this.chosen,
                                this.chosenCount,
                                false,
                                null,
                                f5,
                                g3,
                                g4,
                                f4,
                                this.chosenScroll,
                                string,
                                imFont,
                                imFont2);
                if (itemInfo3 != null) {
                    itemInfo = itemInfo3;
                }
                if (itemInfo4 != null) {
                    itemInfo2 = itemInfo4;
                }
            } else {
                float h5 = f - px24 * 2.0F;
                columnHeader(
                        windowDrawList,
                        imFont,
                        imFont2,
                        h + px24,
                        f3,
                        px282,
                        h5,
                        "All items",
                        Integer.toString(this.availableCount));
                Item item2 = ItemEnchants.resolve(this.itemTarget.get());
                ItemPickerEditor.ItemInfo itemInfo5 =
                        this.column(
                                "##single",
                                this.available,
                                this.availableCount,
                                true,
                                item2,
                                h + px24,
                                g3,
                                h5,
                                f4,
                                this.availableScroll,
                                "No items match",
                                imFont,
                                imFont2);
                if (itemInfo5 != null) {
                    itemInfo = itemInfo5;
                }
            }
            if (!this.pickerJustOpened
                    && ImGui.isMouseClicked(0)
                    && ImGui.isWindowHovered()
                    && !ImGui.isMouseHoveringRect(h, f2, h + f, f2 + g)) {
                bl2 = true;
            }
        }
        ImGui.end();
        ImGui.popStyleVar(4);
        this.pickerJustOpened = false;
        if (itemInfo2 != null && this.listTarget != null) {
            removeFromList(this.listTarget, itemInfo2.item);
        }
        if (itemInfo != null) {
            if (this.listTarget != null) {
                this.listTarget.addValue(itemInfo.stored);
            } else if (this.itemTarget != null) {
                this.pickItem(itemInfo);
                return;
            }
        }
        if (bl2) {
            this.closePicker();
        }
    }

    private void pickItem(ItemPickerEditor.ItemInfo itemInfo) {
        StringSetting itemTarget2 = this.itemTarget;
        ClientModule pickerModule2 = this.pickerModule;
        Item item2 = ItemEnchants.resolve(itemTarget2.get());
        itemTarget2.set(itemInfo.stored);
        this.closePicker();
        StringSetting setting = itemTarget2.enchantSetting();
        if (setting != null) {
            if (item2 != itemInfo.item) {
                setting.set("");
            }
            if (pickerModule2 != null && ItemEnchants.supports(itemInfo.item)) {
                this.openEnchants(pickerModule2, setting);
            }
        }
    }

    private static void removeFromList(StringListSetting setting, Item item2) {
        for (String string : setting.get()) {
            if (ItemEnchants.resolve(string) == item2) {
                setting.removeValue(string);
                return;
            }
        }
    }

    private ItemPickerEditor.ItemInfo column(
            String string,
            ItemPickerEditor.ItemInfo[] itemInfos,
            int i,
            boolean bl,
            Item item2,
            float f,
            float g,
            float h,
            float f2,
            float[] floats,
            String string2,
            ImFont imFont,
            ImFont imFont2) {
        ItemPickerEditor.ItemInfo itemInfo = null;
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
                float pixels = ItemIcons.pixels(EfnWidgets.s(32.0F));
                float px11 = EfnWidgets.s(11.0F);
                float scrollY = ImGui.getScrollY();
                int j = Math.max(0, (int) (scrollY / px42));
                int k = Math.min(i, (int) Math.ceil((scrollY + ImGui.getWindowHeight()) / px42));
                int accent = GuiPalette.accent();
                for (int o = j; o < k; o++) {
                    ItemPickerEditor.ItemInfo itemInfo2 = itemInfos[o];
                    ImGui.setCursorPos(0.0F, o * px42);
                    float cursorScreenPosX = ImGui.getCursorScreenPosX();
                    float cursorScreenPosY = ImGui.getCursorScreenPosY();
                    float f3 = cursorScreenPosY + h2 * 0.5F;
                    ImGui.pushID(itemInfo2.rawId);
                    if (ImGui.invisibleButton("##row", g2, h2)) {
                        itemInfo = itemInfo2;
                    }
                    boolean itemHovered = ImGui.isItemHovered();
                    ImGui.popID();
                    boolean bl2 = item2 != null && itemInfo2.item == item2;
                    if (bl2) {
                        windowDrawList.addRectFilled(
                                cursorScreenPosX,
                                cursorScreenPosY,
                                cursorScreenPosX + g2,
                                cursorScreenPosY + h2,
                                EfnWidgets.c(EfnWidgets.withAlpha(accent, 0.18F)),
                                EfnWidgets.s(8.0F));
                    } else if (itemHovered) {
                        windowDrawList.addRectFilled(
                                cursorScreenPosX,
                                cursorScreenPosY,
                                cursorScreenPosX + g2,
                                cursorScreenPosY + h2,
                                EfnWidgets.c(318767103),
                                EfnWidgets.s(8.0F));
                    }
                    float g3 = cursorScreenPosX + EfnWidgets.s(6.0F);
                    ItemIcons.draw(
                            windowDrawList, itemInfo2.item, g3, f3 - pixels * 0.5F, pixels, 1.0F);
                    float h3 = cursorScreenPosX + g2 - EfnWidgets.s(8.0F) - px11;
                    float f4 = g3 + pixels + EfnWidgets.s(12.0F);
                    drawLabel(
                            windowDrawList,
                            imFont,
                            imFont2,
                            f4,
                            f3,
                            h3 - px11 - EfnWidgets.s(10.0F) - f4,
                            itemInfo2.name,
                            itemInfo2.stored,
                            !itemHovered && !bl2 ? -2042416 : -461582,
                            lineHeight,
                            lineHeight2);
                    if (bl2) {
                        windowDrawList.addCircleFilled(h3, f3, px11, EfnWidgets.c(accent), 24);
                        EfnWidgets.check(windowDrawList, h3, f3, px11 * 0.5F, -1);
                    } else {
                        int m = !itemHovered ? -13226454 : (bl ? accent : -10856974);
                        int n = itemHovered ? -1 : -4542808;
                        windowDrawList.addCircleFilled(h3, f3, px11, EfnWidgets.c(m), 24);
                        windowDrawList.addLine(
                                h3 - px11 * 0.45F,
                                f3,
                                h3 + px11 * 0.45F,
                                f3,
                                EfnWidgets.c(n),
                                EfnWidgets.s(1.6F));
                        if (bl) {
                            windowDrawList.addLine(
                                    h3,
                                    f3 - px11 * 0.45F,
                                    h3,
                                    f3 + px11 * 0.45F,
                                    EfnWidgets.c(n),
                                    EfnWidgets.s(1.6F));
                        }
                    }
                }
            }
            ClickGuiRenderer.smoothScroll(floats, frameDelta());
        }
        ImGui.endChild();
        return itemInfo;
    }

    private void refreshColumns() {
        Object object = this.listTarget != null ? this.listTarget : this.itemTarget;
        long l = this.listTarget != null ? this.listTarget.revision() : this.itemTarget.revision();
        String search2 = this.search.get();
        if (object != this.columnsTarget
                || l != this.columnsRevision
                || !search2.equals(this.columnsKey)
                || this.columnsGeneration != namesGeneration) {
            this.columnsTarget = object;
            this.columnsRevision = l;
            this.columnsKey = search2;
            this.columnsGeneration = namesGeneration;
            ReferenceOpenHashSet referenceOpenHashSet = new ReferenceOpenHashSet();
            if (this.listTarget != null) {
                for (String string : this.listTarget.get()) {
                    Item item2 = ItemEnchants.resolve(string);
                    if (item2 != null) {
                        referenceOpenHashSet.add(item2);
                    }
                }
            }
            String string2 = search2.trim().toLowerCase(Locale.ROOT);
            String[] strings = string2.split(" ");
            int i = ITEMS.size();
            if (this.available.length < i) {
                this.available = new ItemPickerEditor.ItemInfo[i];
                this.chosen = new ItemPickerEditor.ItemInfo[i];
                this.sortKeys = new long[i];
            }
            int j = 0;
            for (int k = 0; k < i; k++) {
                ItemPickerEditor.ItemInfo itemInfo = ITEMS.get(k);
                if (!referenceOpenHashSet.contains(itemInfo.item)) {
                    if (string2.isEmpty()) {
                        this.sortKeys[j++] = k;
                    } else {
                        int matchDistance = matchDistance(itemInfo, string2, strings);
                        if (matchDistance >= 0) {
                            this.sortKeys[j++] = (long) matchDistance << 32 | k;
                        }
                    }
                }
            }
            Arrays.sort(this.sortKeys, 0, j);
            for (int n = 0; n < j; n++) {
                this.available[n] = ITEMS.get((int) this.sortKeys[n]);
            }
            this.availableCount = j;
            int m = 0;
            if (this.listTarget != null) {
                for (String string3 : this.listTarget.get()) {
                    Item item3 = ItemEnchants.resolve(string3);
                    if (item3 != null) {
                        ItemPickerEditor.ItemInfo itemInfo2 = info(item3);
                        if (string2.isEmpty() || matchDistance(itemInfo2, string2, strings) >= 0) {
                            this.chosen[m++] = itemInfo2;
                        }
                    }
                }
            }
            this.chosenCount = m;
        }
    }

    private void openEnchants(ClientModule module, StringSetting enchantTarget2) {
        this.closePicker();
        this.enchantTarget = enchantTarget2;
        this.enchantItem =
                enchantTarget2.enchantItem() == null
                        ? null
                        : ItemEnchants.resolve(enchantTarget2.enchantItem().get());
        String string = this.enchantItem == null ? "No item" : info(this.enchantItem).name;
        this.enchantSubtitle = module.name() + " / " + string;
        this.loadEnchantRows();
        this.enchantOpenedAt = System.nanoTime();
        this.enchantJustOpened = true;
        this.enchantScroll[1] = 0.0F;
        this.enchantScroll[0] = 0.0F;
    }

    private void closeEnchants() {
        this.enchantTarget = null;
        this.enchantItem = null;
        this.enchantRows.clear();
    }

    private void loadEnchantRows() {
        this.enchantRows.clear();
        boolean bl = this.enchantsLoaded = MinecraftClient.getInstance().world != null;
        if (this.enchantsLoaded && this.enchantItem != null) {
            Map map = ItemEnchants.parse(this.enchantTarget.get());
            for (RegistryEntry entry : ItemEnchants.applicable(this.enchantItem)) {
                ItemPickerEditor.EnchantRow enchantRow = new ItemPickerEditor.EnchantRow(entry);
                Integer integer = (Integer) map.get(enchantRow.key);
                if (integer == null) {
                    integer = (Integer) map.get(ItemEnchants.key(enchantRow.name));
                }
                if (integer != null) {
                    enchantRow.on = true;
                    enchantRow.level = Math.max(1, Math.min(enchantRow.max, integer));
                }
                this.enchantRows.add(enchantRow);
            }
        }
    }

    private void writeEnchants() {
        ArrayList list = new ArrayList();
        for (ItemPickerEditor.EnchantRow enchantRow : this.enchantRows) {
            if (enchantRow.on) {
                list.add(
                        enchantRow.max > 1
                                ? enchantRow.path + " " + enchantRow.level
                                : enchantRow.path);
            }
        }
        this.enchantTarget.set(String.join(", ", list));
    }

    private void toggle(ItemPickerEditor.EnchantRow enchantRow) {
        boolean bl = enchantRow.on = !enchantRow.on;
        if (enchantRow.on) {
            for (ItemPickerEditor.EnchantRow enchantRow2 : this.enchantRows) {
                if (enchantRow2 != enchantRow
                        && enchantRow2.on
                        && !Enchantment.canBeCombined(enchantRow.holder, enchantRow2.holder)) {
                    enchantRow2.on = false;
                }
            }
        }
        this.writeEnchants();
    }

    private void renderEnchantWindow() {
        beginFrame();
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float px16 = EfnWidgets.s(16.0F);
        float f = Math.min(displaySizeX - px16 * 2.0F, EfnWidgets.s(560.0F));
        float g =
                Math.min(
                        displaySizeY - EfnWidgets.s(86.0F) - EfnWidgets.s(32.0F),
                        EfnWidgets.s(640.0F));
        float easeOut =
                ClickGuiRenderer.easeOut(ClickGuiRenderer.elapsed(this.enchantOpenedAt, 200.0F));
        float h = Math.round((displaySizeX - f) * 0.5F);
        float f2 =
                Math.round(
                        Math.max(
                                        px16,
                                        (displaySizeY - EfnWidgets.s(86.0F) - g) * 0.5F
                                                + EfnWidgets.s(8.0F))
                                + EfnWidgets.s(14.0F) * (1.0F - easeOut));
        float cardRounding = ClickGuiRenderer.cardRounding();
        ImGui.setNextWindowPos(0.0F, 0.0F, 1);
        ImGui.setNextWindowSize(displaySizeX, displaySizeY, 1);
        if (this.enchantJustOpened) {
            ImGui.setNextWindowFocus();
        }
        ImGui.pushStyleVar(0, Math.max(0.01F, easeOut));
        ImGui.pushStyleVar(2, 0.0F, 0.0F);
        ImGui.pushStyleVar(14, 0.0F, 0.0F);
        ImGui.pushStyleVar(4, 0.0F);
        boolean bl = false;
        ItemPickerEditor.EnchantRow enchantRow = null;
        boolean begin = ImGui.begin("##enchant_picker", 8639);
        if (this.enchantJustOpened) {
            imgui.internal.ImGui.bringWindowToDisplayFront(imgui.internal.ImGui.getCurrentWindow());
        }
        if (begin) {
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            windowDrawList.addRectFilled(
                    0.0F, 0.0F, displaySizeX, displaySizeY, EfnWidgets.c(1929379840));
            shadow(windowDrawList, h, f2, f, g, cardRounding);
            windowDrawList.addRectFilled(
                    h,
                    f2,
                    h + f,
                    f2 + g,
                    EfnWidgets.c(GuiPalette.abgr(ThemeStore.get().cards.color.argbWithAlpha(255))),
                    cardRounding);
            windowDrawList.addRect(
                    h, f2, h + f, f2 + g, EfnWidgets.c(318767103), cardRounding, 0, 1.0F);
            ImFont imFont = ImGuiManager.getInstance().moduleFont();
            ImFont imFont2 = ImGuiManager.getInstance().smallFont();
            float px24 = EfnWidgets.s(24.0F);
            float g2 = f2 + EfnWidgets.s(22.0F);
            float px28 = EfnWidgets.s(28.0F);
            ClickGuiRenderer.drawCardTitle("Enchantments", h + px24, g2 + px28 * 0.5F);
            ImGui.setCursorPos(h + f - px24 - px28, g2);
            if (EfnWidgets.iconButton("##close", px28)) {
                bl = true;
            }
            EfnWidgets.cross(
                    windowDrawList,
                    h + f - px24 - px28 * 0.5F,
                    g2 + px28 * 0.5F,
                    EfnWidgets.s(4.5F),
                    -461582);
            float h2 = g2 + px28 + EfnWidgets.s(2.0F);
            float lineHeight = EfnWidgets.lineHeight(imFont2);
            float lineHeight2 = EfnWidgets.lineHeight(imFont);
            float f3 = h + px24;
            if (this.enchantItem != null) {
                float pixels = ItemIcons.pixels(EfnWidgets.s(20.0F));
                ItemIcons.draw(
                        windowDrawList,
                        this.enchantItem,
                        f3,
                        h2 + lineHeight * 0.5F - pixels * 0.5F,
                        pixels,
                        1.0F);
                f3 += pixels + EfnWidgets.s(8.0F);
            }
            EfnWidgets.text(windowDrawList, imFont2, f3, h2, -4542808, this.enchantSubtitle);
            float g3 = h2 + lineHeight + EfnWidgets.s(8.0F);
            EfnWidgets.text(
                    windowDrawList,
                    imFont2,
                    h + px24,
                    g3,
                    -4542808,
                    EfnWidgets.ellipsize(
                            imFont2,
                            "Pick what the item must have. Clashing enchantments turn off on their"
                                    + " own.",
                            f - px24 * 2.0F));
            float px32 = EfnWidgets.s(32.0F);
            float h3 = g3 + lineHeight + EfnWidgets.s(14.0F);
            float f4 = f2 + g - EfnWidgets.s(20.0F) - px32 - EfnWidgets.s(12.0F);
            float g4 = Math.max(1.0F, f4 - h3);
            float h4 = f - px24 * 2.0F;
            windowDrawList.addLine(
                    h + px24,
                    h3 - EfnWidgets.s(6.0F),
                    h + px24 + h4,
                    h3 - EfnWidgets.s(6.0F),
                    EfnWidgets.c(268435455),
                    1.0F);
            if (this.enchantsLoaded && !this.enchantRows.isEmpty()) {
                ImGui.setCursorPos(h + px24, h3);
                if (ImGui.beginChild("##enchant_rows", h4, g4, false, 24)) {
                    float px42 = EfnWidgets.s(42.0F);
                    float f5 = h4 - EfnWidgets.s(6.0F);
                    float g5 = px42 - EfnWidgets.s(2.0F);
                    ImGui.setCursorPos(0.0F, 0.0F);
                    ImGui.dummy(1.0F, this.enchantRows.size() * px42 + EfnWidgets.s(6.0F));
                    ImDrawList windowDrawList2 = ImGui.getWindowDrawList();
                    int accent = GuiPalette.accent();
                    float scrollY = ImGui.getScrollY();
                    int i = Math.max(0, (int) (scrollY / px42));
                    int j =
                            Math.min(
                                    this.enchantRows.size(),
                                    (int) Math.ceil((scrollY + ImGui.getWindowHeight()) / px42));
                    for (int m = i; m < j; m++) {
                        ItemPickerEditor.EnchantRow enchantRow2 = this.enchantRows.get(m);
                        ImGui.setCursorPos(0.0F, m * px42);
                        float cursorScreenPosX = ImGui.getCursorScreenPosX();
                        float cursorScreenPosY = ImGui.getCursorScreenPosY();
                        float h5 = cursorScreenPosY + g5 * 0.5F;
                        float f6 = enchantRow2.max > 1 ? EfnWidgets.s(112.0F) : 0.0F;
                        ImGui.pushID(m);
                        if (ImGui.invisibleButton("##toggle", Math.max(1.0F, f5 - f6), g5)) {
                            enchantRow = enchantRow2;
                        }
                        boolean itemHovered = ImGui.isItemHovered();
                        if (enchantRow2.on) {
                            windowDrawList2.addRectFilled(
                                    cursorScreenPosX,
                                    cursorScreenPosY,
                                    cursorScreenPosX + f5,
                                    cursorScreenPosY + g5,
                                    EfnWidgets.c(EfnWidgets.withAlpha(accent, 0.14F)),
                                    EfnWidgets.s(8.0F));
                        } else if (itemHovered) {
                            windowDrawList2.addRectFilled(
                                    cursorScreenPosX,
                                    cursorScreenPosY,
                                    cursorScreenPosX + f5,
                                    cursorScreenPosY + g5,
                                    EfnWidgets.c(318767103),
                                    EfnWidgets.s(8.0F));
                        }
                        float px18 = EfnWidgets.s(18.0F);
                        float g6 = cursorScreenPosX + EfnWidgets.s(10.0F);
                        windowDrawList2.addRectFilled(
                                g6,
                                h5 - px18 * 0.5F,
                                g6 + px18,
                                h5 + px18 * 0.5F,
                                EfnWidgets.c(-13226454),
                                EfnWidgets.s(4.0F));
                        windowDrawList2.addRect(
                                g6 + 0.5F,
                                h5 - px18 * 0.5F + 0.5F,
                                g6 + px18 - 0.5F,
                                h5 + px18 * 0.5F - 0.5F,
                                EfnWidgets.c(
                                        enchantRow2.on
                                                ? accent
                                                : (itemHovered ? 1090519039 : 452984831)),
                                EfnWidgets.s(4.0F),
                                0,
                                1.0F);
                        if (enchantRow2.on) {
                            EfnWidgets.check(
                                    windowDrawList2, g6 + px18 * 0.5F, h5, px18 * 0.28F, accent);
                        }
                        float h6 = g6 + px18 + EfnWidgets.s(12.0F);
                        String string =
                                enchantRow2.max > 1
                                        ? enchantRow2.name
                                                + "  (max "
                                                + ItemEnchants.roman(enchantRow2.max)
                                                + ")"
                                        : enchantRow2.name;
                        EfnWidgets.text(
                                windowDrawList2,
                                imFont,
                                h6,
                                h5 - lineHeight2 * 0.5F,
                                !enchantRow2.on && !itemHovered ? -2042416 : -461582,
                                EfnWidgets.ellipsize(
                                        imFont,
                                        string,
                                        cursorScreenPosX + f5 - f6 - EfnWidgets.s(10.0F) - h6));
                        if (enchantRow2.max > 1) {
                            float px282 = EfnWidgets.s(28.0F);
                            float px26 = EfnWidgets.s(26.0F);
                            float f7 = cursorScreenPosX + f5 - EfnWidgets.s(8.0F);
                            ImGui.setCursorScreenPos(f7 - px282, h5 - px26 * 0.5F);
                            if (EfnWidgets.chipButton("##up", "+", px282, px26, false)
                                    && enchantRow2.level < enchantRow2.max) {
                                enchantRow2.level++;
                                if (!enchantRow2.on) {
                                    enchantRow = enchantRow2;
                                } else {
                                    this.writeEnchants();
                                }
                            }
                            String roman = ItemEnchants.roman(enchantRow2.level);
                            float px40 = EfnWidgets.s(40.0F);
                            float g7 = f7 - px282 - px40;
                            EfnWidgets.text(
                                    windowDrawList2,
                                    imFont,
                                    g7 + (px40 - EfnWidgets.textWidth(imFont, roman)) * 0.5F,
                                    h5 - lineHeight2 * 0.5F,
                                    enchantRow2.on ? -461582 : -4542808,
                                    roman);
                            ImGui.setCursorScreenPos(g7 - px282, h5 - px26 * 0.5F);
                            if (EfnWidgets.chipButton("##down", "-", px282, px26, false)
                                    && enchantRow2.level > 1) {
                                enchantRow2.level--;
                                if (!enchantRow2.on) {
                                    enchantRow = enchantRow2;
                                } else {
                                    this.writeEnchants();
                                }
                            }
                        }
                        ImGui.popID();
                    }
                    ClickGuiRenderer.smoothScroll(this.enchantScroll, frameDelta());
                }
                ImGui.endChild();
            } else {
                EfnWidgets.text(
                        windowDrawList,
                        imFont,
                        h + px24 + EfnWidgets.s(8.0F),
                        h3 + EfnWidgets.s(10.0F),
                        -4542808,
                        this.enchantsLoaded
                                ? "This item takes no enchantments."
                                : "Join a world to load enchantments.");
            }
            float h7 = f2 + g - EfnWidgets.s(20.0F) - px32;
            float f8 = EfnWidgets.textWidth(imFont, "Done") + EfnWidgets.s(36.0F);
            ImGui.setCursorPos(h + f - px24 - f8, h7);
            if (EfnWidgets.chipButton("##done", "Done", f8, px32, true)) {
                bl = true;
            }
            float g8 = EfnWidgets.textWidth(imFont, "Clear") + EfnWidgets.s(36.0F);
            ImGui.setCursorPos(h + px24, h7);
            boolean bl2 = false;
            for (ItemPickerEditor.EnchantRow enchantRow3 : this.enchantRows) {
                bl2 |= enchantRow3.on;
            }
            ImGui.beginDisabled(!bl2);
            if (EfnWidgets.chipButton("##clear_enchants", "Clear", g8, px32, false)) {
                for (ItemPickerEditor.EnchantRow enchantRow4 : this.enchantRows) {
                    enchantRow4.on = false;
                }
                this.writeEnchants();
            }
            ImGui.endDisabled();
            if (!this.enchantJustOpened
                    && ImGui.isMouseClicked(0)
                    && ImGui.isWindowHovered()
                    && !ImGui.isMouseHoveringRect(h, f2, h + f, f2 + g)) {
                bl = true;
            }
        }
        ImGui.end();
        ImGui.popStyleVar(4);
        this.enchantJustOpened = false;
        if (enchantRow != null) {
            this.toggle(enchantRow);
        }
        if (bl) {
            this.closeEnchants();
        }
    }

    private String summary(StringSetting setting) {
        ItemPickerEditor.Summary summary = this.summaries.get(setting);
        if (summary != null
                && summary.revision() == setting.revision()
                && summary.names() == namesGeneration) {
            return summary.text();
        } else {
            Map map = ItemEnchants.parse(setting.get());
            ArrayList list = new ArrayList();
            List list2 = ItemEnchants.all();
            for (Entry entry : (Iterable<Entry>) map.entrySet()) {
                String string = null;
                int i = 0;
                for (RegistryEntry entry2 : (Iterable<RegistryEntry>) list2) {
                    if (ItemEnchants.key(ItemEnchants.path(entry2)).equals(entry.getKey())) {
                        string = ((Enchantment) entry2.value()).description().getString();
                        i = ((Enchantment) entry2.value()).getMaxLevel();
                        break;
                    }
                }
                if (string == null) {
                    string = (String) entry.getKey();
                }
                list.add(
                        i == 1
                                ? string
                                : string + " " + ItemEnchants.roman((Integer) entry.getValue()));
            }
            String string2 = String.join(", ", list);
            this.summaries.put(
                    setting,
                    new ItemPickerEditor.Summary(setting.revision(), namesGeneration, string2));
            return string2;
        }
    }

    private static boolean headerChip(
            String string, String string2, float f, float g, ImFont imFont) {
        float h = EfnWidgets.textWidth(imFont, string2) + EfnWidgets.s(28.0F);
        float px28 = EfnWidgets.s(28.0F);
        ImGui.setCursorScreenPos(f - h, g - px28 * 0.5F);
        return EfnWidgets.chipButton(string, string2, h, px28, false);
    }

    private static boolean chip(
            String string,
            String string2,
            boolean bl,
            float f,
            float g,
            ImFont imFont,
            String string3) {
        boolean pushFont = ClickGuiRenderer.pushFont(imFont);
        float h = EfnWidgets.textWidth(imFont, string2) + EfnWidgets.s(20.0F);
        float f2 = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(12.0F);
        ImGui.setCursorPos(f - h, g - f2 * 0.5F);
        ImGui.beginDisabled(bl);
        boolean chipButton = EfnWidgets.chipButton(string, string2, h, f2, false);
        ImGui.endDisabled();
        if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip(string3);
        }
        if (pushFont) {
            ImGui.popFont();
        }
        return chipButton;
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

    private static void beginFrame() {
        refreshNames();
    }

    private static ItemPickerEditor.ItemInfo info(Item item2) {
        refreshNames();
        ItemPickerEditor.ItemInfo itemInfo = BY_ITEM.get(item2);
        if (itemInfo == null) {
            itemInfo = new ItemPickerEditor.ItemInfo(item2);
            itemInfo.name = item2.getDefaultStack().getName().getString();
            itemInfo.nameLower = itemInfo.name.toLowerCase(Locale.ROOT);
            BY_ITEM.put(item2, itemInfo);
        }
        return itemInfo;
    }

    private static void refreshNames() {
        String language2 = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        if (!language2.equals(language) || ITEMS.isEmpty()) {
            language = language2;
            if (ITEMS.isEmpty()) {
                for (Item item2 : Registries.ITEM) {
                    if (item2 != Items.AIR) {
                        ItemPickerEditor.ItemInfo itemInfo2 = new ItemPickerEditor.ItemInfo(item2);
                        ITEMS.add(itemInfo2);
                        BY_ITEM.put(item2, itemInfo2);
                    }
                }
            }
            for (ItemPickerEditor.ItemInfo itemInfo3 : ITEMS) {
                itemInfo3.name = itemInfo3.item.getDefaultStack().getName().getString();
                itemInfo3.nameLower = itemInfo3.name.toLowerCase(Locale.ROOT);
            }
            ITEMS.sort(
                    Comparator.<ItemPickerEditor.ItemInfo, String>comparing(
                                    itemInfo -> itemInfo.nameLower)
                            .thenComparing(itemInfo -> itemInfo.id));
            namesGeneration++;
        }
    }

    private static int matchDistance(
            ItemPickerEditor.ItemInfo itemInfo, String string, String[] strings) {
        int i =
                Math.max(
                        wordsFound(itemInfo.nameLower, strings),
                        wordsFound(itemInfo.idLower, strings));
        int levenshtein = levenshtein(string, itemInfo.nameLower);
        int levenshtein2 = levenshtein(string, itemInfo.idLower);
        float f =
                Math.max(
                        1.0F - (float) levenshtein / Math.max(1, itemInfo.nameLower.length()),
                        1.0F - (float) levenshtein2 / Math.max(1, itemInfo.idLower.length()));
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

    private static float frameDelta() {
        return ClickGuiRenderer.animationDelta(Math.max(ImGui.getIO().getDeltaTime(), 0.001F));
    }

    @Environment(EnvType.CLIENT)
    private static final class EnchantRow {

        final RegistryEntry<Enchantment> holder;

        final String path;

        final String key;

        final String name;

        final int max;

        int level;

        boolean on;

        EnchantRow(RegistryEntry<Enchantment> holder2) {
            this.holder = holder2;
            this.path = ItemEnchants.path(holder2);
            this.key = ItemEnchants.key(this.path);
            this.name = ((Enchantment) holder2.value()).description().getString();
            this.level = this.max = Math.max(1, ((Enchantment) holder2.value()).getMaxLevel());
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class ItemInfo {

        final Item item;

        final int rawId;

        final String id;

        final String stored;

        final String idLower;

        String name;

        String nameLower;

        ItemInfo(Item item2) {
            this.item = item2;
            this.rawId = Registries.ITEM.getRawId(item2);
            Identifier id2 = Registries.ITEM.getId(item2);
            this.id = id2.toString();
            this.stored = ItemEnchants.stored(item2);
            this.idLower = this.id.toLowerCase(Locale.ROOT);
        }
    }

    @Environment(EnvType.CLIENT)
    private record Summary(long revision, int names, String text) {}
}
