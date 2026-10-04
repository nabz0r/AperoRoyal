package com.aperoroyale;

import static org.junit.Assert.*;
import org.junit.Test;

public final class RhythmClockTest {
  @Test public void acceptsOnlyTheClaimedBeatNearItsCenter() {
    long start = 10_000;
    assertTrue(RhythmClock.accepts(start, start + 300, 0, false));
    assertTrue(RhythmClock.accepts(start, start + 900, 1, false));
    assertFalse(RhythmClock.accepts(start, start + 300, 1, false));
    assertFalse(RhythmClock.accepts(start, start + 100, 0, false));
    assertFalse(RhythmClock.accepts(start, start + 520, 0, false));
    assertFalse(RhythmClock.accepts(start, start - 1, 0, false));
  }

  @Test public void remoteWindowAbsorbsModestDelayButRejectsAFreePoint() {
    long start = 42_000;
    assertTrue(RhythmClock.accepts(start, start + 510, 0, true));
    assertFalse(RhythmClock.accepts(start, start + 550, 0, true));
    assertFalse(RhythmClock.accepts(start, start + 1_110, 0, true));
  }
}
