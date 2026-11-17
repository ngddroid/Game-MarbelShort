#!/bin/zsh
# usage: lvl.sh <level> <outname> — unlock level in the debug build, open it, screenshot
ADB=~/Library/Android/sdk/platform-tools/adb; P=com.puzzel.lab.marbelshort; S=${0:h}
$ADB shell am force-stop $P
$ADB shell run-as $P cat shared_prefs/marble_pop.xml > $S/prefs_in.xml
python3 - "$1" "$S/prefs_in.xml" "$S/prefs.xml" <<'PY'
import sys,re
lvl,src,dst=sys.argv[1:4]; s=open(src).read()
def put(s,tag,name,val):
    pat=r'<%s name="%s" value="[^"]*" />'%(tag,name); new='<%s name="%s" value="%s" />'%(tag,name,val)
    return re.sub(pat,new,s) if re.search(pat,s) else s.replace('</map>','    '+new+'\n</map>')
s=put(s,'int','level',lvl); s=put(s,'int','skin','0'); s=put(s,'int','theme','-1'); s=put(s,'boolean','onboarding','true')
open(dst,'w').write(s)
PY
$ADB push $S/prefs.xml /data/local/tmp/prefs.xml >/dev/null
$ADB shell "run-as $P sh -c 'cat /data/local/tmp/prefs.xml > shared_prefs/marble_pop.xml'"
$ADB shell am start -n $P/.MainActivity >/dev/null
sleep 6
$ADB shell input tap 360 1334
sleep 2.4
$ADB exec-out screencap -p > $S/$2.png
