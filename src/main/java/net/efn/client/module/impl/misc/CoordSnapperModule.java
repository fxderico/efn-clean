package net.efn.client.module.impl.misc;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.webhook.WebhookConfig;
import net.efn.client.webhook.WebhookEmbed;
import net.efn.client.webhook.WebhookPayloadBuilder;
import net.efn.client.webhook.WebhookService;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Environment(EnvType.CLIENT)
public final class CoordSnapperModule extends ClientModule {

    private static final String TARGET_LOOKED_AT = "Looked At";

    private static final String TARGET_OWN_POSITION = "Own Position";

    private static final String FORMAT_SPACES = "X Y Z";

    private static final String FORMAT_COMMAS = "X, Y, Z";

    private static final String FORMAT_TP = "/tp Command";

    private static final double STEP_PAST_BLOCK = 0.001;

    private static final String WEBHOOK_TITLE = "\ud83d\udccd Coordinates";

    private static final int WEBHOOK_COLOR = 3438838;

    private static final ExecutorService WEBHOOK_EXECUTOR =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "EFN-CoordSnapper-Webhook");
                        thread.setDaemon(true);
                        return thread;
                    });

    private final KeySetting key = this.addSetting("key", new KeySetting("Key", -1));

    private final ModeSetting target =
            this.addSetting(
                    "target", new ModeSetting("Target", "Looked At", "Looked At", "Own Position"));

    private final NumberSetting range =
            this.addSetting("range", new NumberSetting("Range", 256.0F, 16.0F, 512.0F).step(1.0F));

    private final BooleanSetting stopAtFluids =
            this.addSetting("stop-at-fluids", new BooleanSetting("Stop at Fluids", false));

    private final ModeSetting format =
            this.addSetting(
                    "format",
                    new ModeSetting("Format", "X Y Z", "X Y Z", "X, Y, Z", "/tp Command"));

    private final BooleanSetting includeDimension =
            this.addSetting("include-dimension", new BooleanSetting("Include Dimension", true));

    private final BooleanSetting confirm =
            this.addSetting("confirm", new BooleanSetting("Chat Confirmation", true));

    private final BooleanSetting webhook =
            this.addSetting("webhook", new BooleanSetting("Webhook", false));

    private final StringSetting webhookUrl =
            this.addSetting("webhook-url", new StringSetting("Webhook URL", ""));

    public CoordSnapperModule() {
        super(
                "coord-snapper",
                "Coord Snapper",
                "Copies the coordinates you look at or stand on with one key.",
                ModuleCategory.MISC);
        this.range.visibleWhen(() -> this.target.is("Looked At"));
        this.stopAtFluids.visibleWhen(() -> this.target.is("Looked At"));
        BooleanSetting setting = this.usesNotifications();
        this.confirm.visibleWhen(setting::get);
        this.webhookUrl.visibleWhen(this.webhook::get);
        this.webhookUrl.privateValue();
    }

    @Override
    protected void onEnable() {
        this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
        this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
    }

    private void onKey(KeyInputEvent event) {
        if (event.action == 1) {
            this.onPress(event.key);
        }
    }

    private void onMouse(MouseInputEvent event) {
        if (event.action == 1) {
            this.onPress(1000 + event.button);
        }
    }

    private void onPress(int i) {
        int key2 = this.key.value();
        if (ModuleKeybinds.isBindable(key2) && i == key2) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null && client.player != null && client.world != null) {
                this.snap(client, client.player, client.world);
            }
        }
    }

    private void snap(MinecraftClient client, ClientPlayerEntity player, ClientWorld world) {
        BlockPos pos;
        if (this.target.is("Own Position")) {
            pos = player.getBlockPos();
        } else {
            pos = this.raycast(client, player, world);
            if (pos == null) {
                ModuleChat.send(
                        this,
                        ModuleChat.body()
                                .danger("Nothing copied")
                                .text(": no loaded block within ")
                                .value(this.range.getInt())
                                .text(" blocks of your aim"));
                return;
            }
            if (this.format.is("/tp Command")) {
                pos = pos.up();
            }
        }
        String formatCoords = this.formatCoords(pos, world.getRegistryKey());
        client.keyboard.setClipboard(formatCoords);
        if (this.confirm.get()) {
            ModuleChat.send(this, ModuleChat.body().good("Copied").text(" ").value(formatCoords));
        }
        if (this.webhook.get()) {
            this.sendWebhook(client, player, pos, world.getRegistryKey());
        }
    }

    private void sendWebhook(
            MinecraftClient client,
            ClientPlayerEntity player,
            BlockPos pos,
            RegistryKey<World> key2) {
        String string = this.webhookUrl.get().trim();
        if (string.isEmpty()) {
            ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").warn("not set"));
        } else if (!WebhookConfig.isValidUrl(string)) {
            ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").danger("invalid"));
        } else {
            ServerInfo currentServerEntry = client.getCurrentServerEntry();
            String string2 =
                    currentServerEntry != null && currentServerEntry.name != null
                            ? currentServerEntry.name
                            : "Singleplayer";
            String string3 =
                    "**Coordinates:** `"
                            + String.format(
                                    Locale.ROOT, "%d %d %d", pos.getX(), pos.getY(), pos.getZ())
                            + "`\n**Dimension:** "
                            + dimensionName(key2)
                            + "\n**Server:** "
                            + escapeMarkdown(string2)
                            + "\n**Player:** "
                            + escapeMarkdown(player.getName().getString())
                            + "\n**Time:** <t:"
                            + System.currentTimeMillis() / 1000L
                            + ":R>";
            WebhookPayloadBuilder webhookPayloadBuilder =
                    new WebhookPayloadBuilder()
                            .addEmbed(
                                    new WebhookEmbed(
                                            "\ud83d\udccd Coordinates",
                                            string3,
                                            3438838,
                                            null,
                                            null,
                                            "Sent by EFN",
                                            null));
            WebhookConfig webhookConfig = new WebhookConfig(string);
            boolean confirm2 = this.confirm.get();
            WEBHOOK_EXECUTOR.execute(
                    () -> {
                        boolean send;
                        try {
                            send =
                                    CoordSnapperModule.WebhookHolder.SERVICE.send(
                                            webhookConfig, webhookPayloadBuilder);
                        } catch (RuntimeException | LinkageError exception) {
                            send = false;
                        }
                        if (!send) {
                            ModuleChat.send(
                                    this,
                                    ModuleChat.body()
                                            .text("Webhook ")
                                            .danger("failed")
                                            .text(" (see the log for details)"));
                        } else if (confirm2) {
                            ModuleChat.send(
                                    this, ModuleChat.body().text("Sent to ").good("webhook"));
                        }
                    });
        }
    }

    private static String escapeMarkdown(String string) {
        StringBuilder sb = new StringBuilder(string.length() + 8);
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\\' || c == '*' || c == '_' || c == '~' || c == '`' || c == '|') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private BlockPos raycast(MinecraftClient client, ClientPlayerEntity player, ClientWorld world) {
        FreecamModule module = ModuleRegistry.get(FreecamModule.class);
        Vec3d vec;
        Vec3d cameraPos;
        if (module != null && module.isEnabled()) {
            Camera camera = client.gameRenderer.getCamera();
            cameraPos = camera.getCameraPos();
            vec = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        } else {
            cameraPos = player.getCameraPosVec(1.0F);
            vec = player.getRotationVec(1.0F);
        }
        Vec3d vec2 = cameraPos.add(vec.multiply(this.range.get()));
        FluidHandling fluidHandling =
                this.stopAtFluids.get() ? FluidHandling.ANY : FluidHandling.NONE;
        RaycastContext raycastContext =
                new RaycastContext(cameraPos, vec2, ShapeType.OUTLINE, fluidHandling, player);
        BlockPos pos = BlockPos.ofFloored(cameraPos);
        if (startsInside(raycastContext, world, pos, cameraPos)) {
            Optional optional = new Box(pos).raycast(vec2, cameraPos);
            if (optional.isEmpty()) {
                return null;
            }
            Vec3d vec3 = ((Vec3d) optional.get()).add(vec.multiply(0.001));
            raycastContext =
                    new RaycastContext(vec3, vec2, ShapeType.OUTLINE, fluidHandling, player);
        }
        BlockHitResult blockHitResult;
        return (blockHitResult = world.raycast(raycastContext)).getType() == Type.BLOCK
                ? blockHitResult.getBlockPos()
                : null;
    }

    private static boolean startsInside(
            RaycastContext raycastContext, ClientWorld world, BlockPos pos, Vec3d vec) {
        return contains(
                        raycastContext.getBlockShape(world.getBlockState(pos), world, pos),
                        pos,
                        vec)
                || contains(
                        raycastContext.getFluidShape(world.getFluidState(pos), world, pos),
                        pos,
                        vec);
    }

    private static boolean contains(VoxelShape voxelShape, BlockPos pos, Vec3d vec) {
        for (Box boundingBox : voxelShape.getBoundingBoxes()) {
            if (boundingBox.offset(pos).contains(vec)) {
                return true;
            }
        }
        return false;
    }

    private String formatCoords(BlockPos pos, RegistryKey<World> key2) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        boolean includeDimension2 = this.includeDimension.get();
        String format2 = this.format.get();
        return switch (format2) {
            case "/tp Command" ->
                    includeDimension2
                            ? String.format(
                                    Locale.ROOT,
                                    "/execute in %s run tp @s %d %d %d",
                                    key2.getValue(),
                                    i,
                                    j,
                                    k)
                            : String.format(Locale.ROOT, "/tp %d %d %d", i, j, k);
            case "X, Y, Z" ->
                    String.format(Locale.ROOT, "%d, %d, %d", i, j, k)
                            + dimensionSuffix(key2, includeDimension2);
            default ->
                    String.format(Locale.ROOT, "%d %d %d", i, j, k)
                            + dimensionSuffix(key2, includeDimension2);
        };
    }

    private static String dimensionSuffix(RegistryKey<World> key2, boolean bl) {
        return bl ? " (" + dimensionName(key2) + ")" : "";
    }

    private static String dimensionName(RegistryKey<World> key2) {
        if (World.OVERWORLD.equals(key2)) {
            return "Overworld";
        } else if (World.NETHER.equals(key2)) {
            return "Nether";
        } else {
            return World.END.equals(key2) ? "End" : key2.getValue().toString();
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class WebhookHolder {

        static final WebhookService SERVICE = new WebhookService();

        private WebhookHolder() {}
    }
}
