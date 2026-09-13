# Device benchmark results

Pixel 9 Pro, 960 × 2142, API 37. Non-debuggable, R8 disabled. Offline fixture with production renderer and real glass/card components. Median of repeated runs; misses and frame counts pooled. CPU is whole-process CPU, not power.

| Workload | Scene | Renderer | Runs | FPS | Frame p95 ms | GPU p95 ms | CPU ms/s | PSS MiB | Miss / frames |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|
| bare | clear | legacy | 3 | 30.00 | 10.76 | 6.31 | 262.75 | 91.91 | 0 / 450 |
| bare | clear | sky | 3 | 30.00 | 8.43 | 3.56 | 262.00 | 99.91 | 0 / 451 |
| bare | night | legacy | 3 | 30.00 | 10.60 | 6.47 | 247.60 | 91.90 | 0 / 450 |
| bare | night | sky | 3 | 30.00 | 8.46 | 3.39 | 263.40 | 99.98 | 0 / 450 |
| bare | partly_cloudy | legacy | 3 | 30.00 | 10.83 | 6.47 | 250.60 | 91.72 | 0 / 451 |
| bare | partly_cloudy | sky | 3 | 30.00 | 8.50 | 3.30 | 261.70 | 100.03 | 0 / 451 |
| bare | storm | legacy | 3 | 29.99 | 10.84 | 6.28 | 250.00 | 91.78 | 0 / 449 |
| bare | storm | sky | 3 | 29.99 | 10.75 | 3.33 | 336.53 | 99.88 | 0 / 450 |
| covered | storm | legacy | 3 | 0.00 | — | — | 3.60 | 90.94 | 0 / 0 |
| covered | storm | sky | 3 | 0.00 | — | — | 3.80 | 99.55 | 0 / 0 |
| frozen | storm | legacy | 3 | 0.00 | — | — | 3.80 | 54.63 | 0 / 0 |
| frozen | storm | sky | 3 | 0.00 | — | — | 4.40 | 61.55 | 0 / 0 |
| scroll | storm | legacy | 3 | 120.00 | 11.04 | 4.18 | 1082.20 | 236.31 | 0 / 1800 |
| scroll | storm | sky | 3 | 119.98 | 13.17 | 7.26 | 1018.39 | 246.62 | 0 / 1801 |

All non-debuggable: True. Thermal statuses: [0]. Frame callback drops: 0.

Frame p95 = FrameMetrics TOTAL_DURATION; GPU p95 = GPU_DURATION. Miss = TOTAL_DURATION ≥ DEADLINE for valid deadlines. Frozen/covered percentiles are unavailable when no frames are drawn. These short runs do not measure battery drain, startup, network work or thermal endurance. Raw JSON includes individual frames and environment metadata.

Scroll variability (GPU p95 per run, ms):

- legacy: 3.01, 4.18, 7.72
- sky: 7.26, 2.74, 7.39

The corrected scroll sample has a higher median GPU/frame p95 for the new renderer; no universal speedup is claimed. Bare sky GPU p95 is consistently lower, with about 8 MiB extra PSS. Storm CPU usage increases because rain particles are now drawn. Scroll measurements show substantial device/pipeline variability and should be repeated on target low-end devices. Rare lightning is visually captured separately; these short animated timing samples do not cover its whole 19-second cycle.
