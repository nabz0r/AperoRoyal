package com.aperoroyale;

import static org.junit.Assert.*;
import org.junit.Test;

public final class SocialRoundTest {
  private GameEngine table() {
    GameEngine g = new GameEngine(1206);
    assertTrue(g.addPlayer("Ari", "FR", 0));
    assertTrue(g.addPlayer("Bee", "EN", 0));
    assertTrue(g.addPlayer("Cam", "FR", 11));
    return g;
  }

  @Test public void menuLanguagePersistsLocallyWhileActiveTurnsKeepTheirOwnLanguage() {
    GameEngine g = table();
    assertEquals(0, g.players.get(1).avatar); // Friends may choose the same portrait.
    assertEquals(11, g.players.get(2).avatar);
    g.menuLanguage = "EN";
    g.screen = "HOME";
    assertTrue(g.english());
    g.screen = "GAME";
    assertFalse(g.english());
    g.active = 1;
    assertTrue(g.english());
    g.screen = "SETTINGS";
    g.menuLanguage = "FR";
    assertFalse(g.english());
  }

  @Test public void aPersonCanDeclineAPerformanceWithoutDrinksPointsOrLeaderboardResult() {
    GameEngine g = table();
    g.startNext(8);
    g.screen = "GAME";
    assertFalse(g.passPerformance("Bee"));
    assertTrue(g.passPerformance("Ari"));
    g.finish(false);
    assertEquals("RESULT", g.screen);
    assertEquals(0, g.players.get(0).games);
    assertEquals(0, g.players.get(0).drinks);
    assertEquals(0, g.players.get(0).score);
    assertEquals(0, g.roundSips);
    assertTrue(RoundStories.forRound(g, false)[2].contains("Ni point"));
    GameEngine restored = new GameEngine(1207);
    restored.restore(g.json());
    assertTrue(restored.roundPassed);
    assertEquals(0, restored.players.get(0).drinks);
  }

  @Test public void quizRevealsTheRealContributorOnlyAfterResolution() {
    GameEngine g = table();
    g.startNext(0);
    g.screen = "BET";
    assertTrue(g.placeBet(1));
    assertTrue(g.crewPick("Bee", g.target));
    assertTrue(g.crewPick("Cam", (g.target + 1) % 4));
    assertTrue(g.lockAnswer(g.target));
    assertFalse(g.lockAnswer((g.target + 1) % 4));
    g.finish(true);
    String[] story = RoundStories.forRound(g, true);
    assertEquals(3, story.length);
    assertTrue(story[2].contains("Bee"));
    assertFalse(story[2].contains("Cam"));
  }

  @Test public void bombRelayOrderSurvivesSaveAndCanBeRecounted() {
    GameEngine g = table();
    g.startNext(9);
    g.variant = 0;
    g.screen = "GAME";
    assertTrue(g.bombTap("Ari"));
    assertTrue(g.bombPass("Ari", 2));
    GameEngine restored = new GameEngine(1208);
    restored.restore(g.json());
    assertArrayEquals(new int[] {0, 2}, restored.bombRoute);
    restored.finish(false);
    assertTrue(RoundStories.forRound(restored, false)[1].contains("Ari → Cam"));
  }

  @Test public void everyMiniGameHasAFrenchAndEnglishRevealEvenOnTimeout() {
    GameEngine g = table();
    for (int game = 0; game < 10; game++) {
      g.startNext(game);
      g.screen = "GAME";
      g.finish(false);
      String[] fr = RoundStories.forRound(g, false);
      String[] en = RoundStories.forRound(g, true);
      assertEquals(3, fr.length);
      assertEquals(3, en.length);
      for (int i = 0; i < 3; i++) {
        assertFalse("FR game " + game, fr[i].isEmpty());
        assertFalse("EN game " + game, en[i].isEmpty());
      }
    }
  }
}
