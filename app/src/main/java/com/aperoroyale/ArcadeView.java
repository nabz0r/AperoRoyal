package com.aperoroyale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.Base64;
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

    void editPlayer(int index);

    void vote(int choice);

    void placeBet(int sips);

    void predict(boolean win);

    void beginJury();

    void judge(boolean yes);

    void reflexTap(int expectedStep);

    void rhythmTap(int beat);

    void selectCup(int cup);

    void reportRule();

    void ruleVote(boolean yes);

    void bombTap();

    boolean passPending();

    void confirmPass();

    void catTap();

    void chooseRule(int id);

    void selectGame(int id);

    void setMode(String mode);

    void newParty();

    void resumeParty();

    boolean hasSavedParty();

    void startHost();

    void joinRoom();

    void showSettings();

    void guide();

    void spotifySettings();

    void toggleMusic();

    void toggleEffects();

    void toggleHaptics();

    void changeMusicStyle();

    void changeMusicVolume();

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
  private final ArrayList<float[]> ghostStrokes = new ArrayList<>();
  private boolean ghostDrawing = false;
  private int ghostTurn = -1;
  private float ghostX, ghostY;
  private int predictedReflex = 0;
  private int predictedTurn = -1;
  private long lastPredictionAt = 0;
  private long lastRemoteDrawAt = 0;
  private int statsTab = 0;
  private int guideIndex = 0;
  private String cachedPhoto = "";
  private Bitmap cachedBitmap;
  private final Bitmap hero;
  private final Bitmap avatarSheet;
  private static final int BG = Color.rgb(16, 18, 38),
      PANEL = Color.rgb(31, 37, 63),
      WHITE = Color.rgb(255, 250, 240),
      CYAN = Color.rgb(72, 219, 205),
      PINK = Color.rgb(255, 105, 127),
      YELLOW = Color.rgb(255, 195, 87),
      MUTED = Color.rgb(178, 188, 208);
  private static final int[] TILE = {
    Color.rgb(255, 105, 127),
    Color.rgb(72, 219, 205),
    Color.rgb(255, 195, 87),
    Color.rgb(178, 127, 250)
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
    hero = BitmapFactory.decodeResource(getResources(), R.drawable.arcade_party_hero);
    avatarSheet = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_sheet);
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
      case "VOTE" -> vote(canvas);
      case "LIBRARY" -> library(canvas);
      case "BET" -> bet(canvas);
      case "PREDICT" -> predict(canvas);
      case "RULE_VOTE" -> ruleVote(canvas);
      case "SETTINGS" -> settings(canvas);
      case "GUIDE" -> guide(canvas);
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
    p.setShader(new LinearGradient(0, 0, 400, H, Color.rgb(29, 21, 46), BG,
        Shader.TileMode.CLAMP));
    c.drawRect(0, 0, 400, H, p);
    p.setShader(null);
    p.setColor(Color.argb(22, 255, 195, 87));
    for (int y = 12; y < H; y += 22)
      for (int x = 11 + (y % 44); x < 400; x += 22) c.drawCircle(x, y, 1.2f, p);
    p.setColor(Color.argb(19, 72, 219, 205));
    for (int i = 0; i < 12; i++) {
      float x = i * 37 - 5, roof = H * .70f + (i * 53 % 90);
      c.drawRect(x, roof, x + 32, H, p);
    }
    p.setColor(Color.argb(30, 255, 195, 87));
    p.setStrokeWidth(1);
    for (int i = 0; i < 7; i++) c.drawLine(0, H * .70f + i * 25, 400, H * .70f + i * 25, p);
    particle.setSeed(17);
    long t = System.currentTimeMillis();
    for (int i = 0; i < 24; i++) {
      float x = particle.nextInt(400),
          y = (particle.nextInt((int) H) + (t / 30 + i * 19) % ((int) H)) % H;
      p.setColor(i % 3 == 0 ? Color.argb(125, 255, 195, 87)
          : Color.argb(100, 72, 219, 205));
      c.drawCircle(x, y, 1 + i % 3, p);
    }
    p.setColor(WHITE);
  }

  private void panel(Canvas c, float x, float y, float w, float h, int fill, int stroke) {
    p.setStyle(Paint.Style.FILL);
    p.setColor(Color.argb(85, 0, 0, 0));
    c.drawRoundRect(x + 3, y + 6, x + w + 3, y + h + 6, 16, 16, p);
    p.setColor(fill);
    c.drawRoundRect(x, y, x + w, y + h, 16, 16, p);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(2.3f);
    p.setColor(stroke);
    c.drawRoundRect(x, y, x + w, y + h, 16, 16, p);
    p.setStyle(Paint.Style.FILL);
    p.setColor(Color.argb(39, 255, 255, 255));
    c.drawRoundRect(x + 9, y + 5, x + w - 9, y + 7, 1, 1, p);
  }

  private void text(Canvas c, String s, float x, float y, float size, int color, boolean center) {
    p.setShader(null);
    p.setColor(color);
    p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
    p.setTextSize(size);
    p.setTextAlign(center ? Paint.Align.CENTER : Paint.Align.LEFT);
    c.drawText(s, x, y, p);
  }

  private void display(Canvas c, String s, float x, float y, float size, int color,
      boolean center) {
    p.setShader(null);
    p.setColor(color);
    p.setTypeface(Typeface.create("sans-serif-condensed-black", Typeface.BOLD));
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
    display(c, label, x + w / 2, y + h / 2 + size * .34f,
        size + 1, color == PANEL ? WHITE : BG, true);
    hits.add(new Hit(id, new RectF(x, y, x + w, y + h)));
  }

  private void label(Canvas c, String s, float y) {
    text(c, s, 200, y, 17, MUTED, true);
  }

  private void header(Canvas c, String sub) {
    display(c, "APÉRO", 22, 49, 27, YELLOW, false);
    display(c, "ROYALE", 22, 78, 27, PINK, false);
    boolean canMenu = !"HOME".equals(g.screen) && !"SETTINGS".equals(g.screen)
        && !"GUIDE".equals(g.screen) && !"STATS".equals(g.screen);
    text(c, canMenu ? "☰" : "✦", 365, 62, 26, CYAN, true);
    if (canMenu) hits.add(new Hit("settings", new RectF(325, 12, 398, 89)));
    p.setColor(PINK);
    c.drawRoundRect(20, 91, 380, 94, 2, 2, p);
    if (sub != null) text(c, sub, 200, 122, 14, MUTED, true);
  }

  private void home(Canvas c) {
    header(c, null);
    boolean compact = H < 800;
    float top = compact ? 111 : 133, heroHeight = compact ? 132 : 238;
    panel(c, 22, top, 356, heroHeight, PANEL, YELLOW);
    if (hero != null) {
      p.setFilterBitmap(true);
      p.setColor(WHITE);
      float dw = 344, dh = heroHeight - 25;
      float wanted = dw / dh;
      int sw = hero.getWidth(), sh = Math.min(hero.getHeight(), (int) (sw / wanted));
      int sy = (hero.getHeight() - sh) / 2;
      c.drawBitmap(hero, new Rect(0, sy, sw, sy + sh),
          new RectF(28, top + 12, 372, top + heroHeight - 13), p);
    }
    float heading = top + heroHeight + (compact ? 29 : 43);
    display(c, g.t("TON APÉRO", "YOUR NIGHT"), 200, heading,
        compact ? 30 : 35, WHITE, true);
    display(c, g.t("DEVIENT LÉGENDE", "YOUR LEGEND"), 200, heading + 37,
        compact ? 30 : 35, YELLOW, true);
    block(
        c,
        g.t(
            "10 MINI-JEUX  •  2-6 JOUEURS  •  FR / EN",
            "10 MINI-GAMES  •  2-6 PLAYERS  •  FR / EN"),
        200,
        heading + 66,
        355,
        12,
        MUTED,
        true);
    float y = H - 312;
    button(c, "new", g.t("LANCER LA SOIRÉE", "START THE PARTY"), 35, y, 330, 59, YELLOW);
    if (actions.hasSavedParty())
      button(c, "resume", g.t("REPRENDRE", "RESUME"), 35, y + 68, 330, 59, CYAN);
    else button(c, "join", g.t("REJOINDRE UNE SALLE", "JOIN A ROOM"), 35, y + 68, 330, 59, CYAN);
    button(c, "stats", g.t("CLASSEMENT", "LEADERBOARD"), 35, y + 136, 155, 55, YELLOW);
    button(
        c, "settings", g.t("MUSIQUE / SPOTIFY", "MUSIC / SPOTIFY"), 205, y + 136, 160, 55, YELLOW);
    button(c, "guide", g.t("DÉCOUVRIR LES 10 DÉFIS", "EXPLORE ALL 10 GAMES"),
        35, y + 200, 330, 51, PANEL);
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
      avatar(c, a, 59, y + 28, 0.46f);
      display(c, a.name, 99, y + 29, 20, WHITE, false);
      text(c, g.t("TOUCHE • AVATAR / PHOTO", "TAP • AVATAR / PHOTO"),
          99, y + 47, 9, MUTED, false);
      text(c, a.language, 340, y + 33, 15, YELLOW, true);
      if (!actions.client() || a.name.equals(((MainActivity) getContext()).network.localName))
        hits.add(new Hit("edit:" + i, new RectF(22, y, 378, y + 57)));
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
      block(c, actions.hostAddress(), 200, H - 272, 350, 15, CYAN, true);
    button(c, "mode", "VOTE".equals(g.mode) ? g.t("MODE : VOTE", "MODE: VOTE") : g.t("MODE : LIBRE", "MODE: FREE"), 30, H - 253, 340, 52, CYAN);
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
    button(c, "host", g.t("HÉBERGER", "HOST ROOM"), 30, H - 60, 165, 44, CYAN);
    button(c, "back", g.t("RETOUR", "BACK"), 205, H - 60, 165, 44, PANEL);
  }

  private boolean viewerEnglish() {
    MainActivity a = (MainActivity) getContext();
    GameEngine.Player viewer = a.localParticipant();
    return viewer != null ? "EN".equals(viewer.language) : g.english();
  }

  private String vt(String fr, String en) { return viewerEnglish() ? en : fr; }

  private void vote(Canvas c) {
    MainActivity a = (MainActivity) getContext();
    GameEngine.Player voter = a.localVoter();
    GameEngine.Player participant = a.localParticipant();
    if (actions.passPending()) { passScreen(c, voter); return; }
    header(c, vt("VOTE ARCADE", "ARCADE VOTE"));
    boolean owner = participant != null && participant.name.equals(g.ruleOwner);
    if (!g.ruleOwner.isEmpty() && g.ruleId < 0) {
      rulePicker(c, owner);
      return;
    }
    text(c, vt("CHOISIS LE PROCHAIN DÉFI", "PICK THE NEXT CHALLENGE"), 200, 164, 17, WHITE, true);
    if (voter != null) {
      avatar(c, voter, 53, 202, .36f);
      text(c, voter.name, 83, 207, 18, YELLOW, false);
    } else text(c, vt("VOTE ENVOYÉ • PATIENCE", "VOTE SENT • HOLD TIGHT"), 200, 207, 16, CYAN, true);
    for (int i = 0; i < Math.min(3, g.offers.length); i++) {
      float y = 236 + i * 103;
      panel(c, 26, y, 348, 87, i == 0 ? Color.rgb(36, 49, 86) : PANEL, TILE[i]);
      text(c, "0" + (i + 1), 50, y + 34, 17, TILE[i], false);
      text(c, title(g.offers[i]), 200, y + 43, 19, WHITE, true);
      text(c, vt("VOTER", "VOTE") + "  →", 324, y + 70, 12, TILE[i], true);
      if (voter != null) hits.add(new Hit("vote:" + i, new RectF(26, y, 374, y + 87)));
    }
    text(c, g.voteCount() + " / " + g.players.size() + " " + vt("VOTES", "VOTES"), 200, 571, 17, CYAN, true);
    bonusCard(c);
    ruleReportButton(c);
    catArea(c, participant);
  }

  private void ruleReportButton(Canvas c) {
    if (g.ruleId >= 3) button(c, "ruleReport", vt("RÈGLE BRISÉE ?  •  VOTE",
        "RULE BROKEN?  •  VOTE"), 49, H - 163, 302, 48, PINK);
  }

  private void ruleVote(Canvas c) {
    header(c, vt("LE TRIBUNAL DE L'APÉRO", "THE PARTY COURT"));
    panel(c, 27, 175, 346, 493, PANEL, PINK);
    pixelCat(c, 200, 276, 5);
    block(c, ruleName(g.ruleId), 200, 382, 320, 23, YELLOW, true);
    block(c, g.reportTarget + vt(" a brisé la règle ?", " broke the rule?"),
        200, 434, 320, 19, WHITE, true);
    GameEngine.Player voter = ((MainActivity) getContext()).localRuleVoter();
    if (voter != null) {
      text(c, voter.name + vt(" décide", " decides"), 200, 487, 17, CYAN, true);
      button(c, "ruleYes", vt("OUI, +1 GORGÉE", "YES, +1 SIP"),
          39, 528, 322, 62, PINK);
      button(c, "ruleNo", vt("NON, ON CONTINUE", "NO, PLAY ON"),
          39, 603, 322, 62, CYAN);
    } else block(c, vt("Le groupe vote…", "The group is voting…"),
        200, 557, 300, 19, CYAN, true);
  }

  private void passScreen(Canvas c, GameEngine.Player next) {
    header(c, vt("PASSE LE TÉLÉPHONE", "PASS THE PHONE"));
    pixelCat(c, 200, 310, 6);
    text(c, next == null ? "?" : next.name, 200, 455, 31, YELLOW, true);
    block(c, vt("Ton vote reste secret. À toi de choisir !", "Your vote stays secret. Your turn to choose!"),
        200, 510, 338, 18, WHITE, true);
    button(c, "readyVote", vt("C'EST MOI", "THAT'S ME"), 36, H - 124, 328, 68, CYAN);
  }

  private void bet(Canvas c) {
    header(c, g.t("LE PARI", "THE WAGER"));
    GameEngine.Player player = g.current();
    if (player == null) return;
    avatar(c, player, 200, 225, 1.2f);
    text(c, player.name, 200, 319, 27, WHITE, true);
    block(c, g.t("Combien de gorgées virtuelles mises-tu sur ta victoire ?",
        "How many virtual sips will you stake on your win?"), 200, 368, 330, 18, YELLOW, true);
    for (int i = 1; i <= 3; i++) {
      float y = 459 + (i - 1) * 77;
      panel(c, 35, y, 330, 67, PANEL, TILE[i - 1]);
      text(c, i + " " + g.t(i == 1 ? "GORGÉE" : "GORGÉES", i == 1 ? "SIP" : "SIPS"),
          200, y + 29, 20, WHITE, true);
      int points = (g.ruleId == 0 ? 2 : 1) * (100 + 50 * (i - 1) + (g.bonusId == 1 ? 50 : 0));
      text(c, g.t("Victoire +", "Win +") + points + " pts", 200, y + 51, 12, MUTED, true);
      hits.add(new Hit("bet:" + i, new RectF(35, y, 365, y + 67)));
    }
    text(c, g.t("Défaite : ta mise rejoint la jauge.", "Lose: your stake fills the meter."),
        200, H - 48, 13, MUTED, true);
    text(c, bonusName(), 200, 441, 13, CYAN, true);
  }

  private void predict(Canvas c) {
    header(c, g.t("LE PRONO DES POTES", "FRIENDS' PREDICTION"));
    GameEngine.Player actor = g.current();
    if (actor == null) return;
    long left = Math.max(0, (g.deadline - ((MainActivity) getContext()).hostNow() + 999) / 1000);
    GameSprites.stage(c, p, g.game, 25, 170, 350, H - 278, 0, System.currentTimeMillis());
    GameSprites.icon(c, p, g.game, 200, 270, 5, 0, System.currentTimeMillis());
    text(c, actor.name.toUpperCase(Locale.ROOT), 200, 395, 27, WHITE, true);
    block(c, vt("Va-t-il réussir ce défi ?", "Will they beat this challenge?"),
        200, 434, 320, 19, YELLOW, true);
    GameEngine.Player voter = ((MainActivity) getContext()).localPredictor();
    if (voter != null) {
      text(c, voter.name + " • " + left + "s", 200, 489, 17, CYAN, true);
      button(c, "predictYes", vt("OUI  •  +35 PTS", "YES  •  +35 PTS"),
          38, 528, 324, 66, CYAN);
      button(c, "predictNo", vt("NON  •  +35 PTS", "NO  •  +35 PTS"),
          38, 610, 324, 66, PINK);
    } else {
      block(c, vt("Prono envoyé. Le défi commence dans un instant…",
          "Prediction locked. The challenge starts shortly…"), 200, 562, 315, 18, WHITE, true);
      text(c, left + "s", 200, 638, 30, CYAN, true);
    }
    text(c, vt("Mauvais prono : +1 gorgée virtuelle", "Wrong call: +1 virtual sip"),
        200, H - 68, 13, MUTED, true);
  }

  private String bonusName() {
    String[] fr = {"CLASSIQUE", "TURBO +50 PTS", "BOUCLIER -1 GORGÉE", "TEMPS +5 S"};
    String[] en = {"CLASSIC", "TURBO +50 PTS", "SHIELD -1 SIP", "TIME +5 S"};
    boolean enForViewer = "VOTE".equals(g.screen) || "LIBRARY".equals(g.screen);
    boolean useEnglish = enForViewer ? viewerEnglish() : g.english();
    return (useEnglish ? en : fr)[Math.floorMod(g.bonusId, 4)];
  }

  private void bonusCard(Canvas c) {
    long ticks = System.currentTimeMillis() / 500;
    int accent = TILE[(int) Math.floorMod(g.bonusId + ticks, 4)];
    panel(c, 28, 617, 344, 75, PANEL, accent);
    text(c, vt("✦ DROP DU TOUR ✦", "✦ ROUND DROP ✦"), 200, 645, 12, MUTED, true);
    text(c, bonusName(), 200, 673, 18, accent, true);
  }

  private void settings(Canvas c) {
    header(c, g.t("RÉGLAGES DE LA SOIRÉE", "PARTY SETTINGS"));
    MainActivity a = (MainActivity) getContext();
    float y = 162;
    button(c, "musicToggle", g.t("MUSIQUE : ", "MUSIC: ") + (a.musicEnabled() ? "ON" : "OFF"),
        30, y, 340, 60, a.musicEnabled() ? CYAN : PANEL);
    button(c, "musicStyle", g.t("AMBIANCE : ", "MOOD: ") + (a.musicStyle() == 0 ? "CHILL" : "ARCADE"),
        30, y + 72, 340, 60, YELLOW);
    button(c, "musicVolume", g.t("VOLUME : ", "VOLUME: ") + Math.round(a.musicVolume() * 100) + "%",
        30, y + 144, 340, 60, CYAN);
    button(c, "effectsToggle", "SFX : " + (a.effectsEnabled() ? "ON" : "OFF"),
        30, y + 216, 340, 60, a.effectsEnabled() ? CYAN : PANEL);
    button(c, "hapticToggle", g.t("VIBRATIONS : ", "HAPTICS: ") + (a.hapticsEnabled() ? "ON" : "OFF"),
        30, y + 288, 340, 60, a.hapticsEnabled() ? CYAN : PANEL);
    panel(c, 30, y + 374, 340, 143, PANEL, PINK);
    text(c, "RADIO APÉRO × SPOTIFY", 200, y + 409, 17, WHITE, true);
    block(c, g.t("Optionnelle. Le jeu marche sans compte Spotify.",
        "Optional. The game works without Spotify."), 200, y + 436, 310, 12, MUTED, true);
    button(c, "spotify", g.t("CONFIG.", "CONFIG"), 47, y + 467, 145, 43, PINK);
    button(c, "radio", "RADIO", 207, y + 467, 146, 43, CYAN);
    button(c, "back", g.t("RETOUR", "BACK"), 35, H - 90, 330, 59, PANEL);
  }

  private static final String[][] GUIDE_RULES = {
    {"Quatre réponses, seize secondes. La vitesse compte : une erreur ou le temps écoulé fait perdre la mise.",
      "Four answers, sixteen seconds. Speed counts: one mistake or timeout loses the wager."},
    {"Prends la pose absurde affichée et tiens bon. Le groupe décide si le défi est réussi.",
      "Strike the silly pose on screen and hold it. The group decides if you pulled it off."},
    {"Écoute le morceau et trouve son nom parmi quatre choix. Mélodies originales hors ligne ou Spotify configuré.",
      "Listen and pick the tune from four choices. Original offline melodies or configured Spotify."},
    {"Frappe dix cibles néon avant la fin du chrono. Les cibles changent de place à chaque touche.",
      "Hit ten neon targets before the clock runs out. Targets jump after each hit."},
    {"Choisis un des six gobelets. Le nombre de gobelets piégés égale ta mise : gros pari, gros risque.",
      "Pick one of six cups. The number of cursed cups equals your wager: higher stakes, higher risk."},
    {"Dessine le mot secret sur l'écran, puis passe le téléphone au devineur. Il choisit la réponse.",
      "Draw the secret prompt, then pass the phone to a guesser. They pick the answer."},
    {"Regarde la séquence de couleurs et rejoue-la dans le même ordre avant la fin du temps.",
      "Watch the color sequence and replay it in order before time runs out."},
    {"Tape au centre du beat quatre fois. Le timing compte plus que la vitesse.",
      "Tap near the center of the beat four times. Timing matters more than speed."},
    {"Raconte ton bluff avec aplomb. Les autres joueurs décident s'ils y croient.",
      "Sell your story with confidence. The other players decide if they believe you."},
    {"Désamorce la bombe à huit touches. Chaque touche doit venir du joueur suivant, sur ce téléphone ou le sien.",
      "Defuse with eight taps. Each tap must come from the next player, on this phone or theirs."}
  };

  private static final String[][] GUIDE_TIPS = {
    {"Le groupe peut hurler des indices absurdes.", "The group may shout outrageously bad hints."},
    {"Le jury n'a pas le droit de rire avant de voter.", "The judges must keep a straight face before voting."},
    {"Chante faux pour brouiller les pistes.", "Sing badly to throw everyone off."},
    {"Les spectateurs comptent à rebours à voix haute.", "Spectators count down out loud."},
    {"La mise augmente aussi les pièges.", "Your wager also increases the traps."},
    {"Le dessinateur ne parle pas pendant le quiz.", "The artist stays silent during the guess."},
    {"Le groupe peut créer une distraction théâtrale.", "The group may stage a dramatic distraction."},
    {"Tout le monde marque le tempo avec les mains.", "Everyone claps along to the beat."},
    {"Exige une voix de personnage pour le récit.", "Demand a character voice for the story."},
    {"Crie le prénom du prochain joueur !", "Shout the next player's name!"}
  };

  private void guide(Canvas c) {
    header(c, g.t("GUIDE DES 10 DÉFIS", "10 GAME GUIDES"));
    int i = Math.floorMod(guideIndex, 10);
    panel(c, 24, 169, 352, 453, PANEL, TILE[i % 4]);
    text(c, String.format(Locale.ROOT, "%02d / 10", i + 1), 200, 206, 17, TILE[i % 4], true);
    GameSprites.icon(c, p, i, 200, 272, 5, 0, System.currentTimeMillis());
    text(c, title(i), 200, 353, title(i).length() > 17 ? 20 : 24, WHITE, true);
    block(c, GUIDE_RULES[i][viewerEnglish() ? 1 : 0], 200, 396, 306, 16, WHITE, true);
    p.setColor(PINK);
    c.drawRoundRect(56, 524, 344, 527, 2, 2, p);
    block(c, GUIDE_TIPS[i][viewerEnglish() ? 1 : 0], 200, 556, 303, 14, YELLOW, true);
    text(c, vt("AVANT : MISE + PRONOS DES AMIS", "FIRST: WAGER + FRIENDS' PICKS"),
        200, 654, 11, CYAN, true);
    button(c, "guidePrev", "←", 29, H - 166, 162, 61, CYAN);
    button(c, "guideNext", "→", 209, H - 166, 162, 61, CYAN);
    button(c, "back", g.t("RETOUR", "BACK"), 35, H - 90, 330, 59, PANEL);
  }

  private void library(Canvas c) {
    if (!g.ruleOwner.isEmpty() && g.ruleId < 0) {
      header(c, g.t("SECRET DÉBLOQUÉ", "SECRET UNLOCKED"));
      GameEngine.Player viewer = ((MainActivity) getContext()).localParticipant();
      rulePicker(c, viewer != null && g.ruleOwner.equals(viewer.name));
      return;
    }
    header(c, g.t("CHOIX LIBRE", "FREE PLAY"));
    text(c, g.t("UN JEU, TON CHOIX", "YOUR GAME, YOUR CALL"), 200, 160, 18, WHITE, true);
    for (int i = 0; i < 10; i++) {
      float x = 24 + (i % 2) * 180, y = 186 + (i / 2) * 78;
      panel(c, x, y, 172, 67, PANEL, TILE[i % 4]);
      GameSprites.icon(c, p, i, x + 33, y + 34, 2.15f, 0, System.currentTimeMillis());
      String name = title(i);
      float size = name.length() > 15 ? 11 : 13;
      text(c, name, x + 110, y + 43, size, WHITE, true);
      if (!actions.client()) hits.add(new Hit("pick:" + i, new RectF(x, y, x + 172, y + 67)));
    }
    bonusCard(c);
    ruleReportButton(c);
    catArea(c, ((MainActivity) getContext()).localParticipant());
  }

  private static final String[][] RULE_NAMES = {
    {"DOUBLE XP", "DOUBLE XP"}, {"PAUSE VERRE", "NO SIPS"},
    {"MARCHE ARRIÈRE", "REVERSE TURNS"}, {"MOT TABOU : OUI", "TABOO WORD: YES"},
    {"PSEUDOS INTERDITS", "NO NICKNAMES"}, {"QUESTIONS SEULEMENT", "QUESTIONS ONLY"},
    {"TOAST ROYAL", "ROYAL TOAST"}, {"NE POINTE PAS", "NO POINTING"},
    {"PAS DE 'MOI'", "NO 'ME'"}, {"DERNIER MOT X2", "LAST WORD TWICE"}
  };

  private static final String[][] RULE_DETAILS = {
    {"Points de victoire doublés", "Double points for wins"},
    {"Les défaites ne coûtent pas de gorgées", "Losses add no sips"},
    {"Ordre des tours inversé", "Reverse turn order"},
    {"Un oui/yes coûte une gorgée si le groupe vote", "A yes costs one sip if the group agrees"},
    {"Appelle les amis autrement que par leur pseudo", "Use anything but their nickname"},
    {"Parle uniquement en questions", "Speak only in questions"},
    {"Porte un toast avant chaque défi", "Give a toast before each challenge"},
    {"Aucun doigt pointé vers quelqu'un", "Do not point at anyone"},
    {"Interdit de dire moi/me", "Do not say me"},
    {"Répète le dernier mot de ta phrase", "Repeat your final word"}
  };

  private String ruleName(int id) {
    return RULE_NAMES[Math.floorMod(id, RULE_NAMES.length)][viewerEnglish() ? 1 : 0];
  }

  private void rulePicker(Canvas c, boolean owner) {
    text(c, g.secretId == 0 ? vt("★  CHAT PIXEL DÉBLOQUÉ  ★", "★  PIXEL CAT UNLOCKED  ★")
        : vt("★  SECRET DÉBLOQUÉ  ★", "★  SECRET UNLOCKED  ★"),
        200, 179, 17, YELLOW, true);
    pixelCat(c, 200, 242, 5);
    block(c, owner ? vt("Choisis une règle pour toute la partie", "Choose one rule for the whole party")
        : g.ruleOwner + vt(" choisit la règle…", " is choosing the rule…"),
        200, 349, 350, 20, WHITE, true);
    for (int i = 0; i < 3; i++) {
      int id = g.ruleOffers.length > i ? g.ruleOffers[i] : i;
      float y = 415 + i * 83;
      panel(c, 27, y, 346, 72, PANEL, TILE[i]);
      text(c, RULE_NAMES[id][viewerEnglish() ? 1 : 0], 200, y + 29, 18, WHITE, true);
      text(c, RULE_DETAILS[id][viewerEnglish() ? 1 : 0], 200, y + 55, 11, MUTED, true);
      if (owner) hits.add(new Hit("rule:" + id, new RectF(27, y, 373, y + 72)));
    }
  }

  private void catArea(Canvas c, GameEngine.Player viewer) {
    float y = H - 105;
    if (g.ruleId >= 0) {
      text(c, ruleName(g.ruleId), 200, y + 24, 15, YELLOW, true);
      text(c, vt("SECRETS", "SECRETS") + " " + Integer.bitCount(g.secretMask) + "/11",
          200, y + 50, 11, MUTED, true);
      return;
    }
    pixelCat(c, 86, y + 24, 2.3f);
    String line = viewer == null ? vt("LE CHAT ATTEND…", "THE CAT WAITS…")
        : vt("TAPOTE LE CHAT", "TAP THE CAT") + "  "
        + (g.catTaps.length > g.indexOf(viewer.name) ? g.catTaps[g.indexOf(viewer.name)] : 0) + "/20";
    text(c, line, 235, y + 25, 15, YELLOW, true);
    text(c, vt("Un secret pour toute la salle", "A secret for the whole room"), 235, y + 48, 11, MUTED, true);
    if (viewer != null) hits.add(new Hit("cat", new RectF(30, y - 14, 150, y + 69)));
  }

  private void pixelCat(Canvas c, float cx, float cy, float size) {
    String[] sprite = {
      "................", "..KK......KK....", ".KYYK....KYYK...", ".KYYYKKKKYYYK...",
      ".KYYYYYYYYYYK...", ".KWWYYYYWWYYK...", ".KWWYYYYWWYYK...", ".KBBYYYYBBYYK...",
      ".KYYYYPPYYYYK...", ".KYYYYPPYYYYK...", ".KYYYYYYYYYYK...", "..KYYYYYYYYK....",
      "...KKYYYYKK.....", "....KYYYYK......", "....K....K......", "................"
    };
    c.save();
    c.translate(cx - 8 * size, cy - 8 * size);
    p.setStyle(Paint.Style.FILL);
    for (int y = 0; y < sprite.length; y++) for (int x = 0; x < 16; x++) {
      int color = switch (sprite[y].charAt(x)) {
        case 'K' -> BG; case 'Y' -> YELLOW; case 'W' -> WHITE; case 'B' -> CYAN; case 'P' -> PINK; default -> 0;
      };
      if (color != 0) { p.setColor(color); c.drawRect(x * size, y * size, (x + 1) * size, (y + 1) * size, p); }
    }
    c.restore();
  }

  private void transition(Canvas c) {
    header(c, g.voteWinner >= 0 ? g.t("LA SALLE A CHOISI", "THE ROOM HAS SPOKEN")
        : g.t("TOURNIQUET DE LA MORT", "WHEEL OF CHAOS"));
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
    avatar(c, player, 200, H * .44f, 1.3f);
    GameSprites.icon(c, p, g.game, 292, H * .44f + 12, 3.3f, 0, now);
    text(c, player.name.toUpperCase(Locale.ROOT), 200, H * .44f + 142, 28, WHITE, true);
    text(c, "→ " + title(g.game), 200, H * .44f + 179, 19, CYAN, true);
    text(c, "✦ " + bonusName(), 200, H * .44f + 204, 13, YELLOW, true);
    if (g.voteWinner >= 0 && g.offers.length == 3) {
      text(c, g.voteCount() + " / " + g.players.size() + " " + g.t("VOTES", "VOTES"),
          200, H * .44f + 252, 16, MUTED, true);
    }
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
    boolean enForViewer = "VOTE".equals(g.screen) || "LIBRARY".equals(g.screen)
        || "GUIDE".equals(g.screen);
    return names[Math.max(0, Math.min(9, i))][(enForViewer ? viewerEnglish() : g.english()) ? 1 : 0];
  }

  private void game(Canvas c) {
    GameEngine.Player a = g.current();
    if (a == null) return;
    header(c, title(g.game));
    avatar(c, a, 52, 146, .42f);
    text(c, a.name, 91, 150, 20, WHITE, false);
    text(c, "#" + (g.turn + 1), 355, 150, 16, YELLOW, true);
    panel(c, 20, 169, 360, H - 261, PANEL, GameSprites.accent(g.game));
    GameSprites.stage(c, p, g.game, 23, 172, 354, H - 267,
        g.game == 3 || g.game == 9 ? g.taps : g.progress, System.currentTimeMillis());
    GameSprites.icon(c, p, g.game, 68, 211, 2.15f,
        g.game == 3 ? g.taps : g.progress, System.currentTimeMillis());
    if (g.deadline > 0) {
      long sec = Math.max(0, (g.deadline - ((MainActivity) getContext()).hostNow() + 999) / 1000);
      text(c, sec + "s", 339, 208, 20, YELLOW, true);
    }
    if (g.juryPhase) { jury(c); buzz(c, a); return; }
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
    text(c, g.t("JAUGE DE GORGÉES", "SIP METER"), 22, H - 62, 12, MUTED, false);
    panel(c, 22, H - 50, 263, 21, BG, PINK);
    p.setColor(PINK);
    float fill = Math.min(1, a.sips / 12f);
    c.drawRoundRect(25, H - 47, 25 + 257 * fill, H - 32, 7, 7, p);
    text(c, a.sips + " ×", 332, H - 34, 20, YELLOW, true);
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
    GameSprites.icon(c, p, 1, 200, 498, 5.2f, g.variant, System.currentTimeMillis());
    text(c, "✦  ✦  ✦", 200, 609, 24, PINK, true);
    button(c, "juryStart", g.t("LE JURY DÉCIDE", "LET THE JURY DECIDE"),
        42, H - 178, 316, 65, CYAN);
  }

  private void jury(Canvas c) {
    GameEngine.Player actor = g.current();
    if (actor == null) return;
    GameEngine.Player judge = ((MainActivity) getContext()).localJudge();
    GameSprites.icon(c, p, g.game, 200, 292, 5.2f, g.juryVotes.length,
        System.currentTimeMillis());
    text(c, actor.name, 200, 415, 26, WHITE, true);
    int cast = 0;
    for (int i = 0; i < g.juryVotes.length; i++) if (i != g.active && g.juryVotes[i] >= 0) cast++;
    text(c, cast + " / " + Math.max(1, g.players.size() - 1) + " "
        + g.t("AVIS", "VOTES"), 200, 464, 18, CYAN, true);
    if (judge != null) {
      block(c, judge.name + g.t(" : défi validé ?", ": challenge complete?"),
          200, 509, 324, 18, YELLOW, true);
      button(c, "judgeYes", g.t("OUI, ÇA PASSE", "YES, IT COUNTS"),
          39, 550, 322, 66, CYAN);
      button(c, "judgeNo", g.t("NON, GORGÉE !", "NO, TAKE A SIP!"),
          39, 631, 322, 66, PINK);
    } else block(c, g.t("Le jury décide…", "The jury is deciding…"),
        200, 555, 320, 21, WHITE, true);
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
    if (predictedTurn != g.turn) { predictedTurn = g.turn; predictedReflex = 0; }
    if (predictedReflex > g.taps && System.currentTimeMillis() - lastPredictionAt > 1200)
      predictedReflex = g.taps;
    int shown = actions.client() ? Math.max(g.taps, predictedReflex) : g.taps;
    block(
        c,
        g.t("Tape 10 cibles avant la fin !", "Hit 10 targets before time runs out!"),
        200,
        257,
        310,
        21,
        WHITE,
        true);
    text(c, g.t("CIBLES", "HITS") + " " + shown + " / 10", 200, 321, 23, YELLOW, true);
    float x = g.reflexX(shown), y = g.reflexY(shown);
    p.setColor(PINK);
    c.drawCircle(x, y, 52, p);
    p.setColor(YELLOW);
    c.drawCircle(x, y, 38, p);
    p.setColor(BG);
    c.drawCircle(x, y, 13, p);
  }

  private void roulette(Canvas c) {
    block(
        c,
        g.t("Choisis un gobelet. " + g.wager + " sont piégés !",
            "Pick a cup. " + g.wager + " are cursed!"),
        200,
        257,
        315,
        21,
        WHITE,
        true);
    for (int i = 0; i < 6; i++) {
      float x = 42 + (i % 3) * 107, y = 348 + (i / 3) * 132;
      panel(c, x, y, 91, 108, g.chosenCup == i ? YELLOW : PANEL,
          g.chosenCup == i ? WHITE : GameSprites.accent(g.game));
      p.setColor(YELLOW);
      c.drawRoundRect(x + 18, y + 26, x + 73, y + 81, 9, 9, p);
      text(c, g.chosenCup == i ? "!" : "?", x + 45, y + 67, 35, BG, true);
      if (g.chosenCup < 0) hits.add(new Hit("cup:" + i, new RectF(x, y, x + 91, y + 108)));
    }
    if (g.chosenCup >= 0)
      text(c, g.t("LE GOBELET SE RETOURNE…", "THE CUP IS TURNING…"),
          200, 641, 16, YELLOW, true);
  }

  private void drawGame(Canvas c) {
    if (ghostTurn != g.turn || g.drawingReady) {
      ghostStrokes.clear();
      ghostTurn = g.turn;
    }
    if (!g.drawingReady) {
      MainActivity app = (MainActivity) getContext();
      GameEngine.Player viewer = app.localParticipant();
      if (app.client() && (viewer == null || !viewer.name.equals(g.current().name))) {
        pixelCat(c, 200, 391, 5);
        block(c, vt("Le dessinateur prépare son chef-d'œuvre…",
            "The artist is preparing a masterpiece…"), 200, 548, 305, 19, YELLOW, true);
        return;
      }
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
      if (app.client() && viewer != null && viewer.name.equals(g.current().name)) {
        p.setColor(BG);
        p.setStrokeWidth(5);
        p.setStrokeCap(Paint.Cap.ROUND);
        for (float[] s : ghostStrokes) c.drawLine(s[0], s[1], s[2], s[3], p);
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
        String[] q = GameEngine.DRAW[(g.variant + opt) % GameEngine.DRAW.length];
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
    long elapsed = ((MainActivity) getContext()).hostNow() - g.started;
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
    long phase = (((MainActivity) getContext()).hostNow() - g.started) % 600;
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
    GameSprites.icon(c, p, 8, 200, 500, 5f, g.variant, System.currentTimeMillis());
    block(
        c,
        g.t("Le groupe juge ta performance.", "The group decides your fate."),
        200,
        603,
        315,
        15,
        MUTED,
        true);
    button(c, "juryStart", g.t("PASSER AU JURY", "LET THE JURY VOTE"),
        38, H - 174, 324, 65, CYAN);
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
    float f = Math.max(.1f, 1 - (((MainActivity) getContext()).hostNow() - g.started) / 30000f);
    p.setColor(PINK);
    c.drawCircle(200, 443, 71 + 20 * f, p);
    p.setColor(YELLOW);
    c.drawCircle(200, 443, 56, p);
    text(c, "TAP!", 200, 453, 26, BG, true);
    hits.add(new Hit("bomb", new RectF(105, 350, 295, 540)));
    text(c, g.taps + " / 8", 200, 603, 25, CYAN, true);
    if (g.bombNext < g.players.size())
      text(c, g.t("À TOI : ", "YOUR TAP: ") + g.players.get(g.bombNext).name,
          200, 645, 18, YELLOW, true);
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
    avatar(c, a, 200, 295, 1.3f);
    GameSprites.icon(c, p, g.game, 315, 292, 2.8f, g.lastWon ? 9 : 0,
        System.currentTimeMillis());
    text(c, a.name, 200, 385, 25, WHITE, true);
    glass(
        c,
        200,
        478,
        g.lastWon ? 1 : Math.max(.05f, 1 - (System.currentTimeMillis() - g.started) / 1400f));
    text(c, g.lastWon ? "+" + g.roundPoints + " PTS"
        : (g.roundSips == 0 ? g.t("AUCUNE GORGÉE", "NO SIP")
            : "+" + g.roundSips + " " + g.t("GORGÉES", "SIPS")),
        200, 582, 24, YELLOW, true);
    int right = 0, wrong = 0;
    for (int i = 0; i < g.predictions.length; i++) if (i != g.active && g.predictions[i] >= 0) {
      if ((g.predictions[i] == 1) == g.lastWon) right++; else wrong++;
    }
    text(c, g.t("PRONOS : ", "PREDICTIONS: ") + right + g.t(" JUSTES", " RIGHT")
        + "  •  " + wrong + g.t(" RATÉS", " WRONG"), 200, 632, 14, CYAN, true);
    if (g.secretId == g.game + 1 && g.ruleId < 0)
      text(c, g.t("★ SECRET DÉBLOQUÉ ★", "★ SECRET UNLOCKED ★"),
          200, 660, 14, YELLOW, true);
    button(c, "board", g.t("CLASSEMENT", "LEADERBOARD"), 35, H - 180, 330, 54, PANEL);
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
    header(c, g.t("HALL OF FAME", "HALL OF FAME"));
    button(c, "partyTab", g.t("SOIRÉE", "PARTY"), 25, 146, 108, 48, statsTab == 0 ? YELLOW : PANEL);
    button(c, "historyTab", g.t("HISTOIRE", "ALL TIME"), 146, 146, 108, 48, statsTab == 1 ? CYAN : PANEL);
    button(c, "gamesTab", g.t("DÉFIS", "GAMES"), 267, 146, 108, 48, statsTab == 2 ? PINK : PANEL);
    MainActivity a = (MainActivity) getContext();
    if (statsTab == 0) {
      ArrayList<GameEngine.Player> rows = new ArrayList<>(g.players);
      rows.sort((x, y) -> Integer.compare(y.score, x.score));
      if (rows.isEmpty()) block(c, g.t("Crée une partie pour voir le classement.", "Create a party to see the leaderboard."), 200, 276, 330, 18, MUTED, true);
      for (int i = 0; i < rows.size(); i++) {
        GameEngine.Player r = rows.get(i);
        float y = 212 + i * 75;
        panel(c, 24, y, 352, 66, PANEL, i == 0 ? YELLOW : Color.rgb(71, 91, 137));
        text(c, i == 0 ? "★" : "#" + (i + 1), 43, y + 40, 18, YELLOW, false);
        avatar(c, r, 91, y + 32, .43f);
        text(c, r.name, 120, y + 29, 17, WHITE, false);
        text(c, r.wins + "/" + r.games + " " + g.t("gagnés", "wins") + "  •  " + r.sips + " " + g.t("gorgées", "sips"), 120, y + 51, 11, MUTED, false);
        text(c, r.score + "", 339, y + 39, 18, CYAN, true);
      }
    } else if (statsTab == 1) {
      ArrayList<String[]> rows = a.store.leaderboard();
      if (rows.isEmpty()) block(c, g.t("Aucun tour terminé encore.", "No finished rounds yet."), 200, 276, 330, 19, MUTED, true);
      for (int i = 0; i < Math.min(rows.size(), 7); i++) {
        String[] r = rows.get(i);
        float y = 212 + i * 65;
        panel(c, 24, y, 352, 57, PANEL, i == 0 ? YELLOW : CYAN);
        text(c, "#" + (i + 1), 43, y + 31, 17, YELLOW, false);
        text(c, r[0], 80, y + 26, 16, WHITE, false);
        int games = Integer.parseInt(r[2]);
        int wins = Integer.parseInt(r[1]);
        text(c, (games == 0 ? 0 : wins * 100 / games) + "%  •  " + r[5] + " " + g.t("gorgées", "sips"), 80, y + 47, 11, MUTED, false);
        text(c, r[4] + "pt", 336, y + 31, 16, CYAN, true);
      }
    } else {
      ArrayList<String[]> rows = a.store.gameBreakdown();
      if (rows.isEmpty()) block(c, g.t("Joue un défi pour voir ses stats.",
          "Play a game to see its stats."), 200, 276, 330, 18, MUTED, true);
      for (int i = 0; i < Math.min(rows.size(), 10); i++) {
        String[] r = rows.get(i);
        int gameIndex = 0;
        for (int j = 0; j < GameEngine.TYPES.length; j++)
          if (GameEngine.TYPES[j].equals(r[0])) gameIndex = j;
        float y = 208 + i * 51;
        panel(c, 24, y, 352, 46, PANEL, GameSprites.accent(gameIndex));
        GameSprites.icon(c, p, gameIndex, 49, y + 23, 1.35f, 0, System.currentTimeMillis());
        text(c, title(gameIndex), 78, y + 20, 12, WHITE, false);
        int games = Integer.parseInt(r[1]);
        int wins = Integer.parseInt(r[2]);
        text(c, wins + "/" + games + "  •  " + r[3] + " " + g.t("gorgées", "sips"),
            78, y + 38, 10, MUTED, false);
        text(c, r[4] + "pt", 340, y + 28, 13, CYAN, true);
      }
    }
    button(c, "back", g.t("RETOUR", "BACK"), 35, H - 90, 330, 59, PINK);
  }

  private void avatar(Canvas c, int type, float x, float y, float scale) {
    if (avatarSheet != null) {
      int index = Math.floorMod(type, 6);
      int cellW = avatarSheet.getWidth() / 3, cellH = avatarSheet.getHeight() / 2;
      int sourceX = (index % 3) * cellW, sourceY = (index / 3) * cellH;
      c.save();
      Path crop = new Path();
      crop.addCircle(x, y, 31 * scale, Path.Direction.CW);
      c.clipPath(crop);
      p.setColor(WHITE);
      p.setFilterBitmap(false);
      c.drawBitmap(avatarSheet, new Rect(sourceX, sourceY, sourceX + cellW, sourceY + cellH),
          new RectF(x - 31 * scale, y - 31 * scale, x + 31 * scale, y + 31 * scale), p);
      c.restore();
      return;
    }
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

  private void avatar(Canvas c, GameEngine.Player player, float x, float y, float scale) {
    if (player.photo.isEmpty()) { avatar(c, player.avatar, x, y, scale); return; }
    try {
      if (!player.photo.equals(cachedPhoto)) {
        if (cachedBitmap != null) cachedBitmap.recycle();
        cachedPhoto = player.photo;
        byte[] bytes = Base64.decode(player.photo, Base64.DEFAULT);
        cachedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
      }
      if (cachedBitmap == null) { avatar(c, player.avatar, x, y, scale); return; }
      c.save();
      Path circle = new Path();
      circle.addCircle(x, y, 31 * scale, Path.Direction.CW);
      c.clipPath(circle);
      p.setColor(WHITE);
      c.drawBitmap(cachedBitmap, null, new RectF(x - 31 * scale, y - 31 * scale, x + 31 * scale, y + 31 * scale), p);
      c.restore();
    } catch (Exception ignored) { avatar(c, player.avatar, x, y, scale); }
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
    if (kind == 0 && x >= 325 && y <= 89 && !"HOME".equals(g.screen)
        && !"SETTINGS".equals(g.screen) && !"GUIDE".equals(g.screen)
        && !"STATS".equals(g.screen)) {
      actions.showSettings();
      return true;
    }
    if ("GAME".equals(g.screen) && actions.client() && g.game == 5 && !g.drawingReady) {
      long now = System.currentTimeMillis();
      if (kind == 1 && now - lastRemoteDrawAt < 24) return true;
      if (kind == 1) lastRemoteDrawAt = now;
      MainActivity app = (MainActivity) getContext();
      GameEngine.Player actor = g.current();
      if (actor != null && actor.name.equals(app.network.localName)) {
        if (kind == 0 && x >= 42 && x <= 358 && y >= 283 && y <= 563) {
          ghostDrawing = true;
          ghostX = x; ghostY = y;
        } else if (kind == 1 && ghostDrawing && x >= 42 && x <= 358
            && y >= 283 && y <= 563 && ghostStrokes.size() < 500) {
          ghostStrokes.add(new float[] {ghostX, ghostY, x, y});
          ghostX = x; ghostY = y;
          invalidate();
        } else if (kind == 2) ghostDrawing = false;
      }
      actions.remoteTouch(x, y, kind, H);
      return true;
    }
    if ("GAME".equals(g.screen) && !actions.client()) {
      GameEngine.Player actor = g.current();
      if (actor != null && ((MainActivity) getContext()).network.isRemote(actor.name)
          && !g.juryPhase && g.game != 9 && !(g.game == 5 && g.drawingReady)) return true;
    }
    if ("LOBBY".equals(g.screen) && actions.client()) {
      if (kind == 0) handle(x, y, kind);
      return true;
    }
    if (("SETTINGS".equals(g.screen) || "GUIDE".equals(g.screen) || "STATS".equals(g.screen)
        || "HOME".equals(g.screen)) && actions.client()) {
      if (kind == 0) handle(x, y, kind);
      return true;
    }
    if ("GAME".equals(g.screen) && g.game == 9 && actions.client()) {
      if (kind == 0) handle(x, y, kind);
      return true;
    }
    if ("PREDICT".equals(g.screen) || "RULE_VOTE".equals(g.screen)
        || ("GAME".equals(g.screen) && g.juryPhase)) {
      if (kind == 0) handle(x, y, kind);
      return true;
    }
    if ("GAME".equals(g.screen) && actions.client() && g.game == 3) {
      if (kind == 0) handleClientReflex(x, y);
      return true;
    }
    if ("GAME".equals(g.screen) && actions.client() && (g.game == 7 || g.game == 4)) {
      if (kind == 0) handle(x, y, kind);
      return true;
    }
    if ("VOTE".equals(g.screen) || "LIBRARY".equals(g.screen)) {
      if (kind == 0) handle(x, y, kind);
      return true;
    }
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

  private void handleClientReflex(float x, float y) {
    MainActivity app = (MainActivity) getContext();
    GameEngine.Player current = g.current();
    if (current == null || !current.name.equals(app.network.localName)) return;
    if (predictedTurn != g.turn) { predictedTurn = g.turn; predictedReflex = g.taps; }
    int step = Math.max(g.taps, predictedReflex);
    if (step >= 10 || Math.hypot(x - g.reflexX(step), y - g.reflexY(step)) > 55) return;
    predictedReflex = step + 1;
    lastPredictionAt = System.currentTimeMillis();
    actions.reflexTap(step);
    actions.click();
    invalidate();
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
          if (g.strokes.size() % 12 == 0) actions.save();
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
          actions.click();
          actions.reflexTap(g.taps);
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
      case "board" -> actions.stats();
      case "mode" -> actions.setMode("VOTE".equals(g.mode) ? "FREE" : "VOTE");
      case "cat" -> actions.catTap();
      case "partyTab" -> { statsTab = 0; invalidate(); }
      case "historyTab" -> { statsTab = 1; invalidate(); }
      case "gamesTab" -> { statsTab = 2; invalidate(); }
      case "settings" -> actions.showSettings();
      case "guide" -> actions.guide();
      case "guidePrev" -> { guideIndex = Math.floorMod(guideIndex - 1, 10); invalidate(); }
      case "guideNext" -> { guideIndex = (guideIndex + 1) % 10; invalidate(); }
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
      case "predictYes" -> actions.predict(true);
      case "predictNo" -> actions.predict(false);
      case "ruleReport" -> actions.reportRule();
      case "ruleYes" -> actions.ruleVote(true);
      case "ruleNo" -> actions.ruleVote(false);
      case "juryStart" -> actions.beginJury();
      case "judgeYes" -> actions.judge(true);
      case "judgeNo" -> actions.judge(false);
      case "readyVote" -> actions.confirmPass();
      case "musicToggle" -> actions.toggleMusic();
      case "musicStyle" -> actions.changeMusicStyle();
      case "musicVolume" -> actions.changeMusicVolume();
      case "effectsToggle" -> actions.toggleEffects();
      case "hapticToggle" -> actions.toggleHaptics();
      case "spotify" -> actions.spotifySettings();
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
        long elapsed = ((MainActivity) getContext()).hostNow() - g.started;
        int beat = (int) (elapsed / 600);
        int offset = (int) (elapsed % 600);
        if (Math.abs(offset - 300) <= 175 && beat > g.progress) actions.rhythmTap(beat);
      }
      case "bomb" -> actions.bombTap();
      default -> {
        if (id.startsWith("vote:")) { actions.vote(Integer.parseInt(id.substring(5))); return; }
        if (id.startsWith("bet:")) { actions.placeBet(Integer.parseInt(id.substring(4))); return; }
        if (id.startsWith("rule:")) { actions.chooseRule(Integer.parseInt(id.substring(5))); return; }
        if (id.startsWith("pick:")) { actions.selectGame(Integer.parseInt(id.substring(5))); return; }
        if (id.startsWith("edit:")) { actions.editPlayer(Integer.parseInt(id.substring(5))); return; }
        if (id.startsWith("answer:")) {
          int selected = Integer.parseInt(id.substring(7));
          actions.finishGame(selected == g.target);
        } else if (id.startsWith("cup:")) {
          int cup = Integer.parseInt(id.substring(4));
          actions.selectCup(cup);
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
