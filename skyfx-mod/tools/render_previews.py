#!/usr/bin/env python3
"""Renders mock-ups of the SkyFX menu from assets/skyfx/skies.json.

Uses the same layers/order as Draw.preview in the mod (gradient/bands, milky way, stars, aurora, bodies with
rays + halo, cloud puffs, grid, meteors, particles, lightning, ground). The in-game previews are animated;
this script draws one frame of them.
"""
import json, math, os, random, sys, zlib
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..")
SKIES = json.load(open(os.path.join(ROOT, "src/main/resources/assets/skyfx/skies.json")))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "docs")
os.makedirs(OUT, exist_ok=True)

S = 3  # pixels per GUI pixel
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONTB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
CATS = ["All", "Anime", "Cartoon", "Realistic", "Fantasy", "Retro", "Void"]


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lerp(a, b, t):
    t = max(0, min(1, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def smooth(t):
    return t * t * (3 - 2 * t)


def seed(s):
    return zlib.crc32(s.encode())


class Sky:
    def __init__(self, d):
        self.d = d
        self.id, self.name, self.cat = d["id"], d["name"], d["category"]
        self.z, self.m, self.h, self.b = (hexrgb(d[k]) for k in ("zenith", "mid", "horizon", "bottom"))
        self.alt = None
        if "alt" in d:
            a = d["alt"]
            self.alt = tuple(hexrgb(a[k]) for k in ("zenith", "mid", "horizon", "bottom"))
        self.cycle = d.get("cycle", 40)
        self.bands = d.get("bands", 0)
        self.stars = d.get("stars", {"count": 0, "bright": 1})
        self.plus = self.stars.get("shape") == "plus"
        self.milky = hexrgb(d["milkyway"]) if "milkyway" in d else None
        self.bodies = list(d["bodies"]) if "bodies" in d else ([d["body"]] if "body" in d else [])
        self.aur = [hexrgb(c) for c in d["aurora"]] if "aurora" in d else None
        self.puffs = d.get("puffs")
        self.meteors = d.get("meteors", False)
        self.lightning = d.get("lightning", False)
        self.particles = d.get("particles")
        self.grid = hexrgb(d["grid"]) if "grid" in d else None
        self._clouds = None

    def blend(self, t):
        if not self.alt:
            return 0
        return 0.5 - 0.5 * math.cos(t * math.pi * 2 / self.cycle)

    def at(self, el, k=0):
        z, m, h, b = self.z, self.m, self.h, self.b
        if self.alt and k > 0:
            z, m, h, b = (lerp(x, y, k) for x, y in zip((z, m, h, b), self.alt))
        if self.bands:
            step = 90 / self.bands
            el = (math.floor(el / step) + 0.5) * step if el >= 0 else -(math.floor(-el / step) + 0.5) * step
        if el >= 0:
            t = min(1, el / 90)
            return lerp(h, m, smooth(t / 0.4)) if t < 0.4 else lerp(m, z, smooth((t - 0.4) / 0.6))
        return lerp(h, b, smooth(min(1, -el / 60)))

    def clouds(self):
        if self._clouds is None:
            p = self.puffs
            r = random.Random(seed(self.id) + 11)
            out = []
            for i in range(p["count"] if p else 0):
                slot = 360 / p["count"]
                az = i * slot + r.random() * slot * 0.6
                el = p["elev"][0] + r.random() * (p["elev"][1] - p["elev"][0])
                size = p["size"] * (0.7 + r.random() * 0.6)
                speed = p["speed"] * (0.7 + r.random() * 0.6)
                n = 5 + r.randrange(3)
                pf = []
                for j in range(n):
                    ox = (j / (n - 1) * 2 - 1) * size * 1.5 + (r.random() - 0.5) * size * 0.3
                    rad = size * (0.5 + 0.5 * r.random()) * (1 - 0.45 * abs(ox) / (size * 1.6))
                    pf.append((ox, rad * 0.55, rad))
                out.append((az, el, size, speed, pf))
            self._clouds = out
        return self._clouds


SK = [Sky(d) for d in SKIES]


class Canvas:
    """RGBA canvas in GUI pixels (scaled by S) with alpha-blended primitives."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (int(w * S), int(h * S)), (0, 0, 0, 255))

    def rect(self, x1, y1, x2, y2, rgb, a=255):
        if x2 <= x1 or y2 <= y1:
            return
        lay = Image.new("RGBA", self.im.size, (0, 0, 0, 0))
        ImageDraw.Draw(lay).rectangle([x1 * S, y1 * S, x2 * S - 1, y2 * S - 1], fill=tuple(rgb) + (int(a),))
        self.im.alpha_composite(lay)

    def rect_fast(self, x1, y1, x2, y2, rgb):
        ImageDraw.Draw(self.im).rectangle([x1 * S, y1 * S, x2 * S - 1, y2 * S - 1], fill=tuple(rgb) + (255,))

    def gradv(self, x1, y1, x2, y2, c1, a1, c2, a2):
        x1i, x2i = int(x1 * S), int(x2 * S)
        y1i, y2i = int(y1 * S), int(y2 * S)
        if y2i <= y1i or x2i <= x1i:
            return
        lay = Image.new("RGBA", self.im.size, (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        for y in range(y1i, y2i):
            f = (y - y1i) / max(1, y2i - y1i)
            c = lerp(c1, c2, f)
            a = int(a1 + (a2 - a1) * f)
            d.line([(x1i, y), (x2i - 1, y)], fill=c + (a,))
        self.im.alpha_composite(lay)

    def blob(self, cx, cy, rx, ry, c_in, a_in, c_out, a_out, steps=26):
        if rx <= 0.05 or ry <= 0.05:
            return
        x0, y0 = int((cx - rx) * S) - 1, int((cy - ry) * S) - 1
        x1, y1 = int((cx + rx) * S) + 2, int((cy + ry) * S) + 2
        lay = Image.new("RGBA", (x1 - x0, y1 - y0), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        for i in range(steps, 0, -1):
            f = i / steps
            c = lerp(c_in, c_out, f)
            a = int(a_in + (a_out - a_in) * f)
            ex0, ey0 = (cx - rx * f) * S - x0, (cy - ry * f) * S - y0
            ex1, ey1 = (cx + rx * f) * S - x0 - 1, (cy + ry * f) * S - y0 - 1
            if ex1 > ex0 and ey1 > ey0:
                d.ellipse([ex0, ey0, ex1, ey1], fill=c + (a,))
        # clip to the canvas
        ix0, iy0 = max(0, x0), max(0, y0)
        ix1, iy1 = min(self.im.width, x1), min(self.im.height, y1)
        if ix1 <= ix0 or iy1 <= iy0:
            return
        self.im.alpha_composite(lay.crop((ix0 - x0, iy0 - y0, ix1 - x0, iy1 - y0)), (ix0, iy0))

    def poly(self, pts, rgb, a):
        lay = Image.new("RGBA", self.im.size, (0, 0, 0, 0))
        ImageDraw.Draw(lay).polygon([(x * S, y * S) for x, y in pts], fill=tuple(rgb) + (int(a),))
        self.im.alpha_composite(lay)

    def tri_grad(self, p1, p2, tip, rgb, a0, a1, n=14):
        """Triangle whose alpha goes from a0 (base edge) to a1 (tip)."""
        for i in range(n):
            f1, f2 = i / n, (i + 1) / n
            q = lambda p, f: (p[0] + (tip[0] - p[0]) * f, p[1] + (tip[1] - p[1]) * f)
            self.poly([q(p1, f1), q(p2, f1), q(p2, f2), q(p1, f2)], rgb, a0 + (a1 - a0) * (f1 + f2) / 2)


def preview(sky, x1, y1, x2, y2, cv, t=2.5, snap=True):
    w, h = x2 - x1, y2 - y1
    hy = y1 + h * 0.78
    top = 62.0
    px_deg = w / 62.0
    k = sky.blend(t)

    # 1. gradient / bands
    if sky.bands:
        step = 90 / sky.bands
        i = 0
        while i * step < top:
            e1, e2 = i * step, (i + 1) * step
            ya = hy - min(e2, top) / top * (hy - y1)
            yb = hy - e1 / top * (hy - y1)
            cv.rect_fast(x1, math.floor(ya), x2, math.ceil(yb), sky.at((e1 + e2) / 2, k))
            i += 1
        cv.rect_fast(x1, hy, x2, y2, sky.at(-10, k))
    else:
        els = [62, 45, 30, 18, 9, 3, 0]
        for i in range(len(els) - 1):
            ya = hy - els[i] / top * (hy - y1)
            yb = hy - els[i + 1] / top * (hy - y1)
            cv.gradv(x1, ya, x2, yb, sky.at(els[i], k), 255, sky.at(els[i + 1], k), 255)
        cv.gradv(x1, hy, x2, y2, sky.at(0, k), 255, sky.at(-40, k), 255)

    # 2. milky way
    if sky.milky:
        r = random.Random(5)
        for i in range(26):
            u = i / 25
            mx = x1 + w * (0.05 + 0.9 * u) + (r.random() - 0.5) * w * 0.08
            my = hy - h * 0.1 - (hy - y1 - h * 0.15) * u * 0.9 + (r.random() - 0.5) * h * 0.07
            cv.blob(mx, my, h * 0.16, h * 0.16, sky.milky, 26, sky.milky, 0)

    # 3. stars
    if sky.stars["count"] > 0:
        r = random.Random(seed(sky.id) + 11)
        n = min(36, max(8, sky.stars["count"] // 16))
        for _ in range(n):
            sx, sy = int(x1 + r.random() * w), int(y1 + r.random() * (hy - y1 - 3))
            ph = r.random() * 6.28
            br = (0.45 + r.random() * 0.55) * sky.stars["bright"]
            al = int(255 * min(1, br * (0.55 + 0.45 * math.sin(t * 2 + ph))))
            if sky.plus:
                cv.rect(sx, sy, sx + 1, sy + 1, (255, 255, 255), al)
                for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                    cv.rect(sx + dx, sy + dy, sx + dx + 1, sy + dy + 1, (255, 255, 255), al * 2 // 3)
            else:
                sz = 2 if r.random() > 0.88 else 1
                cv.rect(sx, sy, sx + sz, sy + sz, (255, 255, 255), al)

    # 4. aurora
    if sky.aur:
        for band in range(2):
            for x in range(0, int(w), 2):
                yc = y1 + h * (0.34 + 0.1 * band) + math.sin(x * 0.09 + t * 0.8 + band * 2) * h * 0.05
                c = lerp(sky.aur[0], sky.aur[1], x / w)
                mod = 0.45 + 0.55 * max(0, math.sin(x * 0.045 + band * 1.7 + t * 0.3))
                cv.gradv(x1 + x, yc - h * 0.22, x1 + x + 2, yc, c, 0, c, 150 * mod)

    # 5. bodies
    for b in sky.bodies:
        bx = x1 + w * (0.5 + math.sin(math.radians(b["az"])) * 0.28)
        by = hy - b["elev"] / top * (hy - y1)
        r = max(2.5, b["size"] * px_deg)
        c = hexrgb(b["color"])
        typ = b["type"]
        if typ == "eclipse":
            cv.blob(bx, by, r * 3.4, r * 3.4, c, 190, c, 0)
            cv.blob(bx, by, r, r, (0, 0, 0), 255, (0, 0, 0), 255, 4)
            continue
        crescent = typ == "crescent"
        if not crescent:
            cv.blob(bx, by, r * 5, r * 5, c, 38, c, 0)
            cv.blob(bx, by, r * 2.6, r * 2.6, c, 100, c, 0)
        rays = b.get("rays", 0)
        if rays:
            spikes = b.get("spikes", False)
            r0 = r * (1.05 if spikes else 1.2)
            ln = b.get("rayLen", 20) * px_deg * 0.55
            hw = r0 * math.pi / rays * 0.9 if spikes else r0 * 0.3
            for i in range(rays):
                ang = math.pi * 2 * i / rays + t * 0.04
                dx, dy = math.cos(ang), math.sin(ang)
                p1 = (bx + dx * r0 - dy * hw, by + dy * r0 + dx * hw)
                p2 = (bx + dx * r0 + dy * hw, by + dy * r0 - dx * hw)
                tip = (bx + dx * (r0 + ln), by + dy * (r0 + ln))
                if spikes:
                    cv.poly([p1, p2, tip], c, 255)
                else:
                    cv.tri_grad(p1, p2, tip, c, 80, 0)
        if b.get("halo"):
            hr, th = r * 2.3, max(1.0, r * 0.28)
            pts_o = [(bx + math.cos(a) * (hr + th / 2), by + math.sin(a) * (hr + th / 2)) for a in [i * math.pi / 18 for i in range(37)]]
            pts_i = [(bx + math.cos(a) * (hr - th / 2), by + math.sin(a) * (hr - th / 2)) for a in [i * math.pi / 18 for i in range(37)]]
            cv.poly(pts_o + pts_i[::-1], c, 110)
        cv.blob(bx, by, r, r, c, 255, c, 255, 4)
        if typ == "moon":
            for ox, oy, rr in ((-0.32, 0.25, 0.22), (0.30, -0.18, 0.16), (0.05, 0.40, 0.12)):
                cv.blob(bx + ox * r / b["size"] * b["size"], by - oy * r, rr * r, rr * r, (0x60, 0x68, 0x78), 55, (0x60, 0x68, 0x78), 55, 3)
        if crescent:
            cut = sky.at(b["elev"], k)
            cv.blob(bx + r * 0.38, by - r * 0.08, r * 0.86, r * 0.86, cut, 255, cut, 255, 4)

    # 6. cloud puffs
    if sky.puffs:
        p = sky.puffs
        top_c, bot_c = hexrgb(p["top"]), hexrgb(p["bottom"])
        out_c = hexrgb(p["outline"]) if "outline" in p else None
        clouds = sky.clouds()[:5]
        for layer in range(3):
            if layer == 0 and not out_c:
                continue
            for (az, el, size, speed, pf) in clouds:
                cx = x1 + (((az / 360) + t * 0.012 * speed) % 1.0) * w * 1.5 - w * 0.25
                cy = hy - min(el, 50) / top * (hy - y1)
                for ox, oy, rad in pf:
                    px, py, rr = cx + ox * px_deg * 0.9, cy - oy * px_deg * 0.9, rad * px_deg * 0.9
                    a1, a2 = p["alpha"], int(p["alpha"] * (1 - p["soft"]))
                    if layer == 0:
                        cv.blob(px, py, rr * 1.1 + 0.6, (rr * 1.1 + 0.6) * p["flat"], out_c, 255, out_c, 255, 4)
                    elif layer == 1:
                        cv.blob(px, py + rr * 0.1, rr, rr * p["flat"], bot_c, a1, bot_c, a2, 14)
                    else:
                        cv.blob(px, py - rr * 0.18, rr * 0.8, rr * 0.8 * p["flat"], top_c, a1, top_c, a2, 14)

    # 7. grid
    if sky.grid:
        gc = sky.grid
        cv.rect_fast(x1, hy, x2, y2, lerp(sky.h, sky.b, 0.8))
        frac = (t * 0.3) % 1.0
        for j in range(9):
            d = (j + frac) / 9
            gy = hy + (y2 - hy) * d * d
            cv.rect(x1, int(gy), x2, int(gy) + 1, gc, 200 * (1 - d * 0.5))
        mx = x1 + w / 2
        for m in range(-6, 7):
            xb = mx + m * w / 5
            for yy in range(int(hy), int(y2)):
                fr = (yy - hy) / (y2 - hy)
                gx = int(mx + (xb - mx) * fr)
                if x1 <= gx < x2:
                    cv.rect(gx, yy, gx + 1, yy + 1, gc, 60 + 140 * (1 - fr))

    # 8. meteors
    if sky.meteors:
        for m in range(2):
            ph = 0.5
            hx = x1 + w * (0.25 + 0.35 * m) + ph * w * 0.35
            hyy = y1 + h * (0.1 + 0.2 * m) + ph * h * 0.3
            cv.tri_grad((hx, hyy - 0.6), (hx, hyy + 0.6), (hx - w * 0.14, hyy - h * 0.09), (255, 255, 255), 255, 0)

    # 9. particles
    if sky.particles:
        pa = sky.particles
        col = hexrgb(pa["color"])
        r = random.Random(seed(sky.id) * 13 + 5)
        petal, firefly = pa["type"] == "petal", pa["type"] == "firefly"
        for i in range(min(16, pa["count"])):
            az, el, sp, ph = r.random() * 360, 3 + r.random() * 55, 0.5 + r.random(), r.random() * 6.28
            r.random()
            px = x1 + az / 360 * w
            pulse = 1.0
            if petal:
                py = y1 + ((el / 62 + t * 0.04 * sp) % 1.0) * (hy - y1)
                px += math.sin(t + ph) * 3
            elif firefly:
                px += math.sin(t * 0.6 + ph) * 4
                py = hy - (el * 0.4 + 6) / 62 * (hy - y1) + math.sin(t * 0.8 + ph) * 3
                q = 0.5 + 0.5 * math.sin(t * 1.3 + ph)
                pulse = max(0.55, q * q)
            else:
                py = hy - ((el / 62 + t * 0.05 * sp) % 1.0) * (hy - y1)
                pulse = 0.35 + 0.65 * (0.5 + 0.5 * math.sin(t * 2.2 + ph))
            al = 235 * pulse
            if petal:
                cv.blob(px, py, 1.6, 1.0, col, al, col, al, 3)
            else:
                cv.blob(px, py, 3.2, 3.2, col, al / 3, col, 0, 6)
                cv.rect(int(px), int(py), int(px) + 1, int(py) + 1, (255, 255, 255), al)

    # 10. lightning (frame caught during the flash)
    if sky.lightning:
        inn = 0.85
        cv.rect(x1, y1, x2, hy, (0xCF, 0xD8, 0xFF), 45 * inn)
        r = random.Random(4)
        bx, yy = x1 + w * 0.62, y1 + h * 0.08
        while yy < hy - 2:
            nx = bx + (r.random() - 0.5) * 6
            for q in range(5):
                px = int(bx + (nx - bx) * q / 5)
                cv.rect(px - 1, int(yy + q), px + 2, int(yy + q) + 1, (0x9D, 0xB4, 0xFF), 60)
                cv.rect(px, int(yy + q), px + 1, int(yy + q) + 1, (0xE6, 0xF0, 0xFF), 255 * inn)
            bx = nx
            yy += 5

    # 11. ground
    if not sky.grid:
        ground = lerp(lerp(sky.h, sky.b, 0.5), (0, 0, 0), 0.62)
        for x in range(0, int(w), 2):
            hh = h * 0.07 + (math.sin(x * 0.11) * 0.5 + 0.5) * h * 0.09 + (math.sin(x * 0.037 + 1) * 0.5 + 0.5) * h * 0.07
            cv.rect_fast(x1 + x, hy - hh + h * 0.04, min(x2, x1 + x + 2), y2, ground)


def card_preview(sky, w, h, t=2.5):
    cv = Canvas(w, h)
    preview(sky, 0, 0, w, h, cv, t)
    return cv.im


def rrect(dr, box, r, fill):
    dr.rounded_rectangle([box[0] * S, box[1] * S, box[2] * S - 1, box[3] * S - 1], r * S, fill=fill)


def menu(cat="All", scroll=0, selected="anime_day", W=480, Hh=300):
    pw, ph = min(460, W - 20), min(292, Hh - 20)
    px, py = (W - pw) // 2, (Hh - ph) // 2
    cur = next(s for s in SK if s.id == selected)
    base = Canvas(W, Hh)
    preview(cur, 0, 0, W, Hh, base, t=2.5)
    im = base.im
    im.alpha_composite(Image.new("RGBA", im.size, (0, 0, 0, 60)))
    dr = ImageDraw.Draw(im)
    f = lambda sz, b=False: ImageFont.truetype(FONTB if b else FONT, int(sz * S))
    BG, HEAD, CHIP, DARK, MID, SOFT, ACC = (243,) * 3, (233,) * 3, (227,) * 3, (46,) * 3, (107,) * 3, (154,) * 3, (58,) * 3
    sh = Image.new("RGBA", im.size, (0, 0, 0, 0))
    rrect(ImageDraw.Draw(sh), (px + 2, py + 3, px + pw + 2, py + ph + 3), 8, (0, 0, 0, 38))
    im.alpha_composite(sh)
    dr = ImageDraw.Draw(im)
    rrect(dr, (px, py, px + pw, py + ph), 8, BG)
    rrect(dr, (px, py, px + pw, py + 36), 8, HEAD)
    dr.rectangle([px * S, (py + 20) * S, (px + pw) * S, (py + 36) * S], fill=HEAD)
    dr.rectangle([px * S, (py + 36) * S, (px + pw) * S, (py + 37) * S], fill=(221,) * 3)
    dr.text(((px + 14) * S, (py + 9) * S), "SkyFX", font=f(13, True), fill=DARK)
    dr.text(((px + 14 + 52) * S, (py + 14) * S), "by @Adryh01", font=f(8), fill=SOFT)
    rrect(dr, (px + pw - 28, py + 9, px + pw - 10, py + 27), 4, CHIP)
    dr.text(((px + pw - 21) * S, (py + 12) * S), "x", font=f(9), fill=MID)
    cx = px + 14
    for c in CATS:
        cw = int(dr.textlength(c, font=f(8)) / S) + 16
        sel = c == cat
        rrect(dr, (cx, py + 43, cx + cw, py + 59), 6, ACC if sel else CHIP)
        dr.text(((cx + 8) * S, (py + 46) * S), c, font=f(8), fill=(255,) * 3 if sel else MID)
        cx += cw + 6
    gx, gy, gw, gh = px + 14, py + 64, pw - 36, ph - 104
    cols = max(2, (gw + 8) // 100)
    cw_ = (gw - 8 * (cols - 1)) // cols
    ph_ = int(cw_ * 0.5)
    ch_ = ph_ + 28
    shown = [s for s in SK if cat == "All" or s.cat == cat]
    grid = Image.new("RGBA", (gw * S, gh * S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grid, "RGBA")
    for i, s in enumerate(shown):
        x = (i % cols) * (cw_ + 8)
        y = (i // cols) * (ch_ + 8) - scroll
        if y + ch_ < 0 or y > gh:
            continue
        sel = s.id == selected
        gd.rounded_rectangle([x * S, y * S, (x + cw_) * S - 1, (y + ch_) * S - 1], 5 * S, fill=ACC if sel else (214,) * 3)
        t = 2 if sel else 1
        gd.rounded_rectangle([(x + t) * S, (y + t) * S, (x + cw_ - t) * S - 1, (y + ch_ - t) * S - 1], max(0, 5 - t) * S, fill=(255,) * 3)
        grid.alpha_composite(card_preview(s, cw_ - 8, ph_), ((x + 4) * S, int((y + 4) * S)))
        gd = ImageDraw.Draw(grid, "RGBA")
        gd.text(((x + 6) * S, (y + 4 + ph_ + 6) * S), s.name, font=f(8), fill=DARK if sel else MID)
        if sel:
            bx, by = x + cw_ - 4 - 14, y + 7
            gd.rounded_rectangle([bx * S, by * S, (bx + 11) * S, (by + 11) * S], 3 * S, fill=ACC)
            for (a, b) in [(2, 5), (3, 6), (4, 7), (5, 6), (6, 5), (7, 4), (8, 3)]:
                gd.rectangle([(bx + a) * S, (by + b - 1) * S, (bx + a + 1) * S, (by + b + 1) * S], fill=(255,) * 3)
    im.alpha_composite(grid, (gx * S, gy * S))
    dr = ImageDraw.Draw(im, "RGBA")
    rows = (len(shown) + cols - 1) // cols
    content = rows * (ch_ + 8) - 8
    if content > gh:
        tx = gx + gw + 4
        dr.rectangle([tx * S, gy * S, (tx + 3) * S, (gy + gh) * S], fill=(226,) * 3)
        th = max(18, gh * gh // content)
        ty = gy + int((gh - th) * (scroll / max(1, content - gh)))
        rrect(dr, (tx, ty, tx + 3, ty + th), 1, (156,) * 3)
    fy = py + ph - 26
    dr.rectangle([px * S, (py + ph - 34) * S, (px + pw) * S, (py + ph - 33) * S], fill=(221,) * 3)

    def toggle(x, label, on):
        rrect(dr, (x, fy, x + 22, fy + 12), 5, ACC if on else (207,) * 3)
        kx = x + 2 + (10 if on else 0)
        rrect(dr, (kx, fy + 2, kx + 8, fy + 10), 3, (255,) * 3)
        dr.text(((x + 28) * S, (fy + 1) * S), label, font=f(8), fill=MID)

    toggle(px + 14, "Animations", True)
    toggle(px + 14 + 26 + 56 + 18, "Sky fog", True)
    name = cur.name
    nw = dr.textlength(name, font=f(8)) / S
    aw = dr.textlength("Active: ", font=f(8)) / S
    dr.text(((px + pw - 14 - nw - aw) * S, (fy + 1) * S), "Active: ", font=f(8), fill=SOFT)
    dr.text(((px + pw - 14 - nw) * S, (fy + 1) * S), name, font=f(8), fill=DARK)
    return im.convert("RGB")


def sheet(cat=None, cols=4, cw=160, chh=96, name="skies.png"):
    items = [s for s in SK if cat is None or s.cat == cat]
    rows = (len(items) + cols - 1) // cols
    sc = 3
    im = Image.new("RGB", (cols * (cw + 10) + 10, rows * (chh + 30) + 10), (243, 243, 243))
    dr = ImageDraw.Draw(im)
    fn = ImageFont.truetype(FONT, 13)
    for i, s in enumerate(items):
        x = 10 + (i % cols) * (cw + 10)
        y = 10 + (i // cols) * (chh + 30)
        p = card_preview(s, cw // 2, chh // 2 if False else int(cw * 0.5 * 0.6))  # gui px
        p = p.resize((cw, chh), Image.LANCZOS)
        im.paste(p.convert("RGB"), (x, y))
        dr.text((x, y + chh + 6), s.name, font=fn, fill=(46, 46, 46))
    return im


if __name__ == "__main__":
    menu("All", 0, "anime_day").save(os.path.join(OUT, "menu.png"))
    menu("Cartoon", 0, "cartoon_sunset").save(os.path.join(OUT, "menu_cartoon.png"))
    menu("Void", 0, "void_bloom").save(os.path.join(OUT, "menu_void.png"))
    sheet().save(os.path.join(OUT, "skies.png"))
    print("done")
