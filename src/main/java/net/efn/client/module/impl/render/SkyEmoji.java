package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D.Float;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

@Environment(EnvType.CLIENT)
enum SkyEmoji {
    BROKEN_HEART(
            Set.of("heartbreak", "heartbroken", "brokenhearted"),
            Set.of("k\u0131r\u0131k", "kirik", "y\u0131k\u0131l", "parampar\u00e7a")),
    HEART(Set.of("heart", "hearts"), Set.of("kalp", "kalb")),
    KISS(Set.of("kiss", "kisses", "kissing", "kissed"), Set.of("\u00f6p")),
    HUG(
            Set.of("hug", "hugs", "hold", "holding", "embrace", "cuddle"),
            Set.of("sar\u0131l", "kucak")),
    LOVE(
            Set.of("love", "loved", "lover", "lovers", "loving", "baby", "darling", "honey"),
            Set.of("a\u015fk", "sevgi", "seviyor", "sevdi")),
    SHY(Set.of("shy", "blush", "blushing", "crush", "cute"), Set.of("utan", "tatl\u0131")),
    CRY(
            Set.of("cry", "cried", "crying", "cries", "tears", "tear", "weep"),
            Set.of("a\u011fla", "g\u00f6zya\u015f")),
    SAD(
            Set.of("sad", "lonely", "alone", "pain", "hurt", "hurts", "sorrow", "goodbye"),
            Set.of(
                    "\u00fczg\u00fcn",
                    "yaln\u0131z",
                    "ac\u0131",
                    "h\u00fcz\u00fcn",
                    "keder",
                    "elveda")),
    LAUGH(
            Set.of("laugh", "laughing", "haha", "funny", "lol", "smile", "smiling"),
            Set.of("g\u00fcl\u00fcyor", "g\u00fclerim", "kahkaha", "g\u00fcl\u00fcmse")),
    DEVIL(
            Set.of(
                    "sin",
                    "sins",
                    "sinner",
                    "sinners",
                    "devil",
                    "hell",
                    "evil",
                    "demon",
                    "danger",
                    "dangerous",
                    "wicked"),
            Set.of("\u015feytan", "g\u00fcnah", "cehennem", "tehlike")),
    FIRE(
            Set.of("fire", "burn", "burning", "flame", "flames", "lit", "blaze"),
            Set.of("ate\u015f", "alev", "yan\u0131yor", "yak\u0131yor")),
    ANGRY(
            Set.of("angry", "mad", "hate", "rage", "fury"),
            Set.of("k\u0131zg\u0131n", "nefret", "\u00f6fke", "sinir")),
    SHOCK(
            Set.of("wow", "omg", "shock", "shocked", "crazy", "insane"),
            Set.of("\u015fok", "inan\u0131lmaz", "\u00e7\u0131lg\u0131n")),
    COOL(Set.of("cool", "swag", "drip", "boss", "flex", "fresh"), Set.of("haval\u0131", "efsane")),
    MONEY(
            Set.of("money", "cash", "rich", "dollar", "dollars", "millionaire", "paid"),
            Set.of("zengin", "dolar", "paray\u0131", "paralar")),
    STAR(
            Set.of("star", "stars", "shine", "shining", "sparkle"),
            Set.of("y\u0131ld\u0131z", "parla")),
    MOON(
            Set.of("moon", "night", "midnight", "tonight", "dream", "dreams", "dreaming"),
            Set.of("gece", "r\u00fcya", "mehtap")),
    SLEEP(
            Set.of("sleep", "sleeping", "tired", "asleep"),
            Set.of("uyku", "uyuyor", "uyudu", "yorgun")),
    THINK(
            Set.of("think", "thinking", "thought", "wonder", "why", "maybe", "guess"),
            Set.of("d\u00fc\u015f\u00fcn", "neden", "belki", "acaba")),
    MUSIC(
            Set.of(
                    "dance", "dancing", "song", "songs", "music", "sing", "singing", "melody",
                    "beat"),
            Set.of("dans", "\u015fark\u0131", "m\u00fczik", "melodi"));

    static final int SIZE = 256;

    private static final String IMAGES = "/assets/efn/textures/sky_emoji/";

    private static final int CANVAS = 192;

    private static final Pattern WORD = Pattern.compile("\\p{L}+");

    private static final String[][] PHRASES =
            new String[][] {
                {"broken heart", "broken heart"},
                {"break my heart", "heart"},
                {"kalbim k\u0131r\u0131k", "k\u0131r\u0131k"}
            };

    private static final Color INK = new Color(662080);

    private static final Color TEAR = new Color(10479359);

