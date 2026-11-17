# Marble Pop Factory: Full UI Graphics Upgrade Plan

This plan covers the visual and motion upgrade of every screen of the app. The playfield engine and its renderer stay as they are (they were rebuilt to be light and smooth last round). Everything around them gets upgraded: menus, HUD, popups, transitions and micro-interactions.

Target device class: budget Android phones (test device Samsung SM-A076B, 720 × 1600). Every effect below has to stay at 60 fps there.

---

## 1. Design language: "Candy Glass Factory"

The look is glossy, toy-like and bright. It should feel like polished candy and glass sitting in a dreamy sky, not flat UI on a flat colour.

The depth model gives every interactive element four layers:

| Layer | What it is | How it is drawn |
|---|---|---|
| Shadow | Soft dark blur under the element | Stacked translucent shapes, or a hardware `shadow()` for cards |
| Depth | Solid darker "side" of the object | Same shape offset down 10–12% |
| Body | Main colour, light top to saturated bottom | Vertical gradient |
| Gloss | White highlight on the top 40% | White gradient pill, plus a thin rim light |

All text on colour uses **candy text**:
1. A soft drop-shadow stroke.
2. A thick dark outline.
3. A gradient or flat fill.
4. An optional gloss layer on the top half.

### 1.1 Colour tokens (`ui/theme/Theme.kt`)

| Token | Value | Use |
|---|---|---|
| Ink | `#2A1463` | Outlines, dark text |
| InkSoft | `#5B4A8E` | Body text on cards |
| Panel rim | `#7C55FF → #4521C2` | Popup and card frames |
| Panel fill | `#FFFFFF → #F2ECFF` | Popup and card interior |
| Glass | white 16% fill, white 35% rim | HUD trays, side bubbles, bottom dock |
| Gold text | `#FFF6B0 → #FFC21A` | Rewards, titles |
| Pink text | `#FFC2E2 → #FF4C97` | "Pop", combo |
| Aqua text | `#C9F6FF → #22B8F0` | "Factory", info |

Button colours each have a top, bottom, depth and outline shade:

| Name | Use |
|---|---|
| Green | Play / confirm |
| Yellow | Buy / collect |
| Pink | Highlight |
| Purple | Neutral |
| Blue | Secondary |
| Red | Danger / close |
| Orange | Bundle |
| Gray | Disabled |

### 1.2 Sky palettes (animated backgrounds)

Each palette is three gradient stops, two coloured glows and a silhouette colour.

| Palette | Top → mid → bottom | Glows |
|---|---|---|
| Menu / Candy Lab | `#5A3BEF → #8A52F5 → #E77BD8` | pink, aqua |
| Ocean world | `#1F5FD6 → #2E9BEA → #5FD9E6` | ice-blue, mint |
| Space world | `#140A3F → #2D1B7A → #6A2EB8` | lilac, blue |
| Sunset world (was Lava) | `#FF8A5C → #F2557A → #9C3FB5` | gold, pink |
| Forest world | `#3CC88A → #1FA27A → #137A66` | lime, mint |

The home map blends smoothly between palettes as the player scrolls from one world to the next.

### 1.3 Typography

| Style | Font | Size |
|---|---|---|
| Display | Lilita One | 44–72 sp, gradient fill, gloss |
| Title | Lilita One | 30–34 sp |
| Heading | Lilita One | 20–24 sp |
| Body | Fredoka SemiBold | 15–17 sp |
| Caption | Fredoka Medium | 12–13 sp |

### 1.4 Motion tokens

| Token | Value | Use |
|---|---|---|
| micro | 120 ms | Press feedback |
| fast | 200 ms | Fades, small moves |
| standard | 300 ms | Tab and screen moves |
| emphasized | 420 ms | Hero moves |
| Easing in | cubic(0.05, 0.7, 0.1, 1) | Entering elements |
| Easing out | cubic(0.3, 0, 0.8, 0.15) | Leaving elements |
| Spring bouncy | damping 0.5, stiffness 450 | Pops, popups, stars |
| Spring snappy | damping 0.75, stiffness 700 | Presses, sliding indicators |
| Spring gentle | damping 0.85, stiffness 220 | Large layout moves |

