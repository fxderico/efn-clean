package net.efn.client.util.staff;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstance.AttenuationType;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayDeque;

@Environment(EnvType.CLIENT)
public final class StaffAlarm {

    public static final String BEEP = "Beep";

    public static final String BELL = "Bell";

    public static final String GUARDIAN = "Guardian";

    public static final String OFF = "Off";

    private static final int SIREN_INTERVAL = 4;

    private static final float SIREN_HIGH = 1.5F;

    private static final float SIREN_LOW = 1.0F;

    private static final int BEEP_INTERVAL = 3;

    private static final float BEEP_PITCH = 1.6F;

    private static final int BELL_INTERVAL = 10;

    private static final int GUARDIAN_INTERVAL = 60;

    private static final int MAX_RINGING = 8;

    private String mode = "Off";

    private float volume;

    private int ticksLeft;

    private int step;

    private final ArrayDeque<SoundInstance> ringing = new ArrayDeque<>(8);

    public StaffAlarm() {}

    public void start(String mode2, float f, float g) {
        if (mode2 != null && !"Off".equals(mode2) && !(g <= 0.0F) && !(f <= 0.0F)) {
            int i = Math.max(1, Math.round(f * 20.0F));
            this.volume = Math.min(1.0F, g);
            if (this.isPlaying() && mode2.equals(this.mode)) {
                this.ticksLeft = Math.max(this.ticksLeft, i);
            } else {
                this.stop();
                this.mode = mode2;
                this.ticksLeft = i;
                this.step = 0;
            }
        }
    }

    public void tick() {
        if (this.ticksLeft > 0) {
            int interval = interval(this.mode);
            if (this.step % interval == 0) {
                this.play(this.step / interval);
            }
            this.step++;
            if (--this.ticksLeft == 0) {
                this.step = 0;
            }
        }
    }

    public void stop() {
        this.ticksLeft = 0;
        this.step = 0;
        if (!this.ringing.isEmpty()) {
            SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
            if (soundManager != null) {
                this.ringing.forEach(soundInstance -> soundManager.stop(soundInstance));
            }
            this.ringing.clear();
        }
    }

    public boolean isPlaying() {
        return this.ticksLeft > 0;
    }

    private void play(int i) {
        SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
        if (soundManager != null) {
            String mode2 = this.mode;
            SoundEvent bLOCK_NOTE_BLOCK_BIT;
            float f =
                    switch (mode2) {
                        case "Beep" -> {
                            bLOCK_NOTE_BLOCK_BIT = SoundEvents.BLOCK_NOTE_BLOCK_BIT.value();
                            yield 1.6F;
                        }
                        case "Bell" -> {
                            bLOCK_NOTE_BLOCK_BIT = SoundEvents.BLOCK_BELL_USE;
                            yield 1.0F;
                        }
                        case "Guardian" -> {
                            bLOCK_NOTE_BLOCK_BIT = SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE;
                            yield 1.0F;
                        }
                        default -> {
                            bLOCK_NOTE_BLOCK_BIT = SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
                            yield i % 2 == 0 ? 1.5F : 1.0F;
                        }
                    };
            PositionedSoundInstance positionedSoundInstance =
                    new PositionedSoundInstance(
                            bLOCK_NOTE_BLOCK_BIT.id(),
                            SoundCategory.MASTER,
                            this.volume,
                            f,
                            SoundInstance.createRandom(),
                            false,
                            0,
                            AttenuationType.NONE,
                            0.0,
                            0.0,
                            0.0,
                            true);
            if (this.ringing.size() >= 8) {
                this.ringing.removeFirst();
            }
            this.ringing.addLast(positionedSoundInstance);
            soundManager.play(positionedSoundInstance);
        }
    }

    private static int interval(String string) {
        return switch (string) {
            case "Beep" -> 3;
            case "Bell" -> 10;
            case "Guardian" -> 60;
            default -> 4;
        };
    }
}
