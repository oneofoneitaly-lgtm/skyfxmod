package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who else uses YA CLIENT / YA MODS? Same way as YA CLIENT: the YA backend (api.base, default
 * https://yaprojects.it) keeps a "running the client" flag per player. We log in like YA CLIENT does
 * (shared account secret in ~/.yaclient/config.properties), refresh our own flag every few minutes
 * and ask the public endpoint GET /v1/players/<uuid> about the other players.
 */
final class YaNet {

    private static final String DEFAULT_BASE = "https://yaprojects.it";
    private static final Map<String, Boolean> FLAGS = new ConcurrentHashMap<String, Boolean>();
    private static final Map<String, Long> FETCHED = new ConcurrentHashMap<String, Long>();
    private static final Set<String> PENDING = java.util.Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static volatile String token;
    private static volatile long tokenExpiry, lastBeat;
    private static volatile boolean beating;

    private YaNet() {
    }

    // ------------------------------------------------------------ public

    private static final java.util.Map<String, Boolean> CAPES = new java.util.concurrent.ConcurrentHashMap<String, Boolean>();

    /** True when the player owns the free YA Cape (claimed on the website). Works for yourself too. */
    static boolean hasCape(UUID id) {
        if (id == null) return false;
        String key = id.toString().replace("-", "");
        refresh(key);
        Boolean c = CAPES.get(key);
        return c != null && c;
    }

    static boolean uses(UUID id) {
        Minecraft mc = Minecraft.getMinecraft();
        if (id == null) return false;
        if (mc.thePlayer != null && id.equals(mc.thePlayer.getUniqueID())) return true;
        final String key = id.toString().replace("-", "");
        refresh(key);
        Boolean flag = FLAGS.get(key);
        return flag != null && flag;
    }

    /** Looks the player up in the background (at most every few seconds). */
    private static void refresh(final String key) {
        Boolean flag = FLAGS.get(key);
        Long at = FETCHED.get(key);
        long ttl = flag != null && flag ? 60000L : 300000L; // few lookups: no stutter in crowded lobbies
        if ((at == null || System.currentTimeMillis() - at > ttl) && PENDING.size() < 16 && PENDING.add(key)) {
            LOOKUP.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String r = http("GET", base() + "/v1/players/" + key, null);
                        String rr = r.replace(" ", "");
                        FLAGS.put(key, rr.contains("\"client\":\"1\""));
                        CAPES.put(key, rr.contains("\"cape\":\"ya_cape\""));
                    } catch (Exception e) {
                        FLAGS.put(key, Boolean.FALSE);
                    } finally {
                        FETCHED.put(key, System.currentTimeMillis());
                        PENDING.remove(key);
                    }
                }
            });
        }
    }

    /** One background thread does every lookup, instead of a new thread (and TLS handshake) per player. */
    private static final java.util.concurrent.ExecutorService LOOKUP = java.util.concurrent.Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "YA-Net-Lookup");
            t.setDaemon(true);
            return t;
        }
    });

    /** Call every client tick: keeps our own "I run the client" flag alive while we are in a world. */
    static void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || beating) return;
        Module n = Mods.byId("nicktag");
        if (n != null && !((NickTag) n).announce.b) return;
        if (System.currentTimeMillis() - lastBeat < 9L * 60L * 1000L) return;
        beating = true;
        lastBeat = System.currentTimeMillis();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String tok = login();
                    if (tok != null) http("GET", base() + "/v1/me?token=" + enc(tok), null);
                } catch (Exception e) {
                    token = null;
                    lastBeat = System.currentTimeMillis() - 8L * 60L * 1000L; // retry in about a minute
                } finally {
                    beating = false;
                }
            }
        }, "YA-Net-Beat");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------ internals

    private static File cfgFile() {
        return new File(System.getProperty("user.home"), ".yaclient" + File.separator + "config.properties");
    }

    private static Properties readCfg() {
        Properties p = new Properties();
        try {
            File f = cfgFile();
            if (f.exists()) {
                Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
                try {
                    p.load(r);
                } finally {
                    r.close();
                }
            }
        } catch (Exception ignored) {
        }
        return p;
    }

    private static String base() {
        String b = readCfg().getProperty("api.base", DEFAULT_BASE).trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b.length() == 0 ? DEFAULT_BASE : b;
    }

    /** The account secret is shared with YA CLIENT, so both can log in as the same player. */
    private static synchronized String secret() throws Exception {
        Properties p = readCfg();
        String s = p.getProperty("api.secret", "");
        if (s.matches("[0-9a-f]{32}")) return s;
        byte[] rb = new byte[16];
        new SecureRandom().nextBytes(rb);
        StringBuilder sb = new StringBuilder();
        for (byte x : rb) sb.append(String.format("%02x", x));
        p.setProperty("api.secret", sb.toString());
        File f = cfgFile();
        f.getParentFile().mkdirs();
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            p.store(w, "YA CLIENT");
        } finally {
            w.close();
        }
        return sb.toString();
    }

    private static synchronized String login() throws Exception {
        if (token != null && System.currentTimeMillis() < tokenExpiry) return token;
        Session s = Minecraft.getMinecraft().getSession();
        String name = s.getUsername(), uuid = String.valueOf(s.getPlayerID()).replace("-", "").toLowerCase();
        if (name == null || uuid.length() != 32) return null;
        String tok = s.getToken();
        boolean premium = tok != null && tok.length() > 20;
        String r = http("POST", base() + "/v1/auth", "username=" + enc(name) + "&uuid=" + enc(uuid) + "&secret=" + enc(secret())
            + (premium ? "" : "&offline=1"));
        int i = r.indexOf("\"token\"");
        if (i < 0) return null;
        int a = r.indexOf('"', r.indexOf(':', i) + 1) + 1, b = r.indexOf('"', a);
        token = r.substring(a, b);
        tokenExpiry = System.currentTimeMillis() + 50L * 60L * 1000L;
        return token;
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }

    private static String http(String method, String url, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(7000);
        c.setRequestMethod(method);
        c.setRequestProperty("User-Agent", "YA-CLIENT");
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStream o = c.getOutputStream();
            o.write(body.getBytes("UTF-8"));
            o.close();
        }
        if (c.getResponseCode() >= 400) throw new Exception("HTTP " + c.getResponseCode());
        InputStream in = c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), "UTF-8");
    }
}
