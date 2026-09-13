#!/usr/bin/env python3
import argparse,json,statistics
from pathlib import Path
from collections import defaultdict
p=argparse.ArgumentParser()
p.add_argument('directories',type=Path,nargs='+')
p.add_argument('--output',type=Path,default=Path('docs/benchmarks/sky-performance-summary.md'))
args=p.parse_args();groups=defaultdict(list)
for directory in args.directories:
 env=json.loads((directory/'environment.json').read_text())
 if env.get('phase') is not None:raise SystemExit('Fixed phase is not an animation benchmark')
 for file in directory.glob('*.json'):
  row=json.loads(file.read_text())
  if 'rawFrames' in row:groups[(row['workload'],row['scene'],row['renderer'])].append(row)
lines=['# Device benchmark results','','Pixel 9 Pro, 960 × 2142, API 37. Non-debuggable, R8 disabled. Offline fixture with production renderer and real glass/card components. Median of repeated runs; misses and frame counts pooled. CPU is whole-process CPU, not power.','','| Workload | Scene | Renderer | Runs | FPS | Frame p95 ms | GPU p95 ms | CPU ms/s | PSS MiB | Miss / frames |','|---|---|---|---:|---:|---:|---:|---:|---:|---:|']
def med(rows,k):
 nums=[r[k] for r in rows if isinstance(r.get(k),(int,float))]
 return f'{statistics.median(nums):.2f}' if nums else '—'
for (workload,scene,renderer),rows in sorted(groups.items()):
 for r in rows:
  r['cpuRate']=r['processCpuMs']*1000/r['measurementMs'];r['pssMiB']=r['pssKb']/1024
 lines.append(f"| {workload} | {scene} | {renderer} | {len(rows)} | {med(rows,'renderedFps')} | {med(rows,'totalP95Ms')} | {med(rows,'gpuP95Ms')} | {med(rows,'cpuRate')} | {med(rows,'pssMiB')} | {sum(r['missedDeadlines'] for r in rows)} / {sum(r['deadlineFrames'] for r in rows)} |")
rows=[r for v in groups.values() for r in v]
lines+=['',f"All non-debuggable: {all(not r['debuggable'] for r in rows)}. Thermal statuses: {sorted(set(r[k] for r in rows for k in ['thermalStart','thermalEnd']))}. Frame callback drops: {sum(r['callbackDrops'] for r in rows)}.",'','Frame p95 = FrameMetrics TOTAL_DURATION; GPU p95 = GPU_DURATION. Miss = TOTAL_DURATION ≥ DEADLINE for valid deadlines. Frozen/covered percentiles are unavailable when no frames are drawn. These short runs do not measure battery drain, startup, network work or thermal endurance. Raw JSON includes individual frames and environment metadata.']
args.output.write_text('\n'.join(lines)+'\n');print('\n'.join(lines))
