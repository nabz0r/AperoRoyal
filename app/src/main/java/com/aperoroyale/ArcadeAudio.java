package com.aperoroyale;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.atomic.AtomicBoolean;

/** Original continuous arcade score, mixed by Android with game effects and external audio. */
public final class ArcadeAudio {
  private final AtomicBoolean running = new AtomicBoolean(false);
  private volatile boolean enabled = true;
  private volatile int scene = 0;
  private volatile int style = 0;
  private volatile float volume = .5f;
  private volatile long duckUntil = 0;
  private Thread loop;
  private static final int RATE = 22050;
  private static final int STEP_MS = 180;
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
                    for (int i = 0; i < pcm.length; i++) {
                      double time = i / (double) RATE;
                      double beat = i / (double) pcm.length;
                      double envelope = Math.min(1, i / (RATE * .01)) * Math.min(1, (pcm.length - i) / (RATE * .035));
                      double square = Math.sin(2 * Math.PI * lead * time) >= 0 ? 1 : -1;
                      double triangle = 2 / Math.PI * Math.asin(Math.sin(2 * Math.PI * bass * time));
                      double shimmer = Math.sin(2 * Math.PI * lead * .5 * time);
                      double hat = step % 2 == 0 && beat < .11 ? (Math.sin(i * 7.13) > 0 ? 1 : -1) * (.11 - beat) * 2 : 0;
                      double duck = System.currentTimeMillis() < duckUntil ? .17 : 1;
                      double sample = (square * (style == 0 ? .009 : .043)
                          + triangle * (style == 0 ? .042 : .072)
                          + shimmer * (style == 0 ? .018 : .02)
                          + hat * (style == 0 ? .008 : .027)) * envelope * duck * volume;
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
    new Thread(() -> playNote(84, 55, 0.15f), "arcade-sfx").start();
  }

  public void win() {
    new Thread(
            () -> {
              for (int n : new int[] {72, 76, 79, 84}) playNote(n, 110, 0.18f);
            },
            "arcade-win")
        .start();
  }

  public void lose() {
    new Thread(
            () -> {
              for (int n : new int[] {60, 57, 53, 48}) playNote(n, 120, 0.15f);
            },
            "arcade-lose")
        .start();
  }

  public void tune(int index) {
    duck(3000);
    new Thread(
            () -> {
              for (int n : MELODIES[Math.floorMod(index, MELODIES.length)]) playNote(n, 280, 0.20f);
            },
            "arcade-blind-test")
        .start();
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
      double square = Math.sin(phase) >= 0 ? 1 : -1;
      double envelope = Math.min(1, i / (RATE * .015)) * Math.min(1, (count - i) / (RATE * .06));
      samples[i] = (short) (square * envelope * volume * Short.MAX_VALUE);
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
