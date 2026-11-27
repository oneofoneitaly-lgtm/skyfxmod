package dev.adryh01.yafabric.mods;

import dev.adryh01.yafabric.YaNet;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Scoreboard, chat and tab list: movable / resizable panels with an optional background. */
public final class Panels {

    static final Board BOARD = new Board();
    static final ChatBox CHAT = new ChatBox();
    static final TabList TAB = new TabList();

    private Panels() {
    }

    static void register(List<Module> all) {
        all.add(BOARD);
        all.add(CHAT);
        all.add(TAB);
    }

    // ---- hooks used by the mixins

    /** True when our scoreboard replaces the vanilla sidebar. */
    public static boolean hideVanillaBoard() {
        return BOARD.enabled && BOARD.mc.world != null && BOARD.mc.world.getScoreboard().getObjectiveForSlot(1) != null;
    }

    public static boolean hideVanillaChat() {
        return CHAT.enabled && CHAT.ready();
    }

    public static boolean hideVanillaTab() {
        return TAB.enabled;
    }

    private static void blend() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ------------------------------------------------------------ Scoreboard

    static final class Board extends Module {
        final Setting numbers = Setting.bool("numbers", "Show red numbers", true);
        final Setting bg = Setting.bool("bg", "Background", true);
        final Setting bgOpacity = Setting.slider("bgop", "Background opacity (%)", 40.0F, 5.0F, 100.0F, 5.0F);

        Board() {
            super("scoreboard", "Scoreboard", "Move, resize and remove the background of the sidebar scoreboard.", "Utility");
            settings.add(numbers);
            settings.add(bg);
            settings.add(bgOpacity);
            asHud(1.0F, 0.35F, 0);
            moved = true;
            shadow.b = false;
            // this panel has its own background / colour handling
            settings.remove(background);
            settings.remove(color);
            background.b = false;
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            final TextRenderer fr = Hud.fr();
            final List<String> names = new ArrayList<String>();
            final List<String> pts = new ArrayList<String>();
            String t;
            ScoreboardObjective o = mc.world != null ? mc.world.getScoreboard().getObjectiveForSlot(1) : null;
            if (o != null) {
                t = o.getDisplayName();
                Scoreboard sb = o.getScoreboard();
                List<ScoreboardPlayerScore> list = new ArrayList<ScoreboardPlayerScore>();
                for (ScoreboardPlayerScore s : sb.getAllPlayerScores(o)) {
                    if (s.getPlayerName() != null && !s.getPlayerName().startsWith("#")) list.add(s);
                }
                Collections.sort(list, new Comparator<ScoreboardPlayerScore>() {
                    @Override
                    public int compare(ScoreboardPlayerScore a, ScoreboardPlayerScore b) {
                        if (a.getScore() != b.getScore()) return a.getScore() > b.getScore() ? 1 : -1;
                        return b.getPlayerName().compareToIgnoreCase(a.getPlayerName());
                    }
                });
                if (list.size() > 15) list = list.subList(list.size() - 15, list.size());
                for (int i = list.size() - 1; i >= 0; i--) { // highest score on top
                    ScoreboardPlayerScore s = list.get(i);
                    Team team = sb.getPlayerTeam(s.getPlayerName());
                    names.add(Team.decorateName(team, s.getPlayerName()));
                    pts.add("§c" + s.getScore());
                }
            } else if (editing) {
                t = "YA MODS";
                names.add("Kills: 12");
                pts.add("§c12");
                names.add("Coins: 340");
                pts.add("§c5");
                names.add("play.example.com");
                pts.add("§c1");
            } else {
                return;
            }
            final String title = t;
            int width = fr.getStringWidth(title);
            for (int i = 0; i < names.size(); i++) {
                int w = fr.getStringWidth(names.get(i)) + (numbers.b ? fr.getStringWidth(pts.get(i)) + 6 : 0);
                width = Math.max(width, w);
            }
            final int bw = width + 4;
            final int rows = names.size();
            Hud.box(this, sw, sh, bw, (rows + 1) * 9 + 1, new Hud.Painter() {
                @Override
                public void paint(Module m, TextRenderer f) {
                    int a = (int) (bgOpacity.v * 2.55F);
                    if (bg.b) {
                        DrawableHelper.fill(0, 0, bw, 9, Math.min(255, a + 24) << 24);
                        DrawableHelper.fill(0, 9, bw, 9 + rows * 9 + 1, a << 24);
                    }
                    boolean sh2 = m.shadow.b;
                    text(f, title, bw / 2 - f.getStringWidth(title) / 2, 1, sh2);
                    for (int i = 0; i < rows; i++) {
                        int y = 10 + i * 9;
                        text(f, names.get(i), 2, y, sh2);
                        if (numbers.b) text(f, pts.get(i), bw - 2 - f.getStringWidth(pts.get(i)), y, sh2);
                    }
                }
            });
        }
    }

