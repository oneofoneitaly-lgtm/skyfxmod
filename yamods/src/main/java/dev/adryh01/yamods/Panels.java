package dev.adryh01.yamods;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Scoreboard, chat and tab list: movable / resizable panels with an optional background. */
final class Panels {

    private Panels() {
    }

    private static void blend() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
    }

    // ------------------------------------------------------------ Scoreboard

    static class Board extends Module {
        final Setting numbers = Setting.bool("numbers", "Show red numbers", true);
        final Setting bg = Setting.bool("bg", "Background", true);
        final Setting bgOpacity = Setting.slider("bgop", "Background opacity (%)", 40.0F, 5.0F, 100.0F, 5.0F);
        private ScoreObjective saved;

        Board() {
            super("scoreboard", "Scoreboard", "Move, resize and remove the background of the sidebar scoreboard.", "Utility");
            settings.add(numbers);
            settings.add(bg);
            settings.add(bgOpacity);
            asHud(1.0F, 0.35F, 0);
            moved = true;
            showInDebug = true;
            shadow.b = false;
            // this panel has its own background / colour handling
            settings.remove(background);
            settings.remove(color);
            background.b = false;
        }

        @Override
        public void onTick() {
            if (mc.theWorld == null) return;
            Scoreboard sb = mc.theWorld.getScoreboard();
            ScoreObjective o = sb.getObjectiveInDisplaySlot(1);
            if (o != null) {
                saved = o;
                sb.setObjectiveInDisplaySlot(1, null); // the vanilla scoreboard stays hidden, we draw our own
            } else if (saved != null && (saved.getScoreboard() != sb || sb.getObjective(saved.getName()) != saved)) {
                saved = null;
            }
        }

        @Override
        public void onDisable() {
            if (saved != null && mc.theWorld != null && saved.getScoreboard() == mc.theWorld.getScoreboard()) {
                mc.theWorld.getScoreboard().setObjectiveInDisplaySlot(1, saved);
            }
            saved = null;
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            final FontRenderer fr = Hud.fr();
            final ScoreObjective o = saved;
            final List<String> names = new ArrayList<String>();
            final List<String> pts = new ArrayList<String>();
            String t;
            if (o != null) {
                t = o.getDisplayName();
                Scoreboard sb = o.getScoreboard();
                List<Score> list = new ArrayList<Score>();
                for (Score s : sb.getSortedScores(o)) {
                    if (s.getPlayerName() != null && !s.getPlayerName().startsWith("#")) list.add(s);
                }
                if (list.size() > 15) list = list.subList(list.size() - 15, list.size());
                for (int i = list.size() - 1; i >= 0; i--) { // highest score on top
                    Score s = list.get(i);
                    ScorePlayerTeam team = sb.getPlayersTeam(s.getPlayerName());
                    names.add(ScorePlayerTeam.formatPlayerName(team, s.getPlayerName()));
                    pts.add(EnumChatFormatting.RED + "" + s.getScorePoints());
                }
            } else if (editing) {
                t = "YA MODS";
                names.add("Kills: 12");
                pts.add(EnumChatFormatting.RED + "12");
                names.add("Coins: 340");
                pts.add(EnumChatFormatting.RED + "5");
                names.add("play.example.com");
                pts.add(EnumChatFormatting.RED + "1");
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
                public void paint(Module m, FontRenderer f) {
                    int a = (int) (bgOpacity.v * 2.55F);
                    if (bg.b) {
                        Gui.drawRect(0, 0, bw, 9, Math.min(255, a + 24) << 24);
                        Gui.drawRect(0, 9, bw, 9 + rows * 9 + 1, a << 24);
                    }
                    boolean sh2 = m.shadow.b;
                    f.drawString(title, bw / 2 - f.getStringWidth(title) / 2, 1, 0xFFFFFF, sh2);
                    for (int i = 0; i < rows; i++) {
                        int y = 10 + i * 9;
                        f.drawString(names.get(i), 2, y, 0xFFFFFF, sh2);
                        if (numbers.b) f.drawString(pts.get(i), bw - 2 - f.getStringWidth(pts.get(i)), y, 0xFFFFFF, sh2);
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------ Chat

    /** Draws the chat itself so it can be moved, resized and shown without background. */
    static class ChatBox extends Module {
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
            showInDebug = true;
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
                for (Field f : GuiNewChat.class.getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        lists++;
                        if (lists == 3) { // mc, sentMessages, chatLines, drawnChatLines
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
            if (!ready()) return;
            final FontRenderer fr = Hud.fr();
            GuiNewChat gui = mc.ingameGUI.getChatGUI();
            if (!editing && mc.gameSettings.chatVisibility == EntityPlayer.EnumChatVisibility.HIDDEN) return;
            final List<String> texts = new ArrayList<String>();
            final List<Integer> alphas = new ArrayList<Integer>();
            try {
                List<ChatLine> all = (List<ChatLine>) drawnField.get(gui);
                int scrollPos = scrollField.getInt(gui);
                boolean open = gui.getChatOpen() || editing;
                float opacity = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
                int upd = mc.ingameGUI.getUpdateCounter();
                int max = gui.getLineCount();
                for (int i = 0; i + scrollPos < all.size() && i < max; i++) {
                    ChatLine cl = all.get(i + scrollPos);
                    if (cl == null) continue;
                    int age = upd - cl.getUpdatedCounter();
                    if (age >= 200 && !open) continue;
                    double d = 1.0D - age / 200.0D;
                    d = Math.max(0.0D, Math.min(1.0D, d * 10.0D));
                    int a = (int) (255.0D * d * d);
                    if (open || !fade.b) a = 255;
                    a = (int) (a * opacity);
                    if (a > 3) {
                        texts.add(cl.getChatComponent().getFormattedText());
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
            int w = (int) (gui.getChatWidth() / Math.max(0.1F, gui.getChatScale())) + 4;
            final int n = texts.size();
            final int bw = w;
            Hud.box(this, sw, sh, w, n * 9, new Hud.Painter() {
                @Override
                public void paint(Module m, FontRenderer f) {
                    for (int i = 0; i < n; i++) {
                        int y = (n - 1 - i) * 9; // newest message at the bottom
                        int a = alphas.get(i);
                        if (bg.b) {
                            int ba = Math.min(255, (int) (a / 2 * (bgOpacity.v / 50.0F)));
                            Gui.drawRect(0, y, bw, y + 9, ba << 24);
                        }
                        blend();
                        f.drawString(texts.get(i), 2, y + 1, 0xFFFFFF + (a << 24), m.shadow.b);
                        GlStateManager.disableAlpha();
                        GlStateManager.disableBlend();
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------ Tab list

    /** Own tab list: scale, optional background, heads and ping as bars or number. */
    static class TabList extends Module {
        final Setting scale = Setting.slider("scale", "Scale", 1.0F, 0.6F, 1.6F, 0.1F);
        final Setting bg = Setting.bool("bg", "Background", true);
        final Setting heads = Setting.bool("heads", "Show heads", true);
        final Setting ping = Setting.choice("ping", "Ping", 0, "Bars", "Number", "Off");
        final Setting rows = Setting.slider("rows", "Rows per column", 20.0F, 8.0F, 30.0F, 1.0F);

        TabList() {
            super("tablist", "Tab List", "A cleaner tab list: scale, background, heads and ping style.", "Utility");
            settings.add(scale);
            settings.add(bg);
            settings.add(heads);
            settings.add(ping);
            settings.add(rows);
        }

        @Override
        public void renderOverlay(int sw, int sh) {
            if (!mc.gameSettings.keyBindPlayerList.isKeyDown() || mc.getNetHandler() == null) return;
            Collection<NetworkPlayerInfo> all = mc.getNetHandler().getPlayerInfoMap();
            Scoreboard sb = mc.theWorld.getScoreboard();
            ScoreObjective obj = sb.getObjectiveInDisplaySlot(0);
            if (mc.isIntegratedServerRunning() && all.size() <= 1 && obj == null) return;

            List<NetworkPlayerInfo> list = new ArrayList<NetworkPlayerInfo>(all);
            Collections.sort(list, new Comparator<NetworkPlayerInfo>() {
                @Override
                public int compare(NetworkPlayerInfo a, NetworkPlayerInfo b) {
                    String ta = a.getPlayerTeam() == null ? "" : a.getPlayerTeam().getRegisteredName();
                    String tb = b.getPlayerTeam() == null ? "" : b.getPlayerTeam().getRegisteredName();
                    int c = ta.compareTo(tb);
                    return c != 0 ? c : a.getGameProfile().getName().compareToIgnoreCase(b.getGameProfile().getName());
                }
            });
            if (list.size() > 80) list = list.subList(0, 80);
            int n = list.size();
            if (n == 0) return;

            FontRenderer fr = mc.fontRendererObj;
            int perCol = Math.min(n, (int) rows.v);
            int cols = (n + perCol - 1) / perCol;
            int colW = 0;
            String[] names = new String[n];
            for (int i = 0; i < n; i++) {
                names[i] = mc.ingameGUI.getTabList().getPlayerName(list.get(i));
                int w = fr.getStringWidth(names[i]);
                if (obj != null) w += 4 + fr.getStringWidth(String.valueOf(sb.getValueFromObjective(list.get(i).getGameProfile().getName(), obj).getScorePoints()));
                colW = Math.max(colW, w);
            }
            colW += (heads.b ? 10 : 2) + (ping.idx == 0 ? 14 : (ping.idx == 1 ? fr.getStringWidth("999") + 4 : 2));
            colW = Math.min(colW, (int) ((sw / scale.v - 20) / cols));
            int total = cols * colW + (cols - 1);

            GlStateManager.pushMatrix();
            GlStateManager.translate(sw / 2.0F, 10.0F, 0.0F);
            GlStateManager.scale(scale.v, scale.v, 1.0F);
            GlStateManager.translate(-total / 2.0F, 0.0F, 0.0F);
            if (bg.b) Gui.drawRect(-2, -2, total + 2, perCol * 9 + 1, 0x80000000);
            for (int i = 0; i < n; i++) {
                NetworkPlayerInfo info = list.get(i);
                int x = (i / perCol) * (colW + 1);
                int y = (i % perCol) * 9;
                if (bg.b) Gui.drawRect(x, y, x + colW, y + 8, 0x20FFFFFF);
                int tx = x + 1;
                if (heads.b) {
                    mc.getTextureManager().bindTexture(info.getLocationSkin());
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    Gui.drawScaledCustomSizeModalRect(x + 1, y, 8.0F, 8.0F, 8, 8, 8, 8, 64.0F, 64.0F);
                    EntityPlayer ep = mc.theWorld.getPlayerEntityByUUID(info.getGameProfile().getId());
                    if (ep != null && ep.isWearing(EnumPlayerModelParts.HAT)) {
                        Gui.drawScaledCustomSizeModalRect(x + 1, y, 40.0F, 8.0F, 8, 8, 8, 8, 64.0F, 64.0F);
                    }
                    tx += 9;
                }
                blend();
                fr.drawStringWithShadow(fr.trimStringToWidth(names[i], colW - (tx - x) - 2), tx, y, 0xFFFFFF);
                int right = x + colW - 2;
                if (obj != null) {
                    String sc = String.valueOf(sb.getValueFromObjective(info.getGameProfile().getName(), obj).getScorePoints());
                    fr.drawStringWithShadow(sc, right - fr.getStringWidth(sc) - (ping.idx == 0 ? 12 : (ping.idx == 1 ? fr.getStringWidth("999") + 3 : 0)), y, 0xFFFF55);
                }
                int ms = info.getResponseTime();
                if (ping.idx == 1) {
                    String p = String.valueOf(Math.max(0, ms));
                    int col = ms < 0 ? 0xAAAAAA : (ms < 150 ? 0x55FF55 : (ms < 300 ? 0xFFFF55 : 0xFF5555));
                    fr.drawStringWithShadow(p, right - fr.getStringWidth(p), y, col);
                } else if (ping.idx == 0) {
                    int level = ms < 0 ? 0 : (ms < 150 ? 5 : (ms < 300 ? 4 : (ms < 600 ? 3 : (ms < 1000 ? 2 : 1))));
                    int col = level >= 4 ? 0xFF55FF55 : (level == 3 ? 0xFFFFFF55 : 0xFFFF5555);
                    for (int b = 0; b < 5; b++) {
                        int bx = right - 11 + b * 2;
                        Gui.drawRect(bx, y + 7 - (b + 1), bx + 1, y + 7, b < level ? col : 0xFF505050);
                    }
                }
                GlStateManager.disableBlend();
            }
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
