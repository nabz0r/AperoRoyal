package com.aperoroyale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/** Large touch targets, high contrast, vector avatars and animated arcade presentation. */
public final class ArcadeView extends View {
  public interface Actions {
    void save();

    void finishGame(boolean won);

    void enterGame();

    void addPlayer();

    void newParty();

    void resumeParty();

    boolean hasSavedParty();

    void startHost();

    void joinRoom();

    void showSettings();

    void radio();

    void stats();

    void home();

    void message(String text);

    String status();

    String hostAddress();

    boolean client();

    void remoteTouch(float x, float y, int kind, float height);

    void click();

    void tune();
  }

  private final GameEngine g;
  private final Actions actions;
  private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final ArrayList<Hit> hits = new ArrayList<>();
  private final Random particle = new Random(19);
  private float H = 800, scale = 1, strokeX, strokeY;
  private boolean drawing = false;
  private static final int BG = Color.rgb(13, 16, 37),
      PANEL = Color.rgb(25, 32, 65),
      WHITE = Color.rgb(246, 246, 255),
      CYAN = Color.rgb(49, 229, 216),
      PINK = Color.rgb(255, 74, 156),
      YELLOW = Color.rgb(255, 222, 89),
      MUTED = Color.rgb(159, 175, 215);
  private static final int[] TILE = {
    Color.rgb(255, 82, 119),
    Color.rgb(49, 229, 216),
    Color.rgb(255, 206, 68),
    Color.rgb(126, 98, 255)
  };

  private static final class Hit {
    final String id;
    final RectF area;

    Hit(String id, RectF area) {
      this.id = id;
      this.area = area;
    }
  }

  public ArcadeView(Context context, GameEngine game, Actions actions) {
    super(context);
    g = game;
    this.actions = actions;
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
  }

  public float virtualHeight() {
    return H;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    scale = getWidth() / 400f;
    H = getHeight() / scale;
    canvas.save();
    canvas.scale(scale, scale);
    hits.clear();
    background(canvas);
    switch (g.screen) {
      case "HOME" -> home(canvas);
      case "LOBBY" -> lobby(canvas);
      case "TRANSITION" -> transition(canvas);
      case "GAME" -> game(canvas);
      case "RESULT" -> result(canvas);
      case "STATS" -> stats(canvas);
      default -> home(canvas);
    }
    canvas.restore();
    postInvalidateDelayed(33);
  }

  private void background(Canvas c) {
    p.setShader(new LinearGradient(0, 0, 400, H, BG, Color.rgb(17, 21, 55), Shader.TileMode.CLAMP));
    c.drawRect(0, 0, 400, H, p);
    p.setShader(null);
    p.setColor(Color.argb(28, 130, 150, 230));
    p.setStrokeWidth(1);
    for (int y = 0; y < H; y += 24) c.drawLine(0, y, 400, y, p);
    for (int x = 0; x < 400; x += 24) c.drawLine(x, 0, x, H, p);
    particle.setSeed(17);
    long t = System.currentTimeMillis();
    for (int i = 0; i < 34; i++) {
      float x = particle.nextInt(400),
          y = (particle.nextInt((int) H) + (t / 30 + i * 19) % ((int) H)) % H;
      p.setColor(i % 3 == 0 ? Color.argb(120, 255, 74, 156) : Color.argb(100, 49, 229, 216));
      c.drawCircle(x, y, 1 + i % 3, p);
    }
    p.setColor(WHITE);
  }

  private void panel(Canvas c, float x, float y, float w, float h, int fill, int stroke) {
    p.setStyle(Paint.Style.FILL);
    p.setColor(fill);
    c.drawRoundRect(x, y, x + w, y + h, 18, 18, p);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(2);
    p.setColor(stroke);
    c.drawRoundRect(x, y, x + w, y + h, 18, 18, p);
    p.setStyle(Paint.Style.FILL);
  }

