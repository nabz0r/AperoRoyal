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
}
