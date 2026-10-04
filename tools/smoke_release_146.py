#!/usr/bin/env python3
"""Exercise saved local players, the pause menu and the one-tap radio dock.

Runs on a disposable emulator and clears only that emulator's app data.
Usage: ADB_SERIAL=emulator-5554 python3 tools/smoke_release_146.py
"""
import json
import time
import xml.etree.ElementTree as ET

from smoke_v120 import (adb, add_player, dialog_node, first_edit, state,
                        tap, tap_node, wait_screen, HEIGHT, PACKAGE)


def item(label):
    adb("shell", "uiautomator", "dump", "/sdcard/apero-window.xml")
    root = ET.fromstring(adb("shell", "cat", "/sdcard/apero-window.xml"))
    nodes = list(root.iter("node"))
    for node in [n for n in nodes if n.get("text", "").casefold() == label.casefold()] + nodes:
        if label.casefold() in node.get("text", "").casefold():
            bounds = node.get("bounds", "").replace("][", ",").strip("[]")
            x1, y1, x2, y2 = map(int, bounds.split(","))
            adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
            time.sleep(.45)
            return
    raise AssertionError(f"dialog item {label!r} missing")


def roster():
    raw = adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db '
        '"SELECT data FROM roster WHERE id=2;"')
    return json.loads(raw) if raw else []


def menu():
    tap(362, 54)
    time.sleep(.25)


def main():
    adb("shell", "pm", "clear", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1)
    tap(200, HEIGHT - 326)  # Start the party on the new home screen.
    wait_screen("LOBBY")
    add_player("Pixel")
    add_player("Nova")
    assert [p["name"] for p in roster()] == ["Pixel", "Nova"]
    tap(200, HEIGHT - 227)  # Free mode.
    tap(200, HEIGHT - 96)
    wait_screen("LIBRARY")
    tap(110, 219)  # Quiz.
    wait_screen("TRANSITION")
    tap(200, HEIGHT - 78)
    wait_screen("HANDOFF")
    tap(200, HEIGHT - 78)
    wait_screen("BET")
    tap(200, 492)
    wait_screen("CREW")

    menu()
    time.sleep(13)  # The 12-second preparation clock must stay paused.
    item("Reprendre le jeu")
    assert state()["screen"] == "CREW", "timer expired behind pause menu"

    menu()
    item("Annuler ce défi")
    wait_screen("LIBRARY")
    assert [p["score"] for p in state()["players"]] == [0, 0]
    raw = adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db '
        '"SELECT count(*) FROM history;"')
    assert raw == "0", "cancelled challenge recorded a result"

    menu()
    item("Retour au salon")
    item("Retour au salon")
    assert [p["name"] for p in wait_screen("LOBBY")["players"]] == ["Pixel", "Nova"]

    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(.8)
    tap(200, HEIGHT - 326)
    assert [p["name"] for p in wait_screen("LOBBY")["players"]] == ["Pixel", "Nova"]

    adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db '
        '"INSERT INTO stats(name,wins,games,points) VALUES(\'Pixel\',1,1,100);"')
    adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db '
        '"INSERT INTO history(ts,player,game,won) VALUES(1,\'Pixel\',\'QUIZ\',1);"')
    tap(180, 209)
    item("Modifier pseudo")
    field = first_edit()
    tap_node(field)
    adb("shell", "input", "keyevent", "123")
    for _ in range(5):
        adb("shell", "input", "keyevent", "67")
    adb("shell", "input", "text", "Ace")
    adb("shell", "input", "keyevent", "4")
    tap_node(dialog_node("android:id/button1"))
    time.sleep(.3)
    assert [p["name"] for p in roster()] == ["Ace", "Nova"]
    old = adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db '
        '"SELECT count(*) FROM stats WHERE name=\'Ace\' AND points=100;"')
    assert old == "1", "renaming lost the lifetime score"
    old = adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db '
        '"SELECT count(*) FROM history WHERE player=\'Ace\';"')
    assert old == "1", "renaming lost the turn history"

    tap(343, 158)
    item("Changer")
    assert len(wait_screen("LOBBY")["players"]) == 0
    assert roster() == []
    kept = adb("shell", f'run-as {PACKAGE} sqlite3 databases/apero_royale.db '
        '"SELECT count(*) FROM history WHERE player=\'Ace\';"')
    assert kept == "1", "changing the table erased lifetime history"
    menu()
    item("Home")
    tap(246, HEIGHT - 170)
    item("ORIGINAL")
    prefs = adb("shell", f"run-as {PACKAGE} cat shared_prefs/MainActivity.xml")
    assert 'name="musicProvider" value="0"' in prefs
    tap(246, HEIGHT - 170)
    item("SILENCE")
    prefs = adb("shell", f"run-as {PACKAGE} cat shared_prefs/MainActivity.xml")
    assert 'name="musicProvider" value="5"' in prefs
    tap(246, HEIGHT - 170)
    item("PLAY / PAUSE")  # No external player is required; the command must not crash.
    item("CLOSE")
    print("PASS: roster/restart/rename, pause/cancel/lobby exit, reset, radio and persisted sources")


if __name__ == "__main__":
    main()
