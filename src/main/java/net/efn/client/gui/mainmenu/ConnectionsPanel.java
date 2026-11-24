package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;

import net.efn.client.account.Account;
import net.efn.client.account.AccountType;
import net.efn.client.account.Accounts;
import net.efn.client.gui.HudDraw;
import net.efn.client.proxy.Proxies;
import net.efn.client.proxy.Proxy;
import net.efn.client.proxy.ProxyChecker;
import net.efn.client.proxy.ProxyType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Environment(EnvType.CLIENT)
public final class ConnectionsPanel {

    private static final String NAME_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";

    private static final int WINDOW_FLAGS = 447;

    private static final float ACCOUNTS_W = 820.0F;

    private static final float PROXIES_W = 860.0F;

    private static final float PAD = 24.0F;

    private static final float FIELD_H = 50.0F;

    private static final float ACCOUNT_ROW_H = 68.0F;

    private static final float PROXY_ROW_H = 46.0F;

    private static final float TAB_H = 40.0F;

    private static final float BAR_H = 30.0F;

    private static final float FACE = 46.0F;

    private static final float CHECK = 16.0F;

    private static final int ACCOUNT_ROWS = 6;

    private static final int PROXY_ROWS = 8;

    private static final float FADE = 0.15F;

    private static final float CONFIRM_FOR = 3.0F;

    static final int WINDOW = -15461610;

    private static final int ERROR = -1733516;

    private static final int GOOD = -6501498;

    private final Runnable onProxyChange;

    private final DuskWidgets ui = new DuskWidgets();

    private final MenuFrame frame = new MenuFrame();

    private boolean open;

    private boolean opening;

    private long openedAt;

    private ConnectionsPanel.Tab tab = ConnectionsPanel.Tab.ACCOUNTS;

    private String routeAtOpen = "";

    private Object armed;

    private float armedUntil;

    private AccountType adding = AccountType.CRACKED;

    private AccountType accountFilter;

