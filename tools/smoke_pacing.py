#!/usr/bin/env python3
"""Check the timed drawing handoff and a roulette choice timeout on Android."""
import time

from smoke_handoff import fresh_party, ready_to_predict
from smoke_v120 import adb, state, tap, wait_screen, HEIGHT, SCALE


def open_free_game(index):
    fresh_party("FREE")
    wait_screen("LIBRARY")
    tap(110 + (index % 2) * 180, 219 + (index // 2) * 78)
    ready_to_predict()
    tap(200, HEIGHT - 91)
    tap(200, 561)  # The other player challenges, so no support time is added.
    current = wait_screen("GAME")
    assert current["game"] == index
    return current


def main():
    open_free_game(5)
    adb("shell", "input", "swipe", str(round(90 * SCALE)), str(round(360 * SCALE)),
        str(round(250 * SCALE)), str(round(500 * SCALE)), "300")
    tap(200, HEIGHT - 126)
    current = state()
    assert current["drawingReady"] and current["deadline"] > current["started"]
    original_deadline = current["deadline"]
    time.sleep(13.5)
    assert state()["screen"] == "GAME", "drawing guess expired during private handoff"
    tap(200, HEIGHT - 91)
    current = state()
    assert current["deadline"] > original_deadline + 12_000, "guesser did not get a fresh clock"
    tap(200, 522 + current["target"] * 50)
    assert wait_screen("RESULT")["lastWon"]

    current = open_free_game(4)
    assert current["deadline"] > current["started"]
    time.sleep(max(0, (current["deadline"] - time.time() * 1000) / 1000) + 1.2)
    current = wait_screen("RESULT")
    assert not current["lastWon"], "roulette did not settle after the choice timer"
    print("PASS: drawing handoff keeps a fresh guess clock; roulette choice expires")


if __name__ == "__main__":
    main()
