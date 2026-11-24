package net.efn.client.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
final class AuthHttp {

    private static final HttpClient CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8L))
                    .followRedirects(Redirect.NEVER)
                    .build();

    private static final Duration TIMEOUT = Duration.ofSeconds(30L);

    private static final String USER_AGENT = "EFNClient";

    private AuthHttp() {}

    static AuthHttp.Reply postForm(String string, Map<String, String> map)
            throws InterruptedException {
        String string2 =
                map.entrySet().stream()
                        .map(
                                entry ->
                                        URLEncoder.encode(
                                                        (String) entry.getKey(),
                                                        StandardCharsets.UTF_8)
                                                + "="
                                                + URLEncoder.encode(
                                                        (String) entry.getValue(),
                                                        StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&"));
        return send(
                string,
                HttpRequest.newBuilder(URI.create(string))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(BodyPublishers.ofString(string2)));
    }

    static AuthHttp.Reply postJson(String string, String string2, Map<String, String> map)
            throws InterruptedException {
        Builder builder =
                HttpRequest.newBuilder(URI.create(string))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(string2, StandardCharsets.UTF_8));
        map.forEach(builder::header);
        return send(string, builder);
    }

    static AuthHttp.Reply get(String string, String string2) throws InterruptedException {
        return send(
                string,
                HttpRequest.newBuilder(URI.create(string))
                        .header("Authorization", "Bearer " + string2)
                        .GET());
    }

    static String text(JsonObject json2, String string) {
        JsonElement json3 = json2 == null ? null : json2.get(string);
        return json3 != null && json3.isJsonPrimitive() ? json3.getAsString() : null;
    }

    static JsonObject object(JsonObject json2, String string) {
        JsonElement json3 = json2 == null ? null : json2.get(string);
        return json3 != null && json3.isJsonObject() ? json3.getAsJsonObject() : null;
    }

    private static AuthHttp.Reply send(String string, Builder builder) throws InterruptedException {
        try {
            HttpResponse httpResponse =
                    CLIENT.send(
                            builder.timeout(TIMEOUT)
                                    .header("Accept", "application/json")
                                    .header("User-Agent", "EFNClient")
                                    .build(),
                            BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new AuthHttp.Reply(
                    httpResponse.statusCode(),
                    parse((String) httpResponse.body()),
                    httpResponse.headers());
        } catch (IOException exception) {
            throw new AccountException(
                    "Couldn't reach " + URI.create(string).getHost() + " - check your connection",
                    exception);
        }
    }

    private static JsonObject parse(String string) {
        try {
            JsonElement string2 = JsonParser.parseString(string);
            return string2.isJsonObject() ? string2.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException exception) {
            return new JsonObject();
        }
    }

    @Environment(EnvType.CLIENT)
    record Reply(int status, JsonObject json, HttpHeaders headers) {

        boolean ok() {
            return this.status >= 200 && this.status < 300;
        }

        String text(String string) {
            return AuthHttp.text(this.json, string);
        }
    }
}
