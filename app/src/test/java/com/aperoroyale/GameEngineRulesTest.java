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

  @Test public void backingChangesTheActualRiskAndTime() {
    GameEngine game = room();
    game.startNext(0);
    game.screen = "BET";
    assertTrue(game.placeBet(2));
    assertTrue(game.predict("B", true));
    assertEquals("GAME", game.screen);
    assertEquals(1, game.supportCount());
    assertEquals(1, game.lossSips());
    assertEquals(18_000, game.deadline - game.started);
  }

  @Test public void challengeRaisesTheActorJackpotAndOwnReward() {
    GameEngine game = room();
    game.startNext(4);
    game.screen = "BET";
    assertTrue(game.placeBet(1));
    assertTrue(game.predict("B", false));
    assertEquals(1, game.challengeCount());
    assertEquals(125 + (game.bonusId == 1 ? 50 : 0), game.winPoints());
    game.ruleId = 0;
    assertEquals(225 + (game.bonusId == 1 ? 100 : 0), game.winPoints());
    game.ruleId = 2;
    game.finish(false);
    assertEquals(50, game.players.get(1).score);
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
    game.screen = "GAME";
    assertEquals(8, game.bombGoal());
    assertTrue(game.bombTap("A"));
    assertFalse(game.bombTap("B"));
    assertTrue(game.bombTap("A"));
    assertTrue(game.bombTap("B"));
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
        GameEngine.TUNES.length, GameEngine.DRAW.length, GameEngine.BLUFF.length};
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
    assertTrue(drawLimit >= 30_000 && drawLimit <= 41_000);
    game.drawingReady = true;
    game.resumeGame();
    long guessLimit = game.deadline - game.started;
    assertTrue(guessLimit >= 12_000 && guessLimit <= 23_000);
    assertTrue(guessLimit < drawLimit);
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
}