---

## 2. Component library (rebuilt)

| Component | File | Upgrade |
|---|---|---|
| `OutlinedText` (candy text) | Components.kt | Adds a gradient `brush`, a `gloss` layer and a `maxLines` option |
| `GameButton` | Components.kt | 4-layer jelly look, darker outline, `shine` sweep for main buttons, light haptic tick, deeper press, icon-only mode without padding bugs |
| `IconRoundButton` | Components.kt | Same jelly render; the icon moves with the press |
| `ResourcePill` | Components.kt | Dark glass capsule with rim light; the icon "bumps" and the number counts when the value changes |
| `GamePanel` | Components.kt | Gradient rim frame, cream interior with a faint dot pattern, folded-tail **ribbon** title, optional rotating **sun rays** behind |
| `PopupHost(visible)` | Components.kt | Animated enter **and exit**: dim fade plus spring pop-in, quick shrink-fade out |
| `SectionCard` | Components.kt | Candy card: rim, 3D bottom edge, inner highlight, optional coloured header strip |
| `IconTile` | Components.kt | Gradient rounded-square tile with gloss, used for icons in rows and grids |
| `SegmentedSwitch` | Components.kt | Track with a sliding jelly thumb (Collection tabs) |
| `ProgressTube` | Components.kt | Glass tube progress bar with gloss (achievements, splash) |
| `NotifyBadge` | Components.kt | Pulsing red dot or count badge |
| `GameToggle` | Components.kt | Candy switch with ON/OFF label and a marble knob |
| `ScreenHeader` / `TabHeader` | Components.kt | Glossy titles and consistent spacing |
| `SkyBackground` | Backgrounds.kt (new) | Cached static gradient and glows with a factory skyline, plus a light animated layer of soft bokeh and twinkles |
| `SunRays` | Backgrounds.kt | Rotating sunburst for rewards, splash and new features |
| `ConfettiBurst` | Backgrounds.kt | One-shot confetti for wins and big rewards |
| `rememberTime()` | Backgrounds.kt | Frame clock as `State`, read only in draw (no recomposition) |
| Icons | Icons.kt | New CROWN, CHEVRON, FLAME, CUP, FLAG, SPARK icons; outline polish on the coin |

---

## 3. Screen by screen

### 3.1 Splash
1. **Handoff:** the first frame looks exactly like the system splash (same purple and the same logo at the same size), so there is no visible jump.
2. **Logo move:** the sky fades in with slowly rotating sun rays, and the logo glides up while shrinking slightly.
3. **Title:** "Marble", "Pop", "Factory" pop in one by one with springs, each in its own glossy gradient (gold, pink, aqua), with a pop sound per word.
4. **Tagline:** "Tap · Pop · Sort!" fades in.
5. **Loading bar:** a glass tube bar fills while a marble rolls along its front.
6. **Exit:** the screen hands off to onboarding or home with a zoom-fade.

### 3.2 Onboarding (no skip button)
- **Pager:** a swipeable `HorizontalPager` with parallax. The illustration card slides slower than the page and scales down as it leaves.
- **Card:** each illustration sits in a glass card with a soft inner gradient and a rim.
- **Page 1 demo, redrawn to the real rule:**
  1. Only bottom-row blocks are open.
  2. A tap pops one, and its marbles stream out of the exit.
  3. The block above then wakes up.
- **Text:** a glossy gold title and white body text.
- **Progress:** marble dots that grow and colour in as you advance.
- **Button:** "Next", with the final "Let's Play!" getting a shine sweep and a bounce.

### 3.3 Home (level map)
- **World sky:** a sky per world that blends as you scroll, with world scenery drawn beside the road:

  | World | Scenery |
  |---|---|
  | Candy Lab | Lollipops, candy canes, gumdrops |
  | Ocean | Seaweed, coral, shells, bubbles |
  | Space | Ringed planets, moons, stars |
  | Sunset | Crystal rocks, palm silhouettes |
  | Forest | Pine trees, mushrooms, bushes |

