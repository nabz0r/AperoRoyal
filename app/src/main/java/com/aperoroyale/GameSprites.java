package com.aperoroyale;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/** Ten original pixel sprites and animated stage palettes, drawn without external image loading. */
public final class GameSprites {
  private GameSprites() { }

  private static final String[][] PIXELS = {
    { // trivia: arcade terminal
      "....KKKKKKKK....", "...KAAAAAAAAK...", "..KABBBBBBBBAK..", "..KABWWBBWWBAK..",
      "..KABWWBBWWBAK..", "..KABBBBBBBBAK..", "..KABBCCBBBBAK..", "..KABBCBBCCBAK..",
      "..KABBBBBBBBAK..", "..KAAAAAAAAAAK..", "..KKKKKKKKKKKK..", "...KAAAKKAAAK...",
      "...KAAAKKAAAK...", "....KKK..KKK....", "................", "................"
    },
    { // pose: dancing silhouette
      "......CCCC......", ".....CAAAAC.....", ".....CAAAAC.....", "......KKKK......",
      "..BB..KAAK..BB..", "..BBBKAAAAKBBB..", "...BBKAAAAKBB...", ".....KAAAAK.....",
      ".....KAAAAK.....", "....KABBBBAK....", "....KABBBBAK....", "...KABK..KBAK...",
      "..KABK....KBAK..", "..KKK......KKK..", "................", "................"
    },
    { // blind test: cassette
      "................", "..KKKKKKKKKKKK..", ".KAAAAAAAAAAAAK.", ".KABBBBBBBBBBAK.",
      ".KABKCCKKCCKBAK.", ".KABKCCKKCCKBAK.", ".KABBBBBBBBBBAK.", ".KAAAAAAAAAAAAK.",
      ".KABKKKKKKKKBAK.", ".KABCCCCCCCBBAK.", ".KABBBBBBBBBBAK.", ".KAAAAAAAAAAAAK.",
      "..KKKKKKKKKKKK..", "................", "................", "................"
    },
    { // reflex: bolt
      "........KKK.....", ".......KCCK.....", "......KCCK......", ".....KCCK.......",
      "....KCCK........", "...KCCKKKKK.....", "..KCCCCCCCK.....", "..KKKKCCCK......",
      "......KCCK......", ".....KCCK.......", "....KCCK........", "...KCCK.........",
      "..KCCK..........", "..KKK...........", "................", "................"
    },
    { // roulette: goblet
      "................", "..KKKKKKKKKKKK..", "..KAAAAAAAAAAK..", "...KAAAAAAA AK...",
      "...KAAAAAAA AK...", "....KAAAAAAK....", ".....KAAAAK.....", "......KAAK......",
      ".......KK.......", ".......KK.......", ".......KK.......", "......KAAK......",
      ".....KAAAAK.....", "....KKKKKKKK....", "................", "................"
    },
    { // drawing: spray marker
      "..........KK....", ".........KAAK...", "........KABAK...", ".......KABAK....",
      "......KABAK.....", ".....KABAK......", "....KABAK.......", "...KABAK........",
      "..KABAK.........", ".KABAK..........", "KABAK...........", "KCCK............",
      "KKK.............", "............CC..", ".............C..", "................"
    },
    { // memory: four-color chip
      "....KKKKKKKK....", "...KAAAAAAAAK...", "..KABBBCCBBBAK..", "..KABBBCCBBBAK..",
      "..KABBBCCBBBAK..", "..KAAAAAAAAAAK..", "..KACCCCCCCCAK..", "..KACCCCCCCCAK..",
      "..KAAAAAAAAAAK..", "..KABBBCCBBBAK..", "..KABBBCCBBBAK..", "..KABBBCCBBBAK..",
      "...KAAAAAAAAK...", "....KKKKKKKK....", "................", "................"
    },
    { // rhythm: boombox
      ".....KKKKKK.....", "....KAAAAAAK....", ".KKKKKKKKKKKKKK.", ".KAAAAAAAAAAAAK.",
      ".KAKKAAA AAKKAK.", ".KAKCKAAAAKCKAK.", ".KAKCKAAAAKCKAK.", ".KAKKAAAAAAKKAK.",
      ".KAAAAKCCKAAAAK.", ".KAAAAKCCKAAAAK.", ".KAAAAAAAAAAAAK.", ".KKKKKKKKKKKKKK.",
      "..KK........KK..", "................", "................", "................"
    },
    { // bluff: theatre mask
      "....KKKKKKKK....", "...KAAAAAAAAK...", "..KAAAAAAAAAAK..", "..KAKKAAAAKKAK..",
      "..KACCAAAACCAK..", "..KAKKAAAAKKAK..", "..KAAAAAAAAAAK..", "..KAAKAAAAKAAK..",
      "..KAACCCCCCAAK..", "..KAAAKKKKAAAK..", "...KAAAAAAAAK...", "....KKKKKKKK....",
      "................", "................", "................", "................"
    },
    { // bomb: timer
      "......KKKK......", ".....KAAAAK.....", ".....KAAAAK.....", "....KKKKKKKK....",
      "...KAAAAAAAAK...", "..KAAAAAAAAAAK..", ".KAAAKKKKKKAAAK.", ".KAAKCCCCCCKAAK.",
      ".KAAKCCKKCCKAAK.", ".KAAKCCCCCCKAAK.", ".KAAAKKKKKKAAAK.", "..KAAAAAAAAAAK..",
      "...KAAAAAAAAK...", "....KKKKKKKK....", "................", "................"
    }
  };

