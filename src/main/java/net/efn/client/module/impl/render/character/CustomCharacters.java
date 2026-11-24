package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.gltf.CharacterBake;
import net.efn.client.module.impl.render.character.gltf.ImageDecoder;
import net.efn.client.module.impl.render.character.kit.BakedDesign;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Environment(EnvType.CLIENT)
final class CustomCharacters {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final long MAX_FILE = 268435456L;

    private static final int MAX_TEXTURE = 8192;

    private CustomCharacters() {}

    static Path folder() {
        return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("characters");
    }

    private static Path cache() {
        return folder().resolve(".baked");
    }

    static List<BakedDesign> scan() {
        ArrayList list = new ArrayList();
        try (DirectoryStream directoryStream = Files.newDirectoryStream(folder(), "*.{glb,GLB}")) {
            for (Path path : (Iterable<Path>) directoryStream) {
                if (Files.isRegularFile(path)) {
                    list.add(path);
                }
            }
        } catch (IOException exception) {
            return List.of();
        }
        list.sort(null);
        ArrayList list2 = new ArrayList();
        HashSet set = new HashSet();
        for (Path path2 : (Iterable<Path>) list) {
            String string = path2.getFileName().toString();
            string = string.substring(0, string.length() - 4);
            try {
                String string2 =
                        string
                                + Files.size(path2)
                                + Files.getLastModifiedTime(path2).toMillis()
                                + "3";
                String string3 =
                        "custom_" + slug(string) + "_" + Integer.toHexString(string2.hashCode());
                set.add(string3);
                list2.add(new BakedDesign(string3, string, () -> open(path2, string3)));
            } catch (IOException exception2) {
                LOGGER.warn("[EFN] Skipping custom character {}: {}", path2, exception2.toString());
            }
        }
        prune(set);
        return list2;
    }

    private static String slug(String string) {
        String string2 =
                string.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");
        return string2.isEmpty()
                ? "model"
                : (string2.length() > 32 ? string2.substring(0, 32) : string2);
    }

    private static void prune(Set<String> set) {
        try (DirectoryStream directoryStream = Files.newDirectoryStream(cache(), "*.anc")) {
            for (Path path : (Iterable<Path>) directoryStream) {
                String string = path.getFileName().toString().replace(".anc", "");
                if (!set.contains(string)) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException exception) {
        }
    }

    private static synchronized InputStream open(Path path, String string) throws IOException {
        Path path2 = cache().resolve(string + ".anc");
        if (Files.exists(path2)) {
            return Files.newInputStream(path2);
        } else if (Files.size(path) > 268435456L) {
            throw new IOException("the file is larger than 256 MB");
        } else {
            String string2 = path.getFileName().toString();
            CharacterModule.baking(string2);
            long l = System.nanoTime();
            CharacterBake.Result result =
                    CharacterBake.bake(
                            Files.readAllBytes(path),
                            new CharacterBake.Options(),
                            CustomCharacters::decode);
            LOGGER.info(
                    "[EFN] Baked custom character {} in {} ms, moving {}",
                    string2,
                    (System.nanoTime() - l) / 1000000L,
                    result.limbs());
            if (result.limbs().size() < 3) {
                CharacterModule.notice(string2, result.limbs());
            }
            Files.createDirectories(cache());
            Path path3 = cache().resolve(string + ".tmp");
            Files.write(path3, result.data());
            Files.move(path3, path2, StandardCopyOption.REPLACE_EXISTING);
            return new ByteArrayInputStream(result.data());
        }
    }

    private static ImageDecoder.Picture decode(byte[] bytes) throws IOException {
        ByteBuffer byteBuffer = MemoryUtil.memAlloc(bytes.length);
        ImageDecoder.Picture picture;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            byteBuffer.put(bytes).flip();
            IntBuffer intBuffer = memoryStack.mallocInt(1);
            IntBuffer intBuffer2 = memoryStack.mallocInt(1);
            IntBuffer intBuffer3 = memoryStack.mallocInt(1);
            ByteBuffer byteBuffer2 =
                    STBImage.stbi_load_from_memory(
                            byteBuffer, intBuffer, intBuffer2, intBuffer3, 4);
            if (byteBuffer2 == null) {
                throw new IOException("a texture can't be read: " + STBImage.stbi_failure_reason());
            }
            try {
                int i = intBuffer.get(0);
                int j = intBuffer2.get(0);
                if (i > 8192 || j > 8192) {
                    throw new IOException("a texture is over 8192 px");
                }
                int[] ints = new int[i * j];
                for (int k = 0; k < ints.length; k++) {
                    int m = byteBuffer2.get(k * 4) & 255;
                    int n = byteBuffer2.get(k * 4 + 1) & 255;
                    int o = byteBuffer2.get(k * 4 + 2) & 255;
                    int p = byteBuffer2.get(k * 4 + 3) & 255;
                    ints[k] = p << 24 | m << 16 | n << 8 | o;
                }
                picture = new ImageDecoder.Picture(i, j, ints);
            } finally {
                STBImage.stbi_image_free(byteBuffer2);
            }
        } finally {
            MemoryUtil.memFree(byteBuffer);
        }
        return picture;
    }
}
