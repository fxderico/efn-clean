package net.efn.client.module.impl.misc.noinvleak;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBuffer.MappedView;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.sun.jna.Platform;

import imgui.ImDrawList;
import imgui.ImGui;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.efn.client.gui.BorderlessFullscreen;
import net.efn.client.gui.ItemIcons;
import net.efn.client.gui.ItemLook;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Atlases;

import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class StreamOverlay {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final int SLOTS = 41;

    private static final int CURSOR = 41;

    private static final int ENTRIES = 42;

    private static final long SLOT_BITS = 2199023255551L;

    private static final int CELLS_PER_SIDE = 7;

    private static final int CACHE_LIMIT = 4194304;

    private static final long READBACK_TIMEOUT = 1000000000L;

    private static final long BACKGROUND_INTERVAL = 33000000L;

    private static final int FALLBACK_BACKGROUND = -7631989;

    private static final int[] BLANK = new int[0];

    private static final Runnable NOTHING = () -> {};

    private static final ItemStack[] stack = new ItemStack[42];

    private static final int[] guiX = new int[42];

    private static final int[] guiY = new int[42];

    private static long mask;

    private static int hovered = -1;

    private static boolean selectionSeen;

    private static int selectionX;

    private static int selectionY;

    private static long fakeMask;

    private static int fakeScale;

    private static ImDrawList fakeList;

    private static final DigitFont.Sink FAKE_SINK =
            (i, j, k, value, m) ->
                    fakeList.addRectFilled(
                            i,
                            j,
                            i + k,
                            j + value,
                            m & -16711936 | m << 16 & 0xFF0000 | m >>> 16 & 0xFF);

    private static final ItemStack[] shownStack = new ItemStack[42];

    private static final int[] shownX = new int[42];

    private static final int[] shownY = new int[42];

    private static final int[] shownCount = new int[42];

    private static final int[] shownBar = new int[42];

    private static long shownMask;

    private static int shownScale;

    private static boolean shownOpaque;

    private static boolean shownSelection;

    private static int shownSelectionX;

    private static int shownSelectionY;

    private static int shownHover = -1;

    private static int shownWindowX;

    private static int shownWindowY;

    private static GpuTexture shownAtlas;

    private static int boxX;

    private static int boxY;

    private static boolean backgroundValid;

    private static ItemStack cursorStack;

    private static int cursorCount;

    private static int cursorBar;

    private static int cursorScale;

    private static int cursorX;

    private static int cursorY;

    private static final ItemLook look = new ItemLook();

    private static final Object2ObjectOpenHashMap<List<Object>, int[]> icons =
            new Object2ObjectOpenHashMap<>();

    private static final int[][] slotIcon = new int[42][];

    private static final int[] pendingSlot = new int[49];

    private static final List<Object>[] pendingKey = newKeys(49);

    private static int cachedBytes;

    private static final int[][] background = new int[41][];

    private static final int[][] backgroundNext = new int[41][];

    private static final int[] syncRegion = new int[4];

    private static long backgroundAt;

    private static GpuFence asyncFence;

    private static GpuBuffer asyncBuffer;

    private static long asyncMask;

    private static int asyncScale;

    private static final int[] asyncX = new int[41];

    private static final int[] asyncY = new int[41];

    private static final int[] asyncRegion = new int[4];

    private static final Canvas canvas = new Canvas();

    private static OverlayWindow slotWindow;

    private static OverlayWindow cursorWindow;

    private static GpuTexture scratch;

    private static GpuTextureView scratchView;

    private static GpuTexture scratchDepth;

    private static GpuTextureView scratchDepthView;

    private static GpuBuffer readback;

    private static int iconPx;

    private static volatile boolean unsupported;

    private static volatile long retryAt;

    private static int transientFailures;

    private static boolean toldUnsupported;

    private static boolean toldPaused;

    private static boolean toldFullscreen;

    private static boolean diagnosed;

    private static final long[] BACKOFF = new long[] {2000000000L, 10000000000L, 60000000000L};

    private static final long REPEAT_LOG_INTERVAL = 60000000000L;

    private static final Map<String, long[]> reported = new HashMap<>();

    private StreamOverlay() {}

    public static boolean usable() {
        if (Platform.isWindows() && !unsupported) {
            long l = retryAt;
            return l == 0L || System.nanoTime() - l >= 0L;
        } else {
            return false;
        }
    }

    public static void retry() {
        unsupported = false;
        retryAt = 0L;
        transientFailures = 0;
    }

    public static boolean recordsHotbar() {
        return hotbarVisible();
    }

    public static void hotbar(int i, int j, int k, ItemStack stack2) {
        if (hotbarVisible()) {
            record(i, j, k, stack2);
        }
    }

    public static void container(int i, int j, int k, ItemStack stack2, boolean bl) {
        record(i, j, k, stack2);
        if (bl) {
            hovered = i;
        }
    }

    public static void carried(ItemStack stack2, int i, int j) {
        record(41, i, j, stack2);
    }

    public static void selection(int i, int j) {
        if (hotbarVisible()) {
            selectionSeen = true;
            selectionX = i;
            selectionY = j;
        }
    }

    public static void tooltip(ItemStack stack2, int i, int j) {
        TextPanels.tooltip(stack2, i, j);
    }

    public static void itemName(ItemStack stack2, int i, int j) {
        TextPanels.itemName(stack2, i, j);
    }

    private static boolean hotbarVisible() {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        return currentScreen == null || currentScreen instanceof ChatScreen;
    }

    private static void record(int i, int j, int k, ItemStack stack2) {
        if (i >= 0 && i < 42 && (i != 41 || !stack2.isEmpty())) {
            stack[i] = stack2;
            guiX[i] = j;
            guiY[i] = k;
            mask |= 1L << i;
        }
    }

    public static boolean hasFakes() {
        return fakeMask != 0L;
    }

    public static void drawFakes() {
        long l = fakeMask;
        int i = fakeScale;
        if (l != 0L && i > 0) {
            try {
                fakeList = ImGui.getBackgroundDrawList();
                for (int j = 0; j < 41; j++) {
                    ItemStack stack2;
                    if ((l >>> j & 1L) != 0L && !(stack2 = NoInvLeakModule.fakeFor(j)).isEmpty()) {
                        int k = guiX[j] * i;
                        int m = guiY[j] * i;
                        ItemIcons.draw(fakeList, stack2, k, m, 16.0F * i, 1.0F);
                        DigitFont.decorations(FAKE_SINK, stack2, k, m, i);
                    }
                }
            } catch (RuntimeException | LinkageError exception) {
                fakeMask = 0L;
                fail(exception, "drawing the stream's fake items");
            }
        }
    }

    public static void endFrame() {
        long l = mask;
        mask = 0L;
        int i = hovered;
        hovered = -1;
        boolean bl = selectionSeen && NoInvLeakModule.hidesSelectedSlot();
        selectionSeen = false;
        if (NoInvLeakModule.overlayWanted() && usable()) {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                Window window = client.getWindow();
                if (window.hasZeroWidthOrHeight()) {
                    fakeMask = 0L;
                    hideAll();
                } else {
                    int scaleFactor = window.getScaleFactor();
                    int j = window.getX();
                    int k = window.getY();
                    boolean opaqueCells = NoInvLeakModule.opaqueCells();
                    l = withoutEmpty(l, opaqueCells && NoInvLeakModule.fillsEmpty());
                    GpuTexture gpuTexture = itemAtlas(client);
                    if (gpuTexture != shownAtlas) {
                        OverlayArt.reset();
                        resetIcons(16 * scaleFactor);
                        shownAtlas = gpuTexture;
                        shownMask = 0L;
                    }
                    fakeMask = l & 2199023255551L;
                    fakeScale = scaleFactor;
                    layers(client, window, l, scaleFactor, j, k, opaqueCells, bl, i);
                    TextPanels.update(window, scaleFactor, j, k);
                }
            } catch (RuntimeException | LinkageError exception) {
                fakeMask = 0L;
                fail(exception, "drawing the overlay");
            }
            TextPanels.frameDone();
        } else {
            fakeMask = 0L;
            if (started()) {
                release();
            }
            TextPanels.frameDone();
        }
    }

    private static void fail(Throwable throwable, String string) {
        boolean bl =
                throwable instanceof OverlayWindow.Unsupported || throwable instanceof LinkageError;
        String string2 =
                throwable.getMessage() == null
                        ? throwable.getClass().getSimpleName()
                        : throwable.getMessage();
        long l = 0L;
        if (bl) {
            unsupported = true;
        } else {
            l = BACKOFF[Math.min(transientFailures++, BACKOFF.length - 1)];
            retryAt = System.nanoTime() + l;
        }
        String string3 = string + "|" + throwable.getClass().getName() + "|" + string2;
        long l2 = System.nanoTime();
        long[] longs = reported.get(string3);
        String string4 =
                bl ? "turned off for this session" : "retrying in " + l / 1000000000L + " s";
        if (longs == null) {
            reported.put(string3, new long[] {l2, 0L});
            LOGGER.warn(
                    "[EFN] No Inv Leak: Stream Only failed while {} ({}); {}. Windows build {}."
                        + " Your items are shown as hidden to you as well until it works again.",
                    string,
                    string2,
                    string4,
                    OverlayWindow.windowsBuild(),
                    throwable);
        } else if (l2 - longs[0] >= 60000000000L) {
            LOGGER.warn(
                    "[EFN] No Inv Leak: Stream Only failed again while {} ({}), {} more time(s)"
                            + " since the last report; {}",
                    string,
                    string2,
                    longs[1] + 1L,
                    string4);
            longs[0] = l2;
            longs[1] = 0L;
        } else {
            longs[1]++;
        }
        if (bl && !toldUnsupported) {
            toldUnsupported = true;
            NoInvLeakModule.streamNotice(
                    "Stream Only unavailable", ": " + string2 + ". Items stay hidden for you too.");
        } else if (!bl && !toldPaused) {
            toldPaused = true;
            NoInvLeakModule.streamNotice(
                    "Stream Only paused", ": " + string2 + ". Retrying automatically.");
        }
        try {
            release();
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.debug("[EFN] No Inv Leak: overlay cleanup failed", (Throwable) exception);
        }
    }

    private static void diagnose(MinecraftClient client, Window window) {
        if (!diagnosed) {
            diagnosed = true;
            transientFailures = 0;
            boolean fullscreen = window.isFullscreen();
            boolean bl =
                    fullscreen
                            && ((BorderlessFullscreen.Switchable) (Object) window)
                                    .efn$isBorderless();
            String string =
                    !fullscreen
                            ? "windowed"
                            : (bl ? "borderless fullscreen" : "exclusive fullscreen");
            String string2;
            try {
                string2 = GL11.glGetString(7936) + " / " + GL11.glGetString(7937);
            } catch (RuntimeException | LinkageError exception) {
                string2 = "unknown";
            }
            LOGGER.info(
                    "[EFN] No Inv Leak: Stream Only overlay active (Windows build {}, {}, window"
                            + " {}x{} at {},{}, framebuffer {}x{}, gui scale {}, {}, overlay {})",
                    OverlayWindow.windowsBuild(),
                    string,
                    window.getWidth(),
                    window.getHeight(),
                    window.getX(),
                    window.getY(),
                    client.getFramebuffer().textureWidth,
                    client.getFramebuffer().textureHeight,
                    window.getScaleFactor(),
                    string2,
                    slotWindow != null ? slotWindow.describe() : "pending");
            if (fullscreen && !bl && !toldFullscreen) {
                toldFullscreen = true;
                NoInvLeakModule.streamNotice(
                        "Stream Only",
                        ": exclusive fullscreen can hide the overlay from you. Turn on Borderless"
                                + " Fullscreen.");
            }
        }
    }

    public static void stop() {
        diagnosed = false;
        if (RenderSystem.isOnRenderThread()) {
            release();
        } else {
            MinecraftClient.getInstance().execute(StreamOverlay::release);
        }
    }

    private static boolean started() {
        return slotWindow != null
                || cursorWindow != null
                || scratch != null
                || TextPanels.started();
    }

    private static void layers(
            MinecraftClient client,
            Window window,
            long l,
            int i,
            int j,
            int k,
            boolean bl,
            boolean bl2,
            int m) {
        long l2 = l & 2199023255551L;
        boolean bl3 = l2 != 0L || bl2;
        if (!bl3 && (l >>> 41 & 1L) == 0L) {
            hideSlotsAndCursor();
        } else {
            int n = 16 * i;
            if (n != iconPx || cachedBytes > 4194304) {
                resetIcons(n);
            }
            if (scratch == null) {
                allocate(n);
            }
            if (slotWindow == null && bl3) {
                slotWindow = new OverlayWindow(window.getHandle());
            }
            if (cursorWindow == null && (l >>> 41 & 1L) != 0L) {
                cursorWindow = new OverlayWindow(window.getHandle());
            }
            boolean bl4 =
                    l2 != shownMask
                            || i != shownScale
                            || bl != shownOpaque
                            || bl2 != shownSelection
                            || bl2
                                    && (selectionX != shownSelectionX
                                            || selectionY != shownSelectionY);
            boolean bl5 = false;
            for (int o = 0; o < 41; o++) {
                if ((l2 >>> o & 1L) != 0L) {
                    if (guiX[o] != shownX[o] || guiY[o] != shownY[o]) {
                        bl4 = true;
                    } else if (stack[o] != shownStack[o]
                            || stack[o].getCount() != shownCount[o]
                            || bar(stack[o]) != shownBar[o]) {
                        bl5 = true;
                    }
                }
            }
            if (bl4) {
                backgroundValid = false;
                dropAsync();
            }
            int icons2 = resolveIcons(l, n);
            boolean bl6 = bl && l2 != 0L && !backgroundValid;
            boolean bl7 = false;
            if (icons2 > 0 || bl6) {
                synchronousRead(l2, i, bl6, icons2);
                if (bl6) {
                    bl7 = true;
                    shownHover = m;
                }
            } else if (bl && l2 != 0L) {
                bl7 = takeAsync(l2, i);
            }
            if (!bl3) {
                if (slotWindow != null) {
                    slotWindow.hide();
                }
                shownMask = 0L;
            } else if (bl4 || bl5 || bl7 || icons2 > 0) {
                composeSlots(l2, i, bl, bl2, j, k);
            } else if (j != shownWindowX || k != shownWindowY) {
                slotWindow.move(j + boxX, k + boxY);
                shownWindowX = j;
                shownWindowY = k;
            }
            if (bl
                    && l2 != 0L
                    && asyncFence == null
                    && backgroundValid
                    && (m != shownHover
                            || hotbarVisible() && System.nanoTime() - backgroundAt >= 33000000L)) {
                requestAsync(l2, i);
                shownHover = m;
            }
            cursor(l, i, j, k);
        }
    }

    private static void hideSlotsAndCursor() {
        if (slotWindow != null) {
            slotWindow.hide();
        }
        if (cursorWindow != null) {
            cursorWindow.hide();
        }
        shownMask = 0L;
        cursorStack = null;
    }

    private static long withoutEmpty(long l, boolean bl) {
        if (bl) {
            return l;
        } else {
            long l2 = l;
            for (int i = 0; i < 41; i++) {
                if ((l2 >>> i & 1L) != 0L && stack[i].isEmpty()) {
                    l2 &= ~(1L << i);
                }
            }
            return l2;
        }
    }

    private static void hideAll() {
        hideSlotsAndCursor();
        TextPanels.hide();
    }

    private static int bar(ItemStack stack2) {
        return stack2.isItemBarVisible()
                ? stack2.getItemBarColor() << 5 | stack2.getItemBarStep()
                : -1;
    }

    private static int cellX(int i, int j) {
        return i % 7 * j;
    }

    private static int cellY(int i, int j) {
        return i / 7 * j;
    }

    private static int resolveIcons(long l, int i) {
        int j = 0;
        for (int k = 0; k < 42; k++) {
            if ((l >>> k & 1L) != 0L) {
                ItemStack stack2 = stack[k];
                if (stack2 != shownStack[k]) {
                    slotIcon[k] = null;
                }
                if (slotIcon[k] == null) {
                    if (!stack2.isEmpty() && look.resolve(stack2)) {
                        int[] icon = icons.get(look.identity);
                        if (icon != null) {
                            slotIcon[k] = icon;
                        } else {
                            int m = j++;
                            try {
                                ItemIcons.render(
                                        look,
                                        scratch,
                                        scratchView,
                                        scratchDepth,
                                        scratchDepthView,
                                        cellX(m, i),
                                        cellY(m, i),
                                        i);
                            } catch (RuntimeException exception) {
                                slotIcon[k] = BLANK;
                                look.clear();
                                String string = "icon|" + stack2.getItem();
                                if (reported.putIfAbsent(string, new long[] {System.nanoTime(), 0L})
                                        == null) {
                                    LOGGER.warn(
                                            "[EFN] No Inv Leak: Stream Only could not draw the icon"
                                                    + " for {}; it stays blank",
                                            stack2.getItem(),
                                            exception);
                                }
                                continue;
                            }
                            pendingSlot[m] = k;
                            pendingKey[m] = Arrays.asList(look.identity.toArray());
                        }
                        look.clear();
                    } else {
                        slotIcon[k] = BLANK;
                    }
                }
            }
        }
        return j;
    }

    private static void resetIcons(int i) {
        icons.clear();
        cachedBytes = 0;
        Arrays.fill((Object[]) slotIcon, null);
        dropAsync();
        backgroundValid = false;
        if (i != iconPx || scratch == null) {
            closeTextures();
            Arrays.fill((Object[]) background, null);
            Arrays.fill((Object[]) backgroundNext, null);
        }
    }

    private static void allocate(int i) {
        int j = 7 * i;
        GpuDevice device = RenderSystem.getDevice();
        scratch = device.createTexture("EFN overlay icons", 15, TextureFormat.RGBA8, j, j, 1, 1);
        scratchView = device.createTextureView(scratch);
        scratchDepth =
                device.createTexture(
                        "EFN overlay icon depth", 8, TextureFormat.DEPTH32, j, j, 1, 1);
        scratchDepthView = device.createTextureView(scratchDepth);
        device.createCommandEncoder().clearColorAndDepthTextures(scratch, 0, scratchDepth, 1.0);
        iconPx = i;
    }

    private static GpuTexture itemAtlas(MinecraftClient client) {
        try {
            return client.getAtlasManager().getAtlasTexture(Atlases.ITEMS).getGlTexture();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void synchronousRead(long l, int i, boolean bl, int j) {
        int k = 16 * i;
        int m = 7 * k;
        long l2 = j > 0 ? (long) m * m * 4L : 0L;
        boolean bl2 = bl && region(l, i, syncRegion);
        long l3 = bl2 ? (long) syncRegion[2] * syncRegion[3] * 4L : 0L;
        long l4 = bl2 ? l2 + frameTextureBytes() : l2;
        long l5 = l2 + l3;
        if (bl) {
            backgroundValid = true;
        }
        if (l4 == 0L) {
            if (bl) {
                unpackBackground(null, 0, syncRegion, false, l, i);
            }
        } else {
            GpuDevice device = RenderSystem.getDevice();
            if (readback == null || readback.size() < l4) {
                if (readback != null) {
                    readback.close();
                }
                readback = device.createBuffer(() -> "EFN overlay readback", 9, l4);
            }
            CommandEncoder commandEncoder = device.createCommandEncoder();
            packDefaults();
            try {
                if (j > 0) {
                    commandEncoder.copyTextureToBuffer(scratch, readback, 0L, NOTHING, 0);
                }
                if (bl2) {
                    copyRegion(commandEncoder, readback, l2, syncRegion);
                }
            } finally {
                packReset();
            }
            try (GpuFence fence = commandEncoder.createFence()) {
                GL11.glFlush();
                if (!fence.awaitCompletion(1000000000L)) {
                    throw new IllegalStateException("overlay read-back timed out");
                }
            }
            try (MappedView mappedView =
                    commandEncoder.mapBuffer(readback.slice(0L, l5), true, false)) {
                ByteBuffer byteBuffer = mappedView.data();
                for (int n = 0; n < j; n++) {
                    int[] ints = unpackIcon(byteBuffer, cellX(n, k), cellY(n, k), m, k);
                    icons.put(pendingKey[n], ints);
                    pendingKey[n] = null;
                    cachedBytes += k * k * 4;
                    slotIcon[pendingSlot[n]] = ints;
                }
                if (bl) {
                    unpackBackground(byteBuffer, (int) l2, syncRegion, bl2, l, i);
                    backgroundAt = System.nanoTime();
                }
            }
        }
    }

    private static void requestAsync(long l, int i) {
        backgroundAt = System.nanoTime();
        if (region(l, i, asyncRegion)) {
            long frameTextureBytes = frameTextureBytes();
            GpuDevice device = RenderSystem.getDevice();
            if (asyncBuffer == null || asyncBuffer.size() < frameTextureBytes) {
                if (asyncBuffer != null) {
                    asyncBuffer.close();
                }
                asyncBuffer =
                        device.createBuffer(() -> "EFN overlay background", 9, frameTextureBytes);
            }
            CommandEncoder commandEncoder = device.createCommandEncoder();
            packDefaults();
            try {
                copyRegion(commandEncoder, asyncBuffer, 0L, asyncRegion);
            } finally {
                packReset();
            }
            asyncFence = commandEncoder.createFence();
            GL11.glFlush();
            asyncMask = l;
            asyncScale = i;
            for (int j = 0; j < 41; j++) {
                asyncX[j] = guiX[j];
                asyncY[j] = guiY[j];
            }
        }
    }

    private static boolean takeAsync(long l, int i) {
        if (asyncFence != null && asyncFence.awaitCompletion(0L)) {
            boolean bl = asyncMask == l && asyncScale == i;
            for (int j = 0; bl && j < 41; j++) {
                if ((l >>> j & 1L) != 0L && (asyncX[j] != guiX[j] || asyncY[j] != guiY[j])) {
                    bl = false;
                }
            }
            boolean bl2 = false;
            if (bl) {
                long l2 = (long) asyncRegion[2] * asyncRegion[3] * 4L;
                CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                try (MappedView mappedView =
                        commandEncoder.mapBuffer(asyncBuffer.slice(0L, l2), true, false)) {
                    bl2 = unpackBackground(mappedView.data(), 0, asyncRegion, true, l, i);
                }
            }
            dropAsync();
            return bl2;
        } else {
            return false;
        }
    }

    private static void dropAsync() {
        if (asyncFence != null) {
            asyncFence.close();
            asyncFence = null;
        }
    }

    private static long frameTextureBytes() {
        GpuTexture colorAttachment =
                MinecraftClient.getInstance().getFramebuffer().getColorAttachment();
        return (long) colorAttachment.getWidth(0) * colorAttachment.getHeight(0) * 4L;
    }

    private static void packDefaults() {
        GlStateManager._pixelStore(3333, 4);
        GlStateManager._pixelStore(3331, 0);
        GlStateManager._pixelStore(3332, 0);
    }

    private static void packReset() {
        GlStateManager._pixelStore(3330, 0);
    }

    private static boolean region(long l, int i, int[] ints) {
        GpuTexture colorAttachment =
                MinecraftClient.getInstance().getFramebuffer().getColorAttachment();
        int j = 16 * i;
        int k = 2147483647;
        int m = 2147483647;
        int n = -2147483648;
        int o = -2147483648;
        for (int p = 0; p < 41; p++) {
            if ((l >>> p & 1L) != 0L) {
                k = Math.min(k, guiX[p] * i);
                m = Math.min(m, guiY[p] * i);
                n = Math.max(n, guiX[p] * i + j);
                o = Math.max(o, guiY[p] * i + j);
            }
        }
        k = Math.max(k, 0);
        m = Math.max(m, 0);
        n = Math.min(n, colorAttachment.getWidth(0));
        o = Math.min(o, colorAttachment.getHeight(0));
        if (n > k && o > m) {
            ints[0] = k;
            ints[1] = m;
            ints[2] = n - k;
            ints[3] = o - m;
            return true;
        } else {
            return false;
        }
    }

    private static void copyRegion(
            CommandEncoder commandEncoder, GpuBuffer gpuBuffer, long l, int[] ints) {
        GpuTexture colorAttachment =
                MinecraftClient.getInstance().getFramebuffer().getColorAttachment();
        commandEncoder.copyTextureToBuffer(
                colorAttachment,
                gpuBuffer,
                l,
                NOTHING,
                0,
                ints[0],
                colorAttachment.getHeight(0) - ints[1] - ints[3],
                ints[2],
                ints[3]);
    }

    private static int[] unpackIcon(ByteBuffer byteBuffer, int i, int j, int k, int m) {
        int[] ints = new int[m * m];
        for (int n = 0; n < m; n++) {
            int o = ((k - 1 - (j + n)) * k + i) * 4;
            for (int p = 0; p < m; p++) {
                int q = o + p * 4;
                int r = byteBuffer.get(q + 3) & 255;
                ints[n * m + p] =
                        r << 24
                                | Math.min(r, byteBuffer.get(q) & 255) << 16
                                | Math.min(r, byteBuffer.get(q + 1) & 255) << 8
                                | Math.min(r, byteBuffer.get(q + 2) & 255);
            }
        }
        return ints;
    }

    private static boolean unpackBackground(
            ByteBuffer byteBuffer, int i, int[] ints, boolean bl, long l, int j) {
        int k = 16 * j;
        int m = ints[0];
        int n = ints[1];
        int o = ints[2];
        int p = ints[3];
        boolean bl2 = false;
        for (int q = 0; q < 41; q++) {
            if ((l >>> q & 1L) != 0L) {
                int r = guiX[q] * j - m;
                int t = guiY[q] * j - n;
                if (bl && r >= 0 && t >= 0 && r + k <= o && t + k <= p) {
                    int[] ints2 = backgroundNext[q];
                    if (ints2 == null || ints2.length != k * k) {
                        ints2 = new int[k * k];
                    }
                    for (int u = 0; u < k; u++) {
                        int v = i + ((p - 1 - (t + u)) * o + r) * 4;
                        for (int w = 0; w < k; w++) {
                            int i2 = v + w * 4;
                            ints2[u * k + w] =
                                    0xFF000000
                                            | (byteBuffer.get(i2) & 255) << 16
                                            | (byteBuffer.get(i2 + 1) & 255) << 8
                                            | byteBuffer.get(i2 + 2) & 255;
                        }
                    }
                    int[] ints3 = background[q];
                    if (ints3 != null && Arrays.equals(ints3, ints2)) {
                        backgroundNext[q] = ints2;
                    } else {
                        bl2 = true;
                        background[q] = ints2;
                        backgroundNext[q] = ints3;
                    }
                } else {
                    if (background[q] != null) {
                        bl2 = true;
                    }
                    background[q] = null;
                }
            }
        }
        return bl2;
    }

    private static void composeSlots(long l, int i, boolean bl, boolean bl2, int j, int k) {
        int m = 16 * i;
        OverlayArt.Image image = bl2 ? OverlayArt.selection() : null;
        boolean bl3 = image != null;
        int n = 2147483647;
        int o = 2147483647;
        int p = -2147483648;
        int q = -2147483648;
        for (int r = 0; r < 41; r++) {
            if ((l >>> r & 1L) != 0L) {
                n = Math.min(n, guiX[r]);
                o = Math.min(o, guiY[r]);
                p = Math.max(p, guiX[r] + 17);
                q = Math.max(q, guiY[r] + 17);
            }
        }
        if (bl3) {
            n = Math.min(n, selectionX);
            o = Math.min(o, selectionY);
            p = Math.max(p, selectionX + image.width());
            q = Math.max(q, selectionY + image.height());
        }
        if (n > p) {
            slotWindow.hide();
            shownMask = 0L;
        } else {
            int t = n * i;
            int u = o * i;
            int v = (p - n) * i;
            int w = (q - o) * i;
            canvas.wrap(slotWindow.pixels(v, w), slotWindow.stride(), v, w);
            if (bl) {
                for (int i2 = 0; i2 < 41; i2++) {
                    if ((l >>> i2 & 1L) != 0L) {
                        int j2 = guiX[i2] * i - t;
                        int k2 = guiY[i2] * i - u;
                        int[] ints = background[i2];
                        if (ints != null) {
                            canvas.copy(ints, m, m, j2, k2);
                        } else {
                            canvas.rect(j2, k2, m, m, -7631989);
                        }
                    }
                }
            }
            if (bl3) {
                canvas.blend(
                        image.pixels(),
                        image.width(),
                        image.height(),
                        selectionX * i - t,
                        selectionY * i - u,
                        i);
            }
            for (int m2 = 0; m2 < 41; m2++) {
                if ((l >>> m2 & 1L) != 0L) {
                    int n2 = guiX[m2] * i - t;
                    int o2 = guiY[m2] * i - u;
                    int[] ints2 = slotIcon[m2];
                    if (ints2 != null && ints2 != BLANK) {
                        canvas.blend(ints2, m, m, n2, o2, 1);
                    }
                    if (!stack[m2].isEmpty()) {
                        DigitFont.decorations(canvas, stack[m2], n2, o2, i);
                    }
                }
            }
            slotWindow.present(j + t, k + u, v, w);
            diagnose(MinecraftClient.getInstance(), MinecraftClient.getInstance().getWindow());
            for (int p2 = 0; p2 < 41; p2++) {
                if ((l >>> p2 & 1L) != 0L) {
                    shownStack[p2] = stack[p2];
                    shownX[p2] = guiX[p2];
                    shownY[p2] = guiY[p2];
                    shownCount[p2] = stack[p2].getCount();
                    shownBar[p2] = bar(stack[p2]);
                }
            }
            shownMask = l;
            shownScale = i;
            shownOpaque = bl;
            shownSelection = bl2;
            shownSelectionX = selectionX;
            shownSelectionY = selectionY;
            shownWindowX = j;
            shownWindowY = k;
            boxX = t;
            boxY = u;
        }
    }

    private static void cursor(long l, int i, int j, int k) {
        if ((l >>> 41 & 1L) == 0L) {
            if (cursorWindow != null) {
                cursorWindow.hide();
            }
            cursorStack = null;
        } else {
            ItemStack stack2 = stack[41];
            int m = 16 * i;
            int n = j + guiX[41] * i;
            int o = k + guiY[41] * i;
            boolean bl =
                    stack2 == cursorStack
                            && stack2.getCount() == cursorCount
                            && bar(stack2) == cursorBar
                            && i == cursorScale;
            if (bl) {
                if (n != cursorX || o != cursorY) {
                    cursorWindow.move(n, o);
                }
            } else {
                int p = m + i;
                canvas.wrap(cursorWindow.pixels(p, p), cursorWindow.stride(), p, p);
                int[] ints = slotIcon[41];
                if (ints != null && ints != BLANK) {
                    canvas.blend(ints, m, m, 0, 0, 1);
                }
                DigitFont.decorations(canvas, stack2, 0, 0, i);
                cursorWindow.present(n, o, p, p);
                cursorStack = stack2;
                cursorCount = stack2.getCount();
                cursorBar = bar(stack2);
                cursorScale = i;
            }
            cursorX = n;
            cursorY = o;
            shownStack[41] = stack2;
        }
    }

    private static void closeTextures() {
        if (scratchView != null) {
            scratchView.close();
        }
        if (scratch != null) {
            scratch.close();
        }
        if (scratchDepthView != null) {
            scratchDepthView.close();
        }
        if (scratchDepth != null) {
            scratchDepth.close();
        }
        scratchView = null;
        scratch = null;
        scratchDepthView = null;
        scratchDepth = null;
        iconPx = 0;
    }

    private static void release() {
        dropAsync();
        if (slotWindow != null) {
            slotWindow.destroy();
            slotWindow = null;
        }
        if (cursorWindow != null) {
            cursorWindow.destroy();
            cursorWindow = null;
        }
        TextPanels.release();
        closeTextures();
        if (readback != null) {
            readback.close();
            readback = null;
        }
        if (asyncBuffer != null) {
            asyncBuffer.close();
            asyncBuffer = null;
        }
        icons.clear();
        cachedBytes = 0;
        Arrays.fill((Object[]) slotIcon, null);
        Arrays.fill(pendingKey, null);
        Arrays.fill((Object[]) background, null);
        Arrays.fill((Object[]) backgroundNext, null);
        Arrays.fill(stack, null);
        Arrays.fill(shownStack, null);
        mask = 0L;
        fakeMask = 0L;
        shownMask = 0L;
        shownAtlas = null;
        shownHover = -1;
        cursorStack = null;
        backgroundValid = false;
    }

    private static List<Object>[] newKeys(int i) {
        return new List[i];
    }
}
