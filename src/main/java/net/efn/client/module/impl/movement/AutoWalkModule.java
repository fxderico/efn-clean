package net.efn.client.module.impl.movement;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkSectionPos;

import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class AutoWalkModule extends ClientModule {

    private final ModeSetting direction =
            this.addSetting(
                    "simple-direction",
                    new ModeSetting(
                            "Simple Direction",
                            "Forwards",
                            "Forwards",
                            "Backwards",
                            "Left",
                            "Right"));

    private final BooleanSetting disableOnInput =
            this.addSetting("disable-on-input", new BooleanSetting("Disable On Input", false));

    private final BooleanSetting disableOnY =
            this.addSetting(
                    "disable-on-y-change", new BooleanSetting("Disable On Y Change", false));

    private final BooleanSetting waitForChunks =
            this.addSetting("no-unloaded-chunks", new BooleanSetting("No Unloaded Chunks", true));

    public AutoWalkModule() {
        super("auto-walk", "Auto Walk", "Automatically walks forward.", ModuleCategory.MOVEMENT);
        this.direction.onChange(
                () -> {
                    if (this.isEnabled()) {
                        releaseKeys();
                    }
                });
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onPreTick);
        this.on(KeyInputEvent.TYPE, this::onKey);
        this.on(MouseInputEvent.TYPE, this::onMouseButton);
        this.on(PlayerMoveEvent.TYPE, this::onPlayerMove);
    }

    @Override
    protected void onDisable() {
        releaseKeys();
    }

    private void onPreTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            if (this.disableOnY.get() && player.lastY != player.getY()) {
                this.requestDisable();
            } else {
                this.keyFor(client.options).setPressed(true);
            }
        }
    }

    private void onKey(KeyInputEvent event) {
        if (event.action == 1 && this.disableOnInput.get()) {
            if (event.key != -1) {
                GameOptions options = MinecraftClient.getInstance().options;
                KeyInput keyInput = new KeyInput(event.key, event.scancode, event.mods);
                if (options.forwardKey.matchesKey(keyInput)
                        || options.backKey.matchesKey(keyInput)
                        || options.leftKey.matchesKey(keyInput)
                        || options.rightKey.matchesKey(keyInput)
                        || options.sneakKey.matchesKey(keyInput)
                        || options.jumpKey.matchesKey(keyInput)) {
                    this.onMovementInput();
                }
            }
        }
    }

    private void onMouseButton(MouseInputEvent event) {
        if (event.action == 1 && this.disableOnInput.get()) {
            GameOptions options = MinecraftClient.getInstance().options;
            Click click = new Click(0.0, 0.0, new MouseInput(event.button, event.mods));
            if (options.forwardKey.matchesMouse(click)
                    || options.backKey.matchesMouse(click)
                    || options.leftKey.matchesMouse(click)
                    || options.rightKey.matchesMouse(click)
                    || options.sneakKey.matchesMouse(click)
                    || options.jumpKey.matchesMouse(click)) {
                this.onMovementInput();
            }
        }
    }

    private void onMovementInput() {
        if (MinecraftClient.getInstance().currentScreen == null) {
            this.requestDisable();
        }
    }

    private void onPlayerMove(PlayerMoveEvent event) {
        if (this.waitForChunks.get()) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            ClientWorld world = client.world;
            if (player != null && world != null) {
                int sectionCoordFloored =
                        ChunkSectionPos.getSectionCoordFloored(player.getX() + event.x * 2.0);
                int sectionCoordFloored2 =
                        ChunkSectionPos.getSectionCoordFloored(player.getZ() + event.z * 2.0);
                if (!world.getChunkManager()
                        .isChunkLoaded(sectionCoordFloored, sectionCoordFloored2)) {
                    event.x = 0.0;
                    event.z = 0.0;
                }
            }
        }
    }

    private KeyBinding keyFor(GameOptions gameOptions) {
        String direction2 = this.direction.get();
        return switch (direction2) {
            case "Backwards" -> gameOptions.backKey;
            case "Left" -> gameOptions.leftKey;
            case "Right" -> gameOptions.rightKey;
            default -> gameOptions.forwardKey;
        };
    }

    private static void releaseKeys() {
        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions options = client.options;
        if (options != null) {
            boolean bl = client.currentScreen == null && !isFreecamActive();
            restore(client, options.forwardKey, bl);
            restore(client, options.backKey, bl);
            restore(client, options.leftKey, bl);
            restore(client, options.rightKey, bl);
        }
    }

    private static boolean isFreecamActive() {
        FreecamModule module = ModuleRegistry.get(FreecamModule.class);
        return module != null && module.isEnabled();
    }

    private static void restore(MinecraftClient client, KeyBinding keyBinding, boolean bl) {
        keyBinding.setPressed(bl && isPhysicallyHeld(client, keyBinding));
    }

    private static boolean isPhysicallyHeld(MinecraftClient client, KeyBinding keyBinding) {
        if (keyBinding.isUnbound()) {
            return false;
        } else {
            Key key = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
            Type category = key.getCategory();
            if (category == Type.KEYSYM) {
                return EfnKeyPolling.isKeyDown(client.getWindow(), key.getCode());
            } else {
                return category == Type.MOUSE
                        ? GLFW.glfwGetMouseButton(client.getWindow().getHandle(), key.getCode())
                                == 1
                        : false;
            }
        }
    }
}