    private final DuskWidgets.TextField crackedName =
            new DuskWidgets.TextField(16)
                    .placeholder("Username")
                    .allowedChars(
                            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");

    private final DuskWidgets.TextField token =
            new DuskWidgets.TextField(4096).placeholder("Minecraft access token").password(true);

    private CompletableFuture<?> microsoft;

    private String accountError;

    private final DuskWidgets.TextField proxyInput =
            new DuskWidgets.TextField(512).placeholder("host:port or socks5://user:pass@host:port");

    private String proxyMessage;

    private boolean proxyError;

    private ConnectionsPanel.Filter filter = ConnectionsPanel.Filter.ALL;

    private ProxyType typeFilter;

    private ConnectionsPanel.Sort sort = ConnectionsPanel.Sort.ADDED;

    private final Set<Proxy> selected = new HashSet<>();

    private boolean fileDialog;

    public ConnectionsPanel(Runnable onProxyChange2) {
        this.onProxyChange = onProxyChange2;
    }

    public void open(ConnectionsPanel.Tab tab2) {
        if (!this.open) {
            this.open = true;
            this.opening = true;
            this.openedAt = System.nanoTime();
            this.routeAtOpen = route();
        }
        this.tab = tab2;
        this.armed = null;
        if (tab2 == ConnectionsPanel.Tab.PROXIES) {
            this.proxyInput.focus();
        } else if (this.adding != AccountType.MICROSOFT) {
            this.field(this.adding).focus();
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean keyPressed(int i) {
        if (!this.open) {
            return false;
        } else {
            if (i == 256) {
                this.close();
            }
            return true;
        }
    }

    public void render(float f, float g, float h, float f2) {
        if (this.open) {
            ImGuiIO iO = ImGui.getIO();
            boolean bl = !this.opening && ImGui.isMouseClicked(0);
            this.opening = false;
            ImGui.setNextWindowPos(0.0F, 0.0F);
            ImGui.setNextWindowSize(f, g);
            ImGui.pushStyleVar(2, 0.0F, 0.0F);
            ImGui.pushStyleVar(4, 0.0F);
            ImGui.begin("##efn_connections", 447);
            ImGui.popStyleVar(2);
            try {
                this.frame.begin(
                        ImGui.getWindowDrawList(),
                        ImGui.getForegroundDrawList(),
                        f,
                        g,
                        h,
                        (float) (System.nanoTime() - this.openedAt) / 1.0E9F,
                        f2,
                        iO.getMousePosX(),
                        iO.getMousePosY(),
                        bl);
                this.ui.begin(this.frame);
                try {
                    this.draw();
                } finally {
                    this.ui.end();
                }
                this.drawTooltip();
            } finally {
                ImGui.end();
            }
        }
    }

    private void close() {
        this.open = false;
        this.crackedName.setText("");
        this.token.setText("");
        this.proxyInput.setText("");
        this.selected.clear();
        if (this.microsoft != null) {
            Accounts.get().cancelMicrosoft();
        }
        if (!route().equals(this.routeAtOpen)) {
            this.onProxyChange.run();
        }
    }

    private void draw() {
        ImDrawList imDrawList = this.frame.bg();
        float scale = this.frame.scale();
        float width = this.frame.width();
        float height = this.frame.height();
        float f = Math.min(1.0F, this.frame.time() / 0.15F);
        float g = 1.0F - (1.0F - f) * (1.0F - f);
        this.ui.alpha(g);
        MenuVeil.veil(imDrawList, width, height, 0.5F * g);
        boolean bl = this.tab == ConnectionsPanel.Tab.ACCOUNTS;
        List list = bl ? List.of() : this.shownProxies();
        List list2 = bl ? this.shownAccounts() : List.of();
        float textHeight = this.frame.textHeight(this.frame.itemFont());
        float textHeight2 = this.frame.textHeight(this.frame.bodyFont());
        float h =
                bl
                        ? Math.max(3, Math.min(list2.size(), 6)) * 68.0F * scale
                        : Math.max(1, Math.min(list.size(), 8)) * 46.0F * scale;
        float f2 = bl ? 96.0F * scale : 60.0F * scale + 22.0F * scale;
        float g2 = Math.min((bl ? 820.0F : 860.0F) * scale, width - 32.0F * scale);
        float h2 =
                Math.min(
                        24.0F * scale
                                + textHeight
                                + 16.0F * scale
                                + 50.0F * scale
                                + 12.0F * scale
                                + textHeight2
                                + 18.0F * scale
                                + f2
                                + h
                                + 16.0F * scale,
                        height - 32.0F * scale);
        float f3 = Math.round((width - g2) * 0.5F);
        float g3 = Math.round((height - h2) * 0.5F + (1.0F - g) * 6.0F * scale);
        for (int i = 1; i <= 3; i++) {
            float h3 = i * 4.0F * scale;
            imDrawList.addRectFilled(
                    f3 - h3,
                    g3 - h3 + 4.0F * scale,
                    f3 + g2 + h3,
                    g3 + h2 + h3 + 4.0F * scale,
                    HudDraw.col(-16777216, 0.08F * g));
        }
        imDrawList.addRectFilled(f3, g3, f3 + g2, g3 + h2, HudDraw.col(-15461610, g));
        imDrawList.addRect(
                f3,
                g3,
                f3 + g2,
                g3 + h2,
                HudDraw.col(-1575946, 0.1F * g),
                0.0F,
                0,
                Math.max(1.0F, scale));
        float f4 = 24.0F * scale;
        this.frame.text(
                imDrawList,
                this.frame.itemFont(),
                f3 + f4,
                g3 + f4 * 0.8F,
                -1575946,
                g,
                bl ? "Accounts" : "Proxies");
        this.closeCross(
                f3 + g2 - 44.0F * scale,
                g3 + f4 * 0.8F + textHeight * 0.5F - 16.0F * scale,
                32.0F * scale,
                scale,
                g);
        float g4 = g3 + f4 + textHeight + 16.0F * scale;
        float h4 = g3 + h2 - 16.0F * scale;
        if (bl) {
            this.accounts(list2, f3, g4, g2, h4, scale, g);
        } else {
            this.proxies(list, f3, g4, g2, h4, scale, g);
        }
    }

    private void closeCross(float f, float g, float h, float f2, float g2) {
        ImDrawList imDrawList = this.frame.bg();
        boolean hovered = this.ui.hovered(f, g, h, h);
        if (this.ui.pressed(f, g, h, h)) {
            this.close();
        }
        float hover = this.ui.hover("close", hovered);
        float h2 = f + h * 0.5F;
        float f3 = g + h * 0.5F;
        float g3 = 6.0F * f2;
        int col = HudDraw.col(HudDraw.mix(-7693914, -1575946, hover), g2);
        float h3 = Math.max(1.2F, 1.6F * f2);
        imDrawList.addLine(h2 - g3, f3 - g3, h2 + g3, f3 + g3, col, h3);
        imDrawList.addLine(h2 - g3, f3 + g3, h2 + g3, f3 - g3, col, h3);
    }

    private List<Account> shownAccounts() {
        List<Account> list = Accounts.get().list();
        return this.accountFilter == null
                ? list
                : list.stream().filter(account -> account.type() == this.accountFilter).toList();
    }

    private void accounts(
            List<Account> list, float f, float g, float h, float f2, float g2, float h2) {
        float f3 = 24.0F * g2;
        float g3 = f + f3;
        float h3 = h - 2.0F * f3;
        boolean busy = Accounts.get().busy();
        this.addTabs(g3, g, g2, h2);
        g += 54.0F * g2;
        float f4 = 50.0F * g2;
        ImFont imFont = this.frame.bodyFont();
        String string;
        if (this.adding == AccountType.MICROSOFT) {
            String string2 =
                    this.microsoft != null
                            ? "Waiting for the browser..."
                            : "Sign in with Microsoft";
            if (this.box(
                    "microsoft", string2, g3, g, h3, f4, this.microsoft == null && !busy, h2)) {
                this.signInMicrosoft();
            }
            string = "Opens Microsoft's sign-in in your browser";
        } else {
            DuskWidgets.TextField textField = this.field(this.adding);
            if (this.ui.textField(textField, g3, g, h3, f4)
                    && !textField.text().isBlank()
                    && !busy) {
                this.add(textField);
            }
            string =
                    this.adding == AccountType.CRACKED
                            ? "Press Enter to add. Joins servers that don't check accounts."
                            : "Press Enter to add. A token works until it expires, about a day.";
        }
        g += f4 + 12.0F * g2;
        if (this.microsoft != null) {
            float textWidth = this.frame.textWidth(imFont, "Cancel");
            this.text(
                    DuskWidgets.fit(imFont, Accounts.get().status(), h3 - textWidth - 16.0F * g2),
                    g3,
                    g,
                    -1575946,
                    h2);
            if (this.link("cancel", "Cancel", g3 + h3 - textWidth, g, true, h2)) {
                Accounts.get().cancelMicrosoft();
            }
        } else if (busy) {
            this.text(DuskWidgets.fit(imFont, Accounts.get().status(), h3), g3, g, -7693914, h2);
        } else if (this.accountError != null) {
            this.text(DuskWidgets.fit(imFont, this.accountError, h3), g3, g, -1733516, h2);
        } else {
            this.text(DuskWidgets.fit(imFont, string, h3), g3, g, -7693914, h2);
        }
        float g4;
        this.accountFilters(g3, g4 = g + (this.frame.textHeight(imFont) + 18.0F * g2), g2, h2);
        float h4 = f2 - (g = g4 + 42.0F * g2);
        if (list.isEmpty()) {
            this.centered(
                    this.accountFilter == null
                            ? "No accounts yet"
                            : "No " + this.accountFilter.label() + " accounts",
                    f,
                    g,
                    h,
                    h4,
                    h2);
        } else {
            Account account = Accounts.get().active();
            float f5 = f + 8.0F * g2;
            float g5 = h - 16.0F * g2;
            float h5 = 68.0F * g2;
            float beginScroll = this.ui.beginScroll("accounts", f5, g, g5, h4, list.size() * h5);
            for (int m = 0; m < list.size(); m++) {
                Account account2 = (Account) list.get(m);
                this.accountRow(
                        account2,
                        account2.equals(account),
                        f5,
                        g - beginScroll + m * h5,
                        g5,
                        h5,
                        g2,
                        h2);
            }
            this.ui.endScroll();
        }
    }

    private void addTabs(float f, float g, float h, float f2) {
        ImDrawList imDrawList = this.frame.bg();
        ImFont imFont = this.frame.menuFont();
        float g2 = 40.0F * h;
        float h2 = g + (g2 - this.frame.textHeight(imFont)) * 0.5F;
        for (AccountType accountType : AccountType.values()) {
            String label2 = accountType.label();
            float textWidth = this.frame.textWidth(imFont, label2);
            boolean hovered = this.ui.hovered(f - 6.0F * h, g, textWidth + 12.0F * h, g2);
            if (this.ui.pressed(f - 6.0F * h, g, textWidth + 12.0F * h, g2)
                    && this.microsoft == null
                    && accountType != this.adding) {
                this.adding = accountType;
                this.accountError = null;
                if (accountType != AccountType.MICROSOFT) {
                    this.field(accountType).focus();
                }
            }
            float hover = this.ui.hover(new ConnectionsPanel.Key("add", label2), hovered);
            this.frame.text(
                    imDrawList,
                    imFont,
                    f,
                    h2,
                    accountType == this.adding ? -1575946 : HudDraw.mix(-7693914, -1575946, hover),
                    f2,
                    label2);
            if (accountType == this.adding) {
                imDrawList.addRectFilled(
                        f, g + g2 - 2.0F * h, f + textWidth, g + g2, HudDraw.col(-15408912, f2));
            }
            f += textWidth + 30.0F * h;
        }
    }

    private void accountFilters(float f, float g, float h, float f2) {
        List list = Accounts.get().list();
        if (this.chip("all-accounts", "All", list.size(), this.accountFilter == null, f, g, f2)) {
            this.accountFilter = null;
        }
        f += this.chipWidth("All", list.size()) + 18.0F * h;
        for (AccountType accountType : AccountType.values()) {
            int i =
                    (int)
                            list.stream()
                                    .filter(account -> ((Account) account).type() == accountType)
                                    .count();
            if (this.chip(
                    accountType,
                    accountType.label(),
                    i,
                    this.accountFilter == accountType,
                    f,
                    g,
                    f2)) {
                this.accountFilter = accountType;
            }
            f += this.chipWidth(accountType.label(), i) + 18.0F * h;
        }
    }

    private void accountRow(
            Account account, boolean bl, float f, float g, float h, float f2, float g2, float h2) {
        ImDrawList imDrawList = this.frame.bg();
        boolean hovered = this.ui.hovered(f, g, h, f2);
        float hover = this.ui.hover(account, hovered);
        if (hover > 0.01F && !bl) {
            imDrawList.addRectFilled(
                    f, g, f + h, g + f2, HudDraw.col(-1575946, 0.05F * hover * h2));
        }
        float f3 = 46.0F * g2;
        float g3 = f + 12.0F * g2;
        PlayerFaces.draw(imDrawList, account, g3, g + (f2 - f3) * 0.5F, f3, g2, h2);
        String string =
                account.name().isEmpty() ? account.type().label() + " account" : account.name();
        float removeCross =
                this.removeCross(
                        new ConnectionsPanel.Key(account, "remove"),
                        f + h - 8.0F * g2,
                        g,
                        f2,
                        hover,
                        g2,
                        h2);
        if (removeCross < 0.0F) {
            Accounts.get().remove(account);
        } else {
            if (bl) {
                removeCross = this.tag("Active", -6557705, removeCross, g, f2, h2);
            }
            this.entry(
                    this.frame.menuFont(),
                    this.frame.listFont(),
                    g3 + f3 + 16.0F * g2,
                    g,
                    f2,
                    removeCross - 12.0F * g2,
                    string,
                    account.type().label(),
                    h2);
            if (this.ui.pressed(f, g, h, f2) && !bl) {
                this.login(account);
            }
        }
    }

    private DuskWidgets.TextField field(AccountType accountType) {
        return accountType == AccountType.SESSION ? this.token : this.crackedName;
    }

    private void add(DuskWidgets.TextField textField) {
        CompletableFuture future =
                textField == this.token
                        ? Accounts.get().addSession(textField.text())
                        : Accounts.get().addCracked(textField.text());
        this.track(future);
        future.thenRun(() -> textField.setText(""));
    }

    private void login(Account account) {
        if (!Accounts.get().busy()) {
            this.track(Accounts.get().login(account));
        }
    }

    private void signInMicrosoft() {
        CompletableFuture future = Accounts.get().addMicrosoft();
        this.microsoft = future;
        this.track(future);
        future.whenComplete((account, throwable) -> this.microsoft = null);
    }

    private void track(CompletableFuture<?> future) {
        this.accountError = null;
        future.whenComplete(
                (value, throwable) ->
                        this.accountError = throwable == null ? null : messageOf(throwable));
    }

    private static String messageOf(Throwable throwable) {
        Throwable throwable2 =
                throwable instanceof CompletionException && throwable.getCause() != null
                        ? throwable.getCause()
                        : throwable;
        String message = throwable2.getMessage();
        return message != null && !message.isBlank() ? message : "Login failed";
    }

    private List<Proxy> shownProxies() {
        ArrayList list = new ArrayList();
        for (Proxy proxy : Proxies.get().list()) {
            if (this.filter.matches(proxy)
                    && (this.typeFilter == null || proxy.type == this.typeFilter)) {
                list.add(proxy);
            }
        }
        if (this.sort.order != null) {
            list.sort(this.sort.order);
        }
        return list;
    }

    private static int pingOrder(Proxy proxy) {
        if (proxy.ping >= 0) {
            return proxy.ping;
        } else {
            return proxy.ping == -2 ? 2147483646 : 2147483647;
        }
    }

    private void proxies(
            List<Proxy> list, float f, float g, float h, float f2, float g2, float h2) {
        float f3 = 24.0F * g2;
        float g3 = f + f3;
        float h3 = h - 2.0F * f3;
        List list2 = Proxies.get().list();
        this.selected.retainAll(new HashSet(list2));
        float f4 = 50.0F * g2;
        float g4 = h3 - f4 - 8.0F * g2;
        if (this.ui.textField(this.proxyInput, g3, g, g4, f4)
                && !this.proxyInput.text().isBlank()) {
            int importText = Proxies.get().importText(this.proxyInput.text());
            this.message(
                    importText == 0 ? "Not added: already saved, or not host:port" : null,
                    importText == 0);
            if (importText > 0) {
                this.proxyInput.setText("");
            }
        }
        float h4;
        if (this.pasteButton(h4 = g3 + h3 - f4, g, f4, h2)) {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            this.imported(
                    Proxies.get().importText(clipboard),
                    clipboard.isBlank()
                            ? "The clipboard is empty"
                            : "Nothing new on the clipboard");
        }
        if (this.ui.hoverOf("paste") > 0.5F) {
            this.frame.tooltip(
                    "Paste proxies from the clipboard", h4 + f4 * 0.5F, g + f4 + 6.0F * g2);
        }
        ImFont imFont = this.frame.bodyFont();
        if (this.link("file", "Import file", g3, g = g + 62.0F * g2, !this.fileDialog, h2)) {
            this.importFile();
        }
        if (this.ui.hoverOf("file") > 0.5F) {
            this.frame.tooltip(
                    "A text file with one proxy per line",
                    g3 + this.frame.textWidth(imFont, "Import file") * 0.5F,
                    g + this.frame.textHeight(imFont) + 8.0F * g2);
        }
        float f5 = g3 + (this.frame.textWidth(imFont, "Import file") + 24.0F * g2);
        int pending = ProxyChecker.get().pending();
        if (pending > 0) {
            this.statusRight("Checking, " + pending + " left", false, f5, g3 + h3, g, h2);
        } else if (this.proxyMessage != null) {
            this.statusRight(this.proxyMessage, this.proxyError, f5, g3 + h3, g, h2);
        }
        float g5;
        this.filters(list2, g3, g5 = g + (this.frame.textHeight(imFont) + 18.0F * g2), h3, g2, h2);
        this.actions(list, list2, g3, g = g5 + (30.0F * g2 + 10.0F * g2), h3, g2, h2);
        float h5;
        float f6 = f2 - (h5 = g + (30.0F * g2 + 12.0F * g2));
        if (list.isEmpty()) {
            this.centered(
                    list2.isEmpty() ? "No proxies yet" : "None match the filter", f, h5, h, f6, h2);
        } else {
            float g6 = f + 8.0F * g2;
            float h6 = h - 16.0F * g2;
            float f7 = 46.0F * g2;
            float beginScroll = this.ui.beginScroll("proxies", g6, h5, h6, f6, list.size() * f7);
            int i = Math.max(0, (int) (beginScroll / f7));
            int j = Math.min(list.size(), (int) ((beginScroll + f6) / f7) + 1);
            for (int q = i; q < j; q++) {
                this.proxyRow((Proxy) list.get(q), g6, h5 - beginScroll + q * f7, h6, f7, g2, h2);
            }
            this.ui.endScroll();
        }
    }

    private void filters(List<Proxy> list, float f, float g, float h, float f2, float g2) {
        int[] ints = new int[ConnectionsPanel.Filter.values().length];
        int[] ints2 = new int[ProxyType.values().length];
        for (Proxy proxy : list) {
            for (ConnectionsPanel.Filter filter2 : ConnectionsPanel.Filter.values()) {
                if (filter2.matches(proxy)) {
                    int ordinal = filter2.ordinal();
                    ints[ordinal]++;
                }
            }
            int ordinal2 = proxy.type.ordinal();
            ints2[ordinal2]++;
        }
        float h2 = f;
        for (ConnectionsPanel.Filter filter3 : ConnectionsPanel.Filter.values()) {
            if (this.chip(
                    filter3,
                    filter3.label,
                    ints[filter3.ordinal()],
                    this.filter == filter3,
                    h2,
                    g,
                    g2)) {
                this.filter = filter3;
            }
            h2 += this.chipWidth(filter3.label, ints[filter3.ordinal()]) + 18.0F * f2;
        }
        float f3;
        this.frame
                .bg()
                .addLine(
                        f3 = h2 + 10.0F * f2,
                        g + 4.0F * f2,
                        f3,
                        g + 30.0F * f2 - 4.0F * f2,
                        HudDraw.col(-1575946, 0.12F * g2),
                        Math.max(1.0F, f2));
        h2 = f3 + 20.0F * f2;
        for (ProxyType proxyType : ProxyType.values()) {
            boolean bl = this.typeFilter == proxyType;
            if (this.chip(
                    proxyType, proxyType.label(), ints2[proxyType.ordinal()], bl, h2, g, g2)) {
                this.typeFilter = bl ? null : proxyType;
            }
            h2 += this.chipWidth(proxyType.label(), ints2[proxyType.ordinal()]) + 18.0F * f2;
        }
        ImFont imFont = this.frame.bodyFont();
        String string = "Sort: " + this.sort.label;
        if (this.link(
                "sort",
                string,
                f + h - this.frame.textWidth(imFont, string),
                g + (30.0F * f2 - this.frame.textHeight(imFont)) * 0.5F,
                true,
                g2)) {
            this.sort =
                    ConnectionsPanel.Sort.values()[
                            (this.sort.ordinal() + 1) % ConnectionsPanel.Sort.values().length];
        }
    }

    private void actions(
            List<Proxy> list, List<Proxy> list2, float f, float g, float h, float f2, float g2) {
        ImFont imFont = this.frame.bodyFont();
        float h2 = g + 30.0F * f2 * 0.5F;
        float f3 = h2 - this.frame.textHeight(imFont) * 0.5F;
        boolean bl = !list.isEmpty() && this.selected.containsAll(list);
        if (this.checkbox("select-all", bl, f + 12.0F * f2, h2, f2, g2)) {
            if (bl) {
                this.selected.removeAll(list);
            } else {
                this.selected.addAll(list);
            }
        }
        float g3 = f + 12.0F * f2 + 16.0F * f2 + 18.0F * f2;
        if (this.selected.isEmpty()) {
            if (this.link("check-all", "Check all", g3, f3, !list.isEmpty(), g2)) {
                ProxyChecker.get().check(list);
            }
            List list3;
            ConnectionsPanel.Key key;
            if (!(list3 = list2.stream().filter(proxy -> proxy.ping == -1).toList()).isEmpty()
                    && this.link(
                            key = new ConnectionsPanel.Key("bulk", "remove-dead"),
                            this.armed(key) ? "Remove " + list3.size() + " dead?" : "Remove dead",
                            g3 = g3 + (this.frame.textWidth(imFont, "Check all") + 22.0F * f2),
                            f3,
                            true,
                            g2,
                            this.armed(key) ? -1733516 : -7693914)
                    && this.confirm(key)) {
                Proxies.get().removeAll(list3);
                this.message("Removed " + list3.size() + " dead", false);
            }
        } else {
            List list4 = List.copyOf(this.selected);
            String string = list4.size() + " selected";
            this.text(string, g3, f3, -1575946, g2);
            if (this.link(
                    "check-selected",
                    "Check",
                    g3 = g3 + (this.frame.textWidth(imFont, string) + 22.0F * f2),
                    f3,
                    true,
                    g2)) {
                ProxyChecker.get().check(list4);
            }
            if (this.link(
                    "copy-selected",
                    "Copy",
                    g3 = g3 + (this.frame.textWidth(imFont, "Check") + 22.0F * f2),
                    f3,
                    true,
                    g2)) {
                MinecraftClient.getInstance()
                        .keyboard
                        .setClipboard(
                                String.join(
                                        "\n",
                                        list4.stream()
                                                .map(p -> ConnectionsPanel.line((Proxy) p))
                                                .toList()));
                this.message(
                        "Copied " + list4.size() + (list4.size() == 1 ? " proxy" : " proxies"),
                        false);
            }
            String string2;
            ConnectionsPanel.Key key2;
            if (this.link(
                            key2 = new ConnectionsPanel.Key("bulk", "remove-selected"),
                            string2 = this.armed(key2) ? "Remove " + list4.size() + "?" : "Remove",
                            g3 = g3 + (this.frame.textWidth(imFont, "Copy") + 22.0F * f2),
                            f3,
                            true,
                            g2,
                            this.armed(key2) ? -1733516 : -7693914)
                    && this.confirm(key2)) {
                Proxies.get().removeAll(list4);
                this.selected.clear();
                this.message("Removed " + list4.size(), false);
            }
            if (this.link(
                    "clear-selection",
                    "Clear",
                    g3 = g3 + (this.frame.textWidth(imFont, string2) + 22.0F * f2),
                    f3,
                    true,
                    g2)) {
                this.selected.clear();
            }
        }
    }

    private void proxyRow(Proxy proxy, float f, float g, float h, float f2, float g2, float h2) {
        ImDrawList imDrawList = this.frame.bg();
        boolean bl = proxy.enabled || proxy.isValid();
        boolean hovered = this.ui.hovered(f, g, h, f2);
        float hover = this.ui.hover(proxy, hovered);
        boolean bl2 = this.selected.contains(proxy);
        if (bl2) {
            imDrawList.addRectFilled(f, g, f + h, g + f2, HudDraw.col(-15408912, 0.07F * h2));
        } else if (hover > 0.01F && bl) {
            imDrawList.addRectFilled(
                    f, g, f + h, g + f2, HudDraw.col(-1575946, 0.05F * hover * h2));
        }
        float f3 = g + f2 * 0.5F;
        if (this.checkbox(
                new ConnectionsPanel.Key(proxy, "select"), bl2, f + 12.0F * g2, f3, g2, h2)) {
            if (bl2) {
                this.selected.remove(proxy);
            } else {
                this.selected.add(proxy);
            }
        }
        proxyDot(imDrawList, f + 52.0F * g2, f3, proxy.enabled, 1.3F * g2, h2);
        float removeCross =
                this.removeCross(
                        new ConnectionsPanel.Key(proxy, "remove"),
                        f + h - 8.0F * g2,
                        g,
                        f2,
                        hover,
                        g2,
                        h2);
        if (removeCross < 0.0F) {
            Proxies.get().remove(proxy);
            this.selected.remove(proxy);
        } else {
            if (proxy.enabled) {
                removeCross = this.tag("In use", -6557705, removeCross, g, f2, h2);
            }
            removeCross = this.ping(proxy, removeCross, g, f2, h2);
            String string = proxy.username.isEmpty() ? "" : " \u00b7 " + proxy.username;
            String string2 =
                    proxy.name.equals(proxy.displayAddress())
                            ? proxy.type.label() + string
                            : proxy.type.label() + " \u00b7 " + proxy.displayAddress() + string;
            this.entry(
                    this.frame.bodyFont(),
                    this.frame.detailFont(),
                    f + 74.0F * g2,
                    g,
                    f2,
                    removeCross - 12.0F * g2,
                    proxy.name,
                    string2,
                    h2);
            if (this.ui.pressed(f, g, h, f2) && bl) {
                Proxies.get().setEnabled(proxy, !proxy.enabled);
                this.message(null, false);
            }
        }
    }

    private float ping(Proxy proxy, float f, float g, float h, float f2) {
        float g2 = f - 96.0F * this.frame.scale();
        if (ProxyChecker.get().isChecking(proxy)) {
            int i = (int) (this.frame.time() * 3.0F) % 4;
            this.tag("Checking" + ".".repeat(i) + " ".repeat(3 - i), -7693914, f, g, h, f2);
        } else if (proxy.ping >= 0) {
            this.tag(proxy.ping + " ms", -6501498, f, g, h, f2);
        } else if (proxy.ping == -1) {
            this.tag("Dead", -1733516, f, g, h, f2);
        }
        return g2;
    }

    private static String line(Proxy proxy) {
        String string =
                proxy.username.isEmpty()
                        ? ""
                        : proxy.username
                                + (proxy.password.isEmpty() ? "" : ":" + proxy.password)
                                + "@";
        String string2 =
                proxy.type == ProxyType.SOCKS4
                        ? "socks4://"
                        : (string.isEmpty() ? "" : "socks5://");
        return string2 + string + proxy.displayAddress();
    }

    private void imported(int i, String string) {
        this.message(i > 0 ? "Added " + i + (i == 1 ? " proxy" : " proxies") : string, i == 0);
    }

    private void importFile() {
        this.fileDialog = true;
        Thread thread =
                new Thread(
                        () -> {
                            String string = null;
                            try {
                                string =
                                        TinyFileDialogs.tinyfd_openFileDialog(
                                                "Import proxies", null, null, null, false);
                            } finally {
                                String string2 = string;
                                MinecraftClient.getInstance()
                                        .execute(
                                                () -> {
                                                    this.fileDialog = false;
                                                    if (string2 != null) {
                                                        this.importFrom(Path.of(string2));
                                                    }
                                                });
                            }
                        },
                        "EFN proxy import");
        thread.setDaemon(true);
        thread.start();
    }

    private void importFrom(Path path) {
        try {
            this.imported(
                    Proxies.get().importText(Files.readString(path)),
                    "No proxies found in " + path.getFileName());
        } catch (IOException exception) {
            this.message("Couldn't read " + path.getFileName(), true);
        }
    }

    private void message(String proxyMessage2, boolean proxyError2) {
        this.proxyMessage = proxyMessage2;
        this.proxyError = proxyError2;
    }

    private static String route() {
        Proxy proxy = Proxies.get().enabled();
        return proxy == null
                ? ""
                : proxy.type
                        + " "
                        + proxy.displayAddress()
                        + " "
                        + proxy.username
                        + " "
                        + proxy.password;
    }

    private void entry(
            ImFont imFont,
            ImFont imFont2,
            float f,
            float g,
            float h,
            float f2,
            String string,
            String string2,
            float g2) {
        float textHeight = this.frame.textHeight(imFont);
        float h2 = 3.0F * this.frame.scale();
        boolean bl = string2.isEmpty();
        float f3 =
                bl
                        ? g + (h - textHeight) * 0.5F
                        : g + (h - textHeight - h2 - this.frame.textHeight(imFont2)) * 0.5F;
        this.frame.text(
                this.frame.bg(),
                imFont,
                f,
                f3,
                -1575946,
                g2,
                DuskWidgets.fit(imFont, string, f2 - f));
        if (!bl) {
            this.frame.text(
                    this.frame.bg(),
                    imFont2,
                    f,
                    f3 + textHeight + h2,
                    -7693914,
                    g2,
                    DuskWidgets.fit(imFont2, string2, f2 - f));
        }
    }

    private float tag(String string, int i, float f, float g, float h, float f2) {
        ImFont imFont = this.frame.bodyFont();
        float g2 = f - this.frame.textWidth(imFont, string);
        this.frame.text(
                this.frame.bg(),
                imFont,
                g2,
                g + (h - this.frame.textHeight(imFont)) * 0.5F,
                i,
                f2,
                string);
        return g2 - 14.0F * this.frame.scale();
    }

    private float removeCross(
            ConnectionsPanel.Key key, float f, float g, float h, float f2, float g2, float h2) {
        ImDrawList imDrawList = this.frame.bg();
        if (this.armed(key)) {
            ImFont imFont = this.frame.bodyFont();
            String string = "Remove?";
            float textWidth = this.frame.textWidth(imFont, string);
            float f3 = f - textWidth;
            float textHeight = this.frame.textHeight(imFont);
            boolean hovered = this.ui.hovered(f3 - 6.0F * g2, g, textWidth + 12.0F * g2, h);
            if (this.ui.pressed(f3 - 6.0F * g2, g, textWidth + 12.0F * g2, h)) {
                this.armed = null;
                return -1.0F;
            } else {
                this.frame.text(
                        imDrawList,
                        imFont,
                        f3,
                        g + (h - textHeight) * 0.5F,
                        -1733516,
                        hovered ? h2 : 0.85F * h2,
                        string);
                return f3 - 14.0F * g2;
            }
        } else {
            float g3 = 28.0F * g2;
            float h3 = f - g3;
            float f4 = g + h * 0.5F;
            boolean bl = f2 > 0.5F && this.ui.hovered(h3, f4 - g3 * 0.5F, g3, g3);
            if (bl && this.ui.pressed(h3, f4 - g3 * 0.5F, g3, g3)) {
                this.confirm(key);
            }
            float hover = this.ui.hover(key, bl);
            if (f2 > 0.01F) {
                float g4 = h3 + g3 * 0.5F;
                float h4 = 5.0F * g2;
                float f5 = Math.max(1.2F, 1.5F * g2);
                int col = HudDraw.col(HudDraw.mix(-7693914, -1733516, hover), f2 * h2);
                imDrawList.addLine(g4 - h4, f4 - h4, g4 + h4, f4 + h4, col, f5);
                imDrawList.addLine(g4 - h4, f4 + h4, g4 + h4, f4 - h4, col, f5);
            }
            return h3 - 6.0F * g2;
        }
    }

    private boolean checkbox(Object object, boolean bl, float f, float g, float h, float f2) {
        ImDrawList imDrawList = this.frame.bg();
        float g2 = 8.0F * h;
        float h2 = 16.0F * h;
        float f3 = g - h2 * 0.5F;
        boolean hovered = this.ui.hovered(f - g2, f3 - g2, h2 + 2.0F * g2, h2 + 2.0F * g2);
        boolean bl2 = hovered && this.ui.pressed(f - g2, f3 - g2, h2 + 2.0F * g2, h2 + 2.0F * g2);
        float hover = this.ui.hover(object, hovered);
        if (bl) {
            imDrawList.addRectFilled(f, f3, f + h2, f3 + h2, HudDraw.col(-15408912, f2));
            imDrawList.pathClear();
            imDrawList.pathLineTo(f + h2 * 0.22F, f3 + h2 * 0.52F);
            imDrawList.pathLineTo(f + h2 * 0.42F, f3 + h2 * 0.72F);
            imDrawList.pathLineTo(f + h2 * 0.78F, f3 + h2 * 0.3F);
            imDrawList.pathStroke(HudDraw.col(-16777216, f2), 0, Math.max(1.4F, 2.0F * h));
        } else {
            imDrawList.addRectFilled(f, f3, f + h2, f3 + h2, HudDraw.col(-16777216, 0.3F * f2));
            imDrawList.addRect(
                    f,
                    f3,
                    f + h2,
                    f3 + h2,
                    HudDraw.col(
                            HudDraw.mix(-1575946, -15408912, hover), (0.3F + 0.4F * hover) * f2),
                    0.0F,
                    0,
                    Math.max(1.0F, h));
        }
        return bl2;
    }

    private float chipWidth(String string, int i) {
        return this.frame.textWidth(this.frame.bodyFont(), string)
                + 7.0F * this.frame.scale()
                + this.frame.textWidth(this.frame.bodyFont(), Integer.toString(i));
    }

    private boolean chip(
            Object object, String string, int i, boolean bl, float f, float g, float h) {
        ImDrawList imDrawList = this.frame.bg();
        ImFont imFont = this.frame.bodyFont();
        float scale = this.frame.scale();
        float f2;
        float chipWidth;
        boolean hovered =
                this.ui.hovered(
                        f - 4.0F * scale,
                        g,
                        (chipWidth = this.chipWidth(string, i)) + 8.0F * scale,
                        f2 = 30.0F * scale);
        boolean bl2 = hovered && this.ui.pressed(f - 4.0F * scale, g, chipWidth + 8.0F * scale, f2);
        float hover = this.ui.hover(object, hovered);
        float g2 = g + (f2 - this.frame.textHeight(imFont)) * 0.5F;
        this.frame.text(
                imDrawList,
                imFont,
                f,
                g2,
                bl ? -6557705 : HudDraw.mix(-7693914, -1575946, hover),
                h,
                string);
        float h2 = this.frame.textWidth(imFont, string) + 7.0F * scale;
        this.frame.text(imDrawList, imFont, f + h2, g2, -7693914, 0.8F * h, Integer.toString(i));
        return bl2;
    }

    private boolean box(
            Object object,
            String string,
            float f,
            float g,
            float h,
            float f2,
            boolean bl,
            float g2) {
        ImDrawList imDrawList = this.frame.bg();
        float scale = this.frame.scale();
        float h2 = bl ? g2 : 0.5F * g2;
        boolean bl2 = bl && this.ui.hovered(f, g, h, f2);
        boolean bl3 = bl2 && this.ui.pressed(f, g, h, f2);
        float hover = this.ui.hover(object, bl2);
        imDrawList.addRectFilled(f, g, f + h, g + f2, HudDraw.col(-16777216, 0.35F * h2));
        if (hover > 0.01F) {
            imDrawList.addRectFilled(
                    f, g, f + h, g + f2, HudDraw.col(-15408912, 0.08F * hover * h2));
        }
        imDrawList.addRect(
                f,
                g,
                f + h,
                g + f2,
                HudDraw.col(HudDraw.mix(-1575946, -15408912, hover), (0.2F + 0.5F * hover) * h2),
                0.0F,
                0,
                Math.max(1.0F, scale));
        ImFont imFont = this.frame.bodyFont();
        this.frame.text(
                imDrawList,
                imFont,
                f + (h - this.frame.textWidth(imFont, string)) * 0.5F,
                g + (f2 - this.frame.textHeight(imFont)) * 0.5F,
                HudDraw.mix(-1575946, -6621958, hover),
                h2,
                string);
        return bl3;
    }

    private boolean pasteButton(float f, float g, float h, float f2) {
        ImDrawList imDrawList = this.frame.bg();
        float scale = this.frame.scale();
        boolean hovered = this.ui.hovered(f, g, h, h);
        boolean bl = hovered && this.ui.pressed(f, g, h, h);
        float hover = this.ui.hover("paste", hovered);
        imDrawList.addRectFilled(f, g, f + h, g + h, HudDraw.col(-16777216, 0.35F * f2));
        if (hover > 0.01F) {
            imDrawList.addRectFilled(
                    f, g, f + h, g + h, HudDraw.col(-15408912, 0.08F * hover * f2));
        }
        imDrawList.addRect(
                f,
                g,
                f + h,
                g + h,
                HudDraw.col(HudDraw.mix(-1575946, -15408912, hover), (0.12F + 0.5F * hover) * f2),
                0.0F,
                0,
                Math.max(1.0F, scale));
        float g2 = f + h * 0.5F;
        float h2 = g + h * 0.5F + scale;
        float f3 = Math.max(1.2F, 1.5F * scale);
        int col = HudDraw.col(HudDraw.mix(-7693914, -6621958, hover), f2);
        imDrawList.addRect(
                g2 - 7.0F * scale,
                h2 - 8.0F * scale,
                g2 + 7.0F * scale,
                h2 + 9.0F * scale,
                col,
                2.0F * scale,
                0,
                f3);
        imDrawList.addRectFilled(
                g2 - 3.5F * scale,
                h2 - 10.5F * scale,
                g2 + 3.5F * scale,
                h2 - 6.0F * scale,
                col,
                scale);
        imDrawList.addLine(
                g2 - 3.5F * scale,
                h2 - 1.0F * scale,
                g2 + 3.5F * scale,
                h2 - 1.0F * scale,
                col,
                f3);
        imDrawList.addLine(
                g2 - 3.5F * scale,
                h2 + 3.5F * scale,
                g2 + 2.0F * scale,
                h2 + 3.5F * scale,
                col,
                f3);
        return bl;
    }

    private boolean link(Object object, String string, float f, float g, boolean bl, float h) {
        return this.link(object, string, f, g, bl, h, -7693914);
    }

    private boolean link(
            Object object, String string, float f, float g, boolean bl, float h, int i) {
        ImFont imFont = this.frame.bodyFont();
        float scale = this.frame.scale();
        float textWidth = this.frame.textWidth(imFont, string);
        float textHeight = this.frame.textHeight(imFont);
        boolean bl2 =
                bl && this.ui.hovered(f, g - 4.0F * scale, textWidth, textHeight + 8.0F * scale);
        boolean bl3 =
                bl2 && this.ui.pressed(f, g - 4.0F * scale, textWidth, textHeight + 8.0F * scale);
        float hover = this.ui.hover(object, bl2);
        float f2 = bl ? h : 0.4F * h;
        this.frame.text(this.frame.bg(), imFont, f, g, HudDraw.mix(i, -1575946, hover), f2, string);
        if (hover > 0.01F) {
            this.frame
                    .bg()
                    .addLine(
                            f,
                            g + textHeight + scale,
                            f + textWidth * hover,
                            g + textHeight + scale,
                            HudDraw.col(-1575946, 0.6F * hover * f2),
                            Math.max(1.0F, scale));
        }
        return bl3;
    }

    private void statusRight(String string, boolean bl, float f, float g, float h, float f2) {
        ImFont imFont = this.frame.bodyFont();
        String fit = DuskWidgets.fit(imFont, string, g - f);
        this.text(fit, g - this.frame.textWidth(imFont, fit), h, bl ? -1733516 : -7693914, f2);
    }

    private void text(String string, float f, float g, int i, float h) {
        this.frame.text(this.frame.bg(), this.frame.bodyFont(), f, g, i, h, string);
    }

    private void centered(String string, float f, float g, float h, float f2, float g2) {
        ImFont imFont = this.frame.bodyFont();
        this.frame.text(
                this.frame.bg(),
                imFont,
                f + (h - this.frame.textWidth(imFont, string)) * 0.5F,
                g + (f2 - this.frame.textHeight(imFont)) * 0.5F,
                -7693914,
                g2,
                string);
    }

    private void drawTooltip() {
        String pendingTooltip = this.frame.pendingTooltip();
        if (pendingTooltip != null) {
            ImDrawList imDrawList = this.frame.fg();
            ImFont imFont = this.frame.detailFont();
            float scale = this.frame.scale();
            float f = this.frame.textWidth(imFont, pendingTooltip) + 18.0F * scale;
            float g = this.frame.textHeight(imFont) + 10.0F * scale;
            float h =
                    Math.max(
                            6.0F * scale,
                            Math.min(
                                    this.frame.width() - f - 6.0F * scale,
                                    this.frame.tooltipX() - f * 0.5F));
            float tooltipY = this.frame.tooltipY();
            imDrawList.addRectFilled(
                    h, tooltipY, h + f, tooltipY + g, HudDraw.col(-16053748, 0.96F));
            imDrawList.addRect(
                    h,
                    tooltipY,
                    h + f,
                    tooltipY + g,
                    HudDraw.col(-1575946, 0.14F),
                    0.0F,
                    0,
                    Math.max(1.0F, scale));
            this.frame.text(
                    imDrawList,
                    imFont,
                    h + 9.0F * scale,
                    tooltipY + 5.0F * scale,
                    -1575946,
                    1.0F,
                    pendingTooltip);
        }
    }

    private boolean armed(Object object) {
        return object.equals(this.armed) && this.frame.time() < this.armedUntil;
    }

    private boolean confirm(Object armed2) {
        if (this.armed(armed2)) {
            this.armed = null;
            return true;
        } else {
            this.armed = armed2;
            this.armedUntil = this.frame.time() + 3.0F;
            return false;
        }
    }

    static void proxyDot(ImDrawList imDrawList, float f, float g, boolean bl, float h, float f2) {
        if (bl) {
            imDrawList.addCircleFilled(f, g, 7.0F * h, HudDraw.col(-15408912, 0.16F * f2), 20);
            imDrawList.addCircleFilled(f, g, 3.5F * h, HudDraw.col(-15408912, f2), 12);
        } else {
            imDrawList.addCircle(
                    f, g, 3.5F * h, HudDraw.col(-7693914, 0.8F * f2), 12, Math.max(1.0F, h));
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum Filter {
        ALL("All"),
        WORKING("Working"),
        DEAD("Dead"),
        UNCHECKED("Unchecked");

        final String label;

        private Filter(String label2) {
            this.label = label2;
        }

        boolean matches(Proxy proxy) {
            return switch (this) {
                case ALL -> true;
                case WORKING -> proxy.ping >= 0;
                case DEAD -> proxy.ping == -1;
                case UNCHECKED -> proxy.ping == -2;
            };
        }
    }

    @Environment(EnvType.CLIENT)
    private record Key(Object owner, String what) {}

    @Environment(EnvType.CLIENT)
    private static enum Sort {
        ADDED("Added", null),
        PING("Ping", Comparator.comparingInt(ConnectionsPanel::pingOrder)),
        NAME("Name", Comparator.comparing(proxy -> proxy.name.toLowerCase(Locale.ROOT)));

        final String label;

        final Comparator<Proxy> order;

        private Sort(String label2, Comparator<Proxy> order2) {
            this.label = label2;
            this.order = order2;
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Tab {
        ACCOUNTS,
        PROXIES;

        private Tab() {}
    }
}
