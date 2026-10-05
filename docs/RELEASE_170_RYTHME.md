# v1.7.0 — Room to breathe

A party needs enough time for drawing, listening and friends who hesitate, without making everyone wait for an absent answer. This release turned findings from the [relative-response lab](LABO_ORGANIQUE_2026.md) into four testable game rules.

## Rules changed

1. **Jury and gallery grace.** Once half of the friends have replied, the rest have seven seconds to answer, without extending beyond the round's hard deadline. If everyone replies, the verdict is immediate. On a shared phone, a private handoff pauses this countdown; the host syncs its end time to the other devices.
2. **Cast votes only.** Poses and Drawing evaluate answers that actually arrived. An absent person is not treated as a rejection. A tied jury does not approve a pose; at least half of the drawing guesses that were actually played must be right. Friends may still answer during the grace period.
3. **A little more air.** Drawing received 50 seconds to create the picture rather than 40; Music Quiz received 28 seconds rather than 23. Two-player Turbo removes two seconds rather than four. These are maximum durations: early finishes advance at once.
4. **Table prompts.** An occasional game-specific FR/EN line appears at results and phone handoffs. It invites storytelling without blocking the round. Each device can toggle it in **Settings → Party → Table prompts**.

## Paired synthetic comparison

Three identical seeds were replayed before and after the changes: **30,000 modeled parties and 720,000 rounds per version**, ten games, 2–6 FR/EN profiles, and one, two or several simulated phones. The tables show the minimum–maximum across seeds for the middle recovery model. The virtual players use assumed gestures and response times; these are not observations of people or network packets.

| Setup | Expired rounds, v1.6.0 | Expired rounds, v1.7.0 | Longest no-action stretch p90, v1.6.0 | Longest no-action stretch p90, v1.7.0 |
| --- | ---: | ---: | ---: | ---: |
| One shared phone | 15.3–15.6% | 12.7–13.1% | 106.1–107.6 s | 106.5–108.6 s |
| Two phones | 8.2–8.5% | 6.3–6.6% | 69.7–70.9 s | 70.7–70.9 s |
| One phone per player | 8.1–8.3% | 6.3–6.4% | 64.7–65.3 s | 66.1–66.4 s |
| Turbo | 14.5–14.8% | 11.6–11.9% | 93.0–94.9 s | 92.4–94.6 s |
| Two players | 9.7–10.2% | 7.0–7.4% | 53.5–53.9 s | 55.1–56.1 s |
| Six players | 12.1–12.5% | 10.1–10.4% | 98.2–101.2 s | 98.9–100.7 s |

The p90 no-action stretch rises slightly in some layouts: extra drawing and listening time helps late responses but does not necessarily create more actions. Virtual players do not interpret the table prompts, so their social effect remains unmeasured.

| Game | Expired rounds, v1.6.0 | Expired rounds, v1.7.0 |
| --- | ---: | ---: |
| Trivia | 12.0–12.5% | 11.2–12.0% |
| Poses | 8.4–8.8% | 7.7–8.0% |
| Music Quiz | 14.2–14.9% | 6.1–6.9% |
| Reflex | 12.4–12.8% | 11.7–12.3% |
| Roulette | 1.1–1.2% | 1.1–1.2% |
| Drawing | 17.8–18.1% | 7.9–8.3% |
| Memory | 12.0–12.4% | 11.9–12.1% |
| Rhythm | 0% | 0% |
| Bluff | 14.0–14.4% | 13.6–13.9% |
| Last Wire | 10.6–10.9% | 10.6–10.7% |

Rhythm reads 0% because the scripted players finish its pattern before the deadline; that is not a guarantee on a real phone. Small changes in unaffected games come from the simulated party evolving differently after the adjusted rounds.

| Seed | v1.7.0 cohorts | v1.7.0 games |
| --- | --- | --- |
| `20261005` | [CSV](data/organism-v170-10k-20261005-cohorts.csv) | [CSV](data/organism-v170-10k-20261005-games.csv) |
| `8675309` | [CSV](data/organism-v170-10k-8675309-cohorts.csv) | [CSV](data/organism-v170-10k-8675309-games.csv) |
| `11674260475909` | [CSV](data/organism-v170-10k-11674260475909-cohorts.csv) | [CSV](data/organism-v170-10k-11674260475909-games.csv) |

The [v1.6.0 CSVs](LABO_ORGANIQUE_2026.md), assumptions and export commands are in the lab report. To reproduce v1.7.0, rerun the same test with `APERO_ORGANISM_PARTIES=10000` and each seed, then export with `tools/export_organism_stress.py`.

## Verification boundary

The historical release reported 56 passing Java tests, including five new checks for quorum, cast votes, two-player grace, deadlines and bilingual prompts. An Android 16 emulator completed all ten games with two alternating profiles, stakes, friend actions, results and history. The signed v1.7.0 APK installed and launched on Android 8 and 16 emulators with the same certificate as v1.6.0.

The virtual test does not validate fun, visual or audio appeal, or Wi-Fi/Bluetooth/Internet latency on physical phones. External music services open in their own apps; Music Quiz remains based on original offline motifs.