    private static final Color RED = new Color(16726876);

    private final Set<String> words;

    private final Set<String> stems;

    private SkyEmoji(Set<String> words2, Set<String> stems2) {
        this.words = words2;
        this.stems = stems2;
    }

    static SkyEmoji.Match match(String string) {
        for (String[] strings : PHRASES) {
            Matcher matcher = Pattern.compile(Pattern.quote(strings[0]), 66).matcher(string);
            if (matcher.find()) {
                int i = matcher.start() + strings[0].indexOf(strings[1]);
                return new SkyEmoji.Match(BROKEN_HEART, i, i + strings[1].length());
            }
        }
        ArrayList list = new ArrayList();
        ArrayList list2 = new ArrayList();
        Matcher matcher2 = WORD.matcher(string);
        while (matcher2.find()) {
            list.add(new int[] {matcher2.start(), matcher2.end()});
            list2.add(matcher2.group().toLowerCase(Locale.ROOT).replace("i\u0307", "i"));
        }
        for (SkyEmoji skyEmoji : values()) {
            for (int j = 0; j < list2.size(); j++) {
                String string2 = (String) list2.get(j);
                boolean bl = skyEmoji.words.contains(string2);
                for (String stem : skyEmoji.stems) {
                    bl |= string2.startsWith(stem);
                }
                if (bl) {
                    return new SkyEmoji.Match(
                            skyEmoji, ((int[]) list.get(j))[0], ((int[]) list.get(j))[1]);
                }
            }
        }
        return null;
    }

