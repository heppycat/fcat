# Design principles

## Identity

- **Name:** fcat. **Tagline:** *Read smoothly. Stay curious.*
- **Direction:** a clean, fast, understated utility with a subtle cat identity. Cute
  without looking childish, and professional enough for a serious open-source
  project.
- **Mark:** a calico cat face: white head, bold dark outline, one orange patch, on
  warm paper (`#FFF8EE`). The source is the adaptive icon
  `app/src/main/res/drawable/ic_launcher_foreground.xml`.
- **Colour:** calico orange (`#E8763A`) is the brand accent. On Android 12+ the app
  follows the user's dynamic colour, and the brand colours are the fallback.
- **Where the cat appears:** the icon, the empty states, the about screen. Never on
  the page canvas, and never in a way that slows a task down.

## Principles

1. **The page is the interface.** The document gets the screen. Chrome hides while
   reading and comes back with a single tap. No persistent toolbars over text.
2. **Fast is a feature.** Every interaction gets immediate feedback, even if that is
   a low-resolution page that sharpens. Nothing blocks the main thread. A spinner
   over the page is a bug report.
3. **Respect the reader's place.** Never jump, reset zoom, or lose position
   unexpectedly: not on rotation, not on resize, not after process death.
4. **Native first.** Follow Material 3 and Android conventions: system picker,
   predictive back, edge-to-edge, share sheet, adaptive layouts. Don't invent custom
   controls when a platform one exists.
5. **Calm UI.** No nags, no upsells, no badges, no onboarding carousel. Settings are
   few, and every one earns its place.
6. **Accessible by default.**
   - Every control has a content description.
   - Touch targets are at least 48 dp.
   - Text uses `sp` and respects font scale.
   - Contrast meets WCAG AA.
   - Full keyboard and TalkBack navigation of everything outside the page canvas.
   - Reader-mode alternatives for page content come with the text layer.
7. **Large screens and input devices are first-class.** Tablets, foldables,
   ChromeOS, keyboard shortcuts and mouse wheel are not afterthoughts.
8. **Honest states.**
   - Corrupt, encrypted or unsupported files get a clear message and a way out.
   - Search says when it is still running.
   - Nothing pretends to work when it doesn't.

## Interaction defaults (to be validated in M2 with users)

| Gesture | Action |
|---|---|
| Single tap | Toggle chrome |
| Double tap | Zoom to fit the tapped column, or back to fit width |
| Pinch | Zoom, anchored at the focal point |
| Fling | Continuous scroll with platform physics |
| Back | Close search or selection, then pop the link back stack, then leave the document |
