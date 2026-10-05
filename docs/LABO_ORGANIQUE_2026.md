# Organic-response lab — 30,000 synthetic parties

This is a **modeled behavior experiment**, not biometric research on people. Three seeds ran 10,000 synthetic parties each: 30,000 parties and 720,000 rounds. It exercised GameEngine with 2–6 FR/EN players, one phone, two phones or one phone per person, Vote/Free/Turbo modes, and all ten games. No physical sensors, pulse, alcohol level or real participant behavior were collected.

The virtual agents had different activation, reserve, silence and recovery traits. Their activation model was:

    A_i(t) = A_i(t−1) × exp(−Δt / τ) + reactivity_i × stimulus_i(t)

Fast, medium and slow recovery profiles were compared within each person: first eight versus last eight rounds, normalized against that person's starting state. Figures below are ranges across three seeds. “Reserve loss” is a synthetic fatigue proxy; “activation” is a synthetic response proxy. Neither is a medical or psychological measure.

| Cohort | Activation change | Reserve loss | No-action p90 | Expired rounds |
| --- | ---: | ---: | ---: | ---: |
| One phone | +0.0165–0.0169 | +0.0516–0.0522 | 106–108 s | 15.3–15.6% |
| Two phones | +0.0393–0.0399 | +0.1054–0.1063 | 70–71 s | 8.2–8.5% |
| Individual phones | +0.0503–0.0509 | +0.1246–0.1252 | 65–66 s | 8.1–8.3% |
| Vote | +0.0266–0.0277 | +0.0825–0.0836 | 60–62 s | 8.7–9.0% |
| Free | +0.0314–0.0326 | +0.0913–0.0920 | 98–100 s | 8.5–8.6% |
| Turbo | +0.0432–0.0437 | +0.1076–0.1098 | 93–95 s | 14.5–14.8% |
| Two players | +0.0697–0.0728 | +0.1564–0.1591 | 53–54 s | 9.7–10.2% |
| Six players | +0.0238–0.0249 | +0.0717–0.0721 | 98–102 s | 12.1–12.5% |

Drawing expired in 17.8–18.1% of its modeled rounds and Sound in 14.2–14.9%. Rhythm showed 0% expiry because the scripted agent could always submit its deterministic action; that result **does not** establish physical-device rhythm reliability.

The model's practical signal was that single-phone handoffs and longer creative rounds deserved closer observation, while more devices shortened assumed idle time but consumed more modeled reserve. These are relative responses to the chosen agent rules, not predictions of real human motivation.

Reproduce one seed:

    APERO_ORGANISM_PARTIES=10000 APERO_PEOPLE_SEED=20261005 ./gradlew testDebugUnitTest --tests com.aperoroyale.OrganismStressSimulationTest --rerun-tasks

The other seeds were 8675309 and 11674260475909. The exported cohort and game CSVs live under [data](data/) with organism-10k seed names. Any claim about player fatigue or enjoyment still needs real, consented party sessions and calibration against observed behavior.
