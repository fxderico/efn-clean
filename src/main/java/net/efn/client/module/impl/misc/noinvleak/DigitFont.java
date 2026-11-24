package net.efn.client.module.impl.misc.noinvleak;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
final class DigitFont {

    private static final byte[][] DIGITS =
            new byte[][] {
                {14, 17, 19, 21, 25, 17, 14},
                {4, 12, 4, 4, 4, 4, 14},
                {14, 17, 1, 6, 8, 16, 31},
                {14, 17, 1, 6, 1, 17, 14},
                {2, 6, 10, 18, 31, 2, 2},
                {31, 16, 30, 1, 1, 17, 14},
                {14, 16, 30, 17, 17, 17, 14},
                {31, 1, 2, 4, 8, 8, 8},
                {14, 17, 17, 14, 17, 17, 14},
                {14, 17, 17, 15, 1, 17, 14}
            };

    private static final int SHADOW = -12632257;

    private static final int WHITE = -1;

    private DigitFont() {}

    static void decorations(DigitFont.Sink sink, ItemStack stack, int i, int j, int k) {
        if (stack.isItemBarVisible()) {
            sink.rect(i + 2 * k, j + 13 * k, 13 * k, 2 * k, -16777216);
            sink.rect(
                    i + 2 * k,
                    j + 13 * k,
                    stack.getItemBarStep() * k,
                    k,
                    0xFF000000 | stack.getItemBarColor());
        }
        if (stack.getCount() != 1) {
            count(sink, stack.getCount(), i, j, k);
        }
    }

    private static void count(DigitFont.Sink sink, int i, int j, int k, int m) {
        String string = Integer.toString(i);
        int n = j + (17 - 6 * string.length()) * m;
        int o = k + 9 * m;
        for (int p = 0; p < string.length(); p++) {
            int q = string.charAt(p) - '0';
            if (q >= 0 && q <= 9) {
                int r = n + p * 6 * m;
                glyph(sink, DIGITS[q], r + m, o + m, m, -12632257);
                glyph(sink, DIGITS[q], r, o, m, -1);
            }
        }
    }

    private static void glyph(DigitFont.Sink sink, byte[] bytes, int i, int j, int k, int m) {
        for (int n = 0; n < bytes.length; n++) {
            int o = 0;
            while (o < 5) {
                if ((bytes[n] >> 4 - o & 1) == 0) {
                    o++;
                } else {
                    int p = o;
                    while (o < 5 && (bytes[n] >> 4 - o & 1) != 0) {
                        o++;
                    }
                    sink.rect(i + p * k, j + n * k, (o - p) * k, k, m);
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    interface Sink {

        void rect(int i, int j, int k, int m, int n);
    }
}
