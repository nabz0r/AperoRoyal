package com.aperoroyale;

import static org.junit.Assert.*;
import org.junit.Test;

public final class LateJoinTest {
  private GameEngine room() {
    GameEngine game = new GameEngine(14);
    assertTrue(game.addPlayer("A", "FR", 0));
    assertTrue(game.addPlayer("B", "EN", 1));
    game.begin();
    return game;
  }

  @Test public void guestWatchesCurrentRoundThenIsAdmittedByBothVotes() {
    GameEngine game = room();
    String current = game.screen;
    assertTrue(game.requestJoin("C", "EN"));
    assertEquals(current, game.screen);
    assertEquals(2, game.players.size());
    assertFalse(game.requestJoin("D", "FR"));
    game.screen = "RESULT";
    game.advance();
    assertEquals("JOIN_VOTE", game.screen);
    assertEquals(1, game.turn);
    assertTrue(game.castJoinVote("A", true));
    assertFalse(game.castJoinVote("A", false));
    assertTrue(game.castJoinVote("B", true));
    assertEquals(3, game.players.size());
    assertEquals("C", game.lastJoinName);
    assertTrue(game.lastJoinAccepted);
    assertTrue(game.joinDecisionUntil > 0);
    assertEquals("EN", game.players.get(2).language);
    assertNotEquals("JOIN_VOTE", game.screen);
    assertEquals(3, game.hiddenKind.length);
    assertEquals(-1, game.hiddenKind[2]);
  }

  @Test public void rejectionKeepsGuestOutAndAllowsRetryAtNextBreak() {
    GameEngine game = room();
    assertTrue(game.requestJoin("C", "FR"));
    game.screen = "RESULT";
    game.advance();
    assertTrue(game.castJoinVote("A", true));
    assertTrue(game.castJoinVote("B", false));
    assertFalse(game.lastJoinAccepted);
    assertTrue(game.joinDecisionUntil > 0);
    assertEquals(1, game.lastJoinSips);
    assertEquals(2, game.players.size());
    assertEquals("C", game.lastJoinName);
    assertTrue(game.requestJoin("C", "FR"));
    game.screen = "RESULT";
    game.advance();
    assertEquals("JOIN_VOTE", game.screen);
    assertTrue(game.castJoinVote("A", false));
    assertTrue(game.castJoinVote("B", false));
    assertEquals(2, game.lastJoinSips);
    assertTrue(game.requestJoin("C", "FR"));
    game.screen = "RESULT";
    game.advance();
    assertTrue(game.castJoinVote("A", true));
    assertTrue(game.castJoinVote("B", true));
    assertEquals(2, game.players.get(2).sips);
    assertEquals(2, game.players.get(2).drinks);
  }

  @Test public void pendingGuestAndVotesSurviveSessionRestore() {
    GameEngine game = room();
    assertTrue(game.requestJoin("C", "EN"));
    game.screen = "RESULT";
    game.advance();
    assertTrue(game.castJoinVote("A", true));
    GameEngine restored = new GameEngine(15);
    restored.restore(game.json());
    assertEquals("JOIN_VOTE", restored.screen);
    assertEquals("C", restored.pendingJoinName);
    assertEquals(1, restored.joinVotes[0]);
    assertEquals(-1, restored.joinVotes[1]);
    assertTrue(restored.castJoinVote("B", true));
    assertEquals(3, restored.players.size());
  }

  @Test public void networkSnapshotHidesIndividualAdmissionBallots() {
    GameEngine game = room();
    assertTrue(game.requestJoin("C", "EN"));
    game.screen = "RESULT";
    game.advance();
    assertTrue(game.castJoinVote("A", true));
    GameEngine viewer = new GameEngine(17);
    viewer.restore(game.networkJsonFor("B"));
    assertEquals("JOIN_VOTE", viewer.screen);
    assertEquals(2, viewer.joinVotes[0]);
    assertEquals(-1, viewer.joinVotes[1]);
    assertEquals("C", viewer.pendingJoinName);
  }
}
