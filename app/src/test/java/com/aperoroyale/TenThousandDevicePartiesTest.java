package com.aperoroyale;

import static org.junit.Assert.*;

import java.util.Random;
import org.json.JSONObject;
import org.junit.Test;

/** Deterministic host and per-recipient device snapshots across 10,000 mixed-language rooms. */
public final class TenThousandDevicePartiesTest {
  @Test public void secretContinuesAfterResultAndRemixesExistingRoomRule() throws Exception {
    GameEngine host = new GameEngine(17);
    host.newParty();
    assertTrue(host.addPlayer("Host", "FR", 0));
    assertTrue(host.addPlayer("Guest", "EN", 1));
    host.mode = "FREE";
    host.begin();
    host.startNext(5);
    host.screen = "GAME";
    host.ruleId = 4;
    assertTrue(host.hiddenWaiting("Guest"));
    for (int p = 0; p < 3; p++) assertTrue(host.hiddenTap("Guest", -1));
    int discovered = host.hiddenKind[1];
    assertTrue(discovered >= 0 && discovered < 3);
    int first = host.hiddenTarget(1);
    assertTrue(host.hiddenTap("Guest", first));
    host.finish(false);
    assertEquals("RESULT", host.screen);
    assertTrue(host.hiddenWaiting("Guest"));
    GameEngine restored = new GameEngine(18);
    restored.restore(new JSONObject(host.json().toString()));
    for (int p = 1; p < 4; p++)
      assertTrue(restored.hiddenTap("Guest", restored.hiddenTarget(1)));
    assertEquals(1 << discovered, restored.hiddenWonMask[1]);
    assertEquals(restored.turn, restored.hiddenSolvedTurn[1]);
    assertFalse(restored.hiddenTap("Guest", -1)); // One discovery per waiting turn.
    assertEquals(4, restored.ruleId); // The current round keeps its original rule.
    assertEquals("Guest", restored.queuedRuleOwner);
    assertFalse(restored.hiddenTap("Host", -1));
    restored.advance();
    assertEquals("LIBRARY", restored.screen);
    assertEquals(-1, restored.ruleId);
    assertEquals("Guest", restored.ruleOwner);
    assertTrue(restored.chooseRule("Guest", restored.ruleOffers[0]));
    assertTrue(restored.ruleId >= 0);
  }

