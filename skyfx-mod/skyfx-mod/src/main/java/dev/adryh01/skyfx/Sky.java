package dev.adryh01.skyfx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** One sky definition (loaded from assets/skyfx/skies.json). */
public class Sky {

    public static final int STAR_STRIDE = 11;
    public static final List<Sky> ALL = new ArrayList<Sky>();

    /** Sun / moon / crescent / eclipse. */
    public static class Body {
        public String type = "sun";
        public int color = 0xFFFFFF;
        public float elev, az, size = 5.0F;
        public int rays;
        public float rayLen = 20.0F;
        public boolean spikes, halo;
    }

    /** Settings for the drifting cloud puffs. */
    public static class Puffs {
        public int count = 6;
        public int top = 0xFFFFFF, bottom = 0xCCCCCC, outline = -1;
        public float soft = 0.5F, elevMin = 8, elevMax = 40, size = 7, speed = 1, flat = 0.7F;
        public int alpha = 255;
    }

    public static class Cloud {
        public float az, el, size, speed;
        public float[] puffs; // ox, oy, r per puff (degrees)
    }

    public static class Particles {
        public String type = "sparkle";
        public int count = 30;
        public int color = 0xFFFFFF;
    }

    public String id = "vanilla";
    public String name = "Vanilla";
    public String category = "Realistic";
    public int zenith, mid, horizon, bottom, fog;
    public boolean hasAlt;
    public int altZenith, altMid, altHorizon, altBottom, altFog;
    public float cycle = 40.0F;
    public int bands;
    public int starCount;
    public float starBright = 1.0F;
    public boolean starPlus;
    public int milky = -1;
    public final List<Body> bodies = new ArrayList<Body>();
    public Puffs puffs;
    public boolean aurora;
    public int aurora1 = 0x3DFFA0, aurora2 = 0x8F5CFF;
    public boolean meteors, lightning;
    public Particles particles;
    public int grid = -1;
    public boolean clouds = true;
    public boolean vanilla;

    private float[] stars;
    private float[] milkyData;
    private float[] partData;
    private List<Cloud> cloudList;

    // ------------------------------------------------------------ loading

