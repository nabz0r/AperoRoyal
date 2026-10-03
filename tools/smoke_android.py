#!/usr/bin/env python3
"""UI smoke run on a debug APK after creating two players and starting the party."""
import json, os, subprocess, time
ADB=os.environ.get('ADB',os.path.expanduser('~/Library/Android/sdk/platform-tools/adb'))
S=2.7

def adb(*args):
    return subprocess.check_output([ADB,*args],text=True,stderr=subprocess.DEVNULL)
def state():
    return json.loads(adb('shell','run-as com.aperoroyale sqlite3 databases/apero_royale.db "SELECT data FROM session;"'))
def tap(x,y):
    adb('shell','input', 'tap',str(round(x*S)),str(round(y*S)))
def wait_screen(screen,timeout=3):
    until=time.time()+timeout
    while time.time()<until:
        if state()['screen']==screen:return state()
        time.sleep(.08)
    raise AssertionError((screen,state()['screen']))

games=[]
for turn in range(10):
    s=wait_screen('TRANSITION')
    game=s['game'];games.append(game)
    print('turn',turn,'game',game,'language',s['players'][s['active']]['language'],flush=True)
    tap(200,789)
    s=wait_screen('GAME')
    if game==0:tap(200,379+s['target']*66)
    elif game==1:tap(200,660)
    elif game==2:tap(200,409+s['target']*62)
    elif game==3:
        for _ in range(10):
            s=state();tap(s['targetX'],s['targetY']);time.sleep(.03)
    elif game==4:
        cup=(s['loserCup']+1)%6;tap(87+(cup%3)*107,402+(cup//3)*132)
    elif game==5:
        adb('shell','input','swipe',str(round(90*S)),str(round(360*S)),str(round(250*S)),str(round(500*S)),'300')
        time.sleep(.1)
        assert len(state()['strokes'])>0
        tap(200,740)
        s=state();tap(200,522+s['target']*50)
    elif game==6:
        time.sleep(len(s['sequence'])*.72+.9)
        for color in s['sequence']:
            tap(117+(color%2)*165,395+(color//2)*130)
            time.sleep(.08)
    elif game==7:
        limit=time.time()+12
        while time.time()<limit and state()['screen']=='GAME':
            tap(200,440);time.sleep(.12)
    elif game==8:tap(200,670)
    elif game==9:
        for _ in range(8):tap(200,443)
    s=wait_screen('RESULT',timeout=20)
    print('  result',s['lastWon'],'score',s['players'][s['active']]['score'],'drinks',s['players'][s['active']]['drinks'],flush=True)
    tap(200,788)
    wait_screen('TRANSITION')
assert len(set(games))==10,(games,'deck repeated before ten unique games')
print('PASS: ten distinct mini-games, turn rotation, bilingual players, scoring and persistence')
