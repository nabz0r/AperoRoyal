package com.aperoroyale;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/** A reproducible pacing/content audit, not a substitute for observing a real group. */
public final class PartyExperienceSimulationTest {
  private static final String[] MODES = {"VOTE", "FREE", "TURBO"};
  private static final int[] AUTHORED = {GameEngine.QUIZ_FR.length, GameEngine.POSES.length,
      GameEngine.TUNES.length * 4, 6, 6, GameEngine.DRAW.length, 6, 6,
      GameEngine.BLUFF.length, 6};

  private static final class Cohort {
    int parties, rounds, soloRounds, over45, repeatedGame, repeatedCard;
    int longestPassiveRun, longestPassiveSeconds, maxRoundSeconds;
    int bombCuts, bombCutWins, bombFullRelays;
    long totalSeconds, setupSeconds, totalCoreActions;
  }

  @Test public void fiveHundredPartiesAuditPaceAndMeaningfulParticipation() {
    Cohort[][][] cohorts = new Cohort[5][2][3];
    for (int n = 0; n < 5; n++) for (int device = 0; device < 2; device++)
      for (int mode = 0; mode < 3; mode++) cohorts[n][device][mode] = new Cohort();
    int[] selected = new int[10];
    int[] gameSolo = new int[10];
    long[] gameSeconds = new long[10];
    int totalRounds = 0;

    for (int party = 0; party < 500; party++) {
      int people = 2 + party % 5;
      boolean sharedPhone = (party / 5) % 2 == 0;
      int mode = (party / 10) % 3;
      Cohort cohort = cohorts[people - 2][sharedPhone ? 0 : 1][mode];
      cohort.parties++;
      Random input = new Random(0xE2026L + party);
      GameEngine room = new GameEngine(0xA2026L + party);
      room.newParty();
      for (int p = 0; p < people; p++)
        assertTrue(room.addPlayer("P" + p, p % 2 == 0 ? "FR" : "EN", p));
      room.mode = MODES[mode];
      room.begin();
      int previousGame = -1;
      boolean[][] seenCards = new boolean[10][];
      int[] cardsInPack = new int[10];
      int[] passiveRun = new int[people];
      int[] passiveSeconds = new int[people];
      int favorite = input.nextInt(10);
      for (int game = 0; game < 10; game++) {
        seenCards[game] = new boolean[Math.max(1, AUTHORED[game])];
      }

      for (int round = 0, length = 12 + party % 9; round < length; round++) {
        if (!room.ruleOwner.isEmpty() && room.ruleId < 0)
          assertTrue(room.chooseRule(room.ruleOwner, room.ruleOffers[0]));
        int actor = room.active;
        int setup = sharedPhone ? 15 : 13; // transition 2, ready 4/2, bet 4, result 5
        if (mode == 0) {
          assertEquals("VOTE", room.screen);
          setup += sharedPhone ? 3 * people + 2 * (people - 1) : 5;
          for (int p = 0; p < people; p++)
            assertTrue(room.castVote("P" + p, input.nextInt(3)));
        } else if (mode == 1) {
          assertEquals("LIBRARY", room.screen);
          setup += 4;
          // Most free-play groups revisit a favorite; this stresses the content pack.
          room.startNext(input.nextInt(10) < 6 ? favorite : input.nextInt(10));
        }
        assertEquals("TRANSITION", room.screen);
        int game = room.game;
        selected[game]++;
        if (previousGame == game) cohort.repeatedGame++;
        previousGame = game;
        if (AUTHORED[game] > 0) {
          if (seenCards[game][room.variant]) cohort.repeatedCard++;
          seenCards[game][room.variant] = true;
          if (++cardsInPack[game] == AUTHORED[game]) {
            cardsInPack[game] = 0;
            java.util.Arrays.fill(seenCards[game], false);
          }
        }

        room.enterGame();
        room.readyTurn();
        assertTrue(room.placeBet(1 + input.nextInt(3)));
        if (game != 1 && game != 8 && game != 9) {
          setup += sharedPhone ? 5 * (people - 1) : 5;
          for (int p = 0; p < people; p++)
            if (p != actor) assertTrue(room.predict("P" + p, input.nextBoolean()));
        }
        assertEquals("GAME", room.screen);

        boolean[] coreAction = new boolean[people];
        coreAction[actor] = true;
        boolean roundWon = input.nextBoolean();
        if (game == 1 || game == 8) {
          if (game == 1) assertTrue(room.beginJury());
          else assertTrue(room.chooseBluffTruth("P" + actor, input.nextBoolean()));
          for (int p = 0; p < people; p++) if (p != actor) {
            coreAction[p] = true;
            assertTrue(room.castJury("P" + p, input.nextBoolean()));
          }
        } else if (game == 5) {
          coreAction[(actor + 1) % people] = true;
          room.drawingReady = true;
        } else if (game == 9) {
          while (room.taps < room.bombGoal()) {
            if (room.bombAwaitingPass) {
              if (room.canDefuseBomb() && input.nextInt(3) == 0) {
                int outcome = room.bombCut(room.players.get(room.bombNext).name,
                    input.nextInt(2));
                assertTrue(outcome >= 0);
                cohort.bombCuts++;
                if (outcome == 1) cohort.bombCutWins++;
                roundWon = outcome == 1;
                break;
              }
              int target = -1;
              for (int p = 0; p < people; p++) if (room.canPassBombTo(p)) {
                target = p; break;
              }
              assertTrue(target >= 0);
              assertTrue(room.bombPass(room.players.get(room.bombNext).name, target));
            } else assertTrue(room.bombTap(room.players.get(room.bombNext).name));
          }
          if (!room.bombCutAttempted) {
            assertEquals(room.bombGoal(), room.taps);
            cohort.bombFullRelays++;
            roundWon = true;
          }
          for (int p = 0; p < people; p++) coreAction[p] = true;
        } else if (game == 4) {
          assertTrue(room.selectCup(input.nextInt(6)));
          roundWon = room.cupIsSafe();
        }
        int duration = expectedActionSeconds(room, sharedPhone, people);
        int seconds = setup + duration;
        int actions = 0;
        for (int p = 0; p < people; p++) {
          if (coreAction[p]) {
            actions++;
            cohort.longestPassiveSeconds = Math.max(cohort.longestPassiveSeconds, passiveSeconds[p]);
            passiveRun[p] = passiveSeconds[p] = 0;
          } else {
            cohort.longestPassiveRun = Math.max(cohort.longestPassiveRun, ++passiveRun[p]);
            passiveSeconds[p] += seconds;
          }
        }
        if (actions == 1) cohort.soloRounds++;
        if (actions == 1) gameSolo[game]++;
        gameSeconds[game] += duration;
        cohort.totalCoreActions += actions;
        cohort.setupSeconds += setup;
        cohort.totalSeconds += seconds;
        cohort.maxRoundSeconds = Math.max(cohort.maxRoundSeconds, seconds);
        if (seconds > 45) cohort.over45++;
        cohort.rounds++;
        totalRounds++;
        room.finish(roundWon);
        room.advance();
      }
    }
    assertEquals(7990, totalRounds);
    for (int count : selected) assertTrue("unseen game", count > 0);
    System.out.println("EXPERIENCE MODEL: 500 seeded parties / 7,990 rounds; seconds are assumptions, not measured human time");
    System.out.println("players,phones,mode,parties,rounds,mean_s,setup_pct,over_45_pct,solo_game_pct,core_actions_per_player,max_passive_rounds,max_passive_s,repeated_game,early_variant_repeat,max_round_s,bomb_cuts,bomb_cut_wins,bomb_full_relays");
    for (int people = 2; people <= 6; people++) for (int device = 0; device < 2; device++)
      for (int mode = 0; mode < 3; mode++) {
        Cohort c = cohorts[people - 2][device][mode];
        assertTrue(c.rounds > 0);
        System.out.printf(java.util.Locale.ROOT,
            "%d,%s,%s,%d,%d,%.1f,%.1f,%.1f,%.1f,%.2f,%d,%d,%d,%d,%d,%d,%d,%d%n",
            people, device == 0 ? "shared" : "individual", MODES[mode], c.parties,
            c.rounds, c.totalSeconds / (double) c.rounds,
            100.0 * c.setupSeconds / c.totalSeconds, 100.0 * c.over45 / c.rounds,
            100.0 * c.soloRounds / c.rounds,
            c.totalCoreActions / (double) (c.rounds * people), c.longestPassiveRun,
            c.longestPassiveSeconds, c.repeatedGame, c.repeatedCard, c.maxRoundSeconds,
            c.bombCuts, c.bombCutWins, c.bombFullRelays);
        assertEquals("authored content repeated before its pack was exhausted", 0, c.repeatedCard);
      }
    System.out.println("game,rounds,solo_game_pct,mean_action_s,authored_variants");
    for (int game = 0; game < 10; game++)
      System.out.printf(java.util.Locale.ROOT, "%s,%d,%.1f,%.1f,%d%n",
          GameEngine.TYPES[game], selected[game], 100.0 * gameSolo[game] / selected[game],
          gameSeconds[game] / (double) selected[game], AUTHORED[game]);
    int cuts = 0, cutWins = 0, fullRelays = 0;
    for (Cohort[][] byDevice : cohorts) for (Cohort[] byMode : byDevice)
      for (Cohort c : byMode) {
        cuts += c.bombCuts;
        cutWins += c.bombCutWins;
        fullRelays += c.bombFullRelays;
      }
    assertTrue(cuts > 0 && cutWins > 0 && fullRelays > 0);
    System.out.printf("bomb_cuts=%d bomb_cut_wins=%d bomb_full_relays=%d%n",
        cuts, cutWins, fullRelays);
  }

