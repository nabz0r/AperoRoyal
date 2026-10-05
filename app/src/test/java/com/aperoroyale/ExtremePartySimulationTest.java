package com.aperoroyale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Random;
import java.util.function.LongSupplier;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Reproducible, virtual-clock stress test. The engine makes every decision and enforces its real
 * deadlines. Human pace, attention, conversation and transport faults remain explicit hypotheses.
 * Set APERO_SIM_PARTIES=100000 for the extended run; CI uses a smaller stratified sample.
 */
public final class ExtremePartySimulationTest {
  private static final String[] MODES = {"VOTE", "FREE", "TURBO"};
  private static final String[] SCENARIOS = {"quiet", "conversation", "interrupted", "disrupted"};
  private static final double[] CORE = {8, 17, 12, 10, 4, 23, 13, 10, 17, 0};
  private static final int ROUNDS = 18;
  private static final long SEED = Long.parseLong(System.getenv().getOrDefault(
      "APERO_SIM_SEED", "11674260475909"));

  private static final class Clock implements LongSupplier {
    long now = 1_800_000_000_000L;
    @Override public long getAsLong() { return now; }
    void add(double seconds) { now += Math.max(1, Math.round(seconds * 1000)); }
    void until(long instant) { now = Math.max(now, instant); }
  }

  private static final class Person {
    final double pace, skill, risk;
    final int favorite;
    int ignored;
    Person(Random rng, double groupPace) {
      pace = Math.max(.55, Math.min(2.4, Math.exp(rng.nextGaussian() * .33) * groupPace));
      skill = rng.nextDouble();
      risk = rng.nextDouble();
      favorite = rng.nextInt(10);
    }
  }

  private static final class Event {
    final int person;
    final long at;
    final long handoff;
    final boolean absent;
    Event(int person, long at, long handoff, boolean absent) {
      this.person = person; this.at = at; this.handoff = handoff; this.absent = absent;
    }
  }

  private static final class Stats {
    long parties, rounds, timeout, voteFallback, crewPartial, sharedSkips;
    long attempts, accepted, late, noResponse, wins, sips, gameRepeated, restores;
    long repeatVariant, repeatConcept;
    long seconds, adminSeconds, touchGapSeconds, actorImbalance;
    final long[] roundHistogram = new long[601], gapHistogram = new long[601];
    final long[] gameRounds = new long[10], gameTimeouts = new long[10], gameWins = new long[10];
    final long[] crewAnswered = new long[10], crewPossible = new long[10];
    void round(int game, long millis, long adminMillis, long gapMillis, boolean expired,
        boolean won, int sipsThisRound, boolean repeated) {
      rounds++;
      gameRounds[game]++;
      if (expired) { timeout++; gameTimeouts[game]++; }
      if (won) { wins++; gameWins[game]++; }
      sips += sipsThisRound;
      if (repeated) gameRepeated++;
      seconds += millis;
      adminSeconds += adminMillis;
      touchGapSeconds += gapMillis;
      roundHistogram[(int) Math.min(600, Math.max(0, Math.ceil(millis / 1000.0)))]++;
      gapHistogram[(int) Math.min(600, Math.max(0, Math.ceil(gapMillis / 1000.0)))]++;
    }
    int percentile(double fraction, long[] histogram) {
      long target = (long) Math.ceil(rounds * fraction), count = 0;
      for (int i = 0; i < histogram.length; i++) {
        count += histogram[i];
        if (count >= target) return i;
      }
      return 600;
    }
    String line(String label) {
      return String.format(Locale.ROOT,
          "%s,%d,%d,%.1f,%d,%d,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.2f,%d,%d,%d",
          label, parties, rounds, seconds / (double) rounds / 1000,
          percentile(.90, roundHistogram),
          percentile(.99, roundHistogram), percentile(.90, gapHistogram),
          100.0 * adminSeconds / Math.max(1, seconds),
          100.0 * timeout / rounds, 100.0 * voteFallback / rounds,
          100.0 * crewPartial / rounds, 100.0 * accepted / Math.max(1, attempts),
          100.0 * gameRepeated / rounds, 100.0 * repeatVariant / rounds,
          100.0 * repeatConcept / rounds, actorImbalance / (double) parties,
          late, noResponse, sharedSkips);
    }
  }

