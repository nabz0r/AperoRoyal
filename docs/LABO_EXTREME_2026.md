# Virtual-clock party lab — October 5, 2026

## What was exercised

[`ExtremePartySimulationTest`](../app/src/test/java/com/aperoroyale/ExtremePartySimulationTest.java) drives the **production `GameEngine` and its real deadlines** with a virtual clock. Three independent seeds each ran **100,000 parties of 18 rounds**: **300,000 modeled parties and 5.4 million rounds** in total. Each seed covers 2–6 players, alternating FR/EN, one/two/individual phones, Vote/Free/Turbo and four modeled environments. All ten games, selection, friend contributions, drawing, juries, rhythm, relays, secret rules, saves and actual continuation after restore are exercised.

Seeds: `11674260475909`, `20261005`, `8675309`. The [180 cohorts](data/extreme-100k-cohorts.csv) and [per-game results](data/extreme-100k-games.csv) for the first seed are frozen as CSV. The reported ~4.5-second test-computation time for a long run excludes rendering, audio and network transport; it says nothing about phone performance.

An earlier 3,000-party lab used durations separate from the engine clock. Here, the engine itself receives virtual time and can close a vote, end a challenge, record a result and resume from a save. An action after its deadline is no longer counted as a success.

## Exploration parameters, not player data

| Modeled variable | Range or rule | Purpose |
| --- | --- | --- |
| Stable table speed | Factor 0.80–1.45 | Correlate gestures within a party. |
| Stable individual speed | Bounded lognormal, 0.55–2.40 × base time | Let a hesitant player remain relatively hesitant across rounds. |
| Conversation | Action time ×1.35; occasional 7–30 s interruptions | Avoid independent, instant-click assumptions. |
| Interrupted party | Action time ×1.55; 5.5% of actions get another interruption | Explore a noisy night without claiming that rate was observed. |
| Missing response | Base rates 0.6%, 1.5%, 4.5% or 2.5%; one absence raises the next chance | Test correlated missing actions and their resolution. |
| Multiple devices | Lognormal delay; disturbed scenario with 3–15 s spikes and correlated round outages | Explore delay/loss envelopes; **no real network was measured**. |
| Absent player on one phone | After 20 s, another friend uses **Absent? Skip turn** | Exercise the visible recovery action. |

These numbers are **chosen exploration parameters**, not scientific estimates of party-game users. Three seeds mainly check stability under the *same assumptions*. They cannot repair a wrong assumption.

## Final model results

Ranges below are minimum–maximum across the three seeds. A row fixes player count, device layout, mode and environment. “No-touch p90” is the 90th percentile of the longest time without a screen action for at least one person; talking or laughing during that interval is invisible to the model.

| Setup | Mean round | Round p90 | Individual no-touch p90 | Administration | Incomplete contribution | Challenge expired |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 2, one phone, Vote, calm | 46.4–46.8 s | 66–67 s | 48–49 s | 53.5–54.1% | 0.6% | 9.7–11.2% |
| 6, one phone, Vote, calm | 94.5–95.0 s | 116–117 s | 84 s | 53.3% | 2.8–3.0% | 20.5–21.0% |
| 6, one phone, Vote, interrupted | 117.4–118.0 s | 150–151 s | 133–134 s | 52.2–52.5% | 29.0–30.6% | 52.4–53.2% |
| 6, one phone, Turbo, calm | 57.1–57.5 s | 75–76 s | 72 s | 26.0–26.1% | 10.1–10.7% | 26.7–28.0% |
| 6, one phone, Turbo, interrupted | 66.7–67.1 s | 93–94 s | 91–92 s | 26.6–26.7% | 42.6–44.2% | 59.5–61.2% |
| 6, individual phones, Vote, calm | 42.1–42.2 s | 60–61 s | 50–51 s | 45.7–45.8% | 1.4–2.0% | 18.2–19.1% |
| 6, individual phones, Vote, disturbed link | 61.1–61.3 s | 83–84 s | 77–78 s | 46.5–46.7% | 20.8–21.4% | 42.9–43.2% |

“Expired challenges” are results of the **chosen behavior model**, not observed human failure rates. Per-game data still points to scenarios for real playtests: Poses, Drawing and Bluff depend on how long friends take to perform and tell stories; Last Wire depends on physical passing and synchronization. The model cannot tell whether those seconds are funny or dull.

## Changes triggered by the test

**Shared-phone handoff.** An absent person could leave the app indefinitely on the “It's me” screen because deadlines paused during handoff. **Absent? Skip turn** now records a skipped vote as abstention and a skipped contribution without points. Jury and drawing use submitted answers; an abandoned bomb ends the challenge. [Emulator screenshot](screenshots/handoff-skip.png).

**Friend contributions.** Before the change, a calm six-player Turbo party on one phone failed to collect every contribution in 60.3% of relevant modeled rounds because the window was fixed at five seconds. It became eight seconds in Turbo and twelve in other modes, **renewed after each friend**. Under the same seed and assumptions, incomplete contribution fell to 9.7% in calm Turbo, while mean round duration rose from 42.3 to 53.7 s. In calm Vote, incompleteness fell from 54.3% to 2.7%, while mean round duration rose from 87.2 to 92.1 s. [Before](data/extreme-100k-before-crew-timer.csv) and [after](data/extreme-100k-after-crew-timer.csv) CSVs preserve this sensitivity check. Their old `p90_global_no_touch_s` metric measured inactivity across *all screens at once*; the final model's `p90_no_touch_s` measures individual waiting and must not be directly compared with it.

**Social creation time.** Poses and Bluff were given 40 seconds to act or tell a story; Drawing got 40 seconds to create before guesses. With the first seed and corrected bomb pause, modeled timeout losses moved from 67.6% to 43.9% in Poses, 65.7% to 47.2% in Drawing, 67.7% to 43.8% in Bluff and 63.5% to 53.2% in Last Wire. These are **model sensitivity results**, not proof that 40 seconds is optimal.

## Evidence boundary and reproduction

The model cannot see laughter, discomfort, conversation, skipped dares, fatigue or musical enjoyment. It does not measure rendering, real touch, audio sync, Wi-Fi, Bluetooth, the Internet relay or reconnection on physical phones. Agent win rates are test rules. Predictive claims require consenting FR/EN parties, anonymous phase timing and separate feedback on flow, shared participation and desire to replay.

```sh
APERO_SIM_PARTIES=100000 APERO_SIM_SEED=11674260475909 ./gradlew testDebugUnitTest --tests com.aperoroyale.ExtremePartySimulationTest --rerun-tasks
python3 tools/export_extreme_report.py app/build/test-results/testDebugUnitTest/TEST-com.aperoroyale.ExtremePartySimulationTest.xml docs/data/extreme-100k
```

The short CI run uses 3,600 parties. The test accepts up to 100,000 per run. Published CSVs come from the seed above; the other two establish the table ranges. Timing and behavior parameters are in the test and its `Session` model.
