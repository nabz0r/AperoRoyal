package com.aperoroyale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Random;
import java.util.function.LongSupplier;
import org.junit.Test;

/** Stateful, explicitly hypothetical people playing the production engine. No human data is fitted. */
public final class VirtualPeopleSimulationTest {
  private static final int ROUNDS = 24;
  private static final long SEED = Long.parseLong(System.getenv().getOrDefault(
      "APERO_PEOPLE_SEED", "20261005"));
  private static final boolean OLD_TIMERS = "1".equals(System.getenv("APERO_PEOPLE_OLD_TIMERS"));
  private static final String[] MODES = {"VOTE", "FREE", "TURBO"};
  private static final String[] GROUPS = {"friends", "mixed", "competitive", "distracted"};
  private static final double[] GAME_SECONDS = {7, 16, 11, 0, 3, 21, 12, 0, 19, 0};

  private static final class Clock implements LongSupplier {
    long now = 1_800_000_000_000L;
    @Override public long getAsLong() { return now; }
    void add(double seconds) { now += Math.max(1, Math.round(seconds * 1000)); }
    void reach(long time) { now = Math.max(now, time); }
  }

  /** Taste, memory, relationship and energy affect later decisions. Trait values are design inputs. */
  static final class Person {
    final String name;
    final int style;
    final double[] taste = new double[10], skill = new double[10], boredom = new double[10];
    final double[] affinity;
    final double risk, patience, curiosity, cooperation, pace;
    double confidence = .5, energy = .85, mood = .55;
    int wins, losses, ignored, choices, votes, crewActs, juryActs, guesses;
    Person(int id, int size, int style, Random random, int group) {
      name = "P" + id;
      this.style = style;
      affinity = new double[size];
      risk = .12 + .76 * random.nextDouble();
      patience = .15 + .75 * random.nextDouble();
      curiosity = .15 + .8 * random.nextDouble();
      pace = Math.max(.65, Math.min(1.65, Math.exp(random.nextGaussian() * .22)));
      cooperation = Math.max(.05, Math.min(.95, .35 + random.nextGaussian() * .2
          + (group == 0 ? .25 : group == 2 ? -.2 : 0)));
      energy = clamp(.77 + random.nextGaussian() * .08 - (group == 3 ? .09 : 0));
      mood = clamp(.53 + random.nextGaussian() * .1);
      for (int g = 0; g < 10; g++) {
        double preference = switch (style) {
          case 0 -> g == 0 || g == 2 || g == 6 ? .32 : -.1; // knowledge
          case 1 -> g == 1 || g == 5 || g == 8 ? .36 : -.12; // storyteller
          case 2 -> g == 3 || g == 7 || g == 9 ? .37 : -.1; // arcade
          case 3 -> g == 4 || g == 8 || g == 9 ? .28 : -.05; // gambler
          default -> g == 5 || g == 6 || g == 1 ? .25 : 0; // social
        };
        taste[g] = clamp(.5 + preference + random.nextGaussian() * .16);
        skill[g] = clamp(.24 + random.nextDouble() * .56 + preference * .25);
      }
      for (int p = 0; p < size; p++)
        affinity[p] = p == id ? 1 : clamp((group == 0 ? .72 : group == 2 ? .38 : .55)
            + random.nextGaussian() * .16);
    }
    double utility(int game, int lastGame) {
      return 1.8 * taste[game] + .55 * skill[game] + .35 * curiosity * (boredom[game] == 0 ? 1 : 0)
          - 1.25 * boredom[game] - (game == lastGame ? .5 : 0)
          + .25 * mood - .24 * (1 - energy);
    }
    int favorite(int[] options, int lastGame, Random random) {
      int best = 0;
      double score = Double.NEGATIVE_INFINITY;
      for (int i = 0; i < options.length; i++) {
        double candidate = utility(options[i], lastGame) + random.nextGaussian() * .12;
        if (candidate > score) { best = i; score = candidate; }
      }
      choices++;
      return best;
    }
    int voteFor(int[] offers, int lastGame, Person actor, int actorIndex, Random random) {
      int best = 0;
      double score = Double.NEGATIVE_INFINITY;
      for (int i = 0; i < offers.length; i++) {
        int game = offers[i];
        double social = actor == this ? 0
            : .65 * cooperation * affinity[actorIndex] * actor.taste[game];
        double candidate = utility(game, lastGame) + social + random.nextGaussian() * .12;
        if (candidate > score) { best = i; score = candidate; }
      }
      votes++;
      return best;
    }
    int bet(int game) {
      double appetite = .43 * risk + .25 * confidence + .18 * skill[game] + .14 * mood
          - .12 * Math.min(3, losses - wins);
      return appetite > .68 ? 3 : appetite > .46 ? 2 : 1;
    }
    boolean responds(Random random, int group) {
      double chance = (group == 3 ? .07 : .015) + (1 - energy) * .09
          + (1 - mood) * .06 + Math.min(.12, ignored * .025) - patience * .01;
      boolean yes = random.nextDouble() >= chance;
      ignored = yes ? 0 : Math.min(4, ignored + 1);
      return yes;
    }
    void observe(int game, boolean actor, boolean won, double feltIncluded,
        boolean expired, int group) {
      for (int g = 0; g < 10; g++) boredom[g] *= .82;
      boredom[game] = Math.min(1.5, boredom[game] + .32);
      energy = clamp(energy - (group == 3 ? .034 : .019) + (feltIncluded > .5 ? .004 : 0));
      mood = clamp(mood + .045 * (feltIncluded - .5)
          + (actor ? (won ? .05 : -.075) : .002)
          - (expired ? .075 : 0) - (taste[game] < .42 ? .025 : 0));
      if (actor) {
        if (won) wins++; else losses++;
        confidence = clamp(confidence + (won ? .055 : -.07));
      }
    }
  }

