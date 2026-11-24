package net.efn.client.webhook;

import com.google.gson.JsonObject;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record WebhookEmbed(
        String title,
        String description,
        Integer color,
        String thumbnailUrl,
        String imageUrl,
        String footerText,
        String footerIconUrl) {

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        add(json, "title", this.title);
        add(json, "description", this.description);
        if (this.color != null) {
            json.addProperty("color", this.color);
        }
        addNestedUrl(json, "thumbnail", this.thumbnailUrl);
        addNestedUrl(json, "image", this.imageUrl);
        if (this.footerText != null || this.footerIconUrl != null) {
            JsonObject json2 = new JsonObject();
            add(json2, "text", this.footerText);
            add(json2, "icon_url", this.footerIconUrl);
            json.add("footer", json2);
        }
        return json;
    }

    private static void addNestedUrl(JsonObject json, String string, String string2) {
        if (string2 != null && !string2.isBlank()) {
            JsonObject json2 = new JsonObject();
            json2.addProperty("url", string2);
            json.add(string, json2);
        }
    }

    private static void add(JsonObject json, String string, String string2) {
        if (string2 != null && !string2.isBlank()) {
            json.addProperty(string, string2);
        }
    }
}
