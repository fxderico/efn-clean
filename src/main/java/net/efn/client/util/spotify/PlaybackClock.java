package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PlaybackClock {

    static final long SNAP_MS = 1200L;

    static final long CATCHUP_MS = 1500L;

    private NowPlaying sample;

    private boolean running;

    private long durationMs;

    private double rate = 1.0;

    private double base;

    private long baseAt;

    private double correction;

    public PlaybackClock() {}

    public void update(NowPlaying sample2, long baseAt2) {
        if (sample2 != this.sample) {
            NowPlaying sample3 = this.sample;
            this.sample = sample2;
            if (sample2 != null && sample2.hasTimeline()) {
                double position = this.position(baseAt2);
                double position2 = sample2.positionAt(baseAt2);
                boolean bl =
                        sample3 != null
                                && sample3.hasTimeline()
                                && sample3.sameTrack(sample2)
                                && this.running
                                && sample2.playing()
                                && Math.abs(position2 - position)
                                        < 1200.0 * Math.min(1.0, sample2.rate());
                this.base = bl ? position : position2;
                this.correction = bl ? position2 - position : 0.0;
                this.baseAt = baseAt2;
                this.running = sample2.playing();
                this.durationMs = sample2.durationMs();
                this.rate = sample2.rate();
            } else {
                this.running = false;
                this.durationMs = 0L;
                this.base = 0.0;
                this.correction = 0.0;
                this.baseAt = baseAt2;
            }
        }
    }

    public long positionAt(long l) {
        return this.durationMs <= 0L
                ? 0L
                : Math.max(0L, Math.min(this.durationMs, Math.round(this.position(l))));
    }

    private double position(long l) {
        if (this.durationMs <= 0L) {
            return 0.0;
        } else if (!this.running) {
            return this.base;
        } else {
            double d = Math.max(0L, l - this.baseAt);
            return this.base + d * this.rate + this.correction * Math.min(1.0, d / 1500.0);
        }
    }
}
