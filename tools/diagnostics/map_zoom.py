"""Capture visible map gaps while zooming/panning. Run with the map already open."""
import argparse,subprocess,time,json,re,xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
import numpy as np
p=argparse.ArgumentParser();p.add_argument('--output',required=True);p.add_argument('--package',default='org.fundamentalos.weather.benchmark');a=p.parse_args();out=Path(a.output);out.mkdir(parents=True,exist_ok=True)
adb=str(Path.home()/'Library/Android/sdk/platform-tools/adb')
def run(*args):return subprocess.check_output([adb,*args])
def guard():
 focus=run('shell','dumpsys','window').decode()
 assert any(a.package+'/' in line for line in focus.splitlines() if 'mCurrentFocus=' in line), 'Phone foreground changed; stopping without input'
guard()
run('shell','uiautomator','dump','/sdcard/weather-map-check.xml')
root=ET.fromstring(run('shell','cat','/sdcard/weather-map-check.xml'))
labels=set()
for resource in Path('app/src/main/res').glob('values*/strings.xml'):
 for node in ET.parse(resource).getroot():
  if node.get('name')=='weather_map': labels.add(node.text)
assert any(n.get('text') in labels for n in root.iter('node')), 'Map screen is not open'
rows=[]
for i in range(6):
 guard()
 if i%2==0:run('shell','input','tap','400','1100');run('shell','input','tap','400','1100')
 else:run('shell','input','swipe','250','1150','650','1400','350')
 for frame in range(3):
  guard()
  path=out/f'{i}-{frame}.png';path.write_bytes(run('exec-out','screencap','-p'))
  im=np.asarray(Image.open(path).convert('RGB'))[1000:1850,80:620]
  gap=float(np.mean(np.max(im,axis=2)<40))
  rows.append({'step':i,'frame':frame,'darkGapFraction':gap})
 time.sleep(.4)
(out/'results.json').write_text(json.dumps(rows,indent=2));print('Worst dark gap:',max(r['darkGapFraction'] for r in rows))
assert max(r['darkGapFraction'] for r in rows)<.01,'Large near-black map gaps during zoom/pan'
