#!/usr/bin/env python3
"""Device screenshots only: never enters the activity's performance measurement phase."""
import argparse,json,subprocess,time
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument('--scenes',nargs='+',default=['clear','mostly_clear','partly_cloudy'])
p.add_argument('--renderer',default='sky',choices=['sky','compat','economy'])
p.add_argument('--output',type=Path,default=Path('docs/benchmarks/sky-sun-captures'))
p.add_argument('--install',action='store_true')
p.add_argument('--phase',type=float,default=0.0)
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
adb=str(Path.home()/'Library/Android/sdk/platform-tools/adb');package='org.fundamentalos.weather.benchmark'
def run(*cmd,check=True):return subprocess.run([adb,*cmd],check=check,capture_output=True,text=True,timeout=60).stdout.strip()
if a.install:print(run('install','-r','app/build/outputs/apk/standalone/benchmark/app-standalone-benchmark.apk'),flush=True)
try:
 for scene in a.scenes:
  ident=f'{a.renderer}-{scene}-bare-0';remote=f'/sdcard/Android/data/{package}/files/{ident}'
  run('shell','am','force-stop',package)
  run('shell','rm','-f',remote+'.json',remote+'.png')
  run('shell','am','start','-W','-n',package+'/'+package+'.SkyBenchmarkActivity',
      '--es','renderer',a.renderer,'--es','scene',scene,'--es','run_id',ident,
      '--es','workload','bare','--ei','warmup','750','--ez','capture','true',
      '--ez','capture_only','true','--ef','capture_time',str(a.phase))
  deadline=time.monotonic()+25
  while time.monotonic()<deadline:
   if run('shell','ls',remote+'.json',check=False)==remote+'.json':break
   time.sleep(.3)
  else:raise RuntimeError('Capture did not finish: '+scene)
  for suffix in ['.png','.json']:run('pull',remote+suffix,str(a.output/(ident+suffix)))
  assert json.loads((a.output/(ident+'.json')).read_text())['captureOnly'] is True
  print('Captured '+scene,flush=True)
finally:run('shell','am','force-stop',package,check=False)