  private void text(Canvas c, String s, float x, float y, float size, int color, boolean center) {
    p.setShader(null);
    p.setColor(color);
    p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
    p.setTextSize(size);
    p.setTextAlign(center ? Paint.Align.CENTER : Paint.Align.LEFT);
    c.drawText(s, x, y, p);
  }

  private float block(
      Canvas c, String s, float x, float y, float max, float size, int color, boolean center) {
    p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
    p.setTextSize(size);
    String[] words = s.split(" ");
    String line = "";
    float yy = y;
    for (String word : words) {
      String candidate = line.isEmpty() ? word : line + " " + word;
      if (p.measureText(candidate) > max && !line.isEmpty()) {
        text(c, line, x, yy, size, color, center);
        yy += size * 1.36f;
        line = word;
      } else line = candidate;
    }
    if (!line.isEmpty()) text(c, line, x, yy, size, color, center);
    return yy + size * 1.35f;
  }

  private void button(
      Canvas c, String id, String label, float x, float y, float w, float h, int color) {
    panel(c, x, y, w, h, color, Color.argb(170, 255, 255, 255));
    float size = 19;
    p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
    do {
      p.setTextSize(size);
      if (p.measureText(label) <= w - 16) break;
      size -= 1;
    } while (size > 10);
    text(c, label, x + w / 2, y + h / 2 + size * .34f, size, color == PANEL ? WHITE : BG, true);
    hits.add(new Hit(id, new RectF(x, y, x + w, y + h)));
  }

  private void label(Canvas c, String s, float y) {
    text(c, s, 200, y, 17, MUTED, true);
  }

  private void header(Canvas c, String sub) {
    text(c, "APÉRO", 22, 49, 25, YELLOW, false);
    text(c, "ROYALE", 22, 76, 25, PINK, false);
    text(c, "✦", 365, 62, 29, CYAN, true);
    p.setColor(PINK);
    c.drawRoundRect(20, 91, 380, 94, 2, 2, p);
    if (sub != null) text(c, sub, 200, 122, 14, MUTED, true);
  }

  private void home(Canvas c) {
    header(c, null);
    p.setColor(PINK);
    c.drawCircle(200, 215, 85, p);
    p.setColor(YELLOW);
    c.drawCircle(200, 215, 71, p);
    avatar(c, 2, 200, 215, 1.8f);
    text(c, g.t("LA SOIRÉE", "THE PARTY"), 200, 330, 33, WHITE, true);
    text(c, g.t("EST À TOI", "IS YOURS"), 200, 368, 33, CYAN, true);
    block(
        c,
        g.t(
            "10 MINI-JEUX  •  2-6 JOUEURS  •  FR / EN",
            "10 MINI-GAMES  •  2-6 PLAYERS  •  FR / EN"),
        200,
        412,
        355,
        13,
        MUTED,
        true);
    float y = H - 312;
    button(c, "new", g.t("NOUVELLE PARTIE", "NEW PARTY"), 35, y, 330, 59, PINK);
    if (actions.hasSavedParty())
      button(c, "resume", g.t("REPRENDRE", "RESUME"), 35, y + 68, 330, 59, CYAN);
    else button(c, "join", g.t("REJOINDRE EN WI-FI", "JOIN ON WI-FI"), 35, y + 68, 330, 59, CYAN);
    button(c, "stats", g.t("CLASSEMENT", "LEADERBOARD"), 35, y + 136, 155, 55, YELLOW);
    button(
        c, "settings", g.t("MUSIQUE / SPOTIFY", "MUSIC / SPOTIFY"), 205, y + 136, 160, 55, YELLOW);
    button(c, "join", g.t("REJOINDRE", "JOIN ROOM"), 35, y + 200, 155, 51, PANEL);
    button(c, "radio", "RADIO APÉRO", 205, y + 200, 160, 51, PANEL);
    text(
        c,
        g.t("MUSIQUE ORIGINALE • SANS PUB", "ORIGINAL CHIPTUNES • NO ADS"),
        200,
        H - 14,
        11,
        MUTED,
        true);
  }

