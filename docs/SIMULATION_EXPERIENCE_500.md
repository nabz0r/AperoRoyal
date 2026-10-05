# Pace, repetition and participation — 500 modeled parties

This is a **historical model**, not a user study. It compares early 1.4.x builds under assumed reading, handoff and action times. Later releases changed the game. The figures below describe the model at the named version, not the current party experience.

## 1.4.4: an action for every player

In the modeled 1.4.4 flow, friends answered in Trivia and Sound; placed starting targets in Reflex; protected cups in Roulette; built the initial Memory chain; and selected beats in Rhythm. Drawing guesses, Poses and Bluff juries, and the Bomb relay also involved the table. A private handoff paused the appropriate timer on one phone.

| Configuration | Modeled mean round | Rounds with one participant | Game-specific actions per player |
| --- | ---: | ---: | ---: |
| 2 players, 1 phone, Vote | 39.0 s | 0% | 1.00 |
| 4 players, 1 phone, Vote | 58.2 s | 0% | 1.00 |
| 6 players, 1 phone, Vote | **77.3 s** | 0% | 1.00 |
| 6 players, 1 phone, Turbo | **49.4 s** | 0% | 1.00 |
| 6 players, separate phones, Vote | 35.5 s | 0% | 1.00 |

The participation improvement did not prove the interactions were enjoyable. The six-player, one-phone Vote route remained long.

## 1.4.3 countercheck

Earlier changes added game-specific actions but still left about half the rounds with only the featured player acting within the mini-game:

| One-phone cohort | Mean 1.4.2 → 1.4.3 | One-actor rounds 1.4.2 → 1.4.3 | Longest run without a game-specific action |
| --- | ---: | ---: | ---: |
| 2 players, Vote | 41.7 → **41.3 s** | 59.5 → **52.8%** | 1 → **1 round** |
| 4 players, Vote | 64.8 → **61.1 s** | 59.8 → **50.0%** | 3 → **2 rounds** |
| 6 players, Vote | 87.3 → **81.0 s** | 60.7 → **50.9%** | 5 → **2 rounds** |
| 6 players, Turbo | 59.3 → **53.1 s** | 59.7 → **51.4%** | 5 → **2 rounds** |

## Original 1.4.2 baseline

The model used deterministic input delays rather than real fingers or measured human behavior. Its preparation share included mode selection, wagers, predictions and physical handoffs. The exact round mix was governed by the test. It could identify bottlenecks and logic regressions, but could not infer laughter or replay intent.

| Configuration | Modeled mean round | Setup share | Rounds over 45 s | Actor-only game rounds | Longest actor-only run |
| --- | ---: | ---: | ---: | ---: | ---: |
| 2 players, 1 phone, Vote | 41.7 s | 67.2% | 30.2% | 59.5% | 1 round |
| 4 players, 1 phone, Vote | 64.8 s | 74.1% | 100% | 59.8% | 3 rounds |
| 6 players, 1 phone, Vote | **87.3 s** | **77.9%** | **100%** | **60.7%** | **5 rounds**, up to 434 modeled seconds |
| 6 players, 1 phone, Turbo | 59.3 s | 67.4% | 90.1% | 59.7% | 5 rounds |
| 6 players, separate phones, Vote | 36.8 s | 62.6% | 9.1% | 62.7% | 5 rounds |

Five sequential votes on one phone took roughly 68 modeled seconds per six-player Vote round before the central action; even 35% faster assumed gestures left roughly 44 seconds. That was a pacing risk, not a real-party duration.

## Content and participation at the baseline

| Game | Modeled rounds | Who acted inside the game | Distinct content | Diagnosis then |
| --- | ---: | --- | ---: | --- |
| Trivia | 743 | Featured player | 17 questions | Friends only predicted. |
| Poses | 785 | Player and jurors | 18 prompts | Social, but binary judgment. |
| Sound | 718 | Featured player | 6 motifs | Too little content. |
| Reflex | 870 | Featured player | 1 rule | Variable targets, same ten-tap goal. |
| Roulette | 800 | Featured player | 1 rule | Friends could not affect the reveal. |
| Drawing | 765 | Artist and one guesser | 17 concepts | Four of six friends could wait. |
| Memory | 830 | Featured player | 1 rule | Longer sequences, no friend decision. |
| Rhythm | 783 | Featured player | 1 rule | Four taps, no call and response. |
| Bluff | 839 | Storyteller and jurors | 17 prompts | Strong social seed needing real playtests. |
| Bomb | 857 | Everyone tapped | 1 rule | Handoff without a meaningful choice. |

Six of ten games were actor-only inside the challenge at this point. Predictions were a common scoring layer, not sufficient participation. Compare the later 1.4.4 table above before drawing a current-product conclusion.

## Fixes made in 1.4.2

1. Questions, poses, sound motifs, drawings and bluff prompts used persisted shuffled decks. Each item appeared once before reuse, and the first item after reshuffling differed from the last. The 7,990-round model found **zero premature duplicates**. Free mode could still intentionally repeat a chosen game.
2. Roulette gained a ten-second cup-choice deadline. Drawing gained 30 seconds to create and 12 seconds to guess; the guess timer paused for a physical handoff. Existing time bonuses could still extend play.
3. Regression tests covered deck exhaustion across save and restore, reshuffle boundaries, drawing phases and roulette completion.

Reproduce this historical audit with:

    ./gradlew testDebugUnitTest --tests com.aperoroyale.PartyExperienceSimulationTest

The test result contains the 30-cohort table. A proper follow-up remains an observed 2/4/6-player session on one and several phones, with mixed FR/EN players and unprompted replay choices. Record network divergence separately from enjoyment.
