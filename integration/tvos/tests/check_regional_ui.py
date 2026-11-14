"""Verify language selection, persistence, nonempty regional cards and Nuvio entry."""
import subprocess,time,re,json,gzip,xml.etree.ElementTree as ET
from pathlib import Path

def adb(*args):return subprocess.check_output(['adb',*args],text=True)
def tree():
    adb('shell','uiautomator','dump','/sdcard/regional.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/regional.xml'))
def click(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds','')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def text_click(label):click(next(n for n in tree().iter('node') if n.get('text')==label))
def choose(button,language):text_click(button);time.sleep(.5);text_click(language)
def await_region(name):
    for attempt in range(18):
        time.sleep(1);root=tree();texts=' '.join(n.get('text','') for n in root.iter('node'))
        if name+' catalogue' in texts and re.search(r'[1-9]\d* recommendations',texts):
            assert any(re.search(r'\s+·\s+(19|20)\d{2}',n.get('text','')) for n in root.iter('node')),'No regional poster labels'
            return root
    raise AssertionError('Regional catalogue failed: '+texts)

# Clear prior search text, so first regional browse is a genuine empty-query load.
root=tree();field=next(n for n in root.iter('node') if n.get('class')=='android.widget.EditText');click(field)
adb('shell','input','keyevent','123')
for _ in field.get('text',''):adb('shell','input','keyevent','67')
# Hide any system keyboard without closing the recommendation panel.
adb('shell','input','keyevent','4');time.sleep(.5)
root=tree()
if not any(n.get('text')=='REGION' for n in root.iter('node')):
    # Back can close the overlay on hardware-keyboard emulators; reopen it.
    ai=next(n for n in root.iter('node') if n.get('text','').strip().endswith('AI'));click(ai);time.sleep(1)
choose('REGION','Telugu');root=await_region('Telugu')
Path('tv-preview-shots/regional-telugu.xml').write_text(ET.tostring(root,encoding='unicode'))
text_click('SERIES');root=await_region('Telugu')
Path('tv-preview-shots/regional-telugu-series.xml').write_text(ET.tostring(root,encoding='unicode'))
choose('TELUGU','Hindi');root=await_region('Hindi')
Path('tv-preview-shots/regional-hindi.xml').write_text(ET.tostring(root,encoding='unicode'))
# The language setting survives Activity recreation / process restart.
adb('shell','am','force-stop','com.techpp.glass')
adb('shell','am','start','-W','-n','com.techpp.glass/com.techpp.glass.MainActivity');time.sleep(2)
root=tree();click(next(n for n in root.iter('node') if n.get('text','').strip().endswith('AI')));time.sleep(2)
assert any(n.get('text')=='HINDI' for n in tree().iter('node')),'Language preference was lost'
# Missing companion has an explicit official-install route, never a silent no-op.
text_click('NUVIO');time.sleep(.5)
assert 'Nuvio TV is not installed' in ' '.join(n.get('text','') for n in tree().iter('node'))
text_click('Close')
choose('HINDI','All languages');time.sleep(2)
print('PASS: Telugu movies/series, Hindi catalogue, persistent language preference and Nuvio companion entry')
