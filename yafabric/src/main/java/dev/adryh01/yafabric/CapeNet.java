package dev.adryh01.yafabric;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Who owns the free YA Cape? Asked from the YA backend (GET /v1/players/<uuid>) in the background. */
public final class CapeNet {

    private static final String BASE = "https://yaprojects.it";
    private static final Map<String, Boolean> CAPES = new ConcurrentHashMap<String, Boolean>();
    private static final Map<String, Long> FETCHED = new ConcurrentHashMap<String, Long>();
    private static final Set<String> PENDING = java.util.Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private CapeNet() {
    }

    public static boolean hasCape(UUID id) {
        if (id == null) return false;
        final String key = id.toString().replace("-", "");
        Long at = FETCHED.get(key);
        Boolean have = CAPES.get(key);
        long ttl = have != null && have ? 30000L : 8000L;
        if ((at == null || System.currentTimeMillis() - at > ttl) && PENDING.size() < 8 && PENDING.add(key)) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String r = get(BASE + "/v1/players/" + key).replace(" ", "");
                        CAPES.put(key, r.contains("\"cape\":\"ya_cape\""));
                    } catch (Exception e) {
                        // keep the old answer
                    } finally {
                        FETCHED.put(key, System.currentTimeMillis());
                        PENDING.remove(key);
                    }
                }
            }, "YA-Cape-Lookup");
            t.setDaemon(true);
            t.start();
        }
        return have != null && have;
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(7000);
        c.setRequestProperty("User-Agent", "YA-CLIENT");
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
