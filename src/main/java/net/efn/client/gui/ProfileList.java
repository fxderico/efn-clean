package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImString;

import net.efn.client.config.ConfigProfiles;
import net.efn.client.config.ProfileException;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@Environment(EnvType.CLIENT)
final class ProfileList {

    private static final float ROW_H = 52.0F;

    private static final float CHIP_W = 58.0F;

    private static final float CHIP_H = 28.0F;

    private static final float GAP = 6.0F;

    private static final float LABEL_W = 96.0F;

    private static final float FIELD_H = 30.0F;

    private static final long ARM_NANOS = 3000000000L;

    private static final int DISCORD_LIMIT = 2000;

    private static final String PRIVATE_NOTE =
            "Webhooks, macros, seeds and other private settings are not included.";

    private final BiConsumer<String, Boolean> messages;

    private final ImString renameInput = new ImString(64);

    private final ImString serverInput = new ImString(256);

    private final Map<String, Float> editorHeights = new HashMap<>();

    private String expanded;

    private String armed;

    private long armedAt;

    private String scrollTo;

    private float scrollTarget = 0.0F / 0.0F;

    private boolean focusServer;

    ProfileList(BiConsumer<String, Boolean> messages2) {
        this.messages = messages2;
    }

    void open(String string) {
        this.expanded = null;
        this.toggle(string);
        this.scrollTo = key(string);
    }

    void disarm() {
        this.armed = null;
    }

    float takeScrollTarget() {
        float scrollTarget2 = this.scrollTarget;
        this.scrollTarget = 0.0F / 0.0F;
        return scrollTarget2;
    }

    void render(
            ConfigProfiles configProfiles,
            List<ConfigProfiles.Profile> list,
            String string,
            float f,
            float g) {
        for (ConfigProfiles.Profile profile : list) {
            this.row(configProfiles, profile, profile.name().equalsIgnoreCase(string), f, g);
        }
    }

