package net.efn.client.module.impl.render;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.WorldLabel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.NameFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class NametagsModule extends ClientModule {

    private static final int PANEL_RGB = 1447965;

    private static final int BORDER = 402653183;

    private static final int SHADOW_NEAR = 771751936;

    private static final int SHADOW_FAR = 385875968;

    private static final int DIVIDER = 536870911;

    private static final int EMPTY_SLOT = 452984831;

    private static final int TEXT_NAME = -1447442;

    private static final int TEXT_LABEL = -6052949;

    private static final int TEXT_VALUE = -657929;

    private static final int STATUS_GOOD = -13577896;

    private static final int STATUS_WARN = -10742;

    private static final int STATUS_BAD = -47814;

    private static final int ITEM_COUNT_COLOR = -10742;

    private static final float TEXT_GUARD_BELOW = 0.55F;

    private static final EquipmentSlot[] ARMOR_SLOTS =
            new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            };

    private static final int PADDING = 4;

    private static final int ROW_GAP = 3;

    private static final int ICON_SIZE = 10;

    private static final int ICON_GAP = 2;

    private static final int TEXT_GAP = 4;

    private static final int SECTION_GAP = 3;

    private static final float ICON_SCALE = 0.625F;

    private static final int TAG_CAP = 64;

    private static final double FULL_SIZE_DISTANCE = 8.0;

    private static final int SEEN_LIMIT = 9;

    private static final String SEEN_LABEL = "Seen";

    private static final int SEEN_PLAYERS = 128;

    private static final Comparator<NametagsModule.Nearby> NEAREST =
            Comparator.comparingDouble(NametagsModule.Nearby::distanceSq);

    private final BooleanSetting sPing = this.addSetting("ping", new BooleanSetting("Ping", true));

    private final BooleanSetting sHeldItems =
            this.addSetting("held-items", new BooleanSetting("Held Items", true));

    private final BooleanSetting sSeenItems =
            this.addSetting("seen-items", new BooleanSetting("Seen Items", true));

    private final BooleanSetting sArmor =
            this.addSetting("armor", new BooleanSetting("Armor", true));

    private final BooleanSetting sDurability =
            this.addSetting("durability", new BooleanSetting("Durability", true));

    private final BooleanSetting sHealth =
            this.addSetting("health", new BooleanSetting("Health", true));

    private final BooleanSetting sHideVanilla =
            this.addSetting("hide-vanilla", new BooleanSetting("Hide Vanilla", true));

    private final BooleanSetting sItems =
            this.addSetting("items", new BooleanSetting("Dropped Items", true));

    private final NumberSetting sRange =
            this.addSetting("range", new NumberSetting("Range", 128.0F, 8.0F, 512.0F).step(1.0F));

    private final NumberSetting sMaxTags =
            this.addSetting(
                    "max-tags", new NumberSetting("Max Tags", 64.0F, 1.0F, 64.0F).step(1.0F));

    private final NumberSetting sScale =
            this.addSetting("scale", new NumberSetting("Scale", 1.0F, 0.5F, 2.0F).step(0.1F));

    private final NumberSetting sMinScale =
            this.addSetting(
                    "min-scale", new NumberSetting("Min Scale", 0.55F, 0.3F, 1.0F).step(0.05F));

    private final NumberSetting sBackgroundAlpha =
            this.addSetting(
                    "background-alpha",
                    new NumberSetting("Background Alpha", 0.45F, 0.0F, 1.0F).step(0.05F));

    private final Map<UUID, LinkedHashMap<Item, ItemStack>> seen =
            new LinkedHashMap<>(16, 0.75F, true);

    private ClientWorld seenLevel;

    public NametagsModule() {
        super(
                "nametags",
                "Nametags",
                "Player nametags with held items, armour, health, ping and distance.",
                ModuleCategory.RENDER);
        this.sDurability.visibleWhen(() -> this.sHeldItems.get() || this.sArmor.get());
        this.sSeenItems.visibleWhen(this.sHeldItems::get);
    }

    public static boolean hideVanillaNametags() {
        NametagsModule module = ModuleRegistry.get(NametagsModule.class);
        return module != null && module.isEnabled() && module.sHideVanilla.get();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != this.seenLevel) {
            this.seen.clear();
            this.seenLevel = client.world;
        }
        if (client.world != null && this.sHeldItems.get() && this.sSeenItems.get()) {
            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                if (player != client.player) {
                    this.remember(player.getUuid(), player.getMainHandStack());
                    this.remember(player.getUuid(), player.getOffHandStack());
                }
            }
        }
    }

    private void remember(UUID uuid, ItemStack stack) {
        if (!stack.isEmpty()) {
            LinkedHashMap map = this.seen.computeIfAbsent(uuid, uUIDx -> new LinkedHashMap<>());
            ItemStack stack2 = (ItemStack) map.remove(stack.getItem());
            map.put(
                    stack.getItem(),
                    stack2 != null && ItemStack.areItemsAndComponentsEqual(stack2, stack)
                            ? stack2
                            : stack.copy());
            while (map.size() > 9) {
                map.remove(map.keySet().iterator().next());
            }
            while (this.seen.size() > 128) {
                this.seen.remove(this.seen.keySet().iterator().next());
            }
        }
    }

    private List<ItemStack> seenItems(AbstractClientPlayerEntity player) {
        LinkedHashMap map = this.seen.get(player.getUuid());
        if (map != null && !map.isEmpty()) {
            Item item = player.getMainHandStack().getItem();
            Item item2 = player.getOffHandStack().getItem();
            ArrayList list = new ArrayList(map.size());
            for (ItemStack stack : (Iterable<ItemStack>) map.values()) {
                if (stack.getItem() != item && stack.getItem() != item2) {
                    list.add(0, stack);
                }
            }
            return list;
        } else {
            return List.of();
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null && !client.options.hudHidden) {
            Camera camera = client.gameRenderer.getCamera();
            if (camera != null && camera.isReady()) {
                float partialTick = event.partialTick;
                Vec3d cameraPosVec = client.player.getCameraPosVec(partialTick);
                double d = Math.max(1.0F, this.sRange.get());
                double e = d * d;
                int i = Math.max(1, Math.min(64, this.sMaxTags.getInt()));
                ArrayList list = new ArrayList();
                for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                    double squaredDistance;
                    if (player != client.player
                            && !player.isRemoved()
                            && !player.isDead()
                            && (squaredDistance =
                                            player.getLerpedPos(partialTick)
                                                    .squaredDistanceTo(cameraPosVec))
                                    <= e) {
                        list.add(new NametagsModule.Nearby(player, squaredDistance));
                    }
                }
                List list2 = nearest(list, i);
                int j = i - list2.size();
                List<NametagsModule.Nearby> list3 = new ArrayList<>();
                if (this.sItems.get() && j > 0) {
                    for (Entity entity2 : client.world.getEntities()) {
                        double squaredDistance2;
                        ItemEntity itemEntity;
                        if (entity2 instanceof ItemEntity
                                && !(itemEntity = (ItemEntity) entity2).isRemoved()
                                && !itemEntity.getStack().isEmpty()
                                && (squaredDistance2 =
                                                itemEntity
                                                        .getLerpedPos(partialTick)
                                                        .squaredDistanceTo(cameraPosVec))
                                        <= e) {
                            list3.add(new NametagsModule.Nearby(itemEntity, squaredDistance2));
                        }
                    }
                    list3 = nearest((List<NametagsModule.Nearby>) list3, j);
                }
                for (int k = list3.size() - 1; k >= 0; k--) {
                    this.renderItemTag(
                            event.graphics,
                            client,
                            camera,
                            (ItemEntity) ((NametagsModule.Nearby) list3.get(k)).entity(),
                            partialTick);
                }
                for (int m = list2.size() - 1; m >= 0; m--) {
                    this.renderTag(
                            event.graphics,
                            client,
                            camera,
                            (AbstractClientPlayerEntity)
                                    ((NametagsModule.Nearby) list2.get(m)).entity(),
                            partialTick);
                }
            }
        }
    }

    private static List<NametagsModule.Nearby> nearest(List<NametagsModule.Nearby> list, int i) {
        list.sort(NEAREST);
        return list.size() <= i ? list : list.subList(0, i);
    }

    private void renderTag(
            DrawContext context,
            MinecraftClient client,
            Camera camera,
            AbstractClientPlayerEntity player,
            float f) {
        Vec3d vec = player.getLerpedPos(f).add(0.0, player.getHeight() + 0.15, 0.0);
        float[] floats =
                WorldLabel.project(
                        client,
                        vec,
                        context.getScaledWindowWidth(),
                        context.getScaledWindowHeight());
        if (floats != null) {
            TextRenderer textRenderer = client.textRenderer;
            String string = NameFilter.apply(player.getGameProfile().name());
            String string2 = null;
            int i = -6052949;
            PlayerListEntry playerListEntry;
            if (this.sPing.get()
                    && client.getNetworkHandler() != null
                    && (playerListEntry =
                                    client.getNetworkHandler().getPlayerListEntry(player.getUuid()))
                            != null) {
                int latency = playerListEntry.getLatency();
                string2 = latency + "ms";
                i = latency < 60 ? -13577896 : (latency < 130 ? -10742 : -47814);
            }
            List list = this.sHeldItems.get() ? heldItems(player) : List.of();
            List list2 = this.sArmor.get() ? wornArmor(player) : List.of();
            List list3 =
                    this.sHeldItems.get() && this.sSeenItems.get()
                            ? this.seenItems(player)
                            : List.of();
            String string3 = null;
            int j = -13577896;
            float g = player.getHealth() + player.getAbsorptionAmount();
            if (this.sHealth.get() && Float.isFinite(g)) {
                float h = Math.min(1.0F, g / Math.max(1.0F, player.getMaxHealth()));
                string3 = String.valueOf((int) Math.ceil(g));
                j = h > 0.6F ? -13577896 : (h > 0.3F ? -10742 : -47814);
            }
            double distance = vec.distanceTo(camera.getCameraPos());
            String string4 = Math.round((float) distance) + "m";
            int iconsWidth = iconsWidth(list.size());
            int iconsWidth2 = iconsWidth(list2.size());
            int k = string3 != null ? textRenderer.getWidth(string3) : 0;
            int width = textRenderer.getWidth(string4);
            int m =
                    textRenderer.getWidth(string)
                            + (string2 != null ? 5 + textRenderer.getWidth(string2) : 0);
            int extend = extend(0, iconsWidth, 0);
            extend = extend(extend, iconsWidth2, 7);
            extend = extend(extend, k, 4);
            extend = extend(extend, width, 4);
            int n =
                    list3.isEmpty()
                            ? 0
                            : textRenderer.getWidth("Seen") + 4 + iconsWidth(list3.size());
            int o = Math.max(Math.max(m, extend), n);
            int p = o + 8;
            int q = 30 + (list3.isEmpty() ? 0 : 13);
            float f2 = Math.max(0.5F, Math.min(2.0F, this.sScale.get()));
            double d = Math.max(0.3F, Math.min(1.0F, this.sMinScale.get()));
            float g2 = (float) Math.min(1.0, Math.max(d, 8.0 / Math.max(1.0, distance)));
            float h2 = f2 * g2;
            float f3 = Math.max(0.0F, Math.min(1.0F, this.sBackgroundAlpha.get()));
            boolean bl = f3 < 0.55F;
            float g3 = Math.min(1.0F, f3 * 2.0F);
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(floats[0], floats[1]);
            matrices.scale(h2, h2);
            int r = -p / 2;
            int t = -q - 1;
            drawPanel(context, r, t, r + p, t + q, f3, g3);
            int u = r + 4;
            int v = t + 4;
            context.drawText(textRenderer, string, u, v, -1447442, bl);
            if (string2 != null) {
                context.drawText(
                        textRenderer, string2, u + textRenderer.getWidth(string) + 5, v, i, bl);
            }
            Objects.requireNonNull(textRenderer);
            int w = t + 4 + 9 + 3;
            Objects.requireNonNull(textRenderer);
            int i2 = w + 0 + 1;
            int j2 = u;
            boolean bl2 = false;
            if (!list.isEmpty()) {
                j2 = this.drawIcons(context, textRenderer, player, list, 0, u, w, true, g3);
                bl2 = true;
            }
            if (!list2.isEmpty()) {
                if (bl2) {
                    int k2 = j2 + 3;
                    context.fill(k2, w + 1, k2 + 1, w + 10 - 1, scaleAlpha(536870911, g3));
                    j2 += 7;
                }
                j2 =
                        this.drawIcons(
                                context,
                                textRenderer,
                                player,
                                list2,
                                list.size(),
                                j2,
                                w,
                                false,
                                g3);
                bl2 = true;
            }
            if (string3 != null) {
                if (bl2) {
                    j2 += 4;
                }
                context.drawText(textRenderer, string3, j2, i2, j, bl);
                j2 += k;
                bl2 = true;
            }
            if (bl2) {
                j2 += 4;
            }
            context.drawText(textRenderer, string4, j2, i2, -6052949, bl);
            if (!list3.isEmpty()) {
                int m2 = w + 10 + 3;
                Objects.requireNonNull(textRenderer);
                context.drawText(textRenderer, "Seen", u, m2 + 0 + 1, -6052949, bl);
                this.drawIcons(
                        context,
                        textRenderer,
                        player,
                        list3,
                        list.size() + list2.size(),
                        u + textRenderer.getWidth("Seen") + 4,
                        m2,
                        false,
                        g3);
            }
            matrices.popMatrix();
        }
    }

    private static List<ItemStack> heldItems(AbstractClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();
        return mainHandStack.isEmpty() && offHandStack.isEmpty()
                ? List.of()
                : List.of(mainHandStack, offHandStack);
    }

    private static List<ItemStack> wornArmor(AbstractClientPlayerEntity player) {
        ArrayList list = new ArrayList(ARMOR_SLOTS.length);
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            ItemStack equippedStack = player.getEquippedStack(equipmentSlot);
            if (!equippedStack.isEmpty()) {
                list.add(equippedStack);
            }
        }
        return list;
    }

    private static int iconsWidth(int i) {
        return i == 0 ? 0 : i * 12 - 2;
    }

    private static int extend(int i, int j, int k) {
        if (j <= 0) {
            return i;
        } else {
            return i == 0 ? j : i + k + j;
        }
    }

    private int drawIcons(
            DrawContext context,
            TextRenderer textRenderer,
            AbstractClientPlayerEntity player,
            List<ItemStack> list,
            int i,
            int j,
            int k,
            boolean bl,
            float f) {
        for (int m = 0; m < list.size(); m++) {
            if (m > 0) {
                j += 2;
            }
            this.drawIcon(
                    context,
                    textRenderer,
                    player,
                    (ItemStack) list.get(m),
                    player.getId() * 8 + i + m,
                    j,
                    k,
                    bl,
                    f);
            j += 10;
        }
        return j;
    }

    private void drawIcon(
            DrawContext context,
            TextRenderer textRenderer,
            AbstractClientPlayerEntity player,
            ItemStack stack,
            int i,
            int j,
            int k,
            boolean bl,
            float f) {
        if (stack.isEmpty()) {
            context.fill(j + 2, k + 2, j + 10 - 2, k + 10 - 2, scaleAlpha(452984831, f));
        } else {
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(j, k);
            matrices.scale(0.625F, 0.625F);
            context.drawItem(player, stack, 0, 0, i);
            if (this.sDurability.get() && stack.isItemBarVisible()) {
                context.fill(2, 13, 15, 15, -16777216);
                context.fill(
                        2,
                        13,
                        2 + stack.getItemBarStep(),
                        14,
                        0xFF000000 | stack.getItemBarColor());
            }
            if (bl && stack.getCount() > 1) {
                String string = String.valueOf(stack.getCount());
                context.drawText(
                        textRenderer, string, 17 - textRenderer.getWidth(string), 9, -657929, true);
            }
            matrices.popMatrix();
        }
    }

    private void renderItemTag(
            DrawContext context,
            MinecraftClient client,
            Camera camera,
            ItemEntity itemEntity,
            float f) {
        Vec3d vec = itemEntity.getLerpedPos(f).add(0.0, 0.35, 0.0);
        float[] floats =
                WorldLabel.project(
                        client,
                        vec,
                        context.getScaledWindowWidth(),
                        context.getScaledWindowHeight());
        if (floats != null) {
            ItemStack stack = itemEntity.getStack();
            String string = stack.getName().getString();
            int count = stack.getCount();
            String string2 = count > 1 ? "x" + count : null;
            TextRenderer textRenderer = client.textRenderer;
            int width = textRenderer.getWidth(string);
            int i = string2 != null ? textRenderer.getWidth(string2) : 0;
            int j = width + (string2 != null ? 3 + i : 0);
            double distance = vec.distanceTo(camera.getCameraPos());
            float g = (float) Math.min(1.0, Math.max(0.25, 6.0 / Math.max(1.0, distance)));
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(floats[0], floats[1]);
            matrices.scale(g, g);
            int k = -j / 2;
            byte b = -4;
            context.drawText(textRenderer, string, k, b, -1447442, true);
            if (string2 != null) {
                context.drawText(textRenderer, string2, k + width + 3, b, -10742, true);
            }
            matrices.popMatrix();
        }
    }

    private static void drawPanel(
            DrawContext context, int i, int j, int k, int m, float f, float g) {
        drawR4Rect(context, i - 1, j + 1, k + 1, m + 2, scaleAlpha(385875968, g));
        drawR4Rect(context, i, j + 1, k, m + 1, scaleAlpha(771751936, g));
        drawR4Rect(context, i, j, k, m, Math.round(f * 255.0F) << 24 | 1447965);
        drawR4Border(context, i, j, k, m, scaleAlpha(402653183, g));
    }

    private static int scaleAlpha(int i, float f) {
        int j = Math.round((i >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, f)));
        return j << 24 | i & 16777215;
    }

    private static void drawR4Rect(DrawContext context, int i, int j, int k, int m, int n) {
        if (n >>> 24 != 0) {
            context.fill(i + 4, j, k - 4, j + 1, n);
            context.fill(i + 2, j + 1, k - 2, j + 2, n);
            context.fill(i + 1, j + 2, k - 1, j + 4, n);
            context.fill(i, j + 4, k, m - 4, n);
            context.fill(i + 1, m - 4, k - 1, m - 2, n);
            context.fill(i + 2, m - 2, k - 2, m - 1, n);
            context.fill(i + 4, m - 1, k - 4, m, n);
        }
    }

    private static void drawR4Border(DrawContext context, int i, int j, int k, int m, int n) {
        if (n >>> 24 != 0) {
            context.fill(i + 4, j, k - 4, j + 1, n);
            context.fill(i + 4, m - 1, k - 4, m, n);
            context.fill(i, j + 4, i + 1, m - 4, n);
            context.fill(k - 1, j + 4, k, m - 4, n);
            context.fill(i + 2, j + 1, i + 4, j + 2, n);
            context.fill(i + 1, j + 2, i + 2, j + 4, n);
            context.fill(k - 4, j + 1, k - 2, j + 2, n);
            context.fill(k - 2, j + 2, k - 1, j + 4, n);
            context.fill(i + 1, m - 4, i + 2, m - 2, n);
            context.fill(i + 2, m - 2, i + 4, m - 1, n);
            context.fill(k - 2, m - 4, k - 1, m - 2, n);
            context.fill(k - 4, m - 2, k - 2, m - 1, n);
        }
    }

    @Environment(EnvType.CLIENT)
    private record Nearby(Entity entity, double distanceSq) {}
}
