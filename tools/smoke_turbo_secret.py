#!/usr/bin/env python3
"""Exercise the Turbo secret-rule screen on a shared Android emulator."""

import time
from pathlib import Path
from smoke_v120 import adb, add_player, play, start_from_bet, state, tap, wait_screen, HEIGHT, PACKAGE


def main():
    adb("shell", "pm", "clear", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1.8)
    tap(200, HEIGHT - 329)
    wait_screen("LOBBY")
    add_player("Pixel")
    add_player("Nova")
    tap(322, HEIGHT - 248)
    assert state()["mode"] == "TURBO"
    tap(292, HEIGHT - 178)
    for turn in range(20):
        s = wait_screen("TRANSITION")
        assert s["turn"] == turn
        tap(200, HEIGHT - 78)
        wait_screen("HANDOFF")
        tap(200, HEIGHT - 78)
        wait_screen("BET")
        s = start_from_bet(s["game"], Path("docs/screenshots/games"))
        play(s["game"], s)
        wait_screen("RESULT", timeout=30)
        tap(200, HEIGHT - 78)
        s = state()
        if s["screen"] == "RULE_PICK":
            assert s["ruleOwner"] in {"Pixel", "Nova"}
            assert s["ruleId"] == -1
            tap(200, 450)
            s = wait_screen("TRANSITION")
            assert s["ruleId"] >= 0 and s["turn"] == turn + 1
            print(f"PASS: Turbo secret rule selected by {s['ruleOwner']} after {turn + 1} rounds")
            return
    raise AssertionError("no secret-rule picker appeared in twenty Turbo rounds")


if __name__ == "__main__":
    main()