  private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }

  static final class Stats {
    long parties, rounds, wins, sips, missed, votes, crew, jury, guesses, timeouts;
    long gameRepeats, preferredChoices, highBets, depleted, disengaged, seconds, visits;
    final long[] game = new long[10], gameTimeout = new long[10], gameDisengage = new long[10];
    void add(Stats other) {
      parties += other.parties; rounds += other.rounds; wins += other.wins; sips += other.sips;
      missed += other.missed; votes += other.votes; crew += other.crew; jury += other.jury;
      guesses += other.guesses; timeouts += other.timeouts; gameRepeats += other.gameRepeats;
      preferredChoices += other.preferredChoices; highBets += other.highBets;
      depleted += other.depleted; disengaged += other.disengaged;
      seconds += other.seconds; visits += other.visits;
      for (int i = 0; i < 10; i++) {
        game[i] += other.game[i]; gameTimeout[i] += other.gameTimeout[i];
        gameDisengage[i] += other.gameDisengage[i];
      }
    }
    String row(String label) {
      return String.format(Locale.ROOT,
          "%s,%d,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f",
          label, parties, rounds, seconds / (double) rounds / 1000,
          100.0 * timeouts / rounds, 100.0 * missed / Math.max(1, visits),
          100.0 * gameRepeats / rounds, 100.0 * preferredChoices / Math.max(1, votes),
          100.0 * highBets / rounds, 100.0 * depleted / Math.max(1, visits),
          100.0 * disengaged / Math.max(1, visits),
          100.0 * wins / rounds, sips / (double) rounds,
          (votes + crew + jury + guesses) / (double) rounds);
    }
  }

  private static final class Event {
    final int person;
    final long at, handoff;
    Event(int person, long at, long handoff) {
      this.person = person; this.at = at; this.handoff = handoff;
    }
  }

  /** Engine-observed round trace for independent, paired experience models. */
  static final class RoundObservation {
    final int game, actor, wager, sips;
    final double seconds;
    final boolean won, expired, repeated;
    final double[] inclusion;
    final boolean[] voted;
    RoundObservation(int game, int actor, int wager, int sips, double seconds,
        boolean won, boolean expired, boolean repeated, double[] inclusion, boolean[] voted) {
      this.game = game;
      this.actor = actor;
      this.wager = wager;
      this.sips = sips;
      this.seconds = seconds;
      this.won = won;
      this.expired = expired;
      this.repeated = repeated;
      this.inclusion = inclusion;
      this.voted = voted;
    }
  }

  static final class Party {
    final Clock clock = new Clock();
    final Random random;
    final GameEngine room;
    final Person[] people;
    final Stats stats;
    final int group, topology;
    final int id;
    int lastGame = -1;
    boolean roundExpired;
    RoundObservation lastObservation;
    Party(int id, Stats stats) {
      this.id = id;
      random = new Random(SEED ^ (id * 0x9E3779B97F4A7C15L));
      group = (id / 15) % 4;
      topology = (id / 5) % 3;
      int size = 2 + id % 5;
      people = new Person[size];
      for (int i = 0; i < size; i++) people[i] = new Person(i, size,
          (i + id / 60) % 5, random, group);
      room = new GameEngine(SEED + id, clock);
      room.newParty();
      for (int i = 0; i < size; i++) assertTrue(room.addPlayer(people[i].name,
          i % 2 == 0 ? "FR" : "EN", i));
      room.mode = MODES[(id / 60) % 3];
      room.begin();
      this.stats = stats;
      stats.parties++;
    }
    void delay(int person, double base) {
      clock.add(sampleDelay(person, base));
    }
    boolean actionBeforeDeadline(int person, double base) {
      long arrival = clock.now + Math.max(1, Math.round(sampleDelay(person, base) * 1000));
      if (room.deadline > 0 && arrival >= room.deadline) {
        clock.reach(room.deadline + 1);
        return false;
      }
      clock.reach(arrival);
      return true;
    }
    double sampleDelay(int person, double base) {
      Person p = people[person];
      double multiplier = (group == 3 ? 1.6 : group == 2 ? 1.15 : 1)
          * (topology == 0 ? 1.12 : 1) * (1.35 - .55 * p.energy);
      return base * multiplier * p.pace * Math.exp(random.nextGaussian() * .25);
    }
    ArrayList<Event> schedule(int actor, boolean all, double base) {
      int devices = topology == 0 ? 1 : topology == 1 ? 2 : people.length;
      long[] ready = new long[devices];
      Arrays.fill(ready, clock.now);
      ArrayList<Event> events = new ArrayList<>();
      for (int step = 0; step < people.length - (all ? 0 : 1); step++) {
        int person = (actor + step + (all ? 0 : 1)) % people.length;
        int device = topology == 0 ? 0 : topology == 1 ? person % 2 : person;
        long handoff = topology == 0 && step > 0 ? 1400 + random.nextInt(1400) : 0;
        double network = topology == 0 ? 0 : .15 * Math.exp(random.nextGaussian() * .6)
            + (group == 3 && random.nextDouble() < .025 ? 2 + random.nextInt(8) : 0);
        long at = ready[device] + handoff + Math.round((sampleDelay(person, base) + network) * 1000);
        ready[device] = at;
        events.add(new Event(person, at, handoff));
      }
      events.sort(Comparator.comparingLong(e -> e.at));
      return events;
    }
    boolean absent(int person) {
      stats.visits++;
      boolean missing = !people[person].responds(random, group);
      if (missing) stats.missed++;
      return missing;
    }
    void select(int actor) {
      if ("RULE_PICK".equals(room.screen))
        assertTrue(room.chooseRule(room.ruleOwner, room.ruleOffers[0]));
      if ("VOTE".equals(room.screen) && !room.ruleOwner.isEmpty() && room.ruleId < 0)
        assertTrue(room.chooseRule(room.ruleOwner, room.ruleOffers[0]));
      if ("VOTE".equals(room.screen)) {
        int[] offers = Arrays.copyOf(room.offers, room.offers.length);
        for (Event event : schedule(actor, true, 1.4)) {
          if (!"VOTE".equals(room.screen)) break;
          int i = event.person;
          if (topology == 0) room.deadline += event.handoff;
          if (event.at >= room.deadline) {
            clock.reach(room.deadline + 1);
            assertTrue(room.voteTimedOut(clock.now));
            break;
          }
          clock.reach(event.at);
          if (absent(i)) { assertTrue(room.skipParticipation(people[i].name)); continue; }
          int pick = people[i].voteFor(offers, lastGame, people[actor], actor, random);
          if (offers[pick] == bestTaste(i, offers)) stats.preferredChoices++;
          stats.votes++;
          assertTrue(room.castVote(people[i].name, pick));
        }
        if ("VOTE".equals(room.screen)) {
          clock.reach(room.deadline + 1);
          assertTrue(room.voteTimedOut(clock.now));
        }
      } else if ("LIBRARY".equals(room.screen)) {
        delay(actor, 2.2);
        int[] all = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        room.startNext(all[people[actor].favorite(all, lastGame, random)]);
      }
      assertEquals("TRANSITION", room.screen);
    }
    int bestTaste(int person, int[] offered) {
      int best = offered[0];
      for (int g : offered)
        if (people[person].taste[g] > people[person].taste[best]) best = g;
      return best;
    }
    void contribute(int game, int actor) {
      for (Event event : schedule(actor, false, 1.9)) {
        if (!"CREW".equals(room.screen)) break;
        int i = event.person;
        if (topology == 0) room.deadline += event.handoff;
        if (event.at >= room.deadline) {
          clock.reach(room.deadline + 1);
          assertTrue(room.crewTimedOut());
          break;
        }
        clock.reach(event.at);
        if (absent(i)) { assertTrue(room.skipParticipation(people[i].name)); continue; }
        int choice = random.nextInt(room.crewOptionCount());
        if (game == 0 || game == 2) {
          if (random.nextDouble() < people[i].skill[game]) choice = room.target;
        } else if (game == 4) {
          if (random.nextDouble() < people[i].cooperation * people[i].affinity[actor]) {
            choice = random.nextInt(6); // A well-meaning friend cannot see the hidden cup.
          }
        }
        assertTrue(room.crewPick(people[i].name, choice));
        people[i].crewActs++;
        stats.crew++;
      }
      if ("CREW".equals(room.screen)) {
        clock.reach(room.deadline + 1);
        assertTrue(room.crewTimedOut());
      }
    }
    void audience(int game, int actor) {
      for (Event event : schedule(actor, false, 2.3)) {
        if (!"GAME".equals(room.screen)) break;
        int i = event.person;
        if (topology == 0) {
          room.deadline += event.handoff;
          if (room.audienceClosingAt > 0) room.audienceClosingAt += event.handoff;
        }
        if (room.audienceClosingAt > 0 && event.at >= room.audienceClosingAt) {
          clock.reach(room.audienceClosingAt);
          room.finish(game == 5 ? room.drawWin() : room.juryVerdict());
          break;
        }
        if (event.at >= room.deadline) {
          clock.reach(room.deadline + 1);
          break;
        }
        clock.reach(event.at);
        if (absent(i)) { assertTrue(room.skipParticipation(people[i].name)); continue; }
        Person observer = people[i];
        if (game == 5) {
          double chance = .18 + .43 * observer.skill[5] + .18 * people[actor].skill[5];
          int guess = random.nextDouble() < chance ? room.target :
              (room.target + 1 + random.nextInt(3)) % 4;
          assertTrue(room.drawGuess(observer.name, guess));
          observer.guesses++; stats.guesses++;
        } else {
          boolean yes;
          if (game == 1) yes = random.nextDouble() <
              .2 + .5 * people[actor].skill[1] + .22 * observer.affinity[actor];
          else {
            double detect = .38 + .4 * observer.skill[8];
            yes = random.nextDouble() < detect ? room.bluffTruth == 1
                : room.bluffTruth != 1;
            if (random.nextDouble() < .12 * (1 - observer.affinity[actor])) yes = !yes;
          }
          assertTrue(room.castJury(observer.name, yes));
          observer.juryActs++; stats.jury++;
        }
      }
      if ("GAME".equals(room.screen) && (game == 5 ? room.drawGuessComplete() : room.juryComplete()))
        room.finish(game == 5 ? room.drawWin() : room.juryVerdict());
      if ("GAME".equals(room.screen) && room.audienceClosingAt > 0) {
        clock.reach(room.audienceClosingAt);
        room.finish(game == 5 ? room.drawWin() : room.juryVerdict());
      }
      if ("GAME".equals(room.screen)) {
        clock.reach(room.deadline + 1);
        roundExpired = true;
        room.checkTimeout();
      }
    }
    void challenge(int game, int actor) {
      Person a = people[actor];
      if (game == 9) {
        while ("GAME".equals(room.screen) && clock.now < room.deadline
            && room.taps < room.bombGoal()) {
          if (room.bombAwaitingPass) {
            int holder = room.bombNext, choice = -1;
            double best = -10;
            for (int i = 0; i < people.length; i++) if (room.canPassBombTo(i)) {
              double score = (1 - people[holder].affinity[i]) * (1 - people[holder].cooperation)
                  + people[i].skill[9] * people[holder].cooperation + random.nextDouble() * .2;
              if (score > best) { best = score; choice = i; }
            }
            if (choice < 0) break;
            if (actionBeforeDeadline(holder, 1.0 + (topology == 0 ? 1.1 : 0)))
              assertTrue(room.bombPass(people[holder].name, choice));
          } else {
            int holder = room.bombNext;
            if (actionBeforeDeadline(holder, .5))
              assertTrue(room.bombTap(people[holder].name));
          }
        }
        if (clock.now < room.deadline && room.taps >= room.bombGoal()) room.finish(true);
      } else if (game == 3) {
        for (int i = 0; i < room.reflexGoal() && clock.now < room.deadline; i++) {
          if (!actionBeforeDeadline(actor, .52 + .35 * (1 - a.skill[3]))) break;
          if (random.nextDouble() < .62 + .35 * a.skill[3])
            assertTrue(room.reflexTap(a.name, room.taps));
        }
        if (clock.now < room.deadline) room.finish(room.taps >= room.reflexGoal());
      } else if (game == 7) {
        for (int beat : room.rhythmPattern()) {
          long at = room.started + beat * 600L + 300 + Math.round(random.nextGaussian()
              * (70 + 170 * (1 - a.skill[7])));
          clock.reach(Math.min(at, room.deadline + 1));
          if (clock.now >= room.deadline) break;
          room.rhythmTap(a.name, beat, topology != 0);
        }
        if (clock.now < room.deadline) room.finish(room.rhythmHits >= 4);
      } else {
        if (!actionBeforeDeadline(actor, GAME_SECONDS[game])) {
          roundExpired = true;
          room.checkTimeout();
          return;
        }
        if (game == 1) {
          assertTrue(room.beginJury());
          audience(game, actor);
        } else if (game == 8) {
          assertTrue(room.chooseBluffTruth(a.name, random.nextDouble() < .5 + .15 * a.confidence));
          audience(game, actor);
        } else if (game == 5) {
          assertTrue(room.beginDrawGuess());
          audience(game, actor);
        } else if (game == 4) {
          int cup = random.nextInt(6);
          if (room.crewLead() >= 0 && random.nextDouble() < a.cooperation) cup = room.crewLead();
          assertTrue(room.selectCup(cup));
          clock.reach(room.revealUntil);
          room.finish(room.cupIsSafe());
        } else {
          double chance = .2 + .62 * a.skill[game] + (room.crewLead() >= 0 ? .06 : 0)
              + .07 * (a.confidence - .5);
          room.finish(random.nextDouble() < chance);
        }
      }
      if ("GAME".equals(room.screen)) {
        clock.reach(room.deadline + 1);
        roundExpired = true;
        room.checkTimeout();
      }
    }
    double inclusionFor(int person, int game, int actor) {
      if (person == actor) return .85;
      if (game == 9) return (room.bombVisitedMask & (1 << person)) != 0 ? .8 : .3;
      if (game == 5) return room.drawGuesses.length > person
          && room.drawGuesses[person] >= 0 && room.drawGuesses[person] < 4 ? .8 : .2;
      if (game == 1 || game == 8) return room.juryVotes.length > person
          && room.juryVotes[person] >= 0 && room.juryVotes[person] < 2 ? .8 : .2;
      return room.crewChoices.length > person && room.crewChoices[person] >= 0
          && room.crewChoices[person] < room.crewOptionCount() ? .75 : .2;
    }
    void round() {
      int actor = room.active;
      long start = clock.now;
      roundExpired = false;
      select(actor);
      int game = room.game;
      if (game == lastGame) stats.gameRepeats++;
      room.enterGame();
      delay(actor, 1.4 + (topology == 0 ? 1.2 : 0));
      room.readyTurn();
      delay(actor, 1.4);
      int wager = people[actor].bet(game);
      if (wager == 3) stats.highBets++;
      assertTrue(room.placeBet(wager));
      if ("CREW".equals(room.screen)) contribute(game, actor);
      assertEquals("GAME", room.screen);
      if (OLD_TIMERS && (game == 2 || game == 6)) room.deadline -= game == 2 ? 5000 : 4000;
      challenge(game, actor);
      assertEquals("party=" + id + " turn=" + room.turn + " game=" + game
          + " time=" + clock.now + " deadline=" + room.deadline,
          "RESULT", room.screen);
      boolean expired = roundExpired;
      if (expired) { stats.timeouts++; stats.gameTimeout[game]++; }
      stats.game[game]++;
      stats.rounds++;
      if (room.lastWon) stats.wins++;
      stats.sips += room.roundSips;
      stats.seconds += clock.now - start;
      double[] inclusion = new double[people.length];
      boolean[] voted = new boolean[people.length];
      for (int i = 0; i < people.length; i++) {
        Person p = people[i];
        inclusion[i] = inclusionFor(i, game, actor);
        voted[i] = room.votes.length > i && room.votes[i] >= 0 && room.votes[i] < 3;
        p.observe(game, i == actor, room.lastWon, inclusion[i], expired, group);
        if (p.energy < .4) stats.depleted++;
        // This is an assumed signal for design exploration, never measured churn.
        if (p.mood < .4 && p.energy < .6) {
          stats.disengaged++;
          stats.gameDisengage[game]++;
        }
        if (i != actor && p.crewActs + p.juryActs + p.guesses == 0 && room.turn > 4)
          p.mood = clamp(p.mood - .015);
        if (i != actor) {
          double delta = (room.lastWon ? .012 : -.008) * p.cooperation
              + (game == 9 ? .007 : 0);
          p.affinity[actor] = clamp(p.affinity[actor] + delta);
        }
      }
      delay(actor, 2.8);
      lastObservation = new RoundObservation(game, actor, wager, room.roundSips,
          (clock.now - start) / 1000.0, room.lastWon, expired, game == lastGame,
          inclusion, voted);
      room.advance();
      lastGame = game;
    }
  }

  @Test public void preferencesReactToMemoryAndLosses() {
    Random random = new Random(14);
    Person p = new Person(0, 2, 0, random, 0);
    Arrays.fill(p.taste, .1);
    p.taste[0] = .9;
    p.taste[1] = .7;
    assertEquals(0, p.favorite(new int[] {0, 1}, -1, new Random(1)));
    p.boredom[0] = 1.1;
    assertEquals(1, p.favorite(new int[] {0, 1}, 0, new Random(1)));
    p.confidence = .9;
    int confidentBet = p.bet(0);
    p.confidence = .05;
    p.losses = 4;
    assertTrue(p.bet(0) < confidentBet);
  }

  @Test public void friendshipCanChangeAVoteWithoutChangingTaste() {
    Person voter = new Person(0, 2, 4, new Random(8), 0);
    Person actor = new Person(1, 2, 1, new Random(9), 0);
    Arrays.fill(voter.taste, .1);
    Arrays.fill(voter.skill, .5);
    Arrays.fill(actor.taste, .1);
    voter.taste[0] = .75;
    voter.taste[1] = .73;
    actor.taste[1] = 1;
    Random noNoise = new Random() {
      @Override public double nextGaussian() { return 0; }
    };
    voter.affinity[1] = 0;
    assertEquals(0, voter.voteFor(new int[] {0, 1, 2}, -1, actor, 1, noNoise));
    voter.affinity[1] = 1;
    assertEquals(1, voter.voteFor(new int[] {0, 1, 2}, -1, actor, 1, noNoise));
  }

  @Test public void statefulPeoplePlayEveryChallenge() {
    int parties = Integer.parseInt(System.getenv().getOrDefault("APERO_PEOPLE_PARTIES", "3600"));
    if (parties < 180 || parties > 100_000) throw new IllegalArgumentException("180..100000 parties");
    Stats[][] data = new Stats[3][4];
    Stats[][] sizeMode = new Stats[5][3];
    for (int t = 0; t < 3; t++) for (int g = 0; g < 4; g++) data[t][g] = new Stats();
    for (int n = 0; n < 5; n++) for (int m = 0; m < 3; m++) sizeMode[n][m] = new Stats();
    for (int id = 0; id < parties; id++) {
      Party party = new Party(id, new Stats());
      for (int round = 0; round < ROUNDS; round++) party.round();
      data[(id / 5) % 3][(id / 15) % 4].add(party.stats);
      sizeMode[id % 5][(id / 60) % 3].add(party.stats);
    }
    long[] games = new long[10];
    System.out.println("VIRTUAL PEOPLE: seed=" + SEED + " parties=" + parties
        + " rounds=" + (parties * ROUNDS) + " hypothetical stateful agents; timers="
        + (OLD_TIMERS ? "1.4.8" : "current"));
    System.out.println("topology/group,parties,rounds,mean_s,timeout_pct,missed_action_pct,repeat_game_pct,raw_preference_vote_pct,three_sip_bet_pct,low_energy_observations_pct,low_mood_energy_observations_pct,actor_win_pct,sips_per_round,peer_actions_per_round");
    for (int t = 0; t < 3; t++) for (int g = 0; g < 4; g++) {
      Stats s = data[t][g];
      System.out.println(s.row((t == 0 ? "shared" : t == 1 ? "two" : "each") + "/" + GROUPS[g]));
      for (int game = 0; game < 10; game++) games[game] += s.game[game];
    }
    System.out.println("players/mode,parties,rounds,mean_s,timeout_pct,missed_action_pct,repeat_game_pct,raw_preference_vote_pct,three_sip_bet_pct,low_energy_observations_pct,low_mood_energy_observations_pct,actor_win_pct,sips_per_round,peer_actions_per_round");
    for (int n = 0; n < 5; n++) for (int m = 0; m < 3; m++)
      System.out.println(sizeMode[n][m].row((n + 2) + "/" + MODES[m]));
    System.out.println("game,rounds,timeouts,low_mood_energy_observations");
    for (int game = 0; game < 10; game++) {
      long count = 0, timeouts = 0, disengaged = 0;
      for (Stats[] row : data) for (Stats s : row) {
        count += s.game[game]; timeouts += s.gameTimeout[game];
        disengaged += s.gameDisengage[game];
      }
      System.out.println(GameEngine.TYPES[game] + "," + count + "," + timeouts + "," + disengaged);
      assertTrue("game " + game + " never played", games[game] > 0);
    }
  }
}