  private static final class Session {
    final Clock clock = new Clock();
    final Random rng;
    GameEngine room;
    final Person[] people;
    final int topology, scenario;
    final Stats stats;
    final int id;
    final int[] actorTurns;
    final long[] lastTouch;
    final boolean[][] seenVariant = new boolean[10][], seenConcept = new boolean[10][];
    long worstGap, admin;
    int previousGame = -1;
    boolean blackout;
    boolean gameExpired;
    Session(int id, Stats stats) {
      this.id = id;
      rng = new Random(SEED + id * 0x9E3779B97F4A7C15L);
      topology = (id / 5) % 3; // One phone, two phones, one phone per player.
      scenario = (id / 45) % 4;
      int count = 2 + id % 5;
      people = new Person[count];
      actorTurns = new int[count];
      lastTouch = new long[count];
      double groupPace = .8 + rng.nextDouble() * .65;
      for (int p = 0; p < count; p++) people[p] = new Person(rng, groupPace);
      for (int game = 0; game < 10; game++) {
        seenVariant[game] = new boolean[variantCount(game)];
        seenConcept[game] = new boolean[game == 2 ? GameEngine.TUNES.length : variantCount(game)];
      }
      room = new GameEngine(SEED ^ id, clock);
      room.newParty();
      for (int p = 0; p < count; p++)
        assertTrue(room.addPlayer("P" + p, p % 2 == 0 ? "FR" : "EN", p));
      room.mode = MODES[(id / 15) % 3];
      room.begin();
      this.stats = stats;
      stats.parties++;
    }

    double time(int person, double base) {
      double social = scenario == 0 ? 1 : scenario == 1 ? 1.35 : scenario == 2 ? 1.55 : 1.15;
      double value = base * people[person].pace * social * Math.exp(rng.nextGaussian() * .36);
      if (scenario >= 1 && rng.nextDouble() < (scenario == 2 ? .055 : .018))
        value += 7 + rng.nextInt(24); // Conversation, distraction or interruption burst.
      return value;
    }

    double network() {
      if (topology == 0) return 0;
      double base = scenario == 3 ? .9 : topology == 2 ? .18 : .3;
      double delay = base * Math.exp(rng.nextGaussian() * .65);
      if (scenario == 3 && rng.nextDouble() < .08) delay += 3 + rng.nextInt(13);
      if (blackout) delay += 7 + rng.nextInt(16);
      return delay;
    }

    boolean absent(int person) {
      double chance = scenario == 0 ? .006 : scenario == 1 ? .015 : scenario == 2 ? .045 : .025;
      if (topology > 0 && scenario == 3) chance += .035;
      chance += Math.min(.08, people[person].ignored * .02);
      boolean missed = rng.nextDouble() < chance;
      people[person].ignored = missed ? Math.min(4, people[person].ignored + 1) : 0;
      return missed;
    }

    void touch(int person) {
      worstGap = Math.max(worstGap, clock.now - lastTouch[person]);
      lastTouch[person] = clock.now;
    }

    void waitSeconds(double seconds) { clock.add(seconds); }

    void timeoutGame() {
      clock.until(room.deadline + 1);
      room.checkTimeout();
      gameExpired = true;
    }

    boolean open(int stage) {
      return switch (stage) {
        case 0 -> "VOTE".equals(room.screen);
        case 1 -> "CREW".equals(room.screen);
        case 2 -> "GAME".equals(room.screen) && room.juryPhase;
        default -> "GAME".equals(room.screen) && room.drawingReady;
      };
    }

    void expire(int stage) {
      if (!open(stage)) return;
      clock.until(room.deadline + 1);
      if (stage == 0) {
        if (room.voteTimedOut(clock.now)) stats.voteFallback++;
      } else if (stage == 1) room.crewTimedOut();
      else timeoutGame();
    }

    void answer(int stage, int person) {
      boolean ok = switch (stage) {
        case 0 -> room.castVote("P" + person, vote(person));
        case 1 -> room.crewPick("P" + person, rng.nextInt(room.crewOptionCount()));
        case 2 -> room.castJury("P" + person, rng.nextDouble() < .55);
        default -> room.drawGuess("P" + person,
            rng.nextDouble() < .25 + people[person].skill * .45 ? room.target : rng.nextInt(4));
      };
      if (ok) { stats.accepted++; touch(person); }
      else stats.late++;
      if (stage == 2 && room.juryComplete()) room.finish(room.juryVerdict());
      if (stage == 3 && room.drawGuessComplete()) room.finish(room.drawWin());
    }

