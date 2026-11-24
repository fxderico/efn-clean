package imgui.glfw;

import imgui.EfnAndroidNative;
import imgui.ImGui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharModsCallback;

@Environment(EnvType.CLIENT)
public class EfnGlfw extends ImGuiImplGlfw {

    private final boolean android;

    private GLFWCharModsCallback ownedCharMods;

    private GLFWCharModsCallback previousCharMods;

    private long charModsWindow;

    private boolean usingCharMods;

    private boolean reportedCharacters;

    private boolean wantedTextInput;

    public EfnGlfw() {
        this(EfnAndroidNative.isAndroidRuntime());
    }

    protected EfnGlfw(boolean android2) {
        this.android = android2;
    }

    public boolean init(long l, boolean bl) {
        boolean init = super.init(l, bl);
        if (init && this.android) {
            ImGui.getIO().removeBackendFlags(2);
            System.err.println("[EFN ImGui] Android cursor management delegated to launcher");
        }
        return init;
    }

    protected void updateMouseCursor() {
        if (!this.android) {
            super.updateMouseCursor();
        }
    }

    public void installCallbacks(long charModsWindow2) {
        super.installCallbacks(charModsWindow2);
        if (this.android) {
            this.ownedCharMods = GLFWCharModsCallback.create(this::charModsCallback);
            try {
                this.previousCharMods =
                        GLFW.glfwSetCharModsCallback(charModsWindow2, this.ownedCharMods);
                this.charModsWindow = charModsWindow2;
                this.usingCharMods = true;
                System.err.println(
                        "[EFN ImGui] Android text input: Minecraft character-with-modifiers channel"
                                + " enabled");
            } catch (LinkageError exception) {
                this.ownedCharMods.free();
                this.ownedCharMods = null;
                System.err.println(
                        "[EFN ImGui] Character-with-modifiers callback unavailable; keeping regular"
                                + " character input: "
                                + exception.getClass().getSimpleName());
            }
        }
    }

    public void charModsCallback(long l, int i, int j) {
        if (this.previousCharMods != null && this.shouldChainCallback(l)) {
            this.previousCharMods.invoke(l, i, j);
        }
        if (this.usingCharMods && l == this.charModsWindow) {
            ImGui.getIO().addInputCharacter(i);
            if (!this.reportedCharacters) {
                System.err.println("[EFN ImGui] Android text character received");
                this.reportedCharacters = true;
            }
        }
    }

    public void charCallback(long l, int i) {
        if (!this.usingCharMods || l != this.charModsWindow) {
            super.charCallback(l, i);
        } else if (this.data.prevUserCallbackChar != null && this.shouldChainCallback(l)) {
            this.data.prevUserCallbackChar.invoke(l, i);
        }
    }

    public void newFrame() {
        super.newFrame();
        boolean wantTextInput;
        if (this.usingCharMods
                && (wantTextInput = ImGui.getIO().getWantTextInput()) != this.wantedTextInput) {
            System.err.println(
                    "[EFN ImGui] Text field " + (wantTextInput ? "focused" : "unfocused"));
            this.wantedTextInput = wantTextInput;
        }
    }

    public void windowFocusCallback(long l, boolean bl) {
        if (!bl) {
            EfnKeys.clear(l);
        }
        super.windowFocusCallback(l, bl);
    }

    public void restoreCallbacks(long l) {
        EfnKeys.clear(l);
        if (this.usingCharMods && l == this.charModsWindow) {
            GLFW.glfwSetCharModsCallback(l, this.previousCharMods);
            this.usingCharMods = false;
            this.previousCharMods = null;
            this.charModsWindow = 0L;
            this.ownedCharMods.free();
            this.ownedCharMods = null;
            this.reportedCharacters = false;
            this.wantedTextInput = false;
        }
        super.restoreCallbacks(l);
    }
}
