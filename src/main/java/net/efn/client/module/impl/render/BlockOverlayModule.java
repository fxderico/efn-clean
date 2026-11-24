package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorBlockOverlayMultiPlayerGameMode;
import com.example.client.mixin.AccessorGameRenderer;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.gui.ThemeTint;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class BlockOverlayModule extends ClientModule {

    private static final String BOTH = "Both";

    private static final String OUTLINE = "Outline";

    private static final String FILL = "Fill";

    private static final String THEME = "Theme";

    private static final String GRADIENT = "Gradient";

    private static final String SOLID = "Solid";

    private static final String RAINBOW = "Rainbow";

    private static final float THEME_LIGHTEN = 0.55F;

    private static final float FADE_IN = 0.12F;

    private static final float FADE_OUT = 0.2F;

    private static final double SNAP_DISTANCE = 6.0;

    private static final float FLOW_SPEED = 0.35F;

    private static final double SEGMENT = 0.125;

    private static final double FILL_INFLATE = 0.002;

    private static final double LINE_LIFT = 0.004;

    private static final double GLOW_LIFT = 0.002;

    private final ModeSetting style =
            this.addSetting("style", new ModeSetting("Style", "Both", "Both", "Outline", "Fill"));

    private final ModeSetting colorMode =
            this.addSetting(
                    "color-mode",
                    new ModeSetting(
                            "Color Mode", "Theme", "Theme", "Gradient", "Solid", "Rainbow"));

    private final ColorSetting color =
            this.addSetting("color", new ColorSetting("Color", -15408912));

    private final ColorSetting secondColor =
            this.addSetting("second-color", new ColorSetting("Second Color", -6480));

    private final BooleanSetting flow = this.addSetting("flow", new BooleanSetting("Flow", true));

    private final NumberSetting fillOpacity =
            this.addSetting(
                    "fill-opacity",
                    new NumberSetting("Fill Opacity", 20.0F, 2.0F, 60.0F).step(1.0F));

    private final NumberSetting width =
            this.addSetting("width", new NumberSetting("Width", 2.0F, 1.0F, 5.0F).step(0.5F));

    private final BooleanSetting glow = this.addSetting("glow", new BooleanSetting("Glow", true));

    private final BooleanSetting smooth =
            this.addSetting("smooth", new BooleanSetting("Smooth", true));

    private final NumberSetting smoothSpeed =
            this.addSetting(
                    "smooth-speed",
                    new NumberSetting("Smooth Speed", 16.0F, 4.0F, 40.0F).step(1.0F));

    private final BooleanSetting miningProgress =
            this.addSetting("mining-progress", new BooleanSetting("Mining Progress", true));

    private final BooleanSetting faceOnly =
            this.addSetting("face-only", new BooleanSetting("Face Only", false));

    private final BooleanSetting seeThrough =
            this.addSetting("see-through", new BooleanSetting("See Through", false));

    private ClientWorld level;

    private BlockPos pos;

    private BlockState state;

    private BlockPos partner;

    private BlockState partnerState;

    private final List<double[]> edges = new ArrayList<>();

    private List<Box> boxes = List.of();

    private Box localBounds;

    private Box goal;

    private Box box;

    private Direction face;

    private Box faceBox;

    private float presence;

    private float progress;

    private long lastFrame;

    private Vec3d eye;

    private double time;

    private float fade;

    private double ax;

    private double bx;

    private double ay;

    private double by;

    private double az;

    private double bz;

    public BlockOverlayModule() {
        super(
                "block-overlay",
                "Block Overlay",
                "A smooth, animated outline and fill on the block you look at.",
                ModuleCategory.VISUALS);
        this.color.visibleWhen(() -> !this.colorMode.is("Theme"));
        this.secondColor.visibleWhen(() -> this.colorMode.is("Gradient"));
        this.flow.visibleWhen(() -> !this.colorMode.is("Solid"));
        this.fillOpacity.visibleWhen(() -> !this.style.is("Outline"));
        this.width.visibleWhen(() -> !this.style.is("Fill"));
        this.glow.visibleWhen(() -> !this.style.is("Fill"));
        this.smoothSpeed.visibleWhen(this.smooth::get);
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            if (client.world != this.level) {
                this.reset();
                this.level = client.world;
            }
            long l = System.nanoTime();
            float f =
                    this.lastFrame == 0L
                            ? 0.0F
                            : Math.min((float) (l - this.lastFrame) / 1.0E9F, 0.1F);
            this.lastFrame = l;
            BlockHitResult blockHitResult = target(client);
            if (blockHitResult != null && this.adopt(client, blockHitResult)) {
                this.box =
                        this.box != null
                                        && !(this.presence < 0.02F)
                                        && this.smooth.get()
                                        && !(this.box.getCenter().distanceTo(this.goal.getCenter())
                                                > 6.0)
                                ? lerp(
                                        this.box,
                                        this.goal,
                                        1.0F - (float) Math.exp(-this.smoothSpeed.get() * f))
                                : this.goal;
                this.presence = Math.min(1.0F, this.presence + f / 0.12F);
            } else {
                blockHitResult = null;
                this.presence = Math.max(0.0F, this.presence - f / 0.2F);
                if (this.presence == 0.0F) {
                    this.reset();
                    this.level = client.world;
                    return;
                }
            }
            if (this.box != null) {
                this.updateProgress(client, f, blockHitResult != null);
                this.eye = client.gameRenderer.getCamera().getCameraPos();
                this.time = l % 3600000000000L / 1.0E9;
                this.draw();
            }
        } else {
            this.reset();
        }
    }

    private static BlockHitResult target(MinecraftClient client) {
        HitResult crosshairTarget = client.crosshairTarget;
        BlockHitResult blockHitResult;
        if (!(crosshairTarget instanceof BlockHitResult)
                || (blockHitResult = (BlockHitResult) crosshairTarget).getType() != Type.BLOCK
                || blockHitResult.isInsideBlock()) {
            return null;
        } else if (!((AccessorGameRenderer) client.gameRenderer).efn$shouldRenderBlockOutline()) {
            return null;
        } else {
            BlockPos pos2 = blockHitResult.getBlockPos();
            return !client.world.getBlockState(pos2).isAir()
                            && client.world.getWorldBorder().contains(pos2)
                    ? blockHitResult
                    : null;
        }
    }

    private boolean adopt(MinecraftClient client, BlockHitResult blockHitResult) {
        ClientWorld world = client.world;
        BlockPos pos2 = blockHitResult.getBlockPos();
        BlockPos pos3 = partner(world, pos2, world.getBlockState(pos2));
        BlockPos pos4 = pos3 != null && pos3.compareTo(pos2) < 0 ? pos3 : pos2;
        BlockPos pos5 = pos3 == null ? null : (pos4.equals(pos2) ? pos3 : pos2);
        BlockState blockState = world.getBlockState(pos4);
        BlockState state2 = pos5 == null ? null : world.getBlockState(pos5);
        if (!pos4.equals(this.pos) || blockState != this.state || state2 != this.partnerState) {
            Entity cameraEntity = client.getCameraEntity();
            ShapeContext shapeContext =
                    cameraEntity == null ? ShapeContext.absent() : ShapeContext.of(cameraEntity);
            VoxelShape outlineShape = blockState.getOutlineShape(world, pos4, shapeContext);
            if (pos5 != null) {
                outlineShape =
                        VoxelShapes.union(
                                outlineShape,
                                state2.getOutlineShape(world, pos5, shapeContext)
                                        .offset(pos5.subtract(pos4)));
            }
            if ((outlineShape = outlineShape.simplify()).isEmpty()) {
                return false;
            }
            if (!pos4.equals(this.pos)) {
                this.progress = 0.0F;
            }
            this.pos = pos4;
            this.state = blockState;
            this.partner = pos5;
            this.partnerState = state2;
            this.edges.clear();
            outlineShape.forEachEdge(
                    (value, value2, value3, value4, value5, i) ->
                            this.edges.add(
                                    new double[] {value, value2, value3, value4, value5, i}));
            this.boxes = outlineShape.getBoundingBoxes();
            this.localBounds = outlineShape.getBoundingBox();
            this.goal = this.localBounds.offset(pos4);
        }
        this.face = blockHitResult.getSide();
        this.faceBox =
                this.boxOnFace(
                        blockHitResult
                                .getPos()
                                .subtract(this.pos.getX(), this.pos.getY(), this.pos.getZ()));
        return true;
    }

    private static BlockPos partner(ClientWorld world, BlockPos pos2, BlockState state2) {
        BlockPos pos3 = null;
        if (state2.contains(Properties.DOUBLE_BLOCK_HALF)) {
            pos3 =
                    state2.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                            ? pos2.up()
                            : pos2.down();
        } else if (state2.getBlock() instanceof BedBlock) {
            pos3 = pos2.offset(BedBlock.getOppositePartDirection(state2));
        } else if (state2.getBlock() instanceof ChestBlock
                && state2.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
            pos3 = ChestBlock.getPosInFrontOf(pos2, state2);
        }
        return pos3 != null && world.getBlockState(pos3).isOf(state2.getBlock()) ? pos3 : null;
    }

    private Box boxOnFace(Vec3d vec) {
        double d = 0.001;
        for (Box box2 : this.boxes) {
            if (switch (this.face) {
                        case EAST -> Math.abs(vec.x - box2.maxX) < d;
                        case WEST -> Math.abs(vec.x - box2.minX) < d;
                        case UP -> Math.abs(vec.y - box2.maxY) < d;
                        case DOWN -> Math.abs(vec.y - box2.minY) < d;
                        case SOUTH -> Math.abs(vec.z - box2.maxZ) < d;
                        case NORTH -> Math.abs(vec.z - box2.minZ) < d;
                    }
                    && box2.expand(d).contains(vec)) {
                return box2;
            }
        }
        return this.localBounds;
    }

    private void updateProgress(MinecraftClient client, float f, boolean bl) {
        float g = 0.0F;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        AccessorBlockOverlayMultiPlayerGameMode accessorBlockOverlayMultiPlayerGameMode;
        BlockPos pos2;
        if (bl
                && this.miningProgress.get()
                && interactionManager != null
                && interactionManager.isBreakingBlock()
                && (pos2 =
                                (accessorBlockOverlayMultiPlayerGameMode =
                                                (AccessorBlockOverlayMultiPlayerGameMode)
                                                        interactionManager)
                                        .efn$blockOverlayDestroyPos())
                        != null
                && (pos2.equals(this.pos) || pos2.equals(this.partner))) {
            g =
                    MathHelper.clamp(
                            accessorBlockOverlayMultiPlayerGameMode
                                    .efn$blockOverlayDestroyProgress(),
                            0.0F,
                            1.0F);
        }
        this.progress = this.progress + (g - this.progress) * (1.0F - (float) Math.exp(-18.0F * f));
    }

    private void reset() {
        this.level = null;
        this.pos = null;
        this.state = null;
        this.partner = null;
        this.partnerState = null;
        this.edges.clear();
        this.boxes = List.of();
        this.localBounds = null;
        this.goal = null;
        this.box = null;
        this.face = null;
        this.faceBox = null;
        this.presence = 0.0F;
        this.progress = 0.0F;
        this.lastFrame = 0L;
    }

    private void draw() {
        this.fade = this.presence * this.presence * (3.0F - 2.0F * this.presence);
        double d = 0.94 + 0.06 * this.fade;
        double[] doubles =
                axis(
                        this.box.minX,
                        this.box.maxX,
                        this.goal.minX,
                        this.goal.maxX,
                        this.pos.getX() - this.goal.minX,
                        d);
        double[] doubles2 =
                axis(
                        this.box.minY,
                        this.box.maxY,
                        this.goal.minY,
                        this.goal.maxY,
                        this.pos.getY() - this.goal.minY,
                        d);
        double[] doubles3 =
                axis(
                        this.box.minZ,
                        this.box.maxZ,
                        this.goal.minZ,
                        this.goal.maxZ,
                        this.pos.getZ() - this.goal.minZ,
                        d);
        this.ax = doubles[0];
        this.bx = doubles[1];
        this.ay = doubles2[0];
        this.by = doubles2[1];
        this.az = doubles3[0];
        this.bz = doubles3[1];
        boolean bl = !this.style.is("Fill");
        boolean bl2 = !this.style.is("Outline");
        float f = this.fillOpacity.get() / 100.0F;
        if (bl2) {
            if (this.faceOnly.get()) {
                this.faceFill(this.faceBox, this.face, f, 0.002);
            } else {
                for (Box box2 : this.boxes) {
                    this.boxFill(box2, f, 0.002);
                }
            }
        }
        if (this.progress > 0.01F) {
            this.progressFill(Math.min(0.45F, f * 1.4F + 0.16F));
        }
        if (bl) {
            List edges2 = this.edges;
            if (this.faceOnly.get()) {
                edges2 = faceEdges(this.faceBox, this.face);
            }
            this.outline(edges2);
        }
    }

    private static double[] axis(double d, double e, double d2, double e2, double d3, double e3) {
        double d4 = e2 - d2;
        double e4 = d4 > 1.0E-6 ? (e - d) / d4 : 1.0;
        double d5 = (d + e) * 0.5;
        double e5 = d5 * (1.0 - e3) + (d + d3 * e4) * e3;
        return new double[] {e5, e4 * e3};
    }

    private Vec3d world(double d, double e, double d2) {
        return new Vec3d(this.ax + this.bx * d, this.ay + this.by * e, this.az + this.bz * d2);
    }

    private void outline(List<double[]> list) {
        boolean bl = !this.colorMode.is("Solid");
        ArrayList list2 = new ArrayList(list.size() * 4);
        ArrayList list3 = new ArrayList(list.size() * 4);
        for (double[] doubles : list) {
            double d =
                    Math.sqrt(
                            sq(doubles[3] - doubles[0])
                                    + sq(doubles[4] - doubles[1])
                                    + sq(doubles[5] - doubles[2]));
            int i = bl ? MathHelper.clamp((int) Math.ceil(d / 0.125), 1, 16) : 1;
            for (int j = 0; j < i; j++) {
                double e = (double) j / i;
                double d2 = (double) (j + 1) / i;
                double e2 = (e + d2) * 0.5;
                list2.add(
                        new double[] {
                            MathHelper.lerp(e, doubles[0], doubles[3]),
                            MathHelper.lerp(e, doubles[1], doubles[4]),
                            MathHelper.lerp(e, doubles[2], doubles[5]),
                            MathHelper.lerp(d2, doubles[0], doubles[3]),
                            MathHelper.lerp(d2, doubles[1], doubles[4]),
                            MathHelper.lerp(d2, doubles[2], doubles[5])
                        });
                list3.add(
                        this.colorAt(
                                MathHelper.lerp(e2, doubles[0], doubles[3]),
                                MathHelper.lerp(e2, doubles[1], doubles[4]),
                                MathHelper.lerp(e2, doubles[2], doubles[5])));
            }
        }
        float width2 = this.width.get();
        if (this.glow.get()) {
            for (int k = 0; k < list2.size(); k++) {
                double[] doubles2 = (double[]) list2.get(k);
                int m = (Integer) list3.get(k);
                this.line(
                        doubles2,
                        ColorHelper.scaleAlpha(m, 0.34F * this.fade),
                        width2 + 3.0F,
                        0.002);
                this.line(
                        doubles2,
                        ColorHelper.scaleAlpha(m, 0.13F * this.fade),
                        width2 + 8.0F,
                        0.002);
            }
        }
        for (int n = 0; n < list2.size(); n++) {
            this.line(
                    (double[]) list2.get(n),
                    ColorHelper.scaleAlpha((Integer) list3.get(n), this.fade),
                    width2,
                    0.004);
        }
    }

    private void line(double[] doubles, int i, float f, double d) {
        VisibilityConfigurable visibilityConfigurable =
                GizmoDrawing.line(
                        this.lift(this.world(doubles[0], doubles[1], doubles[2]), d),
                        this.lift(this.world(doubles[3], doubles[4], doubles[5]), d),
                        i,
                        f);
        if (this.seeThrough.get()) {
            visibilityConfigurable.ignoreOcclusion();
        }
    }

    private Vec3d lift(Vec3d vec, double d) {
        Vec3d vec2 = this.eye.subtract(vec);
        double e = vec2.length();
        return e < 1.0E-6 ? vec : vec.add(vec2.multiply(d / e));
    }

    private void boxFill(Box box2, float f, double d) {
        for (Direction direction : Direction.values()) {
            this.faceFill(box2, direction, f, d);
        }
    }

    private void faceFill(Box box2, Direction direction, float f, double d) {
        Vec3d vec = this.world(box2.minX, box2.minY, box2.minZ).subtract(d, d, d);
        Vec3d vec2 = this.world(box2.maxX, box2.maxY, box2.maxZ).add(d, d, d);
        Vec3d vec3 = faceCentre(box2, direction);
        int color2 = this.colorAt(vec3.x, vec3.y, vec3.z);
        int i =
                MathHelper.clamp(Math.round(f * this.fade * 255.0F), 0, 255) << 24
                        | color2 & 16777215;
        VisibilityConfigurable visibilityConfigurable =
                GizmoDrawing.face(vec, vec2, direction, DrawStyle.filled(i));
        if (this.seeThrough.get()) {
            visibilityConfigurable.ignoreOcclusion();
        }
    }

    private void progressFill(float f) {
        double d = 0.004;
        if (this.faceOnly.get()) {
            Box faceBox2 = this.faceBox;
            Box box2 =
                    this.face.getAxis() == Axis.Y
                            ? faceBox2.contract(
                                    faceBox2.getLengthX() * (1.0F - this.progress) * 0.5,
                                    0.0,
                                    faceBox2.getLengthZ() * (1.0F - this.progress) * 0.5)
                            : new Box(
                                    faceBox2.minX,
                                    faceBox2.minY,
                                    faceBox2.minZ,
                                    faceBox2.maxX,
                                    faceBox2.minY + faceBox2.getLengthY() * this.progress,
                                    faceBox2.maxZ);
            this.faceFill(box2, this.face, f, d);
        } else {
            double e = this.localBounds.minY + this.localBounds.getLengthY() * this.progress;
            for (Box box3 : this.boxes) {
                if (!(box3.minY >= e)) {
                    this.boxFill(
                            new Box(
                                    box3.minX,
                                    box3.minY,
                                    box3.minZ,
                                    box3.maxX,
                                    Math.min(box3.maxY, e),
                                    box3.maxZ),
                            f,
                            d);
                }
            }
        }
    }

    private int colorAt(double d, double e, double d2) {
        boolean is = this.colorMode.is("Theme");
        int i = is ? CosmeticAccent.argb() : ThemeTint.world(this.color.argb());
        if (this.colorMode.is("Solid")) {
            return i;
        } else {
            double e2 =
                    (norm(d, this.localBounds.minX, this.localBounds.maxX)
                                    + norm(e, this.localBounds.minY, this.localBounds.maxY)
                                    + norm(d2, this.localBounds.minZ, this.localBounds.maxZ))
                            / 3.0;
            double d3 = this.flow.get() ? this.time * 0.3499999940395355 : 0.0;
            if (this.colorMode.is("Rainbow")) {
                double e3 = e2 * 0.5 - d3;
                return ThemeTint.world(
                        i & 0xFF000000
                                | MathHelper.hsvToRgb((float) (e3 - Math.floor(e3)), 0.65F, 1.0F)
                                        & 16777215);
            } else {
                double d4 =
                        this.flow.get() ? 0.5 - 0.5 * Math.cos(6.283185307179586 * (e2 - d3)) : e2;
                int j =
                        is
                                ? ColorHelper.lerp(0.55F, i, -1)
                                : ThemeTint.world(this.secondColor.argb());
                return ColorHelper.lerp((float) d4, i, j);
            }
        }
    }

    private static double norm(double d, double e, double d2) {
        return d2 - e < 1.0E-6 ? 0.5 : (d - e) / (d2 - e);
    }

    private static Vec3d faceCentre(Box box2, Direction direction) {
        Vec3d center = box2.getCenter();
        return switch (direction) {
            case EAST -> new Vec3d(box2.maxX, center.y, center.z);
            case WEST -> new Vec3d(box2.minX, center.y, center.z);
            case UP -> new Vec3d(center.x, box2.maxY, center.z);
            case DOWN -> new Vec3d(center.x, box2.minY, center.z);
            case SOUTH -> new Vec3d(center.x, center.y, box2.maxZ);
            case NORTH -> new Vec3d(center.x, center.y, box2.minZ);
        };
    }

    private static List<double[]> faceEdges(Box box2, Direction direction) {
        double[][] doubleGrid;
        switch (direction.getAxis()) {
            case X:
                double d = direction == Direction.EAST ? box2.maxX : box2.minX;
                double[][] doubleGrid2 =
                        new double[][] {
                            {d, box2.minY, box2.minZ},
                            {d, box2.maxY, box2.minZ},
                            {d, box2.maxY, box2.maxZ},
                            null
                        };
                doubleGrid = doubleGrid2;
                doubleGrid2[3] = new double[] {d, box2.minY, box2.maxZ};
                break;
            case Y:
                double e = direction == Direction.UP ? box2.maxY : box2.minY;
                double[][] doubleGrid3 =
                        new double[][] {
                            {box2.minX, e, box2.minZ},
                            {box2.maxX, e, box2.minZ},
                            {box2.maxX, e, box2.maxZ},
                            null
                        };
                doubleGrid = doubleGrid3;
                doubleGrid3[3] = new double[] {box2.minX, e, box2.maxZ};
                break;
            case Z:
                double d2 = direction == Direction.SOUTH ? box2.maxZ : box2.minZ;
                double[][] doubleGrid4 =
                        new double[][] {
                            {box2.minX, box2.minY, d2},
                            {box2.maxX, box2.minY, d2},
                            {box2.maxX, box2.maxY, d2},
                            null
                        };
                doubleGrid = doubleGrid4;
                doubleGrid4[3] = new double[] {box2.minX, box2.maxY, d2};
                break;
            default:
                throw new MatchException(null, null);
        }
        double[][] doubleGrid5 = doubleGrid;
        ArrayList list = new ArrayList(4);
        for (int i = 0; i < 4; i++) {
            double[] doubles = doubleGrid5[i];
            double[] doubles2 = doubleGrid5[(i + 1) % 4];
            list.add(
                    new double[] {
                        doubles[0], doubles[1], doubles[2], doubles2[0], doubles2[1], doubles2[2]
                    });
        }
        return list;
    }

    private static Box lerp(Box box2, Box box3, float f) {
        return new Box(
                MathHelper.lerp((double) f, box2.minX, box3.minX),
                MathHelper.lerp((double) f, box2.minY, box3.minY),
                MathHelper.lerp((double) f, box2.minZ, box3.minZ),
                MathHelper.lerp((double) f, box2.maxX, box3.maxX),
                MathHelper.lerp((double) f, box2.maxY, box3.maxY),
                MathHelper.lerp((double) f, box2.maxZ, box3.maxZ));
    }

    private static double sq(double d) {
        return d * d;
    }
}