  private static final int[][] COLORS = {
    {0xfff7a445, 0xff47d8bb, 0xffffe6a1},
    {0xffff6980, 0xff9764e5, 0xffffc39a},
    {0xffaa75fb, 0xff62d7c8, 0xffeec5ff},
    {0xff37dbe5, 0xff247ab5, 0xffffff75},
    {0xffffca56, 0xffe5645c, 0xffffeead},
    {0xffc993ff, 0xffff70a8, 0xfff4e6ff},
    {0xff59b7ff, 0xff8e7bff, 0xffffdf7b},
    {0xff9dde6b, 0xfff5a655, 0xffe5ffd0},
    {0xfff187a5, 0xffa84176, 0xffffd7ca},
    {0xffff685c, 0xff69d9e2, 0xffffdb91}
  };

  public static int accent(int game) { return COLORS[Math.floorMod(game, 10)][0]; }

  public static void icon(Canvas c, Paint p, int game, float cx, float cy, float pixel,
      int progress, long now) {
    int index = Math.floorMod(game, 10);
    String[] sprite = PIXELS[index];
    int[] palette = COLORS[index];
    float bob = (float) Math.sin(now / 320.0 + index) * Math.max(1, pixel * .65f);
    float pulse = (float) (0.5 + 0.5 * Math.sin(now / (index == 9 ? 115.0 : 260.0)));
    p.setColor(withAlpha(palette[0], index == 9 ? (int) (25 + 65 * pulse) : 30));
    c.drawCircle(cx, cy, pixel * (10 + 1.4f * pulse), p);
    c.save();
    if (index == 4 || index == 5 || index == 8)
      c.rotate((float) Math.sin(now / 210.0 + index) * 5, cx, cy);
    if (index == 7) c.scale(1 + .055f * pulse, 1 + .055f * pulse, cx, cy);
    c.translate(cx - 8 * pixel, cy - 8 * pixel + bob);
    p.setStyle(Paint.Style.FILL);
    p.setAntiAlias(false);
    p.setColor(0x66000000);
    c.drawRect(pixel, pixel * 15, pixel * 16, pixel * 16, p);
    for (int y = 0; y < sprite.length; y++) {
      String row = sprite[y];
      for (int x = 0; x < Math.min(16, row.length()); x++) {
        int color = switch (row.charAt(x)) {
          case 'K' -> 0xff12172d;
          case 'A' -> palette[0];
          case 'B' -> palette[1];
          case 'C' -> palette[2];
          case 'W' -> Color.WHITE;
          default -> 0;
        };
        if (color != 0) {
          p.setColor(color);
          c.drawRect(x * pixel, y * pixel, (x + 1) * pixel, (y + 1) * pixel, p);
        }
      }
    }
    p.setColor(palette[2]);
    switch (index) {
      case 0 -> c.drawRect(4 * pixel, (3 + (now / 160 % 6)) * pixel,
          12 * pixel, (4 + (now / 160 % 6)) * pixel, p);
      case 2 -> {
        float spin = (float) (now % 700) / 700f * 6.28318f;
        c.drawCircle((4.5f + (float) Math.cos(spin)) * pixel,
            (5.5f + (float) Math.sin(spin)) * pixel, pixel * .65f, p);
        c.drawCircle((11.5f - (float) Math.cos(spin)) * pixel,
            (5.5f - (float) Math.sin(spin)) * pixel, pixel * .65f, p);
      }
      case 3 -> c.drawRect((4 + now / 105 % 7) * pixel, 2 * pixel,
          (5 + now / 105 % 7) * pixel, 4 * pixel, p);
      case 5 -> c.drawRect((1 + now / 240 % 4) * pixel, 13 * pixel,
          (2 + now / 240 % 4) * pixel, 14 * pixel, p);
      case 6 -> c.drawRect((4 + Math.floorMod(progress, 4) * 2) * pixel, 6 * pixel,
          (6 + Math.floorMod(progress, 4) * 2) * pixel, 8 * pixel, p);
      case 7 -> {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(pixel * .7f);
        c.drawCircle(4.8f * pixel, 7.5f * pixel, (1 + pulse) * pixel, p);
        c.drawCircle(11.2f * pixel, 7.5f * pixel, (1 + pulse) * pixel, p);
        p.setStyle(Paint.Style.FILL);
      }
      case 8 -> {
        if (now / 1200 % 5 == 0) {
          c.drawRect(4 * pixel, 4 * pixel, 6 * pixel, 5 * pixel, p);
          c.drawRect(10 * pixel, 4 * pixel, 12 * pixel, 5 * pixel, p);
        }
      }
      case 9 -> c.drawRect(7 * pixel, 2 * pixel, 9 * pixel,
          (3 + now / 120 % 2) * pixel, p);
      default -> { }
    }
    p.setAntiAlias(true);
    c.restore();
    float orbit = (now % 2100) / 2100f * 6.28318f;
    p.setColor(palette[2]);
    float radius = pixel * 10 + Math.min(4, progress) * pixel;
    c.drawCircle(cx + (float) Math.cos(orbit) * radius,
        cy + (float) Math.sin(orbit) * radius, Math.max(2, pixel * .8f), p);
  }

