package dev.adryh01.yafabric.mods;

import java.util.ArrayList;
import java.util.List;

/** A text HUD module: the text comes from a small function, everything else (position, colour...) is shared. */
final class Info extends Module {

    interface Text {
        String get(Info m);
    }

    private final Text text;
    private final String sample;

    Info(String id, String name, String desc, String cat, String sample, boolean warn, Text text, Setting... extra) {
        super(id, name, desc, cat);
        this.text = text;
        this.sample = sample;
        for (Setting s : extra) settings.add(s);
        asHud(0.01F, 0.02F, warn ? 1 : 0);
    }

    float v(String key) {
        return setting(key).v;
    }

    boolean on(String key) {
        return setting(key).b;
    }

    @Override
    public void renderHud(int sw, int sh, boolean editing) {
        if (mc.player == null || mc.world == null) return;
        String t;
        try {
            t = text.get(this);
        } catch (RuntimeException e) {
            t = "";
        }
        if ((t == null || t.length() == 0) && editing) t = sample;
        if (t == null || t.length() == 0) return;
        List<String> lines = new ArrayList<String>();
        for (String l : t.split("\n")) lines.add(l);
        Hud.lines(this, sw, sh, lines);
    }
}
