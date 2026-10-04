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
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

/** Large touch targets, high contrast, vector avatars and animated arcade presentation. */
public final class ArcadeView extends View {
  public interface Actions {
    void save();

    void finishGame(boolean won);

    void enterGame();

    void readyTurn();

    void drawingReady();

    void drawGuess(int choice);

    void crewPick(int choice);

    void addPlayer();

    void editPlayer(int index);

    void vote(int choice);

    void placeBet(int sips);

    void predict(boolean win);

    void beginJury();

    void bluffTruth(boolean trueStory);

    void judge(boolean yes);

    void reflexTap(int expectedStep);

    void rhythmTap(int beat);

    void selectCup(int cup);

    void reportRule();

    void ruleVote(boolean yes);

    void bombTap();

    void bombPass(int targetPlayer);

    void bombCut(int wire);

    boolean passPending();

    void confirmPass();

    void hiddenTap(int slot);

    void chooseRule(int id);

    void selectGame(int id);

    void setMode(String mode);

    void newParty();

    void clearRoster();

    void showPartyMenu();

    void showRadioDock();

    void resumeParty();

    boolean hasSavedParty();

    void startHost();

    void joinRoom();

    void showSettings();

    void guide();

    void editMusicLink();

    void setMusicProvider(int provider);

    void openMusicProvider();

    void toggleMusic();

    void toggleEffects();

    void toggleHaptics();

    void changeMusicStyle();

    void changeMusicVolume();

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
  private float H = 820, scale = 1, offsetX = 0, strokeX, strokeY;
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
  private int settingsTab = 0;
  private int guideIndex = 0;
  private String cachedPhoto = "";
  private Bitmap cachedBitmap;
  private final Bitmap hero;
  private final Bitmap avatarSheet;
  private final Bitmap sceneAtlas;
  private static final int BG = Color.rgb(16, 18, 22),
      PANEL = Color.rgb(34, 38, 43),
      WHITE = Color.rgb(247, 241, 227),
      CYAN = Color.rgb(111, 181, 174),
      PINK = Color.rgb(182, 86, 91),
      YELLOW = Color.rgb(216, 177, 103),
      MUTED = Color.rgb(166, 170, 166);
  private static final int[] TILE = {
    Color.rgb(182, 86, 91),
    Color.rgb(111, 181, 174),
    Color.rgb(216, 177, 103),
    Color.rgb(147, 130, 174)
  };

  private static final class Hit {
    final String id;
    final String label;
    final RectF area;

    Hit(String id, RectF area) {
      this(id, area, id);
    }

    Hit(String id, RectF area, String label) {
      this.id = id;
      this.area = area;
      this.label = label;
    }
  }

  private int hoverHit = -1;
  private final AccessibilityNodeProvider accessibleControls = new AccessibilityNodeProvider() {
    @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id) {
      if (id == View.NO_ID) {
        AccessibilityNodeInfo root = AccessibilityNodeInfo.obtain(ArcadeView.this);
        onInitializeAccessibilityNodeInfo(root);
        root.setClassName("android.view.ViewGroup");
        root.setVisibleToUser(true);
        root.setImportantForAccessibility(true);
        for (int i = 0; i < hits.size(); i++) root.addChild(ArcadeView.this, i);
        return root;
      }
      if (id < 0 || id >= hits.size()) return null;
      Hit hit = hits.get(id);
      AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
      node.setSource(ArcadeView.this, id);
      node.setParent(ArcadeView.this);
      node.setPackageName(getContext().getPackageName());
      node.setClassName("android.widget.Button");
      node.setText(hit.label);
      node.setContentDescription(hit.label);
      node.setEnabled(true);
      node.setVisibleToUser(true);
      node.setImportantForAccessibility(true);
      node.setFocusable(true);
      node.setClickable(true);
      node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
      Rect relative = new Rect(Math.round(offsetX + hit.area.left * scale),
          Math.round(hit.area.top * scale), Math.round(offsetX + hit.area.right * scale),
          Math.round(hit.area.bottom * scale));
      node.setBoundsInParent(relative);
      int[] location = new int[2];
      getLocationOnScreen(location);
      Rect screenBounds = new Rect(relative);
      screenBounds.offset(location[0], location[1]);
      node.setBoundsInScreen(screenBounds);
      return node;
    }

