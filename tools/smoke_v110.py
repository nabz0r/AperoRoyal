#!/usr/bin/env python3
"""Exercise ten games, two local players and spectator predictions.

Usage: ADB_SERIAL=emulator-5554 python3 tools/smoke_v110.py
The target emulator uses a 1080-pixel-wide immersive display and `run-as`.
"""
import json
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ADB = os.environ.get("ADB", os.path.expanduser("~/Library/Android/sdk/platform-tools/adb"))
SERIAL = os.environ.get("ADB_SERIAL", "emulator-5554")
PACKAGE = "com.aperoroyale"
SCALE = 2.7
HEIGHT = 2340 / SCALE


def adb(*args):
    return subprocess.check_output([ADB, "-s", SERIAL, *args], text=True).strip()


def state():
    for _ in range(10):
        try:
            return json.loads(adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db "SELECT data FROM session;"'))
        except (subprocess.CalledProcessError, json.JSONDecodeError):
            time.sleep(.08)
    raise AssertionError("session database stayed locked")


def tap(x, y):
    adb("shell", "input", "tap", str(round(x * SCALE)), str(round(y * SCALE)))


def wait_screen(wanted, timeout=5):
    until = time.time() + timeout
    while time.time() < until:
        current = state()
        if current["screen"] == wanted:
            return current
        time.sleep(.08)
    raise AssertionError(f"expected {wanted}, got {state()['screen']}")


def dialog_node(resource_id):
    adb("shell", "uiautomator", "dump", "/sdcard/apero-window.xml")
    xml = adb("shell", "cat", "/sdcard/apero-window.xml")
    root = ET.fromstring(xml)
    for node in root.iter("node"):
        if node.get("resource-id") == resource_id:
            return node
    raise AssertionError(f"dialog node {resource_id} missing")


def first_edit():
    adb("shell", "uiautomator", "dump", "/sdcard/apero-window.xml")
    root = ET.fromstring(adb("shell", "cat", "/sdcard/apero-window.xml"))
    for node in root.iter("node"):
        if node.get("class") == "android.widget.EditText":
            return node
    raise AssertionError("nickname field missing")


def tap_node(node):
    left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))


def add_player(name):
    tap(200, HEIGHT - 160)
    field = first_edit()
    tap_node(field)
    adb("shell", "input", "text", name)
    adb("shell", "input", "keyevent", "4")
    tap_node(dialog_node("android:id/button1"))
    wait_screen("LOBBY")
    time.sleep(.35)


def play(game, s):
    if game == 0:
        tap(200, 379 + s["target"] * 66)
    elif game == 1:
        tap(200, HEIGHT - 145)
        wait_screen("GAME")
        tap(200, 585)
    elif game == 2:
        tap(200, 409 + s["target"] * 62)
    elif game == 3:
        for _ in range(12):
            s = state()
            if s["screen"] != "GAME":
                break
            tap(s["targetX"], s["targetY"])
    elif game == 4:
        cup = (s["loserCup"] + s["wager"]) % 6
        tap(87 + (cup % 3) * 107, 402 + (cup // 3) * 132)
    elif game == 5:
        adb("shell", "input", "swipe", str(round(90 * SCALE)), str(round(360 * SCALE)),
            str(round(250 * SCALE)), str(round(500 * SCALE)), "300")
        assert state()["strokes"], "drawing was not recorded"
        tap(200, HEIGHT - 126)
        s = state()
        assert s["drawingReady"], "drawing handoff failed"
        tap(200, 522 + s["target"] * 50)
    elif game == 6:
        time.sleep(len(s["sequence"]) * .72 + .9)
        for color in s["sequence"]:
            tap(117 + (color % 2) * 165, 395 + (color // 2) * 130)
            time.sleep(.07)
    elif game == 7:
        until = time.time() + 15
        while time.time() < until and state()["screen"] == "GAME":
            tap(200, 440)
            time.sleep(.08)
    elif game == 8:
        tap(200, HEIGHT - 140)
        wait_screen("GAME")
        tap(200, 585)
    elif game == 9:
        for _ in range(8):
            tap(200, 443)


def main():
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1.5)
    tap(200, HEIGHT - 282)
    wait_screen("LOBBY")
    add_player("Pixel")
    add_player("Nova")
    assert len(state()["players"]) == 2
    tap(200, HEIGHT - 227)
    assert state()["mode"] == "FREE"
    tap(200, HEIGHT - 96)
    wait_screen("LIBRARY")
    shots = Path("docs/screenshots/games")
    shots.mkdir(parents=True, exist_ok=True)
    slugs = ["trivia", "poses", "blind-test", "reflex", "roulette", "drawing",
             "memory", "rhythm", "bluff", "bomb"]
    for game in range(10):
        if state().get("ruleOwner") and state().get("ruleId", -1) < 0:
            tap(200, 450)
            assert state()["ruleId"] >= 0, "secret room rule was not applied"
        tap(24 + (game % 2) * 180 + 86, 186 + (game // 2) * 78 + 33)
        s = wait_screen("TRANSITION")
        assert s["game"] == game
        tap(200, HEIGHT - 78)
        wait_screen("BET")
        tap(200, 492)
        wait_screen("PREDICT")
        tap(200, 561)
        s = wait_screen("GAME")
        with (shots / (slugs[game] + ".png")).open("wb") as f:
            f.write(subprocess.check_output([ADB, "-s", SERIAL, "exec-out", "screencap", "-p"]))
        play(game, s)
        s = wait_screen("RESULT", timeout=28)
        print(f"{game + 1:02d} {s['players'][s['active']]['name']} {s['lastWon']} "
              f"score={s['players'][s['active']]['score']} sips={s['players'][s['active']]['sips']}", flush=True)
        tap(200, HEIGHT - 78)
        wait_screen("LIBRARY")
    assert state()["turn"] == 10
    stats = adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db "SELECT COUNT(*) FROM history;"')
    assert int(stats) >= 10, stats
    predictions = adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db "SELECT COUNT(*) FROM history WHERE role=\'PREDICTION\';"')
    assert int(predictions) >= 10, predictions
    print("PASS: ten mini-games, local turn rotation, wagers, predictions, history and screenshots")


if __name__ == "__main__":
    main()
