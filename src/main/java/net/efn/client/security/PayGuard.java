package net.efn.client.security;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.StackWalker.StackFrame;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

@Environment(EnvType.CLIENT)
public final class PayGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final long ALLOW_MS = 5000L;

    private static final long SCREEN_GAP_MS = 30000L;

    private static final long CHAT_GAP_MS = 3000L;

    private static final int SHOWN_CHARS = 64;

    private static final Text TITLE =
            Text.literal("Blocked an automatic payment").formatted(Formatting.RED, Formatting.BOLD);

    private static final ConcurrentLinkedQueue<PayGuard.Allowance> allowed =
            new ConcurrentLinkedQueue<>();

    private static volatile long lastScreenMs;

    private static volatile long lastChatMs;

    private static volatile String lastChatShown = "";

    private PayGuard() {}

    public static void allowTyped(String string) {
        if (string != null && string.strip().startsWith("/")) {
            allow(string);
        }
    }

    public static void allow(String string) {
        if (isPay(string)) {
            long l = System.currentTimeMillis();
            allowed.removeIf(allowance -> l - allowance.atMs() > 5000L);
            allowed.offer(new PayGuard.Allowance(normalize(string), l));
        }
    }

    public static boolean blocks(Packet<?> packet) {
        String command2;
        if (packet instanceof CommandExecutionC2SPacket commandExecutionPacket) {
            command2 = commandExecutionPacket.command();
        } else {
            if (!(packet instanceof ChatCommandSignedC2SPacket chatCommandSignedPacket)) {
                return false;
            }
            command2 = chatCommandSignedPacket.command();
        }
        if (isPay(command2) && !consume(command2)) {
            report(command2);
            return true;
        } else {
            return false;
        }
    }

    public static boolean isPay(String string) {
        String string2 = normalize(string);
        int i = string2.indexOf(32);
        String string3 = i < 0 ? string2 : string2.substring(0, i);
        string3 = string3.substring(string3.indexOf(58) + 1);
        return string3.equals("pay") || string3.equals("epay");
    }

    private static String normalize(String string) {
        String string2 = string == null ? "" : string.strip();
        if (string2.startsWith("/")) {
            string2 = string2.substring(1).strip();
        }
        return string2.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean consume(String string) {
        String string2 = normalize(string);
        long l = System.currentTimeMillis();
        Iterator iterator = allowed.iterator();
        while (iterator.hasNext()) {
            PayGuard.Allowance allowance = (PayGuard.Allowance) iterator.next();
            if (l - allowance.atMs() > 5000L) {
                iterator.remove();
            } else if (allowance.command().equals(string2)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    private static void report(String string) {
        String string2 = string.strip();
        String string3 = "/" + (string2.length() > 64 ? string2.substring(0, 64) + "..." : string2);
        LOGGER.warn("[EFN] Pay Guard blocked \"{}\" sent by {}", string3, caller());
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> warn(client, string3));
    }

    private static String caller() {
        return StackWalker.getInstance()
                .walk(
                        stream ->
                                stream.map(StackFrame::getClassName)
                                        .filter(
                                                value ->
                                                        !value.startsWith(
                                                                        "net.efn.client.security.")
                                                                && !value.startsWith(
                                                                        "net.minecraft.")
                                                                && !value.startsWith("com.mojang.")
                                                                && !value.startsWith("io.netty.")
                                                                && !value.startsWith("java.")
                                                                && !value.startsWith("jdk.")
                                                                && !value.startsWith(
                                                                        "org.spongepowered.")
                                                                && !value.startsWith(
                                                                        "com.example.client.mixin."))
                                        .findFirst()
                                        .orElse("unknown code"));
    }

    private static void warn(MinecraftClient client, String string) {
        long l = System.currentTimeMillis();
        if (client.inGameHud != null
                && (l - lastChatMs >= 3000L || !string.equals(lastChatShown))) {
            lastChatMs = l;
            lastChatShown = string;
            client.inGameHud
                    .getChatHud()
                    .addMessage(
                            Text.empty()
                                    .append(
                                            Text.literal("[EFN] ")
                                                    .formatted(
                                                            Formatting.DARK_RED, Formatting.BOLD))
                                    .append(
                                            Text.literal(
                                                            "Blocked "
                                                                    + string
                                                                    + ". Something tried to send a"
                                                                    + " payment command on its own"
                                                                    + " without you typing it. If"
                                                                    + " you didn't do this"
                                                                    + " yourself, something else on"
                                                                    + " your PC may be malicious.")
                                                    .formatted(Formatting.RED)));
        }
        if (!(client.currentScreen instanceof PayGuard.WarningScreen)
                && l - lastScreenMs >= 30000L) {
            lastScreenMs = l;
            client.setScreen(new PayGuard.WarningScreen(client.currentScreen, string));
        }
    }

    @Environment(EnvType.CLIENT)
    private record Allowance(String command, long atMs) {}

    @Environment(EnvType.CLIENT)
    private static final class WarningScreen extends ConfirmScreen {

        WarningScreen(Screen screen, String string) {
            super(
                    value -> {
                        if (value) {
                            Util.getOperatingSystem().open("https://discord.gg/pxHtFp5b6h");
                        }
                        MinecraftClient.getInstance().setScreen(screen);
                    },
                    PayGuard.TITLE,
                    Text.literal(
                            "Something on this PC tried to send "
                                    + string
                                    + " by itself, without you typing it. It was blocked.\n\n"
                                    + "If you didn't do this yourself, something else running on"
                                    + " your computer may be malicious. Consider scanning your"
                                    + " PC.\n\n"
                                    + "If you think this is a false positive, report it in the EFN"
                                    + " Discord."),
                    Text.literal("Open EFN Discord"),
                    Text.literal("Close"));
        }
    }
}
