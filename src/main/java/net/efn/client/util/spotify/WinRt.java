package net.efn.client.util.spotify;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayDeque;
import java.util.Locale;

@Environment(EnvType.CLIENT)
final class WinRt {

    static final int FIRST_SLOT = 6;

    static final int RO_INIT_MULTITHREADED = 1;

    static final int RPC_E_CHANGED_MODE = -2147417850;

    static final int REGDB_E_CLASSNOTREG = -2147221164;

    static final int E_NOINTERFACE = -2147467262;

    static final int E_ABORT = -2147467260;

    static final int E_TIMEOUT = -2147023436;

    private static final int SLOT_QUERY_INTERFACE = 0;

    private static final int SLOT_RELEASE = 2;

    private static final int ASYNC_OPERATION_GET_RESULTS = 8;

    private static final GUID IID_ASYNC_INFO = iid("00000036-0000-0000-C000-000000000046");

    private static final int ASYNC_INFO_GET_STATUS = 7;

    private static final int ASYNC_INFO_GET_ERROR_CODE = 8;

    private static final int ASYNC_INFO_CANCEL = 9;

    private static final int ASYNC_STARTED = 0;

    private static final int ASYNC_COMPLETED = 1;

    private static final int ASYNC_CANCELED = 2;

    private static final long MAX_WAIT_STEP_MS = 5L;

    private WinRt() {}

    static GUID iid(String string) {
        GUID gUID = new GUID(string);
        gUID.write();
        return gUID;
    }

    static int initializeMultithreaded() {
        return WinRt.Combase.INSTANCE.RoInitialize(1);
    }

    static void uninitialize() {
        WinRt.Combase.INSTANCE.RoUninitialize();
    }

    static Pointer activationFactory(String string, GUID gUID) {
        Pointer string2 = createString(string);
        Pointer pointer;
        try {
            PointerByReference pointerByReference = new PointerByReference();
            check(
                    WinRt.Combase.INSTANCE.RoGetActivationFactory(
                            string2, gUID.getPointer(), pointerByReference),
                    "RoGetActivationFactory(" + string + ")");
            Pointer pointer2 =
                    owned(pointerByReference.getValue(), "RoGetActivationFactory(" + string + ")");
            pointer = pointer2;
        } finally {
            WinRt.Combase.INSTANCE.WindowsDeleteString(string2);
        }
        return pointer;
    }

    static int invoke(Pointer pointer, int i, Object... object) {
        Pointer pointer2 = pointer.getPointer(0L);
        Function function =
                Function.getFunction(pointer2.getPointer((long) i * Native.POINTER_SIZE), 63);
        Object[] objects = new Object[object.length + 1];
        objects[0] = pointer;
        System.arraycopy(object, 0, objects, 1, object.length);
        return function.invokeInt(objects);
    }

    static void check(int i, String string) {
        if (i < 0) {
            throw new WinRt.WinRtException(string, i);
        }
    }

    static void release(Pointer pointer) {
        if (pointer != null) {
            invoke(pointer, 2);
        }
    }

    static Pointer queryInterface(Pointer pointer, GUID gUID, String string) {
        PointerByReference pointerByReference = new PointerByReference();
        check(invoke(pointer, 0, gUID.getPointer(), pointerByReference), string);
        return owned(pointerByReference.getValue(), string);
    }

    static Pointer tryQueryInterface(Pointer pointer, GUID gUID) {
        PointerByReference pointerByReference = new PointerByReference();
        return invoke(pointer, 0, gUID.getPointer(), pointerByReference) < 0
                ? null
                : pointerByReference.getValue();
    }

    static Pointer callForObject(Pointer pointer, int i, String string, Object... object) {
        PointerByReference pointerByReference = new PointerByReference();
        check(invoke(pointer, i, append(object, pointerByReference)), string);
        return pointerByReference.getValue();
    }

    static String getString(Pointer pointer, int i, String string) {
        PointerByReference pointerByReference = new PointerByReference();
        check(invoke(pointer, i, pointerByReference), string);
        Pointer pointerByReference2 = pointerByReference.getValue();
        if (pointerByReference2 == null) {
            return "";
        } else {
            String string2;
            try {
                String string3 = readString(pointerByReference2);
                string2 = string3;
            } finally {
                WinRt.Combase.INSTANCE.WindowsDeleteString(pointerByReference2);
            }
            return string2;
        }
    }

    static int getInt32(Pointer pointer, int i, String string) {
        IntByReference intByReference = new IntByReference();
        check(invoke(pointer, i, intByReference), string);
        return intByReference.getValue();
    }

    static long getInt64(Pointer pointer, int i, String string) {
        LongByReference longByReference = new LongByReference();
        check(invoke(pointer, i, longByReference), string);
        return longByReference.getValue();
    }

    static double getDouble(Pointer pointer, int i, String string) {
        DoubleByReference doubleByReference = new DoubleByReference();
        check(invoke(pointer, i, doubleByReference), string);
        return doubleByReference.getValue();
    }

