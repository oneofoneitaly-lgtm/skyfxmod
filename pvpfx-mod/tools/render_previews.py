#!/usr/bin/env python3
"""Mock-ups of the PvPFX menu (same layout rules as ModMenu.java) and a sample HUD."""
import os, sys
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "docs")
os.makedirs(OUT, exist_ok=True)
S = 3
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONTB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
f = lambda sz, b=False: ImageFont.truetype(FONTB if b else FONT, int(sz * S))
BG, HEAD, CHIP, DARK, MID, SOFT, ACC = (243,) * 3, (233,) * 3, (227,) * 3, (46,) * 3, (107,) * 3, (154,) * 3, (58,) * 3

MODS = [
    ("FPS", "Shows your frames per second.", "HUD", True),
    ("CPS", "Clicks per second (left and right).", "HUD", True),
    ("Keystrokes", "WASD, mouse and space with CPS.", "HUD", True),
    ("Coordinates", "Your X, Y, Z and facing direction.", "HUD", True),
    ("Armor Status", "Armor and held item with durability.", "HUD", True),
    ("Potion Status", "Active effects and their time left.", "HUD", False),
    ("Ping", "Your latency to the server.", "HUD", True),
    ("Clock", "The real time.", "HUD", False),
    ("Reach Display", "Distance of your last hit.", "HUD", False),
    ("Toggle Sprint", "Sprints automatically while you walk forward.", "Gameplay", True),
    ("Zoom", "Hold the Zoom key (C by default) to zoom in.", "Gameplay", True),
    ("Fullbright", "Everything is fully lit, even in caves.", "Visual", False),
    ("Hide Fire", "Hides the fire overlay on your screen.", "Visual", False),
]


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rr(dr, box, r, fill):
    dr.rounded_rectangle([box[0] * S, box[1] * S, box[2] * S - 1, box[3] * S - 1], r * S, fill=fill)


def switch(dr, x, y, on):
    rr(dr, (x, y, x + 22, y + 12), 5, ACC if on else (207,) * 3)
    kx = x + 2 + (10 if on else 0)
    rr(dr, (kx, y + 2, kx + 8, y + 10), 3, (255,) * 3)


def backdrop(W, H):
    im = Image.new("RGB", (W * S, H * S))
    d = ImageDraw.Draw(im)
    for y in range(H * S):
        t = y / (H * S)
        d.line([(0, y), (W * S, y)], fill=lerp((58, 120, 220), (190, 225, 250), min(1, t * 1.3)))
    hy = int(H * 0.72 * S)
    d.rectangle([0, hy, W * S, H * S], fill=(70, 120, 60))
    d.rectangle([0, hy, W * S, hy + 4 * S], fill=(90, 150, 70))
    return im.convert("RGBA")


def frame(W=480, H=300):
    pw, ph = min(460, W - 20), min(292, H - 20)
    px, py = (W - pw) // 2, (H - ph) // 2
    im = backdrop(W, H)
    im.alpha_composite(Image.new("RGBA", im.size, (0, 0, 0, 60)))
    sh = Image.new("RGBA", im.size, (0, 0, 0, 0))
    rr(ImageDraw.Draw(sh), (px + 2, py + 3, px + pw + 2, py + ph + 3), 8, (0, 0, 0, 38))
    im.alpha_composite(sh)
    dr = ImageDraw.Draw(im)
    rr(dr, (px, py, px + pw, py + ph), 8, BG)
    rr(dr, (px, py, px + pw, py + 36), 8, HEAD)
    dr.rectangle([px * S, (py + 20) * S, (px + pw) * S, (py + 36) * S], fill=HEAD)
    dr.rectangle([px * S, (py + 36) * S, (px + pw) * S, (py + 37) * S], fill=(221,) * 3)
    dr.text(((px + 14) * S, (py + 9) * S), "PvPFX", font=f(13, True), fill=DARK)
    dr.text(((px + 14 + 50) * S, (py + 14) * S), "by @Adryh01", font=f(8), fill=SOFT)
    rr(dr, (px + pw - 28, py + 9, px + pw - 10, py + 27), 4, CHIP)
    dr.text(((px + pw - 21) * S, (py + 12) * S), "x", font=f(9), fill=MID)
    fy = py + ph - 26
    dr.rectangle([px * S, (py + ph - 34) * S, (px + pw) * S, (py + ph - 33) * S], fill=(221,) * 3)
    rr(dr, (px + 14, fy - 1, px + 78, fy + 15), 6, ACC)
    dr.text(((px + 46) * S - dr.textlength("Edit HUD", font=f(8)) / 2, (fy + 3) * S), "Edit HUD", font=f(8), fill=(255,) * 3)
    hint = "Right Shift to close"
    dr.text(((px + pw - 14) * S - dr.textlength(hint, font=f(8)), (fy + 3) * S), hint, font=f(8), fill=SOFT)
    return im, dr, (px, py, pw, ph)


