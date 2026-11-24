package net.efn.client.util.spotify;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Environment(EnvType.CLIENT)
public final class LyricsService implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String MOD_ID = "efn";

    private static final String HOMEPAGE = "https://efn.lol";

    private static final URI LRCLIB = URI.create("https://lrclib.net/");

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8L);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(12L);

    private static final int MAX_BODY_BYTES = 4194304;

    private static final long REQUEST_GAP_MS = 300L;

    private static final long MAX_INLINE_WAIT_MS = 3000L;

    private static final int MAX_ATTEMPTS = 3;

    private static final long DEFAULT_RETRY_AFTER_MS = 30000L;

    private static final long MAX_RETRY_AFTER_MS = TimeUnit.HOURS.toMillis(1L);

    private static final int FOUND_TTL_DAYS = 30;

    private static final int NOT_FOUND_TTL_HOURS = 12;

    private static final long FOUND_TTL_MS = TimeUnit.DAYS.toMillis(30L);

    private static final long NOT_FOUND_TTL_MS = TimeUnit.HOURS.toMillis(12L);

    private static final long FAILURE_RETRY_MS = 60000L;

    private static final int MEMORY_ENTRIES = 64;

    private static final int DISK_MAX_FILES = 400;

    private static final long DISK_MAX_BYTES = 6291456L;

    private static final int WRITES_PER_PRUNE = 25;

    private static final long IDLE_THREAD_MS = 30000L;

    private final Object lock = new Object();

    private final Map<String, LyricsService.Memo> memory =
            new LinkedHashMap<String, LyricsService.Memo>(128, 0.75F, true) {

                @Override
                protected boolean removeEldestEntry(Entry<String, LyricsService.Memo> entry) {
                    return this.size() > 64;
                }
            };

    private final Set<String> pending = new HashSet<>();

    private final ThreadPoolExecutor worker;

    private final LyricsDiskCache disk;

    private final String userAgent;

    private volatile String wanted;

    private volatile LyricsService.KeyMemo lastKey;

    private volatile boolean closed;

    private volatile boolean online = true;

    private volatile int mode;

    private volatile long busyUntil;

    private volatile HttpClient http;

    private volatile ExecutorService httpExecutor;

    private long nextRequestAt;

    private int writesSincePrune = 25;

    public LyricsService() {
        Path path = defaultCacheDir();
        this.disk = path == null ? null : new LyricsDiskCache(path, 400, 6291456L);
        this.userAgent = defaultUserAgent();
        this.worker =
                new ThreadPoolExecutor(
                        1,
                        1,
                        30000L,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(),
                        daemonThreads("EFN-Lyrics"));
        this.worker.allowCoreThreadTimeOut(true);
    }

    public Lyrics lookup(LyricsQuery lyricsQuery) {
        if (lyricsQuery != null && lyricsQuery.searchable()) {
            String key2;
            this.wanted = key2 = this.keyOf(lyricsQuery);
            long l = System.currentTimeMillis();
            Object lock2 = this.lock;
            synchronized (lock2) {
                LyricsService.Memo memo = this.memory.get(key2);
                if (memo != null && l < memo.freshUntil()) {
                    return memo.lyrics();
                } else {
                    if (!this.closed && this.pending.add(key2)) {
                        try {
                            this.worker.execute(() -> this.resolve(key2, lyricsQuery));
                        } catch (RejectedExecutionException exception) {
                            this.pending.remove(key2);
                        }
                    }
                    if (memo != null) {
                        return memo.lyrics();
                    } else {
                        return this.closed ? Lyrics.UNAVAILABLE : Lyrics.LOADING;
                    }
                }
            }
        } else {
            return Lyrics.NOT_FOUND;
        }
    }

    public void setOnline(boolean online2) {
        if (this.online != online2) {
            this.online = online2;
            Object lock2 = this.lock;
            synchronized (lock2) {
                this.mode++;
                this.memory.clear();
            }
        }
    }

    @Override
    public void close() {
        this.closed = true;
        this.worker.shutdownNow();
        Object lock2 = this.lock;
        synchronized (lock2) {
            this.pending.clear();
        }
        HttpClient http2 = this.http;
        if (http2 != null) {
            http2.shutdownNow();
        }
        ExecutorService httpExecutor2 = this.httpExecutor;
        if (this.httpExecutor != null) {
            httpExecutor2.shutdownNow();
        }
    }

    private void resolve(String string, LyricsQuery lyricsQuery) {
        LyricsDiskCache.Entry entry = null;
        int mode2 = this.mode;
        try {
            if (this.closed || !string.equals(this.wanted)) {
                return;
            }
            entry = this.disk == null ? null : this.disk.read(string);
            if (entry != null) {
                long l = entry.fetchedAt() + (entry.found() ? FOUND_TTL_MS : NOT_FOUND_TTL_MS);
                if (System.currentTimeMillis() < l) {
                    this.remember(string, Lyrics.of(entry.track()), l);
                    return;
                }
            }
            if (this.online) {
                LyricsService.Found found = this.fetch(lyricsQuery);
                long l2 = System.currentTimeMillis();
                Lyrics lyrics2 = Lyrics.of(found.track());
                if (!found.complete()) {
                    this.remember(string, lyrics2, this.retryAt());
                    return;
                }
                LrclibTrack lrclibTrack =
                        lyrics2.status() == Lyrics.Status.NOT_FOUND ? null : found.track();
                this.remember(
                        string,
                        lyrics2,
                        l2 + (lrclibTrack == null ? NOT_FOUND_TTL_MS : FOUND_TTL_MS));
                if (this.disk != null && !this.closed) {
                    this.disk.write(string, lrclibTrack, l2);
                    if (++this.writesSincePrune >= 25) {
                        this.writesSincePrune = 0;
                        this.disk.prune();
                        return;
                    }
                }
                return;
            }
            Lyrics lyrics3 = entry != null ? Lyrics.of(entry.track()) : Lyrics.NOT_FOUND;
            Object lock2 = this.lock;
            synchronized (lock2) {
                if (this.mode == mode2) {
                    this.memory.put(
                            string,
                            new LyricsService.Memo(lyrics3, System.currentTimeMillis() + 60000L));
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return;
        } catch (IOException exception2) {
            LOGGER.debug(
                    "[EFN] Lyrics lookup for \"{}\" failed: {}",
                    lyricsQuery.title(),
                    exception2.toString());
            this.remember(
                    string,
                    entry != null ? Lyrics.of(entry.track()) : Lyrics.UNAVAILABLE,
                    this.retryAt());
            return;
        } catch (RuntimeException exception3) {
            LOGGER.debug(
                    "[EFN] Lyrics lookup for \"" + lyricsQuery.title() + "\" failed",
                    (Throwable) exception3);
            this.remember(string, Lyrics.UNAVAILABLE, this.retryAt());
            return;
        } finally {
            Object lock3 = this.lock;
            synchronized (lock3) {
                this.pending.remove(string);
            }
        }
    }

    private LyricsService.Found fetch(LyricsQuery lyricsQuery)
            throws IOException, InterruptedException {
        LrclibTrack lrclibTrack = this.get(lyricsQuery, true);
        if (lrclibTrack == null && !lyricsQuery.album().isEmpty()) {
            lrclibTrack = this.get(lyricsQuery, false);
        }
        if (lrclibTrack == null
                || !lrclibTrack.hasSynced()
                        && (!lrclibTrack.instrumental() || lrclibTrack.hasPlain())) {
            LrclibTrack lrclibTrack2 =
                    lrclibTrack != null && lrclibTrack.usable() ? lrclibTrack : null;
            try {
                for (String[] strings : LyricsMatcher.searchTerms(lyricsQuery)) {
                    LrclibTrack lrclibTrack3 =
                            LyricsMatcher.best(this.search(strings[0], strings[1]), lyricsQuery);
                    if (lrclibTrack3 != null) {
                        if (lrclibTrack3.hasSynced()) {
                            return new LyricsService.Found(lrclibTrack3, true);
                        }
                        if (lrclibTrack2 == null) {
                            lrclibTrack2 = lrclibTrack3;
                        }
                    }
                }
            } catch (IOException exception) {
                if (lrclibTrack2 == null) {
                    throw exception;
                }
                return new LyricsService.Found(lrclibTrack2, false);
            }
            return new LyricsService.Found(lrclibTrack2, true);
        } else {
            return new LyricsService.Found(lrclibTrack, true);
        }
    }

    private LrclibTrack get(LyricsQuery lyricsQuery, boolean bl)
            throws IOException, InterruptedException {
        LyricsService.Response response =
                this.send(
                        withQuery(
                                "api/get",
                                "track_name",
                                lyricsQuery.title(),
                                "artist_name",
                                lyricsQuery.artist(),
                                "album_name",
                                bl ? lyricsQuery.album() : "",
                                "duration",
                                lyricsQuery.hasDuration()
                                        ? Integer.toString(lyricsQuery.durationSeconds())
                                        : ""));
        if (response.status() == 404) {
            return null;
        } else if (response.status() != 200) {
            throw new IOException("LRCLIB /api/get answered HTTP " + response.status());
        } else {
            return LrclibTrack.fromJson(json(response.body()));
        }
    }

    private List<LrclibTrack> search(String string, String string2)
            throws IOException, InterruptedException {
        LyricsService.Response response =
                this.send(withQuery("api/search", "track_name", string, "artist_name", string2));
        if (response.status() == 404) {
            return List.of();
        } else if (response.status() != 200) {
            throw new IOException("LRCLIB /api/search answered HTTP " + response.status());
        } else {
            return LrclibTrack.listFromJson(json(response.body()));
        }
    }

    private LyricsService.Response send(String string) throws IOException, InterruptedException {
        URI uRI = LRCLIB.resolve(string);
        int i = 1;
        while (true) {
            this.pace();
            HttpRequest httpRequest =
                    HttpRequest.newBuilder(uRI)
                            .timeout(REQUEST_TIMEOUT)
                            .header("User-Agent", this.userAgent)
                            .header("Accept", "application/json")
                            .GET()
                            .build();
            HttpResponse httpResponse = this.exchange(httpRequest);
            this.nextRequestAt = System.currentTimeMillis() + 300L;
            int statusCode = httpResponse.statusCode();
            if (statusCode != 429 && statusCode != 503) {
                return new LyricsService.Response(
                        statusCode,
                        new String((byte[]) httpResponse.body(), StandardCharsets.UTF_8));
            }
            long retryAfterMillis =
                    retryAfterMillis(httpResponse.headers().firstValue("Retry-After").orElse(null));
            if (retryAfterMillis * i > 3000L || i >= 3) {
                this.busyUntil = System.currentTimeMillis() + retryAfterMillis;
                throw new LyricsService.BusyException(
                        "LRCLIB answered HTTP "
                                + statusCode
                                + ", retry in "
                                + retryAfterMillis
                                + " ms");
            }
            this.nextRequestAt = System.currentTimeMillis() + retryAfterMillis * i;
            i++;
        }
    }

    private HttpResponse<byte[]> exchange(HttpRequest httpRequest)
            throws IOException, InterruptedException {
        CompletableFuture future =
                this.http()
                        .sendAsync(
                                httpRequest, responseInfo -> new LyricsService.CappedBody(4194304));
        try {
            return (HttpResponse<byte[]>)
                    future.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new HttpTimeoutException(
                    "LRCLIB did not answer within " + REQUEST_TIMEOUT.toMillis() + " ms");
        } catch (InterruptedException exception2) {
            future.cancel(true);
            throw exception2;
        } catch (ExecutionException exception3) {
            Throwable cause = exception3.getCause();
            if (cause instanceof IOException exception4) {
                throw exception4;
            } else {
                throw new IOException("LRCLIB request failed", cause);
            }
        }
    }

    private void pace() throws IOException, InterruptedException {
        long busyUntil2 = this.busyUntil;
        long l = System.currentTimeMillis();
        if (busyUntil2 - l > 3000L) {
            throw new LyricsService.BusyException(
                    "LRCLIB asked to wait another " + (busyUntil2 - l) + " ms");
        } else {
            long l2 = Math.max(this.nextRequestAt, busyUntil2) - l;
            if (l2 > 0L) {
                Thread.sleep(l2);
            }
        }
    }

    private HttpClient http() throws InterruptedException {
        HttpClient http2 = this.http;
        if (http2 != null) {
            return http2;
        } else if (this.closed) {
            throw new InterruptedException("lyrics service closed");
        } else {
            ExecutorService cachedThreadPool =
                    Executors.newCachedThreadPool(daemonThreads("EFN-Lyrics-Http"));
            http2 =
                    HttpClient.newBuilder()
                            .connectTimeout(CONNECT_TIMEOUT)
                            .followRedirects(Redirect.NORMAL)
                            .executor(cachedThreadPool)
                            .build();
            this.httpExecutor = cachedThreadPool;
            this.http = http2;
            if (this.closed) {
                http2.shutdownNow();
                cachedThreadPool.shutdownNow();
                throw new InterruptedException("lyrics service closed");
            } else {
                return http2;
            }
        }
    }

    private void remember(String string, Lyrics lyrics2, long l) {
        Object lock2 = this.lock;
        synchronized (lock2) {
            this.memory.put(string, new LyricsService.Memo(lyrics2, l));
        }
    }

    private long retryAt() {
        return Math.max(System.currentTimeMillis() + 60000L, this.busyUntil);
    }

    private String keyOf(LyricsQuery lyricsQuery) {
        LyricsService.KeyMemo lastKey2 = this.lastKey;
        if (lastKey2 != null && lastKey2.query().equals(lyricsQuery)) {
            return lastKey2.key();
        } else {
            String cacheKey = lyricsQuery.cacheKey();
            this.lastKey = new LyricsService.KeyMemo(lyricsQuery, cacheKey);
            return cacheKey;
        }
    }

    private static String withQuery(String string, String... string2) {
        StringBuilder sb = new StringBuilder(string);
        byte b = 63;
        for (int i = 0; i + 1 < string2.length; i += 2) {
            String string3 = string2[i + 1];
            if (string3 != null && !string3.isEmpty()) {
                sb.append((char) b)
                        .append(string2[i])
                        .append('=')
                        .append(URLEncoder.encode(string3, StandardCharsets.UTF_8));
                b = 38;
            }
        }
        return sb.toString();
    }

    private static JsonElement json(String string) throws IOException {
        try {
            return LrclibTrack.parse(string);
        } catch (JsonParseException exception) {
            throw new IOException("LRCLIB sent malformed JSON", exception);
        }
    }

    static long retryAfterMillis(String string) {
        if (string != null && !string.isBlank()) {
            String string2 = string.strip();
            long l;
            try {
                l = Long.parseLong(string2) * 1000L;
            } catch (NumberFormatException exception) {
                try {
                    l =
                            ZonedDateTime.parse(string2, DateTimeFormatter.RFC_1123_DATE_TIME)
                                            .toInstant()
                                            .toEpochMilli()
                                    - System.currentTimeMillis();
                } catch (DateTimeParseException exception2) {
                    l = 30000L;
                }
            }
            return Math.max(1000L, Math.min(MAX_RETRY_AFTER_MS, l));
        } else {
            return 30000L;
        }
    }

    private static ThreadFactory daemonThreads(String string) {
        return runnable -> {
            Thread thread = new Thread(runnable, string);
            thread.setDaemon(true);
            return thread;
        };
    }

    private static Path defaultCacheDir() {
        try {
            return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("lyrics-cache");
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }

    static String defaultUserAgent() {
        String string;
        try {
            string =
                    FabricLoader.getInstance()
                            .getModContainer("efn")
                            .map(
                                    modContainer ->
                                            modContainer
                                                    .getMetadata()
                                                    .getVersion()
                                                    .getFriendlyString())
                            .orElse("dev");
        } catch (RuntimeException | LinkageError exception) {
            string = "dev";
        }
        int i = string.indexOf(43);
        if (i > 0) {
            string = string.substring(0, i);
        }
        return "EFN Client/" + string + " (https://efn.lol)";
    }

    @Environment(EnvType.CLIENT)
    private static final class BusyException extends IOException {

        BusyException(String string) {
            super(string);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class CappedBody implements BodySubscriber<byte[]> {

        private final CompletableFuture<byte[]> result = new CompletableFuture<>();

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        private final int limit;

        private Subscription subscription;

        CappedBody(int limit2) {
            this.limit = limit2;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return this.result;
        }

        @Override
        public void onSubscribe(Subscription subscription2) {
            this.subscription = subscription2;
            subscription2.request(9223372036854775807L);
        }

        public void onNext(List<ByteBuffer> list) {
            if (!this.result.isDone()) {
                for (ByteBuffer byteBuffer : list) {
                    if (this.bytes.size() + byteBuffer.remaining() > this.limit) {
                        this.subscription.cancel();
                        this.result.completeExceptionally(
                                new IOException("LRCLIB response too large"));
                        return;
                    }
                    byte[] bytes2 = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes2);
                    this.bytes.write(bytes2, 0, bytes2.length);
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            this.result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            this.result.complete(this.bytes.toByteArray());
        }
    }

    @Environment(EnvType.CLIENT)
    private record Found(LrclibTrack track, boolean complete) {}

    @Environment(EnvType.CLIENT)
    private record KeyMemo(LyricsQuery query, String key) {}

    @Environment(EnvType.CLIENT)
    private record Memo(Lyrics lyrics, long freshUntil) {}

    @Environment(EnvType.CLIENT)
    private record Response(int status, String body) {}
}
