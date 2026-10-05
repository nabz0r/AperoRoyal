package com.aperoroyale;

import static org.junit.Assert.*;
import org.junit.Test;

public final class GameEngineRulesTest {
  private GameEngine room() {
    GameEngine game = new GameEngine();
    assertTrue(game.addPlayer("A", "FR", 0));
    assertTrue(game.addPlayer("B", "EN", 1));
    game.active = 0;
    return game;
  }

  @Test public void triviaGivesTheOtherPlayerARealAnswerAndPoints() {
    GameEngine game = room();
    game.startNext(0);
    game.screen = "BET";
    assertTrue(game.placeBet(2));
    assertEquals("CREW", game.screen);
    assertFalse(game.crewPick("A", game.target));
    assertTrue(game.crewPick("B", game.target));
    assertEquals("GAME", game.screen);
    assertEquals(game.target, game.crewLead());
    assertEquals(1, game.crewCount());
    game.finish(true);
    assertEquals(35, game.crewPoints[1]);
    assertEquals(35, game.players.get(1).score);
  }

  @Test public void rouletteShieldChangesRiskAndRewardsBothPlayers() {
    GameEngine game = room();
    game.startNext(4);
    game.screen = "BET";
    assertTrue(game.placeBet(2));
    int cursed = -1;
    for (int cup = 0; cup < 6; cup++) if (game.cupIsCursed(cup)) { cursed = cup; break; }
    assertTrue(game.crewPick("B", cursed));
    assertTrue(game.selectCup(cursed));
    assertEquals(1, game.lossSips());
    game.finish(false);
    assertEquals(10, game.players.get(1).score);
  }

  @Test public void friendsBuildTheActualMemoryCourseAndBeatPattern() {
    GameEngine game = room();
    assertTrue(game.addPlayer("C", "FR", 2));
    game.startNext(6);
    game.screen = "BET";
    assertTrue(game.placeBet(1));
    assertTrue(game.crewPick("B", 3));
    assertTrue(game.crewPick("C", 1));
    assertEquals("GAME", game.screen);
    assertEquals(3, game.sequence[0]);
    assertEquals(1, game.sequence[1]);

    game.startNext(7);
    game.screen = "BET";
    assertTrue(game.placeBet(1));
    assertTrue(game.crewPick("B", 7));
    assertTrue(game.crewPick("C", 5));
    assertEquals("GAME", game.screen);
    assertTrue(java.util.Arrays.stream(game.rhythmPattern()).anyMatch(n -> n == 7));
    assertTrue(java.util.Arrays.stream(game.rhythmPattern()).anyMatch(n -> n == 5));
  }

  @Test public void privateCrewSnapshotHidesAnswersUntilGameStarts() throws Exception {
    GameEngine game = room();
    game.startNext(0);
    game.screen = "BET";
    assertTrue(game.placeBet(1));
    org.json.JSONObject recipient = game.networkJsonFor("B");
    assertEquals(-1, recipient.optInt("target"));
    assertEquals(-1, recipient.optInt("variant"));
    assertEquals(4, recipient.getJSONArray("screenChoicesEn").length());
    assertTrue(game.crewPick("B", game.target));
    assertEquals("GAME", game.screen);
    assertEquals(-1, game.networkJsonFor("B").optInt("target"));
  }

  @Test public void bluffRewardsFoolingTheJurorAndKeepsTheClaimFixed() {
    GameEngine game = room();
    game.startNext(8);
    game.screen = "GAME";
    assertTrue(game.chooseBluffTruth("A", true));
    assertFalse(game.chooseBluffTruth("A", false));
    assertTrue(game.castJury("B", false));
    assertTrue(game.juryVerdict());
  }

  @Test public void bombGivesEachPlayerTwoTapsBeforePassing() {
    GameEngine game = room();
    game.startNext(9);
    game.variant = 1; // Two taps per holder.
    game.screen = "GAME";
    assertEquals(8, game.bombGoal());
    assertTrue(game.bombTap("A"));
    assertFalse(game.bombTap("B"));
    assertTrue(game.bombTap("A"));
    assertTrue(game.bombAwaitingPass);
    assertFalse(game.bombTap("A"));
    assertFalse(game.bombPass("A", 0));
    assertTrue(game.bombPass("A", 1));
    assertTrue(game.bombTap("B"));
    assertTrue(game.bombTap("B"));
    assertTrue(game.bombPass("B", 0));
  }

