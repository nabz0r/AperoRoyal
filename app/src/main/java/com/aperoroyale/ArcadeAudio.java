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
  private volatile int scene = 0;
  private volatile int style = 0;
  private volatile float volume = .5f;
  private volatile long duckUntil = 0;
  private final ExecutorService effects = Executors.newFixedThreadPool(2);
  private final AtomicLong lastClick = new AtomicLong(0);
  private Thread loop;
  private static final int RATE = 22050;
  private static final int STEP_MS = 250;
  private static final int[][] SCORE = {
    {64, 67, 71, 67, 62, 67, 69, 67, 64, 67, 71, 76, 74, 71, 69, 67},
    {69, 72, 76, 72, 67, 72, 79, 72, 69, 72, 76, 81, 79, 76, 72, 67},
    {72, 76, 79, 84, 79, 76, 74, 79, 72, 76, 79, 86, 84, 79, 76, 74}
  };
  private static final int[][] BASS = {
    {40, 40, 45, 43}, {45, 45, 43, 47}, {48, 48, 50, 47}
  };
  private static final int[][] MELODIES = {
    {64, 67, 71, 76, 71, 67, 64, 59},
    {60, 64, 67, 72, 67, 64, 60, 55},
    {57, 60, 64, 69, 72, 69, 64, 60},
    {62, 65, 69, 74, 69, 65, 62, 57},
    {59, 62, 66, 71, 74, 71, 66, 62},
    {65, 69, 72, 77, 72, 69, 65, 60}
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
                  if (enabled) {
                    double lead = frequency(SCORE[currentScene][step % 16]);
                    double bass = frequency(BASS[currentScene][(step / 4) % 4]);
                    boolean kickStep = step % 4 == 0;
                    boolean snareStep = step % 4 == 2;
                    boolean hatStep = step % 2 == 1;
                    for (int i = 0; i < pcm.length; i++) {
                      double time = (step * (long) samplesPerStep + i) / (double) RATE;
                      double within = i / (double) RATE;
                      double attack = Math.min(1, within / .009);
                      double leadEnvelope = attack * Math.exp(-9 * within);
                      double leadWave = Math.sin(2 * Math.PI * lead * time)
                          + .19 * Math.sin(4 * Math.PI * lead * time);
                      double bassWave = Math.sin(2 * Math.PI * bass * time)
                          + .24 * Math.sin(4 * Math.PI * bass * time);
                      double pad = Math.sin(2 * Math.PI * frequency(BASS[currentScene][(step / 4) % 4] + 19) * time)
                          + .6 * Math.sin(2 * Math.PI * frequency(BASS[currentScene][(step / 4) % 4] + 24) * time);
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
                      double activity = currentScene == 2 ? 1 : currentScene == 1 ? .72 : .55;
                      double sample = (bassWave * .083
                          + pad * (style == 0 ? .018 : .013)
                          + leadWave * leadEnvelope * (style == 0 ? .038 : .060)
                          + kick * (style == 0 ? .055 : .095) * activity
                          + snare * (style == 0 ? .025 : .048) * activity
                          + hat * (style == 0 ? .012 : .028) * activity) * duck * volume;
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

  public boolean enabled() {
    return enabled;
  }

  public void setScene(String screen) {
    scene = "GAME".equals(screen) ? 2 : ("VOTE".equals(screen) || "LIBRARY".equals(screen) ? 1 : 0);
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
    effects.execute(() -> playNote(79, 45, 0.13f));
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

  public void tune(int index) {
    duck(3000);
    effects.execute(() -> {
      for (int n : MELODIES[Math.floorMod(index, MELODIES.length)]) playNote(n, 270, 0.18f);
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