    @Override public boolean performAction(int id, int action, Bundle args) {
      if (id < 0 || id >= hits.size() || action != AccessibilityNodeInfo.ACTION_CLICK) return false;
      RectF target = new RectF(hits.get(id).area);
      post(() -> {
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent tap = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
            offsetX + target.centerX() * scale, target.centerY() * scale, 0);
        onTouchEvent(tap);
        tap.recycle();
      });
      return true;
    }
  };

  @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() {
    return accessibleControls;
  }

  @Override public boolean dispatchHoverEvent(MotionEvent event) {
    AccessibilityManager manager = (AccessibilityManager)
        getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
    if (manager == null || !manager.isTouchExplorationEnabled()) return super.dispatchHoverEvent(event);
    int found = -1;
    float x = (event.getX() - offsetX) / scale, y = event.getY() / scale;
    for (int i = 0; i < hits.size(); i++) if (hits.get(i).area.contains(x, y)) {
      found = i;
      break;
    }
    if (event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) found = -1;
    if (found != hoverHit) {
      if (hoverHit >= 0) announceVirtual(hoverHit, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
      hoverHit = found;
      if (found >= 0) announceVirtual(found, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
    }
    return found >= 0;
  }

  private void announceVirtual(int id, int type) {
    if (id >= hits.size() || getParent() == null) return;
    AccessibilityEvent event = AccessibilityEvent.obtain(type);
    event.setSource(this, id);
    event.setPackageName(getContext().getPackageName());
    event.setClassName("android.widget.Button");
    event.getText().add(hits.get(id).label);
    getParent().requestSendAccessibilityEvent(this, event);
  }

  public ArcadeView(Context context, GameEngine game, Actions actions) {
    super(context);
    g = game;
    this.actions = actions;
    hero = BitmapFactory.decodeResource(getResources(), R.drawable.arcade_lounge_hero);
    avatarSheet = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_sheet);
    sceneAtlas = BitmapFactory.decodeResource(getResources(), R.drawable.game_scene_atlas);
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  }

  public float virtualHeight() {
    return H;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    scale = Math.min(getWidth() / 400f, getHeight() / 820f);
    H = getHeight() / scale;
    offsetX = (getWidth() - 400 * scale) / 2f;
    canvas.drawColor(BG);
    canvas.save();
    canvas.translate(offsetX, 0);
    canvas.scale(scale, scale);
    hits.clear();
    background(canvas);
    switch (g.screen) {
      case "HOME" -> home(canvas);
      case "LOBBY" -> lobby(canvas);
      case "VOTE" -> vote(canvas);
      case "RULE_PICK" -> rulePick(canvas);
      case "LIBRARY" -> library(canvas);
      case "BET" -> bet(canvas);
      case "HANDOFF" -> handoff(canvas);
      case "PREDICT" -> predict(canvas);
      case "CREW" -> crew(canvas);
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
    postInvalidateDelayed("GAME".equals(g.screen) || "TRANSITION".equals(g.screen)
        || "HANDOFF".equals(g.screen) ? 33 : 80);
  }

  private void background(Canvas c) {
    p.setShader(new LinearGradient(0, 0, 400, H, Color.rgb(35, 27, 31), BG,
        Shader.TileMode.CLAMP));
    c.drawRect(0, 0, 400, H, p);
    p.setShader(null);
    p.setColor(Color.argb(13, 216, 177, 103));
    for (int y = 12; y < H; y += 32)
      for (int x = 11 + (y % 64); x < 400; x += 32) c.drawCircle(x, y, .7f, p);
    p.setColor(Color.argb(15, 111, 181, 174));
    for (int i = 0; i < 12; i++) {
      float x = i * 37 - 5, roof = H * .70f + (i * 53 % 90);
      c.drawRect(x, roof, x + 32, H, p);
    }
    p.setColor(Color.argb(22, 216, 177, 103));
    p.setStrokeWidth(1);
    for (int i = 0; i < 7; i++) c.drawLine(0, H * .70f + i * 25, 400, H * .70f + i * 25, p);
    particle.setSeed(17);
    long t = System.currentTimeMillis();
    for (int i = 0; i < 9; i++) {
      float x = particle.nextInt(400),
          y = (particle.nextInt((int) H) + (t / 30 + i * 19) % ((int) H)) % H;
      p.setColor(i % 3 == 0 ? Color.argb(65, 216, 177, 103)
          : Color.argb(60, 111, 181, 174));
      c.drawCircle(x, y, 1 + i % 2, p);
    }
    p.setColor(WHITE);
  }

  private void scene(Canvas c, int game, float x, float y, float w, float h) {
    if (sceneAtlas == null) return;
    int col = Math.floorMod(game, 5), row = Math.floorMod(game, 10) / 5;
    int sw = sceneAtlas.getWidth(), sh = sceneAtlas.getHeight();
    int left = col * sw / 5, right = (col + 1) * sw / 5;
    int top = row * sh / 2, bottom = (row + 1) * sh / 2;
    c.save();
    c.clipRect(x, y, x + w, y + h);
    p.setShader(null);
    p.setColor(WHITE);
    p.setFilterBitmap(true);
    c.drawBitmap(sceneAtlas, new Rect(left, top, right, bottom),
        new RectF(x, y, x + w, y + h), p);
    p.setShader(new LinearGradient(x, y, x, y + h,
        Color.argb(155, 8, 11, 29), Color.argb(205, 8, 11, 29), Shader.TileMode.CLAMP));
    c.drawRect(x, y, x + w, y + h, p);
    p.setShader(null);
    p.setColor(GameSprites.accent(game));
    c.drawRect(x, y, x + w, y + 5, p);
    float scan = y + (System.currentTimeMillis() % 4200L) / 4200f * h;
    p.setColor((25 << 24) | (GameSprites.accent(game) & 0x00ffffff));
    c.drawRect(x, scan, x + w, scan + 3, p);
    c.restore();
  }

  private void panel(Canvas c, float x, float y, float w, float h, int fill, int stroke) {
    p.setStyle(Paint.Style.FILL);
    p.setColor(Color.argb(85, 0, 0, 0));
    c.drawRoundRect(x + 3, y + 6, x + w + 3, y + h + 6, 9, 9, p);
    p.setColor(fill);
    c.drawRoundRect(x, y, x + w, y + h, 9, 9, p);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(1.5f);
    p.setColor(stroke);
    c.drawRoundRect(x, y, x + w, y + h, 9, 9, p);
    p.setStyle(Paint.Style.FILL);
    p.setColor(Color.argb(18, 255, 255, 255));
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
    } while (size > 14);
    display(c, label, x + w / 2, y + h / 2 + size * .34f,
        size + 1, color == PANEL ? WHITE : BG, true);
    hits.add(new Hit(id, new RectF(x, y, x + w, y + h), label));
  }

  private void label(Canvas c, String s, float y) {
    text(c, s, 200, y, 17, MUTED, true);
  }

  private void header(Canvas c, String sub) {
    display(c, "APÉRO", 22, 49, 27, WHITE, false);
    display(c, "ROYALE", 22, 78, 27, YELLOW, false);
    boolean canMenu = !"HOME".equals(g.screen) && !"SETTINGS".equals(g.screen)
        && !"GUIDE".equals(g.screen) && !"STATS".equals(g.screen);
    if (canMenu) {
      text(c, "♫", 291, 61, 25, YELLOW, true);
      hits.add(new Hit("musicShortcut", new RectF(263, 11, 322, 89),
          vt("Ouvrir la musique", "Open music")));
    }
    text(c, canMenu ? "☰" : "✦", 365, 62, 26, YELLOW, true);
    if (canMenu) hits.add(new Hit("settings", new RectF(325, 12, 398, 89),
        vt("Réglages", "Settings")));
    p.setColor(YELLOW);
    c.drawRoundRect(20, 91, 380, 94, 2, 2, p);
    if (sub != null) text(c, sub, 200, 122, 14, MUTED, true);
  }

  private void home(Canvas c) {
    header(c, null);
    float top = 111, heroHeight = Math.min(420, H - 480);
    panel(c, 20, top, 360, heroHeight, PANEL, YELLOW);
    if (hero != null) {
      p.setFilterBitmap(true);
      p.setColor(WHITE);
      float dw = 348, dh = heroHeight - 12;
      float wanted = dw / dh;
      int sw = hero.getWidth(), sh = Math.min(hero.getHeight(), (int) (sw / wanted));
      int sy = (hero.getHeight() - sh) / 2;
      c.drawBitmap(hero, new Rect(0, sy, sw, sy + sh),
          new RectF(26, top + 6, 374, top + heroHeight - 6), p);
    }
    p.setShader(new LinearGradient(0, top + heroHeight * .4f, 0, top + heroHeight,
        Color.TRANSPARENT, Color.rgb(12, 14, 24), Shader.TileMode.CLAMP));
    c.drawRect(26, top + heroHeight * .4f, 374, top + heroHeight - 6, p);
    p.setShader(null);
    p.setColor(Color.argb(205, 12, 17, 28));
    c.drawRoundRect(38, top + 19, 296, top + 48, 8, 8, p);
    float pulse = (float) (Math.sin(System.currentTimeMillis() / 330.0) * .5 + .5);
    p.setColor(Color.argb(150 + (int) (pulse * 90), 236, 190, 99));
    c.drawCircle(48, top + 34, 3 + pulse * 1.5f, p);
    text(c, g.t("10 JEUX · ZÉRO TEMPS MORT", "10 GAMES · NO DOWNTIME"),
        176, top + 39, 11, YELLOW, true);
    display(c, g.t("FAITES DU", "MAKE SOME"), 43, top + heroHeight - 91, 31, WHITE, false);
    display(c, g.t("BRUIT.", "NOISE."), 43, top + heroHeight - 54, 37, YELLOW, false);
    text(c, g.t("2–6 amis  /  un ou plusieurs téléphones", "2–6 friends  /  one or more phones"),
        43, top + heroHeight - 25, 12, WHITE, false);
    float y = top + heroHeight + 14;
    String launch = g.players.size() > 0
        ? g.t("JOUER AVEC LA MÊME ÉQUIPE  →", "PLAY WITH YOUR CREW  →")
        : g.t("LANCER LA SOIRÉE  →", "START THE PARTY  →");
    button(c, "new", launch, 20, y, 360, 61, YELLOW);
    float sweep = 29 + (System.currentTimeMillis() % 2600) / 2600f * 310;
    p.setColor(Color.argb(165, 255, 247, 212));
    p.setStrokeWidth(2.5f);
    c.drawLine(sweep, y + 5, Math.min(371, sweep + 30), y + 5, p);
    if (actions.hasSavedParty()) {
      button(c, "resume", g.t("↺  REPRENDRE", "↺  RESUME"), 20, y + 72, 174, 55, CYAN);
      button(c, "join", g.t("↗  REJOINDRE", "↗  JOIN ROOM"), 206, y + 72, 174, 55, PANEL);
    } else button(c, "join", g.t("↗  REJOINDRE UNE SALLE", "↗  JOIN A ROOM"),
        20, y + 72, 360, 55, CYAN);
    float navY = y + 143;
    homeTile(c, "stats", "◆", g.t("SCORES", "SCORES"), 20, navY, YELLOW);
    homeTile(c, "guide", "▦", g.t("DÉFIS", "GAMES"), 112, navY, PINK);
    homeTile(c, "musicShortcut", "♫", "RADIO", 204, navY, CYAN);
    homeTile(c, "settings", "⚙", g.t("OPTIONS", "SETTINGS"), 296, navY, WHITE);
    panel(c, 20, navY + 84, 360, 56, Color.rgb(29, 34, 48), Color.argb(95, 255, 255, 255));
    text(c, g.players.isEmpty() ? g.t("LA TABLE VOUS ATTEND", "YOUR TABLE IS WAITING")
        : g.t("L'ÉQUIPE EST PRÊTE", "THE CREW IS READY"),
        36, navY + 108, 13, YELLOW, false);
    text(c, g.players.isEmpty() ? g.t("Lancez une soirée en 2 touches", "Start a party in two taps")
        : g.players.size() + g.t(" portraits sauvegardés", " saved profiles"),
        36, navY + 125, 11, MUTED, false);
    for (int i = 0; i < Math.min(g.players.size(), 4); i++)
      avatar(c, g.players.get(i), 238 + i * 36, navY + 112, .45f);
    text(c, g.t("CHAQUE TOUR APPARTIENT À QUELQU'UN", "EVERY TURN BELONGS TO SOMEONE"),
        200, H - 20, 11, MUTED, true);
  }

  private void homeTile(Canvas c, String id, String icon, String caption,
      float x, float y, int accent) {
    panel(c, x, y, 84, 73, PANEL, Color.argb(110, 255, 255, 255));
    p.setColor(accent);
    c.drawRect(x + 8, y + 6, x + 76, y + 9, p);
    text(c, icon, x + 42, y + 41, 28, accent, true);
    text(c, caption, x + 42, y + 62, 10, WHITE, true);
    hits.add(new Hit(id, new RectF(x, y, x + 84, y + 73), caption));
  }

  private void lobby(Canvas c) {
    header(c, vt("SALLE DES LÉGENDES", "HALL OF LEGENDS"));
    label(c, g.players.size() + " / 6  " + vt("JOUEURS", "PLAYERS"), 158);
    if (!actions.client() && !((MainActivity) getContext()).network.hosting) {
      text(c, vt("CHANGER", "RESET"), 343, 158, 11, MUTED, true);
      hits.add(new Hit("resetRoster", new RectF(289, 137, 393, 178),
          vt("Changer de groupe", "Change group")));
    }
    for (int i = 0; i < g.players.size(); i++) {
      GameEngine.Player a = g.players.get(i);
      float y = 181 + i * 65;
      panel(c, 22, y, 356, 57, PANEL, i == g.active ? CYAN : Color.rgb(67, 80, 119));
      avatar(c, a, 59, y + 28, 0.46f);
      display(c, a.name, 99, y + 29, 20, WHITE, false);
      text(c, vt("TOUCHE • PROFIL / PHOTO", "TAP • PROFILE / PHOTO"),
          99, y + 47, 9, MUTED, false);
      text(c, a.language, 340, y + 33, 15, YELLOW, true);
      if (!actions.client() || a.name.equals(((MainActivity) getContext()).network.localName))
        hits.add(new Hit("edit:" + i, new RectF(22, y, 378, y + 57),
            a.name + vt(" : portrait et profil", ": avatar and profile")));
    }
    if (actions.client()) {
      block(
          c,
          vt("En attente de l'hôte…", "Waiting for the host…"),
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
    String modeLabel = "FREE".equals(g.mode) ? g.t("LIBRE • CHOIX DU JEU", "FREE • PICK A GAME")
        : "TURBO".equals(g.mode) ? g.t("TURBO • DÉFIS EXPRESS", "TURBO • QUICK ROUNDS")
        : g.t("VOTE • LE GROUPE CHOISIT", "VOTE • GROUP CHOOSES");
    button(c, "mode", modeLabel, 30, H - 253, 340, 52, CYAN);
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
    if (actions.client()) {
      int local = g.indexOf(a.network.localName);
      if (local >= 0) return "EN".equals(g.players.get(local).language);
    }
    GameEngine.Player viewer = switch (g.screen) {
      case "PREDICT" -> a.localPredictor();
      case "CREW" -> a.localCrew();
      case "RULE_VOTE" -> a.localRuleVoter();
      case "GAME" -> g.juryPhase ? a.localJudge()
          : g.game == 5 && g.drawingReady ? a.localGuesser() : g.current();
      case "HANDOFF", "BET", "RESULT", "TRANSITION" -> g.current();
      default -> a.localParticipant();
    };
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
    GameEngine.Player actor = g.current();
    text(c, vt("TOUR ", "TURN ") + (g.turn + 1) + "  •  "
        + (actor == null ? "?" : actor.name.toUpperCase(Locale.ROOT)), 200, 157, 18, YELLOW, true);
    text(c, g.passiveStreak >= 2
        ? vt("RETOUR DE LA SALLE EN SCÈNE", "THE WHOLE ROOM IS BACK IN")
        : vt("CHOISIS LE PROCHAIN DÉFI", "PICK THE NEXT CHALLENGE"),
        200, 182, 15, WHITE, true);
    if (voter != null) {
      avatar(c, voter, 53, 218, .36f);
      text(c, voter.name + vt(" vote", " votes"), 83, 223, 18, CYAN, false);
    } else text(c, vt("VOTE ENVOYÉ • PATIENCE", "VOTE SENT • HOLD TIGHT"), 200, 223, 16, CYAN, true);
    for (int i = 0; i < Math.min(3, g.offers.length); i++) {
      float y = 250 + i * 103;
      panel(c, 26, y, 348, 87, i == 0 ? Color.rgb(36, 49, 86) : PANEL, TILE[i]);
      text(c, "0" + (i + 1), 50, y + 34, 17, TILE[i], false);
      text(c, title(g.offers[i]), 200, y + 43, 19, WHITE, true);
      text(c, vt("VOTER", "VOTE") + "  →", 324, y + 70, 12, TILE[i], true);
      if (voter != null) hits.add(new Hit("vote:" + i, new RectF(26, y, 374, y + 87),
          vt("Voter pour ", "Vote for ") + title(g.offers[i])));
    }
    text(c, g.voteCount() + " / " + g.players.size() + " " + vt("VOTES", "VOTES"), 200, 585, 17, CYAN, true);
    bonusCard(c);
    ruleReportButton(c);
    if (voter == null) hiddenWaiting(c);
  }

  private void rulePick(Canvas c) {
    header(c, vt("SECRET DÉBLOQUÉ", "SECRET UNLOCKED"));
    GameEngine.Player viewer = ((MainActivity) getContext()).localParticipant();
    rulePicker(c, viewer != null && g.ruleOwner.equals(viewer.name));
  }

  private void ruleReportButton(Canvas c) {
    if (g.ruleId >= 3) button(c, "ruleReport", vt("RÈGLE BRISÉE ?  •  VOTE",
        "RULE BROKEN?  •  VOTE"), 49, H - 163, 302, 48, PINK);
  }

  private void ruleVote(Canvas c) {
    if (actions.passPending()) { passScreen(c, ((MainActivity) getContext()).localRuleVoter()); return; }
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
    String instruction = "GAME".equals(g.screen) && g.game == 5 && g.drawingReady
        ? vt("Le dessin est prêt. À toi de deviner !", "The drawing is ready. Make your guess!")
        : "CREW".equals(g.screen)
        ? vt("Ton choix va modifier son défi !", "Your choice will shape their challenge!")
        : "PREDICT".equals(g.screen)
        ? vt("Ton prono est secret. À toi de miser sur ton pote !", "Your prediction stays secret. Place your call!")
        : "GAME".equals(g.screen)
            ? vt("Le jury décide sans regarder le vote des autres.", "The jury votes without seeing anyone else's choice.")
            : vt("Ton vote reste secret. À toi de choisir !", "Your vote stays secret. Your turn to choose!");
    block(c, instruction,
        200, 510, 338, 18, WHITE, true);
    text(c, vt("CHRONO EN PAUSE", "TIMER PAUSED"), 200, H - 151, 14, YELLOW, true);
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
      hits.add(new Hit("bet:" + i, new RectF(35, y, 365, y + 67),
          i + " " + g.t("gorgées", "sips")));
    }
    text(c, g.t("Défaite : ta mise rejoint la jauge.", "Lose: your stake fills the meter."),
        200, H - 48, 13, MUTED, true);
    text(c, bonusName(), 200, 441, 13, CYAN, true);
  }

  private void predict(Canvas c) {
    if (actions.passPending()) { passScreen(c, ((MainActivity) getContext()).localPredictor()); return; }
    header(c, g.t("LE PRONO DES POTES", "FRIENDS' PREDICTION"));
    GameEngine.Player actor = g.current();
    if (actor == null) return;
    long left = Math.max(0, (g.deadline - ((MainActivity) getContext()).hostNow() + 999) / 1000);
    GameSprites.stage(c, p, g.game, 25, 170, 350, H - 278, 0, System.currentTimeMillis());
    scene(c, g.game, 25, 170, 350, H - 278);
    GameSprites.icon(c, p, g.game, 200, 270, 5, 0, System.currentTimeMillis());
    text(c, actor.name.toUpperCase(Locale.ROOT), 200, 395, 27, WHITE, true);
    block(c, vt("Tu le couvres ou tu le défies ?", "Back them or challenge them?"),
        200, 434, 320, 19, YELLOW, true);
    GameEngine.Player voter = ((MainActivity) getContext()).localPredictor();
    if (voter != null) {
      text(c, voter.name + " • " + left + "s", 200, 489, 17, CYAN, true);
      button(c, "predictYes", vt("JE LE COUVRE  •  +35 PTS", "I BACK THEM  •  +35 PTS"),
          38, 528, 324, 66, CYAN);
      button(c, "predictNo", vt("JE LE DÉFIE  •  +50 PTS", "I CHALLENGE  •  +50 PTS"),
          38, 610, 324, 66, PINK);
    } else {
      block(c, vt("Prono envoyé. Le défi commence dans un instant…",
          "Prediction locked. The challenge starts shortly…"), 200, 562, 315, 18, WHITE, true);
      text(c, left + "s", 200, 638, 30, CYAN, true);
    }
    String backingFr = (g.game == 4 || g.game == 5 || g.game == 7)
        ? "Couverture : -1 gorgée si échec. "
        : "Couverture : +2 s et -1 gorgée si échec. ";
    String backingEn = (g.game == 4 || g.game == 5 || g.game == 7)
        ? "Back: -1 sip on a loss. "
        : "Back: +2s and -1 sip on a loss. ";
    block(c, vt(backingFr + "Défi : +25 pts si victoire. Mauvais choix : +1 gorgée virtuelle.",
        backingEn + "Challenge: +25 pts on a win. Wrong call: +1 virtual sip."),
        200, H - 80, 346, 12, MUTED, true);
  }

  private void crew(Canvas c) {
    MainActivity app = (MainActivity) getContext();
    GameEngine.Player voter = app.localCrew();
    if (actions.passPending()) { passScreen(c, voter); return; }
    header(c, vt("LA SALLE ENTRE EN JEU", "THE ROOM JOINS IN"));
    GameSprites.stage(c, p, g.game, 24, 164, 352, H - 248, 0, System.currentTimeMillis());
    scene(c, g.game, 24, 164, 352, H - 248);
    GameEngine.Player actor = g.current();
    long left = Math.max(0, (g.deadline - app.hostNow() + 999) / 1000);
    text(c, (actor == null ? "?" : actor.name) + "  •  " + left + "s", 200, 199, 19, YELLOW, true);
    text(c, g.crewCount() + " / " + Math.max(1, g.players.size() - 1)
        + vt(" POTES ONT JOUÉ", " FRIENDS HAVE PLAYED"), 200, 229, 14, CYAN, true);
    if (voter == null) {
      GameSprites.icon(c, p, g.game, 200, 400, 5, g.turn, System.currentTimeMillis());
      block(c, vt("Les potes préparent le défi. Ça démarre vite !",
          "Friends are shaping the challenge. Starting soon!"),
          200, 558, 310, 20, WHITE, true);
      hiddenWaiting(c);
      return;
    }
    text(c, voter.name.toUpperCase(Locale.ROOT), 200, 262, 21, WHITE, true);
    if (g.game == 0 || g.game == 2) {
      if (g.game == 0) {
        String prompt = vt(g.screenPromptFr, g.screenPromptEn);
        if ((prompt == null || prompt.isEmpty()) && g.variant >= 0)
          prompt = (viewerEnglish() ? GameEngine.QUIZ_EN : GameEngine.QUIZ_FR)[g.variant][0];
        block(c, prompt, 200, 312, 315, 19, WHITE, true);
      } else {
        block(c, vt("Écoute et vote !", "Listen and vote!"),
            200, 304, 315, 19, WHITE, true);
        button(c, "tune", vt("▶ ÉCOUTER", "▶ PLAY"), 60, 336, 280, 50, PINK);
      }
      String[] visible = viewerEnglish() ? g.screenChoicesEn : g.screenChoicesFr;
      for (int i = 0; i < 4; i++) {
        String label = visible.length == 4 ? visible[i] : "?";
        if (visible.length != 4 && g.variant >= 0) {
          int option = Math.floorMod(i - g.target, 4);
          label = g.game == 0
              ? (viewerEnglish() ? GameEngine.QUIZ_EN : GameEngine.QUIZ_FR)[g.variant][option + 1]
              : GameEngine.TUNES[(g.variant + option) % GameEngine.TUNES.length][viewerEnglish() ? 1 : 0];
        }
        button(c, "crew:" + i, label, 35, (g.game == 0 ? 365 : 400) + i * 65,
            330, 55, i % 2 == 0 ? CYAN : YELLOW);
      }
    } else if (g.game == 3) {
      block(c, vt("Place une cible pour le sprint de ", "Place a target for ")
          + (actor == null ? "?" : actor.name), 200, 319, 315, 19, WHITE, true);
      String[] fr = {"↖ HAUT GAUCHE", "↗ HAUT DROITE", "↙ BAS GAUCHE", "↘ BAS DROITE"};
      String[] en = {"↖ TOP LEFT", "↗ TOP RIGHT", "↙ BOTTOM LEFT", "↘ BOTTOM RIGHT"};
      for (int i = 0; i < 4; i++)
        button(c, "crew:" + i, (viewerEnglish() ? en : fr)[i],
            31 + i % 2 * 174, 366 + i / 2 * 116, 164, 92, TILE[i]);
    } else if (g.game == 4) {
      block(c, vt("Protège un gobelet. Un soutien peut épargner une gorgée !",
          "Shield a cup. Your support can spare a sip!"), 200, 309, 315, 19, WHITE, true);
      for (int i = 0; i < 6; i++)
        button(c, "crew:" + i, vt("GOBELET ", "CUP ") + (i + 1),
            29 + i % 3 * 116, 375 + i / 3 * 105, 108, 89, i % 2 == 0 ? CYAN : PINK);
    } else if (g.game == 6) {
      block(c, vt("Choisis un symbole pour la chaîne mémoire.",
          "Add one symbol to the memory chain."), 200, 314, 316, 20, WHITE, true);
      String[] symbols = {"◆", "●", "▲", "■"};
      for (int i = 0; i < 4; i++)
        button(c, "crew:" + i, symbols[i], 42 + i % 2 * 164,
            367 + i / 2 * 125, 150, 110, TILE[i]);
    } else if (g.game == 7) {
      block(c, vt("Choisis un temps. Le groupe crée le rythme.",
          "Pick a beat. The room builds the rhythm."), 200, 312, 316, 18, WHITE, true);
      for (int i = 0; i < 8; i++)
        button(c, "crew:" + i, String.valueOf(i + 1),
            27 + i % 4 * 88, 395 + i / 4 * 111, 78, 95, i % 2 == 0 ? CYAN : YELLOW);
    }
    text(c, vt("TON CHOIX COMPTE POUR LE SCORE", "YOUR CHOICE EARNS POINTS"),
        200, H - 58, 13, YELLOW, true);
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
    header(c, g.t("PERSONNALISE TA SOIRÉE", "MAKE IT YOUR NIGHT"));
    MainActivity a = (MainActivity) getContext();
    if (settingsTab == 2) {
      text(c, g.t("SOURCE MUSICALE", "MUSIC SOURCE"), 31, 173, 14, MUTED, false);
      String[] names = {g.t("ORIGINAL", "ORIGINAL"), "SPOTIFY", "DEEZER",
          "APPLE MUSIC", "AMAZON MUSIC", g.t("SILENCE", "SILENCE")};
      String[] symbols = {"♪", "◉", "◆", "♫", "▤", "×"};
      for (int i = 0; i < names.length; i++) {
        float x = i % 2 == 0 ? 30 : 207, y = 192 + (i / 2) * 104;
        boolean selected = i == a.musicProvider();
        panel(c, x, y, 163, 92, selected ? Color.rgb(50, 58, 74) : PANEL,
            selected ? YELLOW : Color.argb(100, 255, 255, 255));
        text(c, symbols[i], x + 81, y + 46, 29, selected ? YELLOW : CYAN, true);
        text(c, names[i], x + 81, y + 75, 12, WHITE, true);
        hits.add(new Hit("provider:" + i, new RectF(x, y, x + 163, y + 92), names[i]));
      }
      block(c, g.t("Une playlist mémorisée s'ouvre d'un geste depuis la radio ♫.",
          "Saved playlists open in one tap from the ♫ radio dock."),
          200, 531, 336, 14, MUTED, true);
      button(c, "radioDock", g.t("OUVRIR LA RADIO  ♫", "OPEN PARTY RADIO  ♫"),
          30, 570, 340, 55, CYAN);
      button(c, "settingsMusic", g.t("RETOUR AU SON", "BACK TO SOUND"),
          35, H - 90, 330, 59, PANEL);
      return;
    }
    button(c, "settingsMusic", g.t("MUSIQUE", "MUSIC"), 30, 151, 163, 49,
        settingsTab == 0 ? YELLOW : PANEL);
    button(c, "settingsParty", g.t("PARTIE", "PARTY"), 207, 151, 163, 49,
        settingsTab == 1 ? YELLOW : PANEL);
    if (settingsTab == 0) {
      String source = MusicLinks.name(a.musicProvider());
      if (a.musicProvider() == MusicLinks.ORIGINAL) source = g.t("Bande originale", "Original score");
      if (a.musicProvider() == MusicLinks.SILENT) source = g.t("Silence", "Silence");
      text(c, g.t("SOURCE MUSICALE", "MUSIC SOURCE"), 31, 236, 13, MUTED, false);
      button(c, "musicProvider", source + "  ›", 30, 247, 340, 58, YELLOW);
      if (MusicLinks.external(a.musicProvider())) {
        button(c, "openMusic", g.t("OUVRIR L'APPLICATION  ↗", "OPEN MUSIC APP  ↗"),
            30, 319, 340, 58, CYAN);
        button(c, "musicLink", a.hasMusicLink() ? g.t("CHANGER LA PLAYLIST", "CHANGE PLAYLIST")
            : g.t("AJOUTER UNE PLAYLIST", "ADD A PLAYLIST"), 30, 390, 340, 53, PANEL);
        block(c, g.t("La lecture reste dans votre application musicale. Revenez au jeu pour continuer la manche.",
            "Playback stays in your music app. Return here to continue the round."),
            200, 473, 336, 13, MUTED, true);
      } else if (a.musicProvider() == MusicLinks.ORIGINAL) {
        button(c, "musicToggle", g.t("BANDE ORIGINALE : ", "GAME SOUNDTRACK: ")
            + (a.musicEnabled() ? "ON" : "OFF"), 30, 319, 340, 55,
            a.musicEnabled() ? CYAN : PANEL);
        button(c, "musicStyle", g.t("AMBIANCE : ", "MOOD: ")
            + (a.musicStyle() == 0 ? "CHILL" : "ARCADE"), 30, 386, 340, 55, PANEL);
        button(c, "musicVolume", g.t("VOLUME : ", "VOLUME: ")
            + Math.round(a.musicVolume() * 100) + "%", 30, 453, 340, 55, PANEL);
      } else {
        block(c, g.t("La bande originale est coupée. Les effets restent configurables ci-dessous.",
            "The soundtrack is muted. Effects remain configurable below."),
            200, 350, 336, 14, MUTED, true);
      }
      text(c, g.t("CONFORT DE JEU", "GAME FEEL"), 31, 548, 13, MUTED, false);
      button(c, "effectsToggle", "SFX : " + (a.effectsEnabled() ? "ON" : "OFF"),
          30, 559, 163, 55, a.effectsEnabled() ? CYAN : PANEL);
      button(c, "hapticToggle", g.t("VIBRATIONS : ", "HAPTICS: ")
          + (a.hapticsEnabled() ? "ON" : "OFF"),
          207, 559, 163, 55, a.hapticsEnabled() ? CYAN : PANEL);
      block(c, g.t("Les effets et vibrations se règlent séparément de la musique.",
          "Effects and haptics are independent from music."),
          200, 646, 336, 12, MUTED, true);
    } else {
      text(c, g.t("MODE DE PARTIE", "PARTY MODE"), 31, 236, 13, MUTED, false);
      if ("LOBBY".equals(a.settingsOrigin()))
        button(c, "mode", g.t("CHANGER : ", "CHANGE: ") + g.mode,
            30, 247, 340, 58, YELLOW);
      else panel(c, 30, 247, 340, 58, PANEL, MUTED);
      if (!"LOBBY".equals(a.settingsOrigin()))
        block(c, g.t("Choisis le mode dans le salon avant de lancer la partie.",
            "Choose a mode in the lobby before starting."), 200, 274, 310, 13, MUTED, true);
      button(c, "guide", g.t("GUIDE DES 10 JEUX", "ALL 10 GAME GUIDES"),
          30, 331, 340, 58, CYAN);
      button(c, "stats", g.t("CLASSEMENT", "LEADERBOARD"),
          30, 405, 340, 58, PANEL);
      panel(c, 30, 489, 340, 135, PANEL, YELLOW);
      text(c, g.t("AUTOUR DE LA TABLE", "AROUND THE TABLE"), 200, 521, 15, YELLOW, true);
      block(c, g.t("Un seul téléphone ? Passe-le à chaque tour. Plusieurs ? Rejoins la salle depuis l'accueil.",
          "One phone? Pass it each turn. More phones? Join the room from home."),
          200, 551, 304, 14, WHITE, true);
    }
    button(c, "back", g.t("RETOUR", "BACK"), 35, H - 90, 330, 59, PANEL);
  }

  private static final String[][] GUIDE_RULES = {
    {"Tous répondent d'abord en secret. Le joueur actif voit la tendance, puis choisit sa réponse en seize secondes.",
      "Everyone answers privately first. The actor sees the room's trend, then answers in sixteen seconds."},
    {"Prends la pose absurde affichée et tiens bon. Le groupe décide si le défi est réussi.",
      "Strike the silly pose on screen and hold it. The group decides if you pulled it off."},
    {"Tous écoutent le motif original et votent. Le joueur actif écoute à son tour puis peut suivre la salle.",
      "Everyone listens to the original motif and votes. The actor hears it next and can follow the room."},
    {"Chaque pote place une cible dans un coin. Le joueur actif doit toucher leur parcours avant le chrono.",
      "Each friend places a target in a corner. The actor must clear their course before time runs out."},
    {"Les potes protègent un gobelet chacun. Le joueur actif choisit : soutien visible, pièges secrets et gorgée épargnée si protégé.",
      "Friends shield a cup each. The actor chooses with visible support, secret traps and one sip spared if shielded."},
    {"Dessine la consigne secrète. Tous les autres devinent, chacun en privé ; la moitié doit trouver pour gagner.",
      "Draw the secret prompt. Everyone else guesses privately; at least half must get it right to win."},
    {"Chaque pote ajoute un symbole. Le joueur actif découvre la chaîne créée par le groupe puis la rejoue.",
      "Each friend adds a symbol. The actor sees the group's chain, then plays it back."},
    {"Chaque pote place un temps sur huit. Les quatre temps retenus forment la mesure à frapper.",
      "Each friend picks one of eight beats. The four chosen beats become the pattern to tap."},
    {"Raconte une anecdote vraie ou inventée, puis verrouille ton secret. Si la majorité du jury se trompe, tu gagnes.",
      "Tell a true or made-up story, then lock your secret. Fool the jury majority to win."},
    {"Touche la bombe le nombre de fois affiché, puis choisis à qui la passer. Quand chacun l'a tenue, coupe un fil au risque de perdre, ou continue le relais.",
      "Tap the bomb the displayed number of times, then choose who gets it. Once everyone held it, risk cutting a wire or keep passing."}
  };

  private static final String[][] GUIDE_TIPS = {
    {"Le groupe peut hurler des indices absurdes.", "The group may shout outrageously bad hints."},
    {"Le jury n'a pas le droit de rire avant de voter.", "The judges must keep a straight face before voting."},
    {"Chante faux pour brouiller les pistes.", "Sing badly to throw everyone off."},
    {"Place une cible piégeuse, puis encourage le sprinteur !", "Place a tricky target, then cheer on the runner!"},
    {"La mise augmente aussi les pièges.", "Your wager also increases the traps."},
    {"Le dessinateur ne parle pas pendant le quiz.", "The artist stays silent during the guess."},
    {"Mélange les couleurs pour dérouter ton pote.", "Mix the colors to confuse your friend."},
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
    text(c, vt("AVANT : MISE + ACTION DE CHACUN", "FIRST: WAGER + EVERYONE ACTS"),
        200, 654, 11, CYAN, true);
    button(c, "guidePrev", "←", 29, H - 166, 162, 61, CYAN);
    button(c, "guideNext", "→", 209, H - 166, 162, 61, CYAN);
    button(c, "back", g.t("RETOUR", "BACK"), 35, H - 90, 330, 59, PANEL);
  }

  private void library(Canvas c) {
    if (!g.ruleOwner.isEmpty() && g.ruleId < 0) {
      header(c, vt("SECRET DÉBLOQUÉ", "SECRET UNLOCKED"));
      GameEngine.Player viewer = ((MainActivity) getContext()).localParticipant();
      rulePicker(c, viewer != null && g.ruleOwner.equals(viewer.name));
      return;
    }
    header(c, vt("CHOIX LIBRE", "FREE PLAY"));
    GameEngine.Player actor = g.current();
    text(c, vt("TOUR ", "TURN ") + (g.turn + 1) + "  •  "
        + (actor == null ? "?" : actor.name.toUpperCase(Locale.ROOT)),
        200, 160, 18, YELLOW, true);
    for (int i = 0; i < 10; i++) {
      float x = 24 + (i % 2) * 180, y = 186 + (i / 2) * 78;
      panel(c, x, y, 172, 67, PANEL, TILE[i % 4]);
      GameSprites.icon(c, p, i, x + 33, y + 34, 2.15f, 0, System.currentTimeMillis());
      String name = title(i);
      block(c, name, x + 114, y + 30, 105, 11, WHITE, true);
      if (!actions.client()) hits.add(new Hit("pick:" + i, new RectF(x, y, x + 172, y + 67),
          title(i)));
    }
    bonusCard(c);
    ruleReportButton(c);
    hiddenWaiting(c);
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
      if (owner) hits.add(new Hit("rule:" + id, new RectF(27, y, 373, y + 72),
          ruleName(id)));
    }
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

  /** A private, optional alley appears only after a remote player has contributed. */
  private void hiddenWaiting(Canvas c) {
    MainActivity app = (MainActivity) getContext();
    if (!actions.client() || !g.hiddenWaiting(app.network.localName)) return;
    int i = g.indexOf(app.network.localName);
    if (i < 0 || i >= g.hiddenKind.length) return;
    int kind = g.hiddenKind[i];
    if (kind < 0) {
      if (i < g.hiddenSolvedTurn.length && g.hiddenSolvedTurn[i] == g.turn) {
        panel(c, 32, 328, 336, 266, Color.rgb(12, 19, 29), CYAN);
        pixelCat(c, 200, 407, 4.2f);
        display(c, vt("SECRET TROUVÉ", "SECRET FOUND"), 200, 491, 25, YELLOW, true);
        boolean chooser = app.network.localName.equals(g.queuedRuleOwner)
            || app.network.localName.equals(g.ruleOwner) && g.ruleId < 0;
        block(c, chooser
            ? vt("Tu choisiras la prochaine règle de la salle.",
                "You'll choose the room's next rule.")
            : vt("Un trophée de plus. La nuit continue.",
                "One more trophy. The night goes on."),
            200, 529, 290, 14, WHITE, true);
        return;
      }
      if (g.hiddenWonMask[i] == 7) return;
      float pulse = (float) Math.sin(System.currentTimeMillis() / 310.0);
      p.setColor(Color.argb(30 + Math.round(18 * (1 + pulse)), 216, 177, 103));
      c.drawRoundRect(274, 337, 355, 425, 12, 12, p);
      p.setColor(Color.rgb(18, 24, 34));
      c.drawRect(284, 348, 347, 420, p);
      p.setColor(YELLOW);
      c.drawRect(297, 375, 309, 381, p);
      c.drawRect(326, 375, 338, 381, p);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(2);
      c.drawArc(303, 384, 332, 406, 10, 160, false, p);
      p.setStyle(Paint.Style.FILL);
      for (int dot = 0; dot < g.hiddenProbe[i]; dot++) {
        p.setColor(CYAN);
        c.drawCircle(302 + dot * 16, 411, 2.5f, p);
      }
      hits.add(new Hit("hidden:-1", new RectF(268, 329, 360, 432),
          vt("Une présence dans la ruelle", "Something in the alley")));
      return;
    }
    int accent = kind == 0 ? YELLOW : kind == 1 ? CYAN : PINK;
    panel(c, 22, 247, 356, 435, Color.rgb(12, 19, 29), accent);
    p.setColor(Color.argb(53, 111, 181, 174));
    for (int j = 0; j < 11; j++) {
      float bx = 35 + j * 32;
      float roof = 356 - (j * 47 % 78);
      c.drawRect(bx, roof, bx + 26, 379, p);
      p.setColor(Color.argb(j % 3 == 0 ? 105 : 38, 216, 177, 103));
      c.drawRect(bx + 7, roof + 15, bx + 12, roof + 22, p);
      p.setColor(Color.argb(53, 111, 181, 174));
    }
    p.setColor(Color.argb(150, 12, 19, 29));
    c.drawRect(28, 275, 372, 379, p);
    String[] titlesFr = {"LE CHAT DES TOITS", "LE CODE DES PATTES", "LE CHAT MIROIR"};
    String[] titlesEn = {"ROOFTOP CAT", "PAW CIPHER", "MIRROR CAT"};
    display(c, (viewerEnglish() ? titlesEn : titlesFr)[kind], 200, 303, 22, accent, true);
    String[] rulesFr = {"Suis le chat de toit en toit.",
        "Lis les quatre empreintes dans l'ordre.", "Touche le reflet horizontal du chat."};
    String[] rulesEn = {"Follow the cat across the rooftops.",
        "Read the four paw prints in order.", "Tap the cat's horizontal reflection."};
    block(c, (viewerEnglish() ? rulesEn : rulesFr)[kind], 200, 333, 316, 14, WHITE, true);
    text(c, vt("TON SECRET · ", "YOUR SECRET · ") + (g.hiddenStep[i] + 1) + "/4",
        200, 367, 13, MUTED, true);
    if (kind == 1) {
      for (int j = 0; j < 4; j++) {
        int glyph = (g.hiddenSeed[i] >>> (j * 2)) & 3;
        p.setColor(j < g.hiddenStep[i] ? accent : TILE[glyph]);
        c.drawCircle(145 + j * 36, 382, 6, p);
      }
    }
    int target = g.hiddenTarget(i);
    int source = kind == 2 ? target ^ 1 : -1;
    for (int slot = 0; slot < 4; slot++) {
      float x = slot % 2 == 0 ? 43 : 221;
      float y = slot < 2 ? 399 : 525;
      int tileAccent = TILE[slot];
      panel(c, x, y, 136, 107, Color.rgb(27, 37, 48), tileAccent);
      p.setColor(Color.argb(48, 255, 255, 255));
      for (int n = 0; n < 4; n++) c.drawRect(x + 14 + n * 28, y + 82,
          x + 26 + n * 28, y + 86, p);
      if (kind == 0 && slot == target) pixelCat(c, x + 68, y + 46, 3.4f);
      else if (kind == 2 && slot == source) {
        c.save();
        c.scale(-1, 1, x + 68, y + 46);
        pixelCat(c, x + 68, y + 46, 3.2f);
        c.restore();
      } else if (kind == 1) {
        display(c, "0" + (slot + 1), x + 68, y + 63, 36, tileAccent, true);
      } else {
        text(c, "✦", x + 68, y + 61, 25, tileAccent, true);
      }
      hits.add(new Hit("hidden:" + slot, new RectF(x, y, x + 136, y + 107),
          vt("Case secrète ", "Secret tile ") + (slot + 1)));
    }
    if (kind == 2) {
      p.setColor(PINK);
      c.drawRect(196, 404, 204, 624, p);
    }
    text(c, vt("Premier à finir : nouvelle règle.", "First to finish: a new room rule."),
        200, 655, 11, MUTED, true);
  }

  private boolean hiddenTouch(float x, float y) {
    MainActivity app = (MainActivity) getContext();
    if (!actions.client() || !g.hiddenWaiting(app.network.localName)) return false;
    int i = g.indexOf(app.network.localName);
    if (i < 0 || i >= g.hiddenKind.length) return false;
    if (g.hiddenKind[i] < 0) {
      if (x >= 268 && x <= 360 && y >= 329 && y <= 432) {
        actions.hiddenTap(-1);
        actions.click();
        return true;
      }
      return false;
    }
    for (int slot = 0; slot < 4; slot++) {
      float left = slot % 2 == 0 ? 43 : 221;
      float top = slot < 2 ? 399 : 525;
      if (x >= left && x <= left + 136 && y >= top && y <= top + 107) {
        actions.hiddenTap(slot);
        actions.click();
        return true;
      }
    }
    return x >= 22 && x <= 378 && y >= 247 && y <= 682;
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
    if (g.game == 1 || g.game == 8 || g.game == 9)
      text(c, g.t("TOUTE LA SALLE JOUE", "EVERYONE JOINS THIS ROUND"),
          200, H * .44f + 226, 14, WHITE, true);
    if (g.voteWinner >= 0 && g.offers.length == 3) {
      text(c, g.voteCount() + " / " + g.players.size() + " " + g.t("VOTES", "VOTES"),
          200, H * .44f + 252, 16, MUTED, true);
    }
    button(c, "enter", g.t("PASSER AU JOUEUR", "PASS TO PLAYER"), 32, H - 110, 336, 65, YELLOW);
  }

  private void handoff(Canvas c) {
    GameEngine.Player actor = g.current();
    if (actor == null) return;
    header(c, g.t("TON TOUR ARRIVE", "YOUR TURN IS HERE"));
    panel(c, 27, 154, 346, H - 304, PANEL, GameSprites.accent(g.game));
    GameSprites.stage(c, p, g.game, 31, 159, 338, H - 314, 0, System.currentTimeMillis());
    scene(c, g.game, 31, 159, 338, H - 314);
    text(c, g.t("TOUR ", "TURN ") + (g.turn + 1), 200, 205, 20, YELLOW, true);
    avatar(c, actor, 200, 292, 1.23f);
    display(c, actor.name.toUpperCase(Locale.ROOT), 200, 399, 37, WHITE, true);
    GameSprites.icon(c, p, g.game, 200, 484, 3.7f, 0, System.currentTimeMillis());
    display(c, title(g.game), 200, 572, 26, CYAN, true);
    block(c, g.t("Passe le téléphone. Le chrono attend que tu sois prêt.",
        "Pass the phone. The clock waits until you're ready."),
        200, 615, 305, 16, WHITE, true);
    MainActivity app = (MainActivity) getContext();
    boolean ownPhone = actions.client() ? actor.name.equals(app.network.localName)
        : !app.network.isRemote(actor.name);
    if (ownPhone) button(c, "readyTurn", g.t("JE SUIS PRÊT", "I'M READY"),
        32, H - 113, 336, 69, YELLOW);
    else block(c, g.t("En attente de " + actor.name + "…", "Waiting for " + actor.name + "…"),
        200, H - 80, 332, 18, CYAN, true);
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
    boolean enForViewer = actions.client() || "VOTE".equals(g.screen) || "LIBRARY".equals(g.screen)
        || "GUIDE".equals(g.screen);
    return names[Math.max(0, Math.min(9, i))][(enForViewer ? viewerEnglish() : g.english()) ? 1 : 0];
  }

  private void game(Canvas c) {
    GameEngine.Player a = g.current();
    if (a == null) return;
    if (g.juryPhase && actions.passPending()) {
      passScreen(c, ((MainActivity) getContext()).localJudge());
      return;
    }
    if (g.game == 5 && g.drawingReady && actions.passPending()) {
      passScreen(c, ((MainActivity) getContext()).localGuesser());
      return;
    }
    if (g.game == 9 && actions.passPending()) {
      passScreen(c, ((MainActivity) getContext()).localBomber());
      return;
    }
    header(c, title(g.game));
    avatar(c, a, 52, 146, .42f);
    text(c, a.name, 91, 150, 20, WHITE, false);
    text(c, "TURBO".equals(g.mode) ? "⚡ #" + (g.turn + 1) : "#" + (g.turn + 1),
        355, 150, 16, YELLOW, true);
    panel(c, 20, 169, 360, H - 261, PANEL, GameSprites.accent(g.game));
    GameSprites.stage(c, p, g.game, 23, 172, 354, H - 267,
        g.game == 3 || g.game == 9 ? g.taps : g.progress, System.currentTimeMillis());
    scene(c, g.game, 23, 172, 354, H - 267);
    GameSprites.icon(c, p, g.game, 72, 211, 3f,
        g.game == 3 ? g.taps : g.progress, System.currentTimeMillis());
    if (g.deadline > 0) {
      long sec = Math.max(0, (g.deadline - ((MainActivity) getContext()).hostNow() + 999) / 1000);
      text(c, sec + "s", 339, 208, 20, YELLOW, true);
    }
    if (g.juryPhase) { jury(c); buzz(c, a); return; }
    if (actions.client() && g.game != 9 && g.game != 5
        && !a.name.equals(((MainActivity) getContext()).network.localName)) {
      spectator(c, a);
      buzz(c, a);
      return;
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

  private void spectator(Canvas c, GameEngine.Player actor) {
    GameSprites.icon(c, p, g.game, 200, 374, 6.2f, g.progress,
        System.currentTimeMillis());
    block(c, vt(actor.name + " joue. Tu as déjà influencé ce défi !",
        actor.name + " is playing. You shaped this challenge!"),
        200, 535, 312, 20, WHITE, true);
    String[] involvement = switch (g.game) {
      case 0, 2 -> new String[] {"Ta réponse pèse dans la tendance et rapporte des points.",
          "Your answer shaped the room's trend and can earn points."};
      case 3 -> new String[] {"Ta cible fait partie du parcours qu'il doit franchir.",
          "Your target is part of the course they must clear."};
      case 4 -> new String[] {"Ton gobelet protégé peut lui épargner une gorgée.",
          "Your shielded cup can spare them a sip."};
      case 6 -> new String[] {"Ton symbole ouvre une partie de la chaîne.",
          "Your symbol builds part of the chain."};
      case 7 -> new String[] {"Ton temps influence la mesure du groupe.",
          "Your beat influences the room's pattern."};
      default -> new String[] {"Le jury va bientôt trancher.", "The jury will decide soon."};
    };
    block(c, vt(involvement[0], involvement[1]),
        200, 611, 312, 15, YELLOW, true);
    hiddenWaiting(c);
  }

  private void buzz(Canvas c, GameEngine.Player a) {
    text(c, vt("JAUGE DE GORGÉES", "SIP METER"), 22, H - 62, 12, MUTED, false);
    panel(c, 22, H - 50, 263, 21, BG, PINK);
    p.setColor(PINK);
    float fill = Math.min(1, a.sips / 12f);
    c.drawRoundRect(25, H - 47, 25 + 257 * fill, H - 32, 7, 7, p);
    text(c, a.sips + " ×", 332, H - 34, 20, YELLOW, true);
  }

  private void quiz(Canvas c) {
    String[] q = g.variant < 0 ? null
        : (g.english() ? GameEngine.QUIZ_EN : GameEngine.QUIZ_FR)[g.variant];
    String prompt = q == null ? (g.english() ? g.screenPromptEn : g.screenPromptFr) : q[0];
    String[] choices = g.english() ? g.screenChoicesEn : g.screenChoicesFr;
    block(c, prompt, 200, 257, 295, 21, WHITE, true);
    if (g.crewCount() > 0) {
      int lead = g.crewLead();
      text(c, g.t("LA SALLE : ", "THE ROOM: ") + g.crewCount()
          + g.t(" RÉPONSES", " ANSWERS"), 200, 331, 13, CYAN, true);
      if (lead >= 0)
        button(c, "trustCrew", g.t("SUIVRE LA SALLE • RÉPONSE ", "FOLLOW THE ROOM • ANSWER ")
            + (lead + 1), 45, 620, 310, 47, PINK);
    }
    for (int i = 0; i < 4; i++) {
      int opt = (i - g.target + 4) % 4;
      String label = choices.length == 4 ? choices[i] : q[opt + 1];
      button(c, "answer:" + i, label, 45, 354 + i * 66, 310, 55, i % 2 == 0 ? CYAN : YELLOW);
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
      block(c, judge.name + (g.game == 8
          ? vt(" : vrai ou inventé ?", ": true or made up?")
          : vt(" : défi validé ?", ": challenge complete?")),
          200, 509, 324, 18, YELLOW, true);
      button(c, "judgeYes", g.game == 8 ? vt("C'ÉTAIT VRAI", "IT WAS TRUE")
          : vt("OUI, ÇA PASSE", "YES, IT COUNTS"),
          39, 550, 322, 66, CYAN);
      button(c, "judgeNo", g.game == 8 ? vt("C'ÉTAIT INVENTÉ", "IT WAS MADE UP")
          : vt("NON, GORGÉE !", "NO, TAKE A SIP!"),
          39, 631, 322, 66, PINK);
    } else {
      block(c, g.t("Le jury décide…", "The jury is deciding…"),
          200, 555, 320, 21, WHITE, true);
      hiddenWaiting(c);
    }
  }

  private void blind(Canvas c) {
    block(
        c,
        g.t("Écoute : quel motif entends-tu ?", "Listen: which pattern do you hear?"),
        200,
        258,
        300,
        20,
        WHITE,
        true);
    button(c, "tune", g.t("▶ ÉCOUTER", "▶ PLAY TUNE"), 60, 309, 280, 55, PINK);
    if (g.crewCount() > 0 && g.crewLead() >= 0)
      button(c, "trustCrew", g.t("SUIVRE LA SALLE • ", "FOLLOW THE ROOM • ")
          + (g.crewLead() + 1), 63, 644, 274, 44, PINK);
    for (int i = 0; i < 4; i++) {
      int option = (i - g.target + 4) % 4;
      String name =
          (g.english() ? g.screenChoicesEn : g.screenChoicesFr).length == 4
              ? (g.english() ? g.screenChoicesEn : g.screenChoicesFr)[i]
              : GameEngine.TUNES[(g.variant + option) % 6][g.english() ? 1 : 0];
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
        g.t("Tape " + g.reflexGoal() + " cibles avant la fin !",
            "Hit " + g.reflexGoal() + " targets before time runs out!"),
        200,
        257,
        310,
        21,
        WHITE,
        true);
    text(c, g.t("CIBLES", "HITS") + " " + shown + " / " + g.reflexGoal(),
        200, 321, 23, YELLOW, true);
    float x = g.reflexX(shown), y = g.reflexY(shown);
    float radius = g.reflexHitRadius();
    p.setColor(PINK);
    c.drawCircle(x, y, radius, p);
    p.setColor(YELLOW);
    c.drawCircle(x, y, radius - 14, p);
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
    String[] layoutFr = {"LIGNE", "ALTERNE", "CROIX", "RETOUR", "SERPENT", "COINS"};
    String[] layoutEn = {"LINE", "ALTERNATE", "CROSS", "REVERSE", "SNAKE", "CORNERS"};
    text(c, g.t("MOTIF : ", "LAYOUT: ")
        + (g.english() ? layoutEn : layoutFr)[Math.floorMod(g.variant, 6)],
        200, 317, 15, CYAN, true);
    for (int i = 0; i < 6; i++) {
      float x = 42 + (i % 3) * 107, y = 348 + (i / 3) * 132;
      panel(c, x, y, 91, 108, g.chosenCup == i ? YELLOW : PANEL,
          g.chosenCup == i ? WHITE : GameSprites.accent(g.game));
      p.setColor(YELLOW);
      c.drawRoundRect(x + 18, y + 26, x + 73, y + 81, 9, 9, p);
      text(c, g.chosenCup == i ? "!" : "?", x + 45, y + 67, 35, BG, true);
      if (g.crewChoiceCount(i) > 0)
        text(c, "♥ " + g.crewChoiceCount(i), x + 45, y + 18, 15, PINK, true);
      if (g.chosenCup < 0) hits.add(new Hit("cup:" + i, new RectF(x, y, x + 91, y + 108),
          g.t("Gobelet ", "Cup ") + (i + 1)));
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
        GameSprites.icon(c, p, g.game, 200, 391, 5, g.turn, System.currentTimeMillis());
        block(c, vt("Le dessinateur prépare son chef-d'œuvre…",
            "The artist is preparing a masterpiece…"), 200, 548, 305, 19, YELLOW, true);
        hiddenWaiting(c);
        return;
      }
      String[] prompt = g.variant < 0
          ? new String[] {g.screenPromptFr, g.screenPromptEn} : GameEngine.DRAW[g.variant];
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
          c, "drawReady", g.t("FAIRE DEVINER AUX POTES", "LET FRIENDS GUESS"), 45, H - 156, 310, 59, PINK);
    } else {
      block(c, vt("Devine le dessin !", "Guess the drawing!"), 200, 255, 300, 22, YELLOW, true);
      panel(c, 70, 280, 260, 200, WHITE, CYAN);
      for (float[] s : g.strokes) {
        p.setColor(BG);
        p.setStrokeWidth(4);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(s[0], s[1], s[2], s[3], p);
      }
      if (((MainActivity) getContext()).localGuesser() == null) {
        block(c, vt("Les potes choisissent leur réponse…", "Friends are choosing their answers…"),
            200, 579, 300, 18, CYAN, true);
        text(c, g.drawAnsweredCount() + " / " + (g.players.size() - 1),
            200, 620, 22, YELLOW, true);
        hiddenWaiting(c);
        return;
      }
      for (int i = 0; i < 4; i++) {
        int opt = (i - g.target + 4) % 4;
        String[] q = g.variant < 0
            ? new String[] {g.screenChoicesFr.length == 4 ? g.screenChoicesFr[i] : "?",
                g.screenChoicesEn.length == 4 ? g.screenChoicesEn[i] : "?"}
            : GameEngine.DRAW[(g.variant + opt) % GameEngine.DRAW.length];
        button(
            c,
            "answer:" + i,
            q[viewerEnglish() ? 1 : 0],
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
    String[] patternFr = {"CHAOS", "MIROIR", "ALTERNE", "SANS DOUBLON", "ROUE", "PAIRES"};
    String[] patternEn = {"CHAOS", "MIRROR", "ALTERNATE", "NO REPEAT", "WHEEL", "PAIRS"};
    text(c, g.crewCount() > 0 ? g.t("LA CHAÎNE DES POTES", "FRIENDS' CHAIN")
        : (g.english() ? patternEn : patternFr)[Math.floorMod(g.variant, 6)],
        200, 272, 14, CYAN, true);
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
    if (!glow) hits.add(new Hit("tile:" + i, new RectF(x, y, x + 145, y + 110),
        g.t("Couleur ", "Color ") + (i + 1)));
  }

  private void rhythm(Canvas c) {
    block(
        c,
        g.t("Frappe seulement les temps dorés !", "Hit only the golden beats!"),
        200,
        247,
        315,
        21,
        WHITE,
        true);
    long now = ((MainActivity) getContext()).hostNow();
    int phase = RhythmClock.phase(g.started, now);
    int currentBeat = RhythmClock.beat(g.started, now);
    int nextBeat = g.rhythmPattern()[Math.min(3, g.rhythmHits)];
    for (int i = 0; i < 8; i++)
      text(c, String.valueOf(i + 1), 61 + i * 40, 317, 18,
          i == nextBeat ? YELLOW : i == Math.floorMod(currentBeat, 8) ? CYAN : MUTED, true);
    float pulse = phase < 0 ? 0 : phase < RhythmClock.TARGET_MS
        ? phase / (float) RhythmClock.TARGET_MS
        : (RhythmClock.PERIOD_MS - phase) / (float) RhythmClock.TARGET_MS;
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(5 + 10 * pulse);
    p.setColor(YELLOW);
    c.drawCircle(200, 440, 62 + 33 * pulse, p);
    p.setStyle(Paint.Style.FILL);
    p.setColor(PINK);
    c.drawCircle(200, 440, 68, p);
    text(c, "TAP", 200, 452, 30, WHITE, true);
    hits.add(new Hit("beat", new RectF(90, 330, 310, 550),
        g.t("Frapper sur le temps", "Tap on the beat")));
    text(c, g.t("PROCHAIN TEMPS : ", "NEXT BEAT: ") + (nextBeat + 1),
        200, 603, 17, YELLOW, true);
    text(c, g.rhythmHits + " / 4", 200, 639, 24, CYAN, true);
  }

  private void bluff(Canvas c) {
    String[] q = GameEngine.BLUFF[g.variant];
    block(c, q[g.english() ? 1 : 0], 200, 286, 315, 23, WHITE, true);
    GameSprites.icon(c, p, 8, 200, 500, 5f, g.variant, System.currentTimeMillis());
    block(
        c,
        g.t("Raconte, puis verrouille ta vérité en secret.",
            "Tell the story, then lock your truth in secret."),
        200,
        603,
        315,
        15,
        MUTED,
        true);
    button(c, "bluffTrue", g.t("VRAI", "TRUE"),
        38, H - 174, 152, 65, CYAN);
    button(c, "bluffFalse", g.t("INVENTÉ", "MADE UP"),
        210, H - 174, 152, 65, PINK);
  }

  private void bomb(Canvas c) {
    MainActivity app = (MainActivity) getContext();
    GameEngine.Player holder = app.localBomber();
    if (g.bombAwaitingPass) {
      block(c, vt("À qui passes-tu la bombe ?", "Who gets the bomb next?"),
          200, 260, 320, 22, YELLOW, true);
      if (holder == null) {
        block(c, vt("Le porteur choisit sa prochaine victime…",
            "The holder is choosing the next victim…"), 200, 464, 300, 20, WHITE, true);
      } else {
        int row = 0;
        for (int i = 0; i < g.players.size(); i++) {
          if (!g.canPassBombTo(i)) continue;
          button(c, "bombPass:" + i, g.players.get(i).name.toUpperCase(Locale.ROOT),
              27 + (row % 2) * 176, 306 + (row / 2) * 64, 166, 55,
              row % 2 == 0 ? CYAN : PINK);
          row++;
        }
        if (g.canDefuseBomb()) {
          text(c, vt("OU COUPE • +100 PTS POUR L'ACTEUR",
              "OR CUT • +100 PTS FOR THE ACTOR"), 200, 535, 14, YELLOW, true);
          button(c, "bombCutRed", vt("FIL ROUGE", "RED WIRE"),
              27, 552, 166, 59, PINK);
          button(c, "bombCutBlue", vt("FIL BLEU", "BLUE WIRE"),
              203, 552, 166, 59, CYAN);
        }
      }
      text(c, g.taps + " / " + g.bombGoal(), 200, 657, 24, CYAN, true);
      return;
    }
    block(c, vt(g.bombTapsPerHolder() + " touches, puis choisis à qui passer !",
        g.bombTapsPerHolder() + " taps, then choose who gets it!"),
        200, 261, 310, 20, WHITE, true);
    float duration = Math.max(1, g.deadline - g.started);
    float f = Math.max(.1f, Math.min(1f,
        (g.deadline - ((MainActivity) getContext()).hostNow()) / duration));
    p.setColor(PINK);
    c.drawCircle(200, 443, 71 + 20 * f, p);
    p.setColor(YELLOW);
    c.drawCircle(200, 443, 56, p);
    text(c, "TAP!", 200, 453, 26, BG, true);
    if (holder != null) hits.add(new Hit("bomb", new RectF(105, 350, 295, 540),
        vt("Toucher la bombe", "Tap the bomb")));
    text(c, g.taps + " / " + g.bombGoal(), 200, 603, 25, CYAN, true);
    if (g.bombNext < g.players.size())
      text(c, vt("À TOI : ", "YOUR TAP: ") + g.players.get(g.bombNext).name,
          200, 645, 18, YELLOW, true);
  }

  private void result(Canvas c) {
    header(c, vt("FIN DU TOUR", "ROUND OVER"));
    GameEngine.Player a = g.current();
    if (a == null) return;
    text(
        c,
        g.lastWon ? vt("VICTOIRE !", "VICTORY!") : vt("CUL SEC… VIRTUEL !", "VIRTUAL SIP!"),
        200,
        204,
        27,
        g.lastWon ? CYAN : PINK,
        true);
    avatar(c, a, 200, 295, 1.3f);
    GameSprites.icon(c, p, g.game, 315, 292, 2.8f, g.lastWon ? 9 : 0,
        System.currentTimeMillis());
    text(c, a.name, 200, 385, 25, WHITE, true);
    if (g.game == 4) rouletteReveal(c);
    else glass(
        c,
        200,
        478,
        g.lastWon ? 1 : Math.max(.05f, 1 - (System.currentTimeMillis() - g.started) / 1400f));
    text(c, g.lastWon ? "+" + g.roundPoints + " PTS"
        : (g.roundSips == 0 ? vt("AUCUNE GORGÉE", "NO SIP")
            : "+" + g.roundSips + " " + vt("GORGÉES", "SIPS")),
        200, 582, 24, YELLOW, true);
    int right = 0, wrong = 0;
    for (int i = 0; i < g.predictions.length; i++) if (i != g.active && g.predictions[i] >= 0) {
      if ((g.predictions[i] == 1) == g.lastWon) right++; else wrong++;
    }
    String reveal = switch (g.game) {
      case 1 -> vt("LE JURY A TRANCHÉ", "THE JURY HAS SPOKEN");
      case 8 -> g.bluffTruth == 1
          ? vt("L'HISTOIRE ÉTAIT VRAIE", "THE STORY WAS TRUE")
          : vt("L'HISTOIRE ÉTAIT INVENTÉE", "THE STORY WAS MADE UP");
      case 9 -> vt("RELAIS DE BOMBE TERMINÉ", "BOMB RELAY COMPLETE");
      default -> g.crewPoints.length > 0
          ? vt("LES POTES ONT JOUÉ • ", "FRIENDS PLAYED • ")
              + (g.game == 5 ? g.drawAnsweredCount() : g.crewCount())
              + " / " + (g.players.size() - 1)
          : vt("PRONOS : ", "PREDICTIONS: ") + right + vt(" JUSTES", " RIGHT")
              + "  •  " + wrong + vt(" RATÉS", " WRONG");
    };
    text(c, reveal,
        200, 632, 14, CYAN, true);
    if (g.secretId == g.game + 1 && g.ruleId < 0)
      text(c, vt("★ SECRET DÉBLOQUÉ ★", "★ SECRET UNLOCKED ★"),
          200, 660, 14, YELLOW, true);
    button(c, "board", vt("CLASSEMENT", "LEADERBOARD"), 35, H - 180, 330, 54, PANEL);
    GameEngine.Player nextPlayer = g.players.get(Math.floorMod(
        g.active + (g.ruleId == 2 ? -1 : 1), g.players.size()));
    button(
        c, "next", vt("AU TOUR DE ", "NEXT: ") + nextPlayer.name.toUpperCase(Locale.ROOT),
        35, H - 110, 330, 62, g.lastWon ? CYAN : PINK);
    hiddenWaiting(c);
  }

  private void rouletteReveal(Canvas c) {
    for (int cup = 0; cup < 6; cup++) {
      float x = 107 + (cup % 3) * 93, y = 445 + (cup / 3) * 62;
      p.setColor(g.cupIsCursed(cup) ? PINK : CYAN);
      c.drawCircle(x, y, 25, p);
      text(c, g.cupIsCursed(cup) ? "×" : "✓", x, y + 8, 25, BG, true);
      if (cup == g.chosenCup) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4);
        p.setColor(YELLOW);
        c.drawCircle(x, y, 31, p);
        p.setStyle(Paint.Style.FILL);
      }
    }
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
    float x = (e.getX() - offsetX) / scale, y = e.getY() / scale;
    if (x < 0 || x > 400) return true;
    if (kind == 0 && x >= 325 && y <= 89 && !"HOME".equals(g.screen)
        && !"SETTINGS".equals(g.screen) && !"GUIDE".equals(g.screen)
        && !"STATS".equals(g.screen)) {
      actions.showPartyMenu();
      return true;
    }
    if (kind == 0 && hiddenTouch(x, y)) return true;
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
    if ("GAME".equals(g.screen) && actions.client() && g.game == 5 && g.drawingReady) {
      if (kind == 0) handle(x, y, kind);
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
    if ("HANDOFF".equals(g.screen) && actions.client()) {
      if (kind == 0) handle(x, y, kind);
      return true;
    }
    if ("PREDICT".equals(g.screen) || "CREW".equals(g.screen)
        || "RULE_VOTE".equals(g.screen)
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
    if ("GAME".equals(g.screen) && actions.client() && (g.game == 8 || g.game == 9)) {
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
    if (step >= g.reflexGoal()
        || Math.hypot(x - g.reflexX(step), y - g.reflexY(step)) > g.reflexHitRadius()) return;
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
        if (d <= g.reflexHitRadius()) {
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
      case "mode" -> actions.setMode("VOTE".equals(g.mode) ? "FREE"
          : "FREE".equals(g.mode) ? "TURBO" : "VOTE");
      case "partyTab" -> { statsTab = 0; invalidate(); }
      case "historyTab" -> { statsTab = 1; invalidate(); }
      case "gamesTab" -> { statsTab = 2; invalidate(); }
      case "settings" -> {
        if ("HOME".equals(g.screen)) actions.showSettings();
        else actions.showPartyMenu();
      }
      case "guide" -> actions.guide();
      case "guidePrev" -> { guideIndex = Math.floorMod(guideIndex - 1, 10); invalidate(); }
      case "guideNext" -> { guideIndex = (guideIndex + 1) % 10; invalidate(); }
      case "musicShortcut" -> actions.showRadioDock();
      case "radioDock" -> actions.showRadioDock();
      case "openMusic" -> actions.openMusicProvider();
      case "musicProvider" -> { settingsTab = 2; invalidate(); }
      case "musicLink" -> actions.editMusicLink();
      case "provider:0", "provider:1", "provider:2", "provider:3", "provider:4",
          "provider:5" -> {
        actions.setMusicProvider(Integer.parseInt(id.substring(9)));
        settingsTab = 0;
        invalidate();
      }
      case "settingsMusic" -> { settingsTab = 0; invalidate(); }
      case "settingsParty" -> { settingsTab = 1; invalidate(); }
      case "add" -> actions.addPlayer();
      case "resetRoster" -> actions.clearRoster();
      case "host" -> actions.startHost();
      case "back" -> {
        if ("SETTINGS".equals(g.screen) && settingsTab == 2) {
          settingsTab = 0;
          invalidate();
        } else actions.home();
      }
      case "start" -> {
        if (g.players.size() >= 2) {
          g.begin();
          actions.save();
        }
      }
      case "enter" -> actions.enterGame();
      case "readyTurn" -> actions.readyTurn();
      case "predictYes" -> actions.predict(true);
      case "predictNo" -> actions.predict(false);
      case "trustCrew" -> { if (g.crewLead() >= 0) actions.finishGame(g.crewLead() == g.target); }
      case "ruleReport" -> actions.reportRule();
      case "ruleYes" -> actions.ruleVote(true);
      case "ruleNo" -> actions.ruleVote(false);
      case "juryStart" -> actions.beginJury();
      case "bluffTrue" -> actions.bluffTruth(true);
      case "bluffFalse" -> actions.bluffTruth(false);
      case "judgeYes" -> actions.judge(true);
      case "judgeNo" -> actions.judge(false);
      case "readyVote" -> actions.confirmPass();
      case "musicToggle" -> actions.toggleMusic();
      case "musicStyle" -> actions.changeMusicStyle();
      case "musicVolume" -> actions.changeMusicVolume();
      case "effectsToggle" -> actions.toggleEffects();
      case "hapticToggle" -> actions.toggleHaptics();
      case "win" -> actions.finishGame(true);
      case "lose" -> actions.finishGame(false);
      case "next" -> {
        g.advance();
        actions.save();
      }
      case "tune" -> actions.tune();
      case "drawReady" -> {
        actions.drawingReady();
      }
      case "beat" -> {
        long now = ((MainActivity) getContext()).hostNow();
        int beat = RhythmClock.beat(g.started, now);
        if (RhythmClock.accepts(g.started, now, beat, false) && beat > g.progress)
          actions.rhythmTap(beat);
      }
      case "bomb" -> actions.bombTap();
      case "bombCutRed" -> actions.bombCut(0);
      case "bombCutBlue" -> actions.bombCut(1);
      default -> {
        if (id.startsWith("hidden:")) {
          actions.hiddenTap(Integer.parseInt(id.substring(7)));
          return;
        }
        if (id.startsWith("bombPass:")) {
          actions.bombPass(Integer.parseInt(id.substring(9)));
          return;
        }
        if (id.startsWith("vote:")) { actions.vote(Integer.parseInt(id.substring(5))); return; }
        if (id.startsWith("crew:")) { actions.crewPick(Integer.parseInt(id.substring(5))); return; }
        if (id.startsWith("bet:")) { actions.placeBet(Integer.parseInt(id.substring(4))); return; }
        if (id.startsWith("rule:")) { actions.chooseRule(Integer.parseInt(id.substring(5))); return; }
        if (id.startsWith("pick:")) { actions.selectGame(Integer.parseInt(id.substring(5))); return; }
        if (id.startsWith("edit:")) { actions.editPlayer(Integer.parseInt(id.substring(5))); return; }
        if (id.startsWith("answer:")) {
          int selected = Integer.parseInt(id.substring(7));
          if (g.game == 5) actions.drawGuess(selected);
          else actions.finishGame(selected == g.target);
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