    private void row(
            ConfigProfiles configProfiles,
            ConfigProfiles.Profile profile,
            boolean bl,
            float f,
            float g) {
        String string = profile.name();
        String key = key(string);
        boolean bl2 = key.equals(this.expanded);
        ImGui.pushID("profile_" + key);
        float cursorPosY = ImGui.getCursorPosY();
        if (key.equals(this.scrollTo)) {
            this.scrollTarget = cursorPosY - EfnWidgets.s(8.0F);
            this.scrollTo = null;
        }
        float px52 = EfnWidgets.s(52.0F);
        float animate = EfnWidgets.animate(ImGui.getID("##reveal"), bl2, 16.0F);
        float h =
                animate > 0.001F
                        ? this.editorHeights.getOrDefault(key, EfnWidgets.s(240.0F)) * animate
                        : 0.0F;
        ImGui.setCursorPos(f, cursorPosY);
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        ImGui.setNextItemAllowOverlap();
        ImGui.invisibleButton("##row", g, px52);
        if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
            this.load(configProfiles, profile);
        }
        boolean mouseHoveringRect =
                ImGui.isMouseHoveringRect(
                        cursorScreenPosX,
                        cursorScreenPosY,
                        cursorScreenPosX + g,
                        cursorScreenPosY + px52);
        float animate2 = EfnWidgets.animate(ImGui.getItemID(), mouseHoveringRect, 18.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        float f2 = cursorScreenPosX - EfnWidgets.s(8.0F);
        float g2 = cursorScreenPosX + g + EfnWidgets.s(8.0F);
        float px10 = EfnWidgets.s(10.0F);
        int i = animate > 0.001F ? 48 : 240;
        if (animate > 0.001F) {
            windowDrawList.addRectFilled(
                    f2,
                    cursorScreenPosY,
                    g2,
                    cursorScreenPosY + px52 + h,
                    EfnWidgets.c(EfnWidgets.withAlpha(184549375, animate)),
                    px10);
        }
        if (bl) {
            windowDrawList.addRectFilled(
                    f2,
                    cursorScreenPosY,
                    g2,
                    cursorScreenPosY + px52,
                    EfnWidgets.c(EfnWidgets.withAlpha(GuiPalette.accent(), 0.16F)),
                    px10,
                    i);
        }
        if (animate2 > 0.01F) {
            windowDrawList.addRectFilled(
                    f2,
                    cursorScreenPosY,
                    g2,
                    cursorScreenPosY + px52,
                    EfnWidgets.c(EfnWidgets.withAlpha(218103807, animate2)),
                    px10,
                    i);
        }
        ImFont font = ImGui.getFont();
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        float px58 = EfnWidgets.s(58.0F);
        float px28 = EfnWidgets.s(28.0F);
        float px282 = EfnWidgets.s(28.0F);
        float px6 = EfnWidgets.s(6.0F);
        float h2 = px58 * 2.0F + px282 + px6 * 2.0F;
        float f3 = g - h2 - EfnWidgets.s(12.0F);
        float g3 = bl ? EfnWidgets.textWidth(imFont, "Active") + EfnWidgets.s(14.0F) : 0.0F;
        String ellipsize =
                EfnWidgets.ellipsize(font, string, f3 - (bl ? g3 + EfnWidgets.s(8.0F) : 0.0F));
        float lineHeight = EfnWidgets.lineHeight(font);
        float lineHeight2 = EfnWidgets.lineHeight(imFont);
        float h3 = cursorScreenPosY + (px52 - lineHeight - EfnWidgets.s(3.0F) - lineHeight2) * 0.5F;
        EfnWidgets.text(windowDrawList, font, cursorScreenPosX, h3, -461582, ellipsize);
        if (bl) {
            float f4 =
                    cursorScreenPosX + EfnWidgets.textWidth(font, ellipsize) + EfnWidgets.s(8.0F);
            float g4 = lineHeight2 + EfnWidgets.s(6.0F);
            float h4 = h3 + (lineHeight - g4) * 0.5F;
            windowDrawList.addRectFilled(
                    f4, h4, f4 + g3, h4 + g4, EfnWidgets.c(GuiPalette.accent()), g4 * 0.5F);
            EfnWidgets.text(
                    windowDrawList,
                    imFont,
                    f4 + EfnWidgets.s(7.0F),
                    h4 + EfnWidgets.s(3.0F),
                    -1,
                    "Active");
        }
        EfnWidgets.text(
                windowDrawList,
                imFont,
                cursorScreenPosX,
                h3 + lineHeight + EfnWidgets.s(3.0F),
                profile.broken() ? -10856974 : -4542808,
                EfnWidgets.ellipsize(imFont, summary(profile), f3));
        float f5 = f + g - h2;
        float g5 = cursorPosY + (px52 - px28) * 0.5F;
        ImGui.setCursorPos(f5, g5);
        if (EfnWidgets.chipButton("##load", "Load", px58, px28, false)) {
            this.load(configProfiles, profile);
        }
        if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip(
                    "Apply the saved " + partList(profile.parts()) + " of this profile");
        }
        String string2 = "save:" + key;
        boolean armed2 = this.isArmed(string2);
        ImGui.setCursorPos(f5 + px58 + px6, g5);
        if (EfnWidgets.chipButton("##save", armed2 ? "Sure?" : "Save", px58, px28, armed2)
                && this.confirm(string2)) {
            this.save(configProfiles, profile);
        }
        if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip(
                    "Overwrite this profile with your current " + partList(profile.parts()));
        }
        ImGui.setCursorPos(f5 + (px58 + px6) * 2.0F, cursorPosY + (px52 - px282) * 0.5F);
        float h5 = ImGui.getCursorScreenPosX() + px282 * 0.5F;
        float f6 = ImGui.getCursorScreenPosY() + px282 * 0.5F;
        if (ImGui.invisibleButton("##edit", px282, px282)) {
            this.toggle(string);
        }
        boolean itemHovered = ImGui.isItemHovered();
        float g6 = Math.max(EfnWidgets.animate(ImGui.getItemID(), itemHovered, 18.0F), animate);
        if (g6 > 0.01F) {
            windowDrawList.addCircleFilled(
                    h5, f6, px282 * 0.5F, EfnWidgets.c(EfnWidgets.withAlpha(-12240588, g6)), 32);
        }
        EfnWidgets.chevronRotated(
                windowDrawList,
                h5,
                f6,
                EfnWidgets.s(4.0F),
                EfnTheme.lerpColor(-4542808, -461582, g6),
                animate * 1.5707964F);
        if (itemHovered) {
            ClickGuiRenderer.showTooltip(bl2 ? "Close" : "Edit, share or delete");
        }
        float h6 = cursorPosY + px52;
        if (animate > 0.001F) {
            ImGui.pushClipRect(f2, cursorScreenPosY + px52, g2, cursorScreenPosY + px52 + h, true);
            ImGui.pushStyleVar(0, ImGui.getStyle().getAlpha() * animate);
            this.editorHeights.put(key, this.editor(configProfiles, profile, key, f, g, h6));
            ImGui.popStyleVar();
            ImGui.popClipRect();
        }
        ImGui.setCursorPos(0.0F, h6 + h + EfnWidgets.s(4.0F));
        ImGui.popID();
    }

    private float editor(
            ConfigProfiles configProfiles,
            ConfigProfiles.Profile profile,
            String string,
            float f,
            float g,
            float h) {
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        ImFont font = ImGui.getFont();
        float px30 = EfnWidgets.s(30.0F);
        float px6 = EfnWidgets.s(6.0F);
        float f2 = (px30 - ImGui.getTextLineHeight()) * 0.5F;
        float g2 = f + EfnWidgets.s(96.0F);
        float h2 = g - EfnWidgets.s(96.0F);
        float f3 = h + EfnWidgets.s(6.0F);
        label("Name", f, f3, px30);
        float px78 = EfnWidgets.s(78.0F);
        ImGui.setCursorPos(g2, f3);
        ImGui.pushStyleVar(11, EfnWidgets.s(10.0F), f2);
        ImGui.setNextItemWidth(h2 - px78 - px6);
        ImGui.inputTextWithHint("##rename", "Profile name", this.renameInput);
        boolean submittedWithEnter = EfnWidgets.submittedWithEnter();
        ImGui.popStyleVar();
        ImGui.setCursorPos(f + g - px78, f3);
        if (EfnWidgets.chipButton("##rename_go", "Rename", px78, px30, false)) {
            submittedWithEnter = true;
        }
        if (submittedWithEnter) {
            this.rename(configProfiles, profile);
        }
        float g3;
        label("Includes", f, g3 = f3 + (px30 + EfnWidgets.s(10.0F)), px30);
        ConfigProfiles.Part[] parts = ConfigProfiles.Part.values();
        float h3 = (h2 - px6 * (parts.length - 1)) / parts.length;
        for (int i = 0; i < parts.length; i++) {
            ConfigProfiles.Part part = parts[i];
            boolean bl = profile.parts().contains(part);
            ImGui.setCursorPos(g2 + i * (h3 + px6), g3);
            if (EfnWidgets.chipButton("##part_" + part.key, part.label, h3, px30, bl)) {
                this.setPart(configProfiles, profile, part, !bl);
            }
            if (ImGui.isItemHovered(65536)) {
                ClickGuiRenderer.showTooltip(partTip(part, bl));
            }
        }
        label("Load on join", f, f3 = g3 + (px30 + EfnWidgets.s(10.0F)), px30);
        for (String server : profile.servers()) {
            ImGui.pushID("server_" + server);
            ImGui.setCursorPos(g2, f3);
            float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float cursorScreenPosY = ImGui.getCursorScreenPosY();
            windowDrawList.addRectFilled(
                    cursorScreenPosX,
                    cursorScreenPosY,
                    cursorScreenPosX + h2,
                    cursorScreenPosY + px30,
                    EfnWidgets.c(-13226454),
                    EfnWidgets.s(7.0F));
            EfnWidgets.text(
                    windowDrawList,
                    font,
                    cursorScreenPosX + EfnWidgets.s(10.0F),
                    cursorScreenPosY + (px30 - EfnWidgets.lineHeight(font)) * 0.5F,
                    -461582,
                    EfnWidgets.ellipsize(font, server, h2 - px30 - EfnWidgets.s(14.0F)));
            ImGui.setCursorPos(g2 + h2 - px30, f3);
            if (ImGui.invisibleButton("##remove", px30, px30)) {
                this.removeServer(configProfiles, profile, server);
            }
            boolean itemHovered = ImGui.isItemHovered();
            float animate = EfnWidgets.animate(ImGui.getItemID(), itemHovered, 18.0F);
            float f4 = cursorScreenPosX + h2 - px30 * 0.5F;
            float g4 = cursorScreenPosY + px30 * 0.5F;
            if (animate > 0.01F) {
                windowDrawList.addCircleFilled(
                        f4,
                        g4,
                        px30 * 0.38F,
                        EfnWidgets.c(EfnWidgets.withAlpha(-10856974, 0.18F * animate)),
                        24);
            }
            EfnWidgets.cross(
                    windowDrawList,
                    f4,
                    g4,
                    EfnWidgets.s(3.5F),
                    EfnTheme.lerpColor(-4542808, -10856974, animate));
            if (itemHovered) {
                ClickGuiRenderer.showTooltip("Stop loading on " + server);
            }
            ImGui.popID();
            f3 += px30 + px6;
        }
        float px60 = EfnWidgets.s(60.0F);
        ImGui.setCursorPos(g2, f3);
        ImGui.pushStyleVar(11, EfnWidgets.s(10.0F), f2);
        ImGui.setNextItemWidth(h2 - px60 - px6);
        if (this.focusServer) {
            ImGui.setKeyboardFocusHere();
            this.focusServer = false;
        }
        ImGui.inputTextWithHint("##server", "Server address", this.serverInput, 16);
        boolean submittedWithEnter2 = EfnWidgets.submittedWithEnter();
        ImGui.popStyleVar();
        ImGui.setCursorPos(f + g - px60, f3);
        if (EfnWidgets.chipButton("##server_add", "Add", px60, px30, false)) {
            submittedWithEnter2 = true;
        }
        if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip("Loads this profile when you join one of these servers.");
        }
        if (submittedWithEnter2) {
            this.addServer(configProfiles, profile);
        }
        float h4 = (g - px6 * 2.0F) / 3.0F;
        float f5;
        ImGui.setCursorPos(f, f5 = f3 + (px30 + EfnWidgets.s(12.0F)));
        if (EfnWidgets.chipButton("##export", "Export file", h4, px30, false)) {
            this.export(profile);
        }
        if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip(
                    "Save this profile as a .json file.\n"
                        + "Webhooks, macros, seeds and other private settings are not included.");
        }
        ImGui.setCursorPos(f + h4 + px6, f5);
        if (EfnWidgets.chipButton("##code", "Copy code", h4, px30, false)) {
            this.copyCode(configProfiles, profile);
        }
        if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip(
                    "Copy a share code. Others load it with Paste code.\n"
                        + "Webhooks, macros, seeds and other private settings are not included.");
        }
        String string2 = "delete:" + string;
        boolean armed2 = this.isArmed(string2);
        ImGui.setCursorPos(f + (h4 + px6) * 2.0F, f5);
        if (dangerChip("##delete", armed2 ? "Sure?" : "Delete", h4, px30, armed2)
                && this.confirm(string2)) {
            this.delete(configProfiles, profile);
        }
        if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip(armed2 ? "Click again to delete" : "Delete this profile");
        }
        return f5 + (px30 + EfnWidgets.s(12.0F)) - h;
    }

    private void load(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
        try {
            Set set = configProfiles.load(profile.name());
            this.messages.accept("Loaded " + profile.name() + " (" + partList(set) + ")", false);
        } catch (ProfileException exception) {
            this.messages.accept(exception.getMessage(), true);
        }
    }

    private void save(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
        try {
            Set set = configProfiles.save(profile.name());
            this.messages.accept(
                    "Saved your current " + partList(set) + " to " + profile.name(), false);
        } catch (ProfileException exception) {
            this.messages.accept(exception.getMessage(), true);
        }
    }

    private void rename(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
        try {
            String rename = configProfiles.rename(profile.name(), this.renameInput.get());
            if (rename.equals(profile.name())) {
                return;
            }
            this.expanded = key(rename);
            this.renameInput.set(rename);
            this.messages.accept("Renamed " + profile.name() + " to " + rename, false);
        } catch (ProfileException exception) {
            this.messages.accept(exception.getMessage(), true);
        }
    }

    private void setPart(
            ConfigProfiles configProfiles,
            ConfigProfiles.Profile profile,
            ConfigProfiles.Part part,
            boolean bl) {
        try {
            configProfiles.setPart(profile.name(), part, bl);
            this.messages.accept(
                    bl
                            ? "Saved your current " + part.label + " into " + profile.name()
                            : part.label + " is no longer part of " + profile.name(),
                    false);
        } catch (ProfileException exception) {
            this.messages.accept(exception.getMessage(), true);
        }
    }

    private void addServer(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
        try {
            String addServer = configProfiles.addServer(profile.name(), this.serverInput.get());
            this.serverInput.set("");
            this.messages.accept(profile.name() + " now loads when you join " + addServer, false);
        } catch (ProfileException exception) {
            this.messages.accept(exception.getMessage(), true);
        }
        this.focusServer = true;
    }

    private void removeServer(
            ConfigProfiles configProfiles, ConfigProfiles.Profile profile, String string) {
        try {
            configProfiles.removeServer(profile.name(), string);
            this.messages.accept(profile.name() + " no longer loads on " + string, false);
        } catch (ProfileException exception) {
            this.messages.accept(exception.getMessage(), true);
        }
    }

    private void export(ConfigProfiles.Profile profile) {
        if (FileDialogs.busy()) {
            this.messages.accept("A file window is already open", true);
        } else {
            String string = profile.name();
            FileDialogs.saveProfile(
                    "Export " + string,
                    string + ".json",
                    path -> {
                        String string2 = path.getFileName().toString();
                        Path path2 =
                                string2.toLowerCase(Locale.ROOT).endsWith(".json")
                                        ? path
                                        : path.resolveSibling(string2 + ".json");
                        try {
                            ConfigProfiles.getInstance().exportTo(string, path2);
                            this.messages.accept(
                                    "Exported " + string + " to " + path2.getFileName(), false);
                        } catch (ProfileException exception) {
                            this.messages.accept(exception.getMessage(), true);
                        }
                    });
        }
    }

    private void copyCode(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
        try {
            String shareCode = configProfiles.shareCode(profile.name());
            MinecraftClient.getInstance().keyboard.setClipboard(shareCode);
            this.messages.accept(
                    shareCode.length() > 2000
                            ? "Copied, but it is too long for Discord. Send the file instead"
                            : "Share code for " + profile.name() + " copied",
                    false);
        } catch (ProfileException exception) {
            this.messages.accept(exception.getMessage(), true);
        }
    }

    private void delete(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
        try {
            configProfiles.delete(profile.name());
            if (key(profile.name()).equals(this.expanded)) {
                this.expanded = null;
            }
            this.messages.accept("Deleted " + profile.name(), false);
        } catch (ProfileException exception) {
            this.messages.accept(exception.getMessage(), true);
        }
    }

    private void toggle(String string) {
        String key = key(string);
        if (key.equals(this.expanded)) {
            this.expanded = null;
        } else {
            this.expanded = key;
            this.renameInput.set(string);
            this.serverInput.set("");
        }
    }

    private boolean isArmed(String string) {
        return string.equals(this.armed) && System.nanoTime() - this.armedAt < 3000000000L;
    }

    private boolean confirm(String armed2) {
        if (this.isArmed(armed2)) {
            this.armed = null;
            return true;
        } else {
            this.armed = armed2;
            this.armedAt = System.nanoTime();
            return false;
        }
    }

    private static boolean dangerChip(String string, String string2, float f, float g, boolean bl) {
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        boolean invisibleButton = ImGui.invisibleButton(string, f, g);
        float animate = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
        int i = bl ? -10856974 : EfnTheme.lerpColor(-13226454, -12240588, animate);
        if (ImGui.isItemActive()) {
            i = EfnTheme.lerpColor(i, -16777216, 0.12F);
        }
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        windowDrawList.addRectFilled(
                cursorScreenPosX,
                cursorScreenPosY,
                cursorScreenPosX + f,
                cursorScreenPosY + g,
                EfnWidgets.c(i),
                EfnWidgets.s(7.0F));
        ImFont font = ImGui.getFont();
        int j = bl ? -1 : EfnTheme.lerpColor(-461582, -10856974, animate);
        EfnWidgets.text(
                windowDrawList,
                font,
                cursorScreenPosX + (f - EfnWidgets.textWidth(font, string2)) * 0.5F,
                cursorScreenPosY + (g - EfnWidgets.lineHeight(font)) * 0.5F,
                j,
                string2);
        return invisibleButton;
    }

    private static void label(String string, float f, float g, float h) {
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        float f2 = ImGui.getWindowPosX() + f;
        float g2 = ImGui.getWindowPosY() - ImGui.getScrollY() + g;
        EfnWidgets.text(
                ImGui.getWindowDrawList(),
                imFont,
                f2,
                g2 + (h - EfnWidgets.lineHeight(imFont)) * 0.5F,
                -4542808,
                string);
    }

    private static String summary(ConfigProfiles.Profile profile) {
        if (profile.broken()) {
            return "Can't be read - fix the file or delete it";
        } else {
            String partList = partList(profile.parts());
            List list = profile.servers();
            if (list.size() == 1) {
                return partList + "  \u00b7  loads on " + (String) list.get(0);
            } else {
                return list.size() > 1
                        ? partList + "  \u00b7  loads on " + list.size() + " servers"
                        : partList;
            }
        }
    }

    private static String partList(Set<ConfigProfiles.Part> set) {
        StringBuilder sb = new StringBuilder();
        for (ConfigProfiles.Part part : set) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(part.label);
        }
        return sb.isEmpty() ? "nothing" : sb.toString();
    }

    private static String partTip(ConfigProfiles.Part part, boolean bl) {
        String string =
                switch (part) {
                    case MODULES -> "Module settings, keybinds and toggles.";
                    case HUD -> "HUD layout and the HUD theme.";
                    case THEME -> "GUI theme.";
                    case FRIENDS -> "Your friends list.";
                };
        return string
                + (bl
                        ? "\nClick to remove it from this profile."
                        : "\nClick to add it to this profile.");
    }

    private static String key(String string) {
        return string.toLowerCase(Locale.ROOT);
    }
}
