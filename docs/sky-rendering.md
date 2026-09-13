# Weather sky rendering

The home screen now uses a dedicated sky renderer independently of glass text settings. Android 13+ uses AGSL; older devices use Canvas with the same palette and upper-cloud fade. Existing card/material logic remains separate.

## Softer clouds

Replaced the noisy generated density mask with large connected cloud masses and smooth interiors. Linear sampling removes block artifacts; directional lighting is clamped to avoid harsh bright rims and dark holes. Clouds fade out at roughly 43% of screen height, leaving a quiet lower background. At 627 × 627, grayscale high-frequency RMS (image minus Gaussian blur radius 2) fell from 7.83 to 1.01. This measures fine texture energy, not perceived naturalness.

Seven user-provided Apple Weather references are stored in benchmarks/apple-references for review only; they are not packaged as app assets. Pixel 9 Pro captures and comparisons are in benchmarks/sky-reference-captures. Captures call the actual production renderer with fixed weather and time. Modal references are cropped below the black status area. The comparison script measures RGB error only in lower background side gutters: this is palette agreement, **not overall similarity**. Cloud outlines, animation phase, sun optics and UI still differ. Storm phase zero has no bolt; a brief bolt appears at 14 seconds in a 19-second cycle.

## Performance behavior

Standard quality renders the sky into an offscreen layer at one third width/height, with 3 cloud layers and up to 64 full-resolution precipitation particles. Economy uses one quarter width/height, 2 layers and 24 particles. The texture is decoded once at 627 × 627 (~1.5 MiB ARGB). Mirrored linear sampling avoids discontinuities; cloud evaluation is skipped outside its visible band. There is no per-frame bitmap generation.

Motion uses a persistent elapsed-time clock and fixed texture seeds, updates at approximately 30 Hz, and stops when covered, backgrounded, disabled, in power save or when system animator scale is zero. Power save/low RAM select economy. Pausing preserves phase. Weather changes interpolate over 3.5 seconds. Solar position uses live wall time refreshed each minute, rather than the last observation time. Canvas fallback uses simpler lighting and does not fully reproduce the AGSL cloud threshold or optics.

## Asset provenance

Runtime asset: app/src/main/res/drawable-nodpi/sky_cloud_density.png. Generated using the built-in image generation tool; the tool does not expose its model version. The supplied partly cloudy screenshot was a style/scale reference, not copied into the runtime asset.

Generation brief: Create a new standalone grayscale cloud density texture with four to six large, smooth, contiguous cumulus masses. Extremely smooth interiors; no fine noise, grain, mottling or fractal cauliflower detail. No features smaller than about 40 pixels at 1024 resolution. Density mask, not a photograph. Black softly fading edges; no sky color, sun, text or UI.

The output has smoother interiors but does not perfectly satisfy the requested black edges. MIRROR sampling is used; the asset is not claimed to be seamless. Original generated files remain in the tool output directory.

## Validation

Build with ./gradlew assembleDebug assembleBenchmark. Run the four SkyStateTest unit tests with testDebugUnitTest --tests 'org.fundamentalos.weather.ui.sky.*'. Check translations with python3 tools/check_i18n.py.

The benchmark is a separate .benchmark package with an offline activity. It does not request location or modify stored production weather data. It is release-derived and non-debuggable, debug signed, with R8 disabled. The original fluid shader is preserved in the benchmark source set. Runs reverse A/B ordering each repetition. No system battery or refresh-rate settings are changed.

Use tools/benchmark/run_sky.py with --scenes clear partly_cloudy storm night --workloads bare --repeats 3 --duration 5000 --warmup 1500, and a second run with --scenes storm --workloads scroll frozen covered. Visual capture uses --capture --phase 0; fixed-phase captures must never count as animation performance samples.