    int vote(int person) {
      int best = rng.nextInt(3);
      for (int i = 0; i < room.offers.length; i++)
        if (room.offers[i] == people[person].favorite && rng.nextDouble() < .75) best = i;
      return best;
    }

    void responses(int stage, int actor) {
      int count = stage == 0 ? people.length : people.length - 1;
      long stageStart = clock.now;
      ArrayList<Event> events = new ArrayList<>();
      long[] deviceAvailable = new long[Math.max(1, topology == 0 ? 1 : topology == 1 ? 2 : people.length)];
      for (int step = 0; step < count; step++) {
        int person = (actor + step + (stage == 0 ? 0 : 1)) % people.length;
        stats.attempts++;
        int device = topology == 0 ? 0 : topology == 1 ? person % 2 : person;
        double handoff = topology == 0 && stage != 0 && step == 0
            ? 1.3 + rng.nextDouble() * 3.0
            : deviceAvailable[device] == 0 ? 0 : 1.3 + rng.nextDouble() * 3.0;
        long from = Math.max(stageStart, deviceAvailable[device]);
        boolean missing = absent(person);
        long ready = from + Math.round((handoff + (missing && topology == 0 ? 20
            : time(person, stage == 2 ? 3.5 : 2.8)) + network()) * 1000);
        deviceAvailable[device] = ready;
        if (missing) {
          stats.noResponse++;
          if (topology == 0) stats.sharedSkips++;
        }
        if (!missing || topology == 0)
          events.add(new Event(person, ready, Math.round(handoff * 1000), missing));
      }
      events.sort(Comparator.comparingLong(e -> e.at));
      // On a shared phone, the handoff overlay pauses the host deadline. Its physical time still
      // counts in the room timeline. A failure to confirm the overlay is an unresolved UX risk.
      for (Event event : events) {
        if (!open(stage)) { stats.late++; continue; }
        if (topology == 0) room.deadline += event.handoff + (event.absent ? 20000 : 0);
        if (event.at >= room.deadline) {
          expire(stage);
          stats.late++;
          continue;
        }
        clock.until(event.at);
        if (event.absent) {
          assertTrue(room.skipParticipation("P" + event.person));
        } else answer(stage, event.person);
      }
      if (open(stage)) expire(stage);
      if (stage == 1 && room.crewCount() < people.length - 1) stats.crewPartial++;
    }

    void playGame(int game, int actor) {
      if (game == 9) {
        while (room.taps < room.bombGoal() && clock.now < room.deadline) {
          if (room.bombAwaitingPass) {
            int target = -1;
            for (int i = 1; i < people.length; i++) {
              int candidate = (room.bombNext + i) % people.length;
              if (room.canPassBombTo(candidate)) { target = candidate; break; }
            }
            assertTrue(target >= 0);
            double handoff = 1.2 + rng.nextDouble() * 2.8;
            waitSeconds(handoff + network());
            if (topology == 0) {
              long paused = Math.round(handoff * 1000);
              room.deadline += paused;
              room.started += paused;
            }
            if (clock.now >= room.deadline) break;
            assertTrue(room.bombPass("P" + room.bombNext, target));
          } else {
            int holder = room.bombNext;
            waitSeconds(time(holder, .8) + network());
            if (clock.now >= room.deadline) break;
            assertTrue(room.bombTap("P" + holder));
            touch(holder);
          }
        }
        if (clock.now >= room.deadline) timeoutGame();
        else room.finish(true);
      } else if (game == 7) {
        int[] pattern = room.rhythmPattern();
        for (int i = 0; i < pattern.length; i++) {
          int beat = pattern[i];
          long at = room.started + beat * 600L + 300 + Math.round(rng.nextGaussian() *
              (scenario == 3 ? 170 : 95)) + Math.round(network() * 1000);
          clock.until(at);
          if (clock.now >= room.deadline) break;
          if (room.rhythmTap("P" + actor, beat, topology != 0)) touch(actor);
        }
        if (clock.now >= room.deadline) timeoutGame();
        else room.finish(room.rhythmHits >= 4);
      } else if (game == 3) {
        for (int i = 0; i < room.reflexGoal(); i++) {
          waitSeconds(time(actor, .65) + network());
          if (clock.now >= room.deadline) break;
          if (rng.nextDouble() > .04 + (1 - people[actor].skill) * .08
              && room.reflexTap("P" + actor, room.taps)) touch(actor);
        }
        if (clock.now >= room.deadline) timeoutGame();
        else room.finish(room.taps >= room.reflexGoal());
      } else {
        double duration = time(actor, CORE[game]);
        if (game == 1 || game == 5 || game == 8) duration *= 1.1;
        waitSeconds(duration);
        if (clock.now >= room.deadline) { timeoutGame(); return; }
        touch(actor);
        if (game == 1 || game == 8) {
          if (game == 1) assertTrue(room.beginJury());
          else assertTrue(room.chooseBluffTruth("P" + actor, rng.nextBoolean()));
          responses(2, actor);
        } else if (game == 5) {
          assertTrue(room.beginDrawGuess());
          responses(3, actor);
        } else if (game == 4) {
          assertTrue(room.selectCup(rng.nextInt(6)));
          clock.until(room.revealUntil);
          room.finish(room.cupIsSafe());
        } else {
          double success = .28 + people[actor].skill * .53;
          if (room.crewLead() >= 0 && (game == 0 || game == 2)) success += .08;
          room.finish(rng.nextDouble() < success);
        }
      }
    }

