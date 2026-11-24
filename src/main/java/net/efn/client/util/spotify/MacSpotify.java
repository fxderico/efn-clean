package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.util.Optional;

@Environment(EnvType.CLIENT)
final class MacSpotify extends ScriptedPlayer {

    private static final long TIMEOUT_MS = 2000L;

    private static final String APP_BINARY = "/Contents/MacOS/Spotify";

    private static final String NOT_RUNNING = "not running";

    private static final String STOPPED = "stopped";

    private static final char FIELD = '\u001f';

    private static final int FIELDS = 7;

    private static final String[] READ =
            new String[] {
                "osascript",
                "-e",
                "if application \"Spotify\" is not running then return \"not running\"",
                "-e",
                "tell application \"Spotify\"",
                "-e",
                "set s to (player state) as text",
                "-e",
                "if s is \"stopped\" then return \"stopped\"",
                "-e",
                "set t to current track",
                "-e",
                "set d to character id 31",
                "-e",
                "return s & d & (name of t) & d & (artist of t) & d & (album of t) & d & (duration"
                        + " of t) & d & (player position) & d & (artwork url of t)",
                "-e",
                "end tell"
            };

    MacSpotify() {}

    @Override
    protected ScriptedPlayer.Reading read() throws IOException, InterruptedException {
        if (!appRunning()) {
            return null;
        } else {
            String string = run(2000L, READ).strip();
            if (string.equals("not running")) {
                return null;
            } else if (string.equals("stopped")) {
                return new ScriptedPlayer.Reading(
                        MediaSession.PlaybackState.STOPPED, "", "", "", 0L, 0L, "");
            } else {
                String[] strings = string.split(String.valueOf('\u001f'), -1);
                if (strings.length < 7) {
                    throw new IOException("unexpected Spotify reply: " + string);
                } else {
                    MediaSession.PlaybackState playbackState =
                            strings[0].equals("playing")
                                    ? MediaSession.PlaybackState.PLAYING
                                    : MediaSession.PlaybackState.PAUSED;
                    return new ScriptedPlayer.Reading(
                            playbackState,
                            strings[1].strip(),
                            strings[2].strip(),
                            strings[3].strip(),
                            Math.max(0L, (long) number(strings[4])),
                            Math.max(0L, Math.round(number(strings[5]) * 1000.0)),
                            strings[6].strip());
                }
            }
        }
    }

    @Override
    protected boolean send(ScriptedPlayer.Command command)
            throws IOException, InterruptedException {
        if (!appRunning()) {
            return false;
        } else {
            String string =
                    switch (command) {
                        case PLAY_PAUSE -> "playpause";
                        case NEXT -> "next track";
                        case PREVIOUS -> "previous track";
                    };
            run(
                    2000L,
                    new String[] {
                        "osascript",
                        "-e",
                        "if application \"Spotify\" is running then tell application \"Spotify\" to"
                                + " "
                                + string
                    });
            return true;
        }
    }

    private static boolean appRunning() {
        return ProcessHandle.allProcesses()
                .anyMatch(
                        processHandle -> {
                            Optional optional = processHandle.info().command();
                            return optional.isPresent()
                                    && ((String) optional.get())
                                            .endsWith("/Contents/MacOS/Spotify");
                        });
    }

    private static double number(String string) {
        try {
            return Double.parseDouble(string.strip().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }
}
