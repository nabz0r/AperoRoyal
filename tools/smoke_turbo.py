#!/usr/bin/env python3
"""Check Turbo starts quick rounds and still alternates actors."""
import time
from smoke_v120 import adb, add_player, play, state, tap, wait_screen, HEIGHT, PACKAGE


def main():
    adb("shell", "pm", "clear", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1.8)
    tap(200, HEIGHT - 282)
    wait_screen("LOBBY")
    add_player("Pixel")
    add_player("Nova")
    tap(200, HEIGHT - 227)
    tap(200, HEIGHT - 227)
    assert state()["mode"] == "TURBO"
    tap(200, HEIGHT - 96)
    for turn in range(2):
        s = wait_screen("TRANSITION")
        assert s["active"] == turn and s["turn"] == turn
        tap(200, HEIGHT - 78)
        wait_screen("HANDOFF")
        tap(200, HEIGHT - 78)
        wait_screen("BET")
        tap(200, 492)
        wait_screen("PREDICT")
        tap(200, HEIGHT - 90)
        tap(200, 561)
        s = wait_screen("GAME")
        play(s["game"], s)
        wait_screen("RESULT", timeout=30)
        tap(200, HEIGHT - 78)
    s = wait_screen("TRANSITION")
    assert s["active"] == 0 and s["turn"] == 2
    print("PASS: Turbo mode, two challenges, alternating actors")


if __name__ == "__main__":
    main()
