package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
abstract class ScriptedPlayer implements PlayerSource {

    private static final long ART_TIMEOUT_MS = 3000L;

    private static final long ART_RETRY_MS = 10000L;

    private static final int ART_MAX_BYTES = 4194304;

    private volatile boolean closed;

    private volatile boolean unsupported;

    private volatile String lastError;

    private HttpClient http;

    private ExecutorService httpExecutor;

    private String trackKey;

    private long estimateMs;

    private long estimateAt;

    private boolean wasPlaying;

    private String artUrl = "";

    private long artRetryAt;

    private byte[] art;

    private int artVersion;

    ScriptedPlayer() {}

    protected abstract ScriptedPlayer.Reading read() throws IOException, InterruptedException;

    protected abstract boolean send(ScriptedPlayer.Command command)
            throws IOException, InterruptedException;

    @Override
    public MediaSession.Snapshot poll() {
        if (!this.closed && !this.unsupported) {
            try {
                ScriptedPlayer.Reading reading = this.read();
                this.lastError = null;
                if (reading == null) {
                    this.forgetTrack();
                    return null;
                }
                return this.snapshot(reading);
            } catch (ScriptedPlayer.MissingTool exception) {
                this.unsupported = true;
                this.lastError = exception.getMessage();
            } catch (IOException exception2) {
                this.lastError = exception2.getMessage();
            } catch (InterruptedException exception3) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException exception4) {
                this.lastError = exception4.toString();
            }
            return null;
        } else {
            return null;
        }
    }

    @Override
    public boolean togglePlayPause() {
        return this.command(ScriptedPlayer.Command.PLAY_PAUSE);
    }

    @Override
    public boolean skipNext() {
        return this.command(ScriptedPlayer.Command.NEXT);
    }

    @Override
    public boolean skipPrevious() {
        return this.command(ScriptedPlayer.Command.PREVIOUS);
    }

    @Override
    public String lastError() {
        return this.lastError;
    }

    @Override
    public boolean isUnsupported() {
        return this.unsupported;
    }

    @Override
    public void close() {
        this.closed = true;
        HttpClient http2 = this.http;
        ExecutorService httpExecutor2 = this.httpExecutor;
        this.http = null;
        this.httpExecutor = null;
        if (http2 != null) {
            http2.shutdownNow();
        }
        if (httpExecutor2 != null) {
            httpExecutor2.shutdownNow();
        }
    }

    private boolean command(ScriptedPlayer.Command command) {
        if (!this.closed && !this.unsupported) {
            try {
                boolean send = this.send(command);
                this.lastError = null;
                return send;
            } catch (ScriptedPlayer.MissingTool exception) {
                this.unsupported = true;
                this.lastError = exception.getMessage();
            } catch (IOException exception2) {
                this.lastError = exception2.getMessage();
            } catch (InterruptedException exception3) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException exception4) {
                this.lastError = exception4.toString();
            }
            return false;
        } else {
            return false;
        }
    }

    private MediaSession.Snapshot snapshot(ScriptedPlayer.Reading reading) {
        long l = System.currentTimeMillis();
        String string = reading.title() + "\n" + reading.artist() + "\n" + reading.album();
        boolean bl = !string.equals(this.trackKey);
        this.trackKey = string;
        boolean bl2 = reading.state() == MediaSession.PlaybackState.PLAYING;
        if (bl) {
            this.estimateMs = Math.max(0L, reading.positionMs());
        } else if (this.wasPlaying) {
            this.estimateMs = this.estimateMs + Math.max(0L, l - this.estimateAt);
        }
        this.estimateAt = l;
        this.wasPlaying = bl2;
        if (reading.positionMs() > 0L) {
            this.estimateMs = reading.positionMs();
        }
        long estimateMs2 = this.estimateMs;
        if (reading.durationMs() > 0L) {
            estimateMs2 = Math.min(estimateMs2, reading.durationMs());
        }
        this.updateArt(reading.artUrl(), l);
        return new MediaSession.Snapshot(
                reading.title(),
                reading.artist(),
                reading.album(),
                reading.durationMs(),
                estimateMs2,
                l,
                reading.state(),
                1.0,
                this.art,
                this.art == null ? "" : "image",
                this.artVersion);
    }

    private void forgetTrack() {
        this.trackKey = null;
        this.wasPlaying = false;
        this.estimateMs = 0L;
    }

    private void updateArt(String string, long l) {
        String string2 = string == null ? "" : string.strip();
        if (!string2.equals(this.artUrl)
                || this.art == null && !string2.isEmpty() && l >= this.artRetryAt) {
            this.artUrl = string2;
            byte[] bytes = string2.isEmpty() ? null : this.download(string2);
            if (bytes == null && !string2.isEmpty()) {
                this.artRetryAt = l + 10000L;
            }
            if (bytes != null || this.art != null) {
                this.art = bytes;
                this.artVersion++;
            }
        }
    }

    private byte[] download(String string) {
        URI uRI;
        try {
            uRI = URI.create(string);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        String string2 = uRI.getScheme() == null ? "" : uRI.getScheme().toLowerCase(Locale.ROOT);
        if (!string2.equals("https") && !string2.equals("http")) {
            return null;
        } else {
            try {
                HttpRequest httpRequest =
                        HttpRequest.newBuilder(uRI).timeout(Duration.ofMillis(3000L)).GET().build();
                HttpResponse httpResponse =
                        this.http().send(httpRequest, BodyHandlers.ofByteArray());
                byte[] bytes = (byte[]) httpResponse.body();
                return httpResponse.statusCode() == 200
                                && bytes != null
                                && bytes.length != 0
                                && bytes.length <= 4194304
                        ? bytes
                        : null;
            } catch (IOException exception2) {
                this.lastError = "album art: " + exception2.getMessage();
                return null;
            } catch (InterruptedException exception3) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    private HttpClient http() {
        if (this.http == null) {
            this.httpExecutor =
                    Executors.newCachedThreadPool(
                            runnable -> {
                                Thread thread = new Thread(runnable, "EFN-Spotify-Art");
                                thread.setDaemon(true);
                                return thread;
                            });
            this.http =
                    HttpClient.newBuilder()
                            .connectTimeout(Duration.ofMillis(3000L))
                            .followRedirects(Redirect.NORMAL)
                            .executor(this.httpExecutor)
                            .build();
        }
        return this.http;
    }

    static String run(long l, String... string) throws IOException, InterruptedException {
        Process process;
        try {
            process = new ProcessBuilder(string).start();
        } catch (IOException exception) {
            throw new ScriptedPlayer.MissingTool(string[0] + " is not available");
        }
        String string2;
        try {
            process.getOutputStream().close();
            if (!process.waitFor(l, TimeUnit.MILLISECONDS)) {
                throw new IOException(string[0] + " did not answer within " + l + " ms");
            }
            String all = readAll(process.getInputStream());
            if (process.exitValue() != 0) {
                String string3 = readAll(process.getErrorStream()).strip();
                throw new ScriptedPlayer.CommandFailed(
                        string3.isEmpty()
                                ? string[0] + " exited with " + process.exitValue()
                                : string3);
            }
            string2 = all;
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            process.getInputStream().close();
            process.getErrorStream().close();
        }
        return string2;
    }

    private static String readAll(InputStream inValue) throws IOException {
        return new String(inValue.readAllBytes(), StandardCharsets.UTF_8);
    }

    @Environment(EnvType.CLIENT)
    static enum Command {
        PLAY_PAUSE,
        NEXT,
        PREVIOUS;

        private Command() {}
    }

    @Environment(EnvType.CLIENT)
    static class CommandFailed extends IOException {

        CommandFailed(String string) {
            super(string);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class MissingTool extends IOException {

        MissingTool(String string) {
            super(string);
        }
    }

    @Environment(EnvType.CLIENT)
    record Reading(
            MediaSession.PlaybackState state,
            String title,
            String artist,
            String album,
            long durationMs,
            long positionMs,
            String artUrl) {}
}
