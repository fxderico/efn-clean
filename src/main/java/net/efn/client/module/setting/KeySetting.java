package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.efn.client.input.ModuleKeybinds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class KeySetting extends Setting {

    private int value;

    public KeySetting(String string, int value2) {
        super(string);
        this.value = value2;
    }

    public int value() {
        return this.value;
    }

    public void setValue(int value2) {
        if (this.value != value2) {
            this.value = value2;
            this.notifyChanged();
        }
    }

    @Override
    public JsonElement serialize() {
        return new JsonPrimitive(this.value);
    }

    @Override
    protected void deserializeValue(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            try {
                int i = json.getAsInt();
                if (i == -1 || ModuleKeybinds.isBindable(i)) {
                    this.value = i;
                }
            } catch (RuntimeException exception) {
            }
        }
    }
}
