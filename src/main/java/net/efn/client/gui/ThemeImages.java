/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.gui;

import imgui.EfnAndroidNative;

import net.efn.client.config.HudTheme;
import net.efn.client.config.Surface;
import net.efn.client.config.ThemeStore;
import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Environment(value = EnvType.CLIENT)
public final class ThemeImages {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final ThemeImages INSTANCE = new ThemeImages();

    private static final int MAX_SIZE = 8192;

    private static final long RESCAN_MS = 2000L;

    private static final int SWEEP_TICKS = 20;

    private static final int OPTIMIZED_SIDE = 2048;

    private static final int PREVIEW_SIDE = 384;

    private static final long CACHE_BYTES = 0x4000000L;

    private final Map<String, Texture> textures = new HashMap<String, Texture>();

    private final Map<String, Texture> previews = new HashMap<String, Texture>();

    private final Map<String, GifAnimation> animations = new HashMap<String, GifAnimation>();

    private final Map<String, GifAnimation> posters = new HashMap<String, GifAnimation>();

    private final Set<String> failed = new HashSet<String>();

    private final List<Runnable> pendingFrees = new ArrayList<Runnable>();

    private final List<Runnable> profileListeners = new ArrayList<Runnable>();

    private final Map<String, Long> lastUsed = new HashMap<String, Long>();

    private long frame;

    private boolean optimized;

    private List<String> files = List.of();

    private long lastScan;

    private boolean listingChanged;

    private int ticks;

    private volatile Path pendingImport;

    private volatile boolean dialogOpen;

    private ThemeImages() {
        ClientTickEvents.END_CLIENT_TICK.register(
                minecraftClient -> {
                    if (++this.ticks % 20 == 0) {
                        this.sweep();
                    }
                });
    }

    public static ThemeImages get() {
        return INSTANCE;
    }

    public void beginFrame() {
        this.flushFrees();
        boolean optimizationsEnabled = PerformanceModule.optimizationsEnabled();
        if (this.optimized != optimizationsEnabled) {
            this.optimized = optimizationsEnabled;
            this.retireAll();
            this.profileListeners.forEach(Runnable::run);
            this.flushFrees();
            this.failed.clear();
        }
        ++this.frame;
        GifAnimation.beginFrame(this.optimized);
    }

    public void endFrame() {
        if (!this.optimized) {
            return;
        }
        long cacheBytes = this.cacheBytes();
        if (cacheBytes <= 0x4000000L) {
            return;
        }
        ArrayList<Map.Entry<String, Long>> list =
                new ArrayList<Map.Entry<String, Long>>(this.lastUsed.entrySet());
        list.sort(Comparator.comparingLong(Map.Entry::getValue));
        for (Map.Entry<String, Long> entry : list) {
            if (cacheBytes <= 0x4000000L) break;
            if (entry.getValue() == this.frame) continue;
            cacheBytes -= this.retire(entry.getKey());
        }
    }

    public boolean optimizedImages() {
        return this.optimized;
    }

    public void onImageProfileChange(Runnable runnable) {
        this.profileListeners.add(runnable);
    }

    private void markUsed(String string, String string2) {
        if (this.optimized) {
            this.lastUsed.put(string + string2, this.frame);
        }
    }

    public void retireTexture(Texture texture) {
        if (texture != null) {
            this.pendingFrees.add(() -> GL11.glDeleteTextures(texture.id()));
        }
    }

    private void flushFrees() {
        if (this.pendingFrees.isEmpty()) {
            return;
        }
        ArrayList<Runnable> list = new ArrayList<Runnable>(this.pendingFrees);
        this.pendingFrees.clear();
        list.forEach(Runnable::run);
    }

    private void retireAll() {
        this.textures.values().forEach(this::retireTexture);
        this.previews.values().forEach(this::retireTexture);
        this.animations.values().forEach(gifAnimation -> this.pendingFrees.add(gifAnimation::free));
        this.posters.values().forEach(gifAnimation -> this.pendingFrees.add(gifAnimation::free));
        this.textures.clear();
        this.previews.clear();
        this.animations.clear();
        this.posters.clear();
        this.lastUsed.clear();
    }

    private static long textureBytes(Texture texture) {
        return texture == null ? 0L : (long) texture.width() * (long) texture.height() * 16L / 3L;
    }

    private long cacheBytes() {
        long l = 0L;
        for (Texture texture : this.textures.values()) {
            l += ThemeImages.textureBytes(texture);
        }
        for (Texture preview : this.previews.values()) {
            l += ThemeImages.textureBytes(preview);
        }
        for (GifAnimation animation : this.animations.values()) {
            l += animation.estimatedBytes();
        }
        for (GifAnimation poster : this.posters.values()) {
            l += poster.estimatedBytes();
        }
        return l;
    }

