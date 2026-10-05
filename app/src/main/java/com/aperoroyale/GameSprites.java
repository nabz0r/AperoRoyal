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
    {0xffd8a767, 0xff6f9f95, 0xfff4dec0},
    {0xffc77470, 0xff725a7d, 0xffedbb92},
    {0xffb2949e, 0xff6e9388, 0xffe5c8a6},
    {0xff7ab8a9, 0xff5f8aa3, 0xffddbe80},
    {0xffcaa069, 0xff9b5c5c, 0xffe9cf95},
    {0xffa688b6, 0xffbd776f, 0xfff5dfc3},
    {0xff729cb1, 0xff9389ab, 0xffe4d6a7},
    {0xff90b898, 0xffcb9876, 0xffefdfbd},
    {0xffc8869e, 0xff754c62, 0xffeed2c2},
    {0xffcc7268, 0xff8ba8aa, 0xffe8c897}
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
    int index = Math.floorMod(game, 10);
    int[] palette = COLORS[index];
    c.save();
    c.clipRect(x, y, x + w, y + h);
    p.setShader(new LinearGradient(x, y, x, y + h,
        0x24180f1a, 0x8c100f17, Shader.TileMode.CLAMP));
    c.drawRect(x, y, x + w, y + h, p);
    p.setShader(null);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(2);
    p.setColor(withAlpha(palette[0], 100));
    switch (index) {
      case 0 -> { // crooked television frame and quiz marquee
        c.drawRoundRect(x + 14, y + 68, x + w - 14, y + h - 55, 13, 13, p);
        for (int i = 0; i < 9; i++) {
          p.setColor(withAlpha(palette[i % 2], 110));
          c.drawCircle(x + 35 + i * (w - 70) / 8, y + 43, 2.5f, p);
        }
      }
      case 1 -> { // stage curtains and an audience line
        for (int side = 0; side < 2; side++) {
          float edge = side == 0 ? x + 18 : x + w - 18;
          c.drawArc(edge - 57, y + 10, edge + 57, y + h - 60,
              side == 0 ? -72 : 72, side == 0 ? 145 : -145, false, p);
        }
        c.drawArc(x + 45, y + h - 112, x + w - 45, y + h + 65, 180, 180, false, p);
      }
      case 2 -> { // vinyl grooves
        for (int i = 0; i < 5; i++)
          c.drawCircle(x + w - 11, y + h - 30, 33 + i * 18, p);
        for (int i = 0; i < 7; i++)
          c.drawLine(x + 20 + i * 43, y + 46, x + 20 + i * 43, y + 53 + i % 3 * 8, p);
      }
      case 3 -> { // alley markers
        for (int i = 0; i < 7; i++) {
          float py = y + 58 + i * 65;
          c.drawLine(x + 10, py, x + 30, py + 12, p);
          c.drawLine(x + w - 10, py, x + w - 30, py + 12, p);
        }
      }
      case 4 -> { // six coaster rings
        for (int i = 0; i < 6; i++)
          c.drawCircle(x + 66 + i % 3 * (w - 132) / 2,
              y + h - 178 + i / 3 * 82, 29, p);
      }
      case 5 -> { // gallery frames
        for (int i = 0; i < 3; i++) {
          float px = x + 20 + i * (w - 40) / 3;
          c.drawRoundRect(px, y + 54, px + (w - 60) / 3, y + 142, 3, 3, p);
        }
      }
      case 6 -> { // memory cards around the frame
        for (int i = 0; i < 4; i++) {
          float px = x + 20 + i * (w - 40) / 4;
          c.drawRoundRect(px, y + h - 100, px + (w - 60) / 4, y + h - 28, 4, 4, p);
        }
      }
      case 7 -> { // dancing sound bars
        for (int i = 0; i < 8; i++) {
          float bx = x + 16 + i * (w - 32) / 8;
          float bh = 8 + (i * 17 + now / 180) % 29;
          c.drawLine(bx, y + h - 35, bx, y + h - 35 - bh, p);
        }
      }
      case 8 -> { // court-like velvet spotlights
        c.drawArc(x + 10, y + 18, x + w - 10, y + h - 18, 198, 145, false, p);
        c.drawLine(x + w / 2, y + 20, x + w / 2, y + 54, p);
      }
      case 9 -> { // comic timer halo and harmless wires
        for (int i = 0; i < 10; i++) {
          double angle = i * Math.PI / 5;
          float cx = x + w / 2, cy = y + h * .48f;
          c.drawLine(cx + (float) Math.cos(angle) * 125,
              cy + (float) Math.sin(angle) * 125,
              cx + (float) Math.cos(angle) * 135,
              cy + (float) Math.sin(angle) * 135, p);
        }
      }
      default -> { }
    }
    p.setStyle(Paint.Style.FILL);
    p.setColor(withAlpha(palette[0], 90));
    c.drawRect(x + 8, y + h - 8, x + w - 8, y + h - 6, p);
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
