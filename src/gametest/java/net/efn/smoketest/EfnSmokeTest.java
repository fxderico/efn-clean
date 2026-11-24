package net.efn.smoketest;

import net.efn.client.gui.EfnClickGui;
import net.efn.client.gui.ImGuiScreen;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

import org.spongepowered.asm.mixin.MixinEnvironment;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Production smoke test (run by {@code ./gradlew prodClientSmokeTest}, also in CI).
 *
 * <p>Boots the real remapped jar to the title screen, then runs Mixin's audit, which force-loads
 * every mixin target class and applies all of EFN's injections. EFN's mixin configs are {@code
 * required}, so any injection that can't be applied (wrong target, bad descriptor) throws and the
 * game exits non-zero, failing the build.
 *
 * <p>It then initializes every EFN class (runs each static initializer), which catches decompiler
 * mistakes in {@code static {}} blocks that only blow up the first time a feature is used.
 *
 * <p>With {@code -Defn.guipreview=true} it also opens the click GUI on the Bots tab and a few bot
 * pages, logging {@code EFN_PREVIEW <name>} at each step so the screen can be captured.
 */
public class EfnSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitTicks(40);
        context.takeScreenshot("efn-title-screen");
        context.runOnClient(client -> MixinEnvironment.getCurrentEnvironment().audit());
        context.waitTicks(20);
        context.takeScreenshot("efn-after-mixin-audit");
        context.runOnClient(client -> initializeAllClasses());

        if (Boolean.getBoolean("efn.guipreview")) {
            context.setScreen(ImGuiScreen::new);
            context.waitTicks(40);
            preview(context, "bots-tab", null);
            for (String name :
                    new String[] {"Schematic Builder", "Crystal PvP Bot", "Tpahere Kill Bot"}) {
                preview(context, name, name);
            }
            context.setScreen(() -> null);
            context.waitTicks(20);
        }
    }

    private static void preview(ClientGameTestContext context, String label, String moduleName) {
        context.runOnClient(
                client -> {
                    try {
                        EfnClickGui gui =
                                (EfnClickGui)
                                        field(ImGuiScreen.class, "clickGui")
                                                .get(client.currentScreen);
                        field(EfnClickGui.class, "category").set(gui, ModuleCategory.BOTS);
                        ClientModule module = null;
                        for (ClientModule m : ModuleRegistry.all()) {
                            if (m.name().equals(moduleName)) module = m;
                        }
                        field(EfnClickGui.class, "detail").set(gui, module);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                });
        context.waitTicks(40);
        System.out.println("EFN_PREVIEW " + label);
        context.waitTicks(100);
    }

    /**
     * Runs the static initializer of every EFN class except mixins (Mixin forbids loading those).
     */
    private static void initializeAllClasses() {
        Path root =
                FabricLoader.getInstance()
                        .getModContainer("efn")
                        .orElseThrow()
                        .getRootPaths()
                        .get(0);
        List<String> names;
        try (Stream<Path> files = Files.walk(root)) {
            names =
                    files.map(p -> root.relativize(p).toString().replace('\\', '/'))
                            .filter(
                                    n ->
                                            n.endsWith(".class")
                                                    && !n.contains("/mixin/")
                                                    && !n.contains("module-info"))
                            .map(n -> n.substring(0, n.length() - 6).replace('/', '.'))
                            .sorted()
                            .toList();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        ClassLoader loader = EfnSmokeTest.class.getClassLoader();
        List<String> failures = new ArrayList<>();
        int ok = 0, skipped = 0;
        for (String name : names) {
            try {
                Class.forName(name, true, loader);
                ok++;
            } catch (NoClassDefFoundError e) {
                // Optional integrations (Litematica/MaLiLib) aren't installed in the test client.
                if (String.valueOf(e.getMessage()).contains("fi/dy/masa")) {
                    skipped++;
                } else {
                    failures.add(name + ": " + e);
                }
            } catch (UnsatisfiedLinkError e) {
                // Windows-only JNA bindings (user32, combase) can't load on the Linux CI runner.
                if (System.getProperty("os.name").startsWith("Windows")) {
                    failures.add(name + ": " + e);
                } else {
                    skipped++;
                }
            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                failures.add(name + ": " + t + " caused by " + cause);
            }
        }
        System.out.println(
                "EFN_CLINIT initialized="
                        + ok
                        + " skipped(optional/windows-only)="
                        + skipped
                        + " failed="
                        + failures.size());
        failures.forEach(f -> System.out.println("EFN_CLINIT_FAIL " + f));
        if (!failures.isEmpty()) {
            throw new AssertionError(
                    failures.size()
                            + " EFN classes failed to initialize, first: "
                            + failures.get(0));
        }
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
