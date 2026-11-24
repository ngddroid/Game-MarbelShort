# Marble Pop Factory: Single-Phase Build Script

> Working title: **"Marble Pop Factory"** (you can rename it; the package stays `com.puzzel.lab.marbelshort`).
> Genre: tap → burst → marble-sort puzzle. It uses the same *type* of mechanic as the reference, but the name, art, palette, characters, levels and sounds are **all original**. We copy none of the reference's assets or branding.

Reference captured from the connected **Samsung SM-A076B (720×1600, budget device)** → `docs/reference/`.

---

## 0. What I learned from the reference (on-device analysis)

| Element | Observed behaviour |
|---|---|
| **Block grid** (top) | Colored "pop-it" blocks in a shaped container. Only the **front (bottom) row** is tappable. When a block is removed, the column above **slides down** with a squash bounce. |
| **Burst** | A tapped block bursts into **~9 marbles** that physically pour through a **funnel** (gravity + collisions). |
| **Belt** (middle) | An oval looping conveyor with fixed **slot capacity**. Marbles ride it until they reach a matching cup. |
| **Tray** (bottom) | 4 columns of stacked color cups. The **top cup** of each column takes 3 marbles of its color, then pops/flies away and the column shifts up. |
| **Win / Lose** | Win = grid + belt empty. Lose = belt full and a marble can't board. |
| **Special pieces** | `?` hidden blocks, number-counter blocks, key + chain locks, arrow blocks, crate with counter, ×2 gate, hidden `?` tray cups, unlocks at lvl 6/8/11. |
| **Meta** | Home level path, lives + timer, coins, shop, settings, No-Ads. |

---

## 1. Tech decisions

| Area | Choice | Why |
|---|---|---|
| Language/UI | Kotlin + Jetpack Compose (already set up), Material3 only as a base. All visual components are custom. | Matches the existing project |
| Game rendering | **Custom Compose `Canvas` game engine** with a fixed-timestep loop (`withFrameNanos`), 60 fps target | Full control and very sharp vector graphics at any DPI |
| Graphics style | **Procedural 3D-look rendering**: radial gradients for glossy marbles (specular highlight, rim light, contact shadow), beveled pop-it blocks with bump dots, soft drop shadows, glass-container refraction tint | High quality with no external art and a tiny APK |
| Physics | Lightweight custom 2D circle physics (Verlet + spatial hash) for the funnel pour; the belt uses path-following | Real-feeling marble pours, stays cheap on the A07 |
| Particles | Pooled particle system (confetti, sparkles, pop rings, star bursts, coin fly) | Juice |
| Sound | `SoundPool` SFX + `MediaPlayer` looping music. **All audio is synthesized by a Python script I write** (pop, clack, belt hum, cup-fill chime with rising pitch for combos, win fanfare, coin, button tick, 2 music loops) → `res/raw/*.ogg` | Original and royalty-free; you can swap in licensed audio later |
| Haptics | `Vibrator`/`HapticFeedback` on tap, pop, win | |
| Font | **Lilita One** + **Fredoka** (SIL OFL, downloaded from the google/fonts GitHub repo) → `res/font` | Chunky game look |
| Storage | `SharedPreferences` wrapped in a `GameRepository` (progress, coins, boosters, skins, settings, daily state) | Simple, offline |
| Navigation | Compose state-based navigator with animated transitions (no extra library) | |
| Legal pages | `WebView` screen that loads `file:///android_asset/legal/terms.html` / `privacy.html`, styled in the game theme | As requested |
| Performance | Cached `Brush`/`Path` objects, pre-rendered marble sprites to `ImageBitmap` per color/skin, particle cap, no allocations in the frame loop | The A07 is a low-end device |

New dependencies: `androidx.core:core-splashscreen` only. No ads or analytics SDK.

---

## 2. Screens & flow

```
Splash ─▶ (first launch) Onboarding x4 (NO skip) ─▶ Home
                                                   │
 Bottom bar:  [Shop] [Daily] [ PLAY (center, raised) ] [Collection] [Profile]
                                                   │
                Home ─▶ Game ─▶ Win / Fail popups ─▶ Home / next level
   Top-left gear ─▶ Settings ─▶ Terms (HTML) / Privacy (HTML)
```