    int[] draw() {
        BufferedImage bufferedImage = this.picture();
        if (bufferedImage != null || this != HUG && this != SHY && this != THINK) {
            BufferedImage bufferedImage2 = new BufferedImage(256, 256, 2);
            Graphics2D graphics = bufferedImage2.createGraphics();
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(
                    RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (bufferedImage != null) {
                graphics.drawImage(bufferedImage, 0, 0, 256, 256, null);
                graphics.dispose();
                return bufferedImage2.getRGB(0, 0, 256, 256, null, 0, 256);
            } else {
                graphics.scale(1.3333333333333333, 1.3333333333333333);
                switch (this) {
                    case BROKEN_HEART:
                        heart(graphics, 96.0F, 100.0F, 56.0F, true);
                        break;
                    case HEART:
                        heart(graphics, 96.0F, 100.0F, 64.0F, false);
                        break;
                    case KISS:
                    case HUG:
                    case LOVE:
                    case SHY:
                    case CRY:
                    case SAD:
                    case LAUGH:
                    case DEVIL:
                    case ANGRY:
                    case SHOCK:
                    case COOL:
                    case MONEY:
                    case SLEEP:
                    case THINK:
                    default:
                        this.face(graphics);
                        break;
                    case FIRE:
                        flame(graphics);
                        break;
                    case STAR:
                        star(graphics);
                        break;
                    case MOON:
                        moon(graphics);
                        break;
                    case MUSIC:
                        notes(graphics);
                }
                graphics.dispose();
                return bufferedImage2.getRGB(0, 0, 256, 256, null, 0, 256);
            }
        } else {
            return null;
        }
    }

    private BufferedImage picture() {
        try {
            BufferedImage bufferedImage;
            try (InputStream resourceAsStream =
                    SkyEmoji.class.getResourceAsStream(
                            "/assets/efn/textures/sky_emoji/"
                                    + this.name().toLowerCase(Locale.ROOT)
                                    + ".png")) {
                bufferedImage = resourceAsStream == null ? null : ImageIO.read(resourceAsStream);
            }
            return bufferedImage;
        } catch (IOException exception) {
            return null;
        }
    }

    private void face(Graphics2D graphics2D) {
        if (this == DEVIL) {
            horns(graphics2D);
        }
        sphere(graphics2D, 96.0F, 100.0F, 72.0F);
        switch (this) {
            case KISS:
                closedEye(graphics2D, 72.0F, 92.0F, 13.0F, true);
                eye(graphics2D, 120.0F, 90.0F, 11.0F, 14.0F, 0.0F, 0.0F);
                kissLips(graphics2D, 102.0F, 128.0F);
                heart(graphics2D, 150.0F, 62.0F, 12.0F, false);
                break;
            case HUG:
            case SHY:
            case FIRE:
            case STAR:
            case MOON:
            default:
                eye(graphics2D, 72.0F, 92.0F, 11.0F, 14.0F, 0.0F, 0.0F);
                eye(graphics2D, 120.0F, 92.0F, 11.0F, 14.0F, 0.0F, 0.0F);
                break;
            case LOVE:
                heart(graphics2D, 70.0F, 90.0F, 20.0F, false);
                heart(graphics2D, 122.0F, 90.0F, 20.0F, false);
                openSmile(graphics2D, 96.0F, 122.0F, 30.0F, 18.0F, true);
                break;
            case CRY:
                tears(graphics2D, 72.0F, 98.0F);
                tears(graphics2D, 120.0F, 98.0F);
                closedEye(graphics2D, 72.0F, 92.0F, 13.0F, false);
                closedEye(graphics2D, 120.0F, 92.0F, 13.0F, false);
                brows(graphics2D, 72.0F, 72.0F, 120.0F, 72.0F, -0.35F);
                openFrown(graphics2D, 96.0F, 134.0F, 22.0F, 13.0F);
                break;
            case SAD:
                droopyEye(graphics2D, 72.0F, 94.0F);
                droopyEye(graphics2D, 120.0F, 94.0F);
                brows(graphics2D, 72.0F, 74.0F, 120.0F, 74.0F, -0.3F);
                frown(graphics2D, 96.0F, 134.0F, 18.0F);
                drop(graphics2D, 62.0F, 118.0F, 7.0F);
                break;
            case LAUGH:
                happyEye(graphics2D, 72.0F, 90.0F);
                happyEye(graphics2D, 120.0F, 90.0F);
                laughMouth(graphics2D, 96.0F, 120.0F);
                drop(graphics2D, 44.0F, 98.0F, 8.0F);
                drop(graphics2D, 148.0F, 98.0F, 8.0F);
                break;
            case DEVIL:
                eye(graphics2D, 72.0F, 94.0F, 11.0F, 9.0F, 0.2F, 0.0F);
                eye(graphics2D, 120.0F, 94.0F, 11.0F, 9.0F, -0.2F, 0.0F);
                brows(graphics2D, 72.0F, 78.0F, 120.0F, 78.0F, 0.45F);
                smirk(graphics2D, 96.0F, 126.0F, true);
                break;
            case ANGRY:
                eye(graphics2D, 72.0F, 96.0F, 11.0F, 9.0F, 0.15F, 0.0F);
                eye(graphics2D, 120.0F, 96.0F, 11.0F, 9.0F, -0.15F, 0.0F);
                brows(graphics2D, 72.0F, 78.0F, 120.0F, 78.0F, 0.6F);
                teeth(graphics2D, 96.0F, 130.0F);
                vein(graphics2D, 146.0F, 56.0F);
                break;
            case SHOCK:
                eye(graphics2D, 72.0F, 90.0F, 15.0F, 18.0F, 0.0F, 0.0F);
                eye(graphics2D, 120.0F, 90.0F, 15.0F, 18.0F, 0.0F, 0.0F);
                brows(graphics2D, 72.0F, 64.0F, 120.0F, 64.0F, -0.15F);
                ring(graphics2D, 96.0F, 134.0F, 13.0F, 16.0F);
                break;
            case COOL:
                shades(graphics2D);
                smirk(graphics2D, 98.0F, 128.0F, false);
                break;
            case MONEY:
                dollarEyes(graphics2D);
                openSmile(graphics2D, 96.0F, 122.0F, 30.0F, 18.0F, false);
                banknoteTongue(graphics2D, 96.0F, 138.0F);
                break;
            case SLEEP:
                closedEye(graphics2D, 72.0F, 94.0F, 13.0F, true);
                closedEye(graphics2D, 120.0F, 94.0F, 13.0F, true);
                ring(graphics2D, 96.0F, 130.0F, 6.0F, 7.0F);
                zz(graphics2D);
        }
    }

    private static void sphere(Graphics2D graphics2D, float f, float g, float h) {
        for (int i = 6; i >= 1; i--) {
            float f2 = i * 2.2F;
            graphics2D.setColor(new Color(0, 10, 40, 12));
            graphics2D.fill(
                    new Float(
                            f - h - f2 * 0.5F,
                            g - h + 6.0F - f2 * 0.3F,
                            (h + f2 * 0.5F) * 2.0F,
                            (h + f2 * 0.4F) * 2.0F));
        }
        Float value = new Float(f - h, g - h, h * 2.0F, h * 2.0F);
        graphics2D.setPaint(
                new RadialGradientPaint(
                        new java.awt.geom.Point2D.Float(f - h * 0.35F, g - h * 0.45F),
                        h * 1.55F,
                        new float[] {0.0F, 0.35F, 0.75F, 1.0F},
                        new Color[] {
                            new Color(11132159),
                            new Color(4101621),
                            new Color(1791689),
                            new Color(798078)
                        }));
        graphics2D.fill(value);
        graphics2D.setPaint(
                new RadialGradientPaint(
                        new java.awt.geom.Point2D.Float(f, g + h * 0.9F),
                        h * 1.1F,
                        new float[] {0.0F, 1.0F},
                        new Color[] {new Color(7327999), new Color(7327999, true)}));
        graphics2D.fill(value);
        graphics2D.setStroke(new BasicStroke(2.5F));
        graphics2D.setColor(new Color(666222));
        graphics2D.draw(value);
        AffineTransform transform = graphics2D.getTransform();
        graphics2D.rotate(-0.5, f - h * 0.42F, g - h * 0.55F);
        graphics2D.setPaint(
                new GradientPaint(
                        0.0F,
                        g - h * 0.8F,
                        new Color(255, 255, 255, 190),
                        0.0F,
                        g - h * 0.25F,
                        new Color(255, 255, 255, 0)));
        graphics2D.fill(new Float(f - h * 0.75F, g - h * 0.8F, h * 0.7F, h * 0.42F));
        graphics2D.setTransform(transform);
    }

    private static void eye(
            Graphics2D graphics2D, float f, float g, float h, float f2, float g2, float h2) {
        AffineTransform transform = graphics2D.getTransform();
        graphics2D.rotate(g2, f, g);
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(new Float(f - h, g - f2, h * 2.0F, f2 * 2.0F));
        graphics2D.setColor(INK);
        float f3 = Math.min(h, f2) * 0.62F;
        graphics2D.fill(new Float(f - f3 + h2, g - f3 + f2 * 0.15F, f3 * 2.0F, f3 * 2.0F));
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(
                new Float(f - f3 * 0.25F + h2, g - f3 * 0.55F + f2 * 0.15F, f3 * 0.6F, f3 * 0.6F));
        graphics2D.setTransform(transform);
    }

    private static void closedEye(Graphics2D graphics2D, float f, float g, float h, boolean bl) {
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(5.0F, 1, 1));
        float f2 = bl ? 8.0F : -8.0F;
        graphics2D.draw(new java.awt.geom.QuadCurve2D.Float(f - h, g, f, g + f2, f + h, g));
    }

