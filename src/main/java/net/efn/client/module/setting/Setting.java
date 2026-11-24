package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.efn.client.module.ModuleRevision;
import net.efn.client.module.StableId;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Environment(EnvType.CLIENT)
public abstract class Setting {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private final String name;

    private String id;

    private boolean idBound;

    private long revision;

    private final List<Runnable> changeListeners = new ArrayList<>(1);

    private BooleanSupplier visibility;

    private boolean privateValue;

    private boolean sessionOnly;

    private JsonElement defaultValue;

    private long defaultCheckedRevision = -1L;

    private boolean defaultCheckResult = true;

    protected Setting(String name2) {
        this.name = name2;
        this.id = StableId.legacy(name2);
    }

    public final String id() {
        return this.id;
    }

    public final long revision() {
        return this.revision;
    }

    public final void bindId(String string) {
        if (this.idBound) {
            throw new IllegalStateException("Setting id already bound");
        } else {
            this.id = StableId.validate(string);
            this.idBound = true;
        }
    }

    public final String name() {
        return this.name;
    }

    public final Setting onChange(Runnable runnable) {
        this.changeListeners.add(runnable);
        return this;
    }

    public final void notifyChanged() {
        this.revision++;
        for (int i = 0; i < this.changeListeners.size(); i++) {
            Runnable changeListener = this.changeListeners.get(i);
            try {
                if (changeListener == ModuleRevision.CHANGED) {
                    ModuleRevision.changed();
                } else {
                    changeListener.run();
                }
            } catch (Throwable exception) {
                LOGGER.error("Setting change listener failed for '{}'", this.name, exception);
            }
        }
    }

    public final Setting visibleWhen(BooleanSupplier visibility2) {
        this.visibility = visibility2;
        return this;
    }

    public final boolean isVisible() {
        return this.visibility == null || this.visibility.getAsBoolean();
    }

    public final Setting privateValue() {
        this.privateValue = true;
        return this;
    }

    public final boolean isPrivate() {
        return this.privateValue;
    }

    public final Setting sessionOnly() {
        this.sessionOnly = true;
        this.privateValue = true;
        return this;
    }

    public final boolean isSessionOnly() {
        return this.sessionOnly;
    }

    public final boolean isSecret() {
        return false;
    }

    public final void captureDefault() {
        if (this.defaultValue == null) {
            this.defaultValue = this.serialize();
        }
    }

    public final void adoptDefault(JsonElement json) {
        this.deserialize(json);
        this.defaultValue = this.serialize();
        this.defaultCheckedRevision = -1L;
    }

    public final boolean isDefault() {
        if (this.defaultValue == null) {
            return true;
        } else {
            if (this.defaultCheckedRevision != this.revision) {
                this.defaultCheckResult = this.defaultValue.equals(this.serialize());
                this.defaultCheckedRevision = this.revision;
            }
            return this.defaultCheckResult;
        }
    }

    public final boolean isDefault(JsonElement json) {
        return this.defaultValue != null && sameValue(this.defaultValue, json);
    }

    public static boolean sameValue(JsonElement json, JsonElement json2) {
        if (json == null || json2 == null) {
            return json == json2;
        } else if (json.isJsonPrimitive() && json2.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
            JsonPrimitive jsonPrimitive2 = json2.getAsJsonPrimitive();
            if (jsonPrimitive.isNumber() && jsonPrimitive2.isNumber()) {
                try {
                    return jsonPrimitive
                                    .getAsBigDecimal()
                                    .compareTo(jsonPrimitive2.getAsBigDecimal())
                            == 0;
                } catch (NumberFormatException exception) {
                    return false;
                }
            } else {
                return jsonPrimitive.equals(jsonPrimitive2);
            }
        } else if (json.isJsonArray() && json2.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            JsonArray jsonArray2 = json2.getAsJsonArray();
            if (jsonArray.size() != jsonArray2.size()) {
                return false;
            } else {
                for (int i = 0; i < jsonArray.size(); i++) {
                    if (!sameValue(jsonArray.get(i), jsonArray2.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        } else if (json.isJsonObject() && json2.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonObject jsonObject2 = json2.getAsJsonObject();
            if (!jsonObject.keySet().equals(jsonObject2.keySet())) {
                return false;
            } else {
                for (String string : jsonObject.keySet()) {
                    if (!sameValue(jsonObject.get(string), jsonObject2.get(string))) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return json.equals(json2);
        }
    }

    public final void resetToDefault() {
        if (!this.isDefault()) {
            this.deserialize(this.defaultValue);
        }
    }

    public JsonElement compact(JsonElement json) {
        return json;
    }

    protected final JsonElement defaultJson() {
        return this.defaultValue;
    }

    public abstract JsonElement serialize();

    public final void deserialize(JsonElement json) {
        this.deserializeValue(json);
        this.notifyChanged();
    }

    protected abstract void deserializeValue(JsonElement json);
}
