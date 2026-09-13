#!/usr/bin/env python3
"""Compare UI-free solar patches, preserving the user references and assets."""
from pathlib import Path
import json
import numpy as np
from PIL import Image,ImageDraw
root=Path('docs/benchmarks');scene='clear';size=(480,1000)
ref=Image.open(root/'apple-references/clear.jpg').convert('RGB');ref=ref.crop((0,182,ref.width,ref.height)).resize(size)
old=Image.open(root/'sky-reference-captures/sky-clear-bare-0.png').convert('RGB').resize(size)
new=Image.open(root/'sky-sun-captures/sky-clear-bare-0.png').convert('RGB').resize(size)
box=(int(.16*size[0]),int(.015*size[1]),int(.50*size[0]),int(.14*size[1]))
refPatch=np.asarray(ref.crop(box),dtype=float);rows={}
for label,im in [('reference',ref),('previous',old),('current',new)]:
 a=np.asarray(im.crop(box),dtype=float)
 rows[label]={'solarPatchRgbMae':float(np.abs(a-refPatch).mean()),'brightCoreFraction':float(np.mean(np.min(a,axis=2)>220))}
canvas=Image.new('RGB',(size[0]*3,size[1]+35),'#161c23');draw=ImageDraw.Draw(canvas)
for i,(label,im) in enumerate([('Apple reference',ref),('Previous',old),('Current',new)]):
 canvas.paste(im,(i*size[0],35));draw.text((i*size[0]+12,10),label,fill='white')
canvas.save(root/'sky-sun-captures/sun-before-after.jpg',quality=95)
(root/'sky-sun-captures/solar-metrics.json').write_text(json.dumps({'method':'Crop modal top 182 pixels; normalize to 480x1000; solar patch x=.16:.50,y=.015:.14 avoids UI. RGB MAE is not overall realism; core threshold requires all channels >220.', 'metrics':rows},indent=2))
print(json.dumps(rows,indent=2))
