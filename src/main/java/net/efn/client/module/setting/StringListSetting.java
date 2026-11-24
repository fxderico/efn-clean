/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import imgui.type.ImString;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(value = EnvType.CLIENT)
public final class StringListSetting extends Setting {

    public static final int MAX_ENTRIES = 64;

    private static final int ENTRY_CAPACITY = 256;

    private static final char LEGACY_SEPARATOR = '|';

    private final List<ImString> entries = new ArrayList<ImString>();

    private List<String> values;

    private boolean itemPicker;

    public StringListSetting(String string, String... string2) {
        super(string);
        for (String string3 : string2) {
            this.entries.add(new ImString(string3, 256));
        }
    }

    public StringListSetting itemPicker() {
        this.itemPicker = true;
        return this;
    }

    public boolean isItemPicker() {
        return this.itemPicker;
    }

    public boolean addValue(String string) {
        if (!this.canAdd() || string == null || string.isBlank()) {
            return false;
        }
        this.entries.add(new ImString(string, 256));
        this.changed();
        return true;
    }

    public boolean removeValue(String string) {
        for (int i = 0; i < this.entries.size(); ++i) {
            if (!this.entries.get(i).get().trim().equalsIgnoreCase(string)) continue;
            this.entries.remove(i);
            this.changed();
            return true;
        }
        return false;
    }

    public void clear() {
        if (this.entries.isEmpty()) {
            return;
        }
        this.entries.clear();
        this.changed();
    }

    public List<String> get() {
        if (this.values == null) {
            ArrayList<String> list = new ArrayList<String>(this.entries.size());
            for (ImString entry : this.entries) {
                String string = entry.get().trim();
                if (string.isEmpty()) continue;
                list.add(string);
            }
            this.values = List.copyOf(list);
        }
        return this.values;
    }

    public int size() {
        return this.entries.size();
    }

    public ImString entry(int i) {
        return this.entries.get(i);
    }

    public boolean canAdd() {
        return this.entries.size() < 64;
    }

    public void add() {
        if (!this.canAdd()) {
            return;
        }
        this.entries.add(new ImString("", 256));
        this.changed();
    }

    public void remove(int i) {
        if (i < 0 || i >= this.entries.size()) {
            return;
        }
        this.entries.remove(i);
        this.changed();
    }

    public void edited() {
        this.changed();
    }

    private void changed() {
        this.values = null;
        this.notifyChanged();
    }

    @Override
    public JsonElement serialize() {
        JsonArray array = new JsonArray();
        for (ImString entry : this.entries) {
            array.add(entry.get());
        }
        return array;
    }

    @Override
    protected void deserializeValue(JsonElement json) {
        List<String> list;
        if (json == null) {
            return;
        }
        if (json.isJsonArray()) {
            list = new ArrayList();
            for (JsonElement json2 : json.getAsJsonArray()) {
                if (json2 == null || !json2.isJsonPrimitive()) continue;
                list.add(json2.getAsString());
            }
        } else if (json.isJsonPrimitive()) {
            list = StringListSetting.splitLegacy(json.getAsString());
        } else {
            return;
        }
        this.entries.clear();
        for (int i = 0; i < list.size() && i < 64; ++i) {
            this.entries.add(new ImString((String) list.get(i), 256));
        }
        this.values = null;
    }

    private static List<String> splitLegacy(String string) {
        ArrayList<String> list = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c == '\\' && i + 1 < string.length() && string.charAt(i + 1) == '|') {
                sb.append('|');
                ++i;
                continue;
            }
            if (c == '|') {
                StringListSetting.addTrimmed(list, sb);
                sb.setLength(0);
                continue;
            }
            sb.append(c);
        }
        StringListSetting.addTrimmed(list, sb);
        return list;
    }

    private static void addTrimmed(List<String> list, CharSequence charSequence) {
        String string = charSequence.toString().trim();
        if (!string.isEmpty()) {
            list.add(string);
        }
    }
}