    void round(int index) throws Exception {
      int actor = room.active;
      actorTurns[actor]++;
      java.util.Arrays.fill(lastTouch, clock.now);
      worstGap = admin = 0;
      gameExpired = false;
      long start = clock.now;
      blackout = scenario == 3 && rng.nextDouble() < .025;
      if (!room.ruleOwner.isEmpty() && room.ruleId < 0) {
        if (rng.nextDouble() < .08) {
          clock.until(room.deadline + 1);
          if ("RULE_PICK".equals(room.screen)) assertTrue(room.rulePickTimedOut(clock.now));
          else if ("VOTE".equals(room.screen)) assertTrue(room.voteTimedOut(clock.now));
        } else {
          waitSeconds(time(room.indexOf(room.ruleOwner), 3));
          if (room.ruleId < 0) assertTrue(room.chooseRule(room.ruleOwner, room.ruleOffers[0]));
        }
      }
      if ("VOTE".equals(room.screen)) responses(0, actor);
      else if ("LIBRARY".equals(room.screen)) {
        waitSeconds(time(actor, 3.5));
        int pick = rng.nextDouble() < .65 ? people[actor].favorite : rng.nextInt(10);
        room.startNext(pick);
        touch(actor);
      }
      assertEquals("TRANSITION", room.screen);
      long afterSelection = clock.now;
      int game = room.game;
      if (seenVariant[game][room.variant]) stats.repeatVariant++;
      seenVariant[game][room.variant] = true;
      int concept = game == 2 ? room.variant % GameEngine.TUNES.length : room.variant;
      if (seenConcept[game][concept]) stats.repeatConcept++;
      seenConcept[game][concept] = true;
      waitSeconds(1.5 + rng.nextDouble() * 2.0);
      room.enterGame();
      waitSeconds(topology == 2 ? 1.0 : 2.0 + rng.nextDouble() * 3.0);
      room.readyTurn();
      waitSeconds(time(actor, 2.5));
      assertTrue(room.placeBet(people[actor].risk > .72 ? 3 : people[actor].risk > .35 ? 2 : 1));
      touch(actor);
      admin = clock.now - start;
      if ("CREW".equals(room.screen)) {
        responses(1, actor);
        stats.crewAnswered[game] += room.crewCount();
        stats.crewPossible[game] += people.length - 1;
      }
      assertEquals("GAME", room.screen);
      playGame(game, actor);
      assertEquals("party=" + id + " round=" + index + " game=" + game
          + " now=" + clock.now + " deadline=" + room.deadline,
          "RESULT", room.screen);
      boolean won = room.lastWon;
      int sips = room.roundSips;
      if (topology > 0 && rng.nextDouble() < .04) {
        int viewer = (actor + 1) % people.length;
        if (room.hiddenWaiting("P" + viewer) && room.hiddenWonMask[viewer] != 7
            && room.hiddenSolvedTurn[viewer] != room.turn) {
          for (int i = 0; i < 3; i++) room.hiddenTap("P" + viewer, -1);
          if (room.hiddenKind[viewer] >= 0)
            for (int i = 0; i < 4; i++) room.hiddenTap("P" + viewer, room.hiddenTarget(viewer));
        }
      }
      double resultPause = 3 + rng.nextDouble() * 5;
      if (scenario == 1 && rng.nextDouble() < .12) resultPause += 12 + rng.nextInt(20);
      waitSeconds(resultPause);
      admin += clock.now - Math.max(afterSelection, room.started);
      for (long touched : lastTouch) worstGap = Math.max(worstGap, clock.now - touched);
      stats.round(game, clock.now - start, admin, worstGap, gameExpired, won, sips,
          previousGame == game);
      previousGame = game;
      room.advance();
      if (index == 8 && rng.nextInt(25) == 0) {
        GameEngine recovered = new GameEngine(SEED ^ index, clock);
        recovered.restore(new JSONObject(room.json().toString()));
        assertEquals(room.turn, recovered.turn);
        assertEquals(room.screen, recovered.screen);
        room = recovered;
        stats.restores++;
      }
    }
  }