    private long retire(String string) {
        String string2 = string.substring(2);
        this.lastUsed.remove(string);
        if (string.startsWith("t:") || string.startsWith("p:")) {
            Texture texture =
                    (string.startsWith("t:") ? this.textures : this.previews).remove(string2);
            this.retireTexture(texture);
            return ThemeImages.textureBytes(texture);
        }
        GifAnimation gifAnimation =
                (string.startsWith("a:") ? this.animations : this.posters).remove(string2);
        if (gifAnimation == null) {
            return 0L;
        }
        long estimatedBytes = gifAnimation.estimatedBytes();
        this.pendingFrees.add(gifAnimation::free);
        return estimatedBytes;
    }

    public static boolean isImage(Path path) {
        String string = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return string.endsWith(".png")
                || string.endsWith(".jpg")
                || string.endsWith(".jpeg")
                || ThemeImages.isGif(string);
    }

    public static boolean isGif(String string) {
        return string.toLowerCase(Locale.ROOT).endsWith(".gif");
    }

    public List<String> files() {
        long l = System.currentTimeMillis();
        if (l - this.lastScan > 2000L) {
            this.lastScan = l;
            List<String> list = ThemeImages.scan();
            if (!list.equals(this.files)) {
                this.files = list;
                this.listingChanged = true;
            }
        }
        return this.files;
    }

    public Texture texture(String string) {
        if (string == null || string.isBlank() || this.failed.contains(string)) {
            return null;
        }
        if (ThemeImages.isGif(string)) {
            return this.frame(this.animations, string, false);
        }
        this.markUsed("t:", string);
        Texture texture = this.textures.get(string);
        if (texture == null) {
            texture =
                    ThemeImages.upload(
                            ThemeStore.backgroundsDir().resolve(string),
                            this.optimized ? 2048 : 8192);
            if (texture == null) {
                this.failed.add(string);
            } else {
                this.textures.put(string, texture);
            }
        }
        return texture;
    }

    public Texture preview(String string) {
        if (string == null || string.isBlank() || this.failed.contains(string)) {
            return null;
        }
        if (!ThemeImages.isGif(string)) {
            if (!this.optimized) {
                return this.texture(string);
            }
            this.markUsed("p:", string);
            Texture preview = this.previews.get(string);
            if (preview == null) {
                preview = ThemeImages.upload(ThemeStore.backgroundsDir().resolve(string), 384);
                if (preview == null) {
                    this.failed.add(string);
                } else {
                    this.previews.put(string, preview);
                }
            }
            return preview;
        }
        GifAnimation animation = this.animations.get(string);
        if (animation != null) {
            this.markUsed("a:", string);
        }
        Texture texture = animation != null ? animation.current() : null;
        return texture != null ? texture : this.frame(this.posters, string, true);
    }

    public boolean isBroken(String string) {
        return this.failed.contains(string);
    }

    public String importFile(Path path) {
        if (path == null
                || !Files.isRegularFile(path, new LinkOption[0])
                || !ThemeImages.isImage(path)) {
            return null;
        }
        try {
            Path path2 = ThemeStore.backgroundsDir();
            Files.createDirectories(path2, new FileAttribute[0]);
            if (path2.toAbsolutePath()
                    .normalize()
                    .equals(path.toAbsolutePath().normalize().getParent())) {
                return path.getFileName().toString();
            }
            String uniqueName = ThemeImages.uniqueName(path2, path.getFileName().toString());
            Files.copy(path, path2.resolve(uniqueName), new CopyOption[0]);
            this.lastScan = 0L;
            return uniqueName;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn(
                    "[EFN] Failed to import header image {}", (Object) path, (Object) exception);
            return null;
        }
    }

