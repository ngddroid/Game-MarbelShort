#!/usr/bin/env python3
"""
Builds every app icon from one square artwork: art/app_icon_source.png (512x512 or larger, rounded
corners allowed). Run from the repo root:  python3 tools/set_app_icon.py [path/to/art.png]

Writes:
  app/src/main/res/mipmap-*/ic_launcher.png, ic_launcher_round.png   legacy icons (API < 26)
  app/src/main/res/mipmap-*/ic_launcher_background.png               adaptive icon (API 26+), see below
  app/src/main/res/drawable-nodpi/app_icon.png                       the logo shown inside the app
  docs/store/icon_512.png                                            Play Store icon (full square)
Needs Pillow.
"""
import os, sys
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res")
SRC = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "art/app_icon_source.png")
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

art = Image.open(SRC).convert("RGBA")
n = min(art.size)
art = art.crop(((art.width - n) // 2, (art.height - n) // 2, (art.width + n) // 2, (art.height + n) // 2))

# background colour: the average of the opaque pixels along the top edge (the art's sky)
edge = [art.getpixel((x, max(2, n // 64))) for x in range(n // 4, 3 * n // 4)]
edge = [p for p in edge if p[3] > 250] or [(253, 240, 196, 255)]
bg = tuple(sum(p[i] for p in edge) // len(edge) for i in range(3)) + (255,)

# full-square version: transparent rounded corners filled by the art scaled up a little underneath
square = art.resize((int(n * 1.08),) * 2, Image.LANCZOS).crop((int(n * 0.04),) * 2 + (int(n * 0.04) + n,) * 2)
square = Image.alpha_composite(Image.new("RGBA", (n, n), bg), square)
square = Image.alpha_composite(square, art)

def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)
    print("wrote", os.path.relpath(path, ROOT))

def rounded(img, radius_frac):
    s = img.size[0]
    mask = Image.new("L", (s * 4, s * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, s * 4 - 1, s * 4 - 1), radius=int(s * 4 * radius_frac), fill=255)
    out = img.copy(); out.putalpha(mask.resize((s, s), Image.LANCZOS)); return out

def circle(img):
    s = img.size[0]
    mask = Image.new("L", (s * 4, s * 4), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, s * 4 - 1, s * 4 - 1), fill=255)
    out = img.copy(); out.putalpha(mask.resize((s, s), Image.LANCZOS)); return out

for d, k in DENSITIES.items():
    # legacy: 48dp, art inset by 1dp on each side like the Material legacy grid
    px = round(48 * k); inner = round(46 * k); off = (px - inner) // 2
    legacy = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    legacy.alpha_composite(rounded(square.resize((inner, inner), Image.LANCZOS), 0.18), (off, off))
    save(legacy, os.path.join(RES, f"mipmap-{d}", "ic_launcher.png"))
    rnd = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    rnd.alpha_composite(circle(square.resize((inner, inner), Image.LANCZOS)), (off, off))
    save(rnd, os.path.join(RES, f"mipmap-{d}", "ic_launcher_round.png"))
    # adaptive icon: the whole picture is the background layer, the foreground stays empty, so no
    # launcher effect (foreground parallax or scaling) can separate the picture into two parts. On the 108dp layer the
    # art fills the middle 80dp, so the 72dp launcher mask shows ~90% of it and the rounded corners
    # fall outside every mask; the edge beyond is the art scaled up, for parallax / zoom-out.
    lpx = round(108 * k); apx = round(80 * k); o = (lpx - apx) // 2
    layer = square.resize((lpx, lpx), Image.LANCZOS)
    layer.alpha_composite(square.resize((apx, apx), Image.LANCZOS), (o, o))
    save(layer, os.path.join(RES, f"mipmap-{d}", "ic_launcher_background.png"))
save(art.resize((512, 512), Image.LANCZOS), os.path.join(RES, "drawable-nodpi", "app_icon.png"))
save(square.resize((512, 512), Image.LANCZOS).convert("RGB"), os.path.join(ROOT, "docs/store/icon_512.png"))
