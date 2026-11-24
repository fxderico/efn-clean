package net.efn.client.module.impl.render;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public final class TimeChangerModule extends ClientModule {

    public static volatile TimeChangerModule active;

    private final NumberSetting time =
            this.addSetting(
                    "time", new NumberSetting("Time", 0.0F, -20000.0F, 20000.0F).step(1.0F));

    private ClientWorld trackedLevel;

    private long serverDayTime;

    private long appliedDayTime;

    public TimeChangerModule() {
        super(
                "time-changer",
                "Time Changer",
                "Makes you able to set a custom time.",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        active = this;
        this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null) {
            this.applyForcedTime(world);
        }
    }

    @Override
    protected void onDisable() {
        active = null;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null && world == this.trackedLevel) {
            writeDayTime(world, this.currentServerDayTime(world));
        }
        this.trackedLevel = null;
    }

    private void onPostTick(TickEvent event) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            this.trackedLevel = null;
        } else {
            this.applyForcedTime(world);
        }
    }

    private void applyForcedTime(ClientWorld trackedLevel2) {
        this.serverDayTime =
                trackedLevel2 == this.trackedLevel
                        ? this.currentServerDayTime(trackedLevel2)
                        : trackedLevel2.getTimeOfDay();
        this.trackedLevel = trackedLevel2;
        this.appliedDayTime = this.forcedDayTime();
        writeDayTime(trackedLevel2, this.appliedDayTime);
    }

    private static void writeDayTime(ClientWorld world, long timeOfDay) {
        if (world.getTimeOfDay() != timeOfDay) {
            world.getLevelProperties().setTimeOfDay(timeOfDay);
            world.getEnvironmentAttributes().tick();
        }
    }

    private long currentServerDayTime(ClientWorld world) {
        return this.serverDayTime + (world.getTimeOfDay() - this.appliedDayTime);
    }

    public long interceptServerDayTime(ClientWorld trackedLevel2, long serverDayTime2) {
        this.trackedLevel = trackedLevel2;
        this.serverDayTime = serverDayTime2;
        this.appliedDayTime = this.forcedDayTime();
        return this.appliedDayTime;
    }

    private long forcedDayTime() {
        return (long) this.time.get();
    }
}
