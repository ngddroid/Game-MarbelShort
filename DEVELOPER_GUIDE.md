# Marble Pop Factory — Developer Guide

This document explains how the game is built, so a new developer can find their way around the code, change gameplay safely and add features (ads, new blocks, themes…).

- **Package / applicationId:** `com.puzzel.lab.marbelshort`
- **Stack:** Kotlin, Jetpack Compose (UI), a custom 2D engine drawn on a Compose `Canvas` (no game library)
- **Min SDK** 24 · **target/compile SDK** 37 · **AGP** 9.4.1 · built with the JetBrains JDK 21 that ships with Android Studio
- Everything is **offline**. No accounts, analytics or real-money purchases. Progress is stored on the device.

---

## 1. Build, run, test

```bash
./gradlew :app:assembleDebug          # debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease        # release APK (unsigned; set up signing before publishing)
./gradlew :app:testDebugUnitTest      # JVM tests — generates and solves all 5000 levels (~5 s)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`adb` is not on the PATH on the original dev machine. Use `~/Library/Android/sdk/platform-tools/adb`.

---

## 2. Project layout

```
app/src/main/java/com/puzzel/lab/marbelshort/
├─ MainActivity.kt          system splash, immersive mode, creates GameRepository + SoundManager
├─ core/
│  ├─ GameRepository.kt     ALL persistent player data (SharedPreferences) exposed as Compose state
│  ├─ SoundManager.kt       SoundPool SFX, looping music, haptics; `Sfx` and `Music` enums
│  └─ Ads.kt                rewarded-ad abstraction (debug fake today, real SDK later)
├─ level/                   pure Kotlin, no Android — shared by the game and the unit tests
│  ├─ LevelModel.kt         colours, BlockKind, Feature (unlock levels), BlockSpec, CupSpec, LevelSpec
│  ├─ Board.kt              grid RULES (which blocks can be tapped), TraySim, PuzzleSim (solver)
│  └─ LevelGenerator.kt     deterministic generator for levels 1…5000
├─ engine/
│  ├─ GameLayout.kt         world geometry (board, hopper, spout, belt, tray) in 720-wide world units
│  ├─ Entities.kt           Marble, Cup, BlockVisual, FlyingBlock, ExitingBlock, Particles, FloatingText
│  └─ GameEngine.kt         real-time simulation: taps, marble streams, physics, belt, cups, win/lose
├─ render/
│  ├─ GameRenderer.kt       draws the playground (baked static layers + cached sprites)
│  ├─ MachineArt.kt         procedural art: marbles, jelly blocks, cup pucks, belt socket, roller hub (+ unused legacy parts)
│  ├─ Sprites.kt            `Col` colour helpers, `Painter` (icons/overlays/text), `SpriteCache`
│  └─ Themes.kt             5 colour themes (Candy Lab, Ocean, Space, Lava, Forest) + their sky palettes
└─ ui/
   ├─ MarbleApp.kt          navigation (Screen + Nav), screen transitions, music switching, ad overlay
   ├─ theme/Theme.kt        design tokens: GameColors, Grad (gradients), Skies (sky palettes), Motion, fonts, text styles
   ├─ components/
   │  ├─ Components.kt      jelly GameButton, candy OutlinedText, Ribbon, GamePanel, PopupHost, cards, chips, toggles…
   │  ├─ Backgrounds.kt     SkyBackground, SunRays, ConfettiBurst, rememberTime(), SoftDot/SparklePath
   │  ├─ Icons.kt           ~37 icons drawn in code (GameIcon / drawGameIcon) + ball()
   │  └─ AdComponents.kt    WatchAdDialog, DebugAdOverlay
   └─ screens/              Splash, Onboarding, Main (home map + dock), MapArt (map drawing), Tabs, Game, Settings, Legal

