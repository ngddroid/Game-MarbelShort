#!/usr/bin/env python3
"""
Marble Pop Factory - icon & store art generator.

Single source of truth for the launcher icon design. Running this script:
  * writes the adaptive-icon vector drawables
      app/src/main/res/drawable/ic_launcher_background.xml
      app/src/main/res/drawable/ic_launcher_foreground.xml
      app/src/main/res/drawable/ic_launcher_monochrome.xml
      app/src/main/res/drawable/ic_splash_logo.xml
  * renders legacy launcher PNGs (API 24-25) for every density
      app/src/main/res/mipmap-*/ic_launcher.png, ic_launcher_round.png
  * renders Play Store art
      docs/store/icon_512.png, docs/store/feature_1024x500.png

Shapes are described once (paths made of cubic/quadratic Beziers, solid or
gradient fills, strokes) and emitted both as VectorDrawable XML and as a
Pillow raster (rendered at 4x supersampling, then LANCZOS-downscaled).

Usage:  python3 tools/gen_icon.py            (requires Pillow, no numpy)
"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
FONTS = os.path.join(ROOT, "app", "src", "main", "assets", "fonts")
STORE = os.path.join(ROOT, "docs", "store")
SS = 4  # supersampling factor

# --------------------------------------------------------------------------
# Brand palette
# --------------------------------------------------------------------------
DEEP = "#4B25B8"
PURPLE = "#7B4DFF"
LAVENDER = "#B9A3FF"
PINK = "#FF5FA2"
YELLOW = "#FFC93C"
CYAN = "#33D6F5"
LIME = "#9BE15D"
WHITE = "#FFFFFF"
INK = "#2B1A5E"

# light, base, shade, rim
MARBLES = {
    "pink": ("#FFD1E6", PINK, "#D62C7A", "#8C1550"),
    "cyan": ("#D4F8FF", CYAN, "#1199CC", "#0B5C8A"),
    "yellow": ("#FFF6C9", YELLOW, "#F09A00", "#99580A"),
    "lime": ("#EBFFD0", LIME, "#58A82A", "#2F6B14"),
}


def rgba(c, a=None):
    c = c.lstrip("#")
    if len(c) == 6:
        r, g, b, al = int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 255
    else:
        al, r, g, b = (int(c[i:i + 2], 16) for i in (0, 2, 4, 6))
    if a is not None:
        al = int(round(a * 255))
    return (r, g, b, al)


def xcol(c):
    return "#%02X%02X%02X%02X" % (c[3], c[0], c[1], c[2])


def fnum(v):
    s = ("%.2f" % v).rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


# --------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------
KAPPA = 0.5522847498


class Path:
    def __init__(self):
        self.cmds = []

    def M(self, x, y):
        self.cmds.append(("M", x, y)); return self

    def L(self, x, y):
        self.cmds.append(("L", x, y)); return self

    def C(self, x1, y1, x2, y2, x, y):
        self.cmds.append(("C", x1, y1, x2, y2, x, y)); return self

    def Q(self, x1, y1, x, y):
        self.cmds.append(("Q", x1, y1, x, y)); return self

    def Z(self):
        self.cmds.append(("Z",)); return self

    # full ellipse, optionally rotated (degrees) - affine image of a Bezier is exact
    def ellipse(self, cx, cy, rx, ry, rot=0.0):
        k = KAPPA
        pts = [(rx, 0), (rx, k * ry), (k * rx, ry), (0, ry), (-k * rx, ry), (-rx, k * ry), (-rx, 0),
               (-rx, -k * ry), (-k * rx, -ry), (0, -ry), (k * rx, -ry), (rx, -k * ry), (rx, 0)]
        ca, sa = math.cos(math.radians(rot)), math.sin(math.radians(rot))
        p = [(cx + x * ca - y * sa, cy + x * sa + y * ca) for x, y in pts]
        self.M(*p[0])
        for i in range(1, 13, 3):
            self.C(*p[i], *p[i + 1], *p[i + 2])
        return self.Z()

    def circle(self, cx, cy, r):
        return self.ellipse(cx, cy, r, r)

    def arc(self, cx, cy, rx, ry, a0, a1, move=False):
        """Elliptical arc from angle a0 to a1 (degrees, y-down)."""
        n = max(1, int(math.ceil(abs(a1 - a0) / 90.0 - 1e-9)))
        da = (a1 - a0) / n
        if move:
            t = math.radians(a0)
            self.M(cx + rx * math.cos(t), cy + ry * math.sin(t))
        for i in range(n):
            t0, t1 = math.radians(a0 + i * da), math.radians(a0 + (i + 1) * da)
            k = 4.0 / 3.0 * math.tan((t1 - t0) / 4.0)
            p0 = (cx + rx * math.cos(t0), cy + ry * math.sin(t0))
            p3 = (cx + rx * math.cos(t1), cy + ry * math.sin(t1))
            c1 = (p0[0] - k * rx * math.sin(t0), p0[1] + k * ry * math.cos(t0))
            c2 = (p3[0] + k * rx * math.sin(t1), p3[1] - k * ry * math.cos(t1))
            self.C(*c1, *c2, *p3)
        return self

    def d(self):
        out = []
        for c in self.cmds:
            out.append(c[0] + (",".join(fnum(v) for v in c[1:]) if len(c) > 1 else ""))
        return " ".join(out)

    def polys(self, m):
        """Sample into polygons in device space using affine matrix m."""
        res, cur, start, pts, closed = [], (0, 0), (0, 0), None, False

        def ap(x, y):
            return apply(m, x, y)

        def flush():
            if pts and len(pts) > 1:
                res.append((pts, closed))

        for c in self.cmds:
            op = c[0]
            if op == "M":
                flush()
                cur = start = (c[1], c[2]); pts = [ap(*cur)]; closed = False
            elif op == "L":
                cur = (c[1], c[2]); pts.append(ap(*cur))
            elif op in ("C", "Q"):
                if op == "Q":
                    x0, y0 = cur
                    x1 = x0 + 2.0 / 3.0 * (c[1] - x0); y1 = y0 + 2.0 / 3.0 * (c[2] - y0)
                    x2 = c[3] + 2.0 / 3.0 * (c[1] - c[3]); y2 = c[4] + 2.0 / 3.0 * (c[2] - c[4])
                    seg = (cur, (x1, y1), (x2, y2), (c[3], c[4]))
                else:
                    seg = (cur, (c[1], c[2]), (c[3], c[4]), (c[5], c[6]))
                dp = [ap(*q) for q in seg]
                ln = sum(math.hypot(dp[i + 1][0] - dp[i][0], dp[i + 1][1] - dp[i][1]) for i in range(3))
                n = max(6, int(ln / 2.0))
                for i in range(1, n + 1):
                    t = i / n; u = 1 - t
                    x = u * u * u * dp[0][0] + 3 * u * u * t * dp[1][0] + 3 * u * t * t * dp[2][0] + t * t * t * dp[3][0]
                    y = u * u * u * dp[0][1] + 3 * u * u * t * dp[1][1] + 3 * u * t * t * dp[2][1] + t * t * t * dp[3][1]
                    pts.append((x, y))
                cur = seg[3]
            elif op == "Z":
                closed = True; cur = start
        flush()
        return res


# affine matrix (a, b, c, d, e, f):  x' = a x + b y + c ; y' = d x + e y + f
IDENT = (1, 0, 0, 0, 1, 0)


def mmul(A, B):
    a1, b1, c1, d1, e1, f1 = A
    a2, b2, c2, d2, e2, f2 = B
    return (a1 * a2 + b1 * d2, a1 * b2 + b1 * e2, a1 * c2 + b1 * f2 + c1,
            d1 * a2 + e1 * d2, d1 * b2 + e1 * e2, d1 * c2 + e1 * f2 + f1)


def apply(m, x, y):
    return (m[0] * x + m[1] * y + m[2], m[3] * x + m[4] * y + m[5])


def minv(m):
    a, b, c, d, e, f = m
    det = a * e - b * d
    return (e / det, -b / det, (b * f - e * c) / det, -d / det, a / det, (d * c - a * f) / det)


def translate(x, y):
    return (1, 0, x, 0, 1, y)


# --------------------------------------------------------------------------
# Paint
# --------------------------------------------------------------------------
class Grad:
    def __init__(self, kind, stops, **kw):
        self.kind = kind
        self.stops = [(o, rgba(c) if isinstance(c, str) else c) for o, c in stops]
        self.__dict__.update(kw)

    def t_at(self, x, y):
        if self.kind == "linear":
            dx, dy = self.x2 - self.x1, self.y2 - self.y1
            t = ((x - self.x1) * dx + (y - self.y1) * dy) / (dx * dx + dy * dy)
        else:
            t = math.hypot(x - self.cx, y - self.cy) / self.r
        return 0.0 if t < 0 else 1.0 if t > 1 else t

    def color_at(self, x, y):
        t = self.t_at(x, y)
        s = self.stops
        if t <= s[0][0]:
            return s[0][1]
        for i in range(1, len(s)):
            if t <= s[i][0]:
                o0, c0 = s[i - 1]; o1, c1 = s[i]
                k = 0 if o1 == o0 else (t - o0) / (o1 - o0)
                return tuple(int(round(c0[j] + (c1[j] - c0[j]) * k)) for j in range(4))
        return s[-1][1]

    def xml(self, ind):
        if self.kind == "linear":
            head = ('<gradient android:type="linear" android:startX="%s" android:startY="%s" '
                    'android:endX="%s" android:endY="%s">' % tuple(fnum(v) for v in (self.x1, self.y1, self.x2, self.y2)))
        else:
            head = ('<gradient android:type="radial" android:centerX="%s" android:centerY="%s" '
                    'android:gradientRadius="%s">' % tuple(fnum(v) for v in (self.cx, self.cy, self.r)))
        lines = [ind + head]
        for o, c in self.stops:
            lines.append(ind + '    <item android:offset="%s" android:color="%s" />' % (fnum(o), xcol(c)))
        lines.append(ind + "</gradient>")
        return lines


def linear(x1, y1, x2, y2, stops):
    return Grad("linear", stops, x1=x1, y1=y1, x2=x2, y2=y2)


def radial(cx, cy, r, stops):
    return Grad("radial", stops, cx=cx, cy=cy, r=r)


class Shape:
    def __init__(self, path, fill=None, stroke=None, sw=0.0, even_odd=False, name=None):
        self.path, self.fill, self.stroke, self.sw, self.even_odd, self.name = path, fill, stroke, sw, even_odd, name
        if isinstance(self.fill, str):
            self.fill = rgba(self.fill)
        if isinstance(self.stroke, str):
            self.stroke = rgba(self.stroke)


class Group:
    def __init__(self, children, sx=1.0, sy=1.0, rot=0.0, px=0.0, py=0.0, tx=0.0, ty=0.0, name=None):
        self.children, self.sx, self.sy, self.rot = children, sx, sy, rot
        self.px, self.py, self.tx, self.ty, self.name = px, py, tx, ty, name

    def matrix(self):
        # Android VectorDrawable order: T(-pivot) -> scale -> rotate -> T(pivot + translate)
        ca, sa = math.cos(math.radians(self.rot)), math.sin(math.radians(self.rot))
        m = translate(-self.px, -self.py)
        m = mmul((self.sx, 0, 0, 0, self.sy, 0), m)
        m = mmul((ca, -sa, 0, sa, ca, 0), m)
        return mmul(translate(self.px + self.tx, self.py + self.ty), m)


# --------------------------------------------------------------------------
# VectorDrawable XML writer
# --------------------------------------------------------------------------
def to_xml(items, comment):
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           "<!-- %s -->" % comment,
           "<!-- Generated by tools/gen_icon.py - edit the script and re-run instead of editing by hand. -->",
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    xmlns:aapt="http://schemas.android.com/aapt"',
           '    android:width="108dp"',
           '    android:height="108dp"',
           '    android:viewportWidth="108"',
           '    android:viewportHeight="108">']

    def emit(it, ind):
        if isinstance(it, Group):
            attrs = []
            if it.name: attrs.append('android:name="%s"' % it.name)
            for k, v, dflt in (("pivotX", it.px, 0), ("pivotY", it.py, 0), ("scaleX", it.sx, 1), ("scaleY", it.sy, 1),
                               ("rotation", it.rot, 0), ("translateX", it.tx, 0), ("translateY", it.ty, 0)):
                if abs(v - dflt) > 1e-9:
                    attrs.append('android:%s="%s"' % (k, fnum(v)))
            out.append(ind + "<group" + ("\n" + ind + "    " if attrs else "") + ("\n" + ind + "    ").join(attrs) + ">")
            for c in it.children:
                emit(c, ind + "    ")
            out.append(ind + "</group>")
            return
        attrs = []
        if it.name: attrs.append('android:name="%s"' % it.name)
        attrs.append('android:pathData="%s"' % it.path.d())
        if isinstance(it.fill, tuple):
            attrs.append('android:fillColor="%s"' % xcol(it.fill))
        if it.even_odd:
            attrs.append('android:fillType="evenOdd"')
        if it.stroke is not None:
            attrs += ['android:strokeColor="%s"' % xcol(it.stroke), 'android:strokeWidth="%s"' % fnum(it.sw),
                      'android:strokeLineJoin="round"', 'android:strokeLineCap="round"']
        sep = "\n" + ind + "    "
        if isinstance(it.fill, Grad):
            out.append(ind + "<path" + sep + sep.join(attrs) + ">")
            out.append(ind + '    <aapt:attr name="android:fillColor">')
            out.extend(it.fill.xml(ind + "        "))
            out.append(ind + "    </aapt:attr>")
            out.append(ind + "</path>")
        else:
            out.append(ind + "<path" + sep + sep.join(attrs) + " />")

    for it in items:
        emit(it, "    ")
    out.append("</vector>")
    return "\n".join(out) + "\n"


# --------------------------------------------------------------------------
# Raster renderer
# --------------------------------------------------------------------------
GRID = 112  # max gradient evaluation grid per shape (smooth gradients are upsampled)


def _paint(fill, m, ox, oy, bw, bh):
    if isinstance(fill, tuple):
        return Image.new("RGBA", (bw, bh), fill)
    gw, gh = max(2, min(bw, GRID)), max(2, min(bh, GRID))
    inv = minv(m)
    data = []
    for j in range(gh):
        y = oy + (j + 0.5) * bh / gh
        for i in range(gw):
            x = ox + (i + 0.5) * bw / gw
            data.append(fill.color_at(*apply(inv, x, y)))
    img = Image.new("RGBA", (gw, gh))
    img.putdata(data)
    return img if (gw, gh) == (bw, bh) else img.resize((bw, bh), Image.BILINEAR)


def _composite(canvas, layer, mask, ox, oy):
    a = ImageChops.multiply(layer.getchannel("A"), mask)
    layer.putalpha(a)
    canvas.alpha_composite(layer, dest=(ox, oy))


def draw_items(canvas, items, m):
    W, H = canvas.size
    for it in items:
        if isinstance(it, Group):
            draw_items(canvas, it.children, mmul(m, it.matrix()))
            continue
        polys = it.path.polys(m)
        if not polys:
            continue
        scale = math.sqrt(abs(m[0] * m[4] - m[1] * m[3]))
        swp = it.sw * scale if it.stroke is not None else 0
        xs = [p[0] for pts, _ in polys for p in pts]
        ys = [p[1] for pts, _ in polys for p in pts]
        pad = swp / 2 + 3
        x0, y0 = max(0, int(min(xs) - pad)), max(0, int(min(ys) - pad))
        x1, y1 = min(W, int(max(xs) + pad) + 1), min(H, int(max(ys) + pad) + 1)
        if x1 <= x0 or y1 <= y0:
            continue
        bw, bh = x1 - x0, y1 - y0
        if it.fill is not None:
            mask = Image.new("L", (bw, bh), 0)
            for pts, _ in polys:
                if len(pts) < 3:
                    continue
                sub = Image.new("L", (bw, bh), 0)
                ImageDraw.Draw(sub).polygon([(x - x0, y - y0) for x, y in pts], fill=255)
                mask = ImageChops.difference(mask, sub) if it.even_odd else ImageChops.lighter(mask, sub)
            _composite(canvas, _paint(it.fill, m, x0, y0, bw, bh), mask, x0, y0)
        if it.stroke is not None and swp > 0:
            mask = Image.new("L", (bw, bh), 0)
            d = ImageDraw.Draw(mask)
            w = max(1, int(round(swp)))
            for pts, closed in polys:
                q = [(x - x0, y - y0) for x, y in pts]
                if closed:
                    q = q + q[:2]
                r = w / 2.0
                for i in range(len(q) - 1):
                    d.line((q[i], q[i + 1]), fill=255, width=w)
                for px, py in q:  # round joins / caps
                    d.ellipse((px - r, py - r, px + r, py + r), fill=255)
            _composite(canvas, Image.new("RGBA", (bw, bh), it.stroke), mask, x0, y0)


def render(items, view, size, ss=SS):
    """Render items; view=(x0, y0, w, h) in design units -> size=(W, H) pixels."""
    vx, vy, vw, vh = view
    W, H = size
    s = W * ss / vw
    canvas = Image.new("RGBA", (W * ss, H * ss), (0, 0, 0, 0))
    draw_items(canvas, items, (s, 0, -vx * s, 0, s, -vy * s))
    return canvas


# --------------------------------------------------------------------------
# Design pieces
# --------------------------------------------------------------------------
def sparkle(cx, cy, s, color=WHITE, glow=True):
    p = Path().M(cx, cy - s).Q(cx, cy, cx + s * 0.78, cy).Q(cx, cy, cx, cy + s) \
        .Q(cx, cy, cx - s * 0.78, cy).Q(cx, cy, cx, cy - s).Z()
    items = []
    if glow:
        items.append(Shape(Path().circle(cx, cy, s * 0.95),
                           radial(cx, cy, s * 0.95, [(0, rgba(WHITE, 0.55)), (1, rgba(WHITE, 0))])))
    items.append(Shape(p, color))
    return items


def marble(cx, cy, r, kind, shadow="drop"):
    light, base, shade, rim = MARBLES[kind]
    items = []
    if shadow == "drop":
        items.append(Shape(Path().circle(cx + 0.06 * r, cy + 0.16 * r, r * 1.12),
                           radial(cx + 0.06 * r, cy + 0.16 * r, r * 1.12,
                                  [(0, rgba(INK, 0.55)), (0.82, rgba(INK, 0.45)), (1, rgba(INK, 0))])))
    elif shadow == "ground":
        gy = cy + r * 0.98
        items.append(Group([Shape(Path().circle(cx, gy, r * 1.05),
                                  radial(cx, gy, r * 1.05, [(0, rgba(INK, 0.5)), (1, rgba(INK, 0))]))],
                           sy=0.32, px=cx, py=gy))
    # body
    items.append(Shape(Path().circle(cx, cy, r),
                       radial(cx - 0.32 * r, cy - 0.38 * r, 1.5 * r,
                              [(0, light), (0.3, base), (0.72, shade), (1, rim)]),
                       stroke=rim, sw=max(0.6, 0.085 * r)))
    # bounce light at the bottom
    items.append(Shape(Path().ellipse(cx + 0.12 * r, cy + 0.6 * r, 0.5 * r, 0.22 * r, rot=-18),
                       linear(cx, cy + 0.38 * r, cx, cy + 0.82 * r,
                              [(0, rgba(light, 0)), (1, rgba(light, 0.75))])))
    # main specular highlight
    hx, hy = cx - 0.34 * r, cy - 0.4 * r
    items.append(Shape(Path().ellipse(hx, hy, 0.42 * r, 0.25 * r, rot=-38),
                       linear(hx - 0.2 * r, hy - 0.25 * r, hx + 0.2 * r, hy + 0.25 * r,
                              [(0, rgba(WHITE, 0.98)), (1, rgba(WHITE, 0.55))])))
    # tiny sharp glint
    items.append(Shape(Path().circle(cx + 0.18 * r, cy - 0.66 * r, 0.1 * r), rgba(WHITE, 0.92)))
    return items


# funnel geometry (108 design units)
FX, RIM_Y, RX_O, RY_O, RX_I, RY_I = 54.0, 58.0, 30.0, 8.6, 25.6, 6.2
NECK_Y, NECK_HW, SPOUT_BOT = 77.0, 7.0, 86.0


def funnel_back():
    items = []
    # ground shadow under the spout
    items.append(Group([Shape(Path().circle(FX, SPOUT_BOT + 1.5, 15),
                              radial(FX, SPOUT_BOT + 1.5, 15, [(0, rgba(INK, 0.55)), (1, rgba(INK, 0))]))],
                       sy=0.26, px=FX, py=SPOUT_BOT + 1.5))
    # ring (full)
    items.append(Shape(Path().ellipse(FX, RIM_Y, RX_O, RY_O),
                       linear(FX, RIM_Y - RY_O, FX, RIM_Y + RY_O,
                              [(0, "#FFFFFF"), (0.55, "#E4DAFF"), (1, LAVENDER)]),
                       stroke=INK, sw=2.0))
    # mouth (dark interior)
    items.append(Shape(Path().ellipse(FX, RIM_Y, RX_I, RY_I),
                       linear(FX, RIM_Y - RY_I, FX, RIM_Y + RY_I, [(0, INK), (1, "#5A33C9")]),
                       stroke=rgba("#8E6CF0"), sw=0.8))
    return items


def cone_path(gap=0.0, rx_top=RX_O, ry_top=RY_O):
    top_y = RIM_Y + gap
    p = Path()
    p.M(FX - rx_top, top_y)
    p.C(FX - rx_top + 3, top_y + 11, FX - NECK_HW - 2.5, NECK_Y - 6, FX - NECK_HW, NECK_Y)
    p.L(FX - NECK_HW, SPOUT_BOT - 2.6)
    p.Q(FX - NECK_HW, SPOUT_BOT, FX - NECK_HW + 2.6, SPOUT_BOT)
    p.L(FX + NECK_HW - 2.6, SPOUT_BOT)
    p.Q(FX + NECK_HW, SPOUT_BOT, FX + NECK_HW, SPOUT_BOT - 2.6)
    p.L(FX + NECK_HW, NECK_Y)
    p.C(FX + NECK_HW + 2.5, NECK_Y - 6, FX + rx_top - 3, top_y + 11, FX + rx_top, top_y)
    p.arc(FX, top_y, rx_top, ry_top, 0, 180)
    return p.Z()


def funnel_front():
    items = []
    # cone body
    items.append(Shape(cone_path(),
                       linear(FX - RX_O, 0, FX + RX_O, 0,
                              [(0, "#6236E0"), (0.12, "#9C7BFF"), (0.25, "#EDE6FF"), (0.34, "#C2AEFF"),
                               (0.62, "#8A5CFF"), (1, "#4B25B8")]),
                       stroke=INK, sw=2.0))
    # candy band around the neck
    band = Path()
    band.M(FX - NECK_HW - 1.6, NECK_Y - 4.2)
    band.Q(FX, NECK_Y - 1.4, FX + NECK_HW + 1.6, NECK_Y - 4.2)
    band.L(FX + NECK_HW, NECK_Y + 1.6)
    band.Q(FX, NECK_Y + 4.2, FX - NECK_HW, NECK_Y + 1.6).Z()
    items.append(Shape(band, linear(FX - 8, 0, FX + 8, 0, [(0, "#FF8CC0"), (0.35, "#FFC2DD"), (1, "#E23D88")]),
                       stroke=INK, sw=1.3))
    # glossy streak on the cone
    st = Path()
    st.M(FX - 20.5, RIM_Y + 9.5).Q(FX - 15.5, RIM_Y + 14, FX - 10.6, RIM_Y + 18.5) \
        .Q(FX - 11.6, RIM_Y + 15.5, FX - 17.6, RIM_Y + 10.4).Q(FX - 19.5, RIM_Y + 9.4, FX - 20.5, RIM_Y + 9.5).Z()
    items.append(Shape(st, rgba(WHITE, 0.85)))
    items.append(Shape(Path().circle(FX - 4.6, SPOUT_BOT - 3.6, 1.0), rgba(WHITE, 0.7)))
    # front half of the rim ring
    fr = Path()
    fr.arc(FX, RIM_Y, RX_O, RY_O, 0, 180, move=True)
    fr.L(FX - RX_I, RIM_Y)
    fr.arc(FX, RIM_Y, RX_I, RY_I, 180, 0)
    fr.Z()
    items.append(Shape(fr, linear(FX, RIM_Y, FX, RIM_Y + RY_O,
                                  [(0, "#FFFFFF"), (0.6, "#E6DCFF"), (1, LAVENDER)]),
                       stroke=INK, sw=2.0))
    # rim gloss
    gl = Path()
    gl.arc(FX, RIM_Y + 0.2, RX_O - 2.2, RY_O - 1.6, 112, 160, move=True)
    gl.arc(FX, RIM_Y + 0.2, RX_O - 3.6, RY_O - 2.8, 160, 112)
    gl.Z()
    items.append(Shape(gl, rgba(WHITE, 0.9)))
    return items


def marble_pile():
    return (marble(41.5, 47.5, 11.0, "pink")
            + marble(66.5, 47.5, 11.0, "cyan")
            + marble(54.0, 31.0, 11.0, "yellow")
            + marble(54.0, 52.0, 9.4, "lime"))


def icon_foreground(sparkles=True):
    items = funnel_back() + marble_pile() + funnel_front()
    if sparkles:
        items += sparkle(31.5, 31.0, 5.2) + sparkle(76.5, 29.5, 3.4)
    return items


def icon_background(w=108.0, h=108.0, cx=54.0, cy=48.0, R=80.0, rays=14, bubbles=True):
    items = [Shape(Path().M(0, 0).L(w, 0).L(w, h).L(0, h).Z(),
                   radial(cx, cy, R, [(0, "#B48DFF"), (0.32, "#8457FF"), (0.7, "#5A2ED0"), (1, "#3A1896")]))]
    # soft light rays
    rp = Path()
    L = math.hypot(w, h)
    for i in range(rays):
        a0 = math.radians(i * 360.0 / rays - 90)
        a1 = a0 + math.radians(360.0 / rays * 0.45)
        rp.M(cx, cy).L(cx + L * math.cos(a0), cy + L * math.sin(a0)).L(cx + L * math.cos(a1), cy + L * math.sin(a1)).Z()
    items.append(Shape(rp, radial(cx, cy, R * 0.9, [(0, rgba(WHITE, 0.2)), (0.55, rgba(WHITE, 0.08)), (1, rgba(WHITE, 0))])))
    if bubbles:
        for (bx, by, br, a) in ((22, 25, 4.2, 0.2), (88, 21, 3.0, 0.18), (90, 80, 5.0, 0.16), (17, 82, 3.4, 0.16),
                                (33, 93, 2.2, 0.2), (78, 94, 2.6, 0.16), (95, 50, 2.2, 0.2), (13, 52, 2.0, 0.18),
                                (64, 14, 1.8, 0.2)):
            items.append(Shape(Path().circle(bx, by, br),
                               radial(bx - br * 0.3, by - br * 0.3, br * 1.3,
                                      [(0, rgba(WHITE, a * 0.6)), (0.75, rgba(WHITE, a * 0.9)), (1, rgba(WHITE, a * 1.6))])))
            items.append(Shape(Path().ellipse(bx - br * 0.38, by - br * 0.4, br * 0.32, br * 0.2, rot=-40),
                               rgba(WHITE, min(1, a * 3.5))))
    return items


def monochrome():
    white = rgba(WHITE)
    items = []
    # ring with open mouth
    items.append(Shape(Path().ellipse(FX, RIM_Y, RX_O, RY_O).ellipse(FX, RIM_Y, RX_I - 1.2, RY_I - 1.0),
                       white, even_odd=True))
    # cone, separated from the ring by a small gap; with a gloss cut-out
    cp = cone_path(gap=3.0, rx_top=RX_O - 3.4, ry_top=RY_O - 1.0)
    cp.M(FX - 19.0, RIM_Y + 12.5).Q(FX - 14.6, RIM_Y + 16, FX - 10.6, RIM_Y + 19.5) \
        .Q(FX - 11.6, RIM_Y + 16.8, FX - 16.4, RIM_Y + 12.6).Q(FX - 18.0, RIM_Y + 11.8, FX - 19.0, RIM_Y + 12.5).Z()
    items.append(Shape(cp, white, even_odd=True))
    # three marbles above the rim, each with a highlight cut-out
    for cx, cy, r in ((41.0, 39.0, 9.6), (67.0, 39.0, 9.6), (54.0, 23.0, 9.6)):
        p = Path().circle(cx, cy, r).ellipse(cx - 0.34 * r, cy - 0.4 * r, 0.38 * r, 0.22 * r, rot=-38)
        items.append(Shape(p, white, even_odd=True))
    items += sparkle(30.0, 22.0, 5.2, glow=False)
    # content spans y 13..86 -> scale it into the 66dp safe zone
    return [Group(items, sx=0.8, sy=0.8, px=54, py=49.5, ty=54 - 49.5)]


def splash_logo():
    # same art as the launcher foreground, scaled to sit inside the 72dp splash circle
    return [Group(icon_foreground(sparkles=True), sx=0.9, sy=0.9, px=54, py=56)]


# --------------------------------------------------------------------------
# Raster outputs
# --------------------------------------------------------------------------
ICON_VIEW = (16.5, 16.5, 75.0, 75.0)  # crop of the 108 canvas used for flat icons


def flat_icon_art(size):
    art = render(icon_background() + icon_foreground(), ICON_VIEW, (size, size))
    return art  # supersampled


def masked_icon(size, round_):
    big = size * SS
    art = flat_icon_art(size)
    inset = big * 2.0 / 48.0
    box = (inset, inset * 0.8, big - inset, big - inset * 1.2)
    mask = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(mask)
    if round_:
        d.ellipse(box, fill=255)
    else:
        d.rounded_rectangle(box, radius=(box[2] - box[0]) * 0.2, fill=255)
    # scale the art into the mask box
    bw = int(box[2] - box[0])
    art = art.resize((bw, bw), Image.LANCZOS)
    layer = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    layer.paste(art, (int(box[0]), int(box[1])))
    layer.putalpha(ImageChops.multiply(layer.getchannel("A"), mask))
    # subtle drop shadow + inner rim
    shadow = Image.new("RGBA", (big, big), rgba(INK, 0.0))
    sm = mask.filter(ImageFilter.GaussianBlur(big * 0.018))
    sm = ImageChops.multiply(sm, Image.new("L", (big, big), 110))
    shadow.putalpha(sm)
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    out.alpha_composite(shadow, dest=(0, int(big * 0.012)))
    out.alpha_composite(layer)
    rim = Image.new("L", (big, big), 0)
    rd = ImageDraw.Draw(rim)
    w = max(1, int(big * 0.012))
    if round_:
        rd.ellipse(box, outline=255, width=w)
    else:
        rd.rounded_rectangle(box, radius=(box[2] - box[0]) * 0.2, outline=255, width=w)
    rim_layer = Image.new("RGBA", (big, big), rgba(WHITE, 0.22))
    rim_layer.putalpha(ImageChops.multiply(rim, Image.new("L", (big, big), 56)))
    out.alpha_composite(rim_layer)
    return out.resize((size, size), Image.LANCZOS)


def store_icon():
    art = render(icon_background() + icon_foreground(), (14.0, 14.0, 80.0, 80.0), (512, 512))
    return art.resize((512, 512), Image.LANCZOS).convert("RGB")


def text_layer(size_px, text, font, xy, fill, outline, ow, shadow_off, shadow_blur, anchor="ls"):
    W, H = size_px
    sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(sh).text((xy[0] + shadow_off[0], xy[1] + shadow_off[1]), text, font=font,
                            fill=rgba("#1A0E44", 0.75), stroke_width=ow, stroke_fill=rgba("#1A0E44", 0.75), anchor=anchor)
    sh = sh.filter(ImageFilter.GaussianBlur(shadow_blur))
    tl = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    td = ImageDraw.Draw(tl)
    # chunky hard drop (3D) under the outline, then the face
    td.text((xy[0], xy[1] + ow * 0.9), text, font=font, fill=rgba(INK), stroke_width=ow, stroke_fill=rgba(INK), anchor=anchor)
    td.text(xy, text, font=font, fill=fill, stroke_width=ow, stroke_fill=outline, anchor=anchor)
    sh.alpha_composite(tl)
    return sh


def feature_graphic():
    W, H = 1024, 500
    items = icon_background(w=W, h=H, cx=770, cy=230, R=760, rays=22, bubbles=False)
    # bubbles
    for (bx, by, br, a) in ((120, 60, 16, 0.14), (300, 470, 12, 0.14), (520, 120, 9, 0.16), (930, 130, 14, 0.14),
                            (640, 470, 18, 0.12), (40, 300, 10, 0.16), (1000, 300, 9, 0.16), (420, 30, 7, 0.18)):
        items.append(Shape(Path().circle(bx, by, br),
                           radial(bx - br * 0.3, by - br * 0.3, br * 1.3,
                                  [(0, rgba(WHITE, a * 0.6)), (0.75, rgba(WHITE, a * 0.9)), (1, rgba(WHITE, a * 1.6))])))
    # hero funnel with marbles
    sc = 4.3
    items.append(Group(icon_foreground(sparkles=False), sx=sc, sy=sc, px=54, py=54, tx=790 - 54, ty=258 - 54))
    # scattered marbles
    for (mx, my, mr, k) in ((70, 78, 30, "cyan"), (585, 72, 22, "yellow"), (38, 440, 36, "pink"),
                            (610, 440, 27, "lime"), (985, 58, 24, "pink"), (980, 445, 32, "cyan"),
                            (505, 462, 16, "yellow"), (200, 40, 13, "lime"), (548, 300, 14, "pink"),
                            (655, 125, 12, "cyan")):
        items += marble(mx, my, mr, k)
    for (sx, sy, s) in ((560, 200, 14), (130, 400, 10), (930, 205, 16), (690, 60, 9), (250, 105, 8)):
        items += sparkle(sx, sy, s)
    img = render(items, (0, 0, W, H), (W, H))
    big = img.size
    k = SS
    lil = os.path.join(FONTS, "lilita_one.ttf")
    # fit the two-line title into the left area
    line1, line2 = "Marble Pop", "Factory"
    fs = 128
    while True:
        f = ImageFont.truetype(lil, fs * k)
        if f.getlength(line1) <= 520 * k:
            break
        fs -= 2
    cxl = 300
    img.alpha_composite(text_layer(big, line1, f, (cxl * k, 228 * k), rgba(WHITE), rgba(DEEP), 9 * k,
                                   (0, 14 * k), 10 * k, anchor="ms"))
    f2 = ImageFont.truetype(lil, int(fs * 1.12) * k)
    img.alpha_composite(text_layer(big, line2, f2, (cxl * k, 352 * k), rgba(WHITE), rgba(DEEP), 9 * k,
                                   (0, 14 * k), 10 * k, anchor="ms"))
    # tagline pill
    fr = ImageFont.truetype(os.path.join(FONTS, "fredoka.ttf"), 30 * k)
    try:
        fr.set_variation_by_name("Bold")
    except Exception:
        pass
    tag = "Tap  •  Pop  •  Sort!"
    tw = fr.getlength(tag)
    pw, ph = tw + 56 * k, 54 * k
    px0, py0 = cxl * k - pw / 2, 398 * k
    pill = Image.new("RGBA", big, (0, 0, 0, 0))
    pd = ImageDraw.Draw(pill)
    pd.rounded_rectangle((px0, py0 + 6 * k, px0 + pw, py0 + ph + 6 * k), radius=ph / 2, fill=rgba("#9E1E5C"))
    pd.rounded_rectangle((px0, py0, px0 + pw, py0 + ph), radius=ph / 2, fill=rgba(PINK),
                         outline=rgba(INK), width=4 * k)
    pd.rounded_rectangle((px0 + 18 * k, py0 + 8 * k, px0 + pw - 18 * k, py0 + 20 * k), radius=6 * k,
                         fill=rgba(WHITE, 0.35))
    pd.text((cxl * k, py0 + ph / 2 + 1 * k), tag, font=fr, fill=rgba(WHITE), anchor="mm",
            stroke_width=2 * k, stroke_fill=rgba("#B0256A"))
    img.alpha_composite(pill)
    return img.resize((W, H), Image.LANCZOS).convert("RGB")


# --------------------------------------------------------------------------
def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)
    print("wrote", os.path.relpath(path, ROOT))


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)
    print("wrote", os.path.relpath(path, ROOT))


def main():
    # The launcher icon, in-app logo and store icon now come from art/app_icon_source.png via
    # tools/set_app_icon.py (2026-11-25). This script only renders the Play Store feature graphic;
    # the old vector icon code above is kept for reference and preview().
    save(feature_graphic(), os.path.join(STORE, "feature_1024x500.png"))


def preview(out_dir):
    """Debug helper: renders the vector layers (as a launcher would) into out_dir."""
    os.makedirs(out_dir, exist_ok=True)
    full = (0, 0, 108, 108)
    bg = render(icon_background(), full, (432, 432)).resize((432, 432), Image.LANCZOS)
    fg = render(icon_foreground(), full, (432, 432)).resize((432, 432), Image.LANCZOS)
    comp = bg.copy(); comp.alpha_composite(fg)
    d = ImageDraw.Draw(comp)
    d.ellipse((216 - 132, 216 - 132, 216 + 132, 216 + 132), outline=(255, 0, 0, 160), width=1)  # 66dp safe zone
    comp.save(os.path.join(out_dir, "adaptive.png"))
    for name, items in (("mono", monochrome()), ("splash", splash_logo())):
        im = render(items, full, (432, 432)).resize((432, 432), Image.LANCZOS)
        b = Image.new("RGBA", im.size, rgba("#3A2A70") if name == "mono" else rgba("#FFF4FB"))
        b.alpha_composite(im)
        ImageDraw.Draw(b).ellipse((216 - 144, 216 - 144, 216 + 144, 216 + 144), outline=(255, 0, 0, 160), width=1)
        b.save(os.path.join(out_dir, name + ".png"))


if __name__ == "__main__":
    import sys
    if len(sys.argv) > 2 and sys.argv[1] == "--preview":
        preview(sys.argv[2])
    else:
        main()
