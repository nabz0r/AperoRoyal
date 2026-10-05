package com.aperoroyale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Test;

/**
 * Organism-inspired design probe over real GameEngine rounds. The state variables are deliberately
 * dimensionless: they are not heart rate, HRV, intoxication, emotion, or measurements of people.
 */
public final class OrganismStressSimulationTest {
  private static final int ROUNDS = 24;
  private static final String[] TOPOLOGY = {"one_phone", "two_phones", "one_each"};
  private static final String[] MODE = {"vote", "free", "turbo"};
  private static final Profile[] PROFILES = {
      new Profile("fast", 24, 100),
      new Profile("middle", 55, 180),
      new Profile("slow", 100, 300)
  };

  private record Profile(String name, double activationRecoverySeconds,
      double reserveRecoverySeconds) {}

  static final class Organism {
    final Profile profile;
    final double reactivity, baselineReserve;
    final VirtualPeopleSimulationTest.Person person;
    double activation, reserve, silentSeconds, longestSilence, peakActivation;
    double earlyActivation, lateActivation, earlyReserve, lateReserve;
    int earlyCount, lateCount, meaningfulActions;

    Organism(Profile profile, VirtualPeopleSimulationTest.Person person) {
      this.profile = profile;
      this.person = person;
      reactivity = .65 + .6 * person.risk;
      baselineReserve = .7 + .22 * person.energy;
      reserve = baselineReserve;
    }

    double apply(VirtualPeopleSimulationTest.RoundObservation round, int self, int index) {
      boolean actor = self == round.actor;
      boolean engaged = actor || round.voted[self] || round.inclusion[self] >= .5;
      double affinity = person.affinity[round.actor];
      // Homeostatic return happens over actual virtual elapsed time, not a fixed turn count.
      activation *= Math.exp(-round.seconds / profile.activationRecoverySeconds);
      reserve += (baselineReserve - reserve)
          * (1 - Math.exp(-round.seconds / profile.reserveRecoverySeconds));
      if (engaged) { silentSeconds = 0; meaningfulActions++; }
      else silentSeconds += round.seconds;
      longestSilence = Math.max(longestSilence, silentSeconds);

      // The event response is a design hypothesis, never a physiological calibration.
      double stimulus = actor ? .18 + .035 * (round.wager - 1) :
          round.inclusion[self] >= .5 ? .075 : round.voted[self] ? .045 : .015;
      if (actor && !round.won) stimulus += .035;
      if (round.expired) stimulus += actor ? .12 : .04 * affinity;
      if (round.repeated) stimulus += .018;
      if (!actor && round.sips > 0) stimulus += .01 * affinity;
      activation += reactivity * stimulus;
      peakActivation = Math.max(peakActivation, activation / reactivity);

      double effort = actor ? .048 + .008 * (round.wager - 1) :
          round.inclusion[self] >= .5 ? .024 : round.voted[self] ? .014 : .004;
      if (round.expired && actor) effort += .018;
      reserve = Math.max(0, reserve - effort);

      double relativeActivation = activation / reactivity;
      double relativeReserve = (baselineReserve - reserve) / baselineReserve;
      if (index < 8) {
        earlyActivation += relativeActivation; earlyReserve += relativeReserve; earlyCount++;
      } else if (index >= 16) {
        lateActivation += relativeActivation; lateReserve += relativeReserve; lateCount++;
      }
      return stimulus;
    }

    Trace trace() {
      assertEquals(8, earlyCount);
      assertEquals(8, lateCount);
      return new Trace(earlyActivation / earlyCount, lateActivation / lateCount,
          earlyReserve / earlyCount, lateReserve / lateCount, longestSilence,
          peakActivation, meaningfulActions);
    }
  }

  private record Trace(double earlyActivation, double lateActivation, double earlyReserve,
      double lateReserve, double longestSilence, double peakActivation, int actions) {
    double activationDelta() { return lateActivation - earlyActivation; }
    double reserveDelta() { return lateReserve - earlyReserve; }
  }