    static void text(TextRenderer f, String s, int x, int y, boolean shadow) {
        if (shadow) f.drawWithShadow(s, x, y, 0xFFFFFFFF);
        else f.draw(s, x, y, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------ Chat

    /** Draws the chat itself so it can be moved, resized and shown without background. */
    static final class ChatBox extends Module {
        final Setting bg = Setting.bool("bg", "Background", true);
        final Setting bgOpacity = Setting.slider("bgop", "Background opacity (%)", 50.0F, 5.0F, 100.0F, 5.0F);
        final Setting fade = Setting.bool("fade", "Fade old messages", true);
        private Field drawnField, scrollField;
        private boolean tried, ok;

        ChatBox() {
            super("chatbox", "Chat", "Move and resize the chat and remove its background.", "Utility");
            settings.add(bg);
            settings.add(bgOpacity);
            settings.add(fade);
            asHud(0.0F, 0.87F, 0);
            moved = true;
            settings.remove(background);
            settings.remove(color);
            background.b = false;
        }

        /** Finds the private fields of the vanilla chat by their type (works with any mappings). */
        boolean ready() {
            if (tried) return ok;
            tried = true;
            try {
                int lists = 0;
                for (Field f : ChatHud.class.getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        lists++;
                        if (lists == 3) { // messageHistory, messages, visibleMessages
                            f.setAccessible(true);
                            drawnField = f;
                        }
                    } else if (f.getType() == int.class) {
                        f.setAccessible(true);
                        scrollField = f;
                    }
                }
                ok = drawnField != null && scrollField != null;
            } catch (RuntimeException e) {
                ok = false;
            }
            return ok;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            if (!ready() || mc.inGameHud == null) return;
            ChatHud gui = mc.inGameHud.getChatHud();
            if (!editing && mc.options.chatVisibilityType == PlayerEntity.ChatVisibilityType.HIDDEN) return;
            final List<String> texts = new ArrayList<String>();
            final List<Integer> alphas = new ArrayList<Integer>();
            try {
                List<ChatHudLine> all = (List<ChatHudLine>) drawnField.get(gui);
                int scrollPos = scrollField.getInt(gui);
                boolean open = gui.isChatFocused() || editing;
                float opacity = mc.options.chatOpacity * 0.9F + 0.1F;
                int upd = mc.inGameHud.getTicks();
                int max = gui.getVisibleLineCount();
                for (int i = 0; i + scrollPos < all.size() && i < max; i++) {
                    ChatHudLine cl = all.get(i + scrollPos);
                    if (cl == null) continue;
                    int age = upd - cl.getCreationTick();
                    if (age >= 200 && !open) continue;
                    double d = 1.0D - age / 200.0D;
                    d = Math.max(0.0D, Math.min(1.0D, d * 10.0D));
                    int a = (int) (255.0D * d * d);
                    if (open || !fade.b) a = 255;
                    a = (int) (a * opacity);
                    if (a > 3) {
                        texts.add(cl.getText().asFormattedString());
                        alphas.add(a);
                    }
                }
            } catch (IllegalAccessException e) {
                return;
            }
            if (texts.isEmpty() && editing) {
                texts.add("<Steve> Welcome to YA MODS");
                alphas.add(255);
                texts.add("<Alex> Drag this chat where you like");
                alphas.add(255);
            }
            if (texts.isEmpty()) return;
            int w = (int) (gui.getWidth() / Math.max(0.1F, gui.getChatScale())) + 4;
            final int n = texts.size();
            final int bw = w;
            Hud.box(this, sw, sh, w, n * 9, new Hud.Painter() {
                @Override
                public void paint(Module m, TextRenderer f) {
                    for (int i = 0; i < n; i++) {
                        int y = (n - 1 - i) * 9; // newest message at the bottom
                        int a = alphas.get(i);
                        if (bg.b) {
                            int ba = Math.min(255, (int) (a / 2 * (bgOpacity.v / 50.0F)));
                            DrawableHelper.fill(0, y, bw, y + 9, ba << 24);
                        }
                        int st = Gl.save();
                        blend();
                        f.draw(texts.get(i), 2, y + 1, 0xFFFFFF + (a << 24), m.shadow.b);
                        Gl.restore(st);
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------ Tab list

    /** Own tab list: scale, optional background, heads and ping as bars or number. */
    static final class TabList extends Module {
        final Setting scale = Setting.slider("scale", "Scale", 1.0F, 0.6F, 1.6F, 0.1F);
        final Setting bg = Setting.bool("bg", "Background", true);
        final Setting heads = Setting.bool("heads", "Show heads", true);
        final Setting ping = Setting.choice("ping", "Ping", 0, "Bars", "Number", "Off");
        final Setting rows = Setting.slider("rows", "Rows per column", 20.0F, 8.0F, 30.0F, 1.0F);
        final Setting logo = Setting.bool("logo", "YA logo next to YA users", true);

        TabList() {
            super("tablist", "Tab List", "A cleaner tab list: scale, background, heads and ping style.", "Utility");
            settings.add(scale);
            settings.add(bg);
            settings.add(heads);
            settings.add(ping);
            settings.add(rows);
            settings.add(logo);
        }

        @Override
        public void renderOverlay(int sw, int sh) {
            if (mc.world == null || mc.player == null || !mc.options.playerListKey.isPressed() || mc.getNetworkHandler() == null) return;
            Collection<PlayerListEntry> all = mc.getNetworkHandler().getPlayerList();
            Scoreboard sb = mc.world.getScoreboard();
            ScoreboardObjective obj = sb.getObjectiveForSlot(0);
            if (mc.isIntegratedServerRunning() && all.size() <= 1 && obj == null) return;

            List<PlayerListEntry> list = new ArrayList<PlayerListEntry>(all);
            Collections.sort(list, new Comparator<PlayerListEntry>() {
                @Override
                public int compare(PlayerListEntry a, PlayerListEntry b) {
                    String ta = a.getScoreboardTeam() == null ? "" : a.getScoreboardTeam().getName();
                    String tb = b.getScoreboardTeam() == null ? "" : b.getScoreboardTeam().getName();
                    int c = ta.compareTo(tb);
                    return c != 0 ? c : a.getProfile().getName().compareToIgnoreCase(b.getProfile().getName());
                }
            });
            if (list.size() > 80) list = list.subList(0, 80);
            int n = list.size();
            if (n == 0) return;

            TextRenderer fr = mc.textRenderer;
            int perCol = Math.min(n, (int) rows.v);
            int cols = (n + perCol - 1) / perCol;
            int colW = 0;
            String[] names = new String[n];
            String myNick = NickTag.myNick();
            for (int i = 0; i < n; i++) {
                names[i] = mc.inGameHud.getPlayerListWidget().getPlayerName(list.get(i));
                if (myNick != null && list.get(i).getProfile().getId().equals(mc.player.getUuid())) names[i] = myNick;
                int w = fr.getStringWidth(names[i]);
                if (obj != null) w += 4 + fr.getStringWidth(String.valueOf(sb.getPlayerScore(list.get(i).getProfile().getName(), obj).getScore()));
                colW = Math.max(colW, w);
            }
            colW += (logo.b ? Logo.width(8) + 2 : 0) + (heads.b ? 10 : 2) + (ping.idx == 0 ? 14 : (ping.idx == 1 ? fr.getStringWidth("999") + 4 : 2));
            colW = Math.min(colW, (int) ((sw / scale.v - 20) / cols));
            int total = cols * colW + (cols - 1);

            GL11.glPushMatrix();
            GL11.glTranslatef(sw / 2.0F, 10.0F, 0.0F);
            GL11.glScalef(scale.v, scale.v, 1.0F);
            GL11.glTranslatef(-total / 2.0F, 0.0F, 0.0F);
            if (bg.b) DrawableHelper.fill(-2, -2, total + 2, perCol * 9 + 1, 0x80000000);
            for (int i = 0; i < n; i++) {
                PlayerListEntry info = list.get(i);
                int x = (i / perCol) * (colW + 1);
                int y = (i % perCol) * 9;
                if (bg.b) DrawableHelper.fill(x, y, x + colW, y + 8, 0x20FFFFFF);
                int tx = x + 1;
                if (logo.b) {
                    if (YaNet.uses(info.getProfile().getId())) Logo.draw(tx, y, 8, 1.0F);
                    tx += Logo.width(8) + 2;
                }
                if (heads.b) {
                    int st = Gl.save();
                    mc.getTextureManager().bindTexture(info.getSkinTexture());
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    DrawableHelper.drawTexture(tx, y, 8.0F, 8.0F, 8, 8, 8, 8, 64.0F, 64.0F);
                    PlayerEntity ep = mc.world.getPlayerByUuid(info.getProfile().getId());
                    if (ep != null && ep.isPartVisible(PlayerModelPart.HAT)) {
                        DrawableHelper.drawTexture(tx, y, 40.0F, 8.0F, 8, 8, 8, 8, 64.0F, 64.0F);
                    }
                    Gl.restore(st);
                    tx += 9;
                }
                fr.drawWithShadow(fr.trimToWidth(names[i], colW - (tx - x) - 2), tx, y, 0xFFFFFF);
                int right = x + colW - 2;
                if (obj != null) {
                    String sc = String.valueOf(sb.getPlayerScore(info.getProfile().getName(), obj).getScore());
                    fr.drawWithShadow(sc, right - fr.getStringWidth(sc) - (ping.idx == 0 ? 12 : (ping.idx == 1 ? fr.getStringWidth("999") + 3 : 0)), y, 0xFFFF55);
                }
                int ms = info.getLatency();
                if (ping.idx == 1) {
                    String p = String.valueOf(Math.max(0, ms));
                    int col = ms < 0 ? 0xAAAAAA : (ms < 150 ? 0x55FF55 : (ms < 300 ? 0xFFFF55 : 0xFF5555));
                    fr.drawWithShadow(p, right - fr.getStringWidth(p), y, col);
                } else if (ping.idx == 0) {
                    int level = ms < 0 ? 0 : (ms < 150 ? 5 : (ms < 300 ? 4 : (ms < 600 ? 3 : (ms < 1000 ? 2 : 1))));
                    int col = level >= 4 ? 0xFF55FF55 : (level == 3 ? 0xFFFFFF55 : 0xFFFF5555);
                    for (int b = 0; b < 5; b++) {
                        int bx = right - 11 + b * 2;
                        DrawableHelper.fill(bx, y + 7 - (b + 1), bx + 1, y + 7, b < level ? col : 0xFF505050);
                    }
                }
            }
            GL11.glPopMatrix();
        }
    }
}
