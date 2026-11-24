package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class StableWidth {

    static final float SHRINK_DELAY = 1.5F;

    private static final float SHRINK_RATE = 12.0F;

    private static final float SNAP = 0.25F;

    private float current = -1.0F;

    private float narrowerFor;

    StableWidth() {}

    float update(float current2, float narrowerFor2) {
        if (!(this.current < 0.0F) && !(current2 >= this.current)) {
            this.narrowerFor += narrowerFor2;
            if (this.narrowerFor < 1.5F) {
                return this.current;
            } else {
                this.current =
                        this.current
                                + (current2 - this.current)
                                        * (1.0F - (float) Math.exp(-12.0F * narrowerFor2));
                if (Math.abs(this.current - current2) < 0.25F) {
                    this.current = current2;
                }
                return this.current;
            }
        } else {
            this.current = current2;
            this.narrowerFor = 0.0F;
            return this.current;
        }
    }
}
