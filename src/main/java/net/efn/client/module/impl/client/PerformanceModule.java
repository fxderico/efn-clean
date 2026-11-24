package net.efn.client.module.impl.client;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.esp.ChunkScanner;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.gui.EfnWidgets;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.PerformanceBudget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class PerformanceModule extends ClientModule implements HudOverlayProvider {

    private static volatile boolean optimizing;

    private final BooleanSetting optimize =
            this.addSetting("optimize-client", new BooleanSetting("Optimize Client", true));

    private final BooleanSetting showStats =
            this.addSetting("show-stats", new BooleanSetting("Show Stats", true));

    private final BooleanSetting frustumCulling =
            this.addSetting("frustum-culling", new BooleanSetting("Frustum Culling", true));

    private final NumberSetting scanBudget =
            this.addSetting(
                    "scan-budget-ms",
                    new NumberSetting("Scan Budget ms", 2.0F, 0.5F, 8.0F).step(0.5F));

    private long smoothedNanos;

    private long nextStatsUpdate;

    private String statsText = "";

    public PerformanceModule() {
        super(
                "performance",
                "Performance",
                "Reduces background work and image memory while keeping effects and features."
                        + " Disable to compare FPS.",
                ModuleCategory.CLIENT);
        this.optimize.onChange(this::updateOptimizationState);
    }

    public static boolean optimizationsEnabled() {
        return optimizing;
    }

    @Override
    protected void onEnable() {
        this.updateOptimizationState();
        this.nextStatsUpdate = 0L;
    }

    @Override
    protected void onDisable() {
        optimizing = false;
    }

    private void updateOptimizationState() {
        optimizing = this.isEnabled() && this.optimize.get();
        this.nextStatsUpdate = 0L;
    }

    public static boolean frustumCulling() {
        PerformanceModule module = ModuleRegistry.get(PerformanceModule.class);
        return module == null || module.frustumCulling.get();
    }

    public static long scanBudgetNanos() {
        PerformanceModule module = ModuleRegistry.get(PerformanceModule.class);
        return module == null ? 2000000L : (long) (module.scanBudget.get() * 1000000.0F);
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && this.showStats.get();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        this.smoothedNanos =
                this.smoothedNanos + (Render3DEvent.lastDurationNanos() - this.smoothedNanos) / 8L;
        long l = System.nanoTime();
        if (l >= this.nextStatsUpdate) {
            String string =
                    optimizationsEnabled()
                            ? String.format(
                                    Locale.ROOT,
                                    "  \u00b7  scans %.2f ms",
                                    PerformanceBudget.lastTickNanos() / 1000000.0)
                            : "";
            this.statsText =
                    String.format(
                            Locale.ROOT,
                            "%s  \u00b7  %d FPS  \u00b7  3D CPU %.2f ms%s  \u00b7  queue %d",
                            optimizationsEnabled() ? "Optimized" : "Standard",
                            MinecraftClient.getInstance().getCurrentFps(),
                            this.smoothedNanos / 1000000.0,
                            string,
                            ChunkScanner.pending());
            this.nextStatsUpdate = l + 250000000L;
        }
        String statsText2 = this.statsText;
        ImFont imFont = ImGuiManager.getInstance().hudLabelFont();
        float h = f - EfnWidgets.textWidth(imFont, statsText2) - 8.0F;
        float f2 = g - EfnWidgets.lineHeight(imFont) - 6.0F;
        EfnWidgets.text(imDrawList, imFont, h + 1.0F, f2 + 1.0F, -1610612736, statsText2);
        EfnWidgets.text(imDrawList, imFont, h, f2, -3618616, statsText2);
    }
}