def grid(cat="All", sel_open=None):
    im, dr, (px, py, pw, ph) = frame()
    cx = px + 14
    for c in ["All", "HUD", "Gameplay", "Visual"]:
        cw = int(dr.textlength(c, font=f(8)) / S) + 16
        on = c == cat
        rr(dr, (cx, py + 43, cx + cw, py + 59), 6, ACC if on else CHIP)
        dr.text(((cx + 8) * S, (py + 46) * S), c, font=f(8), fill=(255,) * 3 if on else MID)
        cx += cw + 6
    gx, gy, gw, gh = px + 14, py + 64, pw - 36, ph - 104
    cols, gap, chh = 2, 8, 50
    cw_ = (gw - gap) // 2
    shown = [m for m in MODS if cat == "All" or m[2] == cat]
    lay = Image.new("RGBA", (gw * S, gh * S), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lay)
    for i, (name, desc, c, on) in enumerate(shown):
        x, y = (i % cols) * (cw_ + gap), (i // cols) * (chh + gap)
        if y > gh:
            continue
        rr(ld, (x, y, x + cw_, y + chh), 5, ACC if on else (214,) * 3)
        t = 2 if on else 1
        rr(ld, (x + t, y + t, x + cw_ - t, y + chh - t), max(0, 5 - t), (255,) * 3)
        ld.text(((x + 9) * S, (y + 7) * S), name, font=f(8), fill=DARK)
        ld.text(((x + 9) * S, (y + 20) * S), desc[:46], font=f(8), fill=MID)
        ld.text(((x + 9) * S, (y + chh - 14) * S), c, font=f(8), fill=SOFT)
        switch(ld, x + cw_ - 32, y + 8, on)
        rr(ld, (x + cw_ - 62, y + chh - 18, x + cw_ - 8, y + chh - 5), 5, CHIP)
        ld.text(((x + cw_ - 35) * S - ld.textlength("Settings", font=f(8)) / 2, (y + chh - 15) * S), "Settings", font=f(8), fill=MID)
    im.alpha_composite(lay, (gx * S, gy * S))
    ImageDraw.Draw(im).rectangle([(gx + gw + 4) * S, gy * S, (gx + gw + 7) * S, (gy + gh) * S], fill=(226,) * 3)
    ImageDraw.Draw(im).rounded_rectangle([(gx + gw + 4) * S, gy * S, (gx + gw + 7) * S, (gy + 50) * S], S, fill=(156,) * 3)
    return im.convert("RGB")


def settings_page():
    im, dr, (px, py, pw, ph) = frame()
    gx, gy, gw = px + 14, py + 64, pw - 36
    rr(dr, (gx, py + 43, gx + 52, py + 59), 6, CHIP)
    dr.text(((gx + 26) * S - dr.textlength("< Back", font=f(8)) / 2, (py + 46) * S), "< Back", font=f(8), fill=MID)
    dr.text(((gx + 62) * S, (py + 43) * S), "Keystrokes", font=f(10, True), fill=DARK)
    dr.text(((gx + gw - 30) * S - dr.textlength("Enabled", font=f(8)), (py + 46) * S), "Enabled", font=f(8), fill=DARK)
    switch(dr, gx + gw - 24, py + 45, True)
    rows = [("Show mouse buttons", "sw", True), ("Show space bar", "sw", True), ("Scale", "sl", 0.33),
            ("Text shadow", "sw", True), ("Background", "sw", False), ("Text colour", "col", "White")]
    y = gy + 4
    for label, kind, val in rows:
        rr(dr, (gx, y, gx + gw, y + 20), 4, (255,) * 3)
        dr.text(((gx + 10) * S, (y + 6) * S), label, font=f(8), fill=DARK)
        right = gx + gw - 10
        if kind == "sw":
            switch(dr, right - 22, y + 4, val)
        elif kind == "sl":
            tx = right - 150
            dr.rectangle([tx * S, (y + 9) * S, (tx + 100) * S, (y + 12) * S], fill=(208,) * 3)
            dr.rectangle([tx * S, (y + 9) * S, (tx + int(100 * val)) * S, (y + 12) * S], fill=ACC)
            k = tx + int(100 * val)
            dr.rounded_rectangle([(k - 3) * S, (y + 5) * S, (k + 3) * S, (y + 15) * S], 3 * S, fill=(255,) * 3, outline=ACC, width=S)
            dr.text((right * S - dr.textlength("1.0", font=f(8)), (y + 6) * S), "1.0", font=f(8), fill=MID)
        else:
            rr(dr, (right - 106, y + 3, right, y + 17), 5, CHIP)
            dr.rectangle([(right - 100) * S, (y + 7) * S, (right - 94) * S, (y + 13) * S], fill=(255, 255, 255), outline=(150,) * 3)
            dr.text(((right - 53) * S - dr.textlength(val, font=f(8)) / 2 + 5 * S, (y + 6) * S), val, font=f(8), fill=DARK)
        y += 22
    return im.convert("RGB")


def hud():
    W, H = 480, 270
    im = backdrop(W, H)
    dr = ImageDraw.Draw(im, "RGBA")

    def txt(x, y, s, bg=False):
        if bg:
            dr.rectangle([(x - 3) * S, (y - 2) * S, (x + dr.textlength(s, font=f(8)) / S + 3) * S, (y + 10) * S], fill=(0, 0, 0, 112))
        dr.text(((x + 1) * S, (y + 1) * S), s, font=f(8), fill=(40, 40, 40, 255))
        dr.text((x * S, y * S), s, font=f(8), fill=(255, 255, 255, 255))

    txt(8, 6, "FPS: 144")
    txt(8, 22, "CPS: 9 | 2")
    txt(8, 38, "XYZ: 128 64 -302")
    txt(8, 50, "Facing: North")
    txt(8, 66, "Ping: 38 ms")
    txt(8, 82, "[Sprinting (Toggled)]")

    def key(x, y, w, h, label, down, sub=None):
        dr.rectangle([x * S, y * S, (x + w) * S, (y + h) * S], fill=(255, 255, 255, 208) if down else (0, 0, 0, 128))
        c = (32, 32, 32, 255) if down else (255, 255, 255, 255)
        if label:
            tw = dr.textlength(label, font=f(8)) / S
            dr.text(((x + (w - tw) / 2) * S, (y + (2 if sub else (h - 8) / 2)) * S), label, font=f(8), fill=c)
        if sub:
            tw = dr.textlength(sub, font=f(8)) / S
            dr.text(((x + (w - tw) / 2) * S, (y + 11) * S), sub, font=f(8), fill=c)

    kx, ky = 8, 110
    key(kx + 22, ky, 20, 20, "W", True)
    key(kx, ky + 22, 20, 20, "A", False)
    key(kx + 22, ky + 22, 20, 20, "S", False)
    key(kx + 44, ky + 22, 20, 20, "D", False)
    key(kx, ky + 44, 31, 20, "LMB", True, "9")
    key(kx + 33, ky + 44, 31, 20, "RMB", False, "2")
    key(kx, ky + 66, 64, 12, "", False)
    dr.rectangle([(kx + 20) * S, (ky + 71) * S, (kx + 44) * S, (ky + 72) * S], fill=(255, 255, 255, 255))

    # armor status (coloured squares stand in for the item icons)
    for i, c in enumerate([(80, 220, 220), (80, 220, 220), (80, 220, 220), (80, 220, 220), (200, 200, 220)]):
        y = 190 + i * 14
        dr.rectangle([8 * S, y * S, 20 * S, (y + 12) * S], fill=c + (255,), outline=(30, 90, 90, 255))
        txt(26, y + 2, str([363, 528, 494, 429, 1561][i]))
    txt(444, 6, "Speed II  1:30")
    txt(444, 18, "Strength  0:45")
    txt(440, 250, "18:47")
    return im.convert("RGB")


if __name__ == "__main__":
    grid().save(os.path.join(OUT, "menu.png"))
    settings_page().save(os.path.join(OUT, "settings.png"))
    hud().save(os.path.join(OUT, "hud.png"))
    print("done")
