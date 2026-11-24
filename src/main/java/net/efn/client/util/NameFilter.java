package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.UnaryOperator;

@Environment(EnvType.CLIENT)
public final class NameFilter {

    private static volatile UnaryOperator<String> filter = UnaryOperator.identity();

    private NameFilter() {}

    public static String apply(String string) {
        if (string == null) {
            return null;
        } else {
            String string2 = filter.apply(string);
            return string2 != null ? string2 : string;
        }
    }

    public static void set(UnaryOperator<String> unaryOperator) {
        filter = unaryOperator != null ? unaryOperator : UnaryOperator.identity();
    }
}