  @Test public void tenThousandMultiDevicePartiesKeepSecretsPrivateAndTurnsMoving()
      throws Exception {
    int[] games = new int[10], secrets = new int[3];
    int rounds = 0, privateSnapshots = 0, forbiddenInputs = 0, restores = 0;
    for (int party = 0; party < 10_000; party++) {
      int count = 2 + party % 5;
      Random input = new Random(0x51EC2026L + party);
      GameEngine host = new GameEngine(0xA9E2026L + party);
      host.newParty();
      for (int p = 0; p < count; p++)
        assertTrue(host.addPlayer("D" + p, p % 2 == 0 ? "FR" : "EN", p));
      host.mode = switch (party % 3) {
        case 0 -> "VOTE";
        case 1 -> "FREE";
        default -> "TURBO";
      };
      host.begin();
      for (int round = 0; round < 4; round++) {
        if (!host.ruleOwner.isEmpty() && host.ruleId < 0)
          assertTrue(host.chooseRule(host.ruleOwner, host.ruleOffers[0]));
        int actor = host.active;
        if ("VOTE".equals(host.screen)) {
          for (int p = 0; p < count; p++)
            assertTrue(host.castVote("D" + p, input.nextInt(3)));
        } else if ("LIBRARY".equals(host.screen)) host.startNext((party + round) % 10);
        assertEquals("TRANSITION", host.screen);
        games[host.game]++;
        host.enterGame();
        host.readyTurn();
        assertTrue(host.placeBet(1 + input.nextInt(3)));
        if ("CREW".equals(host.screen))
          for (int p = 0; p < count; p++) if (p != actor)
            assertTrue(host.crewPick("D" + p, input.nextInt(host.crewOptionCount())));
        assertEquals("GAME", host.screen);
        int guest = (actor + 1) % count;
        String guestName = "D" + guest;
        assertFalse(host.hiddenTap("D" + actor, -1));
        forbiddenInputs++;
        if (host.game == 9) {
          assertFalse(host.hiddenTap(guestName, -1));
          forbiddenInputs++;
        } else if (host.hiddenWonMask[guest] != 7) {
          assertTrue(host.hiddenWaiting(guestName));
          assertFalse(host.hiddenTap(guestName, 0));
          forbiddenInputs++;
          for (int probe = 0; probe < 3; probe++) assertTrue(host.hiddenTap(guestName, -1));
          int kind = host.hiddenKind[guest];
          assertTrue(kind >= 0 && kind < 3);
          int wrong = (host.hiddenTarget(guest) + 1) % 4;
          assertTrue(host.hiddenTap(guestName, wrong));
          assertEquals(1, host.hiddenMistakes[guest]);
          for (int step = 0; step < 4; step++)
            assertTrue(host.hiddenTap(guestName, host.hiddenTarget(guest)));
          assertEquals(-1, host.hiddenKind[guest]);
          assertEquals(1 << kind, host.hiddenWonMask[guest] & (1 << kind));
          assertEquals(guestName, host.hiddenLastOwner);
          secrets[kind]++;
        }

        for (int p = 0; p < count; p++) {
          JSONObject privateState = host.networkJsonFor("D" + p);
          GameEngine device = new GameEngine(party + p);
          device.restore(privateState);
          assertEquals(p % 2 == 0 ? "FR" : "EN", device.players.get(p).language);
          assertEquals(host.turn, device.turn);
          assertEquals(-1, device.target);
          assertEquals(-1, device.loserCup);
          for (int other = 0; other < count; other++) if (other != p) {
            assertEquals(-1, device.hiddenKind[other]);
            assertEquals(0, device.hiddenSeed[other]);
            assertEquals(0, device.hiddenWonMask[other]);
            assertEquals(-1, device.hiddenSolvedTurn[other]);
          }
          if (p == guest && host.game != 9)
            assertEquals(host.hiddenWonMask[p], device.hiddenWonMask[p]);
          privateSnapshots++;
        }
        if (party % 25 == 0 && round == 1) {
          GameEngine recovered = new GameEngine(0x5EEDL + party);
          recovered.restore(new JSONObject(host.json().toString()));
          assertArrayEquals(host.hiddenWonMask, recovered.hiddenWonMask);
          assertArrayEquals(host.hiddenKind, recovered.hiddenKind);
          host = recovered;
          restores++;
        }
        boolean won = input.nextBoolean();
        if (host.game == 1) {
          assertTrue(host.beginJury());
          for (int p = 0; p < count; p++) if (p != actor)
            assertTrue(host.castJury("D" + p, input.nextBoolean()));
          won = host.juryVerdict();
        } else if (host.game == 5) {
          assertTrue(host.beginDrawGuess());
          for (int p = 0; p < count; p++) if (p != actor)
            assertTrue(host.drawGuess("D" + p, input.nextInt(4)));
          won = host.drawWin();
        } else if (host.game == 8) {
          assertTrue(host.chooseBluffTruth("D" + actor, input.nextBoolean()));
          for (int p = 0; p < count; p++) if (p != actor)
            assertTrue(host.castJury("D" + p, input.nextBoolean()));
          won = host.juryVerdict();
        } else if (host.game == 9) {
          for (int step = 0; step < Math.min(host.bombGoal(), 5); step++) {
            if (host.bombAwaitingPass) {
              int next = -1;
              for (int p = 0; p < count; p++) if (host.canPassBombTo(p)) { next = p; break; }
              if (next < 0) break;
              assertTrue(host.bombPass(host.players.get(host.bombNext).name, next));
            }
            assertTrue(host.bombTap(host.players.get(host.bombNext).name));
          }
        }
        host.finish(won);
        assertEquals("RESULT", host.screen);
        host.advance();
        assertEquals(round + 1, host.turn);
        assertEquals(Math.floorMod(actor + (host.ruleId == 2 ? -1 : 1), count), host.active);
        rounds++;
      }
    }
    assertEquals(40_000, rounds);
    for (int count : games) assertTrue(count > 0);
    for (int count : secrets) assertTrue(count > 0);
    assertTrue(privateSnapshots > 100_000);
    System.out.printf("DEVICE SIMULATION: 10000 parties, %d rounds, %d private FR/EN snapshots, %d secret wins [%d,%d,%d], %d forbidden inputs, %d restores%n",
        rounds, privateSnapshots, secrets[0] + secrets[1] + secrets[2],
        secrets[0], secrets[1], secrets[2], forbiddenInputs, restores);
  }
}
