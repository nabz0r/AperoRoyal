package com.aperoroyale;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/** Original continuous arcade score, mixed by Android with game effects and external audio. */
public final class ArcadeAudio {
  private final AtomicBoolean running = new AtomicBoolean(false);
  private volatile boolean enabled = true;
  private volatile boolean suspended = false;
  private volatile boolean externalRadio = false;
  private volatile int scene = 0;
  private volatile int gameTheme = 0;
  private volatile int style = 0;
  private volatile float volume = .5f;
  private volatile long duckUntil = 0;
  private final ExecutorService effects = Executors.newFixedThreadPool(2);
  private final AtomicLong lastClick = new AtomicLong(0);
  private Thread loop;
  private static final int RATE = 22050;
  private static final int STEP_MS = 250;
  private static final int[] THEME_SHIFT = {0, 5, -2, 7, -5, 2, 9, 0, 4, -3};
  private static final int[][] SCORE = {
    {0, 0, 64, 0, 0, 67, 0, 0, 71, 0, 67, 0, 0, 0, 62, 0,
     0, 0, 64, 0, 0, 69, 0, 0, 71, 0, 0, 74, 71, 0, 0, 0},
    {0, 69, 0, 0, 72, 0, 0, 76, 0, 0, 72, 0, 79, 0, 0, 0,
     0, 69, 0, 0, 72, 0, 0, 81, 0, 0, 76, 0, 72, 0, 0, 0},
    {72, 0, 0, 79, 0, 76, 0, 0, 84, 0, 0, 79, 0, 76, 0, 0,
     74, 0, 0, 79, 0, 86, 0, 0, 84, 0, 79, 0, 76, 0, 74, 0}
  };
  private static final int[][] BASS = {
    {40, 40, 45, 43, 40, 43, 45, 47},
    {45, 45, 43, 47, 45, 48, 43, 47},
    {48, 48, 50, 47, 48, 52, 50, 47}
  };
  private static final int[][] MELODIES = {
    {60, 62, 64, 65, 67, 69, 71, 72},
    {76, 74, 72, 71, 69, 67, 65, 64},
    {60, 72, 60, 72, 62, 74, 62, 74},
    {67, 0, 67, 0, 72, 0, 72, 0},
    {60, 60, 0, 65, 65, 0, 67, 67},
    {64, 0, 67, 69, 0, 72, 69, 64}
  };

  public void start() {
    if (running.getAndSet(true)) return;
    loop =
        new Thread(
            () -> {
              int samplesPerStep = RATE * STEP_MS / 1000;
              AudioTrack music = null;
              try {
                music = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(samplesPerStep * 8)
                    .setTransferMode(AudioTrack.MODE_STREAM).build();
                music.play();
                int step = 0;
                while (running.get()) {
                  int currentScene = scene;
                  short[] pcm = new short[samplesPerStep];
                  if (enabled && !suspended && !externalRadio) {
                    int theme = gameTheme;
                    int phrase = (step / 64) % 4;
                    int shift = currentScene == 2 ? THEME_SHIFT[Math.floorMod(theme, 10)] : 0;
                    int note = SCORE[currentScene][step % 32];
                    // Menus breathe; a short tune or a friend's voice must own the foreground.
                    if (currentScene != 2 && step % 16 != 4 && step % 16 != 12) note = 0;
                    if (currentScene == 2 && (theme == 2 || theme == 7 || step % 2 != 0)) note = 0;
                    if (note != 0) note += shift + (phrase == 1 ? 12 : phrase == 3 ? -5 : 0);
                    double lead = note == 0 ? 0 : frequency(note);
                    int root = BASS[currentScene][(step / 4) % 8] + shift
                        + (phrase == 2 ? 5 : 0);
                    double bass = frequency(root);
                    boolean kickStep = currentScene == 2 && step % 8 == 0;
                    boolean snareStep = currentScene == 2 && step % 8 == 4;
                    boolean hatStep = currentScene == 2 && style == 1
                        && (theme == 3 || theme == 9) && step % 2 == 1;
                    for (int i = 0; i < pcm.length; i++) {
                      double time = (step * (long) samplesPerStep + i) / (double) RATE;
                      double within = i / (double) RATE;
                      double attack = Math.min(1, within / .009);
                      double leadEnvelope = note == 0 ? 0 : attack * Math.exp(-11 * within);
                      double leadWave = Math.sin(2 * Math.PI * lead * time)
                          + .19 * Math.sin(4 * Math.PI * lead * time);
                      double bassWave = Math.sin(2 * Math.PI * bass * time)
                          + .24 * Math.sin(4 * Math.PI * bass * time);
                      double pad = Math.sin(2 * Math.PI * frequency(root + 19) * time)
                          + .6 * Math.sin(2 * Math.PI * frequency(root + 24) * time);
                      double arpeggio = step % 4 == 3
                          ? Math.sin(2 * Math.PI * frequency(root + 31) * time)
                              * Math.exp(-21 * within) : 0;
                      double kick = 0;
                      if (kickStep && within < .22) {
                        double sweep = 53 + 90 * Math.exp(-30 * within);
                        kick = Math.sin(2 * Math.PI * sweep * within) * Math.exp(-19 * within);
                      }
                      int noiseBits = (int) ((step * 1315423911L + i * 2654435761L) ^ (i << 7));
                      noiseBits ^= noiseBits >>> 13;
                      double noise = (noiseBits & 1023) / 511.5 - 1;
                      double snare = snareStep && within < .16 ? noise * Math.exp(-28 * within) : 0;
                      double hat = hatStep && within < .07 ? noise * Math.exp(-55 * within) : 0;
                      double duck = System.currentTimeMillis() < duckUntil ? .17 : 1;
                      double activity = currentScene == 2 ? .62 : currentScene == 1 ? .32 : .22;
                      if (currentScene == 2 && (theme == 2 || theme == 7 || theme == 8))
                        activity = .14;
                      double sample = (bassWave * .052 * Math.exp(-1.7 * within)
                          + pad * (style == 0 ? .017 : .013)
                          + leadWave * leadEnvelope * (style == 0 ? .019 : .031)
                          + arpeggio * (style == 0 ? .006 : .012)
                          + kick * (style == 0 ? .040 : .067) * activity
                          + snare * (style == 0 ? .011 : .025) * activity
                          + hat * .008 * activity) * duck * volume;
                      pcm[i] = (short) (Math.max(-1, Math.min(1, sample)) * Short.MAX_VALUE);
                    }
                  }
                  music.write(pcm, 0, pcm.length);
                  step++;
                }
              } catch (Exception ignored) {
              } finally {
                if (music != null) {
                  try { music.stop(); } catch (Exception ignored) { }
                  music.release();
                }
              }
            },
            "arcade-audio");
    loop.setDaemon(true);
    loop.start();
  }