  @Test public void bombLetsTheHolderChooseAnUnvisitedFriendAndSurvivesRestore() {
    GameEngine game = room();
    assertTrue(game.addPlayer("C", "FR", 2));
    game.startNext(9);
    game.variant = 1;
    game.screen = "GAME";
    assertTrue(game.bombTap("A"));
    assertTrue(game.bombTap("A"));
    assertTrue(game.canPassBombTo(1));
    assertTrue(game.canPassBombTo(2));
    assertTrue(game.bombPass("A", 2));
    GameEngine copy = new GameEngine(42);
    copy.restore(game.json());
    assertEquals(2, copy.bombNext);
    assertEquals(game.bombVisitedMask, copy.bombVisitedMask);
    assertTrue(copy.bombTap("C"));
    assertTrue(copy.bombTap("C"));
    assertFalse(copy.canPassBombTo(0));
    assertTrue(copy.canPassBombTo(1));
  }

  @Test public void bombCutRequiresEveryPlayerAndRewardsACorrectRisk() {
    GameEngine game = room();
    game.startNext(9);
    game.variant = 1;
    game.screen = "GAME";
    assertTrue(game.bombTap("A"));
    assertTrue(game.bombTap("A"));
    assertFalse(game.canDefuseBomb());
    assertEquals(-1, game.bombCut("A", 0));
    assertTrue(game.bombPass("A", 1));
    assertTrue(game.bombTap("B"));
    assertTrue(game.bombTap("B"));
    assertTrue(game.canDefuseBomb());
    int safeWire = Math.floorMod(game.loserCup + game.taps / game.bombTapsPerHolder(), 2);
    assertEquals(1, game.bombCut("B", safeWire));
    assertEquals(-1, game.bombCut("B", 1 - safeWire));
    game.finish(true);
    assertTrue(game.roundPoints >= 200);
  }

  @Test public void mechanicalVariantsChangeGoalsAndMemoryPatterns() {
    GameEngine game = room();
    game.startNext(3);
    int[] goals = {8, 10, 12, 9, 11, 10};
    for (int variant = 0; variant < 6; variant++) {
      game.variant = variant;
      assertEquals(goals[variant], game.reflexGoal());
      assertTrue(game.reflexHitRadius() >= 40);
    }
    int[] mirror = GameEngine.memorySequence(new java.util.Random(7), 1, 7);
    for (int i = 0; i < mirror.length; i++) assertEquals(mirror[i], mirror[mirror.length - 1 - i]);
    int[] alternate = GameEngine.memorySequence(new java.util.Random(7), 2, 7);
    for (int i = 2; i < alternate.length; i++) assertEquals(alternate[i - 2], alternate[i]);
    int[] distinct = GameEngine.memorySequence(new java.util.Random(7), 3, 7);
    for (int i = 1; i < distinct.length; i++) assertNotEquals(distinct[i - 1], distinct[i]);
    int[] pairs = GameEngine.memorySequence(new java.util.Random(7), 5, 7);
    for (int i = 1; i < pairs.length; i += 2) assertEquals(pairs[i - 1], pairs[i]);
    game.startNext(9);
    for (int variant = 0; variant < 6; variant++) {
      game.variant = variant;
      assertEquals(1 + variant % 3, game.bombTapsPerHolder());
      assertEquals(Math.max(8, (1 + variant % 3) * game.players.size()), game.bombGoal());
    }
    game.startNext(7);
    java.util.HashSet<String> rhythms = new java.util.HashSet<>();
    for (int variant = 0; variant < 6; variant++) {
      game.variant = variant;
      int[] pattern = game.rhythmPattern();
      assertEquals(4, pattern.length);
      for (int i = 1; i < 4; i++) assertTrue(pattern[i] > pattern[i - 1]);
      rhythms.add(java.util.Arrays.toString(pattern));
    }
    assertEquals(6, rhythms.size());
  }

  @Test public void rouletteLayoutsKeepTheStakeRiskAndRevealTheChosenCup() {
    GameEngine game = room();
    game.startNext(4);
    game.screen = "GAME";
    game.loserCup = 2;
    for (int variant = 0; variant < 6; variant++) {
      game.variant = variant;
      for (int stake = 1; stake <= 3; stake++) {
        game.wager = stake;
        int cursed = 0;
        for (int cup = 0; cup < 6; cup++) {
          if (game.cupIsCursed(cup)) cursed++;
          game.chosenCup = cup;
          assertEquals(!game.cupIsCursed(cup), game.cupIsSafe());
        }
        assertEquals(stake, cursed);
      }
    }
  }

  @Test public void promptCannotRepeatImmediately() {
    GameEngine game = room();
    game.startNext(0);
    int first = game.variant;
    game.startNext(0);
    assertNotEquals(first, game.variant);
  }

