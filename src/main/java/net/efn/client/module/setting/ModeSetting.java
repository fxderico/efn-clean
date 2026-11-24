package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ModeSetting extends Setting {

    private List<String> modes;

    private int selectedIndex;

    private String selected;

    public ModeSetting(String string, String string2, String... string3) {
        super(string);
        this.modes = List.copyOf(Arrays.asList(string3));
        this.select(Math.max(0, this.modes.indexOf(string2)));
    }

    public String get() {
        return this.selected;
    }

    public boolean is(String string) {
        return this.selected == string || this.selected.equals(string);
    }

    public void set(String string) {
        int i = this.modes.indexOf(string);
        if (i >= 0 && i != this.selectedIndex) {
            this.select(i);
            this.notifyChanged();
        }
    }

    public String value() {
        return this.get();
    }

    public List<String> modes() {
        return this.modes;
    }

    public void setModes(List<String> list) {
        if (!list.isEmpty() && !list.equals(this.modes)) {
            this.modes = List.copyOf(list);
            int i = this.modes.indexOf(this.selected);
            if (i >= 0) {
                this.selectedIndex = i;
            } else {
                this.select(0);
                this.notifyChanged();
            }
        }
    }

    public void next() {
        if (this.modes.size() >= 2) {
            this.select((this.selectedIndex + 1) % this.modes.size());
            this.notifyChanged();
        }
    }

    private void select(int selectedIndex2) {
        this.selectedIndex = selectedIndex2;
        this.selected = this.modes.get(selectedIndex2);
    }

    @Override
    public JsonElement serialize() {
        return new JsonPrimitive(this.get());
    }

    @Override
    protected void deserializeValue(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            try {
                int i = this.modes.indexOf(json.getAsString());
                if (i >= 0) {
                    this.select(i);
                }
            } catch (RuntimeException exception) {
            }
        }
    }
}
