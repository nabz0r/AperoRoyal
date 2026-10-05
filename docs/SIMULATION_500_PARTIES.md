# 500-party simulation — v1.4.1

The reproducible [`PartySimulationTest`](../app/src/test/java/com/aperoroyale/PartySimulationTest.java) drives the **production game engine** through 500 seeded parties of 2–6 players in Vote, Free and Turbo modes. Each party lasts 12–20 rounds. The test exercises actors, wagers, predictions, voting, jury, roulette, reflex, bomb relays, secret rules and disputed-rule votes. It regularly saves and restores JSON state, then checks turn order, scores, counters and privacy in guest snapshots. The [v1.4.2 participation audit](SIMULATION_EXPERIENCE_500.md) adds another 500 parties with explicit time assumptions.

## Result after fixes

| Measure | Result |
| --- | ---: |
| Simulated parties | 500 |
| Completed rounds | 7,990 |
| Wins / losses | 4,041 / 3,949 |
| Expired predictions | 1,012 |
| Jury deadlines | 396 |
| Secret rules selected | 498, including 166 in Turbo |
| Disputed rules sent to a vote | 436 |
| JSON restores checked | 40,190 |

Every mini-game appeared. The checked runs had no missing turns, negative scores, win counts above round counts, immediate Vote/Turbo repeats or disclosed guest secrets. Seeded decisions produced the same totals on two complete runs on October 4, 2026. These are **not observations of human parties**.

## Bugs caught and fixed

1. **Turbo skipped a newly unlocked secret rule.** `startSelection()` launched the next challenge before the owner had a rule-picking screen. The engine now goes through `RULE_PICK` first, on the owner's phone or their guest device.
2. **The jury timeout ignored cast votes in `GameEngine.checkTimeout()`.** A positive verdict could become a loss when that path handled expiry. It now uses `juryVerdict()` for Poses and Bluff, matching the activity's existing clock path.

Both cases have focused tests in [`GameEngineRulesTest`](../app/src/test/java/com/aperoroyale/GameEngineRulesTest.java). The 500-party test remains in the suite as a regression check.

## Evidence boundary

The simulation exercises engine rules and serialization, **not 500 Android UI sessions or live network connections**. Touch, audio, SQLite, physical devices, packet loss and the Internet relay need separate tests. The ten-game Android journey with two profiles complements this test without proving party quality.

Run: `./gradlew testDebugUnitTest --tests com.aperoroyale.PartySimulationTest`