  private void lobby(Canvas c) {
    header(c, g.t("SALLE DES LÉGENDES", "HALL OF LEGENDS"));
    label(c, g.players.size() + " / 6  " + g.t("JOUEURS", "PLAYERS"), 158);
    for (int i = 0; i < g.players.size(); i++) {
      GameEngine.Player a = g.players.get(i);
      float y = 181 + i * 65;
      panel(c, 22, y, 356, 57, PANEL, i == g.active ? CYAN : Color.rgb(67, 80, 119));
      avatar(c, a.avatar, 59, y + 28, 0.46f);
      text(c, a.name, 99, y + 34, 18, WHITE, false);
      text(c, a.language, 340, y + 34, 17, YELLOW, true);
    }
    if (actions.client()) {
      block(
          c,
          g.t("En attente de l'hôte…", "Waiting for the host…"),
          200,
          H - 111,
          345,
          19,
          CYAN,
          true);
      return;
    }
    if (!actions.hostAddress().isEmpty())
      block(c, "Wi-Fi: " + actions.hostAddress(), 200, H - 221, 350, 15, CYAN, true);
    button(c, "add", g.t("+ AJOUTER JOUEUR", "+ ADD PLAYER"), 30, H - 189, 340, 55, YELLOW);
    button(
        c,
        "start",
        g.t("LANCER LA PARTIE", "START PARTY"),
        30,
        H - 124,
        340,
        56,
        g.players.size() >= 2 ? PINK : PANEL);
    button(c, "host", g.t("HÉBERGER WI-FI", "HOST WI-FI"), 30, H - 60, 165, 44, CYAN);
    button(c, "back", g.t("RETOUR", "BACK"), 205, H - 60, 165, 44, PANEL);
  }

  private void transition(Canvas c) {
    header(c, g.t("TOURNIQUET DE LA MORT", "WHEEL OF CHAOS"));
    GameEngine.Player player = g.current();
    if (player == null) return;
    long now = System.currentTimeMillis();
    float angle = (now % 2700) / 2700f * 360;
    c.save();
    c.rotate(angle, 200, H * .44f);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(17);
    p.setColor(PINK);
    c.drawCircle(200, H * .44f, 94, p);
    p.setColor(CYAN);
    c.drawCircle(200, H * .44f, 70, p);
    p.setStyle(Paint.Style.FILL);
    for (int i = 0; i < 8; i++) {
      double a = i * Math.PI / 4;
      p.setColor(i % 2 == 0 ? YELLOW : PINK);
      c.drawCircle((float) (200 + 82 * Math.cos(a)), (float) (H * .44 + 82 * Math.sin(a)), 8, p);
    }
    c.restore();
    avatar(c, player.avatar, 200, H * .44f, 1.3f);
    text(c, player.name.toUpperCase(Locale.ROOT), 200, H * .44f + 142, 28, WHITE, true);
    text(c, "→ " + title(g.game), 200, H * .44f + 179, 19, CYAN, true);
    button(c, "enter", g.t("C'EST PARTI !", "LET'S GO!"), 32, H - 110, 336, 65, YELLOW);
  }

  private String title(int i) {
    String[][] names = {
      {"CULTURE G", "ODD TRIVIA"},
      {"POSITIONS À LA CON", "SILLY POSES"},
      {"BLIND TEST", "BLIND TEST"},
      {"RÉFLEXE NÉON", "NEON REFLEX"},
      {"ROULETTE ROYALE", "ROYAL ROULETTE"},
      {"DESSIN MAUDIT", "CURSED DRAWING"},
      {"MÉMOIRE FLASH", "FLASH MEMORY"},
      {"RYTHME OU RIEN", "BEAT OR BUST"},
      {"BLUFF ROYAL", "ROYAL BLUFF"},
      {"BOMBE À BULLES", "BUBBLE BOMB"}
    };
    return names[Math.max(0, Math.min(9, i))][g.english() ? 1 : 0];
  }