    public static void load() {
        ALL.clear();
        InputStream in = Sky.class.getResourceAsStream("/assets/skyfx/skies.json");
        try {
            if (in != null) {
                JsonArray arr = new JsonParser().parse(new InputStreamReader(in, "UTF-8")).getAsJsonArray();
                for (JsonElement el : arr) {
                    ALL.add(fromJson(el.getAsJsonObject()));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ignored) {
            }
        }
        if (ALL.isEmpty()) {
            Sky v = new Sky();
            v.vanilla = true;
            v.zenith = 0x78A7FF;
            v.mid = 0x9BC1FF;
            v.horizon = 0xC0D8FF;
            v.bottom = 0x7F9FD0;
            v.fog = 0xC0D8FF;
            ALL.add(v);
        }
    }

    private static Sky fromJson(JsonObject o) {
        Sky s = new Sky();
        s.id = str(o, "id", "sky");
        s.name = str(o, "name", s.id);
        s.category = str(o, "category", "Realistic");
        s.vanilla = o.has("vanilla") && o.get("vanilla").getAsBoolean();
        s.zenith = hex(str(o, "zenith", "#000000"));
        s.mid = hex(str(o, "mid", "#000000"));
        s.horizon = hex(str(o, "horizon", "#000000"));
        s.bottom = hex(str(o, "bottom", "#000000"));
        s.fog = hex(str(o, "fog", "#000000"));
        s.clouds = !o.has("clouds") || o.get("clouds").getAsBoolean();
        s.bands = o.has("bands") ? o.get("bands").getAsInt() : 0;
        s.meteors = o.has("meteors") && o.get("meteors").getAsBoolean();
        s.lightning = o.has("lightning") && o.get("lightning").getAsBoolean();
        if (o.has("milkyway")) s.milky = hex(o.get("milkyway").getAsString());
        if (o.has("grid")) s.grid = hex(o.get("grid").getAsString());
        if (o.has("alt")) {
            JsonObject a = o.getAsJsonObject("alt");
            s.hasAlt = true;
            s.altZenith = hex(str(a, "zenith", "#000000"));
            s.altMid = hex(str(a, "mid", "#000000"));
            s.altHorizon = hex(str(a, "horizon", "#000000"));
            s.altBottom = hex(str(a, "bottom", "#000000"));
            s.altFog = hex(str(a, "fog", "#000000"));
        }
        if (o.has("cycle")) s.cycle = o.get("cycle").getAsFloat();
        if (o.has("stars")) {
            JsonObject st = o.getAsJsonObject("stars");
            s.starCount = st.get("count").getAsInt();
            s.starBright = st.get("bright").getAsFloat();
            s.starPlus = "plus".equals(str(st, "shape", "dot"));
        }
        if (o.has("body")) s.bodies.add(body(o.getAsJsonObject("body")));
        if (o.has("bodies")) {
            for (JsonElement e : o.getAsJsonArray("bodies")) s.bodies.add(body(e.getAsJsonObject()));
        }
        if (o.has("aurora")) {
            JsonArray a = o.getAsJsonArray("aurora");
            s.aurora = true;
            s.aurora1 = hex(a.get(0).getAsString());
            s.aurora2 = hex(a.get(1).getAsString());
        }
        if (o.has("puffs")) {
            JsonObject p = o.getAsJsonObject("puffs");
            Puffs pf = new Puffs();
            pf.count = p.get("count").getAsInt();
            pf.top = hex(str(p, "top", "#FFFFFF"));
            pf.bottom = hex(str(p, "bottom", "#CCCCCC"));
            if (p.has("outline")) pf.outline = hex(p.get("outline").getAsString());
            pf.soft = p.get("soft").getAsFloat();
            JsonArray e = p.getAsJsonArray("elev");
            pf.elevMin = e.get(0).getAsFloat();
            pf.elevMax = e.get(1).getAsFloat();
            pf.size = p.get("size").getAsFloat();
            pf.speed = p.get("speed").getAsFloat();
            pf.flat = p.get("flat").getAsFloat();
            pf.alpha = p.get("alpha").getAsInt();
            s.puffs = pf;
        }
        if (o.has("particles")) {
            JsonObject p = o.getAsJsonObject("particles");
            Particles pa = new Particles();
            pa.type = str(p, "type", "sparkle");
            pa.count = p.get("count").getAsInt();
            pa.color = hex(str(p, "color", "#FFFFFF"));
            s.particles = pa;
        }
        return s;
    }

    private static Body body(JsonObject b) {
        Body r = new Body();
        r.type = str(b, "type", "sun");
        r.color = hex(str(b, "color", "#FFFFFF"));
        r.elev = b.get("elev").getAsFloat();
        r.az = b.get("az").getAsFloat();
        r.size = b.get("size").getAsFloat();
        if (b.has("rays")) r.rays = b.get("rays").getAsInt();
        if (b.has("rayLen")) r.rayLen = b.get("rayLen").getAsFloat();
        r.spikes = b.has("spikes") && b.get("spikes").getAsBoolean();
        r.halo = b.has("halo") && b.get("halo").getAsBoolean();
        return r;
    }

    private static String str(JsonObject o, String key, String def) {
        return o.has(key) ? o.get(key).getAsString() : def;
    }

    private static int hex(String s) {
        return Integer.parseInt(s.replace("#", ""), 16) & 0xFFFFFF;
    }

    public static Sky byId(String id) {
        for (Sky s : ALL) {
            if (s.id.equalsIgnoreCase(id)) return s;
        }
        return null;
    }

    /** Animation clock in seconds (0 when animations are off). */
    public static float timeSec() {
        return SkyFX.animations ? (Minecraft.getSystemTime() % 100000000L) / 1000.0F : 0.0F;
    }

    // ------------------------------------------------------------- colours

    /** 0..1 blend between the base palette and the alternate palette (colour cycling). */
    public float blend(float t) {
        if (!hasAlt || cycle <= 0.0F) return 0.0F;
        return 0.5F - 0.5F * (float) Math.cos(t * Math.PI * 2.0D / cycle);
    }

    public int fogAt(float k) {
        return hasAlt ? lerp(fog, altFog, k) : fog;
    }

    public int colorAt(float elev) {
        return colorAt(elev, 0.0F);
    }

    /** Colour (RGB) of the sky at the given elevation in degrees (-90 .. 90). */
    public int colorAt(float elev, float k) {
        int z = zenith, m = mid, h = horizon, b = bottom;
        if (hasAlt && k > 0.0F) {
            z = lerp(z, altZenith, k);
            m = lerp(m, altMid, k);
            h = lerp(h, altHorizon, k);
            b = lerp(b, altBottom, k);
        }
        if (bands > 0) {
            float step = 90.0F / bands;
            if (elev >= 0.0F) {
                elev = (float) ((Math.floor(elev / step) + 0.5D) * step);
            } else {
                elev = -(float) ((Math.floor(-elev / step) + 0.5D) * step);
            }
        }
        if (elev >= 0.0F) {
            float t = Math.min(1.0F, elev / 90.0F);
            if (t < 0.4F) return lerp(h, m, smooth(t / 0.4F));
            return lerp(m, z, smooth((t - 0.4F) / 0.6F));
        }
        return lerp(h, b, smooth(Math.min(1.0F, -elev / 60.0F)));
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }

    // ------------------------------------------------------- generated data

    /** Star quads: centre (3), right (3), up (3), phase, brightness (vectors pre-scaled to the radius). */
    public float[] stars() {
        if (stars != null) return stars;
        int n = Math.max(0, starCount);
        float[] d = new float[n * STAR_STRIDE];
        Random r = new Random(id.hashCode() * 31L + 7L);
        double radius = SkyRenderer.RADIUS;
        int i = 0;
        while (i < n) {
            double y = r.nextDouble() * 2.0D - 1.0D;
            if (y < -0.25D) continue;
            double phi = r.nextDouble() * Math.PI * 2.0D;
            double rr = Math.sqrt(1.0D - y * y);
            double x = rr * Math.cos(phi);
            double z = rr * Math.sin(phi);

            double ux0 = 0.0D, uy0 = 1.0D, uz0 = 0.0D;
            if (Math.abs(y) > 0.98D) {
                ux0 = 1.0D;
                uy0 = 0.0D;
            }
            double rx = y * uz0 - z * uy0;
            double ry = z * ux0 - x * uz0;
            double rz = x * uy0 - y * ux0;
            double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
            rx /= rl;
            ry /= rl;
            rz /= rl;
            double ux = ry * z - rz * y;
            double uy = rz * x - rx * z;
            double uz = rx * y - ry * x;

            double size = Math.toRadians(0.16D + Math.pow(r.nextDouble(), 3.0D) * 0.55D) * radius;
            if (starPlus) size *= 1.5D;
            int o = i * STAR_STRIDE;
            d[o] = (float) (x * radius);
            d[o + 1] = (float) (y * radius);
            d[o + 2] = (float) (z * radius);
            d[o + 3] = (float) (rx * size);
            d[o + 4] = (float) (ry * size);
            d[o + 5] = (float) (rz * size);
            d[o + 6] = (float) (ux * size);
            d[o + 7] = (float) (uy * size);
            d[o + 8] = (float) (uz * size);
            d[o + 9] = (float) (r.nextDouble() * Math.PI * 2.0D);
            d[o + 10] = 0.45F + r.nextFloat() * 0.55F;
            i++;
        }
        stars = d;
        return d;
    }

    /** Milky way blobs along a tilted great circle: unit dir (3), radius deg, alpha. */
    public float[] milky() {
        if (milkyData != null) return milkyData;
        Random r = new Random(id.hashCode() * 17L + 3L);
        double nx = 0.35D, ny = 0.55D, nz = 0.76D;
        double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= nl;
        ny /= nl;
        nz /= nl;
        // e1 = normalize(n x up)
        double e1x = ny * 0.0D - nz * 1.0D, e1y = nz * 0.0D - nx * 0.0D, e1z = nx * 1.0D - ny * 0.0D;
        double el = Math.sqrt(e1x * e1x + e1y * e1y + e1z * e1z);
        e1x /= el;
        e1y /= el;
        e1z /= el;
        double e2x = ny * e1z - nz * e1y, e2y = nz * e1x - nx * e1z, e2z = nx * e1y - ny * e1x;
        int n = 280;
        float[] d = new float[n * 5];
        for (int i = 0; i < n; i++) {
            double u = r.nextDouble() * Math.PI * 2.0D;
            double g = r.nextGaussian() * 0.1D;
            double x = e1x * Math.cos(u) + e2x * Math.sin(u) + nx * g;
            double y = e1y * Math.cos(u) + e2y * Math.sin(u) + ny * g;
            double z = e1z * Math.cos(u) + e2z * Math.sin(u) + nz * g;
            double l = Math.sqrt(x * x + y * y + z * z);
            d[i * 5] = (float) (x / l);
            d[i * 5 + 1] = (float) (y / l);
            d[i * 5 + 2] = (float) (z / l);
            d[i * 5 + 3] = 1.6F + r.nextFloat() * 3.2F;
            d[i * 5 + 4] = 5.0F + r.nextFloat() * 16.0F;
        }
        milkyData = d;
        return d;
    }

    /** Particles: az, elev, speed, phase, size (deg). */
    public float[] parts() {
        if (partData != null) return partData;
        int n = particles == null ? 0 : particles.count;
        Random r = new Random(id.hashCode() * 13L + 5L);
        float[] d = new float[n * 5];
        for (int i = 0; i < n; i++) {
            d[i * 5] = r.nextFloat() * 360.0F;
            d[i * 5 + 1] = 3.0F + r.nextFloat() * 55.0F;
            d[i * 5 + 2] = 0.5F + r.nextFloat();
            d[i * 5 + 3] = r.nextFloat() * 6.28F;
            d[i * 5 + 4] = 0.25F + r.nextFloat() * 0.35F;
        }
        partData = d;
        return d;
    }

    public List<Cloud> clouds() {
        if (cloudList != null) return cloudList;
        List<Cloud> list = new ArrayList<Cloud>();
        if (puffs != null) {
            Random r = new Random(id.hashCode() * 29L + 11L);
            for (int i = 0; i < puffs.count; i++) {
                Cloud c = new Cloud();
                float slot = 360.0F / puffs.count;
                c.az = i * slot + r.nextFloat() * slot * 0.6F;
                c.el = puffs.elevMin + r.nextFloat() * (puffs.elevMax - puffs.elevMin);
                c.size = puffs.size * (0.7F + r.nextFloat() * 0.6F);
                c.speed = puffs.speed * (0.7F + r.nextFloat() * 0.6F);
                int n = 5 + r.nextInt(3);
                c.puffs = new float[n * 3];
                for (int j = 0; j < n; j++) {
                    float ox = ((j / (float) (n - 1)) * 2.0F - 1.0F) * c.size * 1.5F + (r.nextFloat() - 0.5F) * c.size * 0.3F;
                    float rad = c.size * (0.5F + 0.5F * r.nextFloat()) * (1.0F - 0.45F * Math.abs(ox) / (c.size * 1.6F));
                    c.puffs[j * 3] = ox;
                    c.puffs[j * 3 + 1] = rad * 0.55F;
                    c.puffs[j * 3 + 2] = rad;
                }
                list.add(c);
            }
        }
        cloudList = list;
        return list;
    }
}
