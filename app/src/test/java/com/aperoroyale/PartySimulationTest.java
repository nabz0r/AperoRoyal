package com.aperoroyale;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;
import org.json.JSONObject;

/** Reproducible engine-level party simulation, with every game and room size represented. */
public final class PartySimulationTest {
  @Test public void fiveHundredPartiesFinishWithoutBrokenTurnOrScore() throws Exception {
    int[] seen = new int[GameEngine.TYPES.length];
    int rounds = 0, wins = 0, losses = 0, timeouts = 0, rulePicks = 0,
        turboRulePicks = 0, juryTimeouts = 0, restores = 0, ruleReports = 0;
    for (int party = 0; party < 500; party++) {
      Random choices = new Random(0xA9E2026L + party);
      GameEngine room = new GameEngine(0xB0A4D2026L + party);
      room.newParty();
      int people = 2 + party % 5;
      for (int i = 0; i < people; i++)
        assertTrue(room.addPlayer("P" + i, i % 2 == 0 ? "FR" : "EN", i));
      room.mode = switch (party % 3) {
        case 0 -> "VOTE";
        case 1 -> "FREE";
        default -> "TURBO";
      };
      room.begin();
      room = restored(room);
      restores++;
      int previous = -1;
      int partyRounds = 12 + party % 9;
      for (int round = 0; round < partyRounds; round++) {
        int actor = room.active;
        assertEquals(round, room.turn);
        if (!room.ruleOwner.isEmpty() && room.ruleId < 0) {
          assertTrue("secret has no rule picker in " + room.mode,
              "VOTE".equals(room.screen) || "LIBRARY".equals(room.screen)
                  || "RULE_PICK".equals(room.screen));
          assertEquals(3, room.ruleOffers.length);
          if ("RULE_PICK".equals(room.screen)) turboRulePicks++;
          room = restored(room);
          restores++;
          assertTrue(room.chooseRule(room.ruleOwner, room.ruleOffers[choices.nextInt(3)]));
          rulePicks++;
        }
        if (room.ruleId >= 3 && ("VOTE".equals(room.screen) || "LIBRARY".equals(room.screen))
            && choices.nextInt(7) == 0) {
          String prior = room.screen;
          int previousSips = room.players.get(1).sips;
          assertTrue(room.reportRule("P0", "P1"));
          assertEquals("RULE_VOTE", room.screen);
          room = restored(room);
          restores++;
          for (int i = 0; i < people && "RULE_VOTE".equals(room.screen); i++)
            if (room.reportVotes[i] < 0)
              assertTrue(room.castRuleVote("P" + i, choices.nextBoolean()));
          if ("RULE_VOTE".equals(room.screen)) {
            room.reportDeadline = System.currentTimeMillis() - 1;
            assertTrue(room.ruleVoteTimedOut());
          }
          assertEquals(prior, room.screen);
          assertEquals(previousSips + (room.rulePenaltyApplied ? 1 : 0),
              room.players.get(1).sips);
          ruleReports++;
        }
        if ("VOTE".equals(room.mode)) {
          assertEquals("VOTE", room.screen);
          assertEquals(3, room.offers.length);
          for (int i = 0; i < people; i++) {
            if (i == 1) { room = restored(room); restores++; }
            assertTrue(room.castVote("P" + i, choices.nextInt(3)));
          }
        } else if ("FREE".equals(room.mode)) {
          assertEquals("LIBRARY", room.screen);
          room = restored(room);
          restores++;
          room.startNext(choices.nextInt(GameEngine.TYPES.length));
        }
        assertEquals("TRANSITION", room.screen);
        assertTrue(room.game >= 0 && room.game < GameEngine.TYPES.length);
        if (!"FREE".equals(room.mode) && previous >= 0)
          assertNotEquals("immediate repeat in party " + party, previous, room.game);
        seen[room.game]++;
        previous = room.game;
        if (round % 3 == 0) { room = restored(room); restores++; }
        room.enterGame();
        assertEquals("HANDOFF", room.screen);
        room = restored(room);
        restores++;
        room.readyTurn();
        assertEquals("BET", room.screen);
        room = restored(room);
        restores++;
        assertTrue(room.placeBet(1 + choices.nextInt(3)));
        assertEquals("PREDICT", room.screen);
        if (round % 3 == 1) { room = restored(room); restores++; }
        if (choices.nextInt(8) == 0) {
          room.deadline = System.currentTimeMillis() - 1;
          assertTrue(room.predictionTimedOut());
          timeouts++;
        } else {
          for (int i = 0; i < people; i++)
            if (i != actor) assertTrue(room.predict("P" + i, choices.nextBoolean()));
        }
        assertEquals("GAME", room.screen);
        if (round % 3 == 2) { room = restored(room); restores++; }
        JSONObject spectatorView = room.networkJsonFor("P" + ((actor + 1) % people));
        assertEquals(-1, spectatorView.optInt("target"));
        assertEquals(-1, spectatorView.optInt("loserCup"));
        if (room.game == 6) assertEquals(0, spectatorView.getJSONArray("sequence").length());
        if (room.game == 8) assertEquals(-1, spectatorView.optInt("bluffTruth"));
        boolean won = choices.nextBoolean();
        boolean juryTimeout = false;
        switch (room.game) {
          case 1 -> {
            assertTrue(room.beginJury());
            room = restored(room);
            restores++;
            for (int i = 0; i < people; i++)
              if (i != actor) assertTrue(room.castJury("P" + i, choices.nextBoolean()));
            assertTrue(room.juryComplete());
            won = room.juryVerdict();
            juryTimeout = choices.nextInt(4) == 0;
          }
          case 3 -> {
            if (won) for (int tap = 0; tap < 10; tap++)
              assertTrue(room.reflexTap("P" + actor, tap));
          }
          case 4 -> {
            int cup = choices.nextInt(6);
            assertTrue(room.selectCup(cup));
            assertFalse(room.selectCup((cup + 1) % 6));
            won = room.cupIsSafe();
          }
          case 8 -> {
            assertTrue(room.chooseBluffTruth("P" + actor, choices.nextBoolean()));
            room = restored(room);
            restores++;
            for (int i = 0; i < people; i++)
              if (i != actor) assertTrue(room.castJury("P" + i, choices.nextBoolean()));
            assertTrue(room.juryComplete());
            won = room.juryVerdict();
            juryTimeout = choices.nextInt(4) == 0;
          }
          case 9 -> {
            if (won) while (room.taps < room.bombGoal())
              assertTrue(room.bombTap(room.players.get(room.bombNext).name));
          }
          default -> { }
        }
        if (juryTimeout) {
          room.deadline = System.currentTimeMillis() - 1;
          room.checkTimeout();
          assertEquals("jury timeout skipped result", "RESULT", room.screen);
          juryTimeouts++;
        } else room.finish(won);
        room = restored(room);
        restores++;
        assertEquals("RESULT", room.screen);
        assertEquals(won, room.lastWon);
        assertEquals(0, room.deadline);
        assertEquals(round + 1, totalGames(room));
        for (GameEngine.Player p : room.players) {
          assertTrue("negative score", p.score >= 0);
          assertTrue("wins exceed games", p.wins <= p.games);
          assertTrue("negative sips", p.sips >= 0);
          assertTrue("negative drinks", p.drinks >= 0);
        }
        if (won) wins++; else losses++;
        rounds++;
        room.advance();
        assertEquals(round + 1, room.turn);
        assertEquals(Math.floorMod(actor + (room.ruleId == 2 ? -1 : 1), people), room.active);
      }
      for (GameEngine.Player player : room.players)
        assertTrue("player never received an acting turn", player.games > 0);
    }
    assertEquals(rounds, wins + losses);
    for (int count : seen) assertTrue("game never selected", count > 0);
    assertTrue(rulePicks > 0);
    assertTrue(turboRulePicks > 0);
    assertTrue(juryTimeouts > 0);
    assertTrue(ruleReports > 0);
    System.out.printf("SIMULATION: 500 parties, %d rounds, %d wins, %d losses, %d prediction timeouts, %d jury timeouts, %d rule picks (%d turbo), %d rule reports, %d restores%n",
        rounds, wins, losses, timeouts, juryTimeouts, rulePicks, turboRulePicks, ruleReports,
        restores);
  }