    private static void happyEye(Graphics2D graphics2D, float f, float g) {
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
        graphics2D.draw(
                new java.awt.geom.QuadCurve2D.Float(
                        f - 13.0F, g + 6.0F, f, g - 14.0F, f + 13.0F, g + 6.0F));
    }

    private static void droopyEye(Graphics2D graphics2D, float f, float g) {
        Area area = new Area(new Float(f - 11.0F, g - 13.0F, 22.0F, 26.0F));
        area.subtract(new Area(new Float(f - 16.0F, g - 26.0F, 32.0F, 22.0F)));
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(area);
        graphics2D.setColor(INK);
        graphics2D.fill(new Float(f - 6.0F, g - 2.0F, 12.0F, 12.0F));
        graphics2D.setStroke(new BasicStroke(3.5F, 1, 1));
        graphics2D.draw(
                new java.awt.geom.QuadCurve2D.Float(
                        f - 12.0F, g - 5.0F, f, g - 1.0F, f + 12.0F, g - 5.0F));
    }

    private static void brows(
            Graphics2D graphics2D, float f, float g, float h, float f2, float g2) {
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
        float h2 = 12.0F;
        float f3 = 12.0F * g2;
        graphics2D.draw(
                new java.awt.geom.QuadCurve2D.Float(f - h2, g - f3, f, g - 3.0F, f + h2, g + f3));
        graphics2D.draw(
                new java.awt.geom.QuadCurve2D.Float(
                        h - h2, f2 + f3, h, f2 - 3.0F, h + h2, f2 - f3));
    }

