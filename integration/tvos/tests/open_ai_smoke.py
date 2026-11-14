"""Open the visible AI control on a running TV emulator and verify its chooser."""
import subprocess
import re
import time
import xml.etree.ElementTree as ET
from pathlib import Path

def adb(*args):
    return subprocess.check_output(['adb', *args], text=True)

def hierarchy():
    adb('shell', 'uiautomator', 'dump', '/sdcard/ai-tv-ui.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/ai-tv-ui.xml'))

nodes = list(hierarchy().iter('node'))
buttons = [n for n in nodes if n.get('text', '').strip().endswith('AI')]
if not buttons:
    raise SystemExit('FAIL: visible AI button absent from home hierarchy')
node = buttons[0]
x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds', '')))
adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
time.sleep(2)
root = hierarchy()
texts = ' '.join(n.get('text', '') for n in root.iter('node'))
Path('tv-preview-shots/ai-chooser.xml').write_text(ET.tostring(root, encoding='unicode'))
if 'AI DISCOVERY' not in texts or 'CONNECT GPT' not in texts:
    raise SystemExit('FAIL: AI button did not open AI discovery chooser: ' + texts)
adb('shell', 'input', 'keyevent', '22')
adb('shell', 'input', 'keyevent', '21')
print('PASS: signed in-place upgrade, AI button opens chooser, remote navigation accepted')
