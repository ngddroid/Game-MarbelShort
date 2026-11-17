#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gen_sounds.py -- original procedural SFX + music for "Marble Pop Factory".

Pure Python standard library only (math, random, wave, array). No samples,
no downloads: every sound is synthesized from oscillators, damped modal
resonators, filtered noise, envelopes and a small Schroeder reverb.

Output: 16-bit mono WAV @ 22050 Hz into app/src/main/res/raw/

Usage:
    python3 tools/gen_sounds.py            # generate everything
    python3 tools/gen_sounds.py sfx_pop    # generate only the named sounds
"""
import array
import math
import os
import random
import sys
import time
import wave

SR = 22050
TWO_PI = 2.0 * math.pi
NYQ_SAFE = SR * 0.45
HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.normpath(os.path.join(HERE, '..', 'app', 'src', 'main', 'res', 'raw'))


# =============================================================================
#  Basic helpers
# =============================================================================

def secs(t):
    return max(0, int(round(t * SR)))


def mtof(m):
    return 440.0 * 2.0 ** ((m - 69) / 12.0)


def dbg(x):
    return 10.0 ** (x / 20.0)


def zeros(n):
    return [0.0] * n


def add(dst, src, start=0, gain=1.0):
    """Mix src into dst at sample offset `start` (clipped to dst)."""
    if start < 0:
        src = src[-start:]
        start = 0
    end = min(len(dst), start + len(src))
    if end <= start:
        return dst
    seg = dst[start:end]
    if gain == 1.0:
        dst[start:end] = [a + b for a, b in zip(seg, src)]
    else:
        dst[start:end] = [a + b * gain for a, b in zip(seg, src)]
    return dst


def add_at(dst, src, t, gain=1.0):
    return add(dst, src, secs(t), gain)


def add_wrap(dst, src, start, gain=1.0):
    """Mix src into a circular (looping) buffer -- tails wrap to the start."""
    n = len(dst)
    start %= n
    pos = 0
    ls = len(src)
    while pos < ls:
        chunk = src[pos:pos + (n - start)]
        add(dst, chunk, start, gain)
        pos += len(chunk)
        start = 0


def scale(x, g):
    return [v * g for v in x]


def mul(x, e):
    return [a * b for a, b in zip(x, e)]


def mix(*pairs):
    """mix((buf, gain), (buf, gain), ...) -> new buffer (length of longest)."""
    n = max(len(b) for b, _ in pairs)
    out = zeros(n)
    for b, g in pairs:
        add(out, b, 0, g)
    return out


def noise(n, rng):
    r = rng.random
    return [2.0 * r() - 1.0 for _ in range(n)]


def peak(x):
    return max(abs(v) for v in x) if x else 0.0


def soft_clip(x, drive=1.0):
    t = math.tanh
    k = 1.0 / t(drive)
    return [t(drive * v) * k for v in x]


# =============================================================================
#  Envelopes
# =============================================================================

def env_perc(n, attack, tau):
    """Linear attack then exponential decay (time constant tau seconds)."""
    out = zeros(n)
    a = max(1, secs(attack))
    for i in range(min(a, n)):
        out[i] = (i + 1) / a
    k = math.exp(-1.0 / (max(tau, 1e-5) * SR))
    v = 1.0
    for i in range(a, n):
        out[i] = v
        v *= k
    return out


def env_points(n, pts):
    """Piecewise-linear envelope from [(time_s, value), ...]."""
    out = zeros(n)
    pts = sorted(pts)
    for j in range(len(pts) - 1):
        t0, v0 = pts[j]
        t1, v1 = pts[j + 1]
        i0, i1 = secs(t0), secs(t1)
        if i1 <= i0:
            continue
        dv = (v1 - v0) / (i1 - i0)
        for i in range(max(0, i0), min(n, i1)):
            out[i] = v0 + dv * (i - i0)
    tl, vl = pts[-1]
    for i in range(max(0, secs(tl)), n):
        out[i] = vl
    return out


def fade(x, fin=0.0, fout=0.0):
    n = len(x)
    a = min(n, secs(fin))
    for i in range(a):
        x[i] *= i / a
    b = min(n, secs(fout))
    for i in range(b):
        x[n - 1 - i] *= i / b
    return x


def release_tail(x, gate_n, rel):
    """Exponential release starting at gate_n samples (note-off)."""
    if gate_n >= len(x):
        return x
    k = math.exp(-1.0 / (max(rel, 1e-4) * SR))
    v = 1.0
    for i in range(gate_n, len(x)):
        v *= k
        x[i] *= v
    return x


# =============================================================================
#  Oscillators / resonators
# =============================================================================

def sine(freq, n, phase=0.0):
    w = TWO_PI * freq / SR
    s = math.sin
    return [s(phase + w * i) for i in range(n)]


def glide_freqs(f0, f1, n, glide_n=None, exp_curve=True):
    g = max(1, min(n, glide_n if glide_n else n))
    out = zeros(n)
    if exp_curve:
        r = (f1 / f0) ** (1.0 / g)
        f = f0
        for i in range(g):
            out[i] = f
            f *= r
    else:
        d = (f1 - f0) / g
        for i in range(g):
            out[i] = f0 + d * i
    for i in range(g, n):
        out[i] = f1
    return out


def osc(freqs, harm=((1, 1.0),), phase=0.0):
    """Phase-accumulating additive oscillator driven by per-sample freq list."""
    out = zeros(len(freqs))
    ph = phase
    k = TWO_PI / SR
    s = math.sin
    harm = tuple(harm)
    if len(harm) == 1 and harm[0][0] == 1:
        a0 = harm[0][1]
        for i, f in enumerate(freqs):
            out[i] = a0 * s(ph)
            ph += k * f
        return out
    for i, f in enumerate(freqs):
        v = 0.0
        for h, a in harm:
            v += a * s(h * ph)
        out[i] = v
        ph += k * f
    return out


def modal(freq, partials, n, amp=1.0):
    """Bank of exponentially-damped sine resonators.
    partials: [(ratio, amp, tau_seconds), ...]. Starts at zero phase (click free)."""
    out = zeros(n)
    for ratio, a, tau in partials:
        f = freq * ratio
        if f >= NYQ_SAFE or a == 0.0 or f <= 0:
            continue
        w = TWO_PI * f / SR
        r = math.exp(-1.0 / (tau * SR))
        c = 2.0 * r * math.cos(w)
        r2 = r * r
        m = min(n, int(tau * SR * 7.0) + 2)
        if m < 3:
            continue
        y2 = 0.0
        y1 = a * amp * r * math.sin(w)
        out[1] += y1
        for i in range(2, m):
            y = c * y1 - r2 * y2
            out[i] += y
            y2 = y1
            y1 = y
    return out


def tuned(partials, tau):
    """Scale a (ratio, amp, tau_multiplier) template by a base decay time."""
    return [(r, a, tau * m) for r, a, m in partials]


GLOCK = [(1.0, 1.0, 1.0), (2.76, 0.32, 0.30), (5.40, 0.13, 0.12), (8.93, 0.05, 0.06)]
MARIMBA = [(1.0, 1.0, 1.0), (3.93, 0.28, 0.20), (9.20, 0.06, 0.07)]
VIBES = [(1.0, 1.0, 1.0), (4.0, 0.22, 0.30), (10.0, 0.035, 0.10)]
BELL = [(1.0, 1.0, 1.0), (2.0, 0.45, 0.55), (3.0, 0.22, 0.35), (4.2, 0.18, 0.25), (5.4, 0.08, 0.15)]


def bell(freq, dur, tau, amp=1.0, tmpl=GLOCK):
    return modal(freq, tuned(tmpl, tau), secs(dur), amp)


# =============================================================================
#  Filters
# =============================================================================

def lp1(x, fc):
    a = math.exp(-TWO_PI * fc / SR)
    b = 1.0 - a
    y = 0.0
    out = zeros(len(x))
    for i, v in enumerate(x):
        y = b * v + a * y
        out[i] = y
    return out


def hp1(x, fc):
    lo = lp1(x, fc)
    return [a - b for a, b in zip(x, lo)]


def dc_block(x, fc=18.0, prime=None):
    """DC blocker. `prime` = samples to run first (for seamless loops)."""
    R = math.exp(-TWO_PI * fc / SR)
    xp = 0.0
    y = 0.0
    if prime:
        for v in prime:
            y = v - xp + R * y
            xp = v
    out = zeros(len(x))
    for i, v in enumerate(x):
        y = v - xp + R * y
        xp = v
        out[i] = y
    return out


def svf(x, cutoff, q=0.707, mode='lp', block=16):
    """Zavalishin TPT state-variable filter (stable for any cutoff).
    cutoff: Hz (float) or per-sample list. mode: lp | bp (unity peak) | hp."""
    n = len(x)
    out = zeros(n)
    k = 1.0 / q
    ic1 = ic2 = 0.0
    tan = math.tan
    pi_sr = math.pi / SR
    hi = SR * 0.47
    const = not isinstance(cutoff, list)

    def co(fc):
        fc = 10.0 if fc < 10.0 else (hi if fc > hi else fc)
        g = tan(pi_sr * fc)
        a1 = 1.0 / (1.0 + g * (g + k))
        a2 = g * a1
        return a1, a2, g * a2

    a1, a2, a3 = co(cutoff if const else cutoff[0])
    m = 0 if mode == 'lp' else (1 if mode == 'bp' else 2)
    for i in range(n):
        if not const and i % block == 0:
            a1, a2, a3 = co(cutoff[i])
        v0 = x[i]
        v3 = v0 - ic2
        v1 = a1 * ic1 + a2 * v3
        v2 = ic2 + a2 * ic1 + a3 * v3
        ic1 = 2.0 * v1 - ic1
        ic2 = 2.0 * v2 - ic2
        if m == 0:
            out[i] = v2
        elif m == 1:
            out[i] = k * v1
        else:
            out[i] = v0 - k * v1 - v2
    return out


# =============================================================================
#  Reverb (Schroeder / Freeverb style, mono) + echo
# =============================================================================

COMBS = [1116, 1188, 1277, 1356, 1422, 1491]
APS = [556, 441, 341]


def _reverb_core(inp, room, damp):
    n = len(inp)
    sc = SR / 44100.0
    acc = zeros(n)
    d1 = damp
    d2 = 1.0 - damp
    for L0 in COMBS:
        L = int(L0 * sc)
        y = list(inp)
        filt = 0.0
        for i in range(L, n):
            filt = y[i - L] * d2 + filt * d1
            y[i] += filt * room
        # comb output is the delayed line
        acc[L:] = [a + b for a, b in zip(acc[L:], y[:n - L])]
    for L0 in APS:
        L = int(L0 * sc)
        b = list(acc)
        for i in range(L, n):
            b[i] += 0.5 * b[i - L]
        acc = [(b[i - L] if i >= L else 0.0) - acc[i] for i in range(n)]
    g = 1.0 / len(COMBS)
    return [v * g for v in acc]


def reverb(x, wet=0.25, room=0.80, damp=0.35, tail=0.6, predelay=0.0):
    """Returns dry + wet with an added tail of `tail` seconds."""
    inp = x + zeros(secs(tail) + secs(predelay))
    wv = _reverb_core(inp, room, damp)
    out = list(inp)
    add(out, wv, secs(predelay), wet)
    return out


def reverb_loop(x, room=0.82, damp=0.4, prime_s=3.5):
    """Wet-only reverb of a looping buffer: primed with the loop's end so the
    tail wraps seamlessly into the start."""
    p = min(len(x), secs(prime_s))
    inp = x[len(x) - p:] + x
    wv = _reverb_core(inp, room, damp)
    return wv[p:]


def echo(x, delay, fb=0.35, wet=0.3, tail=0.5, lp=3500.0):
    d = secs(delay)
    y = x + zeros(secs(tail))
    e = zeros(len(y))
    a = math.exp(-TWO_PI * lp / SR)
    b = 1.0 - a
    s = 0.0
    for i in range(d, len(y)):
        s = b * (y[i - d] + e[i - d] * fb) + a * s
        e[i] = s
    return [v + w * wet for v, w in zip(y, e)]


# =============================================================================
#  Output
# =============================================================================

def finalize_sfx(x, peak_db=-1.0, fin=0.0008, fout=0.015):
    x = fade(dc_block(x), fin, fout)
    p = peak(x) or 1.0
    g = dbg(peak_db) / p
    return [v * g for v in x]


def write_wav(name, x):
    path = os.path.join(OUT_DIR, name + '.wav')
    data = array.array('h', [int(round((1.0 if v > 1.0 else (-1.0 if v < -1.0 else v)) * 32767.0))
                             for v in x])
    if sys.byteorder == 'big':
        data.byteswap()
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())
    return path


# =============================================================================
#  Reusable SFX voices
# =============================================================================

def glass_click(freq, rng, amp=1.0, dur=0.05):
    """Small glass marble click: inharmonic high partials + tiny transient."""
    n = secs(dur)
    tone = modal(freq, [(1.0, 1.0, 0.011), (1.53, 0.6, 0.008), (2.21, 0.42, 0.006),
                        (2.98, 0.25, 0.004)], n)
    tr = svf(mul(noise(n, rng), env_perc(n, 0.0002, 0.0012)), 6500.0, 0.9, 'bp')
    knock = modal(freq * 0.31, [(1.0, 0.35, 0.006)], n)
    return [(a * 0.55 + b * 0.7 + c) * amp for a, b, c in zip(tone, tr, knock)]


def bubble(f0, f1, dur, glide, tau, rng=None):
    """Rising-pitch sine with fast decay -- the classic bubbly 'bloop'."""
    n = secs(dur)
    fr = glide_freqs(f0, f1, n, secs(glide))
    s = osc(fr, ((1, 1.0), (2, 0.12)))
    return mul(s, env_perc(n, 0.0015, tau))


def noise_hit(n, rng, fc, q, tau, mode='bp', attack=0.0004):
    return svf(mul(noise(n, rng), env_perc(n, attack, tau)), fc, q, mode)


def thump(f0, f1, dur, tau, glide=0.06):
    n = secs(dur)
    s = osc(glide_freqs(f0, f1, n, secs(glide)))
    return mul(s, env_perc(n, 0.002, tau))


# =============================================================================
#  SFX
# =============================================================================

def sfx_tap():
    rng = random.Random(11)
    n = secs(0.06)
    blip = modal(1450.0, [(1.0, 1.0, 0.010), (2.32, 0.35, 0.005)], n)
    body = modal(430.0, [(1.0, 1.0, 0.016), (2.7, 0.2, 0.008)], n)
    nz = noise_hit(n, rng, 3800.0, 1.1, 0.0025)
    return finalize_sfx(mix((blip, 0.55), (body, 0.45), (nz, 0.45)), fout=0.008)


def sfx_clack():
    rng = random.Random(12)
    x = glass_click(3000.0, rng, dur=0.05)
    return finalize_sfx(x, fout=0.006)


def sfx_pop():
    rng = random.Random(13)
    total = secs(0.62)
    out = zeros(total)
    # main bubbly plastic pop
    add(out, bubble(260.0, 920.0, 0.16, 0.045, 0.040), 0, 0.9)
    add_at(out, bubble(420.0, 1350.0, 0.12, 0.035, 0.028), 0.028, 0.5)
    # air puff
    n = secs(0.08)
    add(out, noise_hit(n, rng, 1100.0, 0.7, 0.012, 'lp', 0.0008), 0, 0.8)
    # plastic body resonance
    add(out, modal(540.0, [(1.0, 1.0, 0.035), (2.71, 0.35, 0.015), (4.1, 0.15, 0.008)], secs(0.2)), 0, 0.35)
    # spray of marble clicks
    t = 0.045
    amp = 0.55
    for _ in range(9):
        f = rng.uniform(2300.0, 3900.0)
        add_at(out, glass_click(f, rng, amp * rng.uniform(0.7, 1.0)), t)
        t += rng.uniform(0.018, 0.05)
        amp *= 0.82
    out = reverb(out, wet=0.16, room=0.72, damp=0.45, tail=0.0)
    return finalize_sfx(out, fout=0.06)


def sfx_plink():
    rng = random.Random(14)
    n = secs(0.22)
    f = 1318.5  # E6
    # tiny downward pitch blip into the marimba-ish note (glass on plastic)
    head = mul(osc(glide_freqs(f * 1.25, f, n, secs(0.010))), env_perc(n, 0.0005, 0.012))
    note = modal(f, tuned(MARIMBA, 0.075), n)
    drop = mul(osc(glide_freqs(900.0, 1900.0, n, secs(0.035))), env_perc(n, 0.002, 0.022))
    cup = modal(620.0, [(1.0, 1.0, 0.012), (2.4, 0.3, 0.006)], n)
    tick = noise_hit(n, rng, 5000.0, 1.0, 0.0015)
    x = mix((note, 0.8), (head, 0.25), (drop, 0.18), (cup, 0.25), (tick, 0.25))
    x = reverb(x, wet=0.14, room=0.68, damp=0.5, tail=0.0)
    return finalize_sfx(x, fout=0.04)


def sfx_cup_complete():
    rng = random.Random(15)
    out = zeros(secs(0.46))
    add(out, bubble(300.0, 820.0, 0.12, 0.04, 0.035), 0, 0.55)
    for i, (t, m) in enumerate([(0.0, 84), (0.065, 88), (0.13, 91)]):  # C6 E6 G6
        add_at(out, bell(mtof(m), 0.33, 0.20 + 0.04 * i), t, 0.55 + 0.1 * i)
        add_at(out, bell(mtof(m + 12), 0.2, 0.08), t, 0.12)
    for _ in range(5):
        m = rng.choice([96, 100, 103, 108])
        add_at(out, bell(mtof(m), 0.15, 0.05), rng.uniform(0.15, 0.32), rng.uniform(0.08, 0.15))
    out = reverb(out, wet=0.22, room=0.78, damp=0.4, tail=0.0)
    return finalize_sfx(out, fout=0.08)


def sfx_combo():
    rng = random.Random(16)
    out = zeros(secs(0.52))
    notes = [84, 88, 91, 96, 100]  # C6 E6 G6 C7 E7
    for i, m in enumerate(notes):
        t = i * 0.042
        add_at(out, bell(mtof(m), 0.4, 0.14 + 0.03 * i), t, 0.5 + 0.06 * i)
        add_at(out, modal(mtof(m - 12), tuned(MARIMBA, 0.06), secs(0.15)), t, 0.25)
    # shimmering swish
    n = secs(0.3)
    sw = svf(mul(noise(n, rng), env_points(n, [(0, 0), (0.12, 1), (0.3, 0)])),
             [6000.0 + 3000.0 * i / n for i in range(n)], 2.0, 'bp')
    add_at(out, sw, 0.05, 0.12)
    out = reverb(out, wet=0.22, room=0.75, damp=0.35, tail=0.0)
    return finalize_sfx(out, fout=0.1)


def ep_note(freq, dur, vel=1.0, tau=0.9, rel=0.08):
    """Soft FM electric piano voice."""
    gate = secs(dur)
    n = gate + secs(rel * 6)
    out = zeros(n)
    w = TWO_PI * freq / SR
    kd = math.exp(-1.0 / (tau * SR))
    kr = math.exp(-1.0 / (rel * SR))
    ki = math.exp(-1.0 / (0.22 * SR))
    I = 1.3 * vel + 0.2
    a = 1.0
    s = math.sin
    att = secs(0.003)
    for i in range(n):
        ph = w * i
        out[i] = a * s(ph + (I + 0.18) * s(ph))
        a *= kd if i < gate else kr
        I *= ki
    for i in range(min(att, n)):
        out[i] *= i / att
    tine = modal(freq, [(7.0, 0.05 * vel, 0.03)], n)
    return [v + t for v, t in zip(out, tine)]


def sfx_win():
    rng = random.Random(17)
    out = zeros(secs(2.3))
    lead = [(0.00, 72, 0.11), (0.11, 76, 0.11), (0.22, 79, 0.11), (0.33, 84, 0.30),
            (0.66, 81, 0.11), (0.77, 83, 0.11), (0.88, 84, 1.2)]
    for t, m, d in lead:
        fin = d > 1.0
        add_at(out, modal(mtof(m), tuned(MARIMBA, 0.5 if fin else 0.22), secs(d + 0.4)), t, 0.55)
        add_at(out, bell(mtof(m + 12), d + 0.3, 0.6 if fin else 0.15), t, 0.22)
    # harmony stabs (C, F, G, C)
    chords = [(0.0, [60, 64, 67], 0.3), (0.33, [64, 67, 72], 0.3), (0.66, [65, 69, 72], 0.11),
              (0.77, [67, 71, 74], 0.11), (0.88, [60, 64, 67, 72], 1.2)]
    for t, ns, d in chords:
        for j, m in enumerate(ns):
            add_at(out, ep_note(mtof(m), d, 0.7, tau=0.7 if d > 1 else 0.3), t + j * 0.006, 0.14)
    for t, m, d in [(0.0, 48, 0.3), (0.33, 55, 0.3), (0.66, 41, 0.11), (0.77, 43, 0.11), (0.88, 36, 1.0)]:
        x = modal(mtof(m), tuned([(1, 1, 1), (2, 0.5, 0.6), (3, 0.3, 0.4), (4, 0.15, 0.3)], 0.35), secs(d + 0.4))
        add_at(out, release_tail(x, secs(d), 0.05), t, 0.35)
    # soft kicks + shimmer
    add(out, thump(140, 50, 0.25, 0.09), 0, 0.5)
    add_at(out, thump(140, 50, 0.25, 0.09), 0.88, 0.6)
    n = secs(1.3)
    cym = svf(mul(noise(n, rng), env_perc(n, 0.002, 0.35)), 7000.0, 0.7, 'hp')
    add_at(out, cym, 0.88, 0.08)
    # sparkle tail
    for k in range(16):
        t = 0.95 + k * 0.06 + rng.uniform(-0.02, 0.02)
        m = rng.choice([96, 98, 100, 103, 105, 108])
        add_at(out, bell(mtof(m), 0.3, 0.09), t, 0.17 * (1.0 - k / 18.0))
    out = reverb(out, wet=0.24, room=0.8, damp=0.35, tail=0.0)
    out = out[:secs(2.3)]
    fade(out, 0, 0.35)
    return finalize_sfx(out, fout=0.35)


def sfx_fail():
    rng = random.Random(18)
    dur = 1.25
    n = secs(dur)
    # three descending "wah" notes then a sagging, wobbling last one
    segs = [(0.0, 0.26, mtof(72)), (0.27, 0.53, mtof(71)), (0.54, 1.25, mtof(70))]
    fr = zeros(n)
    amp = zeros(n)
    cut = zeros(n)
    for t0, t1, f in segs:
        i0, i1 = secs(t0), min(n, secs(t1))
        L = i1 - i0
        last = t1 >= dur
        for i in range(i0, i1):
            u = (i - i0) / SR
            p = (i - i0) / L
            vib_depth = 0.004 + (0.03 * p if last else 0.008 * p)
            bend = (1.0 - 0.06 * p * p) if last else (1.0 - 0.015 * p)
            fr[i] = f * bend * (1.0 + vib_depth * math.sin(TWO_PI * 5.5 * u))
            a = min(1.0, u / 0.03)
            a *= (1.0 - p) ** 1.4 if last else (1.0 - 0.35 * p)
            if not last:
                a *= min(1.0, (i1 - i) / (0.025 * SR))
            amp[i] = a
            cut[i] = 500.0 + 1700.0 * math.sin(math.pi * min(1.0, p * (1.6 if last else 1.0)) ** 0.7) ** 0.8
    for i in range(n):  # fill tiny gaps continuously
        if fr[i] == 0.0:
            fr[i] = fr[i - 1] if i else mtof(72)
    tone = osc(fr, ((1, 1.0), (2, 0.45), (3, 0.3), (4, 0.12), (5, 0.1)))
    tone = svf(tone, cut, 1.6, 'lp')
    x = mul(tone, amp)
    x = reverb(x, wet=0.15, room=0.7, damp=0.5, tail=0.0)
    return finalize_sfx(x[:n], fout=0.08)


def sfx_coin():
    out = zeros(secs(0.27))
    a = modal(mtof(83), tuned(BELL, 0.06), secs(0.065))  # B5
    fade(a, 0, 0.008)
    add(out, a, 0, 0.8)
    add_at(out, modal(mtof(88), tuned(BELL, 0.13), secs(0.21)), 0.06, 1.0)  # E6
    add_at(out, bell(mtof(100), 0.18, 0.06), 0.06, 0.18)
    out = reverb(out, wet=0.12, room=0.7, damp=0.4, tail=0.0)
    return finalize_sfx(out, fout=0.04)


def sfx_button():
    rng = random.Random(19)
    n = secs(0.08)
    b = bubble(520.0, 1150.0, 0.08, 0.028, 0.017)
    body = modal(320.0, [(1.0, 1.0, 0.010)], n)
    tick = noise_hit(n, rng, 4200.0, 1.0, 0.0015)
    return finalize_sfx(mix((b, 0.8), (body, 0.3), (tick, 0.3)), fout=0.015)


def sfx_whoosh():
    rng = random.Random(20)
    n = secs(0.36)
    env = [math.sin(math.pi * min(1.0, (i / n) ** 0.75)) ** 2 for i in range(n)]
    cut = [350.0 * (3000.0 / 350.0) ** math.sin(math.pi * 0.5 * min(1.0, i / (0.55 * n)))
           if i < 0.55 * n else 3000.0 - 1800.0 * (i - 0.55 * n) / (0.45 * n) for i in range(n)]
    a = svf(noise(n, rng), cut, 1.4, 'bp')
    b = svf(noise(n, rng), [c * 0.6 for c in cut], 0.8, 'bp')
    x = mul(mix((a, 0.7), (b, 0.5)), env)
    return finalize_sfx(x, fin=0.004, fout=0.03)


def sfx_star():
    rng = random.Random(21)
    out = zeros(secs(0.45))
    add(out, thump(150.0, 55.0, 0.2, 0.06, 0.05), 0, 0.85)
    add(out, noise_hit(secs(0.06), rng, 900.0, 0.7, 0.010, 'lp', 0.001), 0, 0.5)
    add_at(out, bell(mtof(91), 0.4, 0.22), 0.025, 0.55)   # G6
    add_at(out, bell(mtof(98), 0.35, 0.16), 0.04, 0.3)    # D7
    n = secs(0.22)
    sw = svf(mul(noise(n, rng), env_points(n, [(0, 0), (0.06, 1), (0.22, 0)])),
             [3000.0 * (2.7 ** (i / n)) for i in range(n)], 2.5, 'bp')
    add_at(out, sw, 0.03, 0.12)
    out = reverb(out, wet=0.2, room=0.75, damp=0.4, tail=0.0)
    return finalize_sfx(out, fout=0.07)


def metal_clink(freq, n, tau=0.12):
    return modal(freq, [(1.0, 1.0, tau), (1.47, 0.7, tau * 0.8), (2.09, 0.5, tau * 0.6),
                        (2.56, 0.35, tau * 0.45), (3.11, 0.2, tau * 0.3)], n)


def sfx_unlock():
    rng = random.Random(22)
    out = zeros(secs(0.52))
    add(out, metal_clink(2350.0, secs(0.4), 0.10), 0, 0.5)
    add(out, noise_hit(secs(0.03), rng, 6000.0, 1.0, 0.002), 0, 0.35)
    add_at(out, metal_clink(2100.0, secs(0.3), 0.06), 0.055, 0.3)
    # unlatch: clunk + chk
    add_at(out, noise_hit(secs(0.08), rng, 700.0, 2.0, 0.012), 0.17, 0.9)
    add_at(out, thump(190.0, 120.0, 0.1, 0.03, 0.03), 0.17, 0.6)
    add_at(out, noise_hit(secs(0.03), rng, 2600.0, 1.5, 0.004), 0.235, 0.55)
    add_at(out, modal(900.0, [(1, 1, 0.012), (2.3, 0.4, 0.006)], secs(0.05)), 0.235, 0.3)
    # chain rattle
    t = 0.29
    for k in range(4):
        add_at(out, metal_clink(rng.uniform(2800.0, 4000.0), secs(0.15), 0.035), t, 0.28 * (0.8 ** k))
        t += rng.uniform(0.035, 0.06)
    add_at(out, bell(mtof(88), 0.2, 0.08), 0.26, 0.12)
    out = reverb(out, wet=0.12, room=0.65, damp=0.5, tail=0.0)
    return finalize_sfx(out, fout=0.05)


def sfx_ice_crack():
    rng = random.Random(23)
    N = secs(0.32)
    out = zeros(N)
    # crunchy body
    body = svf(mul(noise(N, rng), env_points(N, [(0, 0), (0.002, 1), (0.05, 0.45), (0.3, 0)])),
               1300.0, 0.8, 'bp')
    add(out, body, 0, 0.45)
    # micro cracks, dense at the start
    for k in range(38):
        t = (rng.random() ** 2.2) * 0.24
        n = secs(rng.uniform(0.002, 0.006))
        a = (1.0 - t / 0.26) * rng.uniform(0.4, 1.0)
        c = svf(mul(noise(n, rng), env_perc(n, 0.0001, 0.0012)), 2500.0, 0.7, 'hp')
        add_at(out, c, t, a * 0.9)
        if rng.random() < 0.3:
            add_at(out, modal(rng.uniform(3000.0, 6500.0), [(1, 1, 0.012), (1.6, 0.5, 0.007)], secs(0.06)),
                   t, a * 0.25)
    add(out, thump(220.0, 140.0, 0.08, 0.018, 0.02), 0, 0.35)
    out = reverb(out, wet=0.1, room=0.6, damp=0.5, tail=0.0)
    return finalize_sfx(out, fout=0.05)


def woodblock(freq, rng, n):
    a = modal(freq, [(1.0, 1.0, 0.018), (2.6, 0.4, 0.008), (4.1, 0.15, 0.004)], n)
    b = noise_hit(n, rng, freq * 3.0, 1.2, 0.0015)
    return [x + 0.4 * y for x, y in zip(a, b)]


def sfx_bomb_tick():
    rng = random.Random(24)
    out = zeros(secs(0.155))
    add(out, woodblock(1250.0, rng, secs(0.07)), 0, 1.0)
    add_at(out, woodblock(930.0, rng, secs(0.075)), 0.075, 0.85)
    return finalize_sfx(out, fout=0.012)


def sfx_explode():
    rng = random.Random(25)
    N = secs(0.72)
    nz = noise(N, rng)
    cut = [3200.0 * (160.0 / 3200.0) ** min(1.0, i / (0.55 * SR)) for i in range(N)]
    body = svf(nz, cut, 0.9, 'lp')
    body = mul(body, env_perc(N, 0.004, 0.17))
    sub = thump(95.0, 34.0, 0.72, 0.22, 0.25)
    pop = bubble(320.0, 110.0, 0.15, 0.08, 0.05)
    x = mix((body, 1.0), (sub, 0.9), (pop, 0.35))
    x = soft_clip(x, 1.6)
    x = lp1(x, 4000.0)
    x = reverb(x, wet=0.18, room=0.75, damp=0.55, tail=0.0)
    return finalize_sfx(x[:N], fin=0.002, fout=0.12)


def sfx_spin_tick():
    rng = random.Random(26)
    n = secs(0.04)
    a = modal(1850.0, [(1.0, 1.0, 0.006), (1.68, 0.5, 0.004)], n)
    b = noise_hit(n, rng, 5000.0, 1.0, 0.001)
    c = modal(520.0, [(1.0, 1.0, 0.007)], n)
    return finalize_sfx(mix((a, 0.6), (b, 0.5), (c, 0.35)), fout=0.006)


def sfx_reward():
    rng = random.Random(27)
    out = zeros(secs(1.05))
    # rising swell
    n = secs(0.4)
    sw = svf(mul(noise(n, rng), env_points(n, [(0, 0), (0.3, 1), (0.4, 0)])),
             [700.0 * (8.0 ** (i / n)) for i in range(n)], 1.8, 'bp')
    add(out, sw, 0, 0.25)
    # harp-ish up arpeggio
    for i, m in enumerate([67, 72, 76, 79, 84]):
        add_at(out, modal(mtof(m), tuned(MARIMBA, 0.25), secs(0.5)), i * 0.055, 0.3)
    # burst chord glow (C add9)
    for m in [84, 88, 91, 98]:
        add_at(out, bell(mtof(m), 0.75, 0.38), 0.3, 0.3)
    for j, m in enumerate([60, 64, 67, 74]):
        add_at(out, ep_note(mtof(m), 0.55, 0.6, tau=0.6), 0.3 + j * 0.005, 0.12)
    add_at(out, thump(160, 60, 0.2, 0.06), 0.3, 0.35)
    # sparkles
    for k in range(26):
        t = 0.28 + (rng.random() ** 1.4) * 0.65
        m = rng.choice([96, 98, 100, 103, 105])
        add_at(out, bell(mtof(m), 0.2, 0.06), t, rng.uniform(0.06, 0.16) * (1.0 - (t - 0.28) / 0.8))
    out = reverb(out, wet=0.28, room=0.82, damp=0.35, tail=0.0)
    return finalize_sfx(out[:secs(1.05)], fout=0.2)


def sfx_warning():
    out = zeros(secs(0.42))
    for t, m in [(0.0, 81), (0.2, 76)]:  # A5 -> E5
        n = secs(0.17)
        f = mtof(m)
        s = osc([f] * n, ((1, 1.0), (2, 0.22), (3, 0.12)))
        env = env_points(n, [(0, 0), (0.012, 1), (0.12, 0.75), (0.17, 0)])
        trem = [1.0 - 0.15 * (0.5 - 0.5 * math.cos(TWO_PI * 18.0 * i / SR)) for i in range(n)]
        add_at(out, mul(mul(s, env), trem), t, 1.0)
    out = lp1(out, 3500.0)
    out = reverb(out, wet=0.1, room=0.6, damp=0.5, tail=0.0)
    return finalize_sfx(out[:secs(0.42)], fout=0.03)


# =============================================================================
#  Music engine
# =============================================================================

PAD_T = 4096
PAD_MASK = PAD_T - 1
PAD_TAB = [sum(a * math.sin(TWO_PI * h * i / PAD_T)
               for h, a in ((1, 1.0), (2, 0.42), (3, 0.24), (4, 0.12), (5, 0.07), (6, 0.04)))
           for i in range(PAD_T)]
_pk = max(abs(v) for v in PAD_TAB)
PAD_TAB = [v / _pk for v in PAD_TAB]


def pad_chord(freqs, dur, attack=0.45, rel=0.7, rng=None, cutoff=1600.0, detune=6.0):
    n = secs(dur + rel)
    out = zeros(n)
    tab = PAD_TAB
    M = PAD_MASK
    for f in freqs:
        for d in (-detune, detune):
            inc = f * 2.0 ** (d / 1200.0) * PAD_T / SR
            p0 = (rng.random() if rng else 0.0) * PAD_T
            v = [tab[int(p0 + inc * i) & M] for i in range(n)]
            out = [a + b for a, b in zip(out, v)]
    env = env_points(n, [(0, 0), (attack, 1.0), (dur, 0.8), (dur + rel, 0.0)])
    out = lp1(mul(out, env), cutoff)
    return [v / (2 * len(freqs)) for v in out]


class Loop:
    def __init__(self, bpm, bars, seed):
        self.spb = 60.0 / bpm * SR
        self.bars = bars
        self.n = int(round(bars * 4 * self.spb))
        self.dry = zeros(self.n)
        self.send = zeros(self.n)
        self.rng = random.Random(seed)

    def pos(self, bar, beat, human=0.0):
        p = (bar * 4 + beat) * self.spb
        if human:
            p += self.rng.gauss(0.0, human) * SR
        return int(round(p))

    def put(self, src, bar, beat, gain=1.0, send=0.0, human=0.004):
        p = self.pos(bar, beat, human)
        add_wrap(self.dry, src, p, gain)
        if send > 0.0:
            add_wrap(self.send, src, p, gain * send)

    def render(self, peak_db, room=0.82, damp=0.4, wet=1.0, drive=1.3):
        wv = reverb_loop(self.send, room, damp)
        x = [a + b * wet for a, b in zip(self.dry, wv)]
        p = secs(2.0)
        x = dc_block(x, 18.0, prime=x[-p:])
        pk = peak(x) or 1.0
        x = [v / pk for v in x]
        x = soft_clip(x, drive)   # gentle glue/limiting of drum peaks
        pk = peak(x) or 1.0
        g = dbg(peak_db) / pk
        return [v * g for v in x]


# ---- drum voices (pre-rendered variants) ------------------------------------

def make_kit(seed):
    rng = random.Random(seed)
    kit = {}
    kit['kick'] = []
    for _ in range(3):
        n = secs(0.3)
        k = thump(rng.uniform(125, 140), 46.0, 0.3, 0.11, 0.07)
        k2 = modal(92.0, [(2.0, 0.25, 0.03), (3.0, 0.12, 0.02)], n)  # harmonics -> audible on phones
        cl = noise_hit(n, rng, 1800.0, 0.7, 0.0015, 'lp')
        kit['kick'].append(fade([a + b + 0.25 * c for a, b, c in zip(k, k2, cl)], 0.001, 0.02))
    kit['shaker'] = []
    for _ in range(4):
        n = secs(0.09)
        e = env_points(n, [(0, 0), (0.008, 1.0), (0.02, 0.5), (0.09, 0.0)])
        s = svf(mul(noise(n, rng), e), 6500.0, 0.8, 'hp')
        kit['shaker'].append(svf(s, 9500.0, 0.7, 'lp'))
    kit['rim'] = []
    for _ in range(3):
        n = secs(0.06)
        a = modal(1750.0, [(1.0, 1.0, 0.012), (2.4, 0.3, 0.006)], n)
        b = noise_hit(n, rng, 2600.0, 1.4, 0.004)
        kit['rim'].append([x + 0.6 * y for x, y in zip(a, b)])
    kit['snap'] = []
    for _ in range(3):
        n = secs(0.16)
        e = zeros(n)
        for t0, g in ((0.0, 0.7), (0.009, 0.8), (0.018, 1.0)):
            add(e, env_perc(n - secs(t0), 0.0005, 0.006 if t0 < 0.018 else 0.045), secs(t0), g)
        s = svf(mul(noise(n, rng), e), 1700.0, 1.1, 'bp')
        kit['snap'].append(s)
    kit['tick'] = []
    for _ in range(2):
        n = secs(0.04)
        kit['tick'].append(svf(noise_hit(n, rng, 8000.0, 0.9, 0.006, 'hp'), 10000.0, 0.7, 'lp'))
    return kit


# ---- melodic voices -----------------------------------------------------------

def v_upright(freq, dur, vel):
    n = secs(dur + 0.25)
    x = modal(freq, [(1, 1.0, 0.55), (2, 0.55, 0.35), (3, 0.3, 0.22), (4, 0.14, 0.14), (5, 0.06, 0.08)], n)
    th = modal(freq * 0.5 + 40, [(1, 0.25, 0.02)], n)
    x = [a + b for a, b in zip(x, th)]
    x = release_tail(x, secs(dur), 0.04)
    for i in range(min(secs(0.004), n)):
        x[i] *= i / secs(0.004)
    return scale(x, vel)


def v_vibes(freq, dur, vel, trem_hz=5.2):
    n = secs(dur + 0.5)
    x = modal(freq, tuned(VIBES, 0.9), n)
    x = release_tail(x, secs(dur + 0.15), 0.12)
    w = TWO_PI * trem_hz / SR
    c = math.cos
    return [v * vel * (0.82 + 0.18 * c(w * i)) for i, v in enumerate(x)]


def v_marimba(freq, dur, vel):
    n = secs(min(dur, 0.6) + 0.3)
    x = modal(freq, tuned(MARIMBA, 0.32), n)
    x = release_tail(x, secs(dur + 0.08), 0.08)
    return scale(x, vel)


def v_glock(freq, dur, vel):
    n = secs(0.6)
    return scale(modal(freq, tuned(GLOCK, 0.35), n), vel)


def v_pluck(freq, dur, vel):
    n = secs(dur + 0.15)
    x = modal(freq, [(1, 1.0, 0.18), (2, 0.5, 0.10), (3, 0.28, 0.06), (4, 0.12, 0.04)], n)
    x = release_tail(x, secs(dur), 0.03)
    return scale(x, vel)


def v_synthbass(freq, dur, vel):
    n = secs(dur + 0.12)
    x = modal(freq, [(1, 1.0, 0.30), (2, 0.6, 0.16), (3, 0.42, 0.11), (4, 0.22, 0.08), (5, 0.12, 0.05),
                     (6, 0.06, 0.04)], n)
    x = release_tail(x, secs(dur), 0.025)
    for i in range(min(secs(0.003), n)):
        x[i] *= i / secs(0.003)
    return scale(x, vel)


# =============================================================================
#  music_home -- calm lounge / bossa, F major, 96 BPM, 12 bars = 30.0 s
# =============================================================================

HOME_CH = {
    'Fmaj7': (41, [53, 57, 60, 64]),
    'Dm7': (38, [50, 53, 57, 60]),
    'Gm7': (43, [55, 58, 62, 65]),
    'C9': (36, [58, 62, 64, 67]),
    'Am7': (45, [55, 60, 64, 67]),
    'D7': (38, [54, 57, 60, 64]),
    'C7sus': (36, [58, 60, 65, 67]),
    'Bbmaj7': (46, [57, 62, 65, 69]),
}
HOME_PROG = [
    [(0, 'Fmaj7')], [(0, 'Dm7')], [(0, 'Gm7')], [(0, 'C9')],
    [(0, 'Fmaj7')], [(0, 'Am7'), (2, 'D7')], [(0, 'Gm7')], [(0, 'C7sus'), (2, 'C9')],
    [(0, 'Bbmaj7')], [(0, 'Am7')], [(0, 'Gm7')], [(0, 'C9')],
]
HOME_MEL = [
    [(0.5, 72, .5), (1, 76, .5), (1.5, 77, .5), (2, 81, 1.5), (3.5, 79, .5)],
    [(0, 77, 1), (1, 76, .5), (1.5, 74, 1.5), (3, 72, 1)],
    [(0.5, 74, .5), (1, 77, .5), (1.5, 79, .5), (2, 82, 1), (3, 81, .5), (3.5, 79, .5)],
    [(0, 76, 1.5), (1.5, 74, .5), (2, 72, 1), (3, 70, 1)],
    [(0, 69, .5), (0.5, 72, .5), (1, 76, 1), (2, 77, .5), (2.5, 81, 1.5)],
    [(0, 79, .5), (0.5, 76, 1.5), (2, 78, .5), (2.5, 81, .5), (3, 84, 1)],
    [(0, 82, 1), (1, 81, .5), (1.5, 79, .5), (2, 77, 1), (3, 74, 1)],
    [(0, 77, 1.5), (2, 76, .5), (2.5, 79, .5), (3, 82, 1)],
    [(0, 81, 1.5), (1.5, 77, .5), (2, 74, 1), (3, 77, .5), (3.5, 81, .5)],
    [(0, 79, 1), (1, 76, .5), (1.5, 72, 1.5), (3.5, 74, .5)],
    [(0, 77, .5), (0.5, 79, .5), (1, 82, 1), (2, 81, .5), (2.5, 79, .5), (3, 77, 1)],
    [(0, 76, 1), (1, 79, .5), (1.5, 76, .5), (2, 74, .5), (2.5, 76, 1.5)],
]


def chord_at(prog_bar, beat):
    cur = prog_bar[0][1]
    for b, name in prog_bar:
        if beat >= b:
            cur = name
    return cur


def music_home():
    L = Loop(96, 12, seed=101)
    rng = L.rng
    kit = make_kit(202)
    for bar in range(L.bars):
        segs = HOME_PROG[bar]
        # --- pad + bass per chord segment
        for si, (b0, name) in enumerate(segs):
            b1 = segs[si + 1][0] if si + 1 < len(segs) else 4
            root, tones = HOME_CH[name]
            seg_beats = b1 - b0
            dur = seg_beats * L.spb / SR
            L.put(pad_chord([mtof(m) for m in tones[1:]], dur, 0.35, 0.6, rng, 1500.0),
                  bar, b0, 0.30, send=0.6, human=0)
            fifth = root + 7 if root + 7 <= 50 else root - 5
            if seg_beats >= 4:
                pat = [(0, root, 1.35, 1.0), (1.5, fifth, 0.42, 0.75), (2, fifth, 1.35, 0.9), (3.5, root, 0.42, 0.7)]
            else:
                pat = [(0, root, 1.35, 1.0), (1.5, fifth, 0.42, 0.75)]
            for bt, m, d, v in pat:
                L.put(v_upright(mtof(m), d * L.spb / SR, v), bar, b0 + bt, 0.28, send=0.05)
        # --- electric piano bossa comping
        comp = [(0, .8), (1.5, .4), (3, .8)] if bar % 2 == 0 else [(0.5, .4), (2, .8), (3.5, .4)]
        for bt, d in comp:
            root, tones = HOME_CH[chord_at(segs, bt)]
            vel = rng.uniform(0.75, 0.95)
            for j, m in enumerate(tones):
                L.put(ep_note(mtof(m), d * L.spb / SR, vel, tau=0.7), bar, bt + j * 0.012, 0.075, send=0.35)
        # --- vibes melody
        for bt, m, d in HOME_MEL[bar]:
            v = rng.uniform(0.8, 1.0)
            L.put(v_vibes(mtof(m), d * L.spb / SR, v), bar, bt, 0.28, send=0.45)
        # --- percussion
        for e in range(8):
            v = (0.55, 0.9, 0.6, 0.85, 0.55, 0.95, 0.6, 0.85)[e] * rng.uniform(0.85, 1.0)
            L.put(rng.choice(kit['shaker']), bar, e * 0.5, 0.11 * v, send=0.15, human=0.003)
        for bt, v in ((0, 1.0), (1.5, 0.45), (2, 0.8), (3.5, 0.4)):
            L.put(rng.choice(kit['kick']), bar, bt, 0.34 * v, human=0.002)
        clave = (0, 1.5, 3) if bar % 2 == 0 else (1, 2.5)
        for bt in clave:
            L.put(rng.choice(kit['rim']), bar, bt, 0.09, send=0.25, human=0.003)
    return L.render(-6.0, room=0.82, damp=0.4, wet=0.55), L.n


# =============================================================================
#  music_game -- bouncy light puzzle loop, C major, 128 BPM, 16 bars = 30.0 s
# =============================================================================

GAME_CH = {
    'C': (36, [60, 64, 67]),
    'Am': (45, [57, 60, 64]),
    'F': (41, [57, 60, 65]),
    'G': (43, [55, 59, 62]),
    'Em': (40, [55, 59, 64]),
    'Dm': (38, [57, 62, 65]),
    'G7': (43, [53, 59, 62]),
}
GAME_PROG = ['C', 'Am', 'F', 'G', 'C', 'Em', 'F', 'G', 'F', 'G', 'Em', 'Am', 'Dm', 'G', 'C', 'G7']
GAME_MEL = [
    [(0, 76, .5), (0.5, 79, .5), (1, 84, 1), (2.5, 81, .5), (3, 79, 1)],
    [(0, 72, .5), (0.5, 76, .5), (1, 81, 1), (2.5, 79, .5), (3, 76, 1)],
    [(0, 77, .5), (0.5, 81, .5), (1, 84, 1), (2.5, 86, .5), (3, 84, 1)],
    [(0, 83, .5), (0.5, 79, .5), (1, 74, 1), (2, 79, .5), (2.5, 81, .5), (3, 83, 1)],
    [(0, 76, .5), (0.5, 79, .5), (1, 84, 1), (2.5, 81, .5), (3, 79, 1)],
    [(0, 79, .5), (0.5, 83, .5), (1, 88, 1), (2.5, 86, .5), (3, 83, 1)],
    [(0, 81, .5), (0.5, 84, .5), (1, 81, .5), (1.5, 77, .5), (2, 72, 1), (3, 77, .5), (3.5, 81, .5)],
    [(0, 79, 1.5), (2, 74, .5), (2.5, 77, .5), (3, 83, 1)],
    [(0, 84, 1), (1, 81, .5), (1.5, 77, .5), (2.5, 81, .5), (3, 84, .5), (3.5, 86, .5)],
    [(0, 86, 1), (1, 83, .5), (1.5, 79, .5), (2.5, 83, .5), (3, 86, .5), (3.5, 88, .5)],
    [(0, 88, 1.5), (1.5, 86, .5), (2, 83, 1), (3, 79, 1)],
    [(0, 81, 1.5), (1.5, 79, .5), (2, 76, 1), (3, 72, 1)],
    [(0, 74, .5), (0.5, 77, .5), (1, 81, 1), (2.5, 84, .5), (3, 81, 1)],
    [(0, 79, .5), (0.5, 83, .5), (1, 86, 1), (2.5, 83, .5), (3, 79, 1)],
    [(0, 84, 1), (1, 79, .5), (1.5, 76, .5), (2, 72, 2)],
    [(1, 74, .5), (1.5, 77, .5), (2, 79, .5), (2.5, 83, .5), (3, 86, .5), (3.5, 79, .5)],
]


def music_game():
    L = Loop(128, 16, seed=303)
    rng = L.rng
    kit = make_kit(404)
    sec = L.spb / SR
    for bar in range(L.bars):
        name = GAME_PROG[bar]
        root, tones = GAME_CH[name]
        # pad
        L.put(pad_chord([mtof(m) for m in tones], 4 * sec, 0.25, 0.5, rng, 1300.0),
              bar, 0, 0.16, send=0.5, human=0)
        # bouncy bass
        fifth = root + 7
        pat = [(0, root, 0.4, 1.0), (1, root + 12, 0.22, 0.6), (1.5, root, 0.3, 0.8),
               (2, fifth, 0.4, 0.9), (3, root + 12, 0.22, 0.6), (3.5, fifth, 0.3, 0.75)]
        for bt, m, d, v in pat:
            L.put(v_synthbass(mtof(m), d * sec, v), bar, bt, 0.30, send=0.03)
        # offbeat plucked chord stabs
        for bt in (0.5, 1.5, 2.5, 3.5):
            vel = rng.uniform(0.75, 1.0) * (1.0 if bt in (1.5, 3.5) else 0.8)
            for j, m in enumerate(tones):
                L.put(v_pluck(mtof(m), 0.32 * sec, vel), bar, bt + j * 0.008, 0.09, send=0.3)
        # marimba melody + glock sparkle doubling
        for bt, m, d in GAME_MEL[bar]:
            v = rng.uniform(0.85, 1.0)
            L.put(v_marimba(mtof(m), d * sec, v), bar, bt, 0.3, send=0.35)
            if bar >= 8:
                L.put(v_glock(mtof(m + 12), d * sec, v), bar, bt, 0.05, send=0.5)
        # light marimba arpeggio fill in the second half
        if bar >= 8:
            arp = [tones[0], tones[1], tones[2], tones[1] + 12 if tones[1] + 12 < 80 else tones[1]]
            for e, bt in enumerate((0.25, 1.25, 2.25, 3.25)):
                L.put(v_marimba(mtof(arp[e] + 12), 0.2 * sec, 0.7), bar, bt, 0.06, send=0.4)
        # drums
        for bt, v in ((0, 1.0), (2, 0.85)):
            L.put(rng.choice(kit['kick']), bar, bt, 0.42 * v, human=0.002)
        if bar % 2 == 1:
            L.put(rng.choice(kit['kick']), bar, 3.5, 0.25, human=0.002)
        for bt in (1, 3):
            L.put(rng.choice(kit['snap']), bar, bt, 0.16, send=0.3, human=0.003)
        for e in range(8):
            v = (0.5, 0.9, 0.6, 0.9, 0.5, 0.9, 0.6, 1.0)[e] * rng.uniform(0.85, 1.0)
            L.put(rng.choice(kit['shaker']), bar, e * 0.5, 0.1 * v, send=0.1, human=0.003)
        for e in range(4):
            L.put(rng.choice(kit['tick']), bar, e + 0.75, 0.035, human=0.003)
    return L.render(-8.0, room=0.78, damp=0.45, wet=0.5), L.n


# =============================================================================
#  Driver
# =============================================================================

SFX = [
    ('sfx_tap', sfx_tap), ('sfx_pop', sfx_pop), ('sfx_clack', sfx_clack), ('sfx_plink', sfx_plink),
    ('sfx_cup_complete', sfx_cup_complete), ('sfx_combo', sfx_combo), ('sfx_win', sfx_win),
    ('sfx_fail', sfx_fail), ('sfx_coin', sfx_coin), ('sfx_button', sfx_button),
    ('sfx_whoosh', sfx_whoosh), ('sfx_star', sfx_star), ('sfx_unlock', sfx_unlock),
    ('sfx_ice_crack', sfx_ice_crack), ('sfx_bomb_tick', sfx_bomb_tick), ('sfx_explode', sfx_explode),
    ('sfx_spin_tick', sfx_spin_tick), ('sfx_reward', sfx_reward), ('sfx_warning', sfx_warning),
]
MUSIC = [('music_home', music_home), ('music_game', music_game)]


def analyze(path):
    with wave.open(path, 'rb') as w:
        assert w.getnchannels() == 1 and w.getsampwidth() == 2 and w.getframerate() == SR
        nf = w.getnframes()
        raw = w.readframes(nf)
    a = array.array('h')
    a.frombytes(raw)
    if sys.byteorder == 'big':
        a.byteswap()
    pk = max(abs(v) for v in a) / 32768.0
    mean = sum(a) / (len(a) * 32768.0)
    rms = math.sqrt(sum(v * v for v in a) / len(a)) / 32768.0
    clipped = sum(1 for v in a if v >= 32767 or v <= -32767)
    return nf / SR, pk, rms, mean, clipped, a


def main(argv):
    os.makedirs(OUT_DIR, exist_ok=True)
    only = set(argv[1:])
    t_start = time.time()
    jobs = [(n, f, False) for n, f in SFX] + [(n, f, True) for n, f in MUSIC]
    for name, fn, is_music in jobs:
        if only and name not in only:
            continue
        t0 = time.time()
        res = fn()
        x = res[0] if is_music else res
        write_wav(name, x)
        print('  %-18s rendered in %5.2fs' % (name, time.time() - t0), flush=True)

    print('\n%-20s %8s %9s %8s %8s %9s %7s %s' % ('file', 'dur(s)', 'size(KB)', 'peakdB', 'rmsdB',
                                                  'dc', 'clip', 'notes'))
    total = 0
    for name, _, is_music in jobs:
        path = os.path.join(OUT_DIR, name + '.wav')
        if not os.path.exists(path):
            print('%-20s MISSING' % name)
            continue
        dur, pk, rms, mean, clipped, a = analyze(path)
        size = os.path.getsize(path)
        total += size
        note = ''
        if is_music:
            diffs = sorted(abs(a[i + 1] - a[i]) for i in range(0, len(a) - 1, 7))
            p999 = diffs[int(len(diffs) * 0.999)]
            seam = abs(a[0] - a[-1])
            note = 'seam=%d (p99.9 step=%d) %s' % (seam, p999, 'OK' if seam <= p999 else 'CHECK')
        if rms < 1e-4:
            note += ' SILENT?'
        print('%-20s %8.3f %9.1f %8.2f %8.2f %9.5f %7d %s' % (
            name + '.wav', dur, size / 1024.0, 20 * math.log10(pk or 1e-9), 20 * math.log10(rms or 1e-9),
            mean, clipped, note))
    print('\nTotal res/raw size: %.1f KB (%.2f MB)   elapsed %.1fs' % (total / 1024.0, total / 1048576.0,
                                                                     time.time() - t_start))


if __name__ == '__main__':
    main(sys.argv)
