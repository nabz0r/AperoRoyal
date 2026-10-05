#!/usr/bin/env python3
"""Check a shared-phone abstention on a disposable Android emulator.

Clears only this emulator's Apéro Royale data.
Usage: ADB_SERIAL=emulator-5554 python3 tools/smoke_skip_handoff.py
"""
from smoke_vote_timeout import start_vote
from smoke_v120 import HEIGHT, state, tap, wait_screen


def main():
    start_vote()
    tap(200, 280)
    assert state()["votes"] == [0, -1]
    tap(200, HEIGHT - 164)  # The new "away? skip" button on the private handoff.
    result = wait_screen("TRANSITION", timeout=2)
    assert result["votes"] == [0, 3], result["votes"]
    assert result["voteWinner"] == 0

    crew_games = {0, 2, 3, 4, 6, 7}
    for _ in range(5):
        ballot = start_vote()
        options = [i for i, game in enumerate(ballot["offers"]) if game in crew_games]
        if options:
            break
    else:
        raise AssertionError("no crew challenge offered")
    tap(200, 280 + 103 * options[0])
    tap(200, HEIGHT - 164)
    assert wait_screen("TRANSITION", timeout=2)["game"] in crew_games
    tap(200, HEIGHT - 78)
    wait_screen("HANDOFF")
    tap(200, HEIGHT - 78)
    wait_screen("BET")
    tap(200, 492)
    wait_screen("CREW")
    tap(200, HEIGHT - 164)
    result = wait_screen("GAME", timeout=2)
    assert result["crewChoices"][1] == -2, result["crewChoices"]
    print("PASS: absent shared-phone voter and crew member can be skipped safely")


if __name__ == "__main__":
    main()
