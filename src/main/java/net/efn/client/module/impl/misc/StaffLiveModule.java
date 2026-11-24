package net.efn.client.module.impl.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import imgui.ImDrawList;

import net.efn.client.chat.ModuleChat;
import net.efn.client.gui.HudPanel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent.OpenUrl;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Environment(EnvType.CLIENT)
public final class StaffLiveModule extends ClientModule implements HudOverlayProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String BROWSER =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
                    + " Chrome/130.0 Safari/537.36";

    private static final Pattern CHANNEL = Pattern.compile("^(yt|tt):@?([A-Za-z0-9._-]+)$", 2);

    private static final String CANONICAL = "<link rel=\"canonical\" href=\"";

    private static final String LIVE_NOW = "\"isLiveNow\":true";

    private static final String META_TITLE = "<meta name=\"title\" content=\"";

    private static final String START = "\"startTimestamp\":\"";

    private static final int MAX_TITLE_CHARS = 48;

    private final StringListSetting channels =
            this.addSetting(
                    "channels",
                    new StringListSetting(
                            "Channels",
                            "yt:wizoxlol",
                            "yt:pastagamer08",
                            "tt:pastagamer08",
                            "tt:theauzzi",
                            "tt:u_vv7_",
                            "yt:DrDonutt"));

    private final NumberSetting interval =
            this.addSetting(
                    "check-every",
                    new NumberSetting("Check Every (s)", 60.0F, 15.0F, 600.0F).step(5.0F));

    private final NumberSetting scale =
            this.addSetting("scale", new NumberSetting("Scale", 1.0F, 0.5F, 3.0F).step(0.05F));

    private final NumberSetting opacity = this.addSetting("opacity", HudPanel.opacitySetting());

    private final HudPanel panel =
            new HudPanel(
                    this.addSetting("pos-x", HudPanel.coordinate("X")),
                    this.addSetting("pos-y", HudPanel.coordinate("Y")),
                    this.scale,
                    this.opacity);

    private final HttpClient http =
            HttpClient.newBuilder()
                    .followRedirects(Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10L))
                    .build();

    private final Map<String, Boolean> live = new ConcurrentHashMap<>();

    private final Map<String, StaffLiveModule.Live> shown = new ConcurrentHashMap<>();

    private volatile Thread worker;

    public StaffLiveModule() {
        super(
                "staff-live",
                "Staff Live",
                "Tells you when DonutSMP staff go live on YouTube or TikTok: a chat line with a"
                    + " watch link, a chime, and a panel with the stream title while they are live."
                    + " Channels are yt:handle or tt:handle.",
                ModuleCategory.DONUT);
        this.usesNotifications();
    }

    @Override
    protected void onEnable() {
        this.live.clear();
        this.shown.clear();
        Thread thread = new Thread(this::run, "EFN Staff Live");
        thread.setDaemon(true);
        this.worker = thread;
        thread.start();
    }

    @Override
    protected void onDisable() {
        Thread worker2 = this.worker;
        this.worker = null;
        if (worker2 != null) {
            worker2.interrupt();
        }
        this.shown.clear();
    }

    private void run() {
        long l = 0L;
        while (this.worker == Thread.currentThread()) {
            if (System.currentTimeMillis() >= l) {
                HashSet set = new HashSet();
                for (StaffLiveModule.Channel channel : this.channels()) {
                    if (this.worker != Thread.currentThread()) {
                        return;
                    }
                    set.add(channel.key());
                    this.check(channel);
                }
                this.live.keySet().retainAll(set);
                this.shown.keySet().retainAll(set);
                l = System.currentTimeMillis() + (long) (this.interval.get() * 1000.0F);
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException exception) {
                return;
            }
        }
    }

    private List<StaffLiveModule.Channel> channels() {
        ArrayList list = new ArrayList();
        for (String string : this.channels.get()) {
            Matcher matcher = CHANNEL.matcher(string.trim());
            if (matcher.find()) {
                list.add(
                        new StaffLiveModule.Channel(
                                matcher.group(1).equalsIgnoreCase("yt"), matcher.group(2)));
            }
        }
        return list;
    }

    private void check(StaffLiveModule.Channel channel) {
        StaffLiveModule.Stream stream;
        try {
            stream =
                    channel.youtube()
                            ? this.youtube(channel.handle())
                            : this.tiktok(channel.handle());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return;
        } catch (Exception exception2) {
            LOGGER.debug(
                    "[Staff Live] check of {} failed: {}", channel.key(), exception2.toString());
            return;
        }
        if (stream != null) {
            Boolean flag = this.live.put(channel.key(), stream.live());
            if (stream.live() && !Boolean.TRUE.equals(flag)) {
                this.announce(channel, stream);
            }
            if (!stream.live()) {
                this.shown.remove(channel.key());
            }
        }
    }

    private StaffLiveModule.Stream youtube(String string) throws Exception {
        HttpRequest httpRequest =
                HttpRequest.newBuilder(URI.create("https://www.youtube.com/@" + string + "/live"))
                        .timeout(Duration.ofSeconds(20L))
                        .header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                                        + " (KHTML, like Gecko) Chrome/130.0 Safari/537.36")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Accept-Encoding", "gzip")
                        .header("Cookie", "SOCS=CAI; CONSENT=YES+")
                        .build();
        HttpResponse httpResponse = this.http.send(httpRequest, BodyHandlers.ofInputStream());
        StaffLiveModule.Stream stream;
        try (InputStream inValue = (InputStream) httpResponse.body()) {
            if (httpResponse.statusCode() != 200) {
                return null;
            }
            boolean bl =
                    httpResponse
                            .headers()
                            .firstValue("Content-Encoding")
                            .map(stringx -> stringx.equalsIgnoreCase("gzip"))
                            .orElse(false);
            InputStreamReader inputStreamReader =
                    new InputStreamReader(
                            (InputStream) (bl ? new GZIPInputStream(inValue) : inValue),
                            StandardCharsets.UTF_8);
            char[] chars = new char[16384];
            StringBuilder sb = new StringBuilder();
            String string2 = null;
            String string3 = null;
            boolean bl2 = false;
            int i;
            while ((i = inputStreamReader.read(chars)) > 0) {
                sb.append(chars, 0, i);
                String after;
                if (string2 == null
                        && (after = after(sb, "<link rel=\"canonical\" href=\"")) != null) {
                    if (!after.contains("/watch?v=")) {
                        return StaffLiveModule.Stream.OFFLINE;
                    }
                    string2 = after;
                }
                if (string2 != null && string3 == null) {
                    string3 = after(sb, "<meta name=\"title\" content=\"");
                }
                if (string2 != null && !bl2) {
                    bl2 = sb.indexOf("\"isLiveNow\":true") >= 0;
                }
                if (bl2) {
                    String after2 = after(sb, "\"startTimestamp\":\"");
                    if (after2 != null || sb.length() > 4096) {
                        return new StaffLiveModule.Stream(
                                true, clean(string3), string2, null, parseStart(after2));
                    }
                } else if (sb.length() > 4096) {
                    sb.delete(0, sb.length() - 512);
                }
            }
            StaffLiveModule.Stream stream2 =
                    bl2
                            ? new StaffLiveModule.Stream(true, clean(string3), string2, null, 0L)
                            : StaffLiveModule.Stream.OFFLINE;
            stream = stream2;
        }
        return stream;
    }

    private StaffLiveModule.Stream tiktok(String string) throws Exception {
        String string2 =
                "https://www.tiktok.com/api-live/user/room/?aid=1988&app_language=en&app_name=tiktok_web&sourceType=54&uniqueId="
                        + URLEncoder.encode(string, StandardCharsets.UTF_8);
        HttpRequest httpRequest =
                HttpRequest.newBuilder(URI.create(string2))
                        .timeout(Duration.ofSeconds(20L))
                        .header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                                        + " (KHTML, like Gecko) Chrome/130.0 Safari/537.36")
                        .header("Accept", "application/json")
                        .build();
        HttpResponse httpResponse = this.http.send(httpRequest, BodyHandlers.ofString());
        if (httpResponse.statusCode() != 200) {
            return null;
        } else {
            JsonElement string3 = JsonParser.parseString((String) httpResponse.body());
            if (!string3.isJsonObject()) {
                return null;
            } else {
                JsonObject json = object(string3.getAsJsonObject(), "data");
                JsonObject json2 = object(json, "liveRoom");
                if (json2 != null && json2.has("status")) {
                    boolean bl = json2.get("status").getAsInt() == 2;
                    JsonObject json3 = object(json, "user");
                    String string4 =
                            json3 != null && json3.has("nickname")
                                    ? json3.get("nickname").getAsString()
                                    : null;
                    String string5 = json2.has("title") ? json2.get("title").getAsString() : null;
                    long l =
                            json2.has("startTime")
                                    ? json2.get("startTime").getAsLong() * 1000L
                                    : 0L;
                    return new StaffLiveModule.Stream(
                            bl,
                            clean(string5),
                            "https://www.tiktok.com/@" + string + "/live",
                            string4,
                            l);
                } else {
                    return null;
                }
            }
        }
    }

    private void announce(StaffLiveModule.Channel channel, StaffLiveModule.Stream stream) {
        String string =
                stream.link() != null
                        ? stream.link()
                        : "https://www.youtube.com/@" + channel.handle() + "/live";
        String string2 =
                stream.nick() != null && !stream.nick().equalsIgnoreCase(channel.handle())
                        ? channel.handle() + " (" + stream.nick() + ")"
                        : channel.handle();
        String string3 = channel.youtube() ? "YouTube" : "TikTok";
        long l = System.currentTimeMillis();
        long l2 = stream.started() > 0L && stream.started() <= l ? stream.started() : l;
        this.shown.putIfAbsent(
                channel.key(), new StaffLiveModule.Live(string2, string3, stream.title(), l2, l));
        LOGGER.info("[Staff Live] {} is live on {}", string2, string3);
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(
                () -> this.announceOnClient(client, string2, string3, stream, (String) string));
    }

    private List<StaffLiveModule.Live> rows() {
        return !this.isEnabled()
                ? List.of()
                : this.shown.values().stream()
                        .sorted(Comparator.comparingLong(StaffLiveModule.Live::shownAt))
                        .toList();
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled()
                && MinecraftClient.getInstance().player != null
                && !this.shown.isEmpty();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        List list = this.rows();
        if (!list.isEmpty()) {
            long l = System.currentTimeMillis();
            ArrayList list2 = new ArrayList();
            for (StaffLiveModule.Live live2 : (Iterable<StaffLiveModule.Live>) list) {
                list2.add(
                        new HudPanel.Line(
                                0,
                                live2.who(),
                                -1,
                                live2.platform() + "  \u00b7  " + ago(l - live2.since()),
                                -47814));
                if (live2.title() != null && !live2.title().isBlank()) {
                    list2.add(new HudPanel.Line(shorten(live2.title()), -6052949));
                }
            }
            this.panel.card(
                    imDrawList,
                    "LIVE",
                    String.valueOf(list.size()),
                    -47814,
                    list2,
                    "staff_list",
                    f,
                    g,
                    0.5F,
                    10.0F);
        }
    }

    private static String shorten(String string) {
        return string.length() <= 48 ? string : string.substring(0, 47).stripTrailing() + "\u2026";
    }

    private static String ago(long l) {
        long l2 = l / 60000L;
        return l2 < 1L ? "now" : (l2 < 60L ? l2 + "m" : l2 / 60L + "h " + l2 % 60L + "m");
    }

    private static String after(CharSequence charSequence, String string) {
        String string2 = charSequence.toString();
        int i = string2.indexOf(string);
        int j = i < 0 ? -1 : string2.indexOf(34, i + string.length());
        return j < 0 ? null : string2.substring(i + string.length(), j);
    }

    private static String clean(String string) {
        if (string == null) {
            return null;
        } else {
            string =
                    string.replace("&quot;", "\"")
                            .replace("&#39;", "'")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&amp;", "&");
            StringBuilder sb = new StringBuilder(string.length());
            string.codePoints()
                    .filter(i -> i >= 32 && i < 8192 || i >= 8208 && i < 9728)
                    .forEach(sb::appendCodePoint);
            return sb.toString().replaceAll("\\s+", " ").trim();
        }
    }

    private static long parseStart(String string) {
        try {
            return string == null ? 0L : OffsetDateTime.parse(string).toInstant().toEpochMilli();
        } catch (RuntimeException exception) {
            return 0L;
        }
    }

    private static JsonObject object(JsonObject json, String string) {
        if (json == null) {
            return null;
        } else {
            JsonElement json2 = json.get(string);
            return json2 != null && json2.isJsonObject() ? json2.getAsJsonObject() : null;
        }
    }

    private void announceOnClient(
            MinecraftClient client,
            String string,
            String string2,
            StaffLiveModule.Stream stream,
            String string3) {
        if (this.isEnabled() && client.player != null) {
            ModuleChat.Body body =
                    ModuleChat.body()
                            .name(string)
                            .text(" is ")
                            .danger("LIVE")
                            .text(" on " + string2);
            if (stream.title() != null && !stream.title().isBlank()) {
                body.text(": " + stream.title());
            }
            body.text(" ")
                    .append(
                            Text.literal("[watch]")
                                    .styled(
                                            style ->
                                                    style.withClickEvent(
                                                                    new OpenUrl(
                                                                            URI.create(string3)))
                                                            .withUnderline(true)));
            ModuleChat.send(this, body);
            if (this.notificationsOn()) {
                client.getSoundManager()
                        .play(
                                PositionedSoundInstance.ui(
                                        SoundEvents.BLOCK_NOTE_BLOCK_CHIME, 1.25F));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private record Channel(boolean youtube, String handle) {

        String key() {
            return (this.youtube ? "yt:" : "tt:") + this.handle.toLowerCase(Locale.ROOT);
        }
    }

    @Environment(EnvType.CLIENT)
    private record Live(String who, String platform, String title, long since, long shownAt) {}

    @Environment(EnvType.CLIENT)
    private record Stream(boolean live, String title, String link, String nick, long started) {

        static final StaffLiveModule.Stream OFFLINE =
                new StaffLiveModule.Stream(false, null, null, null, 0L);
    }
}