  private void game(Canvas c) {
    GameEngine.Player a = g.current();
    if (a == null) return;
    header(c, title(g.game));
    avatar(c, a.avatar, 52, 146, .42f);
    text(c, a.name, 91, 150, 20, WHITE, false);
    text(c, "#" + (g.turn + 1), 355, 150, 16, YELLOW, true);
    panel(c, 20, 169, 360, H - 261, PANEL, CYAN);
    if (g.deadline > 0) {
      long sec = Math.max(0, (g.deadline - System.currentTimeMillis() + 999) / 1000);
      text(c, sec + "s", 339, 208, 20, YELLOW, true);
    }
    switch (g.game) {
      case 0 -> quiz(c);
      case 1 -> pose(c);
      case 2 -> blind(c);
      case 3 -> reflex(c);
      case 4 -> roulette(c);
      case 5 -> drawGame(c);
      case 6 -> memory(c);
      case 7 -> rhythm(c);
      case 8 -> bluff(c);
      case 9 -> bomb(c);
    }
    buzz(c, a);
  }

  private void buzz(Canvas c, GameEngine.Player a) {
    text(c, g.t("JAUGE DE FÊTE", "PARTY METER"), 22, H - 62, 12, MUTED, false);
    panel(c, 22, H - 50, 263, 21, BG, PINK);
    p.setColor(PINK);
    float fill = Math.min(1, a.drinks / 6f);
    c.drawRoundRect(25, H - 47, 25 + 257 * fill, H - 32, 7, 7, p);
    text(c, a.drinks + " ×", 332, H - 34, 20, YELLOW, true);
  }

  private void quiz(Canvas c) {
    String[] q = (g.english() ? GameEngine.QUIZ_EN : GameEngine.QUIZ_FR)[g.variant];
    block(c, q[0], 200, 257, 295, 21, WHITE, true);
    for (int i = 0; i < 4; i++) {
      int opt = (i - g.target + 4) % 4;
      button(c, "answer:" + i, q[opt + 1], 45, 354 + i * 66, 310, 55, i % 2 == 0 ? CYAN : YELLOW);
    }
  }

  private void pose(Canvas c) {
    String[] q = GameEngine.POSES[g.variant];
    block(c, q[g.english() ? 1 : 0], 200, 280, 315, 23, WHITE, true);
    text(c, "✦  ✦  ✦", 200, 452, 33, PINK, true);
    button(c, "win", g.t("DÉFI RÉUSSI", "NAILED IT"), 42, H - 236, 316, 61, CYAN);
    button(c, "lose", g.t("J'AI RATÉ", "I FAILED"), 42, H - 161, 316, 61, PINK);
  }

  private void blind(Canvas c) {
    block(
        c,
        g.t("Écoute et devine le morceau !", "Listen and name that tune!"),
        200,
        258,
        300,
        20,
        WHITE,
        true);
    if ("loading".equals(g.note)) {
      label(c, "Spotify…", 345);
      return;
    }
    button(c, "tune", g.t("▶ ÉCOUTER", "▶ PLAY TUNE"), 60, 309, 280, 55, PINK);
    JSONArray spotifyChoices = null;
    try {
      if (g.note.startsWith("{")) spotifyChoices = new JSONObject(g.note).optJSONArray("choices");
    } catch (Exception ignored) {
    }
    for (int i = 0; i < 4; i++) {
      int option = (i - g.target + 4) % 4;
      String name =
          spotifyChoices == null
              ? GameEngine.TUNES[(g.variant + option) % 6][g.english() ? 1 : 0]
              : spotifyChoices.optString(option, "?");
      button(c, "answer:" + i, name, 35, 384 + i * 62, 330, 51, i % 2 == 0 ? CYAN : YELLOW);
    }
  }

