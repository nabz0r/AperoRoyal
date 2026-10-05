package com.aperoroyale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import org.junit.Test;

/**
 * Synthetic experience audit. Game selection and round transitions use the real engine; human
 * timings and network delays are deliberately varied assumptions, never observed play time.
 */
public final class ExperienceRiskLabTest {
  private static final String[] MODES = {"VOTE", "FREE", "TURBO"};
  private static final double[] CORE_SECONDS = {9, 15, 12, 11, 4, 24, 13, 10, 16, 10};
  private static final int PARTIES = 3000;
  private static final int ROUNDS = 12;

  private static final class Slice {
    int parties, rounds, crewOnly, repeatGame, repeatVariant, repeatConcept, over60;
    double elapsed, administration, longestNoInput;
    final ArrayList<Double> times = new ArrayList<>();
    final ArrayList<Double> noInput = new ArrayList<>();

    void add(Timeline line, boolean crewOnlyRound, boolean sameGame,
        boolean sameVariant, boolean sameConcept) {
      rounds++;
      if (crewOnlyRound) crewOnly++;
      if (sameGame) repeatGame++;
      if (sameVariant) repeatVariant++;
      if (sameConcept) repeatConcept++;
      if (line.time > 60) over60++;
      elapsed += line.time;
      administration += line.administration;
      longestNoInput = Math.max(longestNoInput, line.longestNoInput);
      times.add(line.time);
      noInput.add(line.longestNoInput);
    }
  }

  private static final class Timeline {
    final Random rng;
    final boolean shared;
    final double[] speed, lastInput;
    final double networkDelay;
    double time, administration, longestNoInput;

    Timeline(Random rng, double[] partySpeed, boolean shared, int networkScenario) {
      this.rng = rng;
      this.shared = shared;
      speed = partySpeed;
      lastInput = new double[partySpeed.length];
      networkDelay = shared ? 0 : new double[] {.12, .65, 2.0}[networkScenario];
    }

    void delay(double base, boolean administrative) {
      double duration = base * (.80 + rng.nextDouble() * .40);
      time += duration;
      if (administrative) administration += duration;
    }

    void actions(int[] people, double base, boolean administrative) {
      if (people.length == 0) return;
      if (shared) {
        for (int i = 0; i < people.length; i++) {
          if (i > 0) delay(2.0, true); // Physical handoff and private-screen confirmation.
          double duration = duration(people[i], base);
          time += duration;
          if (administrative) administration += duration;
          touch(people[i], time);
        }
      } else {
        double start = time, end = start;
        for (int person : people) {
          double completion = start + duration(person, base) + networkDelay;
          touch(person, completion);
          end = Math.max(end, completion);
        }
        time = end;
        if (administrative) administration += end - start;
      }
    }

    private double duration(int person, double base) {
      return base * speed[person] * (.72 + rng.nextDouble() * .56);
    }

    private void touch(int person, double when) {
      longestNoInput = Math.max(longestNoInput, when - lastInput[person]);
      lastInput[person] = when;
    }

    void finish() {
      for (double last : lastInput) longestNoInput = Math.max(longestNoInput, time - last);
    }
  }

