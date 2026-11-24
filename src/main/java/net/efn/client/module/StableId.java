package net.efn.client.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class StableId {

    private StableId() {}

    public static String validate(String string) {
        if (!isValid(string)) {
            throw new IllegalArgumentException("Invalid stable id: " + string);
        } else {
            return string;
        }
    }

    public static String legacy(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(string2.length());
        boolean bl = false;
        for (int i = 0; i < string2.length(); i++) {
            char c = string2.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                if (bl && !sb.isEmpty()) {
                    sb.append('-');
                }
                bl = false;
                sb.append(c);
            } else {
                bl = true;
            }
        }
        return validate(sb.toString());
    }

    private static boolean isValid(String string) {
        if (string != null && !string.isEmpty()) {
            boolean bl = true;
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if ((c < 'a' || c > 'z') && (c < '0' || c > '9')) {
                    if (c != '.' && c != '_' && c != '-') {
                        return false;
                    }
                    if (bl) {
                        return false;
                    }
                    bl = true;
                } else {
                    bl = false;
                }
            }
            return !bl;
        } else {
            return false;
        }
    }
}