  private void reflex(Canvas c) {
    block(
        c,
        g.t("Tape 10 cibles avant la fin !", "Hit 10 targets before time runs out!"),
        200,
        257,
        310,
        21,
        WHITE,
        true);
    text(c, g.t("CIBLES", "HITS") + " " + g.taps + " / 10", 200, 321, 23, YELLOW, true);
    p.setColor(PINK);
    c.drawCircle(g.targetX, g.targetY, 52, p);
    p.setColor(YELLOW);
    c.drawCircle(g.targetX, g.targetY, 38, p);
    p.setColor(BG);
    c.drawCircle(g.targetX, g.targetY, 13, p);
  }

  private void roulette(Canvas c) {
    block(
        c,
        g.t("Choisis un gobelet. Un seul est piégé !", "Pick a cup. One is cursed!"),
        200,
        257,
        315,
        21,
        WHITE,
        true);
    for (int i = 0; i < 6; i++) {
      float x = 42 + (i % 3) * 107, y = 348 + (i / 3) * 132;
      panel(c, x, y, 91, 108, i % 2 == 0 ? PINK : CYAN, WHITE);
      p.setColor(YELLOW);
      c.drawRoundRect(x + 18, y + 26, x + 73, y + 81, 9, 9, p);
      text(c, "?", x + 45, y + 67, 35, BG, true);
      hits.add(new Hit("cup:" + i, new RectF(x, y, x + 91, y + 108)));
    }
  }

  private void drawGame(Canvas c) {
    if (!g.drawingReady) {
      String[] prompt = GameEngine.DRAW[g.variant];
      block(
          c,
          g.t("Dessine : ", "Draw: ") + prompt[g.english() ? 1 : 0],
          200,
          239,
          320,
          18,
          YELLOW,
          true);
      panel(c, 40, 280, 320, 285, WHITE, CYAN);
      for (float[] s : g.strokes) {
        p.setColor(BG);
        p.setStrokeWidth(5);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(s[0], s[1], s[2], s[3], p);
      }
      button(
          c, "drawReady", g.t("PASSER AU DEVINEUR", "HAND TO GUESSER"), 45, H - 156, 310, 59, PINK);
    } else {
      block(c, g.t("Devine le dessin !", "Guess the drawing!"), 200, 255, 300, 22, YELLOW, true);
      panel(c, 70, 280, 260, 200, WHITE, CYAN);
      for (float[] s : g.strokes) {
        p.setColor(BG);
        p.setStrokeWidth(4);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(s[0], s[1], s[2], s[3], p);
      }
      for (int i = 0; i < 4; i++) {
        int opt = (i - g.target + 4) % 4;
        String[] q = GameEngine.DRAW[(g.variant + opt) % 6];
        button(
            c,
            "answer:" + i,
            q[g.english() ? 1 : 0],
            35,
            500 + i * 50,
            330,
            44,
            i % 2 == 0 ? CYAN : YELLOW);
      }
    }
  }

  private void memory(Canvas c) {
    long elapsed = System.currentTimeMillis() - g.started;
    long reveal = g.sequence.length * 720L + 750;
    block(
        c, g.t("Retiens la séquence !", "Remember the sequence!"), 200, 246, 310, 21, WHITE, true);
    if (elapsed < reveal) {
      int n = (int) Math.min(g.sequence.length - 1, elapsed / 720);
      text(c, g.t("REGARDE…", "WATCH…"), 200, 297, 18, YELLOW, true);
      tile(c, g.sequence[n], true);
    } else {
      text(c, g.t("À TOI !", "YOUR TURN!"), 200, 297, 19, YELLOW, true);
      for (int i = 0; i < 4; i++) tile(c, i, false);
    }
    text(c, g.progress + " / " + g.sequence.length, 200, 624, 24, CYAN, true);
  }

  private void tile(Canvas c, int i, boolean glow) {
    float x = 45 + (i % 2) * 165, y = 340 + (i / 2) * 130;
    panel(c, x, y, 145, 110, TILE[i], glow ? WHITE : TILE[i]);
    text(c, new String[] {"◆", "●", "▲", "■"}[i], x + 72, y + 68, 54, WHITE, true);
    if (!glow) hits.add(new Hit("tile:" + i, new RectF(x, y, x + 145, y + 110)));
  }

