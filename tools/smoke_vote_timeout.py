#!/usr/bin/env python3
"""Check a missing local vote on a disposable Android emulator.

Clears only this emulator's Apéro Royale data. Usage:
ADB_SERIAL=emulator-5554 python3 tools/smoke_vote_timeout.py
"""
import time

from smoke_v120 import adb, add_player, state, tap, wait_screen, HEIGHT, PACKAGE


def start_vote():
    adb("shell", "pm", "clear", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1)
    tap(200, HEIGHT - 326)
    wait_screen("LOBBY")
    add_player("Pixel")
    add_player("Nova")
    tap(200, HEIGHT - 96)
    return wait_screen("VOTE")


def main():
    first = start_vote()
    assert first["votes"] == [-1, -1]
    tap(200, 280)
    assert state()["votes"] == [0, -1]
    tap(200, HEIGHT - 90)  # Confirm the shared-phone handoff.
    assert state()["screen"] == "VOTE"
    resolved = wait_screen("TRANSITION", timeout=16)
    assert resolved["votes"] == [0, 3], resolved["votes"]
    assert resolved["voteWinner"] == 0

    second = start_vote()
    assert second["votes"] == [-1, -1]
    resolved = wait_screen("TRANSITION", timeout=22)
    assert resolved["votes"] == [3, 3], resolved["votes"]
    print("PASS: one missing vote abstains after 12 s; empty ballot resolves after 18 s")


if __name__ == "__main__":
    main()
