package net.efn.client.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class WebhookPayloadBuilder {

    private final List<WebhookEmbed> embeds = new ArrayList<>();

    private String content;

    public WebhookPayloadBuilder() {}

    public WebhookPayloadBuilder content(String content2) {
        this.content = content2;
        return this;
    }

    public WebhookPayloadBuilder addEmbed(WebhookEmbed webhookEmbed) {
        if (webhookEmbed != null) {
            this.embeds.add(webhookEmbed);
        }
        return this;
    }

    public JsonObject build(WebhookConfig webhookConfig) {
        if ((this.content == null || this.content.isBlank()) && this.embeds.isEmpty()) {
            throw new IllegalStateException(
                    "Webhook payload must include content or at least one embed");
        } else {
            JsonObject json = new JsonObject();
            json.addProperty("username", webhookConfig.username());
            json.addProperty("avatar_url", webhookConfig.avatarUrl());
            if (this.content != null && !this.content.isBlank()) {
                json.addProperty("content", this.content);
            }
            if (!this.embeds.isEmpty()) {
                JsonArray array = new JsonArray();
                for (WebhookEmbed embed : this.embeds) {
                    array.add(embed.toJson());
                }
                json.add("embeds", array);
            }
            return json;
        }
    }
}
