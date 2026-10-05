#!/usr/bin/env python3
"""Verify that a two-person pass-and-play party alternates the first voter and actor."""
import time
import subprocess
from pathlib import Path
from smoke_v120 import adb, add_player, play, start_from_bet, state, tap, wait_screen, HEIGHT, PACKAGE, ADB, SERIAL


def main():
    adb("shell", "pm", "clear", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1.8)
    tap(200, HEIGHT - 329)
    wait_screen("LOBBY")
    add_player("Pixel")
    add_player("Nova")
    tap(292, HEIGHT - 178)
    for turn in range(2):
        s = wait_screen("VOTE")
        assert s["active"] == turn
        if turn == 0:
            Path("docs/screenshots/vote.png").write_bytes(
                subprocess.check_output([ADB, "-s", SERIAL, "exec-out", "screencap", "-p"]))
        if s.get("ruleOwner") and s.get("ruleId", -1) < 0:
            tap(200, 450)
            assert state()["ruleId"] >= 0
        tap(200, 280)
        s = state()
        assert s["votes"][turn] == 0 and s["votes"][1 - turn] == -1, s["votes"]
        tap(200, HEIGHT - 90)
        tap(200, 280)
        s = wait_screen("TRANSITION")
        tap(200, HEIGHT - 78)
        wait_screen("HANDOFF")
        tap(200, HEIGHT - 78)
        wait_screen("BET")
        s = start_from_bet(s["game"], Path("docs/screenshots/games"))
        play(s["game"], s)
        s = wait_screen("RESULT", timeout=30)
        assert s["active"] == turn
        tap(200, HEIGHT - 78)
    s = wait_screen("VOTE")
    assert s["active"] == 0 and s["turn"] == 2
    print("PASS: first voter and active player alternate across two full rounds")


if __name__ == "__main__":
    main()
