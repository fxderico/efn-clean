package net.efn.client.module.impl.misc;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Random;

@Environment(EnvType.CLIENT)
public final class SpamModule extends ClientModule {

    public static volatile SpamModule active;

    private static final int MAX_MESSAGE_LENGTH = 256;

    private final StringListSetting messages =
            this.addSetting("messages", new StringListSetting("Messages", "EFN was here!"));

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay", 20.0F, 0.0F, 1200.0F).step(1.0F));

    private final BooleanSetting disableOnLeave =
            this.addSetting("disable-on-leave", new BooleanSetting("Disable On Leave", true));

    private final BooleanSetting disableOnDisconnect =
            this.addSetting(
                    "disable-on-disconnect", new BooleanSetting("Disable On Disconnect", true));

    private final BooleanSetting random =
            this.addSetting("randomise", new BooleanSetting("Randomise", false));

    private final BooleanSetting autoSplitMessages =
            this.addSetting(
                    "auto-split-messages", new BooleanSetting("Auto Split Messages", false));

    private final NumberSetting splitLength =
            this.addSetting(
                    "split-length",
                    new NumberSetting("Split Length", 256.0F, 1.0F, 256.0F).step(1.0F));

    private final NumberSetting autoSplitDelay =
            this.addSetting(
                    "split-delay",
                    new NumberSetting("Split Delay", 20.0F, 0.0F, 200.0F).step(1.0F));

    private final BooleanSetting bypass =
            this.addSetting("bypass", new BooleanSetting("Bypass", false));

    private final BooleanSetting uppercase =
            this.addSetting(
                    "include-uppercase-characters",
                    new BooleanSetting("Include Uppercase Characters", true));

    private final NumberSetting length =
            this.addSetting("length", new NumberSetting("Length", 16.0F, 1.0F, 256.0F).step(1.0F));

    private final Random rng = new Random();

    private int messageI;

    private int timer;

    private int splitNum;

    private String text;

    private boolean resetPending;

    public SpamModule() {
        super("spam", "Spam", "Spams specified messages in chat.", ModuleCategory.MISC);
        this.splitLength.visibleWhen(this.autoSplitMessages::get);
        this.autoSplitDelay.visibleWhen(this.autoSplitMessages::get);
        this.uppercase.visibleWhen(this.bypass::get);
        this.length.visibleWhen(this.bypass::get);
    }

    @Override
    public boolean inConfigs() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.resetPending = true;
        active = this;
        this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
    }

    @Override
    protected void onDisable() {
        active = null;
    }

    public static void onGameLeft() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) {
            client.execute(SpamModule::onGameLeft);
        } else {
            SpamModule module = active;
            if (module != null) {
                module.handleGameLeft();
            }
        }
    }

    public static void onDisconnectedScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) {
            client.execute(SpamModule::onDisconnectedScreen);
        } else {
            SpamModule module = active;
            if (module != null && module.isEnabled() && module.disableOnDisconnect.get()) {
                module.requestDisable();
            }
        }
    }

    private void handleGameLeft() {
        if (this.isEnabled()) {
            if (this.disableOnLeave.get()) {
                this.requestDisable();
            } else {
                this.resetPending = true;
            }
        }
    }

    private void onPostTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            if (this.resetPending) {
                this.resetPending = false;
                this.timer = this.delay.getInt();
                this.messageI = 0;
                this.splitNum = 0;
            }
            List messages2;
            if (!(messages2 = this.messages.get()).isEmpty()) {
                if (this.timer > 0) {
                    this.timer--;
                } else {
                    if (this.text == null) {
                        int i;
                        if (this.random.get()) {
                            i = this.rng.nextInt(messages2.size());
                        } else {
                            if (this.messageI >= messages2.size()) {
                                this.messageI = 0;
                            }
                            i = this.messageI++;
                        }
                        this.text = (String) messages2.get(i);
                        if (this.bypass.get()) {
                            String nextAlphabetic =
                                    RandomStringUtils.insecure()
                                            .nextAlphabetic(this.length.getInt());
                            if (!this.uppercase.get()) {
                                nextAlphabetic = nextAlphabetic.toLowerCase(Locale.ROOT);
                            }
                            this.text = this.text + " " + nextAlphabetic;
                        }
                    }
                    int splitLength2 = this.splitLength.getInt();
                    if (this.autoSplitMessages.get() && this.text.length() > splitLength2) {
                        int j = (this.text.length() + splitLength2 - 1) / splitLength2;
                        if (this.splitNum >= j) {
                            this.splitNum = 0;
                        }
                        int k = this.splitNum * splitLength2;
                        int m = Math.min(k + splitLength2, this.text.length());
                        sendPlayerMsg(client, player.networkHandler, this.text.substring(k, m));
                        this.splitNum = (this.splitNum + 1) % j;
                        this.timer = this.autoSplitDelay.getInt();
                        if (this.splitNum == 0) {
                            this.timer = this.delay.getInt();
                            this.text = null;
                        }
                    } else {
                        if (this.text.length() > 256) {
                            this.text = this.text.substring(0, 256);
                        }
                        sendPlayerMsg(client, player.networkHandler, this.text);
                        this.timer = this.delay.getInt();
                        this.text = null;
                    }
                }
            }
        }
    }

    private static void sendPlayerMsg(
            MinecraftClient client,
            ClientPlayNetworkHandler clientPlayNetworkHandler,
            String string) {
        client.inGameHud.getChatHud().addToMessageHistory(string);
        if (string.startsWith("/")) {
            clientPlayNetworkHandler.sendChatCommand(string.substring(1));
        } else {
            clientPlayNetworkHandler.sendChatMessage(string);
        }
    }
}