  @Test public void threeThousandMixedPartiesExposePacingAndParticipationRisk() {
    Slice[][][] cohorts = new Slice[5][2][3];
    Slice[] games = new Slice[10];
    Slice[] networkScenarios = {new Slice(), new Slice(), new Slice()};
    for (int n = 0; n < 5; n++) for (int device = 0; device < 2; device++)
      for (int mode = 0; mode < 3; mode++) cohorts[n][device][mode] = new Slice();
    for (int i = 0; i < games.length; i++) games[i] = new Slice();

    for (int party = 0; party < PARTIES; party++) {
      int people = 2 + party % 5;
      boolean shared = (party / 5) % 2 == 0;
      int mode = (party / 10) % 3;
      int networkScenario = (party / 30) % 3;
      Slice cohort = cohorts[people - 2][shared ? 0 : 1][mode];
      cohort.parties++;
      Random rng = new Random(0xA9E20L + party);
      GameEngine room = new GameEngine(0xB9E20L + party);
      room.newParty();
      for (int person = 0; person < people; person++)
        assertTrue(room.addPlayer("P" + person, person % 2 == 0 ? "FR" : "EN", person));
      room.mode = MODES[mode];
      room.begin();
      // A player's pace persists across the party; only action-level jitter changes by round.
      double[] personas = {.70, .90, 1.10, 1.45, 1.90};
      double roomPace = .80 + rng.nextDouble() * .65;
      double[] partySpeed = new double[people];
      for (int person = 0; person < people; person++)
        partySpeed[person] = personas[rng.nextInt(personas.length)] * roomPace;
      int favorite = rng.nextInt(10);
      int previousGame = -1;
      boolean[][] seen = new boolean[10][];
      boolean[][] seenConcept = new boolean[10][];
      for (int game = 0; game < 10; game++) seen[game] = new boolean[variantCount(game)];
      for (int game = 0; game < 10; game++)
        seenConcept[game] = new boolean[game == 2 ? GameEngine.TUNES.length : variantCount(game)];

      for (int round = 0; round < ROUNDS; round++) {
        if (!room.ruleOwner.isEmpty() && room.ruleId < 0)
          assertTrue(room.chooseRule(room.ruleOwner, room.ruleOffers[0]));
        Timeline line = new Timeline(rng, partySpeed, shared, networkScenario);
        int actor = room.active;
        if (mode == 0) {
          assertEquals("VOTE", room.screen);
          int[] voters = order(people, actor, false);
          line.actions(voters, 3.2, true);
          for (int person : voters) assertTrue(room.castVote("P" + person, rng.nextInt(3)));
        } else if (mode == 1) {
          assertEquals("LIBRARY", room.screen);
          line.actions(new int[] {actor}, 4.0, true);
          room.startNext(rng.nextInt(10) < 6 ? favorite : rng.nextInt(10));
        }
        assertEquals("TRANSITION", room.screen);
        int game = room.game;
        boolean repeatedVariant = seen[game][room.variant];
        seen[game][room.variant] = true;
        int concept = game == 2 ? room.variant % GameEngine.TUNES.length : room.variant;
        boolean repeatedConcept = seenConcept[game][concept];
        seenConcept[game][concept] = true;
        line.delay(2.0, true);
        if (shared) line.delay(3.2, true);
        room.enterGame();
        room.readyTurn();
        line.actions(new int[] {actor}, 3.0, true); // Sip wager.
        assertTrue(room.placeBet(1 + rng.nextInt(3)));

        boolean crewOnly = game == 0 || game == 2 || game == 3 || game == 4
            || game == 6 || game == 7;
        int[] peers = order(people, actor, true);
        if (crewOnly) {
          line.actions(peers, 3.0, false);
          for (int person : peers)
            assertTrue(room.crewPick("P" + person, rng.nextInt(room.crewOptionCount())));
        }
        assertEquals("GAME", room.screen);
        boolean won = rng.nextBoolean(); // Only drives engine progression; no fun inference.
        if (game == 9) {
          int taps = 0;
          while (room.taps < room.bombGoal()) {
            if (room.bombAwaitingPass) {
              int target = -1;
              for (int person = 0; person < people; person++) if (room.canPassBombTo(person)) {
                target = person;
                break;
              }
              assertTrue(target >= 0);
              assertTrue(room.bombPass(room.players.get(room.bombNext).name, target));
            } else {
              int holder = room.bombNext;
              assertTrue(room.bombTap("P" + holder));
              line.actions(new int[] {holder}, 1.3, false);
              taps++;
              if (shared && room.bombAwaitingPass) line.delay(2.0, false);
            }
          }
          assertTrue(taps > 0);
          won = true;
        } else {
          line.actions(new int[] {actor}, CORE_SECONDS[game], false);
          if (game == 1 || game == 8) {
            if (game == 1) assertTrue(room.beginJury());
            else assertTrue(room.chooseBluffTruth("P" + actor, rng.nextBoolean()));
            line.actions(peers, 4.0, false);
            for (int person : peers) assertTrue(room.castJury("P" + person, rng.nextBoolean()));
          } else if (game == 5) {
            assertTrue(room.beginDrawGuess());
            line.actions(peers, 4.0, false);
            for (int person : peers) assertTrue(room.drawGuess("P" + person, rng.nextInt(4)));
            won = room.drawWin();
          } else if (game == 4) {
            assertTrue(room.selectCup(rng.nextInt(6)));
            won = room.cupIsSafe();
          }
        }
        line.delay(5.0, true); // Result and next-round acknowledgement.
        // One in 40 rounds gets an exogenous pause; this is a stress assumption.
        if (rng.nextInt(40) == 0) line.delay(5 + rng.nextInt(16), true);
        line.finish();
        cohort.add(line, crewOnly, previousGame == game, repeatedVariant, repeatedConcept);
        games[game].add(line, crewOnly, previousGame == game, repeatedVariant, repeatedConcept);
        if (!shared) networkScenarios[networkScenario].add(line, crewOnly,
            previousGame == game, repeatedVariant, repeatedConcept);
        previousGame = game;
        room.finish(won);
        room.advance();
      }
    }

    System.out.println("EXPERIENCE RISK LAB: 3,000 rooms / 36,000 rounds; hypothetical timing/personas, real engine rotation");
    System.out.println("players,devices,mode,rounds,mean_s,p90_s,admin_pct,p90_max_no_input_s,over_60_pct,crew_only_pct,repeat_variant_pct,repeat_concept_pct");
    for (int people = 2; people <= 6; people++) for (int device = 0; device < 2; device++)
      for (int mode = 0; mode < 3; mode++) {
        Slice c = cohorts[people - 2][device][mode];
        assertTrue(c.rounds > 0);
        System.out.printf(Locale.ROOT, "%d,%s,%s,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f%n",
            people, device == 0 ? "shared" : "individual", MODES[mode], c.rounds,
            c.elapsed / c.rounds, percentile(c.times, .90), 100 * c.administration / c.elapsed,
            percentile(c.noInput, .90), 100.0 * c.over60 / c.rounds,
            100.0 * c.crewOnly / c.rounds, 100.0 * c.repeatVariant / c.rounds,
            100.0 * c.repeatConcept / c.rounds);
      }
    System.out.println("game,rounds,mean_s,p90_max_no_input_s,admin_pct,repeat_variant_pct,repeat_concept_pct,crew_only_pct");
    for (int game = 0; game < games.length; game++) {
      Slice c = games[game];
      assertTrue("unseen game: " + game, c.rounds > 0);
      System.out.printf(Locale.ROOT, "%s,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f%n",
          GameEngine.TYPES[game], c.rounds, c.elapsed / c.rounds,
          percentile(c.noInput, .90), 100 * c.administration / c.elapsed,
          100.0 * c.repeatVariant / c.rounds, 100.0 * c.repeatConcept / c.rounds,
          100.0 * c.crewOnly / c.rounds);
    }
    System.out.println("network_delay_assumption_s,rounds,mean_s,p90_s,p90_max_no_input_s");
    double[] delays = {.12, .65, 2.0};
    for (int scenario = 0; scenario < delays.length; scenario++) {
      Slice c = networkScenarios[scenario];
      System.out.printf(Locale.ROOT, "%.2f,%d,%.1f,%.1f,%.1f%n", delays[scenario],
          c.rounds, c.elapsed / c.rounds, percentile(c.times, .90),
          percentile(c.noInput, .90));
    }
    int total = 0;
    for (Slice[][] byDevice : cohorts) for (Slice[] byMode : byDevice)
      for (Slice c : byMode) total += c.rounds;
    assertEquals(PARTIES * ROUNDS, total);
  }

