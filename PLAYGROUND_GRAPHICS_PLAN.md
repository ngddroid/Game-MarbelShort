# Playground graphics plan — "Candy Glass Machine"

This plan covers the game screen's playfield: everything drawn by `render/GameRenderer.kt` and
`render/MachineArt.kt`. The Compose HUD on top of it (pause, level ribbon, coins, booster tray,
popups) was redesigned earlier and is covered in `UI_GRAPHICS_PLAN.md`. Gameplay, geometry
(`GameLayout`) and hit-testing do not change.

## 1. Analysis of the old playfield (SM-A076B screenshots, levels 1 / 8 / 15 / 31)

| Area | What was wrong |
|---|---|
| Background | A flat two-colour gradient with a few faint circles. It did not match the world skies used on the home map, so going from the menu into a level felt like going to a different app. |
| Board | A near-white sheet with a 6-unit coloured edge. There was no depth, bevel or inner shadow, so it looked like paper, and its corners were blobby. Empty cells were pale ghost rectangles. |
| Hopper / spout | A large empty white funnel with tiny triangle arrows. The spout was a pale rectangle, so the marble chute did not read as a pipe. |
| Blocks | Open blocks were good but flat on top: there was no bevel or glass highlight, and the marbles inside were small. Blocked blocks were washed-out pastel squares with dots, which read as "disabled" rather than "full but stuck". The mystery block was a plain grey tile. |
| Belt | A heavy, flat black-indigo stadium with punched holes. The "9 / 29" counter sat on a plain white bar that looked like a text field. Nothing showed the belt was a machine (no rollers, no housing). |
| Tray | A white panel cut off at the screen edge. Cups were dull: a grey band and no specular highlight. Every row was equally bright, so the active row (the one that takes marbles) did not stand out from the waiting queue. |
| Overall | The parts did not share one light source or material. The HUD is glossy candy glass, while the playfield was flat vector, so the two looked like different games. |

## 2. Design language for the playfield

The whole machine is made of the same **candy glass** as the HUD:

- **Light** comes from the top-left. Every raised part gets a soft drop shadow, a darker depth side,
  a gradient body, a white bevel line on the top edge, and a gloss streak.
- **Frames** (board rim, belt housing, tray cabinet) use the world colour `theme.wall`, with a
  depth side in `theme.wallDark`.
- **Glass** (board interior, tray interior, spout) is a frosted, tinted white. It has an inner
  shadow along its edges, so it looks recessed into the frame.
- **Dark parts** stay dark only where marbles must stand out (the belt track). Elsewhere the scene
  stays light and cheerful.
- **The sky** behind the machine is the same per-world palette as the home map: Candy Lab, Ocean,
  Space, Sunset, Forest. It has glows, soft bokeh and a candy-factory skyline.

## 3. Element specs

### 3.1 Backdrop (baked once)
- A three-stop vertical sky gradient from `GameTheme.sky`, matching `Skies.forWorld` in the UI.
- Two large radial glows: `glowA` at the top left and `glowB` at the right middle.
- A far and a near candy-factory skyline whose horizon sits just above the tray cabinet. Its
  shapes are domes, lollipop towers, chimneys and flat roofs, seeded so they never change between
  frames.
- 12 soft bokeh discs drawn as radial gradients (no blur), plus a bottom haze.
- **Animated:** 7 twinkling sparkles drawn each frame, about 7 small paths in total.

### 3.2 Board (baked once)
1. A blurred drop shadow, offset 16 units down.
2. A depth side: the rim shape filled with `wallDark`, offset 9 units down.
3. A rim: the board outline grown by 10 units, with a vertical gradient from light wall to wall
   to slightly darker wall.
4. A rim bevel: a white stroke clipped to the rim, plus a darker stroke on the outer edge.
5. A glass interior: a gradient from `wallLight` at 80 % white down to 52 % white.
6. An inner shadow: the inner outline stroked 22 units wide with a blur, 7 units down, clipped to
   the interior. This makes the glass look sunk into the rim.
7. Two diagonal glass streaks across the interior.
8. A recessed well under every active cell, built from:
   - a tinted fill;
   - a top inner shadow band;
   - a white lip along the bottom edge.
9. A hopper that darkens toward the spout (a radial gradient), with dotted guide lines from each
   bottom-row column into the spout. Under each column is a rounded white chevron that marks the
   exit.
10. A spout that looks like a glass pipe: a horizontal cylinder gradient with a chrome collar at
    the top and one where it plugs into the belt housing.