  @Test public void virtualClockStressAcrossPartyTopologies() throws Exception {
    String requested = System.getenv("APERO_SIM_PARTIES");
    int parties = requested == null ? 3600 : Integer.parseInt(requested);
    if (parties < 180 || parties > 100_000) throw new IllegalArgumentException("parties 180..100000");
    Stats[][][][] data = new Stats[5][3][3][4];
    for (int p = 0; p < 5; p++) for (int t = 0; t < 3; t++)
      for (int m = 0; m < 3; m++) for (int s = 0; s < 4; s++)
        data[p][t][m][s] = new Stats();
    for (int id = 0; id < parties; id++) {
      int p = id % 5, t = (id / 5) % 3, m = (id / 15) % 3, s = (id / 45) % 4;
      Session session = new Session(id, data[p][t][m][s]);
      for (int round = 0; round < ROUNDS; round++) session.round(round);
      int min = Integer.MAX_VALUE, max = 0;
      for (int turns : session.actorTurns) { min = Math.min(min, turns); max = Math.max(max, turns); }
      data[p][t][m][s].actorImbalance += max - min;
    }
    System.out.println("EXTREME SIM: seed=" + SEED + " parties=" + parties + " rounds=" +
        (parties * ROUNDS) + " modeled inputs; production engine and deadlines");
    System.out.println("players/topology/mode/scenario,parties,rounds,mean_s,p90_s,p99_s,p90_no_touch_s,admin_pct,timeout_pct,vote_fallback_per_round_pct,crew_partial_per_round_pct,accepted_action_pct,repeat_game_pct,repeat_variant_pct,repeat_concept_pct,mean_actor_turn_gap,late_actions,no_response,shared_manual_skips");
    long totalRounds = 0;
    for (int p = 0; p < 5; p++) for (int t = 0; t < 3; t++)
      for (int m = 0; m < 3; m++) for (int s = 0; s < 4; s++) {
      Stats st = data[p][t][m][s];
      if (st.rounds == 0) continue;
      totalRounds += st.rounds;
      System.out.println(st.line((p + 2) + "/"
          + (t == 0 ? "one" : t == 1 ? "two" : "individual")
          + "/" + MODES[m] + "/" + SCENARIOS[s]));
    }
    assertEquals((long) parties * ROUNDS, totalRounds);
    Stats[] byGame = new Stats[10];
    for (int i = 0; i < 10; i++) byGame[i] = new Stats();
    for (Stats[][][] topologies : data) for (Stats[][] modes : topologies)
      for (Stats[] scenarios : modes) for (Stats st : scenarios)
      for (int i = 0; i < 10; i++) {
        byGame[i].gameRounds[i] += st.gameRounds[i];
        byGame[i].gameTimeouts[i] += st.gameTimeouts[i];
        byGame[i].gameWins[i] += st.gameWins[i];
        byGame[i].crewAnswered[i] += st.crewAnswered[i];
        byGame[i].crewPossible[i] += st.crewPossible[i];
      }
    System.out.println("game,rounds,modeled_win_pct,timeout_pct,crew_response_pct");
    for (int i = 0; i < 10; i++) {
      Stats st = byGame[i];
      assertTrue(GameEngine.TYPES[i], st.gameRounds[i] > 0);
      System.out.printf(Locale.ROOT, "%s,%d,%.1f,%.1f,%s%n", GameEngine.TYPES[i],
          st.gameRounds[i], 100.0 * st.gameWins[i] / st.gameRounds[i],
          100.0 * st.gameTimeouts[i] / st.gameRounds[i],
          st.crewPossible[i] == 0 ? "NA" : String.format(Locale.ROOT, "%.1f",
              100.0 * st.crewAnswered[i] / st.crewPossible[i]));
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
}
