package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
final class LinuxSpotify extends ScriptedPlayer {

    private static final long TIMEOUT_MS = 2000L;

    private static final long LOOKUP_GAP_MS = 2000L;

    private static final String BUS_PREFIX = "org.mpris.MediaPlayer2.spotify";

    private static final String OBJECT_PATH = "/org/mpris/MediaPlayer2";

    private static final String PLAYER = "org.mpris.MediaPlayer2.Player";

    private String busName = "org.mpris.MediaPlayer2.spotify";

    private long nextLookupAt;

    LinuxSpotify() {}

    @Override
    protected ScriptedPlayer.Reading read() throws IOException, InterruptedException {
        String busName2 = this.busName(false);
        if (busName2 == null) {
            return null;
        } else {
            String run;
            try {
                run =
                        run(
                                2000L,
                                new String[] {
                                    "dbus-send",
                                    "--session",
                                    "--print-reply",
                                    "--dest=" + busName2,
                                    "/org/mpris/MediaPlayer2",
                                    "org.freedesktop.DBus.Properties.GetAll",
                                    "string:org.mpris.MediaPlayer2.Player"
                                });
            } catch (ScriptedPlayer.CommandFailed exception) {
                if (!gone(exception)) {
                    throw exception;
                }
                this.busName = null;
                return null;
            }
            if (!(LinuxSpotify.DbusReply.parse(run) instanceof Map map)) {
                throw new IOException("unexpected MPRIS reply");
            } else {
                Object object = map.get("Metadata");
                Map map2;
                Map map3 = object instanceof Map ? (map2 = (Map) object) : Map.of();
                String text = text(map.get("PlaybackStatus"));
                MediaSession.PlaybackState playbackState =
                        switch (text) {
                            case "Playing" -> MediaSession.PlaybackState.PLAYING;
                            case "Paused" -> MediaSession.PlaybackState.PAUSED;
                            default -> MediaSession.PlaybackState.STOPPED;
                        };
                return new ScriptedPlayer.Reading(
                        playbackState,
                        text(map3.get("xesam:title")),
                        text(map3.get("xesam:artist")),
                        text(map3.get("xesam:album")),
                        micros(map3.get("mpris:length")),
                        micros(map.get("Position")),
                        text(map3.get("mpris:artUrl")));
            }
        }
    }

    @Override
    protected boolean send(ScriptedPlayer.Command command)
            throws IOException, InterruptedException {
        String busName2 = this.busName(true);
        if (busName2 == null) {
            return false;
        } else {
            String string =
                    switch (command) {
                        case PLAY_PAUSE -> "PlayPause";
                        case NEXT -> "Next";
                        case PREVIOUS -> "Previous";
                    };
            try {
                run(
                        2000L,
                        new String[] {
                            "dbus-send",
                            "--session",
                            "--print-reply",
                            "--dest=" + busName2,
                            "/org/mpris/MediaPlayer2",
                            "org.mpris.MediaPlayer2.Player." + string
                        });
                return true;
            } catch (ScriptedPlayer.CommandFailed exception) {
                if (!gone(exception)) {
                    throw exception;
                } else {
                    this.busName = null;
                    return false;
                }
            }
        }
    }

    private String busName(boolean bl) throws IOException, InterruptedException {
        if (this.busName != null) {
            return this.busName;
        } else {
            long l = System.currentTimeMillis();
            if (!bl && l < this.nextLookupAt) {
                return null;
            } else {
                this.nextLookupAt = l + 2000L;
                String run =
                        run(
                                2000L,
                                new String[] {
                                    "dbus-send",
                                    "--session",
                                    "--print-reply",
                                    "--dest=org.freedesktop.DBus",
                                    "/org/freedesktop/DBus",
                                    "org.freedesktop.DBus.ListNames"
                                });
                Object object = LinuxSpotify.DbusReply.parse(run);
                if (object instanceof List) {
                    for (Object object2 : (List) object) {
                        String string;
                        if (object2 instanceof String
                                && (string = (String) object2)
                                        .startsWith("org.mpris.MediaPlayer2.spotify")) {
                            this.busName = string;
                            return string;
                        }
                    }
                }
                return null;
            }
        }
    }

    private static boolean gone(ScriptedPlayer.CommandFailed commandFailed) {
        String message = commandFailed.getMessage();
        return message != null
                && (message.contains("ServiceUnknown") || message.contains("NameHasNoOwner"));
    }

    private static String text(Object object) {
        if (object instanceof String string) {
            return string.strip();
        } else if (object instanceof List list) {
            ArrayList list2 = new ArrayList();
            for (Object object2 : list) {
                String text = text(object2);
                if (!text.isEmpty()) {
                    list2.add(text);
                }
            }
            return String.join(", ", list2);
        } else {
            return "";
        }
    }

    private static long micros(Object object) {
        long l;
        if (object instanceof Number number) {
            l = Math.max(0L, number.longValue() / 1000L);
        } else {
            l = 0L;
        }
        return l;
    }

    @Environment(EnvType.CLIENT)
    static final class DbusReply {

        private final String[] lines;

        private int at;

        private DbusReply(String[] lines2) {
            this.lines = lines2;
        }

        static Object parse(String string) {
            String[] strings = string.strip().split("\\R");
            LinuxSpotify.DbusReply dbusReply = new LinuxSpotify.DbusReply(strings);
            if (strings.length > 0 && strings[0].startsWith("method return")) {
                dbusReply.at = 1;
            }
            return dbusReply.at < strings.length ? dbusReply.value(dbusReply.next()) : null;
        }

        private String next() {
            return this.at < this.lines.length ? this.lines[this.at++].strip() : "]";
        }

        private Object value(String string) {
            if (string.startsWith("variant")) {
                string = string.substring("variant".length()).strip();
            }
            if (string.equals("array [")) {
                return this.array();
            } else if (!string.startsWith("string ") && !string.startsWith("object path ")) {
                int i = string.lastIndexOf(32);
                String string2 = i < 0 ? string : string.substring(0, i);
                String string3 = i < 0 ? "" : string.substring(i + 1);
                try {
                    return switch (string2) {
                        case "int64", "uint64", "int32", "uint32", "int16", "uint16", "byte" ->
                                Long.parseLong(string3);
                        case "double" -> Double.parseDouble(string3);
                        case "boolean" -> Boolean.parseBoolean(string3);
                        default -> string;
                    };
                } catch (NumberFormatException exception) {
                    return string;
                }
            } else {
                return unquote(string);
            }
        }

        private Object array() {
            ArrayList list = new ArrayList();
            LinkedHashMap map = new LinkedHashMap();
            String string;
            while (this.at < this.lines.length && !(string = this.next()).equals("]")) {
                if (string.equals("dict entry(")) {
                    Object object = this.value(this.next());
                    Object object2 = this.value(this.next());
                    this.next();
                    if (object instanceof String string2) {
                        map.put(string2, object2);
                    }
                } else {
                    list.add(this.value(string));
                }
            }
            return map.isEmpty() ? list : map;
        }

        private static String unquote(String string) {
            int i = string.indexOf(34);
            int j = string.lastIndexOf(34);
            return i >= 0 && j > i ? string.substring(i + 1, j).replace("\\\"", "\"") : "";
        }
    }
}
