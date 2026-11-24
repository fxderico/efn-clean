package net.efn.client.module.impl.render;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class WeaponInspectModule extends ClientModule {

    public static volatile WeaponInspectModule active;

    private static final String SWORDS = "Swords";

    private static final String WEAPONS = "Weapons";

    private static final String ANY_ITEM = "Any Item";

    private static final Identifier KNIFE_TEXTURE =
            Identifier.of("efn", "textures/misc/knife_base.png");

    private static final float EASE_OUT_SECONDS = 0.15F;

    private static final int IDLE = 0;

    private static final int INSPECT = 1;

    private static final int DEPLOY = 2;

    private static final float[] REST = new float[16];

    private static final float SPARKLE_SIZE = 0.07F;

    private final KeySetting key = this.addSetting("key", new KeySetting("Inspect Key", 70));

    private final ModeSetting skin =
            this.addSetting(
                    "skin",
                    new ModeSetting(
                            "Knife Skin", "Butterfly", "None", "Butterfly", "Karambit", "Katana"));

    private final ModeSetting style =
            this.addSetting("style", new ModeSetting("Style", "Classic", "Classic", "Twirl"));

    private final ModeSetting items =
            this.addSetting(
                    "items", new ModeSetting("Items", "Swords", "Swords", "Weapons", "Any Item"));

    private final NumberSetting speed =
            this.addSetting("speed", new NumberSetting("Speed", 1.0F, 0.5F, 2.0F).step(0.05F));

    private final NumberSetting knifeSize =
            this.addSetting(
                    "knife-size", new NumberSetting("Knife Size", 1.0F, 0.5F, 2.0F).step(0.05F));

    private final BooleanSetting showArm =
            this.addSetting("show-arm", new BooleanSetting("Show Arm", true));

    private final BooleanSetting deploy =
            this.addSetting("deploy", new BooleanSetting("Deploy Animation", true));

    private final BooleanSetting slashTrail =
            this.addSetting("slash-trail", new BooleanSetting("Slash Trail", true));

    private final NumberSetting trailLength =
            this.addSetting(
                    "trail-length",
                    new NumberSetting("Trail Length", 0.14F, 0.05F, 0.35F).step(0.01F));

    private final BooleanSetting sparkles =
            this.addSetting("sparkles", new BooleanSetting("Blade Sparkle", true));

    private final BooleanSetting sneakSkips =
            this.addSetting("sneak-skips", new BooleanSetting("Sneak Skips Inspect", true));

    private int mode = 0;

    private InspectAnimation.Clip clip;

    private KnifeSkin clipKnife;

    private long startedAt;

    private float seconds;

    private boolean easingOut;

    private long easeStartedAt;

    private float easeFrom;

    private int slot;

    private Item item;

    private int heldSlot = -1;

    private Item heldItem;

    private final float[] state = new float[16];

    private final float[] trailSample = new float[12];

    private final SlashTrail[] trails = new SlashTrail[2];

    private final Matrix4f base = new Matrix4f();

    private final Matrix4f baseInverse = new Matrix4f();

    private final Matrix3f baseNormal = new Matrix3f();

    private boolean baseKnown;

    public WeaponInspectModule() {
        super(
                "weapon-inspect",
                "CS Knife",
                "Knife skins for your sword and CS-style inspects with a key of your choice (F by"
                        + " default); hold the key to keep the knife spinning.",
                ModuleCategory.VISUALS);
        for (int i = 0; i < this.trails.length; i++) {
            this.trails[i] = new SlashTrail();
        }
        this.knifeSize.visibleWhen(() -> !this.skin.is("None"));
        this.deploy.visibleWhen(() -> !this.skin.is("None"));
        this.sparkles.visibleWhen(() -> !this.skin.is("None"));
        this.trailLength.visibleWhen(this.slashTrail::get);
    }

    @Override
    protected void onEnable() {
        this.mode = 0;
        this.heldItem = null;
        this.clearTrails();
        active = this;
        this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
        this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
    }

    @Override
    protected void onDisable() {
        active = null;
        this.mode = 0;
        this.clearTrails();
    }

    public static void handsBegin(MatrixStack matrices) {
        WeaponInspectModule module = active;
        if (module != null) {
            module.base.set(matrices.peek().getPositionMatrix());
            module.base.invert(module.baseInverse);
            module.baseNormal.set(matrices.peek().getNormalMatrix());
            module.baseKnown = true;
        }
    }

    public static boolean renderItem(
            MatrixStack matrices,
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext itemDisplayContext,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            boolean bl) {
        WeaponInspectModule module = active;
        return module != null
                && module.render(
                        matrices,
                        entity,
                        stack,
                        itemDisplayContext,
                        orderedRenderCommandQueue,
                        i,
                        bl);
    }

    private void onKey(KeyInputEvent event) {
        if (event.action != 0 && this.pressed(event.key, event.action == 2)) {
            event.setCancelled(true);
        }
    }

    private void onMouse(MouseInputEvent event) {
        if (event.action == 1 && this.pressed(1000 + event.button, false)) {
            event.setCancelled(true);
        }
    }

    private boolean pressed(int i, boolean bl) {
        int key2 = this.key.value();
        if (ModuleKeybinds.isBindable(key2) && i == key2) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (client.currentScreen != null || player == null || player.isSpectator()) {
                return false;
            } else if (this.sneakSkips.get() && player.isSneaking()) {
                return false;
            } else {
                ItemStack mainHandStack = player.getMainHandStack();
                if (!this.inspectable(mainHandStack)) {
                    return false;
                } else {
                    if (!bl && !player.isUsingItem()) {
                        this.start(1, player.getInventory().getSelectedSlot(), mainHandStack);
                    }
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private boolean held() {
        int key2 = this.key.value();
        MinecraftClient client = MinecraftClient.getInstance();
        if (ModuleKeybinds.isBindable(key2) && client.currentScreen == null) {
            return key2 >= 1000
                    ? GLFW.glfwGetMouseButton(client.getWindow().getHandle(), key2 - 1000) == 1
                    : EfnKeyPolling.isKeyDown(client.getWindow(), key2);
        } else {
            return false;
        }
    }

    private boolean inspectable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            return !this.items.is("Any Item") && !isSword(stack)
                    ? this.items.is("Weapons")
                            && (stack.isIn(ItemTags.AXES)
                                    || stack.isIn(ItemTags.SPEARS)
                                    || stack.isOf(Items.MACE)
                                    || stack.isOf(Items.TRIDENT))
                    : true;
        }
    }

    private static boolean isSword(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.isIn(ItemTags.SWORDS)
                        || Registries.ITEM.getId(stack.getItem()).getPath().endsWith("_sword"));
    }

    private static boolean handheld(ItemStack stack) {
        return isSword(stack)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.SHOVELS)
                || stack.isIn(ItemTags.HOES);
    }

    private void start(int mode2, int slot2, ItemStack stack) {
        KnifeSkin knifeSkin = isSword(stack) ? KnifeSkin.of(this.skin.get()) : null;
        this.clip =
                mode2 == 2
                        ? knifeSkin.deploy
                        : (knifeSkin != null
                                ? knifeSkin.inspect
                                : InspectAnimation.clip(this.style.get()));
        this.clipKnife = knifeSkin;
        this.mode = mode2;
        this.seconds = this.clip.seconds;
        this.easingOut = false;
        this.startedAt = System.nanoTime();
        this.slot = slot2;
        this.item = stack.getItem();
    }

    private void easeOut() {
        long l = System.nanoTime();
        this.easeFrom = Math.min(this.progress(l), 1.0F);
        this.easeStartedAt = l;
        this.easingOut = true;
    }

    private void onTick(TickEvent event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.mode = 0;
            this.heldItem = null;
        } else {
            ItemStack mainHandStack = player.getMainHandStack();
            int selectedSlot = player.getInventory().getSelectedSlot();
            if (selectedSlot != this.heldSlot || !mainHandStack.isOf(this.heldItem)) {
                this.heldSlot = selectedSlot;
                this.heldItem = mainHandStack.getItem();
                if (this.deploy.get()
                        && isSword(mainHandStack)
                        && KnifeSkin.of(this.skin.get()) != null) {
                    this.start(2, selectedSlot, mainHandStack);
                } else if (this.mode != 0 && !this.easingOut) {
                    this.easeOut();
                }
            } else if (this.mode != 0 && !this.easingOut) {
                if (player.handSwinging) {
                    this.mode = 0;
                } else if (player.isUsingItem()) {
                    this.easeOut();
                } else if (this.loopedProgress(System.nanoTime()) >= 1.0F) {
                    this.mode = 0;
                }
            }
        }
    }

    private float progress(long l) {
        return (float) (l - this.startedAt) / 1.0E9F / (this.seconds / this.speed.get());
    }

    private float loopedProgress(long l) {
        float progress = this.progress(l);
        if (this.mode == 1
                && this.clip.loops()
                && !(progress < this.clip.loopExit)
                && this.held()) {
            float f = this.seconds / this.speed.get();
            long l2 = (long) ((this.clip.loopExit - this.clip.loopEnter) * f * 1.0E9F);
            if (l2 <= 0L) {
                return progress;
            } else {
                long l3 = (long) ((progress - this.clip.loopExit) * f * 1.0E9F);
                this.startedAt += (l3 / l2 + 1L) * l2;
                return this.progress(l);
            }
        } else {
            return progress;
        }
    }

    private float animate(ItemStack stack, KnifeSkin knifeSkin, long l) {
        if (this.mode == 0 || !stack.isOf(this.item)) {
            return 0.0F;
        } else if (knifeSkin != this.clipKnife) {
            this.mode = 0;
            return 0.0F;
        } else {
            float f = 1.0F;
            float easeFrom2;
            if (this.easingOut) {
                float g = (float) (l - this.easeStartedAt) / 1.0E9F / 0.15F;
                if (g >= 1.0F) {
                    this.mode = 0;
                    return 0.0F;
                }
                easeFrom2 = this.easeFrom;
                f = 1.0F - g * g * (3.0F - 2.0F * g);
            } else {
                easeFrom2 = this.loopedProgress(l);
                if (easeFrom2 >= 1.0F) {
                    this.mode = 0;
                    return 0.0F;
                }
            }
            this.clip.sample(easeFrom2, this.state);
            return f;
        }
    }

    private boolean render(
            MatrixStack matrices,
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext itemDisplayContext,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            boolean bl) {
        int j = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
        boolean sword = isSword(stack);
        KnifeSkin knifeSkin = sword ? KnifeSkin.of(this.skin.get()) : null;
        long l = System.nanoTime();
        float f = bl ? this.animate(stack, knifeSkin, l) : 0.0F;
        float[] floats = f > 0.0F ? this.state : REST;
        boolean bl2 = bl && sword && this.slashTrail.get() && this.baseKnown;
        if (bl && !bl2) {
            this.clearTrails();
        }
        if (knifeSkin == null) {
            FirstPersonArm.Arm arm;
            if ((this.showArm.get() && handheld(stack) || HandModelModule.covers(stack, bl))
                    && (arm = FirstPersonArm.arm()) != null) {
                FirstPersonArm.hold(
                        matrices,
                        arm,
                        stack,
                        itemDisplayContext,
                        entity,
                        orderedRenderCommandQueue,
                        i,
                        floats[0] * f,
                        floats[1] * f,
                        floats[2] * f);
            }
            if (f > 0.0F) {
                InspectAnimation.place(matrices, floats, f, j, 0.07F, 0.2F, 0.07F);
            }
            if (bl2) {
                this.swordTrail(matrices.peek().getPositionMatrix(), j);
                this.record(l, 1);
                this.submitGlow(
                        orderedRenderCommandQueue, l, KnifeMaterial.of(stack).glow, 0.0F, null);
            }
            return false;
        } else {
            KnifeMaterial knifeMaterial = KnifeMaterial.of(stack);
            FirstPersonArm.Arm arm2 =
                    !this.showArm.get() && !HandModelModule.wants(bl) ? null : FirstPersonArm.arm();
            boolean bl3 = arm2 != null && arm2.slim();
            matrices.push();
            if (j < 0) {
                matrices.scale(-1.0F, 1.0F, 1.0F);
            }
            knifeSkin.draw(
                    matrices,
                    floats,
                    f,
                    bl3,
                    this.knifeSize.get(),
                    sink(
                            orderedRenderCommandQueue,
                            i,
                            j,
                            arm2,
                            bl && offHandFree(),
                            knifeMaterial,
                            stack.hasGlint()),
                    bl2 ? this.trailSample : null);
            matrices.pop();
            if (bl2) {
                this.record(l, knifeSkin.streakCount());
                float g = this.sparkles.get() && f > 0.0F ? knifeSkin.sparkle(floats, f) : 0.0F;
                this.submitGlow(orderedRenderCommandQueue, l, knifeMaterial.glow, g, knifeSkin);
            }
            return true;
        }
    }

    private static KnifeSkin.Sink sink(
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            int j,
            FirstPersonArm.Arm arm,
            boolean bl,
            KnifeMaterial knifeMaterial,
            boolean bl3) {
        final RenderLayer renderLayer = RenderLayers.entityCutoutNoCull(KNIFE_TEXTURE);
        final int[] palette = knifeMaterial.palette;
        return new KnifeSkin.Sink() {

            @Override
            public void part(MatrixStack matrices, KnifeMesh knifeMesh, KnifeMesh.Shine shine) {
                orderedRenderCommandQueue.submitCustom(
                        matrices,
                        renderLayer,
                        (entry, vertexConsumer) ->
                                knifeMesh.emit(entry, vertexConsumer, palette, i, shine));
                if (bl3) {
                    orderedRenderCommandQueue
                            .getBatchingQueue(1)
                            .submitCustom(
                                    matrices,
                                    RenderLayers.entityGlint(),
                                    (entry, vertexConsumer) ->
                                            knifeMesh.emit(
                                                    entry,
                                                    vertexConsumer,
                                                    palette,
                                                    i,
                                                    KnifeMesh.Shine.NONE));
                }
            }

            @Override
            public void glow(MatrixStack matrices2, KnifeMesh knifeMesh2, KnifeMesh.Shine shine2) {
                orderedRenderCommandQueue
                        .getBatchingQueue(1)
                        .submitCustom(
                                matrices2,
                                RenderLayers.eyes(WeaponInspectModule.KNIFE_TEXTURE),
                                (entry, vertexConsumer) ->
                                        knifeMesh2.emitGlow(
                                                entry, vertexConsumer, knifeMaterial.glow, shine2));
            }

            @Override
            public void arm(MatrixStack matrices3, boolean bl2) {
                if (arm != null && (!bl2 || bl)) {
                    // decompiler shadowed the captured outer flag
                    arm.draw(matrices3, j < 0 != bl2, orderedRenderCommandQueue, i);
                }
            }
        };
    }

    private static boolean offHandFree() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && player.getOffHandStack().isEmpty();
    }

    private void swordTrail(Matrix4f matrix, int i) {
        Matrix4f matrix2 =
                new Matrix4f(matrix)
                        .translate(i * 1.13F / 16.0F, 0.2F, 0.070625F)
                        .rotate(new Quaternionf().rotationXYZ(0.0F, -1.5707964F, 0.43633232F))
                        .scale(0.68F)
                        .translate(-0.5F, -0.5F, -0.5F);
        Vector3f vec = matrix2.transformPosition(0.3125F, 0.3125F, 0.5F, new Vector3f());
        this.trailSample[0] = vec.x;
        this.trailSample[1] = vec.y;
        this.trailSample[2] = vec.z;
        matrix2.transformPosition(0.96875F, 0.96875F, 0.5F, vec);
        this.trailSample[3] = vec.x;
        this.trailSample[4] = vec.y;
        this.trailSample[5] = vec.z;
    }

    private void record(long l, int i) {
        Vector3f vec = new Vector3f();
        for (int j = 0; j < this.trails.length; j++) {
            if (j >= i) {
                this.trails[j].clear();
            } else {
                for (int k = j * 6; k < j * 6 + 6; k += 3) {
                    this.baseInverse.transformPosition(
                            this.trailSample[k],
                            this.trailSample[k + 1],
                            this.trailSample[k + 2],
                            vec);
                    this.trailSample[k] = vec.x;
                    this.trailSample[k + 1] = vec.y;
                    this.trailSample[k + 2] = vec.z;
                }
                this.trails[j].add(l, this.trailSample, j * 6);
            }
        }
    }

    private void clearTrails() {
        for (SlashTrail trail : this.trails) {
            trail.clear();
        }
    }

    private void submitGlow(
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            long l,
            int i,
            float f,
            KnifeSkin knifeSkin) {
        long l2 = (long) (this.trailLength.get() * 1.0E9F);
        int j = knifeSkin != null ? knifeSkin.streakCount() : 1;
        boolean bl = f > 0.0F;
        for (int k = 0; k < j && !bl; k++) {
            bl = !this.trails[k].isEmpty(l, l2);
        }
        if (bl) {
            float g = this.trailSample[3];
            float h = this.trailSample[4];
            float f2 = this.trailSample[5];
            float g2 = (float) (l % 6283185307L) / 1.0E9F * 1.5F;
            MatrixStack matrices = new MatrixStack();
            matrices.peek().getPositionMatrix().set(this.base);
            matrices.peek().getNormalMatrix().set(this.baseNormal);
            orderedRenderCommandQueue
                    .getBatchingQueue(1)
                    .submitCustom(
                            matrices,
                            RenderLayers.eyes(KNIFE_TEXTURE),
                            (entry, vertexConsumer) -> {
                                for (int m = 0; m < j; m++) {
                                    this.trails[m].emit(
                                            entry,
                                            vertexConsumer,
                                            l,
                                            l2,
                                            i,
                                            knifeSkin != null ? knifeSkin.streakStrength(m) : 1.0F);
                                }
                                if (f > 0.0F) {
                                    SlashTrail.sparkle(
                                            entry, vertexConsumer, g, h, f2, 0.07F * f, g2, i, f);
                                }
                            });
        }
    }
}
