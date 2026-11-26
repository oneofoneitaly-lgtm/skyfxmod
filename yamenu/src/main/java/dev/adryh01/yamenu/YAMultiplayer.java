package dev.adryh01.yamenu;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenAddServer;
import net.minecraft.client.gui.GuiScreenServerList;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.OldServerPinger;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class YAMultiplayer extends YAScreen {

    private static final int ROW = 46;
    private static final ResourceLocation UNKNOWN = new ResourceLocation("textures/misc/unknown_server.png");
    private final Map<String, ResourceLocation> icons = new HashMap<String, ResourceLocation>();
    private final Map<String, String> iconData = new HashMap<String, String>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, new java.util.concurrent.ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "YA Server Pinger");
            t.setDaemon(true);
            return t;
        }
    });

    private ServerList servers;
    private final OldServerPinger pinger = new OldServerPinger();
    private int sel = -1, scroll, editIndex = -2;
    private ServerData editCopy;
    private long lastClick;
    private boolean confirmDelete;
    private YButton join, edit, delete;

    YAMultiplayer(GuiScreen parent) {
        super(parent, "Multiplayer");
    }

    @Override
    public void initGui() {
        servers = new ServerList(mc);
        servers.loadServerList();
        for (int i = 0; i < servers.countServers(); i++) ping(servers.getServerData(i));
        buttonList.clear();
        int bw = 92, gap = 5, x = width / 2 - (bw * 5 + gap * 4) / 2, y = height - 38;
        buttonList.add(join = new YButton(1, x, y, bw, 22, "Join"));
        buttonList.add(new YButton(2, x + (bw + gap), y, bw, 22, "Direct connect"));
        buttonList.add(new YButton(3, x + (bw + gap) * 2, y, bw, 22, "Add server"));
        buttonList.add(edit = new YButton(4, x + (bw + gap) * 3, y, bw, 22, "Edit"));
        buttonList.add(delete = new YButton(5, x + (bw + gap) * 4, y, bw, 22, "Delete"));
        buttonList.add(new YButton(0, width - 64, 7, 54, 20, "Back"));
        update();
    }

    private void ping(final ServerData sd) {
        sd.serverMOTD = "Pinging...";
        sd.pingToServer = -1L;
        sd.playerList = null;
        POOL.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    pinger.ping(sd);
                } catch (Exception e) {
                    sd.pingToServer = -1L;
                    sd.serverMOTD = "Can't connect to server";
                }
            }
        });
    }

    private void update() {
        boolean ok = sel >= 0 && sel < servers.countServers();
        join.enabled = ok;
        edit.enabled = ok;
        delete.enabled = ok;
        delete.displayString = confirmDelete ? "Sure?" : "Delete";
    }

    private void connect() {
        if (sel >= 0 && sel < servers.countServers()) FMLClientHandler.instance().connectToServer(this, servers.getServerData(sel));
    }

    @Override
    public void updateScreen() {
        pinger.pingPendingNetworks();
    }

    @Override
    public void onGuiClosed() {
        pinger.clearPendingNetworks();
        for (ResourceLocation rl : icons.values()) mc.getTextureManager().deleteTexture(rl);
        icons.clear();
        iconData.clear();
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case 1:
                connect();
                break;
            case 2:
                mc.displayGuiScreen(new GuiScreenServerList(this, new ServerData("Minecraft Server", "", false)));
                break;
            case 3:
                editIndex = -1;
                editCopy = new ServerData("Minecraft Server", "", false);
                mc.displayGuiScreen(new GuiScreenAddServer(this, editCopy));
                break;
            case 4:
                ServerData o = servers.getServerData(sel);
                editIndex = sel;
                editCopy = new ServerData(o.serverName, o.serverIP, false);
                mc.displayGuiScreen(new GuiScreenAddServer(this, editCopy));
                break;
            case 5:
                if (!confirmDelete) {
                    confirmDelete = true;
                    update();
                } else {
                    servers.removeServerData(sel);
                    servers.saveServerList();
                    sel = -1;
                    confirmDelete = false;
                    update();
                }
                break;
            case 0:
                mc.displayGuiScreen(parent);
                break;
            default:
        }
    }

    /** Called by the add / edit server screen. */
    @Override
    public void confirmClicked(boolean result, int id) {
        if (result && editCopy != null) {
            if (editIndex == -1) {
                servers.addServerData(editCopy);
            } else if (editIndex >= 0 && editIndex < servers.countServers()) {
                ServerData d = servers.getServerData(editIndex);
                d.serverName = editCopy.serverName;
                d.serverIP = editCopy.serverIP;
            }
            servers.saveServerList();
        }
        editCopy = null;
        editIndex = -2;
        mc.displayGuiScreen(this);
    }

    /** The server's own picture (sent in the ping answer), or the grey default one. */
    private ResourceLocation icon(ServerData sd) {
        String data = sd.getBase64EncodedIconData();
        String key = sd.serverIP;
        if (data == null || data.length() == 0) return UNKNOWN;
        if (!data.equals(iconData.get(key))) {
            iconData.put(key, data);
            try {
                if (icons.containsKey(key)) mc.getTextureManager().deleteTexture(icons.remove(key));
                BufferedImage img = javax.imageio.ImageIO.read(new ByteArrayInputStream(java.util.Base64.getMimeDecoder().decode(data)));
                ResourceLocation rl = new ResourceLocation("yamenu", "servers/" + Integer.toHexString(key.hashCode()) + "/icon");
                mc.getTextureManager().loadTexture(rl, new DynamicTexture(img));
                icons.put(key, rl);
            } catch (Exception e) {
                icons.remove(key);
            }
        }
        ResourceLocation rl = icons.get(key);
        return rl == null ? UNKNOWN : rl;
    }

    private int listTop() {
        return 44;
    }

    private int listBottom() {
        return height - 48;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0 || my < listTop() || my > listBottom()) return;
        int cw = Math.min(width - 40, 460), x = width / 2 - cw / 2;
        if (mx < x || mx > x + cw) return;
        int i = (my - listTop() + scroll) / ROW;
        if (i >= 0 && i < servers.countServers()) {
            long now = System.currentTimeMillis();
            if (i == sel && now - lastClick < 400L) {
                connect();
                return;
            }
            sel = i;
            lastClick = now;
            confirmDelete = false;
            update();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scroll = Math.max(0, Math.min(Math.max(0, servers.countServers() * ROW - (listBottom() - listTop())), scroll - Integer.signum(d) * ROW));
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        background(true);
        int cw = Math.min(width - 40, 460), x = width / 2 - cw / 2, top = listTop(), bot = listBottom();
        int f = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, (height - bot) * f, width * f, (bot - top) * f);
        if (servers.countServers() == 0) {
            String s = "No servers yet. Click Add server.";
            fontRendererObj.drawString(s, width / 2 - fontRendererObj.getStringWidth(s) / 2, top + 20, UI.SOFT);
        }
        for (int i = 0; i < servers.countServers(); i++) {
            int y = top + i * ROW - scroll;
            if (y + ROW < top || y > bot) continue;
            ServerData sd = servers.getServerData(i);
            boolean over = mx >= x && mx <= x + cw && my >= Math.max(y, top) && my < Math.min(y + ROW - 4, bot);
            UI.box(x, y, x + cw, y + ROW - 4, 6, i == sel ? UI.ACCENT : UI.BORDER, over ? 0xFFFAFAFA : UI.CARD);
            mc.getTextureManager().bindTexture(icon(sd));
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.drawScaledCustomSizeModalRect(x + 8, y + 5, 0.0F, 0.0F, 64, 64, 32, 32, 64.0F, 64.0F);
            int tx = x + 50;
            fontRendererObj.drawString(sd.serverName, tx, y + 6, UI.DARK);
            String motd = sd.serverMOTD == null ? "" : net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(sd.serverMOTD.split("\n")[0]);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(motd == null ? "" : motd, cw - 170), tx, y + 19, UI.MID);
            fontRendererObj.drawString(sd.serverIP, tx, y + 30, UI.SOFT);
            if (sd.pingToServer >= 0L) {
                String p = sd.pingToServer + " ms";
                int pc = sd.pingToServer < 150L ? 0xFF2E8B3A : sd.pingToServer < 400L ? 0xFFC98A00 : UI.RED;
                fontRendererObj.drawString(p, x + cw - 10 - fontRendererObj.getStringWidth(p), y + 6, pc);
                String pop = sd.populationInfo == null ? "" : net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(sd.populationInfo);
                fontRendererObj.drawString(pop == null ? "" : pop, x + cw - 10 - fontRendererObj.getStringWidth(pop == null ? "" : pop), y + 19, UI.MID);
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        super.drawScreen(mx, my, pt);
    }
}
