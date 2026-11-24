package net.efn.client.util.spotify;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.IntByReference;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class SpotifyWindow {

    private static final String EXECUTABLE = "spotify.exe";

    private static final String MAIN_CLASS = "Chrome_WidgetWin_0";

    private static final String CLASS_FAMILY = "Chrome_WidgetWin";

    private static final long PID_CACHE_MS = 30000L;

    private static final int PID_CACHE_MAX = 512;

    private static final long RECHECK_MS = 10000L;

    private static final String TRACK_SEPARATOR = " - ";

    private static final int KEYEVENTF_EXTENDEDKEY = 1;

    private static final int KEYEVENTF_KEYUP = 2;

    private final char[] textBuffer = new char[512];

    private final char[] classBuffer = new char[64];

    private final char[] pathBuffer = new char[1024];

    private final IntByReference pidOut = new IntByReference();

    private final IntByReference sizeInOut = new IntByReference();

    private final Map<Integer, Boolean> spotifyPids = new HashMap<>();

    private long pidCacheSince;

    private HWND window;

    private long foundAt;

    private HWND best;

    private int bestScore;

    private final WNDENUMPROC finder = this::consider;

    public SpotifyWindow() {}

    public String readTitle() {
        long l = System.currentTimeMillis();
        String text;
        if (this.window == null
                || !User32.INSTANCE.IsWindow(this.window)
                || !this.isSpotify(this.processOf(this.window))
                || !(text = this.text(this.window)).contains(" - ")
                        && (text.isEmpty() || l - this.foundAt >= 10000L)) {
            this.window = this.find();
            this.foundAt = l;
            return this.window == null ? null : this.text(this.window);
        } else {
            return text;
        }
    }

    public static void tap(SpotifyWindow.MediaKey mediaKey) {
        send(mediaKey.virtualKey, 1);
        send(mediaKey.virtualKey, 3);
    }

    private HWND find() {
        long l = System.currentTimeMillis();
        if (l - this.pidCacheSince > 30000L || this.spotifyPids.size() > 512) {
            this.spotifyPids.clear();
            this.pidCacheSince = l;
        }
        this.best = null;
        this.bestScore = 0;
        User32.INSTANCE.EnumWindows(this.finder, Pointer.NULL);
        HWND best2 = this.best;
        this.best = null;
        return best2;
    }

    private boolean consider(HWND best2, Pointer pointer) {
        if (!this.isSpotify(this.processOf(best2))) {
            return true;
        } else {
            String text = this.text(best2);
            if (!text.isEmpty()
                    && !text.equals("Default IME")
                    && !text.equals("MSCTFIME UI")
                    && !text.startsWith("GDI+ Window")) {
                String className = this.className(best2);
                int i = 1;
                if (className.equals("Chrome_WidgetWin_0")) {
                    i += 4;
                } else if (className.startsWith("Chrome_WidgetWin")) {
                    i += 2;
                }
                if (text.contains(" - ")) {
                    i += 3;
                } else if (text.startsWith("Spotify")) {
                    i++;
                }
                if (User32.INSTANCE.IsWindowVisible(best2)) {
                    i++;
                }
                if (i > this.bestScore) {
                    this.bestScore = i;
                    this.best = best2;
                }
                return true;
            } else {
                return true;
            }
        }
    }

    private int processOf(HWND hWND) {
        User32.INSTANCE.GetWindowThreadProcessId(hWND, this.pidOut);
        return this.pidOut.getValue();
    }

    private boolean isSpotify(int i) {
        if (i <= 0) {
            return false;
        } else {
            Boolean spotifyPid = this.spotifyPids.get(i);
            if (spotifyPid != null) {
                return spotifyPid;
            } else {
                boolean bl = "spotify.exe".equals(this.imageName(i));
                this.spotifyPids.put(i, bl);
                return bl;
            }
        }
    }

    private String imageName(int i) {
        HANDLE hANDLE = Kernel32.INSTANCE.OpenProcess(4096, false, i);
        if (hANDLE == null) {
            return "";
        } else {
            String string;
            try {
                this.sizeInOut.setValue(this.pathBuffer.length);
                if (Kernel32.INSTANCE.QueryFullProcessImageName(
                        hANDLE, 0, this.pathBuffer, this.sizeInOut)) {
                    String string2 =
                            new String(
                                    this.pathBuffer,
                                    0,
                                    Math.min(this.sizeInOut.getValue(), this.pathBuffer.length));
                    int j = string2.lastIndexOf(92);
                    return string2.substring(j + 1).toLowerCase(Locale.ROOT);
                }
                String string3 = "";
                string = string3;
            } finally {
                Kernel32.INSTANCE.CloseHandle(hANDLE);
            }
            return string;
        }
    }

    private String text(HWND hWND) {
        int getWindowText =
                User32.INSTANCE.GetWindowText(hWND, this.textBuffer, this.textBuffer.length);
        return getWindowText <= 0
                ? ""
                : new String(this.textBuffer, 0, Math.min(getWindowText, this.textBuffer.length));
    }

    private String className(HWND hWND) {
        int getClassName =
                User32.INSTANCE.GetClassName(hWND, this.classBuffer, this.classBuffer.length);
        return getClassName <= 0
                ? ""
                : new String(this.classBuffer, 0, Math.min(getClassName, this.classBuffer.length));
    }

    private static void send(int i, int j) {
        INPUT iNPUT = new INPUT();
        iNPUT.type = new DWORD(1L);
        iNPUT.input.setType("ki");
        iNPUT.input.ki.wVk = new WORD(i);
        iNPUT.input.ki.wScan = new WORD(0L);
        iNPUT.input.ki.dwFlags = new DWORD(j);
        iNPUT.input.ki.time = new DWORD(0L);
        User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] {iNPUT}, iNPUT.size());
    }

    @Environment(EnvType.CLIENT)
    public static enum MediaKey {
        PLAY_PAUSE(179),
        NEXT(176),
        PREVIOUS(177);

        final int virtualKey;

        private MediaKey(int virtualKey2) {
            this.virtualKey = virtualKey2;
        }
    }
}
