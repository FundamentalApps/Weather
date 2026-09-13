#!/usr/bin/env python3
"""Compare device background captures to user references; never score unrelated UI."""
from pathlib import Path
import argparse,json
import numpy as np
from PIL import Image,ImageDraw,ImageFilter

def main():
 p=argparse.ArgumentParser()
 p.add_argument('--captures',type=Path,default=Path('docs/benchmarks/sky-reference-captures'))
 p.add_argument('--references',type=Path,default=Path('docs/benchmarks/apple-references'))
 p.add_argument('--scenes',nargs='+',default=['clear','mostly_clear','partly_cloudy','cloudy','night','night_cloudy','storm'])
 args=p.parse_args()
 scenes=args.scenes
 report=[]; panels=[]
 for scene in scenes:
  ref=Image.open(args.references/f'{scene}.jpg').convert('RGB')
  if not scene.startswith('night'): ref=ref.crop((0,182,ref.width,ref.height))
  actual=Image.open(args.captures/f'sky-{scene}-bare-0.png').convert('RGB')
  ref=ref.resize((320,700),Image.Resampling.LANCZOS)
  actual=actual.resize(ref.size,Image.Resampling.LANCZOS)
  a=np.asarray(actual,dtype=float);r=np.asarray(ref,dtype=float)
  # The outer side gutters avoid forecast cards. Ignore modal rounded corners,
  # buttons/status, central labels, and cloud/solar structures in this palette score.
  mask=np.zeros((700,320),bool)
  mask[315:610,7:13]=True; mask[315:610,307:313]=True
  error=np.abs(a-r)[mask]
  rows=[]
  for y in [0.45,0.6,0.8]:
   band=mask.copy();band[:int(y*700)]=False;band[int((y+.025)*700):]=False
   rows.append({'y':y,'referenceRgb':np.median(r[band],axis=0).round(1).tolist(),
                'deviceRgb':np.median(a[band],axis=0).round(1).tolist()})
  report.append({'scene':scene,'backgroundGutterMeanAbsoluteRgbError':round(float(error.mean()),2),
                 'samples':rows})
  panel=Image.new('RGB',(640,735),'#151922');panel.paste(ref,(0,35));panel.paste(actual,(320,35))
  ImageDraw.Draw(panel).text((10,10),f'{scene}: Apple reference | Pixel 9 Pro',fill='white')
  panel.save(args.captures/f'compare-{scene}.jpg',quality=93);panels.append(panel)
 columns=min(4,len(panels))
 sheet=Image.new('RGB',(640*columns,735*((len(panels)+columns-1)//columns)),'#151922')
 for i,panel in enumerate(panels):sheet.paste(panel,(i%columns*640,i//columns*735))
 sheet.save(args.captures/'comparison.jpg',quality=94)
 (args.captures/'comparison.json').write_text(json.dumps({
  'method':'Modal references cropped at y=182px; both resampled to 320x700. RGB MAE only on lower side gutters x=7:13/307:313 y=315:610. This is palette error, NOT overall similarity. Cloud shapes assessed visually; reference animation phase unavailable.',
  'scenes':report},indent=2))
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()