### 2.1 App icon
- Adaptive icon (vector foreground + gradient background): three glossy marbles (pink, cyan, yellow) falling into a purple funnel, with a sparkle.
- Monochrome layer for Android 13 themed icons, plus a 512×512 Play Store PNG rendered by a script.

### 2.2 Splash (≈2.2 s)
- System splash (core-splashscreen) hands off to an animated Compose splash: marbles rain down and bounce into a funnel, the logo pops in with a squash-stretch and shine sweep, and a loading bar fills with marble dots. Soft "whoosh + chime".

### 2.3 Onboarding: 4 pages, **no skip**, only "Next" → last page "Let's Play!"
1. **Tap to Pop**: animated mini-demo: a finger taps a block and it bursts into marbles
2. **Ride the Belt**: marbles circle the conveyor
3. **Sort into Cups**: 3 marbles fill a cup and it pops
4. **Don't Jam the Belt!**: belt fills up red, booster icons appear
- Live mini-canvas animations reuse the real engine renderer. Pager dots are marbles. Swipe is disabled so the user steps through every page.

### 2.4 Home (tab: PLAY)
- Top HUD: avatar, ❤ lives with regen timer, coins pill with (+).
- **Animated level road**: vertical winding path of glossy level nodes. The current node pulses; completed nodes show 1–3 stars. Every 10th node is a **chest node** and every 50th is a **boss/hard level** (red). The background parallax-scrolls (floating bubbles and stars).
- Side widgets: Daily Challenge, Lucky Spin, Piggy coin bank, and "Next feature unlock at Lv.X" teaser.
- Big bouncing **Play** button ("Level 37").

