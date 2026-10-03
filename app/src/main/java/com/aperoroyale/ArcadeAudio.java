package com.aperoroyale;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.atomic.AtomicBoolean;

/** Original PCM chiptunes. Low volume leaves headroom for Spotify and accessibility audio. */
public final class ArcadeAudio {
  private final AtomicBoolean running = new AtomicBoolean(false);
  private volatile boolean enabled = true;
  private Thread loop;
  private static final int RATE = 22050;
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
              int i = 0;
              while (running.get()) {
                if (enabled) {
                  playNote(MELODIES[4][i++ % 8] - 12, 150, 0.065f);
                  if (i % 4 == 0) playNote(40, 90, 0.05f);
                } else sleep(180);
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
