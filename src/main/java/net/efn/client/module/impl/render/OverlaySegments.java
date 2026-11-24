package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
final class OverlaySegments {

    private List<double[]> source;

    private boolean varied;

    private int count;

    private double[] points = new double[0];

    private int[] colors = new int[0];

    OverlaySegments() {}

    void update(List<double[]> source2, boolean varied2) {
        if (this.source != source2 || this.varied != varied2) {
            this.count = 0;
            for (double[] doubles : source2) {
                this.count = this.count + subdivisions(doubles, varied2);
            }
            if (this.colors.length < this.count) {
                int i = Math.max(this.count, this.colors.length * 2);
                this.points = Arrays.copyOf(this.points, i * 9);
                this.colors = Arrays.copyOf(this.colors, i);
            }
            int j = 0;
            for (double[] doubles2 : source2) {
                int subdivisions = subdivisions(doubles2, varied2);
                for (int m = 0; m < subdivisions; m++) {
                    double d = (double) m / subdivisions;
                    double e = (double) (m + 1) / subdivisions;
                    double d2 = (d + e) * 0.5;
                    for (int n = 0; n < 3; n++) {
                        double e2 = doubles2[n];
                        double d3 = doubles2[n + 3];
                        this.points[j + n] = e2 + d * (d3 - e2);
                        this.points[j + n + 3] = e2 + e * (d3 - e2);
                        this.points[j + n + 6] = e2 + d2 * (d3 - e2);
                    }
                    j += 9;
                }
            }
            this.source = source2;
            this.varied = varied2;
        }
    }

    private static int subdivisions(double[] doubles, boolean bl) {
        if (!bl) {
            return 1;
        } else {
            double d = doubles[3] - doubles[0];
            double e = doubles[4] - doubles[1];
            double d2 = doubles[5] - doubles[2];
            double e2 = Math.sqrt(d * d + e * e + d2 * d2);
            return Math.max(1, Math.min(16, (int) Math.ceil(e2 / 0.125)));
        }
    }

    int count() {
        return this.count;
    }

    double[] points() {
        return this.points;
    }

    int[] colors() {
        return this.colors;
    }

    void invalidate() {
        this.source = null;
    }

    void clear() {
        this.source = null;
        this.count = 0;
        this.points = new double[0];
        this.colors = new int[0];
    }
}