    static Pointer awaitObject(Pointer pointer, long l, String string) {
        await(pointer, l, string);
        PointerByReference pointerByReference = new PointerByReference();
        check(invoke(pointer, 8, pointerByReference), string + " results");
        return pointerByReference.getValue();
    }

    static boolean awaitBoolean(Pointer pointer, long l, String string) {
        await(pointer, l, string);
        ByteByReference byteByReference = new ByteByReference();
        check(invoke(pointer, 8, byteByReference), string + " results");
        return byteByReference.getValue() != 0;
    }

    static int awaitUInt32(Pointer pointer, long l, String string) {
        await(pointer, l, string);
        IntByReference intByReference = new IntByReference();
        check(invoke(pointer, 8, intByReference), string + " results");
        return intByReference.getValue();
    }

    private static void await(Pointer pointer, long l, String string) {
        if (pointer == null) {
            throw new WinRt.WinRtException(string + " returned no operation", -2147467260);
        } else {
            Pointer pointer2 = queryInterface(pointer, IID_ASYNC_INFO, string + " IAsyncInfo");
            try {
                long l2 = System.nanoTime() + l * 1000000L;
                int int32;
                for (long l3 = 1L;
                        (int32 = getInt32(pointer2, 7, string + " status")) != 1;
                        l3 = Math.min(l3 + 1L, 5L)) {
                    if (int32 == 2) {
                        throw new WinRt.WinRtException(string + " canceled", -2147467260);
                    }
                    if (int32 != 0) {
                        IntByReference intByReference = new IntByReference();
                        int i = invoke(pointer2, 8, intByReference);
                        throw new WinRt.WinRtException(
                                string, i < 0 ? i : intByReference.getValue());
                    }
                    if (System.nanoTime() - l2 >= 0L) {
                        invoke(pointer2, 9);
                        throw new WinRt.WinRtException(string + " timed out", -2147023436);
                    }
                    try {
                        Thread.sleep(l3);
                    } catch (InterruptedException exception) {
                        invoke(pointer2, 9);
                        Thread.currentThread().interrupt();
                        throw new WinRt.WinRtException(string + " interrupted", -2147467260);
                    }
                }
            } finally {
                release(pointer2);
            }
        }
    }

    private static Pointer createString(String string) {
        PointerByReference pointerByReference = new PointerByReference();
        check(
                WinRt.Combase.INSTANCE.WindowsCreateString(
                        new WString(string), string.length(), pointerByReference),
                "WindowsCreateString");
        return pointerByReference.getValue();
    }

    private static String readString(Pointer pointer) {
        IntByReference intByReference = new IntByReference();
        Pointer pointer2 =
                WinRt.Combase.INSTANCE.WindowsGetStringRawBuffer(pointer, intByReference);
        int intByReference2 = intByReference.getValue();
        return pointer2 != null && intByReference2 > 0
                ? new String(pointer2.getCharArray(0L, intByReference2))
                : "";
    }

    private static Pointer owned(Pointer pointer, String string) {
        if (pointer == null) {
            throw new WinRt.WinRtException(string + " returned null", -2147467262);
        } else {
            return pointer;
        }
    }

    private static Object[] append(Object[] objects, Object object) {
        Object[] objects2 = new Object[objects.length + 1];
        System.arraycopy(objects, 0, objects2, 0, objects.length);
        objects2[objects.length] = object;
        return objects2;
    }

    @Environment(EnvType.CLIENT)
    interface Combase extends StdCallLibrary {

        WinRt.Combase INSTANCE = Native.load("combase", WinRt.Combase.class);

        int RoInitialize(int i);

        void RoUninitialize();

        int RoGetActivationFactory(
                Pointer pointer, Pointer pointer2, PointerByReference pointerByReference);

        int WindowsCreateString(WString wString, int i, PointerByReference pointerByReference);

        int WindowsDeleteString(Pointer pointer);

        Pointer WindowsGetStringRawBuffer(Pointer pointer, IntByReference intByReference);
    }

    @Environment(EnvType.CLIENT)
    static final class Refs implements AutoCloseable {

        private final ArrayDeque<Pointer> held = new ArrayDeque<>();

        Refs() {}

        Pointer add(Pointer pointer) {
            if (pointer != null) {
                this.held.push(pointer);
            }
            return pointer;
        }

        @Override
        public void close() {
            while (!this.held.isEmpty()) {
                WinRt.release(this.held.pop());
            }
        }
    }

    @Environment(EnvType.CLIENT)
    static final class WinRtException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        final int hresult;

        WinRtException(String string, int hresult2) {
            super(
                    string
                            + " failed (0x"
                            + Integer.toHexString(hresult2).toUpperCase(Locale.ROOT)
                            + ")",
                    null,
                    false,
                    false);
            this.hresult = hresult2;
        }
    }
}