  private static final class Cohort {
    final List<Trace> traces = new ArrayList<>();
    int parties, rounds, timeouts, unbalancedParties, maxTurnGap;
    void add(Trace trace) { traces.add(trace); }
    String row(String label, String profile) {
      double[] activationDeltas = traces.stream().mapToDouble(Trace::activationDelta).sorted().toArray();
      double[] reserveDeltas = traces.stream().mapToDouble(Trace::reserveDelta).sorted().toArray();
      double[] silences = traces.stream().mapToDouble(Trace::longestSilence).sorted().toArray();
      double meanEarly = traces.stream().mapToDouble(Trace::earlyActivation).average().orElse(0);
      double meanLate = traces.stream().mapToDouble(Trace::lateActivation).average().orElse(0);
      double meanActions = traces.stream().mapToInt(Trace::actions).average().orElse(0);
      return String.format(Locale.ROOT,
          "ORG_COHORT,%s,%s,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.1f,%.1f,%.2f,%.1f,%d",
          label, profile, parties, rounds, traces.size(), meanEarly, meanLate,
          quantile(activationDeltas, .1), quantile(activationDeltas, .5),
          quantile(activationDeltas, .9), quantile(reserveDeltas, .5),
          quantile(reserveDeltas, .9), quantile(silences, .9),
          100.0 * timeouts / Math.max(1, rounds), meanActions,
          100.0 * unbalancedParties / Math.max(1, parties), maxTurnGap);
    }
  }

  private static double quantile(double[] sorted, double q) {
    if (sorted.length == 0) return 0;
    double index = q * (sorted.length - 1);
    int lower = (int) index, upper = Math.min(sorted.length - 1, lower + 1);
    return sorted[lower] + (sorted[upper] - sorted[lower]) * (index - lower);
  }

  @Test public void restReturnsTowardOwnBaselineWithoutInventingAHealthMetric() {
    VirtualPeopleSimulationTest.Person person = new VirtualPeopleSimulationTest.Person(
        0, 2, 1, new java.util.Random(7), 0);
    Organism organism = new Organism(PROFILES[1], person);
    VirtualPeopleSimulationTest.RoundObservation demanding =
        new VirtualPeopleSimulationTest.RoundObservation(0, 0, 3, 3, 12,
            false, true, false, new double[] {.85, .2}, new boolean[2]);
    organism.apply(demanding, 0, 0);
    double peak = organism.activation;
    double lowReserve = organism.reserve;
    VirtualPeopleSimulationTest.RoundObservation quiet =
        new VirtualPeopleSimulationTest.RoundObservation(0, 1, 1, 0, 120,
            true, false, false, new double[] {.2, .85}, new boolean[2]);
    organism.apply(quiet, 0, 1);
    assertTrue(organism.activation < peak);
    assertTrue(organism.reserve > lowReserve);
    assertTrue(organism.longestSilence >= 120);
    assertFalse(Double.isNaN(organism.activation));
  }

  @Test public void personalNormalizationSeparatesReactivityFromExposure() {
    VirtualPeopleSimulationTest.Person first = new VirtualPeopleSimulationTest.Person(
        0, 2, 0, new java.util.Random(3), 0);
    VirtualPeopleSimulationTest.Person second = new VirtualPeopleSimulationTest.Person(
        0, 2, 0, new java.util.Random(17), 0);
    Organism a = new Organism(PROFILES[1], first);
    Organism b = new Organism(PROFILES[1], second);
    VirtualPeopleSimulationTest.RoundObservation event =
        new VirtualPeopleSimulationTest.RoundObservation(3, 0, 2, 0, 25,
            true, false, false, new double[] {.85, .2}, new boolean[2]);
    a.apply(event, 0, 0);
    b.apply(event, 0, 0);
    assertEquals(a.activation / a.reactivity, b.activation / b.reactivity, 1e-12);
  }