  public static void stage(Canvas c, Paint p, int game, float x, float y, float w, float h,
      int progress, long now) {
    int[] palette = COLORS[Math.floorMod(game, 10)];
    c.save();
    c.clipRect(x, y, x + w, y + h);
    p.setShader(new LinearGradient(x, y, x + w, y + h,
        mix(0xff15192d, palette[1], .20f), mix(0xff0e1127, palette[0], .15f),
        Shader.TileMode.CLAMP));
    c.drawRect(x, y, x + w, y + h, p);
    p.setShader(null);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(1);
    p.setColor(withAlpha(palette[0], 36));
    for (int i = 0; i < 9; i++) {
      float ly = y + 42 + i * 58;
      c.drawLine(x, ly, x + w, ly, p);
    }
    for (int i = 0; i < 8; i++) {
      float lx = x + i * 52 + ((now / 80 + game * 13) % 52);
      c.drawLine(lx, y, lx - 75, y + h, p);
    }
    p.setStyle(Paint.Style.FILL);
    p.setColor(withAlpha(palette[2], 25));
    c.drawCircle(x + w * .75f, y + h * .44f, 92 + (progress % 5) * 5, p);
    p.setColor(withAlpha(palette[0], 72));
    c.drawRect(x, y, x + w, y + 7, p);
    for (int i = 0; i < 10; i++) {
      p.setColor(i <= progress % 10 ? palette[0] : withAlpha(palette[0], 52));
      float bx = x + 17 + i * 31;
      c.drawRoundRect(new RectF(bx, y + h - 16, bx + 21, y + h - 11), 2, 2, p);
    }
    c.restore();
  }

  private static int withAlpha(int color, int alpha) {
    return (alpha << 24) | (color & 0x00ffffff);
  }

  private static int mix(int a, int b, float t) {
    return Color.rgb((int) (Color.red(a) * (1 - t) + Color.red(b) * t),
        (int) (Color.green(a) * (1 - t) + Color.green(b) * t),
        (int) (Color.blue(a) * (1 - t) + Color.blue(b) * t));
  }
}
