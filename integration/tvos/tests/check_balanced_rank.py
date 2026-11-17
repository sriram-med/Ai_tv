"""Execute the actual generated rank bridge with a movie-heavy input ordering."""
from pathlib import Path
import tempfile,subprocess
source=Path('upstream/src/com/techpp/glass/SearchPanel.java').read_text()
start=source.index('    private List<Search.Title> rankTitles(')
end=source.index('    void aiPick(',start)
method=source[start:end].replace('private List<Search.Title> rankTitles','static List<Search.Title> rankTitles')
with tempfile.TemporaryDirectory() as temp:
    root=Path(temp);src=root/'com/techpp/glass';src.mkdir(parents=True)
    (src/'ContentRecommender.java').write_text(Path('app/src/main/java/com/techpp/glass/ContentRecommender.java').read_text())
    (src/'Search.java').write_text('package com.techpp.glass; class Search {static class Title {String title="",type="",genres="",overview="";}}')
    (src/'RankingRegression.java').write_text('package com.techpp.glass; import java.util.*; public class RankingRegression {\n'+method+'''public static void main(String[] args) {
        List<Search.Title> titles=new ArrayList<>();
        for(String kind:new String[]{"movie","tv"})for(int i=0;i<80;i++){Search.Title t=new Search.Title();t.type=kind;t.title=kind+i;titles.add(t);}
        List<Search.Title> ranked=rankTitles("",titles);
        long series=ranked.stream().filter(t->t.type.equals("tv")).count();
        if(ranked.size()!=80||series!=40)throw new AssertionError("Ranking excluded a content type: "+series);
        if(rankTitles("",titles.subList(80,160)).size()!=80)throw new AssertionError("Series-only recommendations truncated");
        System.out.println("PASS: actual rank bridge balances 40 movies + 40 series and retains series-only pools");
    }}''')
    files=[str(p) for p in src.glob('*.java')]
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-source','11','-target','11','-d',temp,*files],check=True)
    subprocess.run(['java','-cp',temp,'com.techpp.glass.RankingRegression'],check=True)
