package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Discord Rich Presence written from scratch: talks to the Discord desktop app through its local IPC pipe
 * (Windows named pipe). Shows what you are playing; nothing is sent anywhere else.
 */
final class DiscordRpc extends Module {

    private static final String CLIENT_ID = "1562827651100581970";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    final Setting showServer = Setting.bool("show_server", "Show server address", false);
    final Setting showTime = Setting.bool("show_time", "Show elapsed time", true);

    private volatile Thread thread;
    private volatile boolean running;
    private final long start = System.currentTimeMillis() / 1000L;

    DiscordRpc() {
        super("discord_rpc", "Discord RPC", "Shows YA MODS on your Discord profile.", "Utility");
        settings.add(showServer);
        settings.add(showTime);
    }

    @Override
    public void onEnable() {
        if (running) return;
        running = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        }, "YA-DiscordRPC");
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    @Override
    public void onDisable() {
        running = false;
        Thread t = thread;
        if (t != null) t.interrupt();
    }

    private void loop() {
        while (running) {
            RandomAccessFile pipe = connect();
            if (pipe == null) {
                sleep(15000L);
                continue;
            }
            try {
                send(pipe, 0, "{\"v\":1,\"client_id\":\"" + CLIENT_ID + "\"}");
                read(pipe);
                while (running) {
                    send(pipe, 1, activity());
                    read(pipe);
                    sleep(15000L);
                }
                send(pipe, 1, clearActivity());
            } catch (IOException ignored) {
                // Discord was closed: try again later
            } finally {
                try {
                    pipe.close();
                } catch (IOException ignored) {
                }
            }
            if (running) sleep(15000L);
        }
    }

    private RandomAccessFile connect() {
        for (int i = 0; i < 10; i++) {
            try {
                return new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 32) b.append(' ');
            else b.append(c);
        }
        return b.toString();
    }

    private String pid() {
        String n = ManagementFactory.getRuntimeMXBean().getName();
        int at = n.indexOf('@');
        return at > 0 ? n.substring(0, at) : "0";
    }

    private String activity() {
        Minecraft mc = Minecraft.getMinecraft();
        String state;
        if (mc.theWorld == null) {
            state = "In menus";
        } else if (mc.isSingleplayer()) {
            state = "Singleplayer";
        } else {
            ServerData sd = mc.getCurrentServerData();
            state = showServer.b && sd != null ? "Playing on " + sd.serverIP : "Playing multiplayer";
        }
        StringBuilder a = new StringBuilder();
        a.append("{\"details\":\"Minecraft 1.8.9 with YA MODS\",\"state\":\"").append(esc(state)).append('"');
        if (showTime.b) a.append(",\"timestamps\":{\"start\":").append(start).append('}');
        a.append('}');
        return "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + pid() + ",\"activity\":" + a + "},\"nonce\":\""
                + System.nanoTime() + "\"}";
    }

    private String clearActivity() {
        return "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + pid() + "},\"nonce\":\"" + System.nanoTime() + "\"}";
    }

    private static void send(RandomAccessFile pipe, int op, String json) throws IOException {
        byte[] data = json.getBytes(UTF8);
        ByteBuffer b = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(op).putInt(data.length).put(data);
        pipe.write(b.array());
    }

    private static void read(RandomAccessFile pipe) throws IOException {
        byte[] head = new byte[8];
        pipe.readFully(head);
        int len = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        if (len < 0 || len > 65536) throw new IOException("bad frame");
        byte[] body = new byte[len];
        pipe.readFully(body);
    }
}
