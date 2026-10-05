package com.aperoroyale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public final class AudiencePacingTest {
  private GameEngine table(int people, AtomicLong now) {
    GameEngine room = new GameEngine(1717, now::get);
    for (int i = 0; i < people; i++)
      assertTrue(room.addPlayer("P" + i, i % 2 == 0 ? "FR" : "EN", i));
    return room;
  }

  @Test public void galleryClosesAfterQuorumAndGraceWithoutPunishingMissingGuesses() {
    AtomicLong now = new AtomicLong(1_000_000L);
    GameEngine room = table(5, now);
    room.startNext(5);
    room.screen = "GAME";
    assertTrue(room.beginDrawGuess());
    assertTrue(room.drawGuess("P1", room.target));
    assertEquals(0, room.audienceClosingAt);
    assertTrue(room.drawGuess("P2", room.target));
    assertEquals(now.get() + 7000, room.audienceClosingAt);
    assertEquals(room.audienceClosingAt, room.visibleDeadline());
    assertFalse(room.drawGuessComplete());
    assertTrue(room.drawWin());
    GameEngine restored = table(5, now);
    restored.restore(room.json());
    assertEquals(room.audienceClosingAt, restored.audienceClosingAt);
    now.addAndGet(6999);
    assertFalse(restored.audienceReadyToClose());
    now.incrementAndGet();
    assertTrue(restored.audienceReadyToClose());
    assertFalse(restored.drawGuess("P3", restored.target));
    restored.finish(restored.drawWin());
    assertEquals("RESULT", restored.screen);
    assertTrue(restored.lastWon);
    assertEquals(0, restored.audienceClosingAt);
  }

  @Test public void juryUsesExpressedVotesAndWaitsForTheGracePeriod() {
    AtomicLong now = new AtomicLong(2_000_000L);
    GameEngine room = table(6, now);
    room.startNext(1);
    room.screen = "GAME";
    assertTrue(room.beginJury());
    assertTrue(room.castJury("P1", true));
    assertTrue(room.castJury("P2", true));
    assertEquals(0, room.audienceClosingAt);
    assertTrue(room.castJury("P3", false));
    assertEquals(now.get() + 7000, room.audienceClosingAt);
    assertFalse(room.juryComplete());
    assertTrue(room.juryVerdict());
    now.addAndGet(7000);
    assertTrue(room.audienceReadyToClose());
    assertFalse(room.castJury("P4", true));
  }

  @Test public void duoNeverWaitsForAQuorumGraceAfterItsOnlyGuestVotes() {
    AtomicLong now = new AtomicLong(3_000_000L);
    GameEngine room = table(2, now);
    room.startNext(5);
    room.screen = "GAME";
    assertTrue(room.beginDrawGuess());
    assertTrue(room.drawGuess("P1", room.target));
    assertTrue(room.drawGuessComplete());
    assertEquals(0, room.audienceClosingAt);
  }

  @Test public void blindTestGetsListeningTimeAndTurboEasesForTwoPlayers() {
    AtomicLong now = new AtomicLong(4_000_000L);
    GameEngine duo = table(2, now);
    duo.mode = "TURBO";
    duo.startNext(2);
    duo.screen = "GAME";
    duo.bonusId = 0;
    duo.resumeGame();
    assertEquals(26_000L, duo.deadline - duo.started);
    GameEngine group = table(6, now);
    group.mode = "TURBO";
    group.startNext(2);
    group.screen = "GAME";
    group.bonusId = 0;
    group.resumeGame();
    assertEquals(24_000L, group.deadline - group.started);
  }

  @Test public void tableSparksAreOccasionalAndAvailableInBothLanguages() {
    AtomicLong now = new AtomicLong(5_000_000L);
    GameEngine room = table(2, now);
    room.mode = "TURBO";
    room.turn = 2;
    assertTrue(RoundStories.hasTableSpark(room));
    for (int game = 0; game < 10; game++) {
      room.game = game;
      assertFalse(RoundStories.tableSpark(room, false).isEmpty());
      assertFalse(RoundStories.tableSpark(room, true).isEmpty());
      assertFalse(RoundStories.handoffSpark(room, false).isEmpty());
      assertFalse(RoundStories.handoffSpark(room, true).isEmpty());
    }
    room.turn = 3;
    assertFalse(RoundStories.hasTableSpark(room));
  }
}
