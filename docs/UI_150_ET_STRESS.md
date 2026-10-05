# v1.5.0 — Interface and party stress test

## Visual intent

The home screen kept its party illustration while giving the controls less visual weight. Buttons became slimmer and darker, with a color cue and readable label; their touch targets remained generous. The lobby lists friends and places Vote, Free and Turbo side by side. Audio and party settings use rows rather than a tower of large cards.

The ten game scenes and sprites kept their identities, with calmer framing and a distinct color for each challenge. Trivia, Music Quiz and Drawing answers use A–D rows. Roulette draws six glasses. Memory uses dark cells with colored borders. Rhythm and Last Wire retain large touch regions around smaller visual cores. See the [home screen](screenshots/home.png), [lobby](screenshots/lobby-four.png) and [ten-game gallery](../README.md#ten-games-ten-reasons-to-talk).

## Emulator journeys

- On Android 16, two players completed all ten games with handoffs, wagers, friend actions, saved state and leaderboard. The first Vote and Turbo rounds alternated the active player.
- Vote, Free and Turbo selection, settings and screenshots were checked on 1080 × 2340 and 1080 × 1920 emulator displays.
- Across two emulators on local Wi-Fi, a guest arrived during a round, saw the active challenge on their Android 8 phone, played three hidden waiting games, faced admission at the next break, was declined by timeout, received a virtual sip in SQLite and could request another vote. An earlier journey verified admission after two positive votes and a return to the normal game interface.
- These journeys check interactions and rendering. They do not measure enjoyment, audio on real speakers or physical-phone latency.

## Three reproducible stress models

| Model | Sample | Automated result |
| --- | ---: | --- |
| `TenThousandDevicePartiesTest` | 10,000 parties, 40,000 rounds, 160,000 private FR/EN views | Two tests passed; secrets and restores exercised. |
| `VirtualPeopleSimulationTest` with `APERO_PEOPLE_PARTIES=10000` | 10,000 parties, 240,000 rounds | Three tests passed; stateful agents, voting and contributions. |
| `ExtremePartySimulationTest` with `APERO_SIM_PARTIES=10000` | 10,000 parties, 180,000 rounds | One test passed; 2–6 players, modeled device layouts, three modes and four attention scenarios. |

The models have different assumptions; their volume does not equal 30,000 real parties. The virtual clock applies engine deadlines to assumed response times. In its scenario mix, mean round duration ranged from **82 s** for Vote on one shared phone to **41.6 s** for Turbo on individual phones. Modeled timeout endings were about **26%** for shared-phone Vote and **39%** for individual-phone Turbo in this sample: a shorter round is not automatically smoother. These numbers help identify steps to shorten or explain, not willingness to replay or perceived ease with people. Late admission had transition, resume, ballot-privacy and two-emulator checks; the three long simulations above did not yet model the social behavior of admission requests.

The interface gives each mode a short explanation, one clear action per phase and answer choices that read as decisions rather than a wall of buttons. A useful next human check would time the first round, observe hesitation before each gesture and note when verbal explanation is needed.

## Reproduce

```sh
./gradlew testDebugUnitTest --tests com.aperoroyale.TenThousandDevicePartiesTest --offline
APERO_PEOPLE_PARTIES=10000 ./gradlew testDebugUnitTest --tests com.aperoroyale.VirtualPeopleSimulationTest --rerun-tasks --offline
APERO_SIM_PARTIES=10000 ./gradlew testDebugUnitTest --tests com.aperoroyale.ExtremePartySimulationTest --rerun-tasks --offline
ADB_SERIAL=emulator-5554 python3 tools/smoke_v120.py
ADB_SERIAL=emulator-5554 python3 tools/smoke_turns.py
ADB_SERIAL=emulator-5554 python3 tools/smoke_turbo.py
```

The `smoke_*` scripts operate the selected emulator and may replace its local session. Java tests do not require a device.
