package com.aperoroyale;

/** One host-time beat grid for the visual target, local cue and score validation. */
final class RhythmClock {
  static final int PERIOD_MS = 600;
  static final int TARGET_MS = 300;

  private RhythmClock() { }

  static int beat(long started, long now) {
    long elapsed = now - started;
    return elapsed < 0 ? -1 : (int) (elapsed / PERIOD_MS);
  }

  static int phase(long started, long now) {
    long elapsed = now - started;
    return elapsed < 0 ? -1 : (int) (elapsed % PERIOD_MS);
  }

  static boolean accepts(long started, long now, int claimedBeat, boolean remote) {
    int phase = phase(started, now);
    int tolerance = remote ? 235 : 175;
    return claimedBeat >= 0 && claimedBeat < 40 && claimedBeat == beat(started, now)
        && Math.abs(phase - TARGET_MS) <= tolerance;
  }
}
