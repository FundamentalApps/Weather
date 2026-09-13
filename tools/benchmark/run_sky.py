#!/usr/bin/env python3
"""Paired release-like offline sky benchmark on one attached Android 13+ device."""
import argparse,json,os,subprocess,time,fcntl,tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
PACKAGE='org.fundamentalos.weather.benchmark'
ACTIVITY='org.fundamentalos.weather.benchmark.SkyBenchmarkActivity'

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--adb',default=os.environ.get('ADB',str(Path.home()/'Library/Android/sdk/platform-tools/adb')))
    p.add_argument('--output',type=Path,default=ROOT/'docs/benchmarks/sky-results')
    p.add_argument('--repeats',type=int,default=3)
    p.add_argument('--duration',type=int,default=8000)
    p.add_argument('--warmup',type=int,default=3000)
    p.add_argument('--scenes',nargs='+',default=['clear','overcast','rain','night'])
    p.add_argument('--workloads',nargs='+',default=['bare','scroll'])
    p.add_argument('--renderers',nargs='+',default=['legacy','sky'])
    p.add_argument('--capture',action='store_true')
    p.add_argument('--phase',type=float,default=None)
    p.add_argument('--skip-install',action='store_true')
    args=p.parse_args();args.output.mkdir(parents=True,exist_ok=True)
    if args.phase is not None and not args.capture:
        p.error('--phase is only for visual captures, not animation benchmarks')
    # A second runner must never force-stop a device while another sample is collecting.
    lock=open(Path(tempfile.gettempdir())/'weather-sky-benchmark.lock','w')
    try: fcntl.flock(lock,fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError: raise SystemExit('Another sky benchmark runner is active.')
    def adb(*cmd,check=True):
        return subprocess.run([args.adb,*cmd],capture_output=True,text=True,check=check).stdout.strip()
    devices=[line for line in adb('devices').splitlines()[1:] if line.endswith('\tdevice')]
    if len(devices)!=1:raise SystemExit('Attach exactly one authorized Android device.')
    if int(adb('shell','getprop','ro.build.version.sdk'))<33:raise SystemExit('Paired AGSL benchmark requires Android 13+.')
    if not args.skip_install:
        print(adb('install','-r',str(ROOT/'app/build/outputs/apk/benchmark/app-benchmark.apk')),flush=True)
    metadata={k:adb('shell',*v) for k,v in {
        'model':['getprop','ro.product.model'],'build':['getprop','ro.build.fingerprint'],
        'size':['wm','size'],'density':['wm','density'],'battery':['dumpsys','battery'],
        'thermal':['dumpsys','thermalservice']}.items()}
    metadata.update(vars(args)|{'output':str(args.output)})
    (args.output/'environment.json').write_text(json.dumps(metadata,indent=2))
    try:
        for repeat in range(args.repeats):
            for scene in args.scenes:
                for workload in args.workloads:
                    # Reverse AB ordering on alternating repetitions to reduce warm-cache/thermal bias.
                    renderers=args.renderers if repeat%2==0 else list(reversed(args.renderers))
                    for renderer in renderers:
                        run_id=f'{renderer}-{scene}-{workload}-{repeat}'
                        remote=f'/sdcard/Android/data/{PACKAGE}/files/{run_id}.json'
                        adb('shell','am','force-stop',PACKAGE)
                        adb('shell','rm','-f',remote,check=False)
                        adb('shell','am','start','-W','-n',f'{PACKAGE}/{ACTIVITY}',
                            '--es','renderer',renderer,'--es','scene',scene,'--es','workload',workload,
                            '--es','run_id',run_id,'--ei','duration',str(args.duration),'--ei','warmup',str(args.warmup),
                            '--ez','capture',str(args.capture and repeat==0).lower(),
                            *(['--ef','capture_time',str(args.phase)] if args.phase is not None else []))
                        deadline=time.monotonic()+(args.duration+args.warmup)/1000+40
                        while time.monotonic()<deadline:
                            if adb('shell','ls',remote,check=False)==remote:break
                            time.sleep(1)
                        else:
                            crash=adb('logcat','-d','-b','crash','-t','80',check=False)
                            (args.output/f'{run_id}-crash.txt').write_text(crash)
                            raise RuntimeError(f'{run_id} timed out; see crash log')
                        adb('pull',remote,str(args.output/f'{run_id}.json'))
                        if args.capture and repeat==0:
                            adb('pull',remote.removesuffix('.json')+'.png',str(args.output/f'{run_id}.png'),check=False)
                        data=json.loads((args.output/f'{run_id}.json').read_text())
                        print(f"{run_id}: {data['frames']} frames, p95={data.get('totalP95Ms')} ms, GPU p95={data.get('gpuP95Ms')} ms, misses={data['missedDeadlines']}, cpu={data['processCpuMs']} ms, thermal={data['thermalStart']}/{data['thermalEnd']}",flush=True)
    finally:
        adb('shell','am','force-stop',PACKAGE,check=False)
    print('Results:',args.output,flush=True)

if __name__=='__main__':main()