  @Test public void actualRoundsHavePairedRelativeTrajectories() {
    int parties = Integer.parseInt(System.getenv().getOrDefault("APERO_ORGANISM_PARTIES", "600"));
    if (parties < 180 || parties > 100_000)
      throw new IllegalArgumentException("APERO_ORGANISM_PARTIES must be 180..100000");
    Cohort[][] topology = new Cohort[PROFILES.length][3];
    Cohort[][] mode = new Cohort[PROFILES.length][3];
    Cohort[][] size = new Cohort[PROFILES.length][5];
    for (int model = 0; model < PROFILES.length; model++) {
      for (int i = 0; i < 3; i++) { topology[model][i] = new Cohort(); mode[model][i] = new Cohort(); }
      for (int i = 0; i < 5; i++) size[model][i] = new Cohort();
    }
    int[] gameCount = new int[10];
    int[] gameTimeout = new int[10];
    double[] gameActorPulse = new double[10];
    double[] gameAudiencePulse = new double[10];
    int[] gameAudienceCount = new int[10];
    for (int id = 0; id < parties; id++) {
      VirtualPeopleSimulationTest.Party party =
          new VirtualPeopleSimulationTest.Party(id, new VirtualPeopleSimulationTest.Stats());
      int n = party.people.length;
      Organism[][] organisms = new Organism[PROFILES.length][n];
      for (int model = 0; model < PROFILES.length; model++)
        for (int p = 0; p < n; p++)
          organisms[model][p] = new Organism(PROFILES[model], party.people[p]);
      int[] turns = new int[n];
      for (int round = 0; round < ROUNDS; round++) {
        party.round();
        VirtualPeopleSimulationTest.RoundObservation observed = party.lastObservation;
        assertTrue(observed.seconds > 0);
        assertTrue(observed.game >= 0 && observed.game < 10);
        assertTrue(observed.actor >= 0 && observed.actor < n);
        turns[observed.actor]++;
        gameCount[observed.game]++;
        if (observed.expired) gameTimeout[observed.game]++;
        gameAudienceCount[observed.game] += n - 1;
        for (int model = 0; model < PROFILES.length; model++)
          for (int p = 0; p < n; p++) {
            double pulse = organisms[model][p].apply(observed, p, round);
            if (model == 0) {
              if (p == observed.actor) gameActorPulse[observed.game] += pulse;
              else gameAudiencePulse[observed.game] += pulse;
            }
          }
        for (int model = 0; model < PROFILES.length; model++) {
          Cohort t = topology[model][party.topology];
          Cohort m = mode[model][(id / 60) % 3];
          Cohort s = size[model][n - 2];
          t.rounds++; m.rounds++; s.rounds++;
          if (observed.expired) { t.timeouts++; m.timeouts++; s.timeouts++; }
        }
      }
      int min = Arrays.stream(turns).min().orElse(0), max = Arrays.stream(turns).max().orElse(0);
      assertTrue("every person gets a turn party=" + id, min >= 1);
      for (int model = 0; model < PROFILES.length; model++) {
        Cohort t = topology[model][party.topology];
        Cohort m = mode[model][(id / 60) % 3];
        Cohort s = size[model][n - 2];
        t.parties++; m.parties++; s.parties++;
        for (Cohort cohort : new Cohort[] {t, m, s}) {
          if (max - min > 1) cohort.unbalancedParties++;
          cohort.maxTurnGap = Math.max(cohort.maxTurnGap, max - min);
        }
        for (int p = 0; p < n; p++) {
          Trace trace = organisms[model][p].trace();
          assertTrue(Double.isFinite(trace.lateActivation));
          assertTrue(trace.peakActivation >= 0);
          assertTrue(trace.actions >= 1);
          t.add(trace); m.add(trace); s.add(trace);
        }
      }
    }
    for (int count : gameCount) assertTrue("all ten challenges covered", count > 0);
    System.out.println("ORG_META,parties," + parties + ",rounds," + (parties * ROUNDS)
        + ",engine_seed," + System.getenv().getOrDefault("APERO_PEOPLE_SEED", "20261005")
        + ",models,fast|middle|slow");
    System.out.println("ORG_COHORT,segment,model,parties,rounds,players,early_activation,late_activation,paired_activation_p10,paired_activation_p50,paired_activation_p90,paired_reserve_drop_p50,paired_reserve_drop_p90,longest_silence_p90_s,timeout_pct,engaged_rounds_per_person,turn_gap_gt1_pct,max_turn_gap");
    for (int model = 0; model < PROFILES.length; model++) {
      for (int i = 0; i < 3; i++) System.out.println(topology[model][i].row(TOPOLOGY[i], PROFILES[model].name));
      for (int i = 0; i < 3; i++) System.out.println(mode[model][i].row(MODE[i], PROFILES[model].name));
      for (int i = 0; i < 5; i++) System.out.println(size[model][i].row("players_" + (i + 2), PROFILES[model].name));
    }
    System.out.println("ORG_GAME,game,rounds,timeout_pct,actor_mean_pulse,audience_mean_pulse");
    for (int game = 0; game < 10; game++)
      System.out.println(String.format(Locale.ROOT, "ORG_GAME,%s,%d,%.1f,%.4f,%.4f",
          GameEngine.TYPES[game], gameCount[game],
          100.0 * gameTimeout[game] / gameCount[game],
          gameActorPulse[game] / gameCount[game],
          gameAudiencePulse[game] / gameAudienceCount[game]));
  }
}