### 2.5 Bottom bar (custom drawn, glossy, center button raised)
| Tab | Content |
|---|---|
| **Shop** | Coin packs (rewarded "free" packs on a cooldown, since there's no IAP), booster bundles, marble skins, themes. Everything is bought with coins. |
| **Daily** | Daily Challenge level (seeded by date, 2× coins), **Lucky Spin wheel** (1 free spin/day), 7-day login calendar |
| **PLAY** | Home level road |
| **Collection** | Marble skins (Glossy, Glass, Galaxy, Candy-swirl, Metal, Neon, Emoji-face) and playground themes (Candy Lab, Ocean, Space, Lava, Forest), each a full recolor of the container, belt and background |
| **Profile** | Stats, **Achievements** (30+ with progress bars and claimable rewards), streak |

### 2.6 Game screen (most effort goes here)
Layout (portrait, scaled from a 720×1600 design grid):
1. **Top HUD**: pause (gear), "Level N" pill, coins, upcoming-mechanic lock badges.
2. **Glass container** with a shaped grid (shapes come from the level: rectangle, U, T, stairs, twin-towers, heart, etc.). Frosted-glass back panel, inner shadow, rim highlight.
3. **Funnel**: tapered metal/plastic chute; marbles pour with real collisions.
4. **Belt**: oval track with animated treads, slot dimples, and a **capacity meter** that turns orange→red. The track runs faster when crowded.
5. **Tray**: 4–6 columns of stacked cups, with gloss and bump dots on the top cup. Marbles drop in with a "plink". When a cup is filled with 3 it does pop → shine → flies away, and the stack slides up.
6. **Booster bar**: Undo, Shuffle-tray, Magnet (pulls all marbles of one color off the belt), +Belt Slots, Hammer (remove any block).

**Juice list**
- Block press: squash 0.9 then burst into marbles with a radial pop ring, dust and a little screen shake.
- Marbles: specular highlight that rotates with travel, spin, motion squash, shadow on the belt.
- Cup complete: ring flash, star sparkles, and a "+3" floating text.
- **Combo system (my addition)**: completing cups within 1.2 s chains a combo. "Nice! / Great! / Awesome! / MARBLE-OUS!" banners appear, pop pitch rises, and combos give bonus coins.
- Belt danger: red pulse vignette, heartbeat SFX, warning icon.
- Win: confetti cannon, 3-star reveal with thuds, coin counter roll-up with flying coins, "Claim ×2" button.
- Fail: "Belt Jammed!" popup with marbles spilling, offering Continue (+8 slots for coins) or Retry.

### 2.7 Settings
- Music, SFX, Vibration toggles (custom marble-knob switches), Notifications reminder toggle, Restore progress/reset.
- **Terms of Service** and **Privacy Policy** buttons open a new screen with a game-themed top bar and a WebView loading `assets/legal/terms.html` / `privacy.html`. Those HTML files use the game's purple gradient, rounded cards, marble bullets and Fredoka font, and work offline.
- Version, Rate us, Share.

---

## 3. Gameplay rules (original spec)

- **Grid**: W×H cells inside a container shape. A block is **tappable only if it's exposed** at the funnel side, meaning no block sits below it in its column. Tapping it releases **9 marbles** of its color; blocks above fall down.
- **Belt**: capacity `C` (24 at start, up to 40). Marbles board at the funnel drop point. A full belt makes marbles queue in the funnel. If the funnel overflows, the level is lost.
- **Tray**: K columns of cups. Each cup holds 3 marbles. A marble passing over a column whose **top cup** matches its color and has room drops in.
- **Win**: every block popped and belt empty. **Stars**: by peak belt usage and boosters used.
- **Solvability guarantee**: each color's total marbles = 3 × that color's cups. The generator builds a **reverse-solved** sequence and then a greedy **solver verifies** each level (unit test over all 5000).

### Special elements (introduced gradually)
| Lv | Element | Behaviour |
|---|---|---|
| 1–5 | Tutorial | Hand pointer, 3–4 colors |
| 6 | **Mystery `?` block** | Color revealed when it becomes exposed |
| 8 | **Counter block (n)** | Needs n other pops before it can be tapped |
| 11 | **Hidden tray cups** | `?` cups reveal when they reach the top |
| 15 | **Key & Chain** | Pop the key block to unlock a chained row |
| 20 | **Arrow block** | Tapping it shoves its row left/right/up |
| 25 | **Crate (n)** | Opens after n cups are completed |
| 30 | **×2 Gate** | Marbles passing through double (cups must account for it) |
| 40 | **Ice block** *(mine)* | 2 taps: crack, then burst |
| 55 | **Rainbow block** *(mine)* | Its marbles are wildcards for any cup |
| 70 | **Bomb block** *(mine)* | Pops itself and its neighbors after n moves; you must clear it first |
| 90 | **Paint bucket** *(mine)* | Recolors adjacent blocks |
| 120 | **Belt switch** *(mine)* | Reverses belt direction when tapped |

### 5000 levels
- `LevelGenerator(seed = levelNumber)` is deterministic, so no level files are stored and all levels are guaranteed identical across devices.
- **Levels 1–30 are hand-tuned** in code for a smooth tutorial curve.
- Difficulty curve: colors 3→8, grid size, belt capacity shrinking, special-element density, with a saw-tooth rhythm (easy, easy, medium, hard, relief) and a boss every 50.
- Container shape is picked from 20+ shape templates. Theme (world) changes at fixed unlock levels: 1, 40, 80, 120, 180, 250, 350, 500, 700, 1000, 1400, 2000, 2800, 3800 (see DEVELOPER_GUIDE "Worlds").
- `LevelGeneratorTest` generates and solves all 5000 levels in a JVM unit test.

---

## 4. File / package layout

```
app/src/main/
 ├─ assets/legal/terms.html, privacy.html, legal.css
 ├─ res/raw/*.ogg                    (generated SFX + music)
 ├─ res/font/lilita_one.ttf, fredoka.ttf
 ├─ res/drawable/ic_launcher_*.xml   (new icon), splash assets
 └─ java/com/puzzel/lab/marbelshort/
     ├─ MainActivity.kt, MarbleApp.kt (navigator + transitions)
     ├─ core/       GameRepository, Settings, SoundManager, Haptics, Economy
     ├─ engine/     GameLoop, Physics (Verlet+hash), Belt, Tray, Grid, Particles, Tween/Easing
     ├─ render/     MarbleRenderer (sprite cache), BlockRenderer, ContainerRenderer,
     │              BeltRenderer, TrayRenderer, FxRenderer, Themes, Skins
     ├─ level/      LevelModel, LevelGenerator, HandcraftedLevels, Solver, Shapes
     ├─ ui/components/ GameButton (3D press), Pill, Popup, BottomBar, StarRow,
     │                 CoinFly, Toggle, Confetti, OutlinedText
     ├─ ui/screens/ Splash, Onboarding, Home(+LevelRoad), Game, WinPopup, FailPopup,
     │              Pause, Shop, Daily(+SpinWheel), Collection, Profile/Achievements,
     │              Settings, LegalWebView
     └─ ui/theme/   Colors, Type, Theme
tools/  gen_sounds.py, gen_icon.py (build-time helpers, not shipped)
```

---

## 5. Execution order (one continuous phase, run without stopping)

1. **Foundation**: deps (splashscreen), fonts, theme, color system, navigator, repository, `SoundManager`, haptics.
2. **Engine core**: game loop, tween/easing library, physics, belt path, tray logic, grid logic, win/lose.
3. **Level system**: model, 20+ shapes, hand-crafted 1–30, seeded generator to 5000, solver, unit test across all levels.
4. **Renderers (biggest block of time)**: marble sprite cache with 7 skins, pop-it blocks with bevel and dots, glass container, funnel, animated belt, tray cups, 5 themes, particles, screen shake, combo banners.
5. **Game screen**: HUD, boosters, tutorial hand, pause, win and fail popups with full animations.
6. **Audio**: write `tools/gen_sounds.py`, synthesize 15+ SFX + 2 music loops, wire them to events with pitch variation.
7. **Meta screens**: Splash, Onboarding (no skip), Home level road, bottom bar, Shop, Daily (spin wheel and calendar), Collection, Profile/Achievements.
8. **Settings + legal**: settings screen, themed `terms.html` / `privacy.html` in assets, WebView screen.
9. **App icon**: adaptive vector, monochrome layer, 512 px store icon.
10. **Verify on the Samsung A07**: `assembleDebug`, install via adb, play levels 1–15 + random high levels (e.g. 777, 2500, 5000), screenshot every screen, check FPS (`dumpsys gfxinfo`), fix jank/visual bugs, iterate on polish.

**Done means**: APK builds, runs at a smooth frame rate on the A07, all screens are reachable, all 5000 levels are generated and verified solvable by the test, and audio, haptics and settings persist.

---

## 6. Assumptions (tell me if any are wrong before I start)
- **No ads / no real-money IAP / no Firebase**. The shop uses in-game coins only, with a free "watch-less" coin cooldown. Hooks are left in place so ads/IAP can be added later.
- **Lives system: OFF** (unlimited play is friendlier). It can be turned on with a flag.
- English only, portrait only, offline.
- Audio is synthesized (original). If you have licensed sound packs, drop them in `res/raw` with the same names.
- Name "Marble Pop Factory". Change it if you prefer.

---

## 7. Build status (implemented 2026-11-05)

Everything above is built and verified on the Samsung SM-A076B. Changes from the original plan:
- **Boosters**: Hammer, Magnet, +6 Slots and Shuffle. Undo was dropped because rewinding real-time physics would be unreliable.
- **Rainbow block**: its marbles only fill cups whose colour cannot be completed by the real marbles still left. This keeps every level solvable.
- **×2** is a block holding 18 marbles, not a gate.
- **Lucky multiplier** replaces "Claim ×2" on the win screen (there are no ads): a moving needle over ×2/×3/×5.

**Verification**
- `./gradlew :app:testDebugUnitTest` generates all 5000 levels and replays each one's planned solution. It checks that every planned tap is legal, that each level is solvable within its belt capacity, and that marble/cup counts match per colour.
- Frame timing on the A07 during gameplay: median 10 ms, 90th percentile 15 ms.

**Dev helper**: `tools/jump_level.sh <level> <shot>` unlocks a level in the debug build, opens it and takes a screenshot.
**Asset generators**: `tools/gen_sounds.py` (all audio) and `tools/gen_icon.py` (launcher icon and store art).
**Before release**: replace the placeholder support email in `assets/legal/*.html` and set up release signing.

**Update (rules aligned with the reference):** blocks no longer fall. A block can be tapped only when it has a path of empty cells to the bottom exit (like the reference). Tappable blocks are raised and glossy and show their marbles; blocked ones are flat pastel. Tapping one sends its marbles out along that path. Levels can start with empty slots and inner walls. The generator plans every level under this rule, and the unit test replays all 5000 levels.
