"""Build a key-free fallback catalogue from Wikidata P364 original-language claims."""
import concurrent.futures,json,time,urllib.request,urllib.parse,gzip
from pathlib import Path
LANGUAGES={'te':('Telugu','Q8097'),'hi':('Hindi','Q1568'),'ta':('Tamil','Q5885'),'ml':('Malayalam','Q36236'),'kn':('Kannada','Q33673'),'bn':('Bengali','Q9610'),'mr':('Marathi','Q1571'),'pa':('Punjabi','Q58635'),'en':('English','Q1860')}
def query(language,kind):
    entity=LANGUAGES[language][1];cls='Q5398426' if kind=='series' else 'Q11424'
    return 'SELECT ?film ?filmLabel ?filmDescription ?imdb ?date ?genreLabel WHERE { { SELECT DISTINCT ?film ?imdb ?links WHERE { ?film wdt:P31 wd:'+cls+'; wdt:P364 wd:'+entity+'; wdt:P345 ?imdb; wikibase:sitelinks ?links. } ORDER BY DESC(?links) LIMIT 40 } OPTIONAL { ?film wdt:P577 ?date. } OPTIONAL { ?film wdt:P136 ?genre. } SERVICE wikibase:label { bd:serviceParam wikibase:language "en". } }'
def fetch(key):
    language,kind=key
    url='https://query.wikidata.org/sparql?'+urllib.parse.urlencode({'query':query(language,kind),'format':'json'})
    for attempt in range(3):
        try:
            request=urllib.request.Request(url,headers={'User-Agent':'AI-TV Regional catalog/1.8.7 (https://github.com/sriram-med/Ai_tv)','Accept':'application/sparql-results+json'})
            data=json.load(urllib.request.urlopen(request,timeout=25));items={}
            for row in data['results']['bindings']:
                val=lambda k:row.get(k,{}).get('value','')
                imdb=val('imdb');name=val('filmLabel')
                if not imdb.startswith('tt') or not imdb[2:].isdigit() or not name or name.startswith('Q') and name[1:].isdigit():continue
                item=items.setdefault(imdb,{'id':imdb,'name':name,'type':kind,'description':val('filmDescription'),'genres':[],'poster':'https://images.metahub.space/poster/medium/'+imdb+'/img','background':'https://images.metahub.space/background/medium/'+imdb+'/img','releaseInfo':'','originalLanguage':language,'languageSource':'Wikidata P364','sourceEntity':val('film')})
                year=val('date')[:4]
                if year.isdigit() and (not item['releaseInfo'] or year<item['releaseInfo']):item['releaseInfo']=year
                genre=val('genreLabel').replace(' film','').replace(' television series','')
                if genre and genre not in item['genres']:item['genres'].append(genre)
            return language+'_'+kind,{'metas':list(items.values())}
        except Exception:
            if attempt==2:raise
            time.sleep(2+attempt*2)
if __name__=='__main__':
    keys=[(lang,kind) for lang in LANGUAGES for kind in ['movie','series']]
    result={}
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        for key,value in pool.map(fetch,keys):
            result[key]=value;print(key,len(value['metas']),flush=True)
    output={'source':'Wikidata P364 original language, CC0','languages':{k:v[0] for k,v in LANGUAGES.items()},'catalogs':result}
    Path('integration/tvos/assets/regional_catalogs.json.gz').write_bytes(gzip.compress(json.dumps(output,ensure_ascii=False,separators=(',',':')).encode(),mtime=0))