  @Test public void missingVoteStressAlwaysResolvesWithoutInventingPlayerChoices() {
    System.out.println("VOTE DROPOUT STRESS: independent missed-vote probabilities are hypotheses, not observed rates");
    System.out.println("players,miss_probability,rooms,auto_resolution_pct,mean_abstentions_per_room");
    for (int people = 2; people <= 6; people++) for (double miss : new double[] {.02, .10, .30}) {
      int auto = 0, abstentions = 0;
      Random rng = new Random(0xC9E20L + people * 100 + (int) (miss * 100));
      for (int trial = 0; trial < 1000; trial++) {
        GameEngine room = new GameEngine(0xD9E20L + people * 1000L + trial);
        for (int p = 0; p < people; p++) assertTrue(room.addPlayer("P" + p, p % 2 == 0 ? "FR" : "EN", p));
        room.mode = "VOTE";
        room.begin();
        for (int p = 0; p < people; p++)
          if (rng.nextDouble() >= miss) assertTrue(room.castVote("P" + p, rng.nextInt(3)));
        if ("VOTE".equals(room.screen)) {
          auto++;
          assertTrue(room.voteTimedOut(room.deadline + 1));
        }
        assertEquals("TRANSITION", room.screen);
        for (int vote : room.votes) if (vote == 3) abstentions++;
      }
      System.out.printf(Locale.ROOT, "%d,%.2f,1000,%.1f,%.2f%n", people, miss,
          auto / 10.0, abstentions / 1000.0);
    }
  }

  private static int variantCount(int game) {
    return switch (game) {
      case 0 -> GameEngine.QUIZ_FR.length;
      case 1 -> GameEngine.POSES.length;
      case 2 -> GameEngine.TUNES.length * 4;
      case 5 -> GameEngine.DRAW.length;
      case 8 -> GameEngine.BLUFF.length;
      default -> 6;
    };
  }

  private static int[] order(int people, int actor, boolean peersOnly) {
    int[] result = new int[people - (peersOnly ? 1 : 0)];
    for (int i = 0; i < result.length; i++) result[i] = (actor + i + (peersOnly ? 1 : 0)) % people;
    return result;
  }

  private static double percentile(ArrayList<Double> values, double fraction) {
    ArrayList<Double> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    return sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(fraction * sorted.size()) - 1));
  }
}
