package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.Toast.Visibility;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ChallengeToast implements Toast {

    public static final String RAINBOW = "efn:rainbow";

    private static final long RAINBOW_MILLIS = 2500L;

    private static final int BACKGROUND = -266921698;

    private static final int TITLE_COLOR = -986891;

    private static final int MESSAGE_COLOR = -5656902;

    private static final int COUNT_COLOR = -6643540;

    private static final int TIMER_TRACK = 654311423;

    private static final int TIMER_FILL = -1711276033;

    private static final int TEXT_X = 30;

    private static final int TEXT_PAD = 7;

    private static final int RIGHT_GAP = 3;

    private static final int MAX_WIDTH = 240;

    private static final int LINE_HEIGHT = 10;

    private static final int MAX_LINES = 5;

    private static final long DISPLAY_MILLIS = 5000L;

    private final Object token;

    private final ItemStack icon;

    private final boolean sound;

    private Text title;

    private Text message;

    private int width;

    private int height = 32;

    private List<OrderedText> lines = List.of();

    private int count = 1;

    private boolean changed;

    private boolean hiding;

    private long changedAt;

    private Visibility wanted = Visibility.HIDE;

    public ChallengeToast(ItemStack stack, Text text, Text text2, boolean bl) {
        this(TYPE, stack, text, text2, bl);
    }

    private ChallengeToast(
            Object token2, ItemStack stack, Text title2, Text message2, boolean sound2) {
        this.token = token2;
        this.icon = stack.copy();
        this.title = title2;
        this.message = message2;
        this.sound = sound2;
        this.fit();
    }

    public static void addOrUpdate(ItemStack stack, String string, Text text, boolean bl) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) {
            client.execute(() -> addOrUpdate(stack, string, text, bl));
        } else {
            ToastManager toastManager = client.getToastManager();
            ChallengeToast toast = toastManager.getToast(ChallengeToast.class, string);
            if (toast == null) {
                toastManager.add(new ChallengeToast(string, stack, Text.literal(string), text, bl));
            } else {
                toast.count++;
                toast.message = text;
                toast.changed = true;
                toast.fit();
            }
        }
    }

    private String countLabel() {
        return this.count > 1 ? " x" + this.count : "";
    }

    private void fit() {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int i =
                Math.max(
                        textRenderer.getWidth(this.title)
                                + textRenderer.getWidth(this.countLabel()),
                        textRenderer.getWidth(this.message));
        this.width = MathHelper.clamp(30 + i + 7, 160, 240);
        List list =
                this.message.getString().isEmpty()
                        ? List.of()
                        : textRenderer.wrapLines(this.message, this.width - 30 - 7);
        this.lines = list.size() > 5 ? List.copyOf(list.subList(0, 5)) : list;
        this.height = Math.max(32, 18 + this.lines.size() * 10 + 4);
    }

    @Override
    public Object getType() {
        return this.token;
    }

    @Override
    public SoundEvent getSoundEvent() {
        return this.sound ? SoundEvents.BLOCK_NOTE_BLOCK_BELL.value() : null;
    }

    @Override
    public Visibility getVisibility() {
        return this.wanted;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public float getYPos(int topIndex) {
        float f = topIndex * 32.0F;
        float[] floats = HudRenderer.infoBarBox();
        if (floats == null) {
            return f;
        } else {
            Window window = MinecraftClient.getInstance().getWindow();
            float scaleFactor = window.getScaleFactor();
            float xPos = this.getXPos(window.getScaledWidth(), 1.0F);
            float g = xPos + this.width;
            float h = floats[0] / scaleFactor;
            float f2 = floats[1] / scaleFactor;
            float g2 = floats[2] / scaleFactor;
            float h2 = floats[3] / scaleFactor;
            return !(f2 > 32.0F) && !(g <= h) && !(xPos >= g2) ? f + h2 + 2.0F : f;
        }
    }

    protected long displayMillis() {
        return 5000L;
    }

    protected final int cardRight() {
        return this.width - 3;
    }

    protected final int cardBottom() {
        return this.height - 1;
    }

    @Override
    public void update(ToastManager manager, long time) {
        if (!this.hiding) {
            if (this.changed) {
                this.changedAt = time;
                this.changed = false;
            }
            this.hiding =
                    time - this.changedAt
                            >= this.displayMillis()
                                    * manager.getNotificationDisplayTimeMultiplier();
            this.wanted = this.hiding ? Visibility.HIDE : Visibility.SHOW;
        }
    }

    @Override
    public void onFinishedRendering() {
        if (this.changed) {
            this.hiding = false;
            MinecraftClient.getInstance().getToastManager().add(this);
        }
    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        int i = this.width - 3;
        int j = this.height - 1;
        rounded(context, 0, 1, i, j, -266921698);
        this.drawTimer(context, startTime, i, j);
        boolean bl = this.lines.isEmpty();
        this.drawTitle(context, textRenderer, bl ? 12 : 7);
        for (int k = 0; k < this.lines.size(); k++) {
            context.drawText(
                    textRenderer, rainbowed(this.lines.get(k)), 30, 18 + k * 10, -5656902, true);
        }
        this.renderIcon(context, this.icon);
    }

    protected void renderIcon(DrawContext context, ItemStack stack) {
        context.drawItemWithoutEntity(stack, 8, 8);
    }

    private void drawTimer(DrawContext context, long l, int i, int j) {
        double d =
                this.displayMillis()
                        * MinecraftClient.getInstance()
                                .getToastManager()
                                .getNotificationDisplayTimeMultiplier();
        float f = 1.0F - (float) MathHelper.clamp((l - this.changedAt) / d, 0.0, 1.0);
        byte b = 4;
        int k = i - 4;
        int m = j - 2;
        context.fill(b, m, k, m + 1, 654311423);
        context.fill(b, m, b + Math.round((k - b) * f), m + 1, -1711276033);
    }

    private static void rounded(DrawContext context, int i, int j, int k, int m, int n) {
        context.fill(i + 2, j, k - 2, j + 1, n);
        context.fill(i + 1, j + 1, k - 1, j + 2, n);
        context.fill(i, j + 2, k, m - 2, n);
        context.fill(i + 1, m - 2, k - 1, m - 1, n);
        context.fill(i + 2, m - 1, k - 2, m, n);
    }

    private void drawTitle(DrawContext context, TextRenderer textRenderer, int i) {
        String countLabel = this.countLabel();
        int j = this.width - 30 - 7 - textRenderer.getWidth(countLabel);
        OrderedText orderedText = this.title.asOrderedText();
        if (textRenderer.getWidth(this.title) > j) {
            MutableText text = Text.literal("\u2026");
            StringVisitable stringVisitable =
                    textRenderer.trimToWidth(this.title, j - textRenderer.getWidth(text));
            orderedText =
                    Language.getInstance().reorder(StringVisitable.concat(stringVisitable, text));
        }
        context.drawText(textRenderer, rainbowed(orderedText), 30, i, -986891, true);
        if (!countLabel.isEmpty()) {
            context.drawText(
                    textRenderer,
                    countLabel,
                    30 + textRenderer.getWidth(orderedText),
                    i,
                    -6643540,
                    true);
        }
    }

    private static OrderedText rainbowed(OrderedText orderedText) {
        float f = (float) (Util.getMeasuringTimeMs() % 2500L) / 2500.0F;
        return characterVisitor ->
                orderedText.accept(
                        (i, style, j) -> {
                            if (!"efn:rainbow".equals(style.getInsertion())) {
                                return characterVisitor.accept(i, style, j);
                            } else {
                                float g = i * 0.045F - f;
                                int hsvToRgb =
                                        MathHelper.hsvToRgb(g - (float) Math.floor(g), 0.75F, 1.0F);
                                return characterVisitor.accept(
                                        i,
                                        style.withInsertion(null)
                                                .withColor(TextColor.fromRgb(hsvToRgb)),
                                        j);
                            }
                        });
    }
}