  public void stop() {
    running.set(false);
    effects.shutdownNow();
  }

  public void setEnabled(boolean b) {
    enabled = b;
  }

  public void setSuspended(boolean value) { suspended = value; }
  public void setExternalRadio(boolean value) { externalRadio = value; }

  public boolean enabled() {
    return enabled;
  }

  public void setScene(String screen, int game) {
    scene = "GAME".equals(screen) ? 2
        : ("VOTE".equals(screen) || "LIBRARY".equals(screen) || "CREW".equals(screen) ? 1 : 0);
    gameTheme = Math.floorMod(game, 10);
  }

  public int style() { return style; }
  public void setStyle(int value) { style = value == 1 ? 1 : 0; }
  public float volume() { return volume; }
  public void setVolume(float value) { volume = Math.max(.1f, Math.min(1f, value)); }

  public void duck(int millis) { duckUntil = System.currentTimeMillis() + millis; }

  private static double frequency(int midi) {
    return 440 * Math.pow(2, (midi - 69) / 12.0);
  }

  public void click() {
    long now = System.currentTimeMillis();
    if (now - lastClick.getAndSet(now) < 45) return;
    effects.execute(() -> playNote(79, 42, 0.08f));
  }

  public void turn() {
    effects.execute(() -> {
      playNote(60, 80, .12f);
      playNote(67, 80, .12f);
      playNote(72, 140, .14f);
    });
  }

  public void beat() {
    if (running.get() && !suspended) effects.execute(() -> playNote(82, 55, .22f));
  }

  public void win() {
    effects.execute(() -> {
      for (int n : new int[] {72, 76, 79, 84}) playNote(n, 105, 0.16f);
    });
  }

  public void lose() {
    effects.execute(() -> {
      for (int n : new int[] {60, 57, 53, 48}) playNote(n, 120, 0.14f);
    });
  }

  /** A compact game-specific punctuation, with room for the table to react. */
  public void result(int game, boolean won) {
    int shift = THEME_SHIFT[Math.floorMod(game, THEME_SHIFT.length)];
    duck(1500);
    effects.execute(() -> {
      int[] phrase = won ? new int[] {60, 67, 72} : new int[] {60, 57, 53};
      for (int i = 0; i < phrase.length; i++)
        playNote(phrase[i] + shift, i == 2 ? 155 : 86, i == 2 ? .15f : .11f);
    });
  }

  public void tune(int index) {
    duck(3000);
    effects.execute(() -> {
      int[] keys = {0, 3, 5, 7};
      int key = keys[Math.floorMod(index / MELODIES.length, keys.length)];
      for (int n : MELODIES[Math.floorMod(index, MELODIES.length)]) {
        if (n == 0) sleep(270);
        else playNote(n + key, 270, 0.18f);
      }
    });
  }

  private static void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  private static void playNote(int midi, int millis, float volume) {
    int count = RATE * millis / 1000;
    short[] samples = new short[count];
    double freq = 440 * Math.pow(2, (midi - 69) / 12.0);
    for (int i = 0; i < count; i++) {
      double phase = 2 * Math.PI * freq * i / RATE;
      double warm = Math.sin(phase) + .22 * Math.sin(2 * phase);
      double envelope = Math.min(1, i / (RATE * .008))
          * Math.min(1, (count - i) / (RATE * .045));
      samples[i] = (short) (warm * envelope * volume * .76 * Short.MAX_VALUE);
    }
    AudioTrack track = null;
    try {
      track =
          new AudioTrack.Builder()
              .setAudioAttributes(
                  new AudioAttributes.Builder()
                      .setUsage(AudioAttributes.USAGE_GAME)
                      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                      .build())
              .setAudioFormat(
                  new AudioFormat.Builder()
                      .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                      .setSampleRate(RATE)
                      .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                      .build())
              .setBufferSizeInBytes(count * 2)
              .setTransferMode(AudioTrack.MODE_STATIC)
              .build();
      track.write(samples, 0, count);
      track.play();
      sleep(millis + 25);
    } catch (Exception ignored) {
    } finally {
      if (track != null) {
        try {
          track.stop();
        } catch (Exception ignored) {
        }
        track.release();
      }
    }
  }
}