    public boolean delete(String string) {
        GifAnimation poster;
        if (string == null || !this.files.contains(string)) {
            return false;
        }
        try {
            Files.deleteIfExists(ThemeStore.backgroundsDir().resolve(string));
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn(
                    "[EFN] Failed to delete header image {}", (Object) string, (Object) exception);
            return false;
        }
        Texture texture = this.textures.remove(string);
        this.retireTexture(texture);
        this.retireTexture(this.previews.remove(string));
        GifAnimation animation = this.animations.remove(string);
        if (animation != null) {
            this.pendingFrees.add(animation::free);
        }
        if ((poster = this.posters.remove(string)) != null) {
            this.pendingFrees.add(poster::free);
        }
        for (String string2 : List.of("t:", "p:", "a:", "g:")) {
            this.lastUsed.remove(string2 + string);
        }
        this.failed.remove(string);
        ArrayList<Surface> list = new ArrayList<Surface>(ThemeStore.get().surfaces());
        list.addAll(HudTheme.get().surfaces());
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            Surface surface = (Surface) iterator.next();
            if (!string.equals(surface.image.get())) continue;
            surface.image.set("");
            if (!surface.isImage()) continue;
            surface.background.set("None");
        }
        this.lastScan = 0L;
        return true;
    }

    public void browseAsync() {
        if (EfnAndroidNative.isAndroidRuntime()) {
            AndroidSystemPicker.open();
            return;
        }
        if (this.dialogOpen) {
            return;
        }
        this.dialogOpen = true;
        Thread thread =
                new Thread(
                        () -> {
                            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                                PointerBuffer pointerBuffer = memoryStack.mallocPointer(4);
                                pointerBuffer
                                        .put(memoryStack.UTF8("*.png"))
                                        .put(memoryStack.UTF8("*.jpg"))
                                        .put(memoryStack.UTF8("*.jpeg"))
                                        .put(memoryStack.UTF8("*.gif"))
                                        .flip();
                                String tinyfd_openFileDialog =
                                        TinyFileDialogs.tinyfd_openFileDialog(
                                                "Choose a header image",
                                                null,
                                                pointerBuffer,
                                                "Images (png, jpg, gif)",
                                                false);
                                if (tinyfd_openFileDialog != null) {
                                    this.pendingImport =
                                            Path.of(tinyfd_openFileDialog, new String[0]);
                                }
                            } catch (Throwable exception) {
                                LOGGER.warn("[EFN] File dialog failed", exception);
                            } finally {
                                this.dialogOpen = false;
                            }
                        },
                        "EFN theme file dialog");
        thread.setDaemon(true);
        thread.start();
    }

    public Path takePendingImport() {
        if (EfnAndroidNative.isAndroidRuntime()) {
            return AndroidSystemPicker.takeSelection();
        }
        Path pendingImport2 = this.pendingImport;
        this.pendingImport = null;
        return pendingImport2;
    }

    public void openFolder() {
        this.browseAsync();
    }

    private Texture frame(Map<String, GifAnimation> map, String string, boolean bl) {
        this.markUsed(bl ? "g:" : "a:", string);
        GifAnimation gifAnimation =
                map.computeIfAbsent(
                        string,
                        value ->
                                GifAnimation.load(
                                        ThemeStore.backgroundsDir().resolve((String) value),
                                        bl,
                                        this.optimized));
        Texture gifAnimation2 = gifAnimation.current();
        if (gifAnimation2 == null && gifAnimation.failed()) {
            map.remove(string);
            this.lastUsed.remove((bl ? "g:" : "a:") + string);
            this.failed.add(string);
        }
        return gifAnimation2;
    }

    private void sweep() {
        HashSet<String> set = new HashSet<String>();
        for (Surface surface : ThemeStore.get().surfaces()) {
            if (!surface.isImage()) continue;
            set.add(surface.image.get());
        }
        for (Surface surface2 : HudTheme.get().surfaces()) {
            if (!surface2.isImage()) continue;
            set.add(surface2.image.get());
        }
        this.freeAllBut(this.animations, set, "a:");
        if (!this.listingChanged) {
            return;
        }
        this.listingChanged = false;
        HashSet<String> set2 = new HashSet<String>(this.files);
        this.freeAllBut(this.animations, (Set<String>) set2, "a:");
        this.freeAllBut(this.posters, (Set<String>) set2, "g:");
        for (String string : new ArrayList<String>(this.textures.keySet())) {
            if (set2.contains(string)) continue;
            this.retire("t:" + string);
        }
        for (String string2 : new ArrayList<String>(this.previews.keySet())) {
            if (set2.contains(string2)) continue;
            this.retire("p:" + string2);
        }
        this.failed.retainAll(set2);
    }

    private void freeAllBut(Map<String, GifAnimation> map, Set<String> set, String string) {
        map.entrySet()
                .removeIf(
                        entry -> {
                            if (set.contains(entry.getKey())) {
                                return false;
                            }
                            this.pendingFrees.add(((GifAnimation) entry.getValue())::free);
                            this.lastUsed.remove(string + (String) entry.getKey());
                            return true;
                        });
    }

    private static List<String> scan() {
        Path path2 = ThemeStore.backgroundsDir();
        if (!Files.isDirectory(path2, new LinkOption[0])) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(path2)) {
            return stream.filter(path -> Files.isRegularFile(path, new LinkOption[0]))
                    .filter(ThemeImages::isImage)
                    .map(path -> path.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static String uniqueName(Path path, String string) {
        String string2 = string.replaceAll("[^A-Za-z0-9._-]", "_");
        int i = string2.lastIndexOf(46);
        String string3 = i > 0 ? string2.substring(0, i) : string2;
        String string4 = i > 0 ? string2.substring(i) : "";
        Object object = string2;
        int j = 2;
        while (Files.exists(path.resolve((String) object), new LinkOption[0])) {
            object = string3 + "-" + j + string4;
            ++j;
        }
        return (String) object;
    }

    private static Texture upload(Path path, int i) {
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            return null;
        }
        try {
            return ThemeImages.decode(
                    Files.readAllBytes(path), path.getFileName().toString(), false, i);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[EFN] Failed to load {}", (Object) path.getFileName(), (Object) exception);
            return null;
        }
    }

    static Texture decode(byte[] bytes, String string, boolean bl) {
        return ThemeImages.decode(bytes, string, bl, 8192);
    }

    private static ThemeImages.Texture decode(byte[] bytes, String string, boolean bl, int i) {
        ByteBuffer byteBuffer = null;
        ByteBuffer byteBuffer2 = null;
        Object object;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            byteBuffer = MemoryUtil.memAlloc(bytes.length);
            byteBuffer.put(bytes).flip();
            IntBuffer intBuffer = memoryStack.mallocInt(1);
            IntBuffer intBuffer2 = memoryStack.mallocInt(1);
            IntBuffer intBuffer3 = memoryStack.mallocInt(1);
            byteBuffer2 =
                    STBImage.stbi_load_from_memory(
                            byteBuffer, intBuffer, intBuffer2, intBuffer3, 4);
            if (byteBuffer2 == null) {
                LOGGER.warn("[EFN] Can't decode {}: {}", string, STBImage.stbi_failure_reason());
                return null;
            }
            int j = intBuffer.get(0);
            int k = intBuffer2.get(0);
            if (j <= 8192 && k <= 8192) {
                int[] ints = ImageDownsampler.dimensions(j, k, i);
                if (ints[0] == j && ints[1] == k) {
                    return fromPixels(byteBuffer2, j, k, bl);
                }
                ByteBuffer byteBuffer3 = MemoryUtil.memAlloc(ints[0] * ints[1] * 4);
                try {
                    ImageDownsampler.rgba(byteBuffer2, j, k, byteBuffer3, ints[0], ints[1]);
                    return fromPixels(byteBuffer3, ints[0], ints[1], bl);
                } finally {
                    MemoryUtil.memFree(byteBuffer3);
                }
            }
            LOGGER.warn("[EFN] {} is larger than {}px", string, 8192);
            object = null;
        } finally {
            if (byteBuffer2 != null) {
                STBImage.stbi_image_free(byteBuffer2);
            }
            if (byteBuffer != null) {
                MemoryUtil.memFree(byteBuffer);
            }
        }
        return (ThemeImages.Texture) object;
    }

    public static Texture fromPixels(ByteBuffer byteBuffer, int i, int j, boolean bl) {
        return new Texture(ThemeImages.createTexture(byteBuffer, i, j, bl), i, j);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static int createTexture(ByteBuffer byteBuffer, int i, int j, boolean bl) {
        int glGetInteger = GL11.glGetInteger(32873);
        int glGetInteger2 = GL11.glGetInteger(35055);
        int glGetInteger3 = GL11.glGetInteger(3317);
        int glGetInteger4 = GL11.glGetInteger(3314);
        int glGetInteger5 = GL11.glGetInteger(3316);
        int glGetInteger6 = GL11.glGetInteger(3315);
        try {
            int k;
            GL15.glBindBuffer(35052, 0);
            GL11.glPixelStorei(3317, 4);
            GL11.glPixelStorei(3314, 0);
            GL11.glPixelStorei(3316, 0);
            GL11.glPixelStorei(3315, 0);
            int glGenTextures = GL11.glGenTextures();
            GL11.glBindTexture(3553, glGenTextures);
            GL11.glTexParameteri(3553, 10241, bl ? 9728 : 9987);
            GL11.glTexParameteri(3553, 10240, bl ? 9728 : 9729);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            GL11.glTexImage2D(3553, 0, 32856, i, j, 0, 6408, 5121, byteBuffer);
            if (!bl) {
                GL30.glGenerateMipmap(3553);
            }
            int m = k = glGenTextures;
            return m;
        } finally {
            GL11.glBindTexture(3553, glGetInteger);
            GL15.glBindBuffer(35052, glGetInteger2);
            GL11.glPixelStorei(3317, glGetInteger3);
            GL11.glPixelStorei(3314, glGetInteger4);
            GL11.glPixelStorei(3316, glGetInteger5);
            GL11.glPixelStorei(3315, glGetInteger6);
        }
    }

    @Environment(value = EnvType.CLIENT)
    public record Texture(int id, int width, int height) {}
}
