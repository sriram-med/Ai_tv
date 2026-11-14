"""Compare the Android adaptation against the actual cloned Recommenders model."""
import ast
import base64
from pathlib import Path
import subprocess
import tempfile
import re
import unicodedata
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import linear_kernel

original=Path('vendor/recommenders/tfidf_utils.py')
# Only load the upstream class: BERT and NLTK tokenizers are not used in mode none.
module=ast.parse(original.read_text())
class_node=next(n for n in module.body if isinstance(n,ast.ClassDef) and n.name=='TfidfRecommender')
namespace=dict(TfidfVectorizer=TfidfVectorizer,linear_kernel=linear_kernel,re=re,
               unicodedata=unicodedata,np=np,pd=pd)
exec(compile(ast.Module(body=[class_node],type_ignores=[]),str(original),'exec'),namespace)
Model=namespace['TfidfRecommender']

harness='''import java.util.*;
import java.nio.charset.StandardCharsets;
import com.techpp.glass.ContentRecommender;
public class RecommenderParity {
static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
public static void main(String[] args){
 List<ContentRecommender.Item> items=new ArrayList<>();
 for(int i=1;i<args.length;i++)items.add(new ContentRecommender.Item("item"+i,decode(args[i])));
 for(ContentRecommender.Scored item:ContentRecommender.rank(decode(args[0]),items))
   System.out.println(item.id+"\\t"+item.score);
}}
'''
texts=['Alien A science fiction space horror survival movie',
       'Family picnic A warm comedy about friendship and children',
       'Deep space Aliens in deep space science fiction',
       '<b>Romance</b> love and family in Paris',
       'తెలుగు సినిమా science-fiction adventure']
queries=['space science fiction','family comedy','romance Paris','తెలుగు','unknown words','the and']
def encode(s):return base64.b64encode(s.encode()).decode()
with tempfile.TemporaryDirectory() as tmp:
    entry=Path(tmp)/'RecommenderParity.java';entry.write_text(harness)
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,
        'app/src/main/java/com/techpp/glass/ContentRecommender.java',str(entry)],check=True)
    for query in queries:
        df=pd.DataFrame([{'id':f'item{i+1}','text':s} for i,s in enumerate(texts)]+
                        [{'id':'query','text':query}])
        model=Model(id_col='id',tokenization_method='none')
        clean=model.clean_dataframe(df,['text'])
        tf,vectors=model.tokenize_text(clean)
        model.fit(tf,vectors)
        table=model.recommend_top_k_items(clean,k=len(texts))
        expected={r['rec_id']:r['rec_score'] for _,r in table[table.id=='query'].iterrows()}
        output=subprocess.check_output(['java','-cp',tmp,'RecommenderParity',encode(query),
                *map(encode,texts)],text=True)
        actual=dict((line.split('\t')[0],float(line.split('\t')[1])) for line in output.splitlines())
        assert actual.keys()==expected.keys(),(query,actual,expected)
        for key,score in actual.items():
            assert abs(score-expected[key])<1e-10,(query,key,score,expected[key])
print('PASS: native TF-IDF scores match cloned Recommenders across six queries, including Unicode and zero-overlap queries')