- **Road:** a glass tube. Finished segments are filled with a golden track; locked segments are a frosted tube with dotted light.
- **Nodes:** 3D glossy discs with a depth edge, rim and gloss.

  | Node | Look |
  |---|---|
  | Done | Gold, with an arc of 3 stars |
  | Current | Pink and larger, with a double "ping" ring, a soft pulse and a bobbing avatar pin above |
  | Locked | Frosted lavender with a padlock |
  | Unlocked by an ad | Aqua |
  | Boss | Red with a crown |

- **World start:** a signpost ribbon reading "World 2 · Ocean".
- **Chests:** every 10 levels a chest sits on a pedestal. When it is ready to open, it wiggles and glows.
- **Top bar:**
  - Avatar button (opens Profile), stars pill, coins pill with +, settings gear.
  - A colour-matched fade so the road slides under it.
- **Side widgets:** glass bubbles that bob gently on staggered phases, with a label capsule and a pulsing badge.
- **Play button:** a big jelly button with a shine sweep, plus a "BOSS LEVEL" tag on boss levels.

### 3.4 Bottom dock
- **Dock:** a floating glass bar with rounded top corners, a rim light and a soft shadow.
- **Selected tab:** a glossy pink pill slides between tabs on a snappy spring instead of jumping.
- **Icons:** the selected icon lifts and scales; the others are slightly smaller and dimmer.
- **Play tab:** a gold marble button raised above the dock.

### 3.5 Shop
- **Free Coins hero card:** a pink-to-violet gradient card with rays behind a bobbing gift, and a timer or FREE button.
- **Boosters:** a 2-column grid of tiles. Each tile has the icon on a coloured halo, an owned badge, the name, a short text, a ×1 price button and a ×3 button tagged "-17%".
- **Mega Bundle:** a gold card with a "BEST VALUE" ribbon and the four icons fanned out.
- **Purchase feedback:** a springy "+1 Hammer" toast with the item icon, floating up.

### 3.6 Daily
- **Lucky Spin:**
  - Gold rim with chasing light bulbs.
  - Gradient segments showing coin and booster icons instead of plain words.
  - Glossy hub and a gem pointer that flicks as segments pass.
  - The spin button gets a shine when the spin is free.
- **Daily Challenge:** a violet card with a trophy, a "×2 COINS" badge and a Play button.
- **7-day rewards:**
  - Tiles with day labels.
  - Today's tile glows and pulses.
  - Claimed days get a green check stamp.
  - Day 7 is a large chest tile.

### 3.7 Collection
- **Tabs:** a segmented switch for Marbles / Themes with a sliding thumb.
- **Skin tiles:**
  - A spotlight behind the marble trio.
  - A rarity chip set by price: Common, Rare, Epic, Legendary.
  - The selected tile glows green with a check.
  - Locked tiles are dimmed with a lock badge.
- **Theme cards:** a mini playground preview, the theme name and a Use / Unlock button.

### 3.8 Profile
- **Player card:** a gradient header, a large avatar marble in a gold ring with a level badge, the name and a stars pill.
- **Stats:** six tiles, each with its own icon (flag, star, marbles, cup, flame, crown).
- **Achievements:**
  - A claimed / total counter.
  - Each row has a medal icon, a progress tube with "x / target" text, and a claim button that shines when ready.

### 3.9 Settings and Legal
- **Settings:** grouped candy cards with coloured icon tiles:
  - Sound & Feel toggles.
  - Legal rows with chevrons opening Terms and Privacy.
  - Progress reset.
  - A footer with the logo and version.
- **Legal:** the themed HTML shown inside a rounded glass frame, with the new header. The page CSS colours are aligned to the new palette.

### 3.10 Game screen (around the playfield)
- **HUD:** a round pause button, a glossy level badge ("SUPER HARD" crown chip on boss levels) and an animated coin pill.
- **Booster tray:** a glass tray holding four jelly booster buttons with count or + badges. The active hammer glows and pulses.
- **Combo banner:** a starburst behind gradient candy text, with a pop-in and tilt.
- **Popups**, all with animated exit:

  | Popup | Contents |
  |---|---|
  | Win | Rays, confetti, stars popping with sparkle bursts, coin count-up, glossy lucky-multiplier bar with a gem needle |
  | Fail | Jammed-belt illustration, Continue and Try Again buttons |
  | Pause | Toggles and the main buttons |
  | Buy booster | Item on a ray pedestal |
  | New feature | "NEW!" ribbon with rays behind the block preview |