    private static void openSmile(
            Graphics2D graphics2D, float f, float g, float h, float f2, boolean bl) {
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(f - h, g);
        generalPath.quadTo(f, g + f2 * 2.2F, f + h, g);
        generalPath.quadTo(f, g + f2 * 0.4F, f - h, g);
        generalPath.closePath();
        graphics2D.setColor(new Color(2755102));
        graphics2D.fill(generalPath);
        if (bl) {
            Shape clip = graphics2D.getClip();
            graphics2D.clip(generalPath);
            graphics2D.setColor(new Color(16740241));
            graphics2D.fill(new Float(f - h * 0.5F, g + f2 * 0.7F, h, f2 * 1.4F));
            graphics2D.setClip(clip);
        }
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(3.0F, 1, 1));
        graphics2D.draw(generalPath);
    }

    private static void openFrown(Graphics2D graphics2D, float f, float g, float h, float f2) {
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(f - h, g + f2 * 0.6F);
        generalPath.quadTo(f, g - f2 * 1.4F, f + h, g + f2 * 0.6F);
        generalPath.quadTo(f, g + f2 * 0.2F, f - h, g + f2 * 0.6F);
        generalPath.closePath();
        graphics2D.setColor(new Color(2755102));
        graphics2D.fill(generalPath);
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(3.0F, 1, 1));
        graphics2D.draw(generalPath);
    }

    private static void frown(Graphics2D graphics2D, float f, float g, float h) {
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
        graphics2D.draw(
                new java.awt.geom.QuadCurve2D.Float(
                        f - h, g + 4.0F, f, g - 10.0F, f + h, g + 4.0F));
    }

    private static void smirk(Graphics2D graphics2D, float f, float g, boolean bl) {
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
        float h = bl ? 30.0F : 22.0F;
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(f - h, g - 2.0F);
        generalPath.curveTo(f - h * 0.3F, g + 10.0F, f + h * 0.5F, g + 8.0F, f + h, g - 10.0F);
        graphics2D.draw(generalPath);
    }

    private static void laughMouth(Graphics2D graphics2D, float f, float g) {
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(f - 34.0F, g - 4.0F);
        generalPath.lineTo(f + 34.0F, g - 4.0F);
        generalPath.quadTo(f + 30.0F, g + 38.0F, f, g + 38.0F);
        generalPath.quadTo(f - 30.0F, g + 38.0F, f - 34.0F, g - 4.0F);
        generalPath.closePath();
        graphics2D.setColor(new Color(2755102));
        graphics2D.fill(generalPath);
        Shape clip = graphics2D.getClip();
        graphics2D.clip(generalPath);
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(
                new java.awt.geom.RoundRectangle2D.Float(
                        f - 34.0F, g - 6.0F, 68.0F, 13.0F, 6.0F, 6.0F));
        graphics2D.setColor(new Color(16740241));
        graphics2D.fill(new Float(f - 18.0F, g + 18.0F, 36.0F, 26.0F));
        graphics2D.setClip(clip);
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(3.0F, 1, 1));
        graphics2D.draw(generalPath);
    }

    private static void teeth(Graphics2D graphics2D, float f, float g) {
        java.awt.geom.RoundRectangle2D.Float value =
                new java.awt.geom.RoundRectangle2D.Float(
                        f - 26.0F, g - 10.0F, 52.0F, 20.0F, 12.0F, 12.0F);
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(value);
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(2.5F));
        graphics2D.draw(value);
        graphics2D.drawLine((int) (f - 26.0F), (int) g, (int) (f + 26.0F), (int) g);
        for (int i = -2; i <= 2; i++) {
            graphics2D.drawLine(
                    (int) (f + i * 9.0F),
                    (int) (g - 10.0F),
                    (int) (f + i * 9.0F),
                    (int) (g + 10.0F));
        }
    }

    private static void ring(Graphics2D graphics2D, float f, float g, float h, float f2) {
        graphics2D.setColor(new Color(2755102));
        graphics2D.fill(new Float(f - h, g - f2, h * 2.0F, f2 * 2.0F));
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(3.0F));
        graphics2D.draw(new Float(f - h, g - f2, h * 2.0F, f2 * 2.0F));
    }

    private static void kissLips(Graphics2D graphics2D, float f, float g) {
        graphics2D.setColor(INK);
        graphics2D.setStroke(new BasicStroke(5.5F, 1, 1));
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(f - 6.0F, g - 12.0F);
        generalPath.quadTo(f + 10.0F, g - 10.0F, f + 2.0F, g - 2.0F);
        generalPath.quadTo(f + 12.0F, g + 4.0F, f - 6.0F, g + 10.0F);
        graphics2D.draw(generalPath);
    }

    private static void tears(Graphics2D graphics2D, float f, float g) {
        graphics2D.setPaint(
                new GradientPaint(
                        0.0F, g, new Color(13235199), 0.0F, g + 70.0F, new Color(5227519)));
        graphics2D.fill(
                new java.awt.geom.RoundRectangle2D.Float(f - 7.0F, g, 14.0F, 66.0F, 14.0F, 14.0F));
    }

    private static void drop(Graphics2D graphics2D, float f, float g, float h) {
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(f, g - h * 2.2F);
        generalPath.curveTo(f + h * 1.2F, g - h * 0.6F, f + h, g + h, f, g + h);
        generalPath.curveTo(f - h, g + h, f - h * 1.2F, g - h * 0.6F, f, g - h * 2.2F);
        graphics2D.setColor(TEAR);
        graphics2D.fill(generalPath);
        graphics2D.setColor(new Color(255, 255, 255, 200));
        graphics2D.fill(new Float(f - h * 0.45F, g - h * 0.4F, h * 0.5F, h * 0.6F));
    }

    private static void shades(Graphics2D graphics2D) {
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(42.0F, 80.0F);
        generalPath.lineTo(90.0F, 80.0F);
        generalPath.quadTo(90.0F, 108.0F, 68.0F, 108.0F);
        generalPath.quadTo(44.0F, 108.0F, 42.0F, 80.0F);
        generalPath.closePath();
        GeneralPath generalPath2 = (GeneralPath) generalPath.clone();
        generalPath2.transform(AffineTransform.getTranslateInstance(60.0, 0.0));
        graphics2D.setColor(new Color(724764));
        graphics2D.fill(generalPath);
        graphics2D.fill(generalPath2);
        graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
        graphics2D.draw(new java.awt.geom.Line2D.Float(88.0F, 84.0F, 104.0F, 84.0F));
        graphics2D.setColor(new Color(255, 255, 255, 120));
        graphics2D.setStroke(new BasicStroke(4.0F, 1, 1));
        graphics2D.drawLine(52, 86, 62, 96);
        graphics2D.drawLine(112, 86, 122, 96);
    }

    private static void dollarEyes(Graphics2D graphics2D) {
        graphics2D.setFont(new Font("SansSerif", 1, 40));
        graphics2D.setColor(new Color(2347360));
        graphics2D.drawString("$", 60, 106);
        graphics2D.drawString("$", 108, 106);
    }

    private static void banknoteTongue(Graphics2D graphics2D, float f, float g) {
        graphics2D.setColor(new Color(4054148));
        graphics2D.fill(
                new java.awt.geom.RoundRectangle2D.Float(
                        f - 14.0F, g - 4.0F, 28.0F, 26.0F, 6.0F, 6.0F));
        graphics2D.setColor(new Color(2002510));
        graphics2D.setStroke(new BasicStroke(2.0F));
        graphics2D.draw(
                new java.awt.geom.RoundRectangle2D.Float(
                        f - 14.0F, g - 4.0F, 28.0F, 26.0F, 6.0F, 6.0F));
    }

    private static void zz(Graphics2D graphics2D) {
        graphics2D.setColor(Color.WHITE);
        graphics2D.setFont(new Font("SansSerif", 1, 30));
        graphics2D.drawString("Z", 140, 52);
        graphics2D.setFont(new Font("SansSerif", 1, 20));
        graphics2D.drawString("z", 162, 30);
    }

    private static void vein(Graphics2D graphics2D, float f, float g) {
        graphics2D.setColor(RED);
        graphics2D.setStroke(new BasicStroke(5.0F, 1, 1));
        for (int i = 0; i < 4; i++) {
            AffineTransform transform = graphics2D.getTransform();
            graphics2D.rotate(i * 3.141592653589793 / 2.0, f, g);
            graphics2D.draw(
                    new java.awt.geom.QuadCurve2D.Float(
                            f - 4.0F, g - 14.0F, f - 4.0F, g - 4.0F, f - 14.0F, g - 4.0F));
            graphics2D.setTransform(transform);
        }
    }

    private static void horns(Graphics2D graphics2D) {
        for (int i = -1; i <= 1; i += 2) {
            GeneralPath generalPath = new GeneralPath();
            float f = 96.0F + i * 40.0F;
            generalPath.moveTo(f - i * 16.0F, 48.0F);
            generalPath.quadTo(f + i * 12.0F, 34.0F, f + i * 18.0F, 10.0F);
            generalPath.quadTo(f + i * 26.0F, 40.0F, f + i * 14.0F, 62.0F);
            generalPath.closePath();
            graphics2D.setPaint(
                    new GradientPaint(
                            0.0F, 10.0F, new Color(14183423), 0.0F, 62.0F, new Color(6954936)));
            graphics2D.fill(generalPath);
            graphics2D.setColor(new Color(4001138));
            graphics2D.setStroke(new BasicStroke(2.5F));
            graphics2D.draw(generalPath);
        }
    }

    private static Shape heartShape(float f, float g, float h) {
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(f, g + h * 0.95F);
        generalPath.curveTo(
                f - h * 1.35F, g + h * 0.05F, f - h * 1.1F, g - h * 1.05F, f, g - h * 0.45F);
        generalPath.curveTo(
                f + h * 1.1F, g - h * 1.05F, f + h * 1.35F, g + h * 0.05F, f, g + h * 0.95F);
        generalPath.closePath();
        return generalPath;
    }

    private static void heart(Graphics2D graphics2D, float f, float g, float h, boolean bl) {
        Shape shape = heartShape(f, g, h);
        SkyEmoji.HeartPaint heartPaint = new SkyEmoji.HeartPaint(f, g, h);
        if (!bl) {
            heartPaint.fill(graphics2D, shape);
        } else {
            GeneralPath generalPath = new GeneralPath();
            generalPath.moveTo(f, g - h);
            generalPath.lineTo(f - h * 0.18F, g - h * 0.2F);
            generalPath.lineTo(f + h * 0.15F, g + h * 0.15F);
            generalPath.lineTo(f - h * 0.1F, g + h * 0.6F);
            generalPath.lineTo(f, g + h * 1.2F);
            generalPath.lineTo(f - h * 2.0F, g + h * 1.2F);
            generalPath.lineTo(f - h * 2.0F, g - h * 1.2F);
            generalPath.closePath();
            Area area = new Area(shape);
            area.intersect(new Area(generalPath));
            Area area2 = new Area(shape);
            area2.subtract(new Area(generalPath));
            area.transform(AffineTransform.getRotateInstance(-0.1, f, g + h));
            area.transform(AffineTransform.getTranslateInstance(-5.0, 0.0));
            area2.transform(AffineTransform.getRotateInstance(0.1, f, g + h));
            area2.transform(AffineTransform.getTranslateInstance(5.0, 0.0));
            heartPaint.fill(graphics2D, area);
            heartPaint.fill(graphics2D, area2);
        }
    }

    private static void flame(Graphics2D graphics2D) {
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(96.0F, 176.0F);
        generalPath.curveTo(40.0F, 176.0F, 30.0F, 122.0F, 58.0F, 86.0F);
        generalPath.curveTo(66.0F, 110.0F, 76.0F, 112.0F, 80.0F, 108.0F);
        generalPath.curveTo(70.0F, 74.0F, 92.0F, 42.0F, 110.0F, 16.0F);
        generalPath.curveTo(112.0F, 58.0F, 150.0F, 76.0F, 152.0F, 120.0F);
        generalPath.curveTo(154.0F, 156.0F, 130.0F, 176.0F, 96.0F, 176.0F);
        generalPath.closePath();
        graphics2D.setPaint(
                new GradientPaint(
                        0.0F, 16.0F, new Color(8382719), 0.0F, 176.0F, new Color(1791689)));
        graphics2D.fill(generalPath);
        graphics2D.setColor(new Color(666222));
        graphics2D.setStroke(new BasicStroke(2.5F));
        graphics2D.draw(generalPath);
        GeneralPath generalPath2 = new GeneralPath();
        generalPath2.moveTo(98.0F, 168.0F);
        generalPath2.curveTo(70.0F, 168.0F, 66.0F, 136.0F, 82.0F, 116.0F);
        generalPath2.curveTo(88.0F, 132.0F, 96.0F, 130.0F, 98.0F, 122.0F);
        generalPath2.curveTo(96.0F, 102.0F, 108.0F, 86.0F, 116.0F, 76.0F);
        generalPath2.curveTo(118.0F, 104.0F, 132.0F, 118.0F, 128.0F, 142.0F);
        generalPath2.curveTo(126.0F, 160.0F, 114.0F, 168.0F, 98.0F, 168.0F);
        generalPath2.closePath();
        graphics2D.setPaint(
                new GradientPaint(0.0F, 76.0F, Color.WHITE, 0.0F, 168.0F, new Color(11132159)));
        graphics2D.fill(generalPath2);
    }

    private static void star(Graphics2D graphics2D) {
        GeneralPath generalPath = new GeneralPath();
        for (int i = 0; i < 10; i++) {
            double d = -1.5707963267948966 + i * 3.141592653589793 / 5.0;
            double e = i % 2 == 0 ? 80.0 : 34.0;
            float f = (float) (96.0 + Math.cos(d) * e);
            float g = (float) (100.0 + Math.sin(d) * e);
            if (i == 0) {
                generalPath.moveTo(f, g);
            } else {
                generalPath.lineTo(f, g);
            }
        }
        generalPath.closePath();
        graphics2D.setPaint(
                new RadialGradientPaint(
                        new java.awt.geom.Point2D.Float(80.0F, 80.0F),
                        100.0F,
                        new float[] {0.0F, 0.5F, 1.0F},
                        new Color[] {new Color(16774832), new Color(16762941), new Color(14255642)},
                        CycleMethod.NO_CYCLE));
        graphics2D.fill(generalPath);
        graphics2D.setColor(new Color(9064202));
        graphics2D.setStroke(new BasicStroke(2.5F, 1, 1));
        graphics2D.draw(generalPath);
        graphics2D.setColor(new Color(255, 255, 255, 170));
        graphics2D.fill(new Float(72.0F, 62.0F, 22.0F, 12.0F));
    }

    private static void moon(Graphics2D graphics2D) {
        Area area = new Area(new Float(30.0F, 26.0F, 140.0F, 140.0F));
        area.subtract(new Area(new Float(70.0F, 8.0F, 130.0F, 130.0F)));
        graphics2D.setPaint(
                new RadialGradientPaint(
                        new java.awt.geom.Point2D.Float(60.0F, 70.0F),
                        120.0F,
                        new float[] {0.0F, 1.0F},
                        new Color[] {new Color(16775112), new Color(15909198)}));
        graphics2D.fill(area);
        graphics2D.setColor(new Color(10119954));
        graphics2D.setStroke(new BasicStroke(2.5F));
        graphics2D.draw(area);
        graphics2D.setColor(new Color(16775112));
        sparkle(graphics2D, 150.0F, 140.0F, 12.0F);
        sparkle(graphics2D, 168.0F, 104.0F, 7.0F);
    }

    private static void sparkle(Graphics2D graphics2D, float f, float g, float h) {
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(f, g - h);
        generalPath.quadTo(f, g, f + h, g);
        generalPath.quadTo(f, g, f, g + h);
        generalPath.quadTo(f, g, f - h, g);
        generalPath.quadTo(f, g, f, g - h);
        graphics2D.fill(generalPath);
    }

    private static void notes(Graphics2D graphics2D) {
        graphics2D.setPaint(
                new GradientPaint(
                        0.0F, 30.0F, new Color(9425919), 0.0F, 170.0F, new Color(1791689)));
        graphics2D.fill(new Float(36.0F, 132.0F, 44.0F, 32.0F));
        graphics2D.fill(new Float(112.0F, 116.0F, 44.0F, 32.0F));
        graphics2D.fill(new java.awt.geom.Rectangle2D.Float(70.0F, 42.0F, 10.0F, 104.0F));
        graphics2D.fill(new java.awt.geom.Rectangle2D.Float(146.0F, 26.0F, 10.0F, 104.0F));
        GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(70.0F, 42.0F);
        generalPath.lineTo(156.0F, 26.0F);
        generalPath.lineTo(156.0F, 50.0F);
        generalPath.lineTo(70.0F, 66.0F);
        generalPath.closePath();
        graphics2D.fill(generalPath);
        graphics2D.setColor(new Color(666222));
        graphics2D.setStroke(new BasicStroke(2.5F));
        graphics2D.draw(new Float(36.0F, 132.0F, 44.0F, 32.0F));
        graphics2D.draw(new Float(112.0F, 116.0F, 44.0F, 32.0F));
        graphics2D.setColor(new Color(255, 255, 255, 160));
        graphics2D.fill(new Float(46.0F, 138.0F, 14.0F, 8.0F));
        graphics2D.fill(new Float(122.0F, 122.0F, 14.0F, 8.0F));
    }

    @Environment(EnvType.CLIENT)
    private record HeartPaint(float cx, float cy, float r) {

        void fill(Graphics2D graphics2D, Shape shape) {
            graphics2D.setPaint(
                    new RadialGradientPaint(
                            new java.awt.geom.Point2D.Float(
                                    this.cx - this.r * 0.4F, this.cy - this.r * 0.5F),
                            this.r * 1.7F,
                            new float[] {0.0F, 0.45F, 1.0F},
                            new Color[] {
                                new Color(16752050), new Color(16726876), new Color(11014190)
                            }));
            graphics2D.fill(shape);
            graphics2D.setColor(new Color(7211552));
            graphics2D.setStroke(new BasicStroke(Math.max(1.5F, this.r * 0.05F)));
            graphics2D.draw(shape);
            Shape clip = graphics2D.getClip();
            graphics2D.clip(shape);
            graphics2D.setColor(new Color(255, 255, 255, 150));
            graphics2D.fill(
                    new Float(
                            this.cx - this.r * 0.85F,
                            this.cy - this.r * 0.75F,
                            this.r * 0.55F,
                            this.r * 0.35F));
            graphics2D.setClip(clip);
        }
    }

    @Environment(EnvType.CLIENT)
    record Match(SkyEmoji emoji, int start, int end) {}
}
