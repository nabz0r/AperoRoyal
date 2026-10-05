#!/usr/bin/env python3
"""Exercise ten games, explicit two-player handoffs and the wager flow.

Usage: ADB_SERIAL=emulator-5554 python3 tools/smoke_v120.py
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
            return json.loads(subprocess.check_output([ADB, "-s", SERIAL, "shell",
                f'run-as {PACKAGE} sqlite3 databases/apero_royale.db "SELECT data FROM session;"'],
                text=True, stderr=subprocess.DEVNULL).strip())
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
    tap(105, HEIGHT - 177)
    time.sleep(.25)
    field = first_edit()
    tap_node(field)
    adb("shell", "input", "text", name)
    adb("shell", "input", "keyevent", "4")
    tap_node(dialog_node("android:id/button1"))
    wait_screen("LOBBY")
    time.sleep(.45)


def start_from_bet(game, shots):
    tap(200, 492)
    if state()["screen"] == "CREW":
        tap(200, HEIGHT - 91)
        time.sleep(.12)
        with (shots / f"crew-{game}.png").open("wb") as f:
            f.write(subprocess.check_output([ADB, "-s", SERIAL,
                "exec-out", "screencap", "-p"]))
        s = state()
        choice = s["target"] if game in (0, 2) else 0
        if game == 0: tap(200, 392 + choice * 65)
        elif game == 2: tap(200, 427 + choice * 65)
        elif game == 3: tap(110, 410)
        elif game == 4: tap(83, 420)
        elif game == 6: tap(117, 422)
        elif game == 7: tap(66, 442)
    return wait_screen("GAME")


def play(game, s):
    if game == 0:
        tap(200, 379 + s["target"] * 66)
    elif game == 1:
        tap(200, HEIGHT - 145)
        wait_screen("GAME")
        tap(200, HEIGHT - 91)
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
        tap(200, HEIGHT - 91)
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
        tap(110, HEIGHT - 140)
        wait_screen("GAME")
        assert state()["bluffTruth"] == 1, "the actor's secret truth was not locked"
        tap(200, HEIGHT - 91)
        tap(200, 665)
    elif game == 9:
        goal = max(8, (1 + s["variant"] % 3) * len(s["players"]))
        while state()["screen"] == "GAME" and state()["taps"] < goal:
            current = state()
            if current["bombAwaitingPass"]:
                if current["bombVisitedMask"] == (1 << len(current["players"])) - 1:
                    shots = Path("docs/screenshots/games")
                    with (shots / "bomb-choice.png").open("wb") as f:
                        f.write(subprocess.check_output([ADB, "-s", SERIAL,
                            "exec-out", "screencap", "-p"]))
                    tap(110, 581)  # Choose a wire once everybody has held the bomb.
                    assert state()["screen"] == "RESULT", "wire cut did not end the round"
                    break
                tap(110, 332)  # Only one eligible friend with two local players.
                assert not state()["bombAwaitingPass"]
                tap(200, HEIGHT - 91)  # Private handoff to the chosen holder.
            else:
                tap(200, 443)


def main():
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1.5)
    tap(200, HEIGHT - 329)
    wait_screen("LOBBY")
    add_player("Pixel")
    add_player("Nova")
    assert len(state()["players"]) == 2
    tap(200, HEIGHT - 248)
    assert state()["mode"] == "FREE"
    tap(292, HEIGHT - 178)
    wait_screen("LIBRARY")
    shots = Path("docs/screenshots/games")
    shots.mkdir(parents=True, exist_ok=True)
    slugs = ["trivia", "poses", "blind-test", "reflex", "roulette", "drawing",
             "memory", "rhythm", "bluff", "bomb"]
    for game in range(10):
        assert state()["active"] == game % 2, "turn owner did not alternate"
        if state().get("ruleOwner") and state().get("ruleId", -1) < 0:
            tap(200, 450)
            assert state()["ruleId"] >= 0, "secret room rule was not applied"
        tap(24 + (game % 2) * 180 + 86, 186 + (game // 2) * 78 + 33)
        s = wait_screen("TRANSITION")
        assert s["game"] == game
        tap(200, HEIGHT - 78)
        wait_screen("HANDOFF")
        assert state()["players"][state()["active"]]["name"] == ["Pixel", "Nova"][game % 2]
        tap(200, HEIGHT - 78)
        wait_screen("BET")
        s = start_from_bet(game, shots)
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
    crew = adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db "SELECT COUNT(*) FROM history WHERE role=\'CREW\';"')
    assert int(crew) >= 7, crew
    print("PASS: ten multiplayer mini-games, visible local handoffs, alternating actors, wagers, crew actions, history and screenshots")


if __name__ == "__main__":
    main()
