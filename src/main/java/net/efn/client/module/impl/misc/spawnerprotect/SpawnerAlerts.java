package net.efn.client.module.impl.misc.spawnerprotect;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
public final class SpawnerAlerts {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Gson GSON = new Gson();

    private static final DateTimeFormatter LOCAL_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy \u2022 HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Istanbul"));

    private static final int MAX_ATTEMPTS = 3;

    private static final double MAX_RETRY_SECONDS = 30.0;

    private static final Duration TIMEOUT = Duration.ofSeconds(5L);

    private static final ThreadPoolExecutor WORKER =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(64),
                    runnable -> {
                        Thread thread = new Thread(runnable, "EFN-SpawnerProtect-Alerts");
                        thread.setDaemon(true);
                        return thread;
                    },
                    (runnable, threadPoolExecutor) ->
                            LOGGER.warn("[EFN] Spawner Protect alert queue is full"));

    private SpawnerAlerts() {}

    public static Map<String, Object> field(String string, String string2, boolean bl) {
        return Map.of("name", string, "value", string2, "inline", bl);
    }

    public static String payload(
            String string, String string2, int i, List<Map<String, Object>> list, Instant instant) {
        Map map =
                Map.of(
                        "title",
                        string,
                        "description",
                        string2,
                        "color",
                        i,
                        "fields",
                        new ArrayList(list),
                        "timestamp",
                        instant.toString(),
                        "author",
                        Map.<String, String>of("name", "EFN  /  SPAWNER PROTECT"),
                        "footer",
                        Map.<String, String>of(
                                "text",
                                "Minecraft 1.21.11 \u2022 "
                                        + LOCAL_TIME.format(instant)
                                        + " (TR)"));
        return GSON.toJson(
                Map.of(
                        "username",
                        "Spawner Protect \u2022 EFN",
                        "allowed_mentions",
                        Map.<String, List>of("parse", List.of()),
                        "embeds",
                        List.<Map>of(map)));
    }

    public static void send(String string, String string2) {
        if (string != null && !string.isBlank()) {
            WORKER.execute(() -> post(string.trim(), string2));
        }
    }

    private static void post(String string, String string2) {
        int i = 0;
        while (true) {
            label50:
            {
                if (i < 3) {
                    try {
                        HttpRequest httpRequest =
                                HttpRequest.newBuilder(URI.create(string))
                                        .timeout(TIMEOUT)
                                        .header("Content-Type", "application/json; charset=utf-8")
                                        .POST(
                                                BodyPublishers.ofString(
                                                        string2, StandardCharsets.UTF_8))
                                        .build();
                        HttpResponse httpResponse =
                                SpawnerAlerts.ClientHolder.CLIENT.send(
                                        httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
                        int statusCode = httpResponse.statusCode();
                        if (statusCode >= 200 && statusCode < 300) {
                            return;
                        }
                        if (statusCode != 429 || i >= 2) {
                            LOGGER.warn("[EFN] Spawner Protect alert failed (HTTP {})", statusCode);
                            return;
                        }
                        double retryAfter = retryAfter((String) httpResponse.body());
                        if (Double.isFinite(retryAfter) && !(retryAfter > 30.0)) {
                            Thread.sleep((long) (Math.max(0.25, retryAfter) * 1000.0) + 100L);
                            break label50;
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (Exception exception2) {
                        LOGGER.warn(
                                "[EFN] Spawner Protect alert could not be sent ({})",
                                exception2.getClass().getSimpleName());
                        return;
                    }
                }
                LOGGER.warn("[EFN] Spawner Protect alerts are rate limited");
                return;
            }
            i++;
        }
    }

    private static double retryAfter(String string) {
        if (string != null && !string.isBlank()) {
            try {
                JsonObject jsonObject = JsonParser.parseString(string).getAsJsonObject();
                return jsonObject.has("retry_after")
                        ? jsonObject.get("retry_after").getAsDouble()
                        : 1.0;
            } catch (RuntimeException exception) {
                return 1.0;
            }
        } else {
            return 1.0;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientHolder {

        static final HttpClient CLIENT =
                HttpClient.newBuilder().connectTimeout(SpawnerAlerts.TIMEOUT).build();

        private ClientHolder() {}
    }
}
