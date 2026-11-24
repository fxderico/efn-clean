package net.efn.client.module.impl.misc.noinvleak;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinUser.BLENDFUNCTION;
import com.sun.jna.platform.win32.WinUser.SIZE;
import com.sun.jna.platform.win32.WinUser.WNDCLASSEX;
import com.sun.jna.platform.win32.WinUser.WindowProc;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.glfw.GLFWNativeWin32;

import java.nio.ByteOrder;
import java.nio.IntBuffer;

@Environment(EnvType.CLIENT)
final class OverlayWindow {

    private static final int FIRST_EXCLUDING_BUILD = 19041;

    private static final int ERROR_CLASS_ALREADY_EXISTS = 1410;

    private static int build = -1;

    private static final String CLASS_NAME = "EfnNoInvLeakOverlay";

    private static final int WS_EX_TOOLWINDOW = 128;

    private static final int WS_EX_NOACTIVATE = 134217728;

    private static final int WDA_EXCLUDEFROMCAPTURE = 17;

    private static final WindowProc PROC =
            (hWND, i, wPARAM, lPARAM) -> User32.INSTANCE.DefWindowProc(hWND, i, wPARAM, lPARAM);

    private static boolean classRegistered;

    private final POINT destination = new POINT();

    private final POINT source = new POINT(0, 0);

    private final SIZE size = new SIZE();

    private final BLENDFUNCTION blend = new BLENDFUNCTION();

    private HWND hwnd;

    private HDC memory;

    private HBITMAP bitmap;

    private HANDLE previousBitmap;

    private Pointer bitsPointer;

    private IntBuffer bits;

    private int capacityWidth;

    private int capacityHeight;

    private boolean visible;

