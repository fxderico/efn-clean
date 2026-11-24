package net.efn.client.service;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
public final class TickJitter {

    private static final float VARIANCE = 0.2F;

    private TickJitter() {}

    public static int apply(int i) {
        if (i <= 0) {
            return i;
        } else {
            int j = Math.max(1, Math.round(i * 0.2F));
            int k = ThreadLocalRandom.current().nextInt(-j, j + 1);
            return Math.max(1, i + k);
        }
    }
}