  @Test public void authoredCardsExhaustTheirPackBeforeRepeatingEvenAfterRestore() throws Exception {
    int[] games = {0, 1, 2, 5, 8};
    int[] counts = {GameEngine.QUIZ_FR.length, GameEngine.POSES.length,
        GameEngine.TUNES.length * 4, GameEngine.DRAW.length, GameEngine.BLUFF.length};
    for (int k = 0; k < games.length; k++) {
      GameEngine game = room();
      boolean[] seen = new boolean[counts[k]];
      int previous = -1;
      for (int i = 0; i < counts[k] * 2; i++) {
        game.startNext(games[k]);
        assertFalse("card repeated before pack exhausted", seen[game.variant]);
        assertNotEquals("same card at pack boundary", previous, game.variant);
        seen[game.variant] = true;
        previous = game.variant;
        if (i == counts[k] - 1 || i == 2) {
          GameEngine copy = new GameEngine(100 + k);
          copy.restore(game.json());
          game = copy;
        }
        if ((i + 1) % counts[k] == 0) java.util.Arrays.fill(seen, false);
      }
    }
  }

  @Test public void rouletteAndBothDrawingPhasesHaveFiniteTimeLimits() {
    GameEngine game = room();
    game.startNext(4);
    game.screen = "GAME";
    game.resumeGame();
    assertTrue(game.deadline > game.started);
    assertTrue(game.selectCup(0));
    assertEquals(0, game.deadline);
    assertTrue(game.revealUntil > 0);

    game.startNext(5);
    game.screen = "GAME";
    game.resumeGame();
    long drawLimit = game.deadline - game.started;
    assertTrue(drawLimit >= 40_000 && drawLimit <= 51_000);
    assertTrue(game.beginDrawGuess());
    long guessLimit = game.deadline - game.started;
    assertTrue(guessLimit >= 12_000 && guessLimit <= 23_000);
    assertTrue(guessLimit < drawLimit);
  }

  @Test public void everyChallengeIncludesTheWholeRoom() {
    for (String mode : new String[] {"VOTE", "TURBO"}) {
      GameEngine game = room();
      for (int p = 2; p < 6; p++) assertTrue(game.addPlayer("P" + p, "FR", p));
      game.mode = mode;
      for (int id : new int[] {0, 2, 3, 4, 6, 7}) {
        game.startNext(id);
        game.screen = "BET";
        assertTrue(game.placeBet(1));
        assertEquals("CREW", game.screen);
        for (int p = 1; p < game.players.size(); p++) {
          String name = game.players.get(p).name;
          assertTrue(game.crewPick(name, p % game.crewOptionCount()));
          assertFalse(game.crewPick(name, 0));
        }
        assertEquals("GAME", game.screen);
        assertEquals(game.players.size() - 1, game.crewCount());
      }
    }
  }

  @Test public void groupGamesGoStraightFromWagerToTheirSharedAction() {
    for (int id : new int[] {1, 5, 8, 9}) {
      GameEngine game = room();
      game.startNext(id);
      game.screen = "BET";
      assertTrue(game.placeBet(2));
      assertEquals("GAME", game.screen);
      assertEquals(2, game.wager);
      assertEquals(-1, game.predictions[1]);
    }
  }

  @Test public void turboWaitsForSecretRuleBeforeStartingNextChallenge() {
    GameEngine game = room();
    game.mode = "TURBO";
    game.startNext(0);
    game.screen = "GAME";
    game.deadline = System.currentTimeMillis() + 20_000;
    game.finish(true);
    assertEquals("A", game.ruleOwner);
    game.advance();
    assertEquals("RULE_PICK", game.screen);
    assertTrue(game.chooseRule("A", game.ruleOffers[0]));
    assertEquals("TRANSITION", game.screen);
  }

  @Test public void juryTimeoutUsesTheVotesAlreadyCast() {
    GameEngine game = room();
    game.startNext(1);
    game.screen = "GAME";
    assertTrue(game.beginJury());
    assertTrue(game.castJury("B", true));
    game.deadline = System.currentTimeMillis() - 1;
    game.checkTimeout();
    assertEquals("RESULT", game.screen);
    assertTrue(game.lastWon);
  }

  @Test public void missingVoteAbstainsAndThePartyContinuesAfterRestore() {
    GameEngine game = room();
    game.mode = "VOTE";
    game.begin();
    assertEquals("VOTE", game.screen);
    assertTrue(game.castVote("A", 1));
    assertFalse(game.voteTimedOut(game.deadline - 1));
    GameEngine restored = new GameEngine();
    restored.restore(game.json());
    assertTrue(restored.voteTimedOut(restored.deadline + 1));
    assertEquals("TRANSITION", restored.screen);
    assertEquals(3, restored.votes[1]);
    assertEquals(1, restored.voteWinner);
    assertFalse(restored.voteTimedOut(restored.deadline + 1));
  }

