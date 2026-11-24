package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
final class FileDialogs {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String PATTERN = "*.json";

    private static final String DESCRIPTION = "EFN profile (*.json)";

    private static volatile boolean open;

    private static volatile Path lastFolder;

    private FileDialogs() {}

    static boolean busy() {
        return open;
    }

    static void openProfile(String string, Consumer<Path> consumer) {
        show(
                memoryStack ->
                        TinyFileDialogs.tinyfd_openFileDialog(
                                string,
                                startIn(null),
                                filters(memoryStack),
                                "EFN profile (*.json)",
                                false),
                consumer);
    }

    static void saveProfile(String string, String string2, Consumer<Path> consumer) {
        show(
                memoryStack ->
                        TinyFileDialogs.tinyfd_saveFileDialog(
                                string,
                                startIn(string2),
                                filters(memoryStack),
                                "EFN profile (*.json)"),
                consumer);
    }

    private static PointerBuffer filters(MemoryStack memoryStack) {
        return memoryStack.mallocPointer(1).put(memoryStack.UTF8("*.json")).flip();
    }

    private static String startIn(String string) {
        Path path = lastFolder;
        if (path == null || !Files.isDirectory(path)) {
            path = defaultFolder();
        }
        return string == null ? path + File.separator : path.resolve(string).toString();
    }

    private static Path defaultFolder() {
        try {
            Path path = Path.of(System.getProperty("user.home", "."));
            Path path2 = path.resolve("Downloads");
            return Files.isDirectory(path2) ? path2 : path;
        } catch (InvalidPathException exception) {
            return Path.of(".").toAbsolutePath();
        }
    }

    private static void show(Function<MemoryStack, String> function, Consumer<Path> consumer) {
        if (!open) {
            open = true;
            Thread thread =
                    new Thread(
                            () -> {
                                String string = null;
                                try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                                    string = (String) function.apply(memoryStack);
                                } catch (Throwable exception) {
                                    LOGGER.warn("[EFN] File dialog failed", exception);
                                } finally {
                                    open = false;
                                }
                                if (string != null && !string.isBlank()) {
                                    Path absolutePath;
                                    try {
                                        absolutePath = Path.of(string).toAbsolutePath();
                                    } catch (InvalidPathException exception2) {
                                        return;
                                    }
                                    if (absolutePath.getParent() != null) {
                                        lastFolder = absolutePath.getParent();
                                    }
                                    MinecraftClient.getInstance()
                                            .execute(() -> consumer.accept(absolutePath));
                                }
                            },
                            "EFN file dialog");
            thread.setDaemon(true);
            thread.start();
        }
    }
}
