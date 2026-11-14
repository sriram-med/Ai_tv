"""Execute the generated Java navigation and version comparator without an Android device."""
from pathlib import Path
import subprocess
import tempfile

src = Path('upstream/src/com/techpp/glass')
scene = (src / 'SceneHome.java').read_text()
nav = scene.split('        // Top Bar navigation', 1)[1].split('            if (k == KeyEvent.KEYCODE_DPAD_DOWN)', 1)[0]
updater = (src / 'AppUpdater.java').read_text()
versions = updater.split('    public static String cleanVersion', 1)[1].split('    /**\n     * Queries GitHub', 1)[0]
java = '''public class Regression {
static class View {
 static final int VISIBLE=0;
 static View focused;
 int visibility=VISIBLE;
 int getVisibility(){return visibility;}
 void requestFocus(){focused=this;}
}
static class KeyEvent {static final int KEYCODE_DPAD_LEFT=21, KEYCODE_DPAD_RIGHT=22;}
static View whoBar=new View(),weatherBtn=new View(),aiSceneButton=new View(),moonBtn=new View();
static boolean move(View f,int k) { boolean down=true;
''' + nav + '''} return false; }
public static String cleanVersion''' + versions + '''
static void expect(View from,int key,View to){View.focused=null;move(from,key);if(View.focused!=to)throw new AssertionError("Unreachable top-bar control");}
public static void main(String[] args){
 expect(weatherBtn,22,aiSceneButton);expect(aiSceneButton,22,moonBtn);
 expect(moonBtn,21,aiSceneButton);expect(aiSceneButton,21,weatherBtn);
 weatherBtn.visibility=8;
 expect(whoBar,22,aiSceneButton);expect(aiSceneButton,21,whoBar);
 if(!isNewerVersion("1.8.0","ai-tv-v1.8.1"))throw new AssertionError("Update hidden");
 if(isNewerVersion("1.8.0","ai-tv-v1.8.0"))throw new AssertionError("Same version offered");
 if(isNewerVersion("1.8.1","ai-tv-v1.8.0"))throw new AssertionError("Downgrade offered");
 System.out.println("PASS: remote AI navigation, hidden weather, release version comparisons");
}}
'''
with tempfile.TemporaryDirectory() as temp:
    path = Path(temp) / 'Regression.java'
    path.write_text(java)
    subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', str(path)], check=True)
    subprocess.run(['java', '-cp', temp, 'Regression'], check=True)