  private void rhythm(Canvas c) {
    block(
        c,
        g.t("Tape sur le beat. 4 coups parfaits !", "Tap on the beat. 4 clean hits!"),
        200,
        247,
        315,
        21,
        WHITE,
        true);
    long phase = (System.currentTimeMillis() - g.started) % 600;
    float pulse = phase < 300 ? phase / 300f : (600 - phase) / 300f;
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(5 + 10 * pulse);
    p.setColor(YELLOW);
    c.drawCircle(200, 440, 62 + 33 * pulse, p);
    p.setStyle(Paint.Style.FILL);
    p.setColor(PINK);
    c.drawCircle(200, 440, 68, p);
    text(c, "TAP", 200, 452, 30, WHITE, true);
    hits.add(new Hit("beat", new RectF(90, 330, 310, 550)));
    text(c, g.rhythmHits + " / 4", 200, 611, 24, CYAN, true);
  }

  private void bluff(Canvas c) {
    String[] q = GameEngine.BLUFF[g.variant];
    block(c, q[g.english() ? 1 : 0], 200, 286, 315, 23, WHITE, true);
    text(c, "♦  ♠  ♥  ♣", 200, 443, 32, YELLOW, true);
    block(
        c,
        g.t("Le groupe juge ta performance.", "The group decides your fate."),
        200,
        493,
        315,
        15,
        MUTED,
        true);
    button(c, "win", g.t("ILS Y CROIENT", "THEY BOUGHT IT"), 38, H - 225, 324, 58, CYAN);
    button(c, "lose", g.t("DÉMASQUÉ !", "BUSTED!"), 38, H - 151, 324, 58, PINK);
  }

  private void bomb(Canvas c) {
    block(
        c,
        g.t("Passe le téléphone et désamorce à 8 taps !", "Pass the phone and defuse with 8 taps!"),
        200,
        261,
        310,
        20,
        WHITE,
        true);
    float f = Math.max(.1f, 1 - (System.currentTimeMillis() - g.started) / 12000f);
    p.setColor(PINK);
    c.drawCircle(200, 443, 71 + 20 * f, p);
    p.setColor(YELLOW);
    c.drawCircle(200, 443, 56, p);
    text(c, "TAP!", 200, 453, 26, BG, true);
    hits.add(new Hit("bomb", new RectF(105, 350, 295, 540)));
    text(c, g.taps + " / 8", 200, 603, 25, CYAN, true);
  }

  private void result(Canvas c) {
    header(c, g.t("FIN DU TOUR", "ROUND OVER"));
    GameEngine.Player a = g.current();
    if (a == null) return;
    text(
        c,
        g.lastWon ? g.t("VICTOIRE !", "VICTORY!") : g.t("CUL SEC… VIRTUEL !", "VIRTUAL SIP!"),
        200,
        204,
        27,
        g.lastWon ? CYAN : PINK,
        true);
    avatar(c, a.avatar, 200, 295, 1.3f);
    text(c, a.name, 200, 385, 25, WHITE, true);
    glass(
        c,
        200,
        478,
        g.lastWon ? 1 : Math.max(.05f, 1 - (System.currentTimeMillis() - g.started) / 1400f));
    text(c, g.lastWon ? "+100 PTS" : "+1 " + g.t("VERRE", "DRINK"), 200, 582, 24, YELLOW, true);
    block(
        c,
        g.t(
            "Boire est toujours facultatif. Eau bienvenue !",
            "Drinking is always optional. Water wins!"),
        200,
        630,
        305,
        14,
        MUTED,
        true);
    button(
        c, "next", g.t("TOUR SUIVANT", "NEXT TURN"), 35, H - 110, 330, 62, g.lastWon ? CYAN : PINK);
  }

