# The Apéro Royale design archive

The game is built in public, and its design claims come with a test boundary. Start with the current release, then follow the trail from visual decisions to synthetic party models. **A simulated party is a diagnostic tool, not a human playtest.** No report here establishes that a game is viral, that people enjoyed it, or that physical-phone networking has a particular latency.

## Start here

| Report | What it answers |
| --- | --- |
| [v1.8.0 — Trust the table](RELEASE_180_SOCIAL.md) | Named-friend trust, round sharing, private network snapshots and 10,000 focused social rounds. |
| [v1.7.0 — Room to breathe](RELEASE_170_RYTHME.md) | Seven-second jury/drawing grace, revised timers and paired timing comparison. |
| [v1.6.0 — The night is yours](RELEASE_160_DESIGN.md) | Adult art direction, ten game reveals, FR/EN menus and emulator verification. |
| [v1.5.0 — Interface and party stress](UI_150_ET_STRESS.md) | Slimmer controls, late admission and three long synthetic models. |

## Game design and product research

| Note | Lens |
| --- | --- |
| [Ten-game redesign](REFONTE_DIX_MINI_JEUX.md) | Participant roles, content variety and acceptance criteria proposed before later implementations. |
| [Product audit](AUDIT_PRODUIT_2026.md) | Early friction inventory and implementation priorities; a historical snapshot, not the current feature list. |
| [Social party-game stress review](STRESS_TEST_SOCIAL_2026.md) | How success criteria and scene-level social dynamics were framed; interpretation remains qualitative. |
| [v1.4.6 design](RELEASE_146_DESIGN.md) | Persistent profiles, navigation, music dock and Play Store reference analysis. |

## Simulation labs

| Lab | Model and evidence |
| --- | --- |
| [Relative-response lab](LABO_ORGANIQUE_2026.md) | 30,000 virtual parties, 720,000 modeled rounds, individual baselines and three seeds. Includes [CSV data](data/). |
| [Stateful virtual players](LABO_GENS_VIRTUELS_2026.md) | Assumed tastes, relationships, energy and mood, with reproducible seeds. |
| [Virtual-clock extremes](LABO_EXTREME_2026.md) | Deadlines, absent responses, handoffs and device layouts with 100,000 modeled parties. |
| [Synthetic party experience](LABO_EXPERIENCE_2026.md) | A smaller early model of preparation, waiting and participation. |
| [500-party resilience](SIMULATION_500_PARTIES.md) | State restoration and transition bugs caught in an early engine version. |
| [500-party participation](SIMULATION_EXPERIENCE_500.md) | Early repetition and turn-density diagnosis. |
| [10,000 multi-device parties](SIMULATION_10000_MULTI.md) | Private snapshots and hidden-game behavior in a modeled network room. |

The archive is written in English. Numbers in older reports belong to the version and assumptions named in each report.

## Reproduce the current checks

From the repository root, with JDK 17 and Android SDK 36:

```sh
APERO_SIM_PARTIES=10000 APERO_PEOPLE_PARTIES=10000 \
APERO_ORGANISM_PARTIES=10000 APERO_SOCIAL_ROUNDS=10000 \
./gradlew :app:testDebugUnitTest :app:assembleRelease :app:lintVitalRelease \
  --offline --rerun-tasks
```

The `docs/data/` CSVs are frozen outputs from named historical runs. They are useful for inspecting and reproducing those comparisons, not as measured player behavior. The current signed APK and its checksum are in the [release notes](../RELEASE.md).