app/src/main/assets/legal/  terms.html, privacy.html, legal.css (game-themed, opened in a WebView)
app/src/main/assets/fonts/  fonts used by the HTML pages
app/src/main/res/raw/       all sounds (generated WAVs)
app/src/main/res/font/      lilita_one.ttf, fredoka.ttf (variable) — SIL OFL
app/src/test/.../LevelGeneratorTest.kt
tools/gen_sounds.py         regenerates every sound (pure Python, no dependencies)
tools/gen_icon.py           regenerates launcher icons + Play Store art (needs Pillow)
tools/jump_level.sh         debug helper: unlock a level on a device and open it
docs/reference/             screenshots of the reference game used during analysis
docs/store/                 512px icon, 1024x500 feature graphic
GAME_PLAN.md                original design plan and the decisions/changes made since
UI_GRAPHICS_PLAN.md         the "Candy Glass Factory" UI design system and per-screen spec
PLAYGROUND_GRAPHICS_PLAN.md the "Candy Glass Machine" playfield redesign: analysis, element specs, perf rules, QA
```

---

## 3. How the game plays (rules)

The board is a grid of **blocks**. Each block holds **9 marbles** of one colour (a DOUBLE block holds 18).

1. A block can be tapped only when it has **a way out**. Either it sits in the bottom row, or it touches an empty cell that is connected through empty cells to the bottom row. **Blocks never fall.** Arrow blocks can also leave straight in their arrow direction if that line is empty.
2. Tapping it makes its marbles **stream out along that path** of empty cells, down into the **hopper**.
3. Marbles go through the spout and board the **belt** (a loop with a limited number of slots, its *capacity*).
4. Under the belt is the **tray**: columns of stacked **cups**. Each cup takes 3 marbles of its colour, and only the **top** cup of a column accepts marbles. A full cup pops and the column slides up.
5. **Win:** board, belt and tray are all empty. **Lose:** the belt is full and nothing can drop for a whole lap (*Belt Jammed*), a bomb timer runs out, or no moves are left.

Blocks that are open are drawn raised and glossy, with their marbles visible. Blocked ones are flat pastel. The look updates as soon as a path opens (the block "wakes up").

### Special blocks / features

`Feature` in `LevelModel.kt` defines when each mechanic is introduced. That level shows a "New!" popup.

| Level | Feature | `BlockKind` | Behaviour (implemented in `Board`/`GameEngine`) |
|---|---|---|---|
| 6 | Mystery | `MYSTERY` | Grey "?" until it gets a way out, then its colour is revealed |
| 8 | Caged | `COUNTER` | Locked until `param` blocks have been popped in total |
| 11 | Mystery cup | (`CupSpec.hidden`) | Cup colour hidden until it reaches the top of its column |
| 15 | Key & chains | `KEY` / `CHAINED` | Popping the key (same `param` = lock id) unlocks its chained blocks |
| 20 | Arrow | `ARROW` | `param` 0 left, 1 right, 2 up; may also exit straight that way |
| 25 | Crate | `CRATE` | Locked until `param` cups have been completed |
| 30 | Double | `DOUBLE` | 18 marbles / 6 cups |
| 40 | Ice | `ICE` | First tap cracks, second tap pops |
| 55 | Rainbow | `RAINBOW` | Rainbow marbles only fill cups whose colour can no longer be completed by real marbles (keeps levels solvable) |
| 70 | Bomb | `BOMB` | Lose when `moves ≥ param` while it is still on the board |
| 90 | Paint | `PAINT` / `GRAY` | Grey blocks stay locked and grey until their bucket (same `param`) pops |
| 120 | Belt switch | `SWITCH` | Popping it reverses the belt |

---

## 4. Level data and generation (`level/`)

### Model
- **`LevelSpec`**:
  - **Grid:** `width × height` grid with `mask` (active cells of the board shape) and `cells: Array<BlockSpec?>`. The index is `row * width + col`, and **row 0 is the bottom row** (the exit row).
  - **Tray:** `tray` is a list of cup columns, top cup first.
  - **Belt:** `capacity` is the number of belt slots.
  - **Other fields:** `theme`, `isBoss`, `introduces`, and `plan`. `plan` is the verified tap order of block ids; ice blocks appear twice.
- **`Board`** holds the rules:
  - `refresh()` runs a breadth-first search from the empty bottom cells to compute `exposed[]` (which blocks are open).
  - Other members: `hasExit`, `canTap`, `pathOut` (the cells marbles travel through), `tap`, plus lock and counter helpers.
- **`PuzzleSim`** is an instant, discrete version of the game: a marble goes to a matching top cup immediately. The generator and the tests use it.

### Generator (`LevelGenerator`)
`LevelGenerator.get(n)` is **deterministic**: it seeds `java.util.Random` from `n` and caches the last 12 levels. The same level is identical on every device, and no level files are stored.

1. **Recipe:**
   - Levels 1–30 come from the hand-tuned `handmade` table (shape heights, colours, empty slots, features).
   - Later levels use `recipeFor()`. A difficulty value `d` rises with the level number and follows a 5-level saw-tooth (easy, easy, medium, hard, relief). Every 50th level is a boss ("Super Hard"), and every 10th level ending in 7 gets a small difficulty bump.
2. **Shape:** column heights (11 templates), optional inner walls (connectivity is checked), plus a few initially empty slots.
3. **Planned solution:** blocks are removed one by one, each only when `Board` says it is open (the same rule as the game). This order becomes `plan`.
4. **Colours** are spread over the plan.
5. **Special kinds** are assigned *consistently with the plan*. For example, the key is popped before its chains, and a counter's `param` is never larger than the pops that happen before it.
6. **Cups** are built from the plan (3 per block), shuffled within a difficulty "window", then dealt into 4 or 5 columns. Some cups are hidden.
7. **Verify** with `PuzzleSim`. The belt **capacity is the plan's peak load plus a difficulty margin** (clamped to 14…48). If the peak is too high, the window shrinks and it retries; if that fails, the generator uses a fresh seed.

**`LevelGeneratorTest` replays the plan of all 5000 levels.** It checks that every tap is legal, that each level is solved within capacity, that marbles and cup slots balance per colour, that each level starts with an open block, and that intro levels contain their feature. **Run it after any change to `level/`.**

---

## 5. Real-time engine (`engine/`)

### World units and layout
The world is always **720 units wide**; its height follows the screen. `GameLayout` stacks, from top to bottom:
1. The **board**: the grid, with `cell` capped at 128 units.
2. The **hopper**: a funnel whose height `funnelH` is 92–150.
3. The **spout** and its **gate**: one marble waits at the gate to board the belt.
4. The **belt**: a stadium path given by `beltPoint(s)`.
5. The **tray**, starting at `trayTop`.

Extra vertical space goes first to more visible tray rows, then to a taller funnel, then above the board. Useful helpers: `cellLeft/cellTop/cellCenterX/cellCenterY`, `colCenter`, `cupTop`.

### `GameEngine.update(dt)` order
`updateBlocks → updateFlying → updatePaths → physics → updateBelt → updateDropping → updateCups → particles/texts/exiting → checkEnd`

### Marble life cycle (`MarbleState`)
```
PATH ──▶ PHYSICS ──▶ BOARDING ──▶ BELT ──▶ DROPPING ──▶ (inside a cup, not an entity any more)
 ▲ streams through empty cells, along waypoints from Board.pathOut
         ▲ Verlet physics in the hopper (3 substeps, soft contacts, anti-jam nudge, pull to centre)
                      ▲ short tween into an empty belt slot passing the spout
                                 ▲ rides slot i at arc length slotS(i) = i*spacing + beltOffset
                                            ▲ leaves the belt when crossing the centre of a column whose top cup accepts it
