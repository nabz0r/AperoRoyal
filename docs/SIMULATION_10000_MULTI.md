# v1.4.5 audit — 10,000 modeled multi-device parties

## What the test simulated

`TenThousandDevicePartiesTest` creates **10,000 independent rooms** with 2–6 players and alternating FR/EN profiles. Each room plays four rounds in Vote, Free or Turbo mode, covering all ten mini-games. Each virtual device receives its own network snapshot. The host remains the sole authority for answers, secrets, turns and room rules.

Eligible guests inspect a waiting-room clue, play four actions in the discovered hidden game, try an invalid cell and receive another state. The test checks that an active player cannot exploit waiting mechanics, a bomb relay is preserved, other people's goals and puzzle seeds are not sent, and profile languages survive private snapshots. Another test resumes a secret after a result, restores the save and checks that a discovery offers a new rule **on the next round**, without changing the current round's sips or points.

| Measure | Result |
| --- | ---: |
| Modeled parties | 10,000 |
| Completed rounds | 40,000 |
| Private FR/EN snapshots inspected | 160,000 |
| Hidden games completed | 35,945 |
| Chase / code / mirror | 12,731 / 14,731 / 8,483 |
| Forbidden actions rejected | 80,000 |
| Host-state restores | 400 |

Run: `./gradlew testDebugUnitTest --tests com.aperoroyale.TenThousandDevicePartiesTest`

## Design decisions from the audit

- A hidden game starts only after the player's main action has been submitted. It cannot replace answering, voting, wagering, judging or passing the bomb.
- Three taps on a subtle clue reveal a game; four meaningful actions replace the previous mechanical 20-tap counter.
- Progress survives result and selection screens. One discovery per player per round prevents waiting time from becoming a leaderboard advantage.
- Hidden-game wins give no ordinary points, since a friend whose phone waits longer should not score more. The first discovery of a secret type may offer a room-wide rule next round.
- The waiting scene uses a night alley, four high-contrast cells and a small animated pixel cat. An EN guest sees English instructions even if the active player is FR.

## Evidence boundary

The 10,000 rooms run **inside the engine**, without 10,000 sockets or people. Private snapshots test distributed data shape and authorization rules, not Wi-Fi, Bluetooth or Internet latency. One Wi-Fi journey between Android 16 and Android 8 emulators checked an EN guest joining a FR host, finding the clue, completing a code **after** a round result, receiving a trophy, selecting a rule on the guest phone and seeing rule `3` applied by the host. Enjoyment, natural discovery rate and desire to replay still require real groups.