### 3.3 Blocks (sprites, baked per colour × state)
- **Open** (has a path out):
  - Body: a raised jelly cube with a contact shadow, a depth side (0.10 s) and a gradient face.
  - Light: a white top bevel and a dark outline.
  - Window: a recessed glass window with a top inner shadow. It holds 9 larger marbles
    (r = 0.105 s), each with a tiny contact shadow.
  - Gloss: a glass cover with a curved top-left gloss highlight and a sharp glint.
- **Blocked**:
  - Body: a lower cube (0.05 s depth) with a pastel face.
  - Window: the marbles stay visible but faded, under a frosted lid with a few frost specks. It
    reads as "full, but stuck" instead of "disabled".
- **Mystery**:
  - Body: a lavender candy cube with a soft diagonal candy stripe pattern.
  - Mark: a big gold "?" with a dark outline, plus two sparkles.
- **Rainbow / grey (paint)**: built the same way as the normal block, using rainbow or grey colours.

### 3.4 Belt
- **Housing** (baked): a candy-wall stadium with a depth side, gradient body and top gloss.
- **Track** (baked): a deep indigo, tinted toward the world colour. It has an inner shadow along the
  top edge, so it reads as a channel.
- **Sockets** (one cached sprite blitted per slot each frame): a recessed cup with a lit lower lip.
  Small chrome rivets sit between the sockets and move with the belt, so the conveyor visibly runs
  even when it is empty.
- **Rollers** (each frame): a candy hub wheel at each end of the island, rotated by
  `beltOffset / beltR`, so the machine looks like it drives the belt.
- **Island** (baked): a glossy white candy pill.
- **Gauge** (each frame): a recessed capsule between the rollers.
  - The fill grows with `beltCount / capacity`: green, then yellow from 55 %, then orange-red
    from 78 %.
  - The text on it is white with an outline, so it can be read over any fill.
  - Above 78 % the fill pulses, matching the danger vignette.

### 3.5 Tray
- **Cabinet** (baked): a candy-wall frame with a frosted glass interior, an inner shadow and a top
  gloss line. Its bottom fades into a soft haze at the screen edge, so the cut-off looks
  intentional.
- **Lanes** (baked): one recessed glass tube per column, with a shadow on the left and a highlight
  on the right.
- **Cups** (sprites): "jelly boxes", Option A of three mockups the user chose on 2026-11-21. They
  match the board blocks and the reference.
  - Body: a rounded candy box in the solid marble colour, with a darker depth side, a gradient
    face, a white bevel, a dark outline and a glint.
  - Holes: three recessed holes in a row, each with a dark gradient and a white lower lip.
    Marbles land centred in them (`GameLayout.dimpleDx = ±cupW·0.27`, `dimpleDy = cupH·0.42`).
  - Hidden cups: a purple box with a gold "?".
  - The earlier "queue fog" over the lower rows was removed: every row stays at full colour, as in
    the approved mockup.

## 4. Performance rules
- Everything static is baked into 3 bitmaps: backdrop, board and base. Every block, cup, socket and
  roller state is a cached sprite. Per frame the playfield adds only:
  - about 30 socket blits and 30 rivet dots;
  - 2 roller blits;
  - 1 gauge (3 draws);
  - 7 sparkles.
- No per-frame `Path.op`, blur or shader allocation. Every gradient used per frame is created once
  in `init`.
- Target on the SM-A076B (120 Hz): 0 missed frame deadlines in `dumpsys gfxinfo` during play, and
  a median GPU time at or below the old 3 ms plus 1 ms.

## 5. QA checklist (results on the SM-A076B, 2026-11-06)
- [x] Level 1 (tutorial): board, hopper, belt and tray render correctly, and the hand pointer still lines up with the block.
- [x] Level 31 (irregular board) and levels 60, 80 and 110: the rim follows staircase and notched outlines with true rounded corners. The first attempt used `CornerPathEffect` on a union of squares, which made flat, chamfered-looking corners. It was replaced by rounded rects with bridges.
- [x] Open, blocked, mystery, arrow, crate, cage, bomb and x2 blocks are all readable.
- [x] Marbles are clearly visible on the belt in Candy, Ocean, Space, Sunset and Forest.
- [x] The gauge fills green during play, and its text is readable over the fill. The yellow and red stages use the same code path, but I didn't reach them on the phone.
- [x] Intro animation: the board scales in and the base slides up as before.
- [x] gfxinfo during busy play (4 blocks popped at once, combo x5): 0 missed deadlines out of 1660 frames, GPU at 3 ms (50th percentile) and 7 ms (99th).
- Tuning after the first device pass:
  - The glass interiors were made more neutral (`wallLight` lightened 90 / 80 / 66 %).
  - The inner shadows now use the frame colour, which keeps the Space glass from turning grey.
  - Blocked blocks are a little more saturated, under a lighter frost.
  - The belt track takes only 15 % of the world tint.