FLYING: arrow blocks without a path and hammered blocks fly to the hopper mouth (`burstAtMouth`).
```
- **Hopper cap (`hopperCap`):** when the hopper is full, streaming marbles wait in their exit cell instead of spilling over the board.
- **Cups:** `Cup.reserved` counts marbles already on their way, and `arrived` counts the ones that landed. A cup accepts a marble only if it is the top cup, settled (`display < 0.3`), revealed and not full. Cups slide with an under-damped spring (`vel`).
- **Combos:** cups completed within 1.5 s of each other chain a combo. This triggers `GameListener.combo()`, which shows the banner, and earns coins from combo 3 upwards.
- **Intro:** for the first `INTRO_MACHINE` (0.55 s) plus a stagger:
  - The board pops in and the dish rises.
  - Blocks drop into their cells (`BlockVisual.dropY` / `delay`).
  - Pucks spring up into place.
  - Taps are ignored while `inIntro` is true.
- **End conditions** live in `checkEnd()`; see the rules above. `continueGame()` revives after a fail:
  - Jam → +10 slots.
  - Bomb → +6 moves.
  - No moves → shuffle the tray and add +6 slots.
- **Results:**
  - `stars` is based on peak belt use: ≤62% gives 3 stars, ≤86% gives 2.
  - Any booster caps the result at 2 stars, and a continue sets it to 1.
  - `coinReward()` gives the base coins plus a star bonus, combo coins and a boss bonus.
- **Events:** the engine never touches Android. It reports through `GameListener` (`sound`, `haptic`, `combo`, `statusChanged`); `GameScreen` implements it.

### Boosters (counts saved in `GameRepository.boosters`)
| Booster | Engine call | Effect |
|---|---|---|
| Hammer | `hammer(x, y)` | Force-pops any block (ignores locks and paths) |
| Magnet | `magnet()` | Sends belt marbles of the best colour into any matching cups, even buried ones |
| +6 Slots | `boosterSlots()` | Grows belt capacity and re-maps the marbles onto the new slots |
| Shuffle | `boosterShuffle()` | Shuffles the colours of cups that have no reserved marbles |

---

## 6. Rendering (`render/`)

`GameRenderer` draws everything onto the native `android.graphics.Canvas` of the Compose `Canvas` in `GameScreen`. It scales world units by `scale = widthPx / 720`.

The look is the "Candy Glass Machine" described in `PLAYGROUND_GRAPHICS_PLAN.md`: candy-coloured frames (`theme.wall` / `wallDark`), frosted glass interiors, light from the top-left, and the same per-world sky as the home map.

- **Static art is baked once** into three bitmaps:
  - `backdrop`: the world sky (`GameTheme.sky`, the same colours as `ui.theme.Skies`), two glows, bokeh, and a far and near candy-factory skyline with lit windows.
  - `board`: the board outline, its candy rim and glass interior, the recessed cell wells, the hopper with dotted guide lanes and exit chevrons, and the glass pipe with two collars. `boardShape(grow, radius)` unions one rounded rect per active cell, plus bridge rects between neighbours (so rounded corners leave no notches), plus the hopper. `rounded()` then softens the concave steps with a `CornerPathEffect` through `getFillPath`, so the result can also be used as a clip.
  - `base`: the tray cabinet (rim and frosted interior), the glass lanes, the belt housing, the dark track, the island and the gauge recess.

  `board` and `base` are drawn with offsets and scale during the intro.
- **Drawn every frame (cheap):**
  - 7 twinkling sparkles.
  - One `socket` sprite blit and one rivet per belt slot. They move with `slotS(i)`, so the belt visibly runs even when it is empty.
  - Two roller `hub` sprites, rotated by `beltOffset / beltR`.
  - The fill gauge, using three pre-built shaders: green, yellow from 55 %, and red from 78 % with a pulse. Its label string is rebuilt only when the count changes.
  - One "queue fog" gradient rect over the tray, so the active cup row stands out.
- **Moving things are single bitmap blits from `SpriteCache`:**
  - `marbleShadowed` (each marble is one image with its shadow built in).
  - `block(color, variant, size, skin, open)`, which has separate open and closed versions that cross-fade via `BlockVisual.open`.
  - `cup`, `socket`, `hub`, and `overlay` (cage, chains, crate, ice, key, arrow, bomb, bucket, switch).
- **Palette:** colours come from the selected `GameTheme` (`Themes.kt`).
  - The belt track is deep indigo, only lightly tinted by the world, so marbles stand out.
  - Cell wells use a neutral slate tint, so pastel blocks never blend into the board in the pink or green worlds.
- **Art painters:**
  - `MachineArt.marble` draws the layered glass marble with a fixed-light gloss and 7 skins. Index 0 is "Glossy", a cat's-eye glass marble.
  - `MachineArt.jelly` draws the board block:
    - Open: raised, with a bevel, a recessed window holding 9 marbles, and a glass gloss and glint.
    - Blocked: lower, with the marbles faded behind a frosted lid.
    - Mystery: a lavender candy stripe with a gold "?".
  - `MachineArt.puck` draws the cup: a glossy candy cylinder with a specular stripe, a raised lip and 3 deep sockets.
  - `MachineArt.socket` and `MachineArt.hub` draw the belt parts.
  - `crate`, `gear`, `cradle` and `chromeRect` are left over from an earlier "factory machine" design and are currently unused.

**Performance rules** (the game holds about 60 fps on a budget Samsung A07):
- Never allocate in `draw()`. Reuse `dst`, `tmp` and `path`, and get every bitmap from `SpriteCache`.
- **Paint alpha pitfall:** `Paint.alpha` also multiplies shader output. Always set `p.alpha = 255` (or call `p.reset()`) before assigning a shader after drawing a translucent colour. The code follows `p.alpha = 255; p.shader = …`.
- `BlurMaskFilter` is only used on software canvases, i.e. when building sprites and baked layers, never on the screen canvas.
- Measured on the SM-A076B (120 Hz) after the playfield redesign: 0 missed frame deadlines out of 1660 frames of busy play, with a median GPU time of 3 ms and a 99th percentile of 7 ms.

---

## 7. UI (`ui/`)

The full visual spec is in **UI_GRAPHICS_PLAN.md** ("Candy Glass Factory": glossy candy and glass in a dreamy sky). This section is the code map.

### Navigation and motion
- `MarbleApp` holds `Nav`: the current `screen` (`Screen` sealed interface), a back stack, the selected bottom `tab`, and `forward` (true for `go`/`replace`, false for `back`/`home`).
- `screenTransition()` picks the animation from the pair of screens:

  | Change | Animation |
  |---|---|
  | Splash → anything | fade with a gentle zoom |
  | Home ↔ Game | zoom through (the old screen scales past, the new one scales in) |
  | Game → Game | short crossfade |
  | Other screens | push / pop slides, using `forward` |

- Tabs slide left/right in `MainScreen`, following the tab index.
- Music switches between `Music.HOME` and `Music.GAME`.

**Flow:** Splash → Onboarding (first launch only, 4 swipeable pages, no skip button) → Main.

### Design tokens (`theme/Theme.kt`)
| Token | What it holds |
|---|---|
| `GameColors` | Flat colours. `Ink` (#2A1463) is the outline colour for almost everything. |
| `Grad` | Brushes for candy text and panels (`Gold`, `Pink`, `Aqua`, `Mint`, `Snow`, `Coral`, `PanelRim`, `PanelFill`). |
| `Skies` | `SkyPalette`s: `Menu`, plus one per world via `Skies.forWorld(themeIndex)` (Candy Lab, Ocean, Space, Sunset, Forest). |
| `Motion` | Durations, easings (`EaseIn` for entering, `EaseOut` for leaving) and springs (`bouncy`, `snappy`, `gentle`). |

### Components (`components/`)
| Component | Notes |
|---|---|
| `OutlinedText` | Candy text: shadow stroke + outline + fill. `brush =` for a gradient fill, `gloss = true` for the glassy top half. |
| `GameButton` | Jelly button: shadow, depth side, gradient body, gloss, outline. `shine = true` adds the periodic light streak (use it on the main call to action only). Plays the button sound and a light haptic. |
| `IconRoundButton` | Icon-only `GameButton`. |
| `ResourcePill` | Dark glass capsule. The icon bumps when the value changes. |
| `Ribbon` | Banner with folded tails. `height` scales everything. |
| `GamePanel` | Popup card: rim + cream interior + ribbon title + optional close button. `rays = true` adds `SunRays` behind it. |
| `PopupHost(visible)` | Dim + spring pop-in, and an animated exit when `visible` turns false. Use `rememberLatest(value)` when the popup content depends on a value that becomes null on close. |
| `SectionCard` | Candy card used by every tab. |
| `IconTile`, `Chip`, `SegmentedSwitch`, `ProgressTube`, `NotifyBadge`, `SectionTitle`, `GameToggle`, `ScreenHeader` | Smaller building blocks. |
| `idleBounce()`, `floaty()` | Breathing pulse and slow bob. Both work in the draw phase only. |
| `SkyBackground(palette)` | Gradient + glows + candy-factory skyline (static, cached in its own GPU layer) + soft bokeh and twinkles (animated). `MenuBackground()` is the menu sky. |
| `SunRays`, `ConfettiBurst` | Reward effects. |
| `rememberTime()` | Seconds since composition, as `State`. Read it inside draw lambdas. |

### Screens
- **Splash** (`IntroScreens.kt`):
  - The first frame matches the system splash (same purple, `ic_splash_logo` at 288 dp).
  - The sky fades in, the logo glides up, "Marble / Pop / Factory" pops in word by word, and a glass tube loads.
- **Onboarding:** a `HorizontalPager` with parallax cards. Page 1's demo shows the real rule: only blocks with a clear path to the bottom open.
- **Main** (`MainScreen.kt`):
  - `AnimatedSky` blends the background palette (the current world on the Play tab, the menu sky on other tabs).
  - `BottomDock` has a sliding pink selection pill and a raised gold Play button.
- **Home map** (`MainScreen.kt` + `MapArt.kt`):
  - A reversed `LazyColumn` of 5000 `LevelNode`s.
  - Each item draws its glass-tube road segment (`drawRoad`; gold when finished) and one world-themed scenery prop (`drawScenery`) on the side away from the node.
  - Nodes are 3D discs (`drawNodeDisc`). Only the current node animates: ping rings and the player pin.
  - Chests sit every 10 levels and a world sign every 25.
  - The world shown at the screen centre drives the sky.
  - Tapping a **locked** level opens the Watch-Ad dialog (see section 9).
- **Tabs** (`Tabs.kt`):
  - `TabHeader` + `TabBody`: a scroll column with faded top and bottom edges, using one offscreen layer.
  - Shop has a free-coin hero, a booster grid, the Mega Bundle and a `RewardToast`.
  - Daily has the `LuckyWheel` (its face is baked into a bitmap once and rotated; chasing bulbs; a gem pointer that flicks), the daily challenge and the 7-day tiles.
  - Collection has a segmented switch, skin tiles with rarity chips, and theme cards.
  - Profile has the player card, stat tiles and achievements with progress tubes.
- **Settings / Legal** (`SettingsScreens.kt`):
  - Settings uses grouped cards with icon tiles, toggles and link rows with chevrons.
  - Legal shows the HTML page in a framed `WebView`. `legal.css` uses the same sky colours.
- **`GameScreen`:**
  - **Game loop:** `withFrameNanos` calls `engine.update(dt)` while no blocking overlay is open and bumps a frame counter, which invalidates the canvas.
  - **Input:** a tap is handled on pointer-down (`awaitFirstDown`) for responsiveness.
  - **HUD:** pause, a `Ribbon` level badge (plus a crown + "SUPER HARD" chip on boss levels), coins, and a glass `BoosterTray`. The tutorial hand shows on levels 1–2. The `ComboBanner` has rays and gradient text.
  - **Popups**, each in its own `PopupHost(visible = overlay == …)` so they animate in and out:
    - `INTRO`: the "New!" feature popup on a glowing pedestal.
    - `PAUSE`: with sound toggles.
    - `WIN`: rays, confetti, stars with sparkle bursts, coin count-up and the lucky multiplier needle (×2/×3/×5).
    - `FAIL`: jammed-belt illustration; continue for 150 coins, or retry.
    - `BUY`: buy a booster.
  - Rewards and stats are written to `GameRepository` once, when the engine status changes.

### Smoothness rules (keep these when adding UI)
1. **Draw-phase reads only.** Read animation values only inside `graphicsLayer {}`, `drawBehind {}` / `drawWithCache {}`, `offset {}` or `Canvas` lambdas. A value read during composition rebuilds the UI every frame. When text must change per frame (coin count-up, loading %), put it in its own small composable or use `derivedStateOf`.
2. **Animate only what is visible.** On the map, only the current node and claimable chests have infinite animations.
3. **Cache.** Use `drawWithCache` for paths and brushes. Bake complex static art into a bitmap (wheel face) or an offscreen layer (sky).
4. **No real-time blur.** Glass is faked with translucent fills and rim lights.
5. **Check frames on the device.** The SM-A076B screen runs at **120 Hz**. Use `Number Frame deadline missed` and the `gpu percentile` lines of `dumpsys gfxinfo`; the legacy percentiles and "Janky frames (legacy)" are not meaningful there. Results after this redesign: 0 missed deadlines on the home map, while scrolling, on tab switches, in popups and in gameplay. Median GPU time per frame: 6 ms on home, 3 ms in game.

---

## 8. Persistence (`core/GameRepository.kt`)

SharedPreferences file **`marble_pop`**. Every value is mirrored into Compose state, so the UI updates automatically.

| Key | Meaning |
|---|---|
| `onboarding` | onboarding finished |
| `level` | highest unlocked level |
| `stars` | 5001-char string, char *n* = stars ('0'–'3') of level *n* |
| `coins`, `booster_<NAME>` | currency and booster counts (start: 300 coins, 2 of each booster) |
| `skin`, `owned_skins`, `theme`, `owned_themes` | selected/owned cosmetics (`theme = -1` = auto: theme changes every 25 levels) |
| `ad_levels` | levels unlocked early with a rewarded ad |
| `music`, `sfx`, `haptics`, `name`, `avatar` | settings / profile |
| `stat_<NAME>`, `ach`, `chests` | stats for achievements, claimed achievements, opened chests |
| `spin_day`, `login_streak`, `login_day`, `daily_win`, `free_gift_at` | daily systems |

`resetProgress()` (Settings → Reset Progress) clears everything except the onboarding flag.

Economy constants live next to their features:
- `Booster` prices and the `Catalog` of skins and themes are in `GameRepository.kt`.
- `CONTINUE_PRICE` is in `GameScreen.kt`.
- `LOGIN_REWARDS` and `FREE_GIFT_COOLDOWN` are in the repository's companion object.
- Wheel prizes are in `Tabs.kt`.

---

## 9. Ads (`core/Ads.kt`) — current state and how to add a real SDK

Screens only use the **`RewardedAds`** interface:
```kotlin
interface RewardedAds {
    val isAvailable: Boolean
    fun show(placement: AdPlacement, onResult: (rewarded: Boolean) -> Unit)
}
```
- **Debug builds:** `Ads.rewarded = DebugRewardedAds`. `show()` opens `DebugAdOverlay`, a full-screen "TEST AD" with a 3-second countdown. **Get Reward** grants the reward and ✕ closes without it.
- **Release builds:** `NoRewardedAds`. `isAvailable = false`, so no "Watch Ad" buttons appear, locked levels stay locked, and skins and themes can only be bought with coins.
- **Where ads are offered today** (`AdPlacement`):
  - `UNLOCK_LEVEL`: tap a locked node on the home road. On reward, the level is added to `ad_levels` and opens.
  - `UNLOCK_SKIN` / `UNLOCK_THEME`: the **Unlock** button in Collection offers **Watch Ad** or **Buy with coins**. On reward, the item is granted and selected.

**To integrate a real SDK later** (e.g. AdMob):
1. Add the SDK dependency and the app ID to the manifest. Initialise it in `MainActivity.onCreate`.
2. Create `class AdMobRewardedAds(activity: Activity) : RewardedAds`:
   - Preload a rewarded ad and expose `isAvailable` as "an ad is loaded".
   - In `show()`, call the SDK's show method. Call `onResult(true)` from the reward callback, or `onResult(false)` on dismiss/failure, then preload the next ad.
3. Point `Ads.rewarded` at it, for example in `MainActivity` (make `Ads.rewarded` a `var`). Keep `DebugRewardedAds` for debug builds if you like.
4. Optionally map each `AdPlacement` to its own ad unit ID.
5. Update `assets/legal/privacy.html`: it currently says there are no ads or third parties. Also add a consent flow (UMP) for the EEA/UK.

Other natural places for rewarded ads, which currently don't use them: the free coin gift (Shop), the "Continue" after failing, extra spins, and claiming ×2 on the win screen.

---

## 10. Audio, fonts, icons, legal pages

- **Sounds:** `tools/gen_sounds.py` synthesises every WAV in `res/raw` (pure Python standard library, about 6 s). Run `python3 tools/gen_sounds.py` to rebuild everything, or `python3 tools/gen_sounds.py sfx_pop` to rebuild one sound. `SoundManager` enforces a minimum gap per `Sfx` so rapid events don't stack.
- **Icons:** `tools/gen_icon.py` regenerates the adaptive icon vectors, the legacy PNG mipmaps and `docs/store/*`. Edit the script, not the generated XML.
- **Legal:** `assets/legal/terms.html` and `privacy.html` contain a **placeholder support e-mail** (marked `TODO(developer)`); replace it before release.
- **Fonts:** Lilita One and Fredoka, both SIL Open Font License.

---

## 11. Debugging helpers

- **Jump to any level on a connected device** (debug build only; uses `run-as` to edit the save file):
  ```bash
  tools/jump_level.sh 120 shot_name     # sets level=120, default skin/theme, opens the game, saves shot_name.png next to the script
  ```
- **Frame timing:** `adb shell dumpsys gfxinfo com.puzzel.lab.marbelshort` (call `… reset` first). Read `Number Frame deadline missed` and the gpu percentiles; see the smoothness rules in section 7.
- **Inspect the save file:** `adb shell run-as com.puzzel.lab.marbelshort cat shared_prefs/marble_pop.xml`

---

## 12. How to… (common changes)

**Add a new block type**
1. Add a value to `BlockKind` (and a `Feature` entry with an unlock level and popup text if it should be introduced).
2. Rules: extend `Board.isLocked` / `canTap` / `tap` (and `counterRemaining` if it shows a number).
3. Generator: assign it in `LevelGenerator.build()` *consistently with the planned order*, and add it to `weight`/`countFor`.
4. Engine effects on pop: `GameEngine.streamOut` / `burstAtMouth` (see how `SWITCH` reverses the belt).
5. Art: draw an overlay in `Painter` and register it in `SpriteCache.overlay`, then use it in `GameRenderer.drawBlock`. Add a preview to `FeatureIntroContent` in `GameScreen`.
6. Run `testDebugUnitTest`.

**Tune difficulty**: change `handmade` (levels 1–30) or `recipeFor()` (block count, colours, cup window, belt margin, feature chances). The tests guarantee solvability, so run them after every change.

**Add a theme**: append a `GameTheme` to `Themes.all` and a `ThemeInfo` (name, price) to `Catalog.themes`. The board, dish and backdrop colours are derived from the theme fields.

**Add a marble skin**: add a branch in `MachineArt.marbleBody` (and `marbleGloss` if lighting differs) and a `Skin` to `Catalog.skins`. The index must match.

**Add a sound**: add a generator in `tools/gen_sounds.py`, run it, then add an `Sfx` entry pointing at the new `R.raw` file.

---

## 13. Known gaps / TODO

- **Unused art:** `MachineArt.crate/gear/cradle/chromeRect/rivet` are from the earlier factory-machine design and are unused; delete them if not needed.
- **Visual check:** with the current playground, only levels 1, 3, 6, 14, 15, 23 and 24 were checked by eye on a device (plus the ad flows). The other special blocks and themes are covered only by the rule and solvability tests. The fail popup and the confetti were not seen on a device after the UI redesign.
- **World names:** the 4th theme is still called `Lava` in `Themes.kt` / `Catalog`; the UI shows it as "Sunset". Rename it in data if you want the names to match.
- **Release:** release signing is not configured, ads are not integrated (see section 9), and the legal pages contain a placeholder e-mail.
