# Marble Pop – Color Sort Puzzle — Developer Guide

This document explains how the game is built, so a new developer can find their way around the code, change gameplay safely and add features (ads, new blocks, themes…).

- **Package / applicationId:** `com.ngstudio.marblepop`
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
app/src/main/java/com/ngstudio/marblepop/
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
│  ├─ MachineArt.kt         procedural art: marbles, jelly blocks, lid-box cups, belt socket, roller hub (+ unused legacy parts)
│  ├─ Sprites.kt            `Col` colour helpers, `Painter` (icons/overlays/text), `SpriteCache`
│  └─ Themes.kt             14 worlds (colour themes + sky palettes) and the level each one unlocks at
└─ ui/
   ├─ MarbleApp.kt          navigation (Screen + Nav), screen transitions, music switching, ad overlay
   ├─ theme/Theme.kt        design tokens: GameColors, Grad (gradients), Skies (sky palettes), Motion, fonts, text styles
   ├─ components/
   │  ├─ Components.kt      jelly GameButton, candy OutlinedText, Ribbon, GamePanel, PopupHost, cards, chips, toggles…
   │  ├─ Backgrounds.kt     SkyBackground, SunRays, ConfettiBurst, rememberTime(), SoftDot/SparklePath
   │  ├─ Icons.kt           ~37 icons drawn in code (GameIcon / drawGameIcon) + ball()
   │  └─ AdComponents.kt    WatchAdDialog, DebugAdOverlay
   └─ screens/              Splash, Onboarding, Main (home map + dock), MapArt (map drawing), Tabs, Game, Settings, Legal

app/src/main/assets/legal/  copy of public/ (privacy_policy.html, terms_conditions.html, index.html, legal.css, fonts, logos), opened in a WebView
public/                     the website: same pages, upload the folder as is (public.zip)
app/src/main/assets/fonts/  fonts used by the HTML pages
app/src/main/res/raw/       all sounds (generated WAVs)
app/src/main/res/font/      lilita_one.ttf, fredoka.ttf (variable) — SIL OFL
app/src/test/.../LevelGeneratorTest.kt
tools/gen_sounds.py         regenerates every sound (pure Python, no dependencies)
tools/set_app_icon.py       builds launcher icons, in-app logo and store icon from art/app_icon_source.png (needs Pillow)
tools/gen_icon.py           renders the Play Store feature graphic (old vector icon code kept for reference)
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
7. **Verify** with `PuzzleSim`. The belt **capacity is the plan's peak load plus a difficulty margin** (clamped to 14…28: 28 balls of `GameLayout.BALL_R` fit on the belt. Before 2026-11-24 the limit was 48; most levels then needed 32–38 slots. With 28, the generator mixes the cup order a little less to keep the peak low. Boards, colours and special blocks are unchanged, and no level falls back to the simple board. Capacity is now about 25 from level 31 on). If the peak is too high, the window shrinks and it retries; if that fails, the generator uses a fresh seed.

**Difficulty curve (retuned 2026-11-21: "hard much sooner" plus a tighter belt):**
- **Levels 1–10** (`TUTORIAL_LEVELS`) are the unchanged hand-made tutorial.
- **Levels 11–30** keep their hand-made boards and feature introductions, but with a tight belt (3 spare slots for 11–20, 2 for 21–30), a more mixed cup order and, from 21, one extra colour.
- **31+:** `d = 0.6 + 0.4 · (1 − e^(−(n−30)/15))`, plus the saw-tooth and the boss and "7" bumps, minus 0.12 on a feature-intro level. Difficulty is close to the maximum by about level 60.
  - Board size: about `20 + 22·d` blocks (at most 6×7).
  - Colours: `4 + 6·d` (at most 10).
  - Spare belt slots: `4 − 2.5·d`, at least 2, or 1 on boss levels. Capacity = plan peak + margin.
  - Cup-order window: `2 + 9·d`.
- **Special blocks and hidden cups (raised the same day):**
  - Up to `2 + 3·d` feature types per level, each with probability `(0.6 + 0.4·d)·weight`.
  - Counts per type:

    | Feature | Count |
    |---|---|
    | Mystery block, hidden cup | `3 + rand(3 + 6·d)` |
    | Cage, crate, ice, arrow (and the rest) | `2 + rand(2 + 5·d)` |
    | Key, paint | `1 + rand(1 + 2·d)` |

  - Each key chains 2–4 blocks.
  - Levels 11–30 get one more of each of their special blocks (keys excepted).
  - Averages: about 17–20 special blocks and 3–5 hidden cups per level from 31 on.