  private void glass(Canvas c, float x, float y, float level) {
    p.setColor(WHITE);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(4);
    Path path = new Path();
    path.moveTo(x - 40, y - 58);
    path.lineTo(x - 30, y + 55);
    path.lineTo(x + 30, y + 55);
    path.lineTo(x + 40, y - 58);
    path.close();
    c.drawPath(path, p);
    p.setStyle(Paint.Style.FILL);
    p.setColor(YELLOW);
    float top = y + 49 - 98 * level;
    c.drawRoundRect(x - 29, top, x + 29, y + 49, 5, 5, p);
    for (int i = 0; i < 5; i++) {
      p.setColor(WHITE);
      c.drawCircle(x - 20 + i * 9, y + 40 - i * 17, 2 + i % 2, p);
    }
  }

  private void stats(Canvas c) {
    header(c, g.t("CLASSEMENT HISTORIQUE", "LIFETIME LEADERBOARD"));
    MainActivity a = (MainActivity) getContext();
    ArrayList<String[]> rows = a.store.leaderboard();
    if (rows.isEmpty())
      block(
          c,
          g.t("Aucune partie terminée encore.", "No completed rounds yet."),
          200,
          245,
          330,
          20,
          MUTED,
          true);
    for (int i = 0; i < Math.min(rows.size(), 8); i++) {
      String[] r = rows.get(i);
      float y = 178 + i * 66;
      panel(c, 25, y, 350, 58, PANEL, i == 0 ? YELLOW : CYAN);
      text(c, "#" + (i + 1), 49, y + 35, 17, YELLOW, false);
      text(c, r[0], 90, y + 34, 18, WHITE, false);
      text(c, r[4] + "pt", 340, y + 34, 17, CYAN, true);
      text(
          c,
          r[1] + "W / " + r[2] + "G / " + r[3] + g.t(" verres", " drinks"),
          90,
          y + 52,
          10,
          MUTED,
          false);
    }
    button(c, "back", g.t("RETOUR", "BACK"), 35, H - 90, 330, 59, PINK);
  }

  private void avatar(Canvas c, int type, float x, float y, float scale) {
    c.save();
    c.translate(x, y);
    c.scale(scale, scale);
    int[] colors = {YELLOW, CYAN, YELLOW, PINK, CYAN, PINK};
    p.setColor(colors[Math.floorMod(type, 6)]);
    c.drawCircle(0, 0, 30, p);
    if (type == 0) {
      ear(c, -20, -24);
      ear(c, 20, -24);
    } else if (type == 1) {
      p.setColor(CYAN);
      c.drawCircle(-18, -24, 12, p);
      c.drawCircle(18, -24, 12, p);
    } else if (type == 2) {
      p.setColor(PINK);
      c.drawCircle(23, 11, 14, p);
      p.setColor(YELLOW);
      c.drawOval(-24, -17, 23, 27, p);
    } else if (type == 3) {
      p.setColor(PINK);
      c.drawOval(-32, -17, 32, 24, p);
    } else if (type == 4) {
      p.setColor(CYAN);
      c.drawRoundRect(-30, -25, 30, 27, 7, 7, p);
      p.setColor(YELLOW);
      c.drawRect(-3, -37, 3, -25, p);
      c.drawCircle(0, -39, 5, p);
    } else {
      p.setColor(PINK);
      c.drawCircle(0, 0, 31, p);
      p.setColor(WHITE);
      c.drawCircle(0, 0, 20, p);
    }
    p.setColor(BG);
    c.drawCircle(-11, -5, 4, p);
    c.drawCircle(11, -5, 4, p);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(2.5f);
    c.drawArc(-11, 0, 11, 15, 10, 160, false, p);
    p.setStyle(Paint.Style.FILL);
    c.restore();
  }

  private void ear(Canvas c, float x, float y) {
    p.setColor(YELLOW);
    Path path = new Path();
    path.moveTo(x - 12, y + 7);
    path.lineTo(x, y - 21);
    path.lineTo(x + 12, y + 7);
    path.close();
    c.drawPath(path, p);
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    int kind =
        e.getActionMasked() == MotionEvent.ACTION_DOWN
            ? 0
            : e.getActionMasked() == MotionEvent.ACTION_MOVE ? 1 : 2;
    if (kind > 2) return true;
    float x = e.getX() / scale, y = e.getY() / scale;
    if (actions.client()) {
      actions.remoteTouch(x, y, kind, H);
      return true;
    }
    handle(x, y, kind);
    return true;
  }

