package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
final class GifAnimation {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final int MAX_SIDE = 1024;

    private static final int MAX_FRAMES = 300;

    private static final long MAX_PIXELS = 33554432L;

    private static final int OPTIMIZED_SIDE = 768;

    private static final long OPTIMIZED_PIXELS = 8388608L;

    private static final int POSTER_SIDE = 256;

    private static final int QUEUE_FRAMES = 8;

    private static final long UPLOAD_BUDGET_NANOS = 2000000L;

    private static long frameUploadNanos;

    private static final ExecutorService POSTERS =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "EFN GIF posters");
                        thread.setDaemon(true);
                        return thread;
                    });

    private final String name;

    private final boolean poster;

    private final boolean optimized;

    private final BlockingQueue<GifDecoder.Frame> queue = new ArrayBlockingQueue<>(8);

    private final List<ThemeImages.Texture> textures = new ArrayList<>();

    private final int[] ends;

    private volatile boolean decoded;

    private volatile boolean cancelled;

    private volatile int decodedFramePixels;

    private long startNanos = -1L;

    private GifAnimation(String name2, boolean poster2, boolean optimized2) {
        this.name = name2;
        this.poster = poster2;
        this.optimized = optimized2;
        this.ends = new int[poster2 ? 1 : 300];
    }

    static GifAnimation load(Path path, boolean bl, boolean bl2) {
        GifAnimation gifAnimation = new GifAnimation(path.getFileName().toString(), bl, bl2);
        if (bl) {
            POSTERS.execute(() -> gifAnimation.decode(path));
        } else {
            Thread.ofPlatform()
                    .daemon()
                    .name("EFN GIF decoder")
                    .start(() -> gifAnimation.decode(path));
        }
        return gifAnimation;
    }

    static void beginFrame(boolean bl) {
        frameUploadNanos = 0L;
    }

    long estimatedBytes() {
        long l = (long) this.queue.size() * this.decodedFramePixels * 4L;
        for (ThemeImages.Texture texture : this.textures) {
            l += (long) texture.width() * texture.height() * 16L / 3L;
        }
        return l;
    }

    ThemeImages.Texture current() {
        boolean decoded2 = this.decoded;
        this.upload();
        int i = this.textures.size();
        if (i == 0) {
            return null;
        } else {
            if (this.startNanos < 0L) {
                if (!decoded2 || !this.queue.isEmpty()) {
                    return this.textures.get(0);
                }
                this.startNanos = System.nanoTime();
            }
            long l = (System.nanoTime() - this.startNanos) / 1000000L % this.ends[i - 1];
            int j = 0;
            int k = i - 1;
            while (j < k) {
                int m = j + k >>> 1;
                if (this.ends[m] > l) {
                    k = m;
                } else {
                    j = m + 1;
                }
            }
            return this.textures.get(j);
        }
    }

    boolean failed() {
        return this.decoded && this.queue.isEmpty() && this.textures.isEmpty();
    }

    void free() {
        this.cancelled = true;
        this.queue.clear();
        for (ThemeImages.Texture texture : this.textures) {
            GL11.glDeleteTextures(texture.id());
        }
        this.textures.clear();
    }

    private void decode(Path path) {
        try {
            if (!this.cancelled) {
                GifDecoder gifDecoder = GifDecoder.open(Files.readAllBytes(path));
                int width = gifDecoder.width();
                int height = gifDecoder.height();
                int i = Math.min(gifDecoder.frameCount(), this.ends.length);
                int j = this.poster ? 256 : (this.optimized ? 768 : 1024);
                long l = this.optimized ? 8388608L : 33554432L;
                double d = Math.min(1.0, (double) j / Math.max(width, height));
                if (!this.poster) {
                    d = Math.min(d, Math.sqrt(l / ((double) i * width * height)));
                }
                int k = Math.max(1, (int) Math.floor(width * d));
                int m = Math.max(1, (int) Math.floor(height * d));
                this.decodedFramePixels = k * m;
                if (!this.poster && i < gifDecoder.frameCount()) {
                    LOGGER.info(
                            "[EFN] {} has {} frames; only the first {} will play",
                            this.name,
                            gifDecoder.frameCount(),
                            i);
                }
                if (!this.poster && k < width) {
                    LOGGER.info(
                            "[EFN] {} ({}x{}, {} frames) plays at {}x{} to keep its memory use"
                                    + " down",
                            this.name,
                            width,
                            height,
                            i,
                            k,
                            m);
                }
                gifDecoder.decode(k, m, i, this::offer);
                return;
            }
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("[EFN] Failed to load {}", this.name, exception);
            return;
        } finally {
            this.decoded = true;
        }
    }

    private boolean offer(GifDecoder.Frame frame) {
        int[] ints = frame.argb();
        for (int i = 0; i < ints.length; i++) {
            int j = ints[i];
            ints[i] = j & -16711936 | j >>> 16 & 0xFF | (j & 0xFF) << 16;
        }
        try {
            while (!this.cancelled) {
                if (this.queue.offer(frame, 50L, TimeUnit.MILLISECONDS)) {
                    return true;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private void upload() {
        if (!this.optimized || frameUploadNanos < 2000000L) {
            long l = System.nanoTime();
            GifDecoder.Frame frame;
            try {
                while (this.textures.size() < this.ends.length
                        && (frame = this.queue.poll()) != null) {
                    ByteBuffer byteBuffer = MemoryUtil.memAlloc(frame.argb().length * 4);
                    try {
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(frame.argb());
                        this.textures.add(
                                ThemeImages.fromPixels(
                                        byteBuffer, frame.width(), frame.height(), false));
                    } finally {
                        MemoryUtil.memFree(byteBuffer);
                    }
                    int i = this.textures.size();
                    this.ends[i - 1] = (i > 1 ? this.ends[i - 2] : 0) + frame.delayMs();
                    long l2 = System.nanoTime() - l;
                    if (l2 + (this.optimized ? frameUploadNanos : 0L) >= 2000000L) {
                        break;
                    }
                }
            } finally {
                if (this.optimized) {
                    frameUploadNanos = frameUploadNanos + (System.nanoTime() - l);
                }
            }
        }
    }
}