- **Measured win rates** (no boosters; measured before the 28-slot belt cap of 2026-11-24, so re-measure):

  | Levels | Smart player | Perfect-play bot |
  |---|---|---|
  | 31–60 | 65% | 85–96% |
  | 61–200 | 38% | 85–96% |
  | 201+ | 46–56% | 85–96% |

  - The *smart player* pops the colour the front cups need, without knowing the solution. That morning it won 72–95% of levels 11–200.
  - The *perfect-play bot* follows the plan and waits when the next block is still locked (e.g. a crate waiting for cups). It shows the levels stay winnable.
  - The bots' script is not in the repo: `DifficultyPrint`, kept as a scratch file.
- **Belt limits:** a tighter belt (1 spare slot) or much more cup mixing (window `4 + 16·d`) mostly made levels unfair. The perfect-play bot fell to about 77% while the smart player barely changed, so special blocks and hidden information are the main difficulty lever.

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
  - `stars`:
    - 3: won without boosters and the belt never got completely full (`peakBelt < capacity`).
    - 2: a booster was used, or the belt filled up at some point.
    - 1: "Continue" was used.
  - The old rule (≤62% peak belt use for 3 stars, ≤86% for 2) stopped working after the difficulty change. Levels leave only about 2 spare slots, so even perfect play gave 1 star.
  - Measured on wins:

    | Player | 3-star wins (levels 11+) |
    |---|---|
    | Perfect-play bot | 77–87% |
    | Smart player | 56–59% |

    Levels 1–10 give 3 stars to both.
  - `coinReward()` gives the base coins plus a star bonus, combo coins and a boss bonus.
- **Events:** the engine never touches Android. It reports through `GameListener` (`sound`, `haptic`, `combo`, `statusChanged`); `GameScreen` implements it.