  public void handleRemote(float x, float y, int kind) {
    handle(x, y, kind);
  }

  private void handle(float x, float y, int kind) {
    if (kind != 0) {
      if (g.game == 5 && "GAME".equals(g.screen) && !g.drawingReady && drawing) {
        if (kind == 1) {
          if (x >= 42 && x <= 358 && y >= 283 && y <= 563 && g.strokes.size() < 500) {
            g.strokes.add(new float[] {strokeX, strokeY, x, y});
            strokeX = x;
            strokeY = y;
            invalidate();
            if (g.strokes.size() % 3 == 0) actions.save();
          }
        } else {
          drawing = false;
          actions.save();
        }
      }
      return;
    }
    for (Hit h : new ArrayList<>(hits))
      if (h.area.contains(x, y)) {
        press(h.id);
        return;
      }
    if ("GAME".equals(g.screen)) {
      if (g.game == 3) {
        double d = Math.hypot(x - g.targetX, y - g.targetY);
        if (d <= 55) {
          g.taps++;
          g.targetX = 75 + new Random().nextInt(250);
          g.targetY = 280 + new Random().nextInt((int) Math.max(100, H - 500));
          actions.click();
          if (g.taps >= 10) actions.finishGame(true);
          else actions.save();
        }
      } else if (g.game == 5 && !g.drawingReady && x >= 42 && x <= 358 && y >= 283 && y <= 563) {
        drawing = true;
        strokeX = x;
        strokeY = y;
      }
    }
  }

  private void press(String id) {
    actions.click();
    switch (id) {
      case "new" -> actions.newParty();
      case "resume" -> actions.resumeParty();
      case "join" -> actions.joinRoom();
      case "stats" -> actions.stats();
      case "settings" -> actions.showSettings();
      case "radio" -> actions.radio();
      case "add" -> actions.addPlayer();
      case "host" -> actions.startHost();
      case "back" -> actions.home();
      case "start" -> {
        if (g.players.size() >= 2) {
          g.begin();
          actions.save();
        }
      }
      case "enter" -> actions.enterGame();
      case "win" -> actions.finishGame(true);
      case "lose" -> actions.finishGame(false);
      case "next" -> {
        g.advance();
        actions.save();
      }
      case "tune" -> actions.tune();
      case "drawReady" -> {
        g.drawingReady = true;
        actions.save();
      }
      case "beat" -> {
        long elapsed = System.currentTimeMillis() - g.started;
        int beat = (int) (elapsed / 600);
        int offset = (int) (elapsed % 600);
        if (Math.abs(offset - 300) <= 155 && beat != g.progress) {
          g.progress = beat;
          g.rhythmHits++;
          if (g.rhythmHits >= 4) actions.finishGame(true);
          else actions.save();
        }
      }
      case "bomb" -> {
        g.taps++;
        if (g.taps >= 8) actions.finishGame(true);
        else actions.save();
      }
      default -> {
        if (id.startsWith("answer:")) {
          int selected = Integer.parseInt(id.substring(7));
          actions.finishGame(selected == g.target);
        } else if (id.startsWith("cup:")) {
          int cup = Integer.parseInt(id.substring(4));
          actions.finishGame(cup != g.loserCup);
        } else if (id.startsWith("tile:")) {
          if (System.currentTimeMillis() - g.started < g.sequence.length * 720L + 750) return;
          int color = Integer.parseInt(id.substring(5));
          if (g.progress < g.sequence.length && color == g.sequence[g.progress]) {
            g.progress++;
            if (g.progress == g.sequence.length) actions.finishGame(true);
            else actions.save();
          } else actions.finishGame(false);
        }
      }
    }
    invalidate();
  }
}
