package com.aperoroyale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.content.Context;
import java.util.Locale;

/** A shareable 9:16 poster made locally from the resolved round, never a player photo. */
final class RoundCard {
  private static final int WHITE = 0xfff7ead7;
  private static final int GOLD = 0xffe7b961;
  private static final int MUTED = 0xffaca9a5;
  private static final String[][] NAMES = {
      {"CULTURE G", "TRIVIA"}, {"POSITIONS", "POSES"}, {"BLIND TEST", "MUSIC QUIZ"},
      {"RÉFLEXE", "REFLEX"}, {"ROULETTE", "ROULETTE"}, {"DESSIN MAUDIT", "CURSED DRAW"},
      {"MÉMOIRE", "MEMORY"}, {"RYTHME", "RHYTHM"}, {"BLUFF ROYAL", "ROYAL BLUFF"},
      {"DERNIER FIL", "LAST WIRE"}
  };

  private RoundCard() { }

  static Bitmap render(Context context, GameEngine g, boolean en) {
    Bitmap out = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(out);
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    int accent = GameSprites.accent(g.game);
    p.setShader(new LinearGradient(0, 0, 1080, 1920,
        0xff1c1722, 0xff101b23, Shader.TileMode.CLAMP));
    c.drawRect(0, 0, 1080, 1920, p);
    p.setShader(null);
    p.setColor(0x334d6570);
    for (int i = 0; i < 12; i++) c.drawCircle(70 + (i * 187) % 940,
        110 + (i * 293) % 1680, 2 + i % 3, p);
    p.setColor(GOLD);
    c.drawRect(70, 230, 1010, 235, p);
    word(c, p, "APÉRO", 73, 126, 82, WHITE, false);
    word(c, p, "ROYALE", 354, 126, 82, GOLD, false);
    word(c, p, "EST. 2026  /  AFTER DARK", 77, 196, 27, MUTED, false);
    word(c, p, (en ? "ROUND " : "MANCHE ") + (g.turn + 1),
        77, 299, 32, GOLD, false);
    word(c, p, NAMES[Math.floorMod(g.game, 10)][en ? 1 : 0],
        77, 377, 68, WHITE, false);

    RectF art = new RectF(70, 423, 1010, 1110);
    p.setColor(0xff23272c);
    c.drawRoundRect(art, 35, 35, p);
    c.save();
    c.clipRect(art);
    Bitmap atlas = BitmapFactory.decodeResource(context.getResources(),
        R.drawable.game_scene_atlas);
    if (atlas != null) {
      int col = Math.floorMod(g.game, 5), row = Math.floorMod(g.game, 10) / 5;
      int left = col * atlas.getWidth() / 5 + 2;
      int right = (col + 1) * atlas.getWidth() / 5 - 2;
      int tileTop = row * atlas.getHeight() / 2 + 2;
      int tileBottom = (row + 1) * atlas.getHeight() / 2 - 2;
      int cropHeight = Math.min(tileBottom - tileTop,
          Math.round((right - left) * art.height() / art.width()));
      int top = tileTop + (tileBottom - tileTop - cropHeight) / 2;
      p.setFilterBitmap(true);
      p.setColor(WHITE);
      c.drawBitmap(atlas, new Rect(left, top, right, top + cropHeight), art, p);
      atlas.recycle();
    }
    p.setShader(new LinearGradient(0, 423, 0, 1110,
        0x101b1820, 0xd8191720, Shader.TileMode.CLAMP));
    c.drawRect(art, p);
    p.setShader(null);
    p.setColor(0xaa171820);
    c.drawRoundRect(811, 960, 979, 1060, 18, 18, p);
    word(c, p, String.format(Locale.ROOT, "%02d / 10", g.game + 1),
        836, 1028, 47, GOLD, true);
    c.restore();
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(5);
    p.setColor(accent);
    c.drawRoundRect(art, 35, 35, p);
    p.setStyle(Paint.Style.FILL);

    String actor = g.current() == null ? "?" : g.current().name;
    word(c, p, actor.toUpperCase(Locale.ROOT), 88, 1205, 72, WHITE, false);
    String verdict = g.roundPassed ? (en ? "PASS, THEN PLAY ON" : "ON PASSE, ON CONTINUE")
        : g.lastWon ? (en ? "ONE FOR THE TABLE" : "LA TABLE S'EN SOUVIENDRA")
            : (en ? "A STORY FOR TOMORROW" : "ÇA SE RACONTERA DEMAIN");
    word(c, p, verdict, 88, 1282, 38, accent, false);
    String[] story = RoundStories.forRound(g, en);
    p.setColor(0xff26272d);
    c.drawRoundRect(70, 1340, 1010, 1668, 28, 28, p);
    p.setColor(accent);
    c.drawRect(70, 1340, 78, 1668, p);
    word(c, p, en ? "AFTER THE REVEAL" : "APRÈS LE DÉVOILEMENT",
        109, 1405, 29, GOLD, false);
    lines(c, p, story[0], 109, 1496, 870, 48, WHITE, 2);
    lines(c, p, story[1], 109, 1611, 870, 37, MUTED, 2);
    String tally = g.roundPassed ? (en ? "NO PENALTY" : "SANS PÉNALITÉ")
        : g.lastWon ? "+" + g.roundPoints + " PTS"
            : g.roundSips == 0 ? (en ? "NO SIP" : "AUCUNE GORGÉE")
                : "+" + g.roundSips + (en ? " VIRTUAL SIP(S)" : " GORGÉE(S) VIRTUELLE(S)");
    word(c, p, tally, 78, 1779, 42, GOLD, false);
    word(c, p, en ? "PLAY TOGETHER · REMEMBER THE NIGHT"
        : "JOUEZ ENSEMBLE · RACONTEZ LA NUIT", 78, 1846, 26, MUTED, false);
    return out;
  }

  private static void word(Canvas c, Paint p, String text, float x, float y,
      float size, int color, boolean bold) {
    p.setShader(null);
    p.setStyle(Paint.Style.FILL);
    p.setTypeface(Typeface.create("sans-serif-condensed", bold ? Typeface.BOLD : Typeface.NORMAL));
    p.setTextSize(size);
    p.setColor(color);
    float scale = Math.min(1f, 920f / Math.max(1f, p.measureText(text)));
    c.save();
    c.translate(x, y);
    c.scale(scale, 1f);
    c.drawText(text, 0, 0, p);
    c.restore();
  }

  private static void lines(Canvas c, Paint p, String text, float x, float y,
      float width, float size, int color, int maxLines) {
    p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    p.setTextSize(size);
    p.setColor(color);
    String[] words = text.split(" ");
    String line = "";
    int count = 0;
    for (String word : words) {
      String candidate = line.isEmpty() ? word : line + " " + word;
      if (!line.isEmpty() && p.measureText(candidate) > width) {
        c.drawText(line, x, y + count * (size + 9), p);
        count++;
        if (count >= maxLines) return;
        line = word;
      } else line = candidate;
    }
    if (!line.isEmpty() && count < maxLines) c.drawText(line, x, y + count * (size + 9), p);
  }
}