### 3.11 Watch-ad dialog and test ad
These pick up the new panel, ribbon and buttons automatically.

---

## 4. Transitions

| From → To | Animation |
|---|---|
| Splash → anything | Fade with a gentle zoom-in (420 ms) |
| Home → Game | Home zooms past (scale up, fade); the game zooms in from 0.9 |
| Game → Home | The reverse |
| Game → Game (next level / retry) | Quick crossfade (the playfield's own intro does the rest) |
| Home → Settings → Legal | Push: slide in from the right, the old screen drifts left and fades |
| Back | Pop: the reverse direction, with the old screen on top |
| Tab → tab | Slide in the direction of the tab (left or right) with a fade |
| Popups | Spring pop-in, quick shrink-fade out, dim fades both ways |

---

## 5. Smoothness and performance rules

1. **Read animation values in the draw phase only.**
   - Use `graphicsLayer {}`, `drawBehind {}`, `drawWithCache {}` or `offset {}`.
   - Never read them in composition; that rebuilds the UI every frame.
   - This fixes the old `idleBounce` and the splash and onboarding clocks.
2. **Animate only what is visible and needed.**
   - On the map, only the current node and claimable chests animate. The other nodes are static.
3. **Static layers are cached.** Sky gradients, glows and skylines are built once in `drawWithCache`. Only a small particle layer redraws.
4. **Soft particles use one pre-baked bitmap,** drawn with alpha and scale. No per-frame shaders.
5. **No real-time blur.** Glass is faked with translucent fills and rim lights, which is cheap on budget GPUs.
6. **One offscreen layer at most per screen** (the fade edge on scrolling lists).
7. **Verification:** `dumpsys gfxinfo` on home scroll, tab switching and popups. Target: 90th-percentile frame under 16 ms and under 5% janky frames on the SM-A076B.

---

## 6. Implementation order

1. Theme tokens, `Backgrounds.kt`, the component rebuild and new icons.
2. App transitions and the `Nav` direction flag.
3. Splash and onboarding (pager, new page-1 demo).
4. Home map, top bar, side widgets and bottom dock.
5. Shop, Daily, Collection and Profile tabs.
6. Settings, Legal and the CSS colour alignment.
7. Game HUD, booster tray, combo banner and all game popups.
8. Build, install on the SM-A076B, screenshot every screen, profile frames and fix issues.
9. Update DEVELOPER_GUIDE.md with the new UI system.

## 7. QA checklist (results on the SM-A076B, 2026-11-06)

- [x] Every screen screenshotted before and after on the test device. Seen: splash, onboarding 1–4, home map in two worlds, Shop, Daily, Collection, Profile, Settings, Terms, reset, pause, new-feature, buy-booster, win (before and after Collect), watch-ad dialog and test ad.
- [x] No text clipped at 720 px width. Coins and stars pills were checked at 4 digits; a 5-digit width was worked out but not seen on the device.
- [x] Popups open and close with animation; back button works on the screens tried (pause, settings, legal, popups).
- [x] Tab switches slide in the direction of the tab; screen push and pop directions are correct.
- [x] Onboarding still has no skip button; swiping and Next both work.
- [x] Home map: the current level is shown on open, the world sky blends while scrolling, the chest marker shows.
- [x] Watch-ad unlock flow still works in debug (locked level 24 → test ad → reward → level opens).
- [x] `./gradlew :app:assembleRelease :app:testDebugUnitTest` passes (2/2 tests, all 5000 levels).
- [x] Frame stats: 0 missed frame deadlines at 120 Hz on home idle, home scroll, every tab switch, Shop scroll, popups and gameplay. Median GPU time: home 6 ms (10 ms before the sky layer was cached), tabs 6–7 ms, game 3 ms.
- [ ] Not seen on a device: the fail popup and the confetti after its physics retune.
