package net.efn.client.util;

import com.mojang.brigadier.suggestion.Suggestion;

import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class CompletionChannel {

    public static final CompletionChannel INSTANCE = new CompletionChannel();

    private static final int WIRE_ID = 0;

    private static final long FORGET_MS = 15000L;

    private static final long FOREIGN_MS = 4000L;

    private static final Object FOREIGN = new Object();

    private final Object lock = new Object();

    private final LinkedList<CompletionChannel.Request> line = new LinkedList<>();

    private final Map<Integer, CompletionChannel.Request> byKey = new HashMap<>();

    private Object connection;

    private int nextKey = 1;

    private boolean sending;

    private boolean installed;

    private CompletionChannel() {}

    public static void install() {
        if (!INSTANCE.installed) {
            INSTANCE.installed = true;
            PacketSendEvent.TYPE.listen(
                    CompletionChannel.class,
                    packetSendEvent -> INSTANCE.outgoing(packetSendEvent.packet));
            PacketReceiveNettyEvent.TYPE.listen(
                    CompletionChannel.class,
                    packetReceiveNettyEvent -> {
                        if (INSTANCE.ingest(packetReceiveNettyEvent.packet)) {
                            packetReceiveNettyEvent.setCancelled(true);
                        }
                    });
        }
    }

    public int fire(Object object, String string, CompletionChannel.Form form) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler != null && object != null && string != null) {
            Object lock2 = this.lock;
            CompletionChannel.Request request;
            synchronized (lock2) {
                this.follow(networkHandler);
                int i = this.nextKey++;
                if (this.nextKey == 2147483647) {
                    this.nextKey = 1;
                }
                request =
                        new CompletionChannel.Request(
                                i,
                                object,
                                want(string),
                                System.currentTimeMillis(),
                                form == CompletionChannel.Form.BOTH ? 2 : 1);
                this.line.addLast(request);
                this.byKey.put(i, request);
            }
            this.sending = true;
            byte b;
            try {
                if (form != CompletionChannel.Form.BARE) {
                    networkHandler.sendPacket(
                            new RequestCommandCompletionsC2SPacket(0, "/" + string));
                }
                if (form != CompletionChannel.Form.SLASH) {
                    networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(0, string));
                }
                return request.key;
            } catch (RuntimeException exception) {
                Object lock3 = this.lock;
                synchronized (lock3) {
                    this.line.remove(request);
                    this.byKey.remove(request.key);
                }
                byte b2 = -1;
                b = b2;
            } finally {
                this.sending = false;
            }
            return b;
        } else {
            return -1;
        }
    }

    public List<String> poll(int i) {
        Object lock2 = this.lock;
        synchronized (lock2) {
            CompletionChannel.Request request = this.byKey.get(i);
            return request != null && !request.forgotten ? request.names : null;
        }
    }

    public void forget(int i) {
        Object lock2 = this.lock;
        synchronized (lock2) {
            CompletionChannel.Request request = this.byKey.remove(i);
            if (request != null) {
                request.forgotten = true;
                if (!request.waiting()) {
                    this.line.remove(request);
                }
            }
        }
    }

    public void prune(long l) {
        Object lock2 = this.lock;
        synchronized (lock2) {
            this.expire(l);
        }
    }

    public void clear(Object object) {
        Object lock2 = this.lock;
        synchronized (lock2) {
            Iterator iterator = this.line.iterator();
            while (iterator.hasNext()) {
                CompletionChannel.Request request = (CompletionChannel.Request) iterator.next();
                if (request.owner == object) {
                    request.forgotten = true;
                    this.byKey.remove(request.key);
                    if (!request.waiting()) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private void outgoing(Packet<?> packet) {
        if (!this.sending
                && packet
                        instanceof
                        RequestCommandCompletionsC2SPacket requestCommandCompletionsPacket) {
            ClientPlayNetworkHandler networkHandler =
                    MinecraftClient.getInstance().getNetworkHandler();
            Object lock2 = this.lock;
            synchronized (lock2) {
                this.follow(networkHandler);
                this.line.addLast(
                        new CompletionChannel.Request(
                                0,
                                FOREIGN,
                                want(requestCommandCompletionsPacket.getPartialCommand()),
                                System.currentTimeMillis(),
                                1));
            }
        }
    }

    private boolean ingest(Packet<?> packet) {
        if (!(packet instanceof CommandSuggestionsS2CPacket commandSuggestionsPacket)) {
            return false;
        } else {
            ArrayList list = new ArrayList();
            for (Suggestion suggestion : commandSuggestionsPacket.getSuggestions().getList()) {
                String text = suggestion.getText();
                if (text != null && !text.isBlank()) {
                    list.add(text.trim());
                }
            }
            String string = list.isEmpty() ? "" : core((String) list.get(0));
            String string2 = list.isEmpty() ? "" : core((String) list.get(list.size() - 1));
            Object lock2 = this.lock;
            synchronized (lock2) {
                this.expire(System.currentTimeMillis());
                CompletionChannel.Request request = null;
                for (CompletionChannel.Request request2 : this.line) {
                    if (request2.waiting()
                            && (list.isEmpty()
                                    || string.startsWith(request2.want)
                                            && string2.startsWith(request2.want))) {
                        request = request2;
                        break;
                    }
                }
                if (request == null) {
                    return false;
                } else {
                    Iterator iterator = this.line.iterator();
                    CompletionChannel.Request request3;
                    while (iterator.hasNext()
                            && (request3 = (CompletionChannel.Request) iterator.next())
                                    != request) {
                        if (request3.waiting()) {
                            request3.answered = request3.expected;
                            if (request3.owner == FOREIGN || request3.forgotten) {
                                iterator.remove();
                            }
                        }
                    }
                    request.answered++;
                    if (list.isEmpty() || request.names != null && !request.names.isEmpty()) {
                        if (request.names == null && !request.waiting()) {
                            request.names = List.of();
                        }
                    } else {
                        request.names = List.copyOf(list);
                    }
                    if (!request.waiting() && (request.owner == FOREIGN || request.forgotten)) {
                        this.line.remove(request);
                    }
                    return request.owner != FOREIGN;
                }
            }
        }
    }

    private void follow(ClientPlayNetworkHandler connection2) {
        if (connection2 != this.connection) {
            this.connection = connection2;
            this.line.clear();
            this.byKey.clear();
        }
    }

    private void expire(long l) {
        Iterator iterator = this.line.iterator();
        while (iterator.hasNext()) {
            CompletionChannel.Request request = (CompletionChannel.Request) iterator.next();
            long l2 = l - request.at;
            if (l2 > 15000L || request.owner == FOREIGN && l2 > 4000L) {
                iterator.remove();
                if (request.owner != FOREIGN) {
                    this.byKey.remove(request.key, request);
                }
            }
        }
    }

    static String want(String string) {
        String string2 = string == null ? "" : string.trim();
        if (string2.startsWith("/")) {
            string2 = string2.substring(1);
        }
        int i;
        string2 = (i = string2.lastIndexOf(32)) >= 0 ? string2.substring(i + 1) : "";
        return stripDots(string2).toLowerCase(Locale.ROOT);
    }

    public static String core(String string) {
        String string2 = string.replaceAll("\u00a7.", "").trim();
        int i = string2.lastIndexOf(32);
        if (i >= 0) {
            string2 = string2.substring(i + 1);
        }
        string2 = string2.replaceAll("^[\"']|[\"']$", "");
        return stripDots(string2).toLowerCase(Locale.ROOT);
    }

    private static String stripDots(String string) {
        int i = 0;
        while (i < string.length() && string.charAt(i) == '.') {
            i++;
        }
        return string.substring(i);
    }

    @Environment(EnvType.CLIENT)
    public static enum Form {
        BOTH,
        SLASH,
        BARE;

        private Form() {}
    }

    @Environment(EnvType.CLIENT)
    private static final class Request {

        final int key;

        final Object owner;

        final String want;

        final long at;

        final int expected;

        int answered;

        List<String> names;

        boolean forgotten;

        Request(int key2, Object owner2, String want2, long at2, int expected2) {
            this.key = key2;
            this.owner = owner2;
            this.want = want2;
            this.at = at2;
            this.expected = expected2;
        }

        boolean waiting() {
            return this.answered < this.expected;
        }
    }
}
