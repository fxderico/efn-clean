package net.efn.client.util.spotify;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid.GUID;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class MediaSession implements PlayerSource {

    private static final String MANAGER_CLASS =
            "Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager";

    private static final GUID IID_MANAGER_STATICS =
            WinRt.iid("2050C4EE-11A0-57DE-AED7-C97C70338245");

    private static final int STATICS_REQUEST_ASYNC = 6;

    private static final int MANAGER_GET_CURRENT_SESSION = 6;

    private static final int MANAGER_GET_SESSIONS = 7;

    private static final int VECTOR_VIEW_GET_AT = 6;

    private static final int VECTOR_VIEW_GET_SIZE = 7;

    private static final int SESSION_GET_SOURCE_APP_USER_MODEL_ID = 6;

    private static final int SESSION_TRY_GET_MEDIA_PROPERTIES_ASYNC = 7;

    private static final int SESSION_GET_TIMELINE_PROPERTIES = 8;

    private static final int SESSION_GET_PLAYBACK_INFO = 9;

    private static final int SESSION_TRY_SKIP_NEXT_ASYNC = 16;

    private static final int SESSION_TRY_SKIP_PREVIOUS_ASYNC = 17;

    private static final int SESSION_TRY_TOGGLE_PLAY_PAUSE_ASYNC = 20;

    private static final int PROPERTIES_GET_TITLE = 6;

    private static final int PROPERTIES_GET_ARTIST = 9;

    private static final int PROPERTIES_GET_ALBUM_TITLE = 10;

    private static final int PROPERTIES_GET_THUMBNAIL = 15;

    private static final int TIMELINE_GET_START_TIME = 6;

    private static final int TIMELINE_GET_END_TIME = 7;

    private static final int TIMELINE_GET_POSITION = 10;

    private static final int TIMELINE_GET_LAST_UPDATED_TIME = 11;

    private static final int PLAYBACK_INFO_GET_PLAYBACK_STATUS = 7;

    private static final int PLAYBACK_INFO_GET_PLAYBACK_RATE = 10;

    private static final int REFERENCE_GET_VALUE = 6;

    private static final int STREAM_REFERENCE_OPEN_READ_ASYNC = 6;

    private static final GUID IID_RANDOM_ACCESS_STREAM =
            WinRt.iid("905A0FE1-BC53-11DF-8C49-001E4FC686DA");

    private static final int RANDOM_ACCESS_STREAM_GET_SIZE = 6;

    private static final GUID IID_INPUT_STREAM = WinRt.iid("905A0FE2-BC53-11DF-8C49-001E4FC686DA");

    private static final GUID IID_CONTENT_TYPE_PROVIDER =
            WinRt.iid("97D098A5-3B99-4DE9-88A5-E11D2F50C795");

    private static final int CONTENT_TYPE_PROVIDER_GET_CONTENT_TYPE = 6;

    private static final String DATA_READER_CLASS = "Windows.Storage.Streams.DataReader";

    private static final GUID IID_DATA_READER_FACTORY =
            WinRt.iid("D7527847-57DA-4E15-914C-06806699A098");

    private static final int DATA_READER_FACTORY_CREATE = 6;

    private static final int DATA_READER_PUT_INPUT_STREAM_OPTIONS = 12;

    private static final int DATA_READER_READ_BYTES = 14;

    private static final int DATA_READER_LOAD_ASYNC = 29;

    private static final int INPUT_STREAM_OPTIONS_PARTIAL = 1;

    private static final long REQUEST_TIMEOUT_MS = 3000L;

    private static final long PROPERTIES_TIMEOUT_MS = 1000L;

    private static final long THUMBNAIL_TIMEOUT_MS = 1500L;

    private static final long CONTROL_TIMEOUT_MS = 1000L;

    private static final long MANAGER_RETRY_MS = 5000L;

    private static final int MAX_SESSIONS = 64;

    private static final int THUMBNAIL_CHECKS = 3;

    private static final long THUMBNAIL_RECHECK_MS = 2000L;

    private static final long THUMBNAIL_SETTLE_MS = 1500L;

    private static final int THUMBNAIL_CHUNK = 65536;

    private static final int THUMBNAIL_MAX_BYTES = 4194304;

    private static final long UNIX_EPOCH_TICKS = 116444736000000000L;

    private static final long TICKS_PER_MS = 10000L;

    private final String appFilter;

    private volatile Thread owner;

    private boolean ownerInitialized;

    private volatile boolean closed;

    private volatile boolean unsupported;

    private volatile String lastError;

    private Pointer manager;

    private Pointer dataReaderFactory;

    private long managerRetryAt;

    private String sessionApp = "";

    private String trackKey;

    private String trackAlbum = "";

    private long trackChangedAt;

    private byte[] thumbnail;

    private String thumbnailType = "";

    private int thumbnailVersion;

    private int thumbnailChecksLeft;

    private long nextThumbnailCheckAt;

    private byte[] staleThumbnail;

    public MediaSession(String string) {
        this.appFilter = string == null ? null : string.toLowerCase(Locale.ROOT);
    }

    public static MediaSession spotify() {
        return new MediaSession("spotify");
    }

    @Override
    public MediaSession.Snapshot poll() {
        try {
            if (!this.prepare()) {
                return null;
            } else {
                this.lastError = null;
                return this.read();
            }
        } catch (WinRt.WinRtException exception) {
            return this.fail(exception.getMessage());
        } catch (LinkageError exception2) {
            this.unsupported = true;
            return this.fail("WinRT unavailable: " + exception2);
        } catch (RuntimeException exception3) {
            return this.fail(exception3.toString());
        }
    }

    @Override
    public boolean togglePlayPause() {
        return this.control(20, "TryTogglePlayPauseAsync");
    }

    @Override
    public boolean skipNext() {
        return this.control(16, "TrySkipNextAsync");
    }

    @Override
    public boolean skipPrevious() {
        return this.control(17, "TrySkipPreviousAsync");
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
        if (this.owner == Thread.currentThread()) {
            this.dispose();
        }
    }

    private boolean prepare() {
        Thread thread = Thread.currentThread();
        if (this.owner != thread) {
            if (this.owner != null && this.owner.isAlive()) {
                this.lastError = "called from a thread other than the owner";
                return false;
            }
            if (this.closed || this.unsupported) {
                return false;
            }
            int initializeMultithreaded = WinRt.initializeMultithreaded();
            if (initializeMultithreaded < 0 && initializeMultithreaded != -2147417850) {
                throw new WinRt.WinRtException("RoInitialize", initializeMultithreaded);
            }
            this.owner = thread;
            boolean bl = this.ownerInitialized = initializeMultithreaded >= 0;
        }
        if (this.closed) {
            this.dispose();
            return false;
        } else if (this.unsupported) {
            return false;
        } else {
            if (this.manager == null) {
                long l = System.currentTimeMillis();
                if (l < this.managerRetryAt) {
                    return false;
                }
                this.managerRetryAt = l + 5000L;
                this.manager = this.requestManager();
            }
            return this.manager != null;
        }
    }

    private Pointer requestManager() {
        Pointer pointer;
        try (WinRt.Refs refs = new WinRt.Refs()) {
            Pointer pointer2;
            try {
                pointer2 =
                        refs.add(
                                WinRt.activationFactory(
                                        "Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager",
                                        IID_MANAGER_STATICS));
            } catch (WinRt.WinRtException exception) {
                if (exception.hresult == -2147221164 || exception.hresult == -2147467262) {
                    this.unsupported = true;
                }
                throw exception;
            }
            Pointer pointer3 = refs.add(WinRt.callForObject(pointer2, 6, "RequestAsync"));
            Pointer pointer4 = WinRt.awaitObject(pointer3, 3000L, "RequestAsync");
            pointer = pointer4;
        }
        return pointer;
    }

    private void dispose() {
        Pointer manager2 = this.manager;
        Pointer dataReaderFactory2 = this.dataReaderFactory;
        this.manager = null;
        this.dataReaderFactory = null;
        try {
            WinRt.release(manager2);
            WinRt.release(dataReaderFactory2);
        } catch (RuntimeException | LinkageError exception) {
        }
        this.forgetTrack();
        if (this.ownerInitialized) {
            this.ownerInitialized = false;
            try {
                WinRt.uninitialize();
            } catch (RuntimeException | LinkageError exception2) {
            }
        }
    }

    private MediaSession.Snapshot fail(String lastError2) {
        this.lastError = lastError2;
        return null;
    }

    private void dropManager() {
        Pointer manager2 = this.manager;
        this.manager = null;
        try {
            WinRt.release(manager2);
        } catch (RuntimeException | LinkageError exception) {
        }
    }

    private MediaSession.Snapshot read() {
        MediaSession.Snapshot snapshot;
        try (WinRt.Refs refs = new WinRt.Refs()) {
            Pointer session = this.findSession(refs);
            if (session == null) {
                this.forgetTrack();
                return null;
            }
            String sessionApp2 = this.sessionApp;
            MediaSession.PlaybackState cLOSED = MediaSession.PlaybackState.CLOSED;
            double d = 1.0;
            Pointer pointer = refs.add(WinRt.callForObject(session, 9, "GetPlaybackInfo"));
            if (pointer != null) {
                cLOSED =
                        MediaSession.PlaybackState.of(WinRt.getInt32(pointer, 7, "PlaybackStatus"));
                Pointer pointer2 = refs.add(WinRt.callForObject(pointer, 10, "PlaybackRate"));
                double e;
                if (pointer2 != null
                        && (e = WinRt.getDouble(pointer2, 6, "PlaybackRate value")) > 0.0
                        && e <= 16.0) {
                    d = e;
                }
            }
            long l = 0L;
            long l2 = 0L;
            long l3 = 0L;
            long l4 = 0L;
            Pointer pointer3 = refs.add(WinRt.callForObject(session, 8, "GetTimelineProperties"));
            if (pointer3 != null) {
                l = WinRt.getInt64(pointer3, 6, "StartTime");
                l2 = WinRt.getInt64(pointer3, 7, "EndTime");
                l3 = WinRt.getInt64(pointer3, 10, "Position");
                l4 = WinRt.getInt64(pointer3, 11, "LastUpdatedTime");
            }
            long l5 = System.currentTimeMillis();
            String string = "";
            String string2 = "";
            String string3 = "";
            Pointer pointer4 =
                    refs.add(WinRt.callForObject(session, 7, "TryGetMediaPropertiesAsync"));
            Pointer pointer5 =
                    refs.add(WinRt.awaitObject(pointer4, 1000L, "TryGetMediaPropertiesAsync"));
            if (pointer5 != null) {
                string = WinRt.getString(pointer5, 6, "Title").strip();
                string2 = WinRt.getString(pointer5, 9, "Artist").strip();
                string3 = WinRt.getString(pointer5, 10, "AlbumTitle").strip();
            }
            long l6 = System.currentTimeMillis();
            String string4 = sessionApp2 + "\n" + string + "\n" + string2 + "\n" + string3;
            if (!string4.equals(this.trackKey)) {
                boolean bl =
                        this.trackKey != null
                                && !string3.isEmpty()
                                && string3.equals(this.trackAlbum);
                byte[] bytes2 = this.staleThumbnail = bl ? null : this.thumbnail;
                if (!bl) {
                    this.setThumbnail(null, "");
                }
                this.trackKey = string4;
                this.trackAlbum = string3;
                this.trackChangedAt = l6;
                this.thumbnailChecksLeft = 3;
                this.nextThumbnailCheckAt = l6;
            }
            if (pointer5 != null
                    && this.thumbnailChecksLeft > 0
                    && l6 >= this.nextThumbnailCheckAt) {
                MediaSession.Thumbnail thumbnail2 = this.readThumbnail(pointer5);
                l6 = System.currentTimeMillis();
                boolean bl2 =
                        thumbnail2 != null
                                && this.staleThumbnail != null
                                && Arrays.equals(thumbnail2.bytes(), this.staleThumbnail);
                if ((thumbnail2 == null || bl2) && l6 - this.trackChangedAt < 1500L) {
                    this.nextThumbnailCheckAt = l6;
                } else {
                    if (thumbnail2 != null) {
                        this.setThumbnail(thumbnail2.bytes(), thumbnail2.type());
                    }
                    this.staleThumbnail = null;
                    this.thumbnailChecksLeft--;
                    this.nextThumbnailCheckAt = l6 + 2000L;
                }
            }
            long l7 = Math.max(0L, (l2 - l) / 10000L);
            long l8 = Math.max(0L, (l3 - l) / 10000L);
            long l9;
            if (cLOSED == MediaSession.PlaybackState.PLAYING
                    && l7 > 0L
                    && (l9 = l6 - (l4 > 0L ? (l4 - 116444736000000000L) / 10000L : l5)) > 0L) {
                l8 += (long) (Math.min(l9, l7) * d);
            }
            if (l7 > 0L) {
                l8 = Math.min(l8, l7);
            }
            MediaSession.Snapshot snapshot2 =
                    new MediaSession.Snapshot(
                            string,
                            string2,
                            string3,
                            l7,
                            l8,
                            l6,
                            cLOSED,
                            d,
                            this.thumbnail,
                            this.thumbnailType,
                            this.thumbnailVersion);
            snapshot = snapshot2;
        }
        return snapshot;
    }

    private Pointer findSession(WinRt.Refs refs) {
        if (this.appFilter == null) {
            Pointer pointer = refs.add(this.managerCall(6, "GetCurrentSession"));
            if (pointer != null) {
                this.sessionApp = WinRt.getString(pointer, 6, "SourceAppUserModelId");
            }
            return pointer;
        } else {
            Pointer pointer2 = refs.add(this.managerCall(7, "GetSessions"));
            if (pointer2 == null) {
                return null;
            } else {
                int i = Math.min(WinRt.getInt32(pointer2, 7, "Sessions.Size"), 64);
                for (int j = 0; j < i; j++) {
                    Pointer pointer3 =
                            refs.add(WinRt.callForObject(pointer2, 6, "Sessions.GetAt", j));
                    String string;
                    if (pointer3 != null
                            && (string = WinRt.getString(pointer3, 6, "SourceAppUserModelId"))
                                    .toLowerCase(Locale.ROOT)
                                    .contains(this.appFilter)) {
                        this.sessionApp = string;
                        return pointer3;
                    }
                }
                return null;
            }
        }
    }

    private Pointer managerCall(int i, String string) {
        try {
            return WinRt.callForObject(this.manager, i, string);
        } catch (WinRt.WinRtException exception) {
            this.dropManager();
            throw exception;
        }
    }

    private void forgetTrack() {
        this.trackKey = null;
        this.trackAlbum = "";
        this.thumbnailChecksLeft = 0;
        this.staleThumbnail = null;
        this.setThumbnail(null, "");
    }

    private void setThumbnail(byte[] thumbnail2, String string) {
        if (thumbnail2 == null
                ? this.thumbnail != null
                : !Arrays.equals(thumbnail2, this.thumbnail)) {
            this.thumbnail = thumbnail2;
            this.thumbnailType = thumbnail2 == null ? "" : string;
            this.thumbnailVersion++;
        }
    }

    private MediaSession.Thumbnail readThumbnail(Pointer pointer) {
        try {
            MediaSession.Thumbnail thumbnail2;
            try (WinRt.Refs refs = new WinRt.Refs()) {
                Pointer pointer2 = refs.add(WinRt.callForObject(pointer, 15, "Thumbnail"));
                if (pointer2 == null) {
                    return null;
                }
                Pointer pointer3 = refs.add(WinRt.callForObject(pointer2, 6, "OpenReadAsync"));
                Pointer pointer4 = refs.add(WinRt.awaitObject(pointer3, 1500L, "OpenReadAsync"));
                if (pointer4 == null) {
                    return null;
                }
                String string = "";
                Pointer pointer5 =
                        refs.add(WinRt.tryQueryInterface(pointer4, IID_CONTENT_TYPE_PROVIDER));
                if (pointer5 != null) {
                    string = WinRt.getString(pointer5, 6, "ContentType");
                }
                long int64;
                if ((int64 =
                                        WinRt.getInt64(
                                                refs.add(
                                                        WinRt.queryInterface(
                                                                pointer4,
                                                                IID_RANDOM_ACCESS_STREAM,
                                                                "IRandomAccessStream")),
                                                6,
                                                "Stream.Size"))
                                < 0L
                        || int64 > 4194304L) {
                    return null;
                }
                Pointer pointer6 =
                        refs.add(WinRt.queryInterface(pointer4, IID_INPUT_STREAM, "IInputStream"));
                Pointer pointer7 =
                        refs.add(
                                WinRt.callForObject(
                                        this.dataReaderFactory(), 6, "CreateDataReader", pointer6));
                if (pointer7 == null) {
                    return null;
                }
                WinRt.check(WinRt.invoke(pointer7, 12, 1), "InputStreamOptions");
                ByteArrayOutputStream byteArrayOutputStream =
                        new ByteArrayOutputStream(int64 > 0L ? (int) int64 : 65536);
                while (int64 == 0L || byteArrayOutputStream.size() < int64) {
                    int i =
                            int64 > 0L
                                    ? (int) Math.min(int64 - byteArrayOutputStream.size(), 65536L)
                                    : 65536;
                    Pointer pointer8 = WinRt.callForObject(pointer7, 29, "LoadAsync", i);
                    int awaitUInt32;
                    try {
                        awaitUInt32 = WinRt.awaitUInt32(pointer8, 1500L, "LoadAsync");
                    } finally {
                        WinRt.release(pointer8);
                    }
                    if (awaitUInt32 <= 0) {
                        break;
                    }
                    if (byteArrayOutputStream.size() + awaitUInt32 > 4194304) {
                        return null;
                    }
                    byte[] bytes2 = new byte[awaitUInt32];
                    WinRt.check(WinRt.invoke(pointer7, 14, awaitUInt32, bytes2), "ReadBytes");
                    byteArrayOutputStream.write(bytes2, 0, awaitUInt32);
                }
                if (byteArrayOutputStream.size() == 0
                        || int64 > 0L && byteArrayOutputStream.size() != int64) {
                    return null;
                }
                MediaSession.Thumbnail thumbnail3 =
                        new MediaSession.Thumbnail(byteArrayOutputStream.toByteArray(), string);
                thumbnail2 = thumbnail3;
            }
            return thumbnail2;
        } catch (WinRt.WinRtException exception) {
            this.lastError = "album art: " + exception.getMessage();
            return null;
        }
    }

    private Pointer dataReaderFactory() {
        if (this.dataReaderFactory == null) {
            this.dataReaderFactory =
                    WinRt.activationFactory(
                            "Windows.Storage.Streams.DataReader", IID_DATA_READER_FACTORY);
        }
        return this.dataReaderFactory;
    }

    private boolean control(int i, String string, Object... object) {
        try {
            if (!this.prepare()) {
                return false;
            } else {
                boolean bl;
                try (WinRt.Refs refs = new WinRt.Refs()) {
                    Pointer session = this.findSession(refs);
                    if (session == null) {
                        return false;
                    }
                    Pointer pointer = refs.add(WinRt.callForObject(session, i, string, object));
                    boolean awaitBoolean = WinRt.awaitBoolean(pointer, 1000L, string);
                    this.lastError = null;
                    bl = awaitBoolean;
                }
                return bl;
            }
        } catch (WinRt.WinRtException exception) {
            this.fail(exception.getMessage());
            return false;
        } catch (LinkageError exception2) {
            this.unsupported = true;
            this.fail("WinRT unavailable: " + exception2);
            return false;
        } catch (RuntimeException exception3) {
            this.fail(exception3.toString());
            return false;
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum PlaybackState {
        CLOSED,
        OPENED,
        CHANGING,
        STOPPED,
        PLAYING,
        PAUSED;

        private static final MediaSession.PlaybackState[] VALUES = values();

        private PlaybackState() {}

        static MediaSession.PlaybackState of(int i) {
            return i >= 0 && i < VALUES.length ? VALUES[i] : CLOSED;
        }
    }

    @Environment(EnvType.CLIENT)
    public record Snapshot(
            String title,
            String artist,
            String album,
            long durationMs,
            long positionMs,
            long sampledAtMs,
            MediaSession.PlaybackState state,
            double rate,
            byte[] thumbnail,
            String thumbnailType,
            int thumbnailVersion) {

        public boolean hasThumbnail() {
            return this.thumbnail != null;
        }
    }

    @Environment(EnvType.CLIENT)
    private record Thumbnail(byte[] bytes, String type) {}
}
