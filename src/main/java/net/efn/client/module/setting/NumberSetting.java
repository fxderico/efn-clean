package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.math.BigDecimal;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class NumberSetting extends Setting {

    private static final int MAX_DECIMALS = 4;

    private static final double[] POW10 = new double[] {1.0, 10.0, 100.0, 1000.0, 10000.0};

    private static final String[] FORMATS = new String[] {"%.0f", "%.1f", "%.2f", "%.3f", "%.4f"};

    private final float[] value;

    private final float min;

    private final float max;

    private float step;

    private int stepDecimals;

    private double gridMin;

    private double gridStep;

    public NumberSetting(String string, float f, float min2, float max2) {
        super(string);
        this.value = new float[] {f};
        this.min = min2;
        this.max = max2;
    }

    public NumberSetting step(float step2) {
        if (step2 >= 0.0F && !Float.isInfinite(step2)) {
            this.step = step2;
            this.stepDecimals =
                    step2 > 0.0F ? Math.max(decimalsOf(step2), decimalsOf(this.min)) : 0;
            this.gridMin = this.toDecimals(this.min);
            double decimals = this.toDecimals(step2);
            this.gridStep = decimals > 0.0 ? decimals : step2;
            this.set(this.value[0]);
            return this;
        } else {
            throw new IllegalArgumentException(
                    "Invalid step " + step2 + " for setting '" + this.name() + "'");
        }
    }

    public float step() {
        return this.step;
    }

    public float get() {
        return this.value[0];
    }

    public int getInt() {
        return Math.round(this.value[0]);
    }

    public void set(float f) {
        float snap = this.snap(f);
        if (this.value[0] != snap) {
            this.value[0] = snap;
            this.notifyChanged();
        }
    }

    public float snap(float f) {
        float g = this.clamp(f);
        if (this.step <= 0.0F) {
            return g;
        } else {
            long l = Math.round((g - this.gridMin) / this.gridStep + 1.0E-9);
            float onGrid = this.onGrid(l);
            if (onGrid > this.max) {
                onGrid = this.onGrid(l - 1L);
            }
            return Math.max(this.min, Math.min(this.max, onGrid));
        }
    }

    public int decimals() {
        if (this.step > 0.0F) {
            return this.stepDecimals;
        } else {
            float f = this.max - this.min;
            if (this.min == Math.rint(this.min) && this.max == Math.rint(this.max) && f >= 20.0F) {
                return 0;
            } else {
                return f <= 2.0F ? 2 : 1;
            }
        }
    }

    public String formatted() {
        int decimals = this.decimals();
        return decimals == 0
                ? Integer.toString(Math.round(this.value[0]))
                : String.format(Locale.ROOT, FORMATS[decimals], this.value[0]);
    }

    public float[] value() {
        return this.value;
    }

    public float min() {
        return this.min;
    }

    public float max() {
        return this.max;
    }

    private float clamp(float f) {
        if (Float.isNaN(f)) {
            return this.value[0];
        } else if (f < this.min) {
            return this.min;
        } else {
            return f > this.max ? this.max : f;
        }
    }

    private float onGrid(long l) {
        return (float) this.toDecimals(this.gridMin + l * this.gridStep);
    }

    private double toDecimals(double d) {
        double e = POW10[this.stepDecimals];
        return Math.round(d * e) / e;
    }

    private static int decimalsOf(float f) {
        if (!Float.isFinite(f)) {
            return 0;
        } else {
            int scale = new BigDecimal(Float.toString(f)).stripTrailingZeros().scale();
            return Math.max(0, Math.min(4, scale));
        }
    }

    @Override
    public JsonElement serialize() {
        return new JsonPrimitive(this.value[0]);
    }

    @Override
    protected void deserializeValue(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            try {
                this.value[0] = this.snap(json.getAsFloat());
            } catch (RuntimeException exception) {
            }
        }
    }
}
