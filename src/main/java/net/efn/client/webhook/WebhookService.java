package net.efn.client.webhook;

import com.google.gson.JsonObject;

import net.efn.client.EfnClientMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

@Environment(EnvType.CLIENT)
public final class WebhookService {

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();

    public WebhookService() {}

    public boolean send(WebhookConfig webhookConfig, WebhookPayloadBuilder webhookPayloadBuilder) {
        JsonObject json;
        try {
            json = webhookPayloadBuilder.build(webhookConfig);
        } catch (RuntimeException exception) {
            EfnClientMod.LOGGER.warn(
                    "[EFN] Failed to build webhook payload", (Throwable) exception);
            return false;
        }
        HttpRequest httpRequest =
                HttpRequest.newBuilder(URI.create(webhookConfig.url()))
                        .timeout(Duration.ofSeconds(10L))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(json.toString()))
                        .build();
        try {
            HttpResponse httpResponse =
                    this.httpClient.send(httpRequest, BodyHandlers.discarding());
            int statusCode = httpResponse.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return true;
            }
            EfnClientMod.LOGGER.warn("[EFN] Webhook request failed with status {}", statusCode);
        } catch (InterruptedException | IOException exception2) {
            if (exception2 instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            EfnClientMod.LOGGER.warn("[EFN] Webhook request failed", (Throwable) exception2);
        }
        return false;
    }
}
