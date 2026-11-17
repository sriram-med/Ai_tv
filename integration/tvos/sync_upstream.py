#!/usr/bin/env python3
"""Resolve a stable upstream release; update the pinned source and monotonic APK version."""
import json,urllib.request,re,subprocess
from pathlib import Path
p=Path('integration/tvos/upstream.json');c=json.loads(p.read_text())
def get(path):
    request=urllib.request.Request('https://api.github.com/repos/'+c['repository']+'/'+path,headers={'User-Agent':'AI-TV-upstream-sync','Accept':'application/vnd.github+json'})
    with urllib.request.urlopen(request,timeout=30) as r:return json.load(r)
r=get('releases/latest');tag=r['tag_name']
assert not r['prerelease'] and not r['draft'] and re.fullmatch(r'v?\d+\.\d+\.\d+',tag),'Stable semver release required'
sha=get('commits/'+tag)['sha']
if c['sha']==sha:raise SystemExit(0)
version=tag.lstrip('v')
def parts(v):return tuple(map(int,v.split('.')))
if parts(version)<=parts(c['version']):
    a,b,d=parts(c['version']);version=f'{a}.{b}.{d+1}'
c.update(tag=tag,sha=sha,version=version,versionCode=c['versionCode']+1)
p.write_text(json.dumps(c,indent=2)+'\n')
print('Pinned upstream',tag,sha,'as AI TV',version)
