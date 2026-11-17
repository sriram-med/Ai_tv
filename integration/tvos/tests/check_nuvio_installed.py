"""Recognize a launcher-visible fork with a nonofficial application ID."""
import subprocess,time,re,xml.etree.ElementTree as ET
from pathlib import Path
def adb(*args):return subprocess.check_output(['adb',*args],text=True)
def tree():
    adb('shell','uiautomator','dump','/sdcard/nuvio.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/nuvio.xml'))
def click(node):
    x,y,r,b=map(int,re.findall(r'\d+',node.get('bounds','')))
    adb('shell','input','tap',str((x+r)//2),str((y+b)//2))
adb('install','-r','nuvio-probe.apk')
adb('shell','am','force-stop','com.techpp.glass')
adb('shell','am','start','-W','-n','com.techpp.glass/com.techpp.glass.MainActivity');time.sleep(2)
click(next(n for n in tree().iter('node') if n.get('text','').strip().endswith('AI')));time.sleep(2)
click(next(n for n in tree().iter('node') if n.get('text')=='NUVIO'));time.sleep(1)
root=tree()
assert any('NUVIO PROBE RECEIVED' in n.get('text','') for n in root.iter('node')),'Installed Nuvio fork not recognized'
Path('tv-preview-shots/nuvio-installed.xml').write_text(ET.tostring(root,encoding='unicode'))
adb('uninstall','org.example.nuviolegacy')
adb('shell','am','start','-W','-n','com.techpp.glass/com.techpp.glass.MainActivity')
print('PASS: installed Nuvio-labelled fork discovered and launched without official package ID')
