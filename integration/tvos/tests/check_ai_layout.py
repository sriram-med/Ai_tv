"""Drive remote focus/scroll and assert AI results never cross the search toolbar."""
import subprocess,time,re,xml.etree.ElementTree as ET
from pathlib import Path

def adb(*args):return subprocess.check_output(['adb',*args],text=True)
def tree():
    adb('shell','uiautomator','dump','/sdcard/layout.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/layout.xml'))
def bounds(node):return list(map(int,re.findall(r'\d+',node.get('bounds',''))))
def click(node):
    x1,y1,x2,y2=bounds(node);adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def screenshot(name):
    adb('shell','screencap','-p','/sdcard/layout.png')
    subprocess.run(['adb','pull','/sdcard/layout.png','tv-preview-shots/'+name+'.png'],check=True)
def check(name):
    root=tree();parents={child:parent for parent in root.iter() for child in parent}
    field=next(n for n in root.iter('node') if n.get('class')=='android.widget.EditText')
    bar=parents[field];bottom=bounds(bar)[3]
    viewports=[n for n in root.iter('node') if n.get('class')=='android.widget.ScrollView']
    assert viewports,'Missing recommendation viewport'
    viewport=viewports[0];top=bounds(viewport)[1]
    assert top>=bottom+2,(name,'Viewport overlaps toolbar',top,bottom)
    for node in viewport.iter('node'):
        label=node.get('text','')
        if label and bounds(node)[3]>bounds(node)[1]:
            assert bounds(node)[1]>=top-1,(name,'Result draws above viewport',label,bounds(node),top)
    Path('tv-preview-shots/'+name+'.xml').write_text(ET.tostring(root,encoding='unicode'))
    return root

def open_ai():
    for attempt in range(10):
        root=tree()
        if any(n.get('class')=='android.widget.EditText' for n in root.iter('node')):return
        buttons=[n for n in root.iter('node') if n.get('text','').strip().endswith('AI')]
        if buttons:click(buttons[0]);time.sleep(2);return
        time.sleep(1)
    raise AssertionError('Cannot open AI screen')

Path('tv-preview-shots').mkdir(exist_ok=True)
# Existing flow leaves the populated AI screen open. Focus genre row, then scroll
# enough to force off-screen headings through the viewport boundary (the reported bug).
for name,size,density,font in [('layout-1080',None,None,'1.0'),('layout-720-large-text','1280x720','213','1.3')]:
    if size:
        adb('shell','wm','size',size);adb('shell','wm','density',density)
        adb('shell','settings','put','system','font_scale',font)
        adb('shell','am','force-stop','com.techpp.glass')
        adb('shell','am','start','-W','-n','com.techpp.glass/com.techpp.glass.MainActivity')
        time.sleep(3);open_ai();time.sleep(3)
    root=check(name+'-initial')
    buttons=[n for n in root.iter('node') if n.get('text')=='MOVIES']
    if buttons:click(buttons[0]);time.sleep(2)
    # Native wheel scrolling and DPAD traversal both move the recommendation list.
    adb('shell','input','keyevent','20');adb('shell','input','keyevent','20')
    adb('shell','input','keyevent','22');adb('shell','input','keyevent','21')
    root=check(name+'-focused')
    vp=next(n for n in root.iter('node') if n.get('class')=='android.widget.ScrollView')
    x1,y1,x2,y2=bounds(vp)
    adb('shell','input','swipe',str((x1+x2)//2),str(y2-30),str((x1+x2)//2),str(y1+20),'350')
    time.sleep(1);check(name+'-scrolled');screenshot(name+'-scrolled')
    # Header actions stay in their fixed bounds when keyboard expands/collapses.
    root=tree();keyboard=next(n for n in root.iter('node') if n.get('text')=='KEYBOARD')
    click(keyboard);time.sleep(1);check(name+'-keyboard-open')
    click(next(n for n in tree().iter('node') if n.get('text')=='KEYBOARD'))
    time.sleep(1);check(name+'-keyboard-closed')
print('PASS: AI toolbar/result separation during remote focus, scrolling and keyboard toggles at 1080p and 720p with enlarged text')
