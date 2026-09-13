"""Exercise real home-to-screen transitions and flag frames exceeding 50 ms."""
import argparse, json, re, subprocess, time
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--output',required=True);p.add_argument('--runs',type=int,default=3);p.add_argument('--package',default='org.fundamentalos.weather');a=p.parse_args()
adb=str(Path.home()/'Library/Android/sdk/platform-tools/adb');pkg=a.package
def run(*args):return subprocess.check_output([adb,*args],text=True)
def guard():
 focus=run('shell','dumpsys','window')
 assert any(pkg+'/' in line for line in focus.splitlines() if 'mCurrentFocus=' in line), 'Phone foreground changed; stopping without input'

run('shell','input','keyevent','KEYCODE_WAKEUP');run('shell','wm','dismiss-keyguard')
run('shell','am','force-stop',pkg);run('shell','am','start','-W','-n',pkg+'/org.fundamentalos.weather.MainActivity');time.sleep(2)
results=[]
for label,x in [('map',90),('locations',870)]:
 for attempt in range(a.runs):
  guard()
  run('shell','dumpsys','gfxinfo',pkg,'reset')
  run('shell','input','tap',str(x),'1998');time.sleep(1.2)
  guard()
  raw=run('shell','dumpsys','gfxinfo',pkg,'framestats')
  durations=[];header=None
  for line in raw.splitlines():
   if line.startswith('Flags,'):header=line.split(',')
   elif header and re.match(r'^\d+,',line):
    row=dict(zip(header,line.split(',')))
    if row.get('Flags')=='0':
     ns=int(row['FrameCompleted'])-int(row['IntendedVsync'])
     if 0<ns<5e9:durations.append(ns/1e6)
  results.append({'screen':label,'run':attempt,'frames':len(durations),'maxMs':max(durations,default=0),'over50ms':sum(v>50 for v in durations),'durationsMs':durations})
  guard()
  run('shell','input','keyevent','4');time.sleep(1)
Path(a.output).write_text(json.dumps(results,indent=2))
for r in results:print({k:v for k,v in r.items() if k!='durationsMs'})
assert all(r['frames'] and r['over50ms']==0 for r in results),'Navigation contains frames over 50 ms'
