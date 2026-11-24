package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record NowPlaying(
        NowPlaying.Status status,
        String title,
        String artist,
        String album,
        NowPlaying.Source source,
        long durationMs,
        long positionMs,
        long sampledAtMs,
        double rate,
        AlbumArt art) {

    private static final String SEPARATOR = " - ";

    static final long LYRICS_MIN_LENGTH_MS = 30000L;

    static final long LYRICS_MAX_LENGTH_MS = 3600000L;

    public static final NowPlaying UNSUPPORTED = idle(NowPlaying.Status.UNSUPPORTED);

    public static final NowPlaying WAITING = idle(NowPlaying.Status.WAITING);

    public static final NowPlaying NOT_RUNNING = idle(NowPlaying.Status.NOT_RUNNING);

    public NowPlaying(
            NowPlaying.Status status,
            String title,
            String artist,
            String album,
            NowPlaying.Source source,
            long durationMs,
            long positionMs,
            long sampledAtMs,
            double rate,
            AlbumArt art) {
        if (status == null) {
            status = NowPlaying.Status.NOT_RUNNING;
        }
        if (source == null) {
            source = NowPlaying.Source.NONE;
        }
        title = title == null ? "" : title;
        artist = artist == null ? "" : artist;
        album = album == null ? "" : album;
        durationMs = Math.max(0L, durationMs);
        positionMs =
                durationMs > 0L
                        ? Math.max(0L, Math.min(positionMs, durationMs))
                        : Math.max(0L, positionMs);
        if (!(rate > 0.0) || rate > 16.0) {
            rate = 1.0;
        }
        this.status = status;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.source = source;
        this.durationMs = durationMs;
        this.positionMs = positionMs;
        this.sampledAtMs = sampledAtMs;
        this.rate = rate;
        this.art = art;
    }

    public NowPlaying(NowPlaying.Status status, String string, String string2) {
        this(status, string, string2, "", NowPlaying.Source.WINDOW, 0L, 0L, 0L, 1.0, null);
    }

    private static NowPlaying idle(NowPlaying.Status status2) {
        return new NowPlaying(status2, "", "", "", NowPlaying.Source.NONE, 0L, 0L, 0L, 1.0, null);
    }

    public boolean playing() {
        return this.status == NowPlaying.Status.PLAYING;
    }

    public boolean hasTrack() {
        return !this.title.isEmpty();
    }

    public boolean active() {
        return this.status == NowPlaying.Status.PLAYING || this.status == NowPlaying.Status.PAUSED;
    }

    public boolean hasTimeline() {
        return this.source == NowPlaying.Source.SESSION && this.durationMs > 0L;
    }

    public boolean lyricsEligible() {
        if (!this.hasTimeline() || !this.hasTrack() || this.artist.isBlank()) {
            return false;
        } else {
            return this.durationMs >= 30000L && this.durationMs <= 3600000L
                    ? !this.artist.equalsIgnoreCase("Spotify")
                            && !this.title.equalsIgnoreCase("Advertisement")
                    : false;
        }
    }

    public boolean sameTrack(NowPlaying nowPlaying) {
        return nowPlaying != null
                && this.title.equals(nowPlaying.title)
                && this.artist.equals(nowPlaying.artist)
                && this.album.equals(nowPlaying.album);
    }

    public long positionAt(long l) {
        if (!this.hasTimeline()) {
            return 0L;
        } else if (!this.playing()) {
            return this.positionMs;
        } else {
            long l2 = this.positionMs + (long) (Math.max(0L, l - this.sampledAtMs) * this.rate);
            return Math.min(l2, this.durationMs);
        }
    }

    public NowPlaying toggled() {
        long l = System.currentTimeMillis();
        return switch (this.status) {
            case PAUSED ->
                    this.hasTrack()
                            ? new NowPlaying(
                                    NowPlaying.Status.PLAYING,
                                    this.title,
                                    this.artist,
                                    this.album,
                                    this.source,
                                    this.durationMs,
                                    this.positionMs,
                                    l,
                                    this.rate,
                                    this.art)
                            : this;
            case PLAYING ->
                    new NowPlaying(
                            NowPlaying.Status.PAUSED,
                            this.title,
                            this.artist,
                            this.album,
                            this.source,
                            this.durationMs,
                            this.positionAt(l),
                            l,
                            this.rate,
                            this.art);
            default -> this;
        };
    }

    public static NowPlaying fromSession(
            MediaSession.Snapshot snapshot, AlbumArt albumArt, NowPlaying nowPlaying) {
        NowPlaying.Status status2 =
                switch (snapshot.state()) {
                    case PLAYING -> NowPlaying.Status.PLAYING;
                    case CLOSED -> NowPlaying.Status.NOT_RUNNING;
                    case CHANGING -> {
                        if (nowPlaying != null
                                && nowPlaying.source == NowPlaying.Source.SESSION
                                && nowPlaying.playing()) {
                            yield NowPlaying.Status.PLAYING;
                        } else {
                            yield NowPlaying.Status.PAUSED;
                        }
                    }
                    default -> NowPlaying.Status.PAUSED;
                };
        return status2 == NowPlaying.Status.NOT_RUNNING
                ? NOT_RUNNING
                : new NowPlaying(
                        status2,
                        snapshot.title(),
                        snapshot.artist(),
                        snapshot.album(),
                        NowPlaying.Source.SESSION,
                        snapshot.durationMs(),
                        snapshot.positionMs(),
                        snapshot.sampledAtMs(),
                        snapshot.rate(),
                        albumArt);
    }

    public static NowPlaying fromWindowTitle(String string3, NowPlaying nowPlaying) {
        if (string3 == null) {
            return NOT_RUNNING;
        } else {
            String string4 = string3.strip();
            if (string4.isEmpty()) {
                return NOT_RUNNING;
            } else {
                int i = string4.indexOf(" - ");
                if (i < 0 && (string4.equals("Spotify") || string4.startsWith("Spotify "))) {
                    return nowPlaying != null && nowPlaying.hasTrack()
                            ? new NowPlaying(
                                    NowPlaying.Status.PAUSED, nowPlaying.title, nowPlaying.artist)
                            : new NowPlaying(NowPlaying.Status.PAUSED, "", "");
                } else {
                    return i > 0
                            ? new NowPlaying(
                                    NowPlaying.Status.PLAYING,
                                    string4.substring(i + " - ".length()).strip(),
                                    string4.substring(0, i).strip())
                            : new NowPlaying(NowPlaying.Status.PLAYING, string4, "");
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Source {
        NONE,
        WINDOW,
        SESSION;

        private Source() {}
    }

    @Environment(EnvType.CLIENT)
    public static enum Status {
        UNSUPPORTED,
        WAITING,
        NOT_RUNNING,
        PAUSED,
        PLAYING;

        private Status() {}
    }
}