See [results](benchmarks/sky-performance-summary.md), raw JSON, and [Android FrameMetrics timing definitions](https://developer.android.com/reference/android/view/FrameMetrics). Results describe this renderer harness on one device, not production startup, network behavior or battery endurance.

The seven final palette gutter errors range from 1.61 to 5.61 intensity levels per RGB channel (0–255 scale); clear daytime is 2.45. These samples exclude cloud shapes and foreground UI. The large cumulus texture is smoother than the reference’s detailed cloud outlines, and the sun remains a simplified optical approximation; the result is not pixel-identical to Apple Weather.

Four clock/interpolation/wind unit tests pass. Debug and benchmark APKs build, and all six locale resource/placeholder checks pass. Existing unrelated full lint failures and backend-dependent tests are documented by the earlier i18n work and are not claimed as passing here.

The first scroll fixture omitted the weather card theme and was superseded. Those rows were moved to benchmarks/sky-scroll-superseded and excluded from the final summary. Corrected scroll samples are in benchmarks/sky-scroll-final. Bare-background, covered and frozen measurements use the same unchanged renderer. Final glass captures use HomeScreen's weather card colors and light/dark text materials. The obsolete HomeScreen background tint overlay was removed so its sky colors match the calibrated renderer captures.

## Solar appearance correction (2026-09-12)

The user found the daytime sky too blue and the solar disc artificial. Comparing only lower side gutters had missed the reference's broad neutral/warm scattering around the sun. The clear palette now shifts toward less saturated blue; solar rendering blends a neutral aerial aureole, warm bloom and a larger soft white core with restrained diffraction lobes. Lens ghosts vary in radius and are less regular. The Canvas fallback also receives a white core and neutral halo. Solar work is skipped when its visibility is negligible.

New reference/previous/current comparisons are in benchmarks/sky-sun-captures. tools/benchmark/compare_sun.py measures a UI-free patch around the sun separately from background gutters. Its brightness threshold and RGB errors describe the patch, not physical realism or whole-screen similarity. The prior seven-scene comparison and benchmark summary document the preceding cloud revision. Updated clear-sky timings are in benchmarks/sky-sun-performance and sky-sun-performance-summary.md.

## Spectral halo and lens flare (visual work; performance deferred)

The user explicitly paused performance work to prioritize the missing rainbow halo and lens glow. The AGSL renderer now composites a faint, broad red/green/blue halo and five unequal optical reflections over the cloud layer. A defocused violet spot and lateral streak sit near the sun; blue/cyan discs and a weak rim follow the optical axis toward the lower-right image center. Clear and thin-cloud halo radii are separately calibrated to the supplied references (roughly 0.27 and 0.22 of image height), rather than claiming a physical relationship between cloud cover and halo radius. The Canvas fallback has a softer approximation of the colored halo and optical discs.

Use tools/capture_sky.py for device visual checks. Its capture_only activity path writes a screenshot and completion marker, then stops: it never enters the performance collection phase. The interrupted sky-sun-performance directory is not valid evidence for the final optical revision. Updated visuals are in benchmarks/sky-sun-captures; no current performance conclusion is claimed.

## Off-axis halo and six-lobe correction

The AGSL halo is now displaced along the optical axis, mildly stretched, and warped with bounded low-frequency angular terms. Angular brightness varies, so it is neither a centered circle nor a uniform rainbow band. The sun uses six aperture-like lobes with longer, soft white tips; the additional ten-lobe modulation was removed.

The existing solar elevation calculation now also supplies continuous sunrise-to-sunset progress using latitude, longitude, declination and local solar hour angle. UTC normalization makes the result invariant to the device timezone for the same instant and coordinates. Optical orientation derives from the projected sun/optical-axis relationship; coordinates do not seed arbitrary noise. This is our reference-guided model, not a verified description of Apple's private implementation. Three unit tests cover timezone invariance, longitude response and finite polar behavior. Performance remains deferred as requested.

## Warm, brighter core and independent ray reach

The latest visual adjustment keeps the core footprint close to the supplied sunny reference while warming the aureole/bloom and lifting the core toward warm white. Six narrower rays now use a separate reach envelope with an exponential falloff and a soft cutoff; their extent no longer comes from enlarging the disc. Device screenshots, rather than performance measurements, are used for this iteration.

## Source occlusion: no detached sun rays

Previously, per-pixel cloud compositing could hide the core while leaving ray tips outside the cloud visible. Solar opacity now samples the actual source center and four nearby points using the exact same cloud coordinates/opacity function and quality layers as the visible cloud renderer. A source visibility gate attenuates the core; a stricter direct-exposure gate removes ray bloom, angular core extensions and lens reflections before the core becomes unreadable. A screen-edge gate also prevents rays surviving when the source center leaves the viewport. The gate changes continuously with cloud motion; it introduces no separate random animation.

Visual-only device checks: clear sky, a dense cloud crossing the source (partly_cloudy phase 90), and the source visible again (phase 1350). Screenshots are under benchmarks/sky-occlusion. These checks do not collect performance metrics. Foreground app UI is composited separately; this fix concerns atmospheric/source and viewport occlusion, not arbitrary UI geometry.

The follow-up visual check also found that coarse total cloud-cover dimming could leave a pale core with comparatively bright spokes even when the source itself was unobstructed. Patchy cloud cover no longer globally dims the sun; source-local transmittance controls that case. Ray bloom starts outside the inner disc, preventing a spoke cross from showing through a partially faded core.

## Smaller core / more consistent halo visibility

The user requested a smaller sun and less frequently invisible rainbow. Core radii were reduced by roughly 10% without shortening the rays. The halo's minimum angular intensity and overall strength were lifted while retaining its asymmetric shape. Halo visibility now follows the source visibility; the stricter direct-exposure gate is reserved for rays and lens ghosts. A fully blocked source still suppresses all of them, while thin-cloud attenuation no longer unnecessarily erases the entire halo.

## Unified solar emission

Core and six diffraction lobes now form one smooth opacity union, composited in one pass with a shared warm-white radial color gradient. Ray roots broaden near the core and narrow only toward their ends; a low-intensity broad lobe also joins the aureole. This replaces separately colored ray/core overlays and removes the deliberate inner ray gap. Source occlusion remains shared, and ray/lens gating still suppresses detached corners when the source is covered.

Visual verification of unified emission: rebuilt debug and capture APKs successfully,
installed the debug app on the connected Pixel 9 Pro, and captured clear,
mostly-clear, cloud-covered (phase 90), and cloud-opening (phase 1350) scenes.
`benchmarks/sky-sun-captures/fused-sun-comparison.jpg` juxtaposes the Apple reference
with device captures. The ray roots join the bright core continuously; the covered
sample has neither a visible core nor detached rays. These are screenshot-only
runs (`captureOnly: true`); no performance measurement was performed.

Solar scattering refinement: added a circular inner scattering field and a broader,
fainter outer skirt, combined as a smooth opacity union. This increases illumination
between the six diffraction lobes and softens their transition into the surrounding
sky. The core radii, ray lengths, spectral halo and source-occlusion gates are
unchanged. This is an artistic approximation calibrated against the supplied Apple
reference, not a physical atmospheric simulation.
Verified with successful debug/capture builds and screenshot-only runs on Pixel 9
Pro (clear, mostly clear, cloud-covered phase 90). The comparison at
`benchmarks/sky-sun-captures/scattering-comparison.jpg` shows broader illumination
between the rays; the covered sample still suppresses the source and its rays.
Installed and reopened the debug app. Performance measurements remain paused.

Irregular solar scattering: the neutral aureole and warm scattering now share a
smooth angular radius deformation (one-, three-, and five-lobe low-frequency
components). It follows the screen optical axis without time modulation or texture
noise. Deformation fades out near the source, preserving the centered white core;
its bounded amplitude retains continuous soft falloff. The six diffraction rays,
spectral halo, and occlusion gates are unchanged. This is visual art direction,
not a claim about Apple's implementation or geographic atmospheric optics.

Cloud bank contours (2026-09-13): removed the broad screen-height opacity fade.
AGSL now terminates each layer through its sampled density contour; dense lobes
extend below thinner regions and the sky is untouched below the bank. The visible
cloud and solar-occlusion paths use the same contour. The pre-AGSL fallback bakes a
cloud-shaped alpha contour once and no longer paints a sky gradient over the cloud.

Cloud motion visibility: real home screenshots confirmed that the animation was
advancing, but the drift was too subtle (only about 1.7% of sampled upper-cloud
pixels changed by more than two RGB levels over the observed interval). Increased
advection by 4× while retaining separate layer rates; a gentle minimum flow keeps
high clouds moving even in surface calm. Source occlusion samples the same moving
field. Compatibility rendering also increases horizontal drift and adds slow,
bounded vertical movement. No time-varying noise is added. Navigation still pauses
home sky animation from push start until returning home.

Final visual validation: captured partly-cloudy scenes at 0 s and 8 s, plus cloudy
and mostly-clear scenes under `benchmarks/sky-cloud-contours`. An actual 8-second
home-screen recording is `cloud-motion.mp4` (not time-accelerated). Its upper cloud
region changes over 6.6 s: mean RGB difference 0.82 and 13.1% of pixels changing
by more than two levels. This confirms live animation, separately from the fixed
phase captures. These are visual checks, not a sky performance benchmark.
