/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.jna.Platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.Closeable;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Environment(value = EnvType.CLIENT)
public final class DiscordIpc implements Closeable {

    private static final int OP_HANDSHAKE = 0;

    private static final int OP_FRAME = 1;

    private static final int OP_CLOSE = 2;

    private static final int OP_PING = 3;

    private static final int OP_PONG = 4;

    private static final int RPC_VERSION = 1;

    private static final int SOCKET_SLOTS = 10;

    private static final String[] UNIX_TEMP_VARIABLES =
            new String[] {"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"};

    private static final boolean WINDOWS = Platform.isWindows();

    private static final long REPLY_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5L);

    private static final long READ_POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(20L);

    private static final int MAX_FRAME_BYTES = 0x100000;

    private static final long PID = ProcessHandle.current().pid();

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final Pipe pipe;

    private final ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

    private DiscordIpc(Pipe pipe2) {
        this.pipe = pipe2;
    }

    public static DiscordIpc connect(String string) throws IOException {
        Pipe pipe2 = WINDOWS ? DiscordIpc.openWindowsPipe() : DiscordIpc.openUnixSocket();
        Pipe pipe3 = pipe2;
        if (pipe2 == null) {
            return null;
        }
        DiscordIpc discordIpc = new DiscordIpc(pipe2);
        try {
            discordIpc.handshake(string);
            return discordIpc;
        } catch (IOException | RuntimeException exception) {
            discordIpc.close();
            throw exception;
        }
    }

    public void setActivity(JsonObject json) throws IOException, RefusedException {
        JsonObject json2;
        String string = UUID.randomUUID().toString();
        JsonObject json3 = new JsonObject();
        json3.addProperty("pid", PID);
        json3.add("activity", json != null ? json : JsonNull.INSTANCE);
        JsonObject json4 = new JsonObject();
        json4.addProperty("cmd", "SET_ACTIVITY");
        json4.add("args", json3);
        json4.addProperty("nonce", string);
        this.send(1, json4);
        long l = System.nanoTime() + REPLY_TIMEOUT_NANOS;
        while ((json2 = this.answer(this.readFrame(l, true))) == null
                || !string.equals(DiscordIpc.string(json2, "nonce"))) {}
        if ("ERROR".equals(DiscordIpc.string(json2, "evt"))) {
            throw new RefusedException(DiscordIpc.errorMessage(json2));
        }
    }

    public void poll() throws IOException {
        Frame frame;
        while ((frame = this.readFrame(System.nanoTime() + REPLY_TIMEOUT_NANOS, false)) != null) {
            this.answer(frame);
        }
    }

    @Override
    public void close() {
        try {
            this.pipe.close();
        } catch (IOException exception) {
            // empty catch block
        }
    }

    private void handshake(String string) throws IOException {
        JsonObject json;
        JsonObject json2 = new JsonObject();
        json2.addProperty("v", 1);
        json2.addProperty("client_id", string);
        this.send(0, json2);
        long l = System.nanoTime() + REPLY_TIMEOUT_NANOS;
        while (true) {
            if ((json = this.answer(this.readFrame(l, true))) == null
                    || !"DISPATCH".equals(DiscordIpc.string(json, "cmd"))) {
                continue;
            }
            String string2 = DiscordIpc.string(json, "evt");
            if ("READY".equals(string2)) {
                return;
            }
            if ("ERROR".equals(string2)) break;
        }
        throw new IOException("Discord refused the handshake: " + DiscordIpc.errorMessage(json));
    }

    private JsonObject answer(Frame frame) throws IOException {
        return switch (frame.opcode()) {
            case 1 -> frame.json();
            case 3 -> {
                this.send(4, frame.body());
                yield null;
            }
            case 2 ->
                    throw new IOException(
                            "Discord closed the connection: "
                                    + DiscordIpc.errorMessage(frame.json()));
            default -> null;
        };
    }

    private Frame readFrame(long l, boolean bl) throws IOException {
        this.header.clear();
        if (!bl && this.pipe.readWaiting(this.header) == 0) {
            return null;
        }
        this.readFully(this.header, l);
        this.header.flip();
        int header2 = this.header.getInt();
        int header3 = this.header.getInt();
        if (header3 < 0 || header3 > 0x100000) {
            throw new IOException("Discord sent a frame of " + header3 + " bytes");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(header3);
        this.readFully(byteBuffer, l);
        return new Frame(header2, byteBuffer.array());
    }

    private void readFully(ByteBuffer byteBuffer, long l) throws IOException {
        while (byteBuffer.hasRemaining()) {
            if (this.pipe.readWaiting(byteBuffer) > 0) continue;
            if (System.nanoTime() - l >= 0L) {
                throw new SocketTimeoutException("Discord did not answer in time");
            }
            LockSupport.parkNanos(READ_POLL_NANOS);
        }
    }

    private void send(int i, JsonObject json) throws IOException {
        this.send(i, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    private void send(int i, byte[] bytes) throws IOException {
        ByteBuffer byteBuffer =
                ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(i).putInt(bytes.length).put(bytes).flip();
        this.pipe.write(byteBuffer, System.nanoTime() + REPLY_TIMEOUT_NANOS);
    }

    private static String errorMessage(JsonObject json) {
        JsonElement json2 = json.get("data");
        String string =
                DiscordIpc.string(
                        json2 != null && json2.isJsonObject() ? json2.getAsJsonObject() : json,
                        "message");
        return string != null ? string : json.toString();
    }

    private static String string(JsonObject json, String string) {
        JsonElement json2 = json.get(string);
        return json2 != null && json2.isJsonPrimitive() ? json2.getAsString() : null;
    }

    private static Pipe openWindowsPipe() {
        for (int i = 0; i < 10; ++i) {
            try {
                return new WindowsPipe(new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"));
            } catch (FileNotFoundException exception) {
                continue;
            }
        }
        return null;
    }

    private static Pipe openUnixSocket() {
        for (Path unixFolder : DiscordIpc.unixFolders()) {
            for (int i = 0; i < 10; ++i) {
                Path path = unixFolder.resolve("discord-ipc-" + i);
                if (!Files.exists(path, new LinkOption[0])) continue;
                try {
                    SocketChannel socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
                    try {
                        socketChannel.connect(UnixDomainSocketAddress.of(path));
                        socketChannel.configureBlocking(false);
                        return new UnixPipe(socketChannel);
                    } catch (IOException exception) {
                        socketChannel.close();
                        throw exception;
                    }
                } catch (IOException exception2) {
                    // empty catch block
                }
            }
        }
        return null;
    }

    private static Set<Path> unixFolders() {
        LinkedHashSet<Path> set = new LinkedHashSet<Path>();
        for (String string : UNIX_TEMP_VARIABLES) {
            String getenv = System.getenv(string);
            if (getenv == null || getenv.isBlank()) continue;
            try {
                set.add(Path.of(getenv, new String[0]));
            } catch (InvalidPathException exception) {
                // empty catch block
            }
        }
        set.add(Path.of("/tmp", new String[0]));
        LinkedHashSet set2 = new LinkedHashSet();
        for (Path path : set) {
            set2.add(path);
            DiscordIpc.addSubfolders(set2, path.resolve("app"), "*");
            DiscordIpc.addSubfolders(set2, path, "snap.*");
        }
        return set2;
    }

    private static void addSubfolders(Set<Path> set, Path path, String string) {
        if (!Files.isDirectory(path, new LinkOption[0])) {
            return;
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, string)) {
            for (Path path2 : directoryStream) {
                if (!Files.isDirectory(path2, new LinkOption[0])) continue;
                set.add(path2);
            }
        } catch (IOException | SecurityException exception) {
            // empty catch block
        }
    }

    @Environment(value = EnvType.CLIENT)
    private static interface Pipe extends Closeable {

        public int readWaiting(ByteBuffer byteBuffer) throws IOException;

        public void write(ByteBuffer byteBuffer, long l) throws IOException;
    }

    @Environment(value = EnvType.CLIENT)
    private record Frame(int opcode, byte[] body) {

        JsonObject json() throws IOException {
            try {
                JsonElement string =
                        JsonParser.parseString(new String(this.body, StandardCharsets.UTF_8));
                if (string.isJsonObject()) {
                    return string.getAsJsonObject();
                }
            } catch (JsonParseException exception) {
                // empty catch block
            }
            throw new IOException("Discord sent a frame that is not a JSON object");
        }
    }

    @Environment(value = EnvType.CLIENT)
    public static final class RefusedException extends Exception {

        RefusedException(String string) {
            super(string);
        }
    }

    @Environment(value = EnvType.CLIENT)
    private record WindowsPipe(RandomAccessFile file) implements Pipe {

        @Override
        public int readWaiting(ByteBuffer byteBuffer) throws IOException {
            long l = this.file.length();
            if (l <= 0L) {
                return 0;
            }
            int i =
                    this.file.read(
                            byteBuffer.array(),
                            byteBuffer.arrayOffset() + byteBuffer.position(),
                            (int) Math.min(l, (long) byteBuffer.remaining()));
            if (i < 0) {
                throw new EOFException("Discord hung up");
            }
            byteBuffer.position(byteBuffer.position() + i);
            return i;
        }

        @Override
        public void write(ByteBuffer byteBuffer, long l) throws IOException {
            this.file.write(
                    byteBuffer.array(),
                    byteBuffer.arrayOffset() + byteBuffer.position(),
                    byteBuffer.remaining());
            byteBuffer.position(byteBuffer.limit());
        }

        @Override
        public void close() throws IOException {
            this.file.close();
        }
    }

    @Environment(value = EnvType.CLIENT)
    private record UnixPipe(SocketChannel channel) implements Pipe {

        @Override
        public int readWaiting(ByteBuffer byteBuffer) throws IOException {
            int i = this.channel.read(byteBuffer);
            if (i < 0) {
                throw new EOFException("Discord hung up");
            }
            return i;
        }

        @Override
        public void write(ByteBuffer byteBuffer, long l) throws IOException {
            while (byteBuffer.hasRemaining()) {
                if (this.channel.write(byteBuffer) > 0) continue;
                if (System.nanoTime() - l >= 0L) {
                    throw new SocketTimeoutException("Discord stopped reading");
                }
                LockSupport.parkNanos(READ_POLL_NANOS);
            }
        }

        @Override
        public void close() throws IOException {
            this.channel.close();
        }
    }
}
