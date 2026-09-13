# Map and navigation investigation — 2026-09-13

## Reproduction

`tools/diagnostics/navigation_frames.py` drives three real home→map and three
home→locations transitions on the connected Pixel 9 Pro. It captures Android
FrameStats and fails on frames exceeding 50 ms; raw durations are preserved.
Debug runs are retained separately and must not be compared to non-debug builds.
`navigation-release-before.json` measures the previously installed non-debug app;
`navigation-release-after.json` measures the updated build with the same workload.
The harness includes first composition, layout, animation and tile arrival during
its 1.2-second sampling window, not just steady-state GPU time.

## Findings and changes

- A traced locations push spent 55.7 ms in Compose movable-content insertion,
  54.6 ms composing LocationsScreen, and 65 ms in traversal in the debug build.
  The Navigation overlay scene moved already-composed content between scenes.
  The root now holds home and pushed screens as stable keyed siblings. Saved
  destinations, slide direction, parallax, predictive back and nested settings
  navigation are retained. The animation starts after initial layout, and home
  sky animation pauses as soon as a destination is pushed. Progress changes update
  layer properties rather than invalidating the underlying draw contents.
- The map waited for a separate average-reference request, then made average
  temperature transparent. This request and anomaly alpha are removed. Only
  temperature is loaded, always on; the radar/switch card is gone.
- Missing base tiles were transparent over a near-black Compose surface. A captured
  frame had 96.2% near-black pixels in the inspected map region. Missing base tiles
  now show map paper; cached parent tiles bridge zoom-in requests without disk or
  network access on the UI thread. Tile paints use bitmap filtering. Cold, uncached
  regions still require a network request; no offline map coverage is fabricated.
- The old approximation constructor mistook thread-count/queue-size arguments for
  zoom bounds. The provider now uses osmdroid's defaults.
- Temperature smoothing now decodes palette bands into scalar temperatures,
  applies a separable Gaussian, then interpolates the legend. WMS tiles request
  a 16-pixel gutter on all sides at unchanged geographic pixel spacing, filter it,
  then crop. Missing data remains transparent. Cache keys include revision and
  observation time. Removed overlays detach their providers.

## Regression checks

The average-reference regression test first failed on device, then passed after
removing the dependency. Three native tests verify full visibility for a uniform
valid temperature, intermediate legend colours rather than RGB colour mixing,
and exact equality between adjacent guttered tiles and a single continuous field.
Run them through `adb shell am instrument` using the installed debug test APK.
`tools/diagnostics/map_zoom.py` saves screenshots and checks a map-only region for
large near-black gaps while zooming and panning. It does not prove complete offline
coverage or the absence of all possible network delays.

Sky screenshot captures are visual checks only; sky performance testing remains
paused. Navigation measurements above were requested separately by the user.

Additional fixes after the first comparison: saved places now use LazyColumn so
initial composition is limited to visible rows. Map preferences/SQLite/archive
provider construction runs on Dispatchers.IO, with cancellation cleanup; the
Android MapView itself is still created and manipulated on the UI thread. The
final non-debug run is `navigation-release-final.json` (the earlier `after` file
is an intermediate build, not the final result).

Final navigation sample (three pushes per destination, non-debug Pixel 9 Pro):
frames over 50 ms fell from 35 to 2 across the six windows. Map runs peaked at
41.4 / 73.6 / 36.6 ms; locations at 63.8 / 45.0 / 38.8 ms. The strict zero-slow-frame
assertion still fails: these measurements support a substantial reduction in
stutter, not a claim of universally jank-free navigation. The zoom/pan screenshot
check passed (largest near-black fraction 0.0096%, below its 1% threshold).

Visual QA also checked location-list → settings → back, entering location search,
and dismissing the keyboard. The temperature overlay now registers the map's
invalidation handler so newly downloaded temperature tiles appear without another
pan/touch. The live DWD endpoint accepted the guttered WMS PNG request using fresh
observation metadata; a stale previous-day timestamp correctly returned a service
error rather than data (not treated as a successful image).

Final QA correction: the last attempted zoom run was not on the map (the captured
screen was home and the phone subsequently changed foreground apps). Its files
are quarantined in `map-zoom-invalid-foreground`, and its apparent pass is invalid.
The earlier successful zoom/pan observation remains evidence for the fix, but the
latest 16-pixel smoothing revision has not received a complete final UI zoom pass.
`map-temperature-preview.png` shows the earlier 8-pixel smoothing revision on the
real map. Three native temperature tests passed again with the 16-pixel gutter.
Automated touches stopped when another foreground app was observed. No final
predictive-back gesture result is claimed from that interrupted check.