  @Test public void unansweredSecretRuleCannotFreezeTurbo() {
    GameEngine game = room();
    game.mode = "TURBO";
    game.ruleOwner = "A";
    game.ruleOffers = new int[] {3, 5, 7};
    game.startSelection();
    assertEquals("RULE_PICK", game.screen);
    assertFalse(game.rulePickTimedOut(game.deadline - 1));
    assertTrue(game.rulePickTimedOut(game.deadline + 1));
    assertEquals(3, game.ruleId);
    assertEquals("TRANSITION", game.screen);
  }

  @Test public void voteTimeoutAlsoResolvesAnUnansweredSecretRule() {
    GameEngine game = room();
    game.mode = "VOTE";
    game.ruleOwner = "B";
    game.ruleOffers = new int[] {4, 6, 8};
    game.begin();
    assertTrue(game.castVote("A", 2));
    assertTrue(game.voteTimedOut(game.deadline + 1));
    assertEquals(4, game.ruleId);
    assertEquals(3, game.votes[1]);
    assertEquals(2, game.voteWinner);
    assertEquals("TRANSITION", game.screen);
  }

  @Test public void sharedPhoneCanSkipAbsentVoterWithoutFabricatingAVote() {
    GameEngine game = room();
    game.mode = "VOTE";
    game.begin();
    assertTrue(game.castVote("A", 1));
    assertTrue(game.skipParticipation("B"));
    assertEquals("TRANSITION", game.screen);
    assertEquals(3, game.votes[1]);
    assertEquals(1, game.voteWinner);
    assertFalse(game.skipParticipation("B"));
  }

  @Test public void skippedCrewJuryAndDrawingAnswersNeverEarnPoints() {
    GameEngine game = room();
    game.startNext(0);
    game.screen = "BET";
    assertTrue(game.placeBet(1));
    assertTrue(game.skipParticipation("B"));
    assertEquals("GAME", game.screen);
    assertEquals(0, game.crewCount());
    assertFalse(game.crewPick("B", 0));
    game.finish(true);
    assertEquals(0, game.players.get(1).score);

    game.startNext(1);
    game.screen = "GAME";
    assertTrue(game.beginJury());
    assertTrue(game.skipParticipation("B"));
    assertTrue(game.juryComplete());
    assertFalse(game.juryVerdict());

    game.startNext(5);
    game.screen = "GAME";
    assertTrue(game.beginDrawGuess());
    assertTrue(game.skipParticipation("B"));
    assertTrue(game.drawGuessComplete());
    assertFalse(game.drawWin());
    assertEquals(0, game.drawCorrectCount());
  }

  @Test public void skippedPredictionCannotEarnPointsOrCauseASip() {
    GameEngine game = room();
    game.startNext(0);
    game.screen = "PREDICT";
    game.predictions = new int[] {2, -1};
    assertTrue(game.skipParticipation("B"));
    assertEquals("GAME", game.screen);
    game.finish(false);
    assertEquals(0, game.players.get(1).score);
    assertEquals(0, game.players.get(1).sips);
  }

  @Test public void virtualClockDrivesProductionDeadlines() {
    java.util.concurrent.atomic.AtomicLong now = new java.util.concurrent.atomic.AtomicLong(100_000);
    GameEngine game = new GameEngine(5, now::get);
    assertTrue(game.addPlayer("A", "FR", 0));
    assertTrue(game.addPlayer("B", "EN", 1));
    game.begin();
    assertEquals(118_000, game.deadline);
    now.set(118_001);
    assertTrue(game.voteTimedOut(now.get()));
    assertEquals("TRANSITION", game.screen);
    assertEquals(3, game.votes[0]);
    assertEquals(3, game.votes[1]);
  }

  @Test public void crewDeadlineRenewsAfterEachSharedPhoneContribution() {
    java.util.concurrent.atomic.AtomicLong now = new java.util.concurrent.atomic.AtomicLong(100_000);
    GameEngine game = new GameEngine(7, now::get);
    assertTrue(game.addPlayer("A", "FR", 0));
    assertTrue(game.addPlayer("B", "EN", 1));
    assertTrue(game.addPlayer("C", "FR", 2));
    game.mode = "TURBO";
    game.startNext(0);
    game.screen = "BET";
    assertTrue(game.placeBet(1));
    assertEquals(108_000, game.deadline);
    now.set(107_500);
    assertTrue(game.crewPick("B", 0));
    assertEquals(115_500, game.deadline);
    assertFalse(game.crewTimedOut());
    now.set(114_000);
    assertTrue(game.skipParticipation("C"));
    assertEquals("GAME", game.screen);
    assertEquals(1, game.crewCount());
  }
}