    OverlayWindow(long l) {
        int windowsBuild = windowsBuild();
        if (windowsBuild > 0 && windowsBuild < 19041) {
            throw new OverlayWindow.Unsupported(
                    "Windows build "
                            + windowsBuild
                            + " cannot hide a window from capture (needs 19041, Windows 10 2004, or"
                            + " newer)");
        } else {
            HMODULE hMODULE = Kernel32.INSTANCE.GetModuleHandle(null);
            if (!classRegistered) {
                WNDCLASSEX wNDCLASSEX = new WNDCLASSEX();
                wNDCLASSEX.lpfnWndProc = PROC;
                wNDCLASSEX.hInstance = hMODULE;
                wNDCLASSEX.lpszClassName = "EfnNoInvLeakOverlay";
                if (User32.INSTANCE.RegisterClassEx(wNDCLASSEX).intValue() == 0
                        && Native.getLastError() != 1410) {
                    throw failure("RegisterClassEx");
                }
                classRegistered = true;
            }
            HWND hWND = new HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(l)));
            this.hwnd =
                    User32.INSTANCE.CreateWindowEx(
                            134742176,
                            "EfnNoInvLeakOverlay",
                            "",
                            -2147483648,
                            0,
                            0,
                            1,
                            1,
                            hWND,
                            null,
                            hMODULE,
                            null);
            if (this.hwnd == null) {
                throw failure("CreateWindowEx");
            } else if (!OverlayWindow.Affinity.INSTANCE.SetWindowDisplayAffinity(this.hwnd, 17)) {
                int lastError = Native.getLastError();
                this.destroy();
                throw new OverlayWindow.Unsupported(
                        "SetWindowDisplayAffinity failed with error " + lastError);
            } else {
                IntByReference intByReference = new IntByReference();
                if (OverlayWindow.Affinity.INSTANCE.GetWindowDisplayAffinity(
                                this.hwnd, intByReference)
                        && intByReference.getValue() != 17) {
                    int intByReference2 = intByReference.getValue();
                    this.destroy();
                    throw new OverlayWindow.Unsupported(
                            "capture exclusion did not stick (affinity reads back as 0x"
                                    + Integer.toHexString(intByReference2)
                                    + ")");
                } else {
                    this.blend.BlendOp = 0;
                    this.blend.SourceConstantAlpha = -1;
                    this.blend.AlphaFormat = 1;
                }
            }
        }
    }

    IntBuffer pixels(int i, int j) {
        if (i > this.capacityWidth || j > this.capacityHeight) {
            int k = Math.max(i, this.capacityWidth);
            int m = Math.max(j, this.capacityHeight);
            this.releaseBitmap();
            BITMAPINFO bITMAPINFO = new BITMAPINFO();
            bITMAPINFO.bmiHeader.biWidth = k;
            bITMAPINFO.bmiHeader.biHeight = -m;
            bITMAPINFO.bmiHeader.biPlanes = 1;
            bITMAPINFO.bmiHeader.biBitCount = 32;
            bITMAPINFO.bmiHeader.biCompression = 0;
            PointerByReference pointerByReference = new PointerByReference();
            this.bitmap =
                    GDI32.INSTANCE.CreateDIBSection(
                            null, bITMAPINFO, 0, pointerByReference, null, 0);
            if (this.bitmap == null) {
                throw failure("CreateDIBSection");
            }
            if (this.memory == null) {
                this.memory = GDI32.INSTANCE.CreateCompatibleDC(null);
                if (this.memory == null) {
                    throw failure("CreateCompatibleDC");
                }
            }
            this.previousBitmap = GDI32.INSTANCE.SelectObject(this.memory, this.bitmap);
            if (this.previousBitmap == null) {
                throw failure("SelectObject");
            }
            this.capacityWidth = k;
            this.capacityHeight = m;
            this.bitsPointer = pointerByReference.getValue();
            this.bits =
                    this.bitsPointer
                            .getByteBuffer(0L, (long) k * m * 4L)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .asIntBuffer();
        }
        this.bitsPointer.setMemory(0L, (long) this.capacityWidth * j * 4L, (byte) 0);
        return this.bits;
    }

    int stride() {
        return this.capacityWidth;
    }

    void present(int i, int j, int k, int m) {
        this.destination.x = i;
        this.destination.y = j;
        this.size.cx = k;
        this.size.cy = m;
        if (!User32.INSTANCE.UpdateLayeredWindow(
                this.hwnd,
                null,
                this.destination,
                this.size,
                this.memory,
                this.source,
                0,
                this.blend,
                2)) {
            throw failure("UpdateLayeredWindow");
        } else {
            if (!this.visible) {
                User32.INSTANCE.ShowWindow(this.hwnd, 4);
                this.visible = true;
            }
        }
    }

    void move(int i, int j) {
        User32.INSTANCE.SetWindowPos(this.hwnd, null, i, j, 0, 0, 21);
    }

    void hide() {
        if (this.visible) {
            User32.INSTANCE.ShowWindow(this.hwnd, 0);
            this.visible = false;
        }
    }

    void destroy() {
        this.releaseBitmap();
        if (this.memory != null) {
            GDI32.INSTANCE.DeleteDC(this.memory);
            this.memory = null;
        }
        if (this.hwnd != null) {
            User32.INSTANCE.DestroyWindow(this.hwnd);
            this.hwnd = null;
        }
        this.visible = false;
    }

    private void releaseBitmap() {
        if (this.bitmap != null) {
            if (this.previousBitmap != null) {
                GDI32.INSTANCE.SelectObject(this.memory, this.previousBitmap);
            }
            GDI32.INSTANCE.DeleteObject(this.bitmap);
            this.bitmap = null;
            this.previousBitmap = null;
            this.bitsPointer = null;
            this.bits = null;
            this.capacityWidth = 0;
            this.capacityHeight = 0;
        }
    }

    String describe() {
        if (this.hwnd == null) {
            return "no window";
        } else {
            IntByReference intByReference = new IntByReference(-1);
            boolean getWindowDisplayAffinity =
                    OverlayWindow.Affinity.INSTANCE.GetWindowDisplayAffinity(
                            this.hwnd, intByReference);
            int getDpiForWindow;
            try {
                getDpiForWindow = OverlayWindow.Affinity.INSTANCE.GetDpiForWindow(this.hwnd);
            } catch (UnsatisfiedLinkError exception) {
                getDpiForWindow = -1;
            }
            return "affinity 0x"
                    + (getWindowDisplayAffinity
                            ? Integer.toHexString(intByReference.getValue())
                            : "?")
                    + ", dpi "
                    + getDpiForWindow
                    + ", visible "
                    + User32.INSTANCE.IsWindowVisible(this.hwnd);
        }
    }

    static int windowsBuild() {
        if (build >= 0) {
            return build;
        } else {
            try {
                build =
                        Integer.parseInt(
                                Advapi32Util.registryGetStringValue(
                                                WinReg.HKEY_LOCAL_MACHINE,
                                                "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                                                "CurrentBuildNumber")
                                        .trim());
            } catch (RuntimeException | LinkageError exception) {
                build = 0;
            }
            return build;
        }
    }

    private static RuntimeException failure(String string) {
        return new IllegalStateException(string + " failed with error " + Native.getLastError());
    }

    @Environment(EnvType.CLIENT)
    private interface Affinity extends StdCallLibrary {

        OverlayWindow.Affinity INSTANCE =
                Native.load("user32", OverlayWindow.Affinity.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean SetWindowDisplayAffinity(HWND hWND, int i);

        boolean GetWindowDisplayAffinity(HWND hWND, IntByReference intByReference);

        int GetDpiForWindow(HWND hWND);
    }

    @Environment(EnvType.CLIENT)
    static final class Unsupported extends RuntimeException {

        Unsupported(String string) {
            super(string);
        }
    }
}