### Funnel capacity: the funnel fills up to the line under the board, then "WAIT!"
Balls fill the whole funnel, up to the line just under the bottom row of blocks. Only when it is full does a tap get refused. Balls never gather up in the board.
- **`hopperCap` is measured per level.** At level start `measureFunnel()` silently drops test balls spread across the funnel (golden-ratio steps) through the real physics and lets each one settle. It counts how many fit before the settled pile reaches the line (centre above `gridBottom + 0.25·r`), minus a small slack (1, or capacity/15) for balls still rolling in. With 21-unit balls on tall phones it is typically 20–25 balls (at least one x2 block of 18).
- **All or nothing:** `funnelHasRoom(block)` lets a block release only if all of its balls fit: balls in the funnel or on their way, plus its own 9 (18 for x2), must fit within `hopperCap`, and the settled pile must not have reached the line anywhere (`pileAtLine()`, e.g. a corner under a side exit). An empty funnel always takes one block. Otherwise the block stays closed and whole, wobbles, and "WAIT!" floats over the funnel. `hammer()` refuses the same way without using up the booster. An opened block never keeps balls back: all of them leave together, with only the short start stagger. (A version that let the leftover balls wait inside the opened block was removed at the user's request.)
- **Physics that fills the width:**
  - Gravity rolls balls down the slopes. The only centre drift is a small non-accumulating creep near the pipe mouth (a drift added to `x` but not to `px` acts like sideways gravity and stacked balls into a tower).
  - Balls leave an exit rolling toward the middle, so the funnel fills from the centre outward.
  - The stall nudge only acts when the pipe is empty.
  - A funnel ball can't be squeezed more than 0.2·r above the line; a ball above the line is eased inward.

### Funnel anti-jam (no stuck balls in the "tunnel")
Round balls in a narrowing funnel can wedge side by side above the pipe mouth, forming an "arch". This happened in the original code too.
- **Leader:** while the pipe is empty, `physics()` picks the lowest ball in the funnel as the leader. It is steered toward the pipe centre (harder the lower it is), and in a collision with another ball it holds its place while the other ball takes the whole push. The funnel therefore always feeds one ball at a time.
- **Regression test:** `FunnelFlowTest` plays about 490 levels on two screen heights, tapping every 0.15 s, which is the worst case for the funnel. It fails if:
  - balls sit in the funnel with none reaching the pipe for 2 s;
  - a ball waits at the gate for more than a lap while the belt has a free slot;
  - a launched ball waits at an exit because the funnel is full;
  - a funnel ball stays more than a quarter-radius above the line for more than 1 s;
  - an opened block keeps some balls back for more than 0.8 s (partial release).

  Before these fixes, 713 of 976 runs got stuck and every run piled balls into the board. Now none do.

### Belt speed
The belt normally runs at `beltSpeed` (175 units/s). `beltBoost()` switches it to `BELT_BOOST` (2x) while no ball can board, and the speed eases between the two.
- **Nothing waiting:** no ball is in the board or funnel (`PATH` / `PHYSICS`), so every released ball is already on the belt and is delivered to the cups quickly.
- **Belt full:** balls are waiting but the belt has no free slot, so the boost clears it sooner.

As soon as balls are waiting and a slot is free, the belt goes back to normal speed so they can board. The jam and no-move timers still use the normal `lapTime`, so a boosted belt never fails a level early.

### Boosters (counts saved in `GameRepository.boosters`)
| Booster | Engine call | Effect |
|---|---|---|
| Hammer | `hammer(x, y)` | Force-pops any block (ignores locks and paths) |
| Magnet | `magnet()` | Sends belt marbles of the best colour into any matching cups, even buried ones |
| +6 Slots | `boosterSlots()` | Grows belt capacity and re-maps the marbles onto the new slots |
| Shuffle | `boosterShuffle()` | Shuffles the colours of cups that have no reserved marbles |

Notes (checked 2026-11-21):
- **Not used up when they do nothing.** A refused hammer (full funnel → "WAIT!"), a magnet with nothing to pull, or a shuffle with fewer than 2 cups doesn't consume the booster. Every use caps the level at 2 stars.
- **Belt label:** the renderer rebuilds the "count / capacity" label when *either* number changes. It used to wait for the count, so right after +6 Slots the old capacity stayed on screen. `magnet()` updates `beltCount` straight away.
- **Crowded belt:** the belt has a fixed width. When boosters or Continue add slots, belt marbles shrink to `0.49 · spacing` so they never overlap, and the rivets between sockets hide.
- **`BoosterTest`** covers each booster:

  | Booster | Checked |
  |---|---|
  | +6 Slots | capacity grows by 6, no marble is lost, slot indices stay consistent |
  | Magnet | only matching colours move, cups never over-fill |
  | Shuffle | the same colour set, cups being filled are untouched, hidden cups are revealed |
  | Hammer | locked blocks are smashed; it is refused, and not used up, when the funnel is full |

  It also plays whole levels using every booster, checking that they finish without losing a marble.

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
- **Moving things are single bitmap blits from `SpriteCache`:**
  - `marbleShadowed` (each marble is one image with its shadow built in).
  - `block(color, variant, size, skin, open)`, which has separate open and closed versions that cross-fade via `BlockVisual.open`.
  - `cup`, `socket`, `hub`, and `overlay` (cage, chains, crate, ice, key, arrow, bomb, bucket, switch).
- **Belt vs. tray:** `trayTop` leaves the belt housing (`HOUSING_RIM` 13 past the ball track), its shadow (7) and a clear `TRAY_GAP` (16) above the first row of cups.
- **Board vs. HUD:** `GameScreen` passes the bottom of the HUD (status bar + 150dp: top row, booster tray and a small gap) as `topInset`. `GameLayout` keeps `boardPadTop` (= `BOARD_FRAME` 24 + 10) above the top row, because the board frame reaches 24 units above the grid. `LayoutFitTest` lays out all 5000 levels on six phone and tablet screens. It checks that the frame stays below the HUD, that the belt clears the funnel, and that at least 3 cup rows are visible. **Run it after changing the HUD, `GameLayout` or the board frame.** (Fixed 2026-11-24: tall boards such as level 111 had their top blocks under the booster tray.)
- **Marble sizes:** marbles that burst out of a block use `GameLayout.hopperR` while they travel through the board, the hopper and the pipe. Since 2026-11-24 it is `BALL_R` = 21, the size in the reference art (about 42 px on a 720-wide screen), on tall phones. It scales down to `MIN_BALL_R` = 14 on short screens (16:9 phones, tablets), following `roomy` (the height below the HUD, 940 → 1240 units). The funnel grows with it, from 110–170 to 170–250 units tall, so it still holds an x2 block (18 balls); short screens could not fit both big balls and a usable board. The spout width, gate and hopper physics are sized from it. On the belt they keep the same size (`GameLayout.beltMarbleR = min(hopperR, belt fit)`) and sit side by side, like the reference art (2026-11-24).
  - *Belt fit* is the largest radius at which `capacity` marbles fit on the belt (`0.49 · length / capacity`). With at most 28 slots that is always the full ball size. Only the +6 slots booster (or Continue) makes belt marbles a little smaller.
  - The belt grows with the marble: `beltR = 22 + beltMarbleR`, which keeps the gauge island the same size, and the tray moves down to clear the housing.
  - Cup marbles use `GameLayout.cupMarbleR`: the belt size, capped so three fit in a cup with a thin wall between the holes (`0.43 · HOLE_SPREAD · cupW`) and fit its height (`0.27 · cupH`). That is about 15.7 units with 4 columns and 14 with 5 (it was a fixed 10.8 before 2026-11-24). Cup holes are drawn at `1.04 · cupMarbleR`, with centres `MachineArt.HOLE_SPREAD` (0.29) · cupW apart. A marble changes from belt to cup size while it drops.
- **Block colours:** blocks and cups use solid, saturated marble colours, as in the reference (palette in `MarbleColors`).
  - Blocked blocks are a solid sealed lid with no marbles shown.
  - Open blocks show their 9 marbles.
- **Palette:** colours come from the level's `GameTheme` (`Themes.kt`, see "Worlds" below).
  - The belt track is deep indigo, only lightly tinted by the world, so marbles stand out.
  - Cell wells use a neutral slate tint, so pastel blocks never blend into the board in the pink or green worlds.
- **Art painters:**
  - `MachineArt.marble` draws the layered glass marble with a fixed-light gloss and 7 skins. Index 0 is "Glossy", a cat's-eye glass marble.
  - `MachineArt.jelly` draws the board block:
    - Open: raised, with a bevel, a recessed window holding 9 marbles, and a glass gloss and glint.
    - Blocked: lower, with the marbles faded behind a frosted lid.
    - Mystery: a lavender candy stripe with a gold "?".
  - `MachineArt.puck` draws the cup as a "jelly box": a rounded candy box with a depth side, bevel and glint, and 3 recessed holes. The hole centres must match `GameLayout.dimpleDx` / `dimpleDy`, which the engine uses as drop targets and the renderer uses to draw the marbles sitting in a cup.
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

### Worlds (`render/Themes.kt`)
Themes are **worlds that unlock by progress**, not shop items. `Themes.all` is in unlock order. A level uses the world with the highest `unlockLevel` at or below its number (`Themes.indexForLevel`, also `LevelGenerator.themeFor`). In game, `GameRepository.themeForLevel(n, daily)` uses the level's own world, the same one the map shows, so a level unlocked early with an ad (e.g. 82 while the player is on 5) is shown in its world (Forest). Only daily challenges, which pick a random level from 100 to 3099, are capped at the player's highest level so they never reveal a far-off world.

**Unlock early and choose (2026-11-25):** in Collection → Worlds, a locked world has an **Unlock** button that opens `WatchAdDialog` (`AdPlacement.UNLOCK_WORLD`; the button only shows while `Ads.rewarded` is available, so debug builds today). A rewarded ad adds the world to `adUnlockedWorlds` (pref `ad_worlds`) and selects it. Any unlocked world has **Use**, which sets `worldChoice` (pref `world_choice`); the **Auto** card at the top sets it back to -1. While a world is chosen, `themeForLevel()` returns it for every level, including daily challenges, and the "New World!" card is skipped (the world is just marked seen). `unlockedTheme` (shown as "your world now") still follows progress only.

| # | World | From level | Map scenery |
|---|---|---|---|
| 1 | Candy Lab | 1 | candy |
| 2 | Ocean | 40 | ocean |
| 3 | Forest | 80 | forest |
| 4 | Sunset | 120 | desert crystals |
| 5 | Bubblegum Bay | 180 | candy |
| 6 | Space | 250 | space |
| 7 | Frosty Peaks | 350 | snow |
| 8 | Honey Hive | 500 | forest |
| 9 | Jelly Lagoon | 700 | ocean |
| 10 | Choco Falls | 1000 | candy |
| 11 | Neon Arcade | 1400 | space |
| 12 | Gear Works | 2000 | desert crystals |
| 13 | Volcano Forge | 2800 | desert crystals |
| 14 | Royal Galaxy | 3800 | space |

Why these levels: new worlds come every 40 levels at first, so players who just hit the hard levels (31+) get a visible reward often. After that the gaps widen (60, 70, 100, 150, 200, 300, 400, 600, 800, 1000) because few players get far and each world should feel earned. The last world still leaves 1200 levels before level 5000.

- **"New World!" card:** the first time a player opens a level in a world above `theme_seen`, `GameScreen` shows `NewWorldContent` (overlay `WORLD`) before any feature intro, then calls `markThemeSeen`. Daily challenges never show it. On first launch `theme_seen` is set to the player's current world, so existing players aren't shown cards for worlds they already reached.
- **Players who bought themes before this change:** `migrateBoughtThemes()` runs once. It deletes the old `theme` pick and `owned_themes`, and refunds the old coin price of every owned theme (Ocean 800, Space 1500, Lava/Sunset 1200, Forest 1000, kept in `Catalog.legacyThemePrices`). Themes unlocked with an ad can't be told apart from bought ones, so they are refunded too. The home map then shows a one-time "Worlds Update" popup with the amount.

### Design tokens (`theme/Theme.kt`)
| Token | What it holds |
|---|---|
| `GameColors` | Flat colours. `Ink` (#2A1463) is the outline colour for almost everything. |
| `Grad` | Brushes for candy text and panels (`Gold`, `Pink`, `Aqua`, `Mint`, `Snow`, `Coral`, `PanelRim`, `PanelFill`). |
| `Skies` | `SkyPalette`s: `Menu`, plus one per world via `Skies.forWorld(themeIndex)`, built from `GameTheme.sky` so the map and the game share one source. |
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
  - Chests sit every 10 levels. A world sign ("WORLD n" + the world's name) sits on each world's first level.
  - Scenery props come from the world's `GameTheme.scenery` set: candy, ocean, space, desert crystals, forest or snow.
  - The world shown at the screen centre drives the sky.
  - Tapping a **locked** level opens the Watch-Ad dialog (see section 9).
- **Tabs** (`Tabs.kt`):
  - `TabHeader` + `TabBody`: a scroll column with faded top and bottom edges, using one offscreen layer.
  - Shop has a free-coin hero, a booster grid, the Mega Bundle and a `RewardToast`.
  - Daily has the `LuckyWheel` (its face is baked into a bitmap once and rotated; chasing bulbs; a gem pointer that flicks), the daily challenge and the 7-day tiles.
  - Collection has a segmented switch (Marbles / Worlds), skin tiles with rarity chips, and world cards. World cards are read-only: playing, unlocked, or locked with "Reach level N". The map's **Worlds** side button opens that page (`Nav.collectionPage`).
  - Profile has the player card, stat tiles and achievements with progress tubes.
- **Settings / Legal** (`SettingsScreens.kt`):
  - Settings uses grouped cards with icon tiles, toggles and link rows with chevrons.
  - Legal shows the HTML page in a framed `WebView`. `legal.css` uses the same sky colours.
- **`GameScreen`:**
  - **Game loop:** `withFrameNanos` calls `engine.update(dt)` while no blocking overlay is open and bumps a frame counter, which invalidates the canvas.
  - **Input:** a tap is handled on pointer-down (`awaitFirstDown`) for responsiveness.
  - **HUD:** pause, a `Ribbon` level badge (plus a crown + "SUPER HARD" chip on boss levels), coins, and a glass `BoosterTray`. The tap tutorial hand shows on levels 1–2; special blocks have their own spotlight tutorial (see `TUTORIAL` below). The `ComboBanner` has rays and gradient text.
  - **Popups**, each in its own `PopupHost(visible = overlay == …)` so they animate in and out:
    - `INTRO` and `TUTORIAL`: teaching special blocks. When a level contains special-block features the player has never been taught, the level opens like this:
      1. **Cards:** a "New!" card (on a glowing pedestal) for each new feature, one after another. Each card shows "n / total" and a Next button; the last one says "Show me!". `Feature.presentIn(spec)` lists the level's features: chains count as KEY, grey blocks as PAINT, hidden cups as HIDDEN_CUP.
      2. **Spotlight:** once the board has assembled (`!engine.inIntro`), each feature gets a tutorial step. The board dims and a real block of that kind (or the highest visible hidden cup) is ringed. `engine.featureTarget(f)` finds it. The hand points at it, and a card gives the how-to (`tutorialTip`). The game is paused meanwhile. The last step says "Let's play!".
      3. **Remembered:** `GameRepository.seenFeatures` (pref `seen_features`) keeps each feature from being taught twice. The first time this version runs, features whose unlock level the player has already passed count as seen.
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
| `skin`, `owned_skins` | selected/owned marble skin |
| `theme_seen` | highest world index whose "New World!" card was shown |
| `theme_refund` | coins returned for bought themes, waiting for the one-time "Worlds Update" notice |
| `theme`, `owned_themes` | **legacy**, removed on first launch by `migrateBoughtThemes()` (see "Worlds") |
| `ad_levels` | levels unlocked early with a rewarded ad |
| `music`, `sfx`, `haptics`, `name`, `avatar` | settings / profile |
| `stat_<NAME>`, `ach`, `chests` | stats for achievements, claimed achievements, opened chests |
| `spin_day`, `login_streak`, `login_day`, `daily_win`, `free_gift_at` | daily systems |

`resetProgress()` (Settings → Reset Progress) clears everything except the onboarding flag.

Economy constants live next to their features:
- `Booster` prices and the `Catalog` of skins are in `GameRepository.kt`. Themes are not sold; they unlock by level (`Themes.kt`).
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
- **Release builds:** `NoRewardedAds`. `isAvailable = false`, so no "Watch Ad" buttons appear, locked levels stay locked, and skins can only be bought with coins.
- **Where ads are offered today** (`AdPlacement`):
  - `UNLOCK_LEVEL`: tap a locked node on the home road. On reward, the level is added to `ad_levels` and opens.
  - `UNLOCK_SKIN`: the **Unlock** button on a marble skin in Collection offers **Watch Ad** or **Buy with coins**. On reward, the skin is granted and selected. (Worlds can't be unlocked with ads or coins.)

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
- **Icons:** the app icon is the artwork `art/app_icon_source.png` (2026-11-25). `python3 tools/set_app_icon.py` turns it into the legacy and round mipmaps, the adaptive icon (the whole picture is the background layer `mipmap-*/ic_launcher_background.png`, with an empty foreground; the art fills 80dp of the 108dp layer, so the 72dp launcher mask shows about 90% of it), the in-app logo `drawable-nodpi/app_icon.png` (loading screen, Settings footer) and `docs/store/icon_512.png`. To change the icon, replace the artwork and re-run the script. There is no themed (monochrome) icon any more. The notification icon `ic_notification.xml` is still the old funnel silhouette, because notification icons must be one colour. `tools/gen_icon.py` now only renders `docs/store/feature_1024x500.png`.
- **Legal:** Settings → Privacy / Terms first loads the live page from **https://game-marblepop.web.app** (`LEGAL_SITE` in `SettingsScreens.kt`). If the phone is offline, the site returns an error, or the page doesn't load within 8 s, it shows the same page from `assets/legal/` instead. The pages live in `public/` (website) and are copied as they are into `app/src/main/assets/legal/` (in-app WebView, Settings → Terms / Privacy). Edit `public/`, then copy it over: `rm -rf app/src/main/assets/legal && cp -R public app/src/main/assets/legal`. Contact: store.ngdtechnolab@gmail.com.
- **Fonts:** Lilita One and Fredoka, both SIL Open Font License.

---

## 11. Debugging helpers

- **Developer Tools in Settings (debug builds only, 2026-11-25):** a card above "Progress" for checking the Firebase and Play features on a device:
  - *Send reminder now*: posts today's morning or evening reminder through `DailyReminders.show()`, the same path the 9 AM / 7 PM alarm uses.
  - *Copy push token*: copies the FCM token. Paste it in Firebase console → Messaging → "Send test message".
  - *Simulate app update*: swaps in Play's `FakeAppUpdateManager` and plays a flexible update through to "downloaded", so the "Update Ready!" popup and Restart path can be checked without Play.
  - *Log test error (non-fatal)* and *Test crash*: send a Crashlytics non-fatal error or a real crash. Reports upload on the next launch.
  - Analytics DebugView: `adb shell setprop debug.firebase.analytics.app com.ngstudio.marblepop` (`.none.` turns it off).
- **Add test coins (debug builds only):** every coin pill (home, tabs and game HUD) shows a green "+" that opens a "Coins · DEBUG BUILD" popup with +100, +1,000 and +10,000 buttons (and "Open Shop" from home). The pieces live in `ui/components/AdComponents.kt`:
  - `CoinPill(value, modifier, onShop)` draws the pill.
  - The `DebugCoins` state object holds whether the popup is open.
  - `DebugCoinsOverlay()` is the popup, rendered once at the app root next to `DebugAdOverlay`.

  In release builds `BuildConfig.DEBUG` is false: the "+" only appears where `onShop` is given (home) and simply opens the Shop.

- **Jump to any level on a connected device** (debug build only; uses `run-as` to edit the save file):
  ```bash
  tools/jump_level.sh 120 shot_name     # sets level=120, default skin, skips the "New World!" card, opens the game, saves shot_name.png next to the script
  ```
- **Frame timing:** `adb shell dumpsys gfxinfo com.ngstudio.marblepop` (call `… reset` first). Read `Number Frame deadline missed` and the gpu percentiles; see the smoothness rules in section 7.
- **Inspect the save file:** `adb shell run-as com.ngstudio.marblepop cat shared_prefs/marble_pop.xml`

---

## 12. How to… (common changes)

**Add a new block type**
1. Add a value to `BlockKind` (and a `Feature` entry with an unlock level and popup text if it should be introduced).
2. Rules: extend `Board.isLocked` / `canTap` / `tap` (and `counterRemaining` if it shows a number).
3. Generator: assign it in `LevelGenerator.build()` *consistently with the planned order*, and add it to `weight`/`countFor`.
4. Engine effects on pop: `GameEngine.streamOut` / `burstAtMouth` (see how `SWITCH` reverses the belt).
5. Art: draw an overlay in `Painter` and register it in `SpriteCache.overlay`, then use it in `GameRenderer.drawBlock`. Add a preview to `FeatureIntroContent` in `GameScreen`, a how-to line to `tutorialTip`, and map the kind in `Feature.presentIn` / `GameEngine.featureTarget` if its name differs from the feature.
6. Run `testDebugUnitTest`.

**Tune difficulty**: change `handmade` (levels 1–30) or `recipeFor()` (block count, colours, cup window, belt margin, feature chances). The tests guarantee solvability, so run them after every change.

**Add a world (theme)**: insert a `GameTheme` into `Themes.all` at its place in the unlock order, with its `unlockLevel` (higher than the one before it) and a `scenery` set. Fill every colour field and a 6-colour `sky`; the board frame, glass, belt tint and backdrop are derived from `wall`/`wallDark`/`wallLight` and `sky`. Add a chip colour to `WorldColors` in `MainScreen.kt`. `ThemeScheduleTest` checks the order and the palette.

**Add a marble skin**: add a branch in `MachineArt.marbleBody` (and `marbleGloss` if lighting differs) and a `Skin` to `Catalog.skins`. The index must match.

**Add a sound**: add a generator in `tools/gen_sounds.py`, run it, then add an `Sfx` entry pointing at the new `R.raw` file.

---

## 13. Known gaps / TODO

- **Firebase config is a stopgap (2026-11-25):** the package is now `com.ngstudio.marblepop`, but `app/google-services.json` only holds a copy of the old app's entry (`com.puzzel.lab.marbelshort`) with the new package name, so the build passes. Firebase (messaging, Crashlytics, Analytics) is still registered for the old package. Add `com.ngstudio.marblepop` as an Android app in Firebase project `marble-pop-factory` and replace `app/google-services.json` with the downloaded file.
- **Unused art:** `MachineArt.crate/gear/cradle/chromeRect/rivet` are from the earlier factory-machine design and are unused; delete them if not needed.
- **Visual check:** with the current playground, only levels 1, 3, 6, 14, 15, 23 and 24 were checked by eye on a device (plus the ad flows). The other special blocks and themes are covered only by the rule and solvability tests. The fail popup and the confetti were not seen on a device after the UI redesign.
- **Release:** release signing is not configured, and ads are not integrated (see section 9).
