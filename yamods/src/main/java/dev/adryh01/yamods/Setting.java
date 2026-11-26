package dev.adryh01.yamods;

import java.awt.Color;
import java.util.Locale;

/** One option of a module: on/off, slider, choice list or colour. */
public class Setting {

    public static final int BOOL = 0, SLIDER = 1, CHOICE = 2, COLOR = 3, TEXT = 4;

    public static final int[] PALETTE = {0xFFFFFF, 0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5599FF, 0xFF77FF, 0xAAAAAA};
    public static final String[] PALETTE_NAMES = {"White", "Red", "Orange", "Yellow", "Green", "Aqua", "Blue", "Pink", "Grey", "Rainbow"};

    public final String key, label;
    public final int type;
    public boolean b;
    public float v, min, max, step = 1.0F;
    public int idx;
    public String[] opts;
    public String s = "";
    public int maxLen = 16;

    private Setting(String key, String label, int type) {
        this.key = key;
        this.label = label;
        this.type = type;
    }

    public static Setting bool(String key, String label, boolean def) {
        Setting s = new Setting(key, label, BOOL);
        s.b = def;
        return s;
    }

    public static Setting slider(String key, String label, float def, float min, float max, float step) {
        Setting s = new Setting(key, label, SLIDER);
        s.v = def;
        s.min = min;
        s.max = max;
        s.step = step;
        return s;
    }

    public static Setting choice(String key, String label, int def, String... opts) {
        Setting s = new Setting(key, label, CHOICE);
        s.idx = def;
        s.opts = opts;
        return s;
    }

    public static Setting text(String key, String label, String def, int maxLen) {
        Setting t = new Setting(key, label, TEXT);
        t.s = def;
        t.maxLen = maxLen;
        return t;
    }

    public static Setting color(String key, String label, int def) {
        Setting s = new Setting(key, label, COLOR);
        s.idx = def;
        s.opts = PALETTE_NAMES;
        return s;
    }

    /** Colour as 0xRRGGBB (the last palette entry is an animated rainbow). */
    public int rgb() {
        if (idx >= PALETTE.length) {
            return Color.HSBtoRGB((System.currentTimeMillis() % 4000L) / 4000.0F, 0.65F, 1.0F) & 0xFFFFFF;
        }
        return PALETTE[idx];
    }

    public String display() {
        switch (type) {
            case BOOL:
                return b ? "On" : "Off";
            case SLIDER:
                return step >= 1.0F ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
            case TEXT:
                return s.length() == 0 ? "-" : s;
            default:
                return opts[idx];
        }
    }

    public void cycle(int dir) {
        if (type == BOOL) {
            b = !b;
        } else if (type == CHOICE || type == COLOR) {
            idx = (idx + dir + opts.length) % opts.length;
        }
    }

    public float fraction() {
        return (v - min) / (max - min);
    }

    public void setFraction(float f) {
        f = Math.max(0.0F, Math.min(1.0F, f));
        float raw = min + (max - min) * f;
        v = Math.max(min, Math.min(max, Math.round(raw / step) * step));
    }

    public String save() {
        switch (type) {
            case BOOL:
                return String.valueOf(b);
            case SLIDER:
                return String.valueOf(v);
            case TEXT:
                return s;
            default:
                return String.valueOf(idx);
        }
    }

    public void load(String str) {
        if (type == TEXT) {
            this.s = str.length() > maxLen ? str.substring(0, maxLen) : str;
            return;
        }
        String s = str;
        try {
            switch (type) {
                case BOOL:
                    b = Boolean.parseBoolean(s);
                    break;
                case SLIDER:
                    v = Math.max(min, Math.min(max, Float.parseFloat(s)));
                    break;
                default:
                    idx = Math.max(0, Math.min(opts.length - 1, Integer.parseInt(s)));
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
