package com.aperoroyale;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Test;

/** Replays actual quiz and music rounds with variable party size and private peer snapshots. */
public final class SocialFeatureStressTest {
  @Test public void trustAndRoomLeadStayConsistentAcrossTenThousandMixedRounds()
      throws Exception {
    int rounds = Integer.parseInt(System.getenv().getOrDefault("APERO_SOCIAL_ROUNDS", "300"));
    if (rounds < 50 || rounds > 100_000) throw new IllegalArgumentException("50..100000 rounds");
    int trusted = 0, wins = 0, snapshots = 0;
    for (int id = 0; id < rounds; id++) {
      GameEngine host = new GameEngine(20261005L + id);
      int players = 2 + id % 5;
      for (int i = 0; i < players; i++)
        assertTrue(host.addPlayer("P" + i, i % 2 == 0 ? "FR" : "EN", i));
      host.startNext(id % 2 == 0 ? 0 : 2);
      host.screen = "BET";
      assertTrue(host.placeBet(1 + id % 3));
      int answer = host.target;
      for (int i = 1; i < players; i++) {
        int pick = (answer + ((id + i) % 3 == 0 ? 0 : 1)) % 4;
        assertTrue(host.crewPick("P" + i, pick));
      }
      assertEquals("GAME", host.screen);
      int lead = host.crewLead();
      for (int i = 0; i < players; i++) {
        JSONObject privateView = host.networkJsonFor("P" + i);
        GameEngine peer = new GameEngine();
        peer.restore(privateView);
        assertEquals(lead, peer.crewLead());
        assertEquals(host.featuredFriend(), peer.featuredFriend());
        for (int j = 1; j < players; j++)
          assertEquals(2, privateView.getJSONArray("crewChoices").getInt(j));
        snapshots++;
      }
      boolean followFriend = id % 3 == 0;
      if (followFriend) {
        assertTrue(host.trustFriend("P0", host.featuredFriend()));
        trusted++;
      } else if (lead >= 0 && id % 3 == 1) {
        assertTrue(host.lockAnswer(lead));
      } else {
        assertTrue(host.lockAnswer((answer + id % 2) % 4));
      }
      boolean won = host.selected == answer;
      host.finish(won);
      if (won) wins++;
      assertEquals("RESULT", host.screen);
      assertEquals(won, host.lastWon);
      if (followFriend && won) assertTrue(host.crewPoints[1] >= 25);
      assertFalse(RoundStories.forRound(host, false)[0].isEmpty());
      assertFalse(RoundStories.forRound(host, true)[0].isEmpty());
      GameEngine resumed = new GameEngine();
      resumed.restore(host.json());
      assertEquals(host.roundPoints, resumed.roundPoints);
      assertEquals(host.crewPoints[1], resumed.crewPoints[1]);
      assertEquals(host.trustedFriend, resumed.trustedFriend);
    }
    System.out.println("SOCIAL FEATURE STRESS: rounds=" + rounds + " trusted=" + trusted
        + " wins=" + wins + " private_snapshots=" + snapshots
        + " languages=FR/EN players=2..6 games=quiz/music");
  }
}
