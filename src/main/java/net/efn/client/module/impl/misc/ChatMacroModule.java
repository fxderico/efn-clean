package net.efn.client.module.impl.misc;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.StringHelper;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class ChatMacroModule extends ClientModule {

    public static final int MAX_MACROS = 8;

    private static final String ACTION_SEND = "Send";

    private static final String ACTION_OPEN = "Open in Chat";

    private final ModeSetting slots =
            this.addSetting("slots", new ModeSetting("Slots", "3", slotChoices()));

    private final ModeSetting pressAction =
            this.addSetting(
                    "on-press", new ModeSetting("On Press", "Send", "Send", "Open in Chat"));

    private final NumberSetting cooldown =
            this.addSetting(
                    "cooldown", new NumberSetting("Cooldown (s)", 1.0F, 1.0F, 10.0F).step(0.5F));

    private final KeySetting[] keys = new KeySetting[8];

    private final StringSetting[] texts = new StringSetting[8];

    private int pendingSlot = -1;

    private long lastSendNanos;

    private boolean sentAny;

    public ChatMacroModule() {
        super(
                "chat-macro",
                "Chat Macro",
                "Sends chat messages or commands with a key press.",
                ModuleCategory.MISC);
        this.usesNotifications();
        this.cooldown.visibleWhen(() -> this.pressAction.is("Send"));
        for (int i = 0; i < 8; i++) {
            int j = i + 1;
            this.keys[i] =
                    this.addSetting(
                            "macro-" + j + "-key", new KeySetting("Macro " + j + " Key", -1));
            this.texts[i] =
                    this.addSetting(
                            "macro-" + j + "-text", new StringSetting("Macro " + j + " Text", ""));
            this.texts[i].privateValue();
            this.keys[i].visibleWhen(() -> j <= this.slotCount());
            this.texts[i].visibleWhen(() -> j <= this.slotCount());
        }
    }

    @Override
    public boolean inConfigs() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.pendingSlot = -1;
        this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
        this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
        this.on(TickEvent.TYPE, Stage.PRE, this::onTick);
    }

    @Override
    protected void onDisable() {
        this.pendingSlot = -1;
    }

    private void onKey(KeyInputEvent event) {
        if (event.action == 1) {
            this.onPress(event.key);
        }
    }

    private void onMouse(MouseInputEvent event) {
        if (event.action == 1) {
            this.onPress(1000 + event.button);
        }
    }

    private void onPress(int i) {
        if (ModuleKeybinds.isBindable(i) && this.pendingSlot == -1) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null && client.player != null) {
                int slotCount = this.slotCount();
                for (int k = 0; k < slotCount; k++) {
                    if (this.keys[k].value() == i && !this.texts[k].get().isBlank()) {
                        this.pendingSlot = k;
                        return;
                    }
                }
            }
        }
    }

    private void onTick(TickEvent event) {
        int pendingSlot2 = this.pendingSlot;
        if (pendingSlot2 >= 0) {
            this.pendingSlot = -1;
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            if (client.player != null && networkHandler != null && client.currentScreen == null) {
                String string = normalize(this.texts[pendingSlot2].get());
                if (!string.isEmpty() && !string.equals("/")) {
                    if (this.pressAction.is("Open in Chat")) {
                        client.setScreen(new ChatScreen(string, false));
                    } else {
                        long l = System.nanoTime();
                        long l2 = (long) (this.cooldown.get() * 1.0E9) - (l - this.lastSendNanos);
                        if (this.sentAny && l2 > 0L) {
                            double d = Math.ceil(l2 / 1.0E8) / 10.0;
                            ModuleChat.actionBar(
                                    this,
                                    ModuleChat.body()
                                            .warn("On cooldown")
                                            .text(", ")
                                            .value(String.format(Locale.ROOT, "%.1fs", d))
                                            .text(" left"));
                        } else {
                            this.lastSendNanos = l;
                            this.sentAny = true;
                            client.inGameHud.getChatHud().addToMessageHistory(string);
                            if (string.startsWith("/")) {
                                networkHandler.sendChatCommand(string.substring(1));
                            } else {
                                networkHandler.sendChatMessage(string);
                            }
                        }
                    }
                }
            }
        }
    }

    static String normalize(String string) {
        String stripInvalidChars = StringHelper.stripInvalidChars(string);
        return StringHelper.truncateChat(StringUtils.normalizeSpace(stripInvalidChars.trim()));
    }

    private int slotCount() {
        return Integer.parseInt(this.slots.get());
    }

    private static String[] slotChoices() {
        String[] strings = new String[8];
        for (int i = 0; i < 8; i++) {
            strings[i] = Integer.toString(i + 1);
        }
        return strings;
    }
}
