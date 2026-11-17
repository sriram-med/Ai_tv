"""Exercise the persistent mood selector without crowding the poster viewport."""
import subprocess,time,re,xml.etree.ElementTree as ET
from pathlib import Path
def adb(*args):return subprocess.check_output(['adb',*args],text=True)
def tree():
    adb('shell','uiautomator','dump','/sdcard/mood.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/mood.xml'))
def click(label):
    node=next(n for n in tree().iter('node') if n.get('text','').casefold()==label.casefold())
    x,y,r,b=map(int,re.findall(r'\d+',node.get('bounds','')))
    adb('shell','input','tap',str((x+r)//2),str((y+b)//2))
click('MOOD');time.sleep(.5);click('Dark & tense')
for _ in range(15):
    time.sleep(1);root=tree();texts=' '.join(n.get('text','') for n in root.iter('node'))
    if 'DARK & TENSE' in texts and re.search(r'[1-9]\d* recommendations',texts):break
else:raise AssertionError('Mood failed to produce recommendations: '+texts)
Path('tv-preview-shots/mood-dark.xml').write_text(ET.tostring(root,encoding='unicode'))
click('DARK & TENSE');time.sleep(.5);click('All moods');time.sleep(1)
assert any(n.get('text')=='MOOD' for n in tree().iter('node')),'Clear mood failed'
print('PASS: mood selection, actual recommendations and clearing mood')
