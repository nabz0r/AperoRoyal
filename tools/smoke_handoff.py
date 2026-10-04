#!/usr/bin/env python3
"""Check local handoffs pause crew and jury clocks until the next player is ready."""
import time
from smoke_v120 import adb, add_player, state, tap, wait_screen, HEIGHT, PACKAGE


def fresh_party(mode):
    adb("shell", "pm", "clear", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1.4)
    tap(200, HEIGHT - 282)
    wait_screen("LOBBY")
    add_player("Pixel")
    add_player("Nova")
    if mode == "FREE":
        tap(200, HEIGHT - 227)
    elif mode == "TURBO":
        tap(200, HEIGHT - 227)
        tap(200, HEIGHT - 227)
    assert state()["mode"] == mode
    tap(200, HEIGHT - 96)


def ready_to_bet():
    wait_screen("TRANSITION")
    tap(200, HEIGHT - 78)
    wait_screen("HANDOFF")
    tap(200, HEIGHT - 78)
    wait_screen("BET")


def ready_to_crew():
    ready_to_bet()
    tap(200, 492)
    wait_screen("CREW")


def main():
    fresh_party("FREE")
    wait_screen("LIBRARY")
    tap(110, 375)  # Roulette: friends now shield a cup.
    ready_to_crew()
    time.sleep(13.2)  # Longer than the normal twelve-second crew clock.
    assert state()["screen"] == "CREW", "crew action expired during phone handoff"
    tap(200, HEIGHT - 91)
    assert state()["screen"] == "CREW"
    tap(83, 420)
    wait_screen("GAME")

    fresh_party("FREE")
    wait_screen("LIBRARY")
    tap(290, 219)  # Silly Poses, the first row's right card.
    ready_to_bet()
    tap(200, 492)
    wait_screen("GAME")
    tap(200, HEIGHT - 145)
    assert state()["juryPhase"]
    time.sleep(21.2)  # Longer than the jury's original twenty-second clock.
    assert state()["screen"] == "GAME", "jury expired during phone handoff"
    tap(200, HEIGHT - 91)
    tap(200, 585)
    wait_screen("RESULT")

    fresh_party("FREE")
    wait_screen("LIBRARY")
    tap(110, 219)  # Quiz: crew action and resume path.
    ready_to_crew()
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1.3)
    assert state()["screen"] == "CREW", "saved game was replaced by home screen"
    tap(110, HEIGHT - 215)  # Resume button is the left half of the Home row.
    wait_screen("CREW")
    time.sleep(13.2)
    assert state()["screen"] == "CREW", "resumed handoff expired"
    tap(200, HEIGHT - 91)
    adb("shell", "input", "keyevent", "3")  # Android Home, while prediction is active.
    time.sleep(13.2)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(.6)
    assert state()["screen"] == "CREW", "backgrounded round expired"
    tap(200, 392)
    wait_screen("GAME")
    print("PASS: local handoffs, saved resume and backgrounded clocks")


if __name__ == "__main__":
    main()
