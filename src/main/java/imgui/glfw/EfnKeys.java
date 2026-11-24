package imgui.glfw;

import imgui.EfnAndroidNative;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class EfnKeys {

    private static final boolean ANDROID = EfnAndroidNative.isAndroidRuntime();

    private static final int FIRST_KEY = 32;

    private static final int LAST_KEY = 348;

    private static final Map<Long, EfnKeys.State> STATES = new HashMap<>();

    private static final EfnKeys.KeyReader GLFW_READER = GLFW::glfwGetKey;

    private EfnKeys() {}

    public static boolean isAndroid() {
        return ANDROID;
    }

    public static int getKey(long l, int i) {
        return getKey(l, i, ANDROID, GLFW_READER);
    }

    public static void record(long l, int i, int j) {
        record(l, i, j, ANDROID);
    }

    public static void clear(long l) {
        Map map = STATES;
        synchronized (map) {
            STATES.remove(l);
        }
    }

    static int getKey(long l, int i, boolean bl, EfnKeys.KeyReader keyReader) {
        if (!bl) {
            return keyReader.get(l, i);
        } else if (i >= 32 && i <= 348) {
            Map map = STATES;
            synchronized (map) {
                EfnKeys.State state = STATES.get(l);
                if (state != null && state.unavailable[i]) {
                    return state.down[i] ? 1 : 0;
                }
            }
            try {
                int j = keyReader.get(l, i);
                return j != 1 && j != 2 ? 0 : 1;
            } catch (IndexOutOfBoundsException exception) {
                Map map2 = STATES;
                synchronized (map2) {
                    EfnKeys.State state2 = state(l);
                    state2.unavailable[i] = true;
                    return state2.down[i] ? 1 : 0;
                }
            }
        } else {
            return 0;
        }
    }

    static void record(long l, int i, int j, boolean bl) {
        if (bl && i >= 32 && i <= 348) {
            if (j == 1 || j == 0 || j == 2) {
                Map map = STATES;
                synchronized (map) {
                    state(l).down[i] = j != 0;
                }
            }
        }
    }

    private static EfnKeys.State state(long l) {
        EfnKeys.State state = STATES.get(l);
        if (state == null) {
            state = new EfnKeys.State();
            STATES.put(l, state);
        }
        return state;
    }

    @Environment(EnvType.CLIENT)
    interface KeyReader {

        int get(long l, int i);
    }

    @Environment(EnvType.CLIENT)
    private static final class State {

        final boolean[] down = new boolean[349];

        final boolean[] unavailable = new boolean[349];

        private State() {}
    }
}