  private static int totalGames(GameEngine room) {
    int sum = 0;
    for (GameEngine.Player p : room.players) sum += p.games;
    return sum;
  }

  private static GameEngine restored(GameEngine source) throws Exception {
    JSONObject saved = new JSONObject(source.json().toString());
    GameEngine copy = new GameEngine(0x5EED2026L ^ source.turn * 1315423911L
        ^ source.screen.hashCode() ^ source.game);
    copy.restore(saved);
    assertEquals(source.screen, copy.screen);
    assertEquals(source.active, copy.active);
    assertEquals(source.turn, copy.turn);
    assertEquals(source.players.size(), copy.players.size());
    assertEquals(source.game, copy.game);
    assertEquals(source.wager, copy.wager);
    assertEquals(source.ruleId, copy.ruleId);
    assertEquals(source.deadline, copy.deadline);
    assertEquals(source.ruleOwner, copy.ruleOwner);
    assertArrayEquals(source.offers, copy.offers);
    assertArrayEquals(source.votes, copy.votes);
    assertArrayEquals(source.predictions, copy.predictions);
    assertArrayEquals(source.juryVotes, copy.juryVotes);
    for (int i = 0; i < source.players.size(); i++) {
      GameEngine.Player a = source.players.get(i), b = copy.players.get(i);
      assertEquals(a.name, b.name);
      assertEquals(a.language, b.language);
      assertEquals(a.score, b.score);
      assertEquals(a.wins, b.wins);
      assertEquals(a.games, b.games);
      assertEquals(a.sips, b.sips);
    }
    return copy;
  }
}
