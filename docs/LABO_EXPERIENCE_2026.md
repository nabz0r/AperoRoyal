# Experience-risk lab — 3,000 modeled rooms

**Historical 1.4.7 experiment.** ExperienceRiskLabTest modeled 3,000 rooms, 36,000 rounds, 2–6 players, mixed FR/EN, all ten games and one or several phones. Human response, phone handoff and network delay were assumptions, **not measured observations**. The model can expose pacing and logic risk; it cannot score whether friends actually laugh or want a rematch.

The lab used these measures: administration share of a round; p90 of the longest interval without player input; share of rounds with only crew or featured-player actions; repeat share for a mechanical variant; and repeat share for a concept card. It did not use real network transport in this experiment.

## Cohort results

| Players and devices | Mode | Mean round | Round p90 | Administration share | Longest no-input p90 |
| --- | --- | ---: | ---: | ---: | ---: |
| 2, one phone | Vote | 51.5 s | 73.2 s | 52.4% | 49.0 s |
| 2, one phone | Turbo | 39.3 s | 56.3 s | 38.1% | 49.4 s |
| 4, one phone | Vote | 74.8 s | 96.8 s | 57.5% | 61.7 s |
| 4, one phone | Turbo | 49.9 s | 69.5 s | 36.8% | 62.1 s |
| 6, one phone | Vote | 102.6 s | 131.8 s | 58.9% | 79.5 s |
| 6, one phone | Free | 69.2 s | 94.5 s | 40.2% | 86.5 s |
| 6, one phone | Turbo | 63.8 s | 86.3 s | 34.5% | 78.7 s |
| 6, several phones | Vote | 47.7 s | 67.2 s | 42.7% | 51.0 s |
| 6, several phones | Turbo | 40.7 s | 60.3 s | 30.9% | 52.3 s |

The no-input measure is the p90 longest interval without a screen touch for at least one player. A player may be talking or laughing during that interval. The one-phone, six-player Vote route was the clearest pacing concern.

Assumed network delays of 0.12, 0.65 and 2.0 seconds yielded mean rounds of 40.0, 42.7 and 48.5 seconds across mixed modes. Those are **mode outputs**, not measured transport latency.

## Per-game modeled timing

| Game | Mean round | No-input p90 | Specific note |
| --- | ---: | ---: | --- |
| Trivia | 46.7 s | 42.8 s | Answer and reveal pacing. |
| Poses | 59.0 s | 72.4 s | Performance and jury wait. |
| Sound | 51.5 s | 47.4 s | Concept repetition about 11%. |
| Reflex | 50.0 s | 46.6 s | Setup before short action. |
| Roulette | 39.5 s | 40.3 s | Quick choice and reveal. |
| Drawing | 71.7 s | 91.8 s | Longest creative action. |
| Memory | 52.1 s | 49.6 s | Handoff and sequence. |
| Rhythm | 48.2 s | 44.0 s | Timing still needs real-device calibration. |
| Bluff | 60.2 s | 74.1 s | Conversation is deliberately variable. |
| Bomb | 48.7 s | 50.9 s | Relay timing. |

## A room-stalling vote defect

A vote phase could wait indefinitely for a player who never submitted a vote. The fix set an 18-second first-vote deadline and 12 seconds after each subsequent vote; missing players abstained. A separate 1,000-vote model per group size tried 2%, 10% and 30% missing-vote assumptions. At six players and 2% per-player missing, 12.4% of rounds had at least one abstention. All scenarios progressed after the fix. Those input probabilities were assumptions, not measured absence rates.

Reproduce the lab with:

    ./gradlew testDebugUnitTest --tests com.aperoroyale.ExperienceRiskLabTest

The next evidence needed is observed tables: whether setup is understood without instruction, whether pauses feel dramatic or tedious, whether reveals prompt conversation and whether a game is voluntarily requested again. Physical devices must separately test radio coexistence, Wi-Fi/Bluetooth/Internet delay and reconnect behavior.
