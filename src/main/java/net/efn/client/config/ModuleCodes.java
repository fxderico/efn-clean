package net.efn.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleCodes {

    private static final String PREFIX = "EfnModule:";

    private static final int FORMAT = 2;

    private ModuleCodes() {}

    public static String copy(ClientModule module) {
        JsonObject json = new JsonObject();
        for (Setting setting : module.settings()) {
            if (!setting.isPrivate() && !setting.isDefault()) {
                json.add(setting.id(), setting.compact(setting.serialize()));
            }
        }
        JsonObject json2 = new JsonObject();
        json2.addProperty("format", 2);
        json2.addProperty("module", module.id());
        if (module.settingsVersion() > 0) {
            json2.addProperty("settingsVersion", module.settingsVersion());
        }
        json2.add("settings", json);
        return ProfileCodes.encode(json2, "EfnModule:");
    }

    public static void paste(ClientModule module, String string) throws ProfileException {
        if (!module.inConfigs()) {
            throw new ProfileException(module.name() + " can't be set by configs");
        } else if (!ProfileCodes.contains(string, "EfnModule:")
                && ProfileCodes.contains(string, "EfnProfile:")) {
            throw new ProfileException("That's a whole profile, paste it in the Config tab");
        } else {
            JsonObject json =
                    ProfileCodes.decode(string, "EfnModule:", "No module config on the clipboard");
            int i = 0;
            if (json.has("format") && json.get("format").isJsonPrimitive()) {
                try {
                    i = json.get("format").getAsInt();
                } catch (RuntimeException exception) {
                }
            }
            if (i < 2) {
                throw new ProfileException("Made by an old EFN - old configs no longer work");
            } else {
                String string2 =
                        json.has("module") && json.get("module").isJsonPrimitive()
                                ? json.get("module").getAsString()
                                : "";
                if (!module.id().equals(string2)) {
                    ClientModule module2 =
                            string2.isEmpty() ? null : ModuleRegistry.resolve(string2);
                    throw new ProfileException(
                            "That config is for "
                                    + (module2 != null ? module2.name() : "another module"));
                } else {
                    JsonObject json2 =
                            json.has("settings") && json.get("settings").isJsonObject()
                                    ? json.getAsJsonObject("settings")
                                    : new JsonObject();
                    for (Setting setting : module.settings()) {
                        if (!setting.isPrivate()) {
                            JsonElement json3 = json2.get(setting.id());
                            if (json3 != null) {
                                setting.deserialize(json3);
                            } else {
                                setting.resetToDefault();
                            }
                        }
                    }
                    int settingsVersion = module.settingsVersion();
                    int j = 0;
                    if (json.has("settingsVersion")
                            && json.get("settingsVersion").isJsonPrimitive()) {
                        try {
                            j = json.get("settingsVersion").getAsInt();
                        } catch (RuntimeException exception2) {
                        }
                    }
                    if (settingsVersion > 0 && j < settingsVersion) {
                        module.migrateSettings(j);
                    }
                }
            }
        }
    }
}
