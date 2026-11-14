// Adapted from Recommenders TfidfRecommender, MIT license.
// Copyright (c) Recommenders contributors. Android adaptation (2026).
package com.techpp.glass;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Content-based recommendations, fully on-device. No account or API key. */
public final class ContentRecommender {
    public static final class Item {
        public final String id, text;
        public Item(String id, String text) { this.id=id; this.text=text == null ? "" : text; }
    }
    public static final class Scored {
        public final String id;
        public final double score;
        Scored(String id,double score) { this.id=id; this.score=score; }
    }
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
        "a about above across after afterwards again against all almost alone along already also although always am among amongst amoungst amount an and another any anyhow anyone anything anyway anywhere are around as at back be became because become becomes becoming been before beforehand behind being below beside besides between beyond bill both bottom but by call can cannot cant co con could couldnt cry de describe detail do done down due during each eg eight either eleven else elsewhere empty enough etc even ever every everyone everything everywhere except few fifteen fifty fill find fire first five for former formerly forty found four from front full further get give go had has hasnt have he hence her here hereafter hereby herein hereupon hers herself him himself his how however hundred i ie if in inc indeed interest into is it its itself keep last latter latterly least less ltd made many may me meanwhile might mill mine more moreover most mostly move much must my myself name namely neither never nevertheless next nine no nobody none noone nor not nothing now nowhere of off often on once one only onto or other others otherwise our ours ourselves out over own part per perhaps please put rather re same see seem seemed seeming seems serious several she should show side since sincere six sixty so some somehow someone something sometime sometimes somewhere still such system take ten than that the their them themselves then thence there thereafter thereby therefore therein thereupon these they thick thin third this those though three through throughout thru thus to together too top toward towards twelve twenty two un under until up upon us very via was we well were what whatever when whence whenever where whereafter whereas whereby wherein whereupon wherever whether which while whither who whoever whole whom whose why will with within without would yet you your yours yourself yourselves".split(" ")));
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]{2,}");
    private static Map<String,Double> terms(String text) {
        String clean=Normalizer.normalize(text,Normalizer.Form.NFC)
            .replaceAll("<.*?>", "").replace('\n',' ').replace('\r',' ').replace('\t',' ')
            .replace("Â\u00a0", "").replaceAll("[^\\p{Z}\\s\\u0085\\p{L}\\p{N}]+", "").toLowerCase(Locale.ROOT);
        List<String> tokens=new ArrayList<>();
        Matcher matcher=WORD.matcher(clean);
        while(matcher.find()) if(!STOP.contains(matcher.group())) tokens.add(matcher.group());
        Map<String,Double> counts=new HashMap<>();
        for(int width=1;width<=3;width++) for(int i=0;i+width<=tokens.size();i++) {
            String gram=String.join(" ",tokens.subList(i,i+width));
            counts.put(gram,counts.getOrDefault(gram,0d)+1d);
        }
        return counts;
    }
    public static List<Scored> rank(String query,List<Item> catalog) {
        List<Item> items=new ArrayList<>(); Set<String> seen=new HashSet<>();
        for(Item item:catalog) {
            if(item != null && item.id != null && !item.id.isEmpty() && seen.add(item.id)) items.add(item);
            if(items.size()==80)break;
        }
        List<Map<String,Double>> docs=new ArrayList<>();
        for(Item item:items)docs.add(terms(item.text));
        docs.add(terms(query == null ? "" : query));
        Map<String,Integer> frequencies=new HashMap<>();
        for(Map<String,Double> doc:docs) for(String term:doc.keySet())
            frequencies.put(term,frequencies.getOrDefault(term,0)+1);
        for(Map<String,Double> doc:docs) {
            double length=0;
            for(Map.Entry<String,Double> term:doc.entrySet()) {
                double weight=term.getValue()*(Math.log((1d+docs.size())/(1d+frequencies.get(term.getKey())))+1d);
                term.setValue(weight);length+=weight*weight;
            }
            double norm=Math.sqrt(length);
            if(norm>0) for(Map.Entry<String,Double> term:doc.entrySet())term.setValue(term.getValue()/norm);
        }
        Map<String,Double> prompt=docs.get(docs.size()-1);
        List<Scored> result=new ArrayList<>();
        for(int i=0;i<items.size();i++) {
            double score=0;
            for(Map.Entry<String,Double> term:prompt.entrySet())score+=term.getValue()*docs.get(i).getOrDefault(term.getKey(),0d);
            result.add(new Scored(items.get(i).id,score));
        }
        // Stable ties preserve the verified catalog order for cold-start / empty queries.
        result.sort((a,b)->Double.compare(b.score,a.score));
        return result;
    }
}
