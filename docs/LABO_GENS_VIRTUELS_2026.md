# Stateful virtual-player lab — October 5, 2026

For a social reading of these numbers, see the [party-game design review](STRESS_TEST_SOCIAL_2026.md). The later [relative-response lab](LABO_ORGANIQUE_2026.md) reuses engine-played parties to track each agent's reaction, recovery, reserve and no-action time.

## What the agents do

[`VirtualPeopleSimulationTest`](../app/src/test/java/com/aperoroyale/VirtualPeopleSimulationTest.java) makes **stateful artificial players** act in the production `GameEngine`. Each has preferences and skill by game, a stable pace, patience, wager tolerance, trust, energy, mood and affinity with each friend. These states change across rounds. An agent can prefer a new challenge, vote for a game the active friend likes, stake less after losses, judge an alibi based on trust or reply less often after a frustrating sequence. A controlled test verifies that memory and relationships affect choices.

These are **procedural agents**, not language-model characters or copies of guests. They do not improvise conversations or experience humor, awkwardness or joy. Their behavior coefficients were chosen to explore design risks and **were not calibrated on observed people**.

## Reproducible experiment

Three seeds (`20261005`, `8675309`, `11674260475909`) × **30,000 parties of 24 rounds** yielded **90,000 parties and 2.16 million rounds per timer version**. An old-timer comparison replayed the previous Music Quiz and Memory deadlines in the current engine: **180,000 parties and 4.32 million rounds** in total. Each seed spans 2–6 players, alternating FR/EN, one/two/individual phones, three game modes and four fictional group conditions: friends, mixed, competitive and distracted. It exercises all ten games and their real engine transitions, friend contributions, juries, drawing, wagers, scores and deadlines. Multi-device actions may arrive in parallel; shared-phone actions serialize. It measures no live networking, screen or sound.

The comparison keeps seeds and cohorts fixed. Later decisions may still diverge because wins and timeouts influence mood and future votes; differences are not human causal effects. Full outputs are in [`docs/data/`](data/) as `virtual-people-legacy-*-{games,players-mode,topology-group}.csv` and `virtual-people-v149-*-{games,players-mode,topology-group}.csv`.

| Model signal | Old timer | v1.4.9 | Cautious reading |
| --- | ---: | ---: | --- |
| Music Quiz expired | 30.0–30.5% | 14.4–14.6% | Five extra seconds allow more listening and answering in this model. |
| Memory expired | 20.9–21.3% | 12.1–12.4% | Four extra seconds reduce forced endings. |
| Friends on one phone: challenge expired | 5.8–5.9% | 4.0% | Overall gain is modest. |
| Distracted table on one phone: challenge expired | 48.6–49.1% | 44.5–44.9% | This scenario remains a major risk. |
| Six players in Vote: mean round | 41.7–41.9 s | 41.8–42.0 s | The model omits free conversation time. |
| Six players in Turbo: challenge expired | 19.8–19.9% | 16.4% | The fast mode retains a time tradeoff. |

“Low energy” (<0.4) and “low mood with energy <0.6” are **arbitrary internal model signals**, not dropout or replay probabilities. For example, around 85.5–86.2% of modeled observations in the distracted, shared-phone condition remained below both mood/energy thresholds after the timer change. That mostly shows how our assumed fatigue and lateness reinforce one another; it must not be cited as human retention.

## Changes and unknowns

Music Quiz rose from 18 to **23 seconds** and Memory from 22 to **26 seconds**, before mode and bonus modifiers. In the same model, fewer challenges expired without much change in mean round time. Drawing remained around 17.8–17.9% timeout endings; its risk also involves creation time, phone passing and guesses. The project did not change every timer on this basis alone.

Agents do not see art, hear motifs, converse freely, understand consent or consumption, or decide whether a pose is funny. A modeled “network action” does not traverse Wi-Fi, Bluetooth or Internet. Thinking times and response rates are test scenarios. Three seeds test calculation stability **under those assumptions**, not whether the assumptions are true. Predictive claims require consent-based human sessions, anonymous phase timing and separate feedback on flow, participation, comfort and desire to replay.

## Reproduce

```sh
APERO_PEOPLE_PARTIES=30000 APERO_PEOPLE_SEED=20261005 APERO_PEOPLE_OLD_TIMERS=1 ./gradlew testDebugUnitTest --tests com.aperoroyale.VirtualPeopleSimulationTest --rerun-tasks
python3 tools/export_virtual_people.py app/build/test-results/testDebugUnitTest/TEST-com.aperoroyale.VirtualPeopleSimulationTest.xml docs/data/virtual-people-legacy-20261005
APERO_PEOPLE_PARTIES=30000 APERO_PEOPLE_SEED=20261005 ./gradlew testDebugUnitTest --tests com.aperoroyale.VirtualPeopleSimulationTest --rerun-tasks
python3 tools/export_virtual_people.py app/build/test-results/testDebugUnitTest/TEST-com.aperoroyale.VirtualPeopleSimulationTest.xml docs/data/virtual-people-v149-20261005
```

Without overrides, the shorter CI run uses 3,600 parties. `APERO_PEOPLE_PARTIES` accepts 180–100,000; `APERO_PEOPLE_OLD_TIMERS=1` restores only the two older deadlines in the model. Versioned CSVs make cohorts and outputs inspectable.