  private static int expectedActionSeconds(GameEngine room, boolean shared, int people) {
    int cap = room.deadline <= 0 ? Integer.MAX_VALUE
        : (int) ((room.deadline - room.started) / 1000L);
    int actorSeconds = switch (room.game) {
      case 0 -> 9; // read and answer
      case 1 -> 15; // physical pose
      case 2 -> 12; // hear and identify the original motif
      case 3 -> 11; // ten targets
      case 4 -> 4; // select a cup
      case 5 -> 24; // draw
      case 6 -> 13; // reveal + replay
      case 7 -> 10; // four beats
      case 8 -> 16; // tell a story and choose truth
      default -> room.taps; // one modeled second per tap performed before the cut or completion
    };
    int seconds = Math.min(actorSeconds, cap);
    if (room.game == 1 || room.game == 8)
      seconds += shared ? 5 * (people - 1) : 5; // jury decisions and local handoffs
    if (room.game == 5) seconds += shared ? 8 : 5; // handoff and guess
    if (room.game == 9) {
      int handoffs = (room.taps - 1) / room.bombTapsPerHolder();
      seconds += (shared ? 4 : 2) * handoffs; // choose a person, then physical/network handoff
      if (room.bombCutAttempted) seconds += 2; // one final risk decision
    }
    return seconds;
  }
}
