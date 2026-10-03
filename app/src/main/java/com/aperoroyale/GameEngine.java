package com.aperoroyale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/** Single authority for turn order, game rotation and scoring. All state is serializable. */
public final class GameEngine {
  public static final String[] TYPES = {
    "QUIZ", "POSE", "BLIND", "REFLEX", "ROULETTE", "DRAW", "MEMORY", "RHYTHM", "BLUFF", "BOMB"
  };

  public static final class Player {
    public String name, language, photo = "";
    public int avatar, score, wins, games, drinks, sips;

    Player(String name, String language, int avatar) {
      this.name = name;
      this.language = language;
      this.avatar = avatar;
    }

    JSONObject json() {
      JSONObject j = new JSONObject();
      try {
        j.put("name", name);
        j.put("language", language);
        j.put("avatar", avatar);
        j.put("photo", photo);
        j.put("score", score);
        j.put("wins", wins);
        j.put("games", games);
        j.put("drinks", drinks);
        j.put("sips", sips);
      } catch (Exception ignored) {
      }
      return j;
    }

    static Player from(JSONObject j) {
      Player p =
          new Player(
              j.optString("name", "Player"), j.optString("language", "FR"), j.optInt("avatar"));
      p.score = j.optInt("score");
      p.wins = j.optInt("wins");
      p.games = j.optInt("games");
      p.drinks = j.optInt("drinks");
      p.sips = j.optInt("sips", p.drinks);
      p.photo = j.optString("photo", "");
      return p;
    }
  }

  public final ArrayList<Player> players = new ArrayList<>();
  private final Random random = new Random();
  private final ArrayList<Integer> deck = new ArrayList<>();
  public String screen = "HOME";
  public int active = 0,
      turn = 0,
      game = 0,
      variant = 0,
      progress = 0,
      taps = 0,
      selected = -1,
      target = 0,
      rhythmHits = 0;
  public int wager = 1;
  public int bombNext = 0;
  public long deadline = 0, started = 0;
  public boolean lastWon = false, drawingReady = false;
  public String note = "";
  public int[] sequence = {};
  public float targetX = 200, targetY = 390;
  public final ArrayList<float[]> strokes = new ArrayList<>();
  public int loserCup = 0;
  public String mode = "VOTE";
  public int[] offers = {}, votes = {}, catTaps = {};
  public String ruleOwner = "";
  public int ruleId = -1, voteWinner = -1, freePick = 0;
  public int bonusId = 0;

  public Player current() {
    return players.isEmpty() ? null : players.get(Math.min(active, players.size() - 1));
  }

  public boolean english() {
    Player p = current();
    return p == null ? "en".equals(Locale.getDefault().getLanguage()) : "EN".equals(p.language);
  }

  public String t(String fr, String en) {
    return english() ? en : fr;
  }

  public boolean addPlayer(String name, String language, int avatar) {
    name = name.trim();
    if (name.isEmpty() || players.size() >= 6 || name.length() > 16) return false;
    for (Player p : players) if (p.name.equalsIgnoreCase(name)) return false;
    players.add(new Player(name, language, avatar % 6));
    screen = "LOBBY";
    return true;
  }

  public void newParty() {
    players.clear();
    active = turn = 0;
    screen = "LOBBY";
    deck.clear();
    mode = "VOTE";
    offers = votes = catTaps = new int[0];
    ruleOwner = "";
    ruleId = voteWinner = -1;
  }

  public void begin() {
    if (players.size() < 2) return;
    active = 0;
    turn = 0;
    startSelection();
  }

  public void startSelection() {
    deadline = 0;
    started = System.currentTimeMillis();
    voteWinner = -1;
    bonusId = random.nextInt(4);
    if ("FREE".equals(mode)) {
      screen = "LIBRARY";
      return;
    }
    if (deck.isEmpty()) {
      for (int i = 0; i < TYPES.length; i++) deck.add(i);
      Collections.shuffle(deck, random);
    }
    ArrayList<Integer> pool = new ArrayList<>(deck);
    if (turn > 0) pool.remove(Integer.valueOf(game));
    if (pool.size() < 3)
      for (int i = 0; i < TYPES.length; i++)
        if (!pool.contains(i) && (turn == 0 || i != game)) pool.add(i);
    Collections.shuffle(pool, random);
    offers = new int[] {pool.get(0), pool.get(1), pool.get(2)};
    votes = new int[players.size()];
    java.util.Arrays.fill(votes, -1);
    if (catTaps.length != players.size()) catTaps = new int[players.size()];
    screen = "VOTE";
  }

  public int indexOf(String name) {
    for (int i = 0; i < players.size(); i++)
      if (players.get(i).name.equals(name)) return i;
    return -1;
  }

  public boolean castVote(String name, int choice) {
    int i = indexOf(name);
    if (!"VOTE".equals(screen) || i < 0 || i >= votes.length || votes[i] >= 0 || choice < 0 || choice >= 3) return false;
    votes[i] = choice;
    completeVoteIfReady();
    return true;
  }

  public int voteCount() {
    int n = 0;
    for (int v : votes) if (v >= 0) n++;
    return n;
  }

  private void completeVoteIfReady() {
    if (voteCount() != players.size() || (!ruleOwner.isEmpty() && ruleId < 0)) return;
    int[] counts = new int[3];
    for (int v : votes) counts[v]++;
    int max = Math.max(counts[0], Math.max(counts[1], counts[2]));
    ArrayList<Integer> tied = new ArrayList<>();
    for (int i = 0; i < 3; i++) if (counts[i] == max) tied.add(i);
    voteWinner = tied.get(random.nextInt(tied.size()));
    startNext(offers[voteWinner]);
  }

  public boolean catTap(String name) {
    int i = indexOf(name);
    if (!("VOTE".equals(screen) || "LIBRARY".equals(screen)) || i < 0 || ruleId >= 0 || !ruleOwner.isEmpty()) return false;
    if (catTaps.length != players.size()) catTaps = new int[players.size()];
    catTaps[i]++;
    if (catTaps[i] >= 20) ruleOwner = name;
    return true;
  }

  public boolean chooseRule(String name, int id) {
    if (!name.equals(ruleOwner) || ruleId >= 0 || id < 0 || id > 2) return false;
    ruleId = id;
    if ("VOTE".equals(screen)) completeVoteIfReady();
    return true;
  }

  public void startNext(int chosen) {
    if (deck.isEmpty()) for (int i = 0; i < TYPES.length; i++) deck.add(i);
    game = chosen;
    deck.remove(Integer.valueOf(chosen));
    variant = random.nextInt(6);
    target = random.nextInt(4);
    progress = taps = 0;
    selected = -1;
    rhythmHits = 0;
    wager = 1;
    bombNext = active;
    drawingReady = false;
    note = "";
    strokes.clear();
    targetX = 80 + random.nextInt(240);
    targetY = 380 + random.nextInt(200);
    loserCup = random.nextInt(6);
    int count = 3 + Math.min(2, turn / 10);
    sequence = new int[count];
    for (int i = 0; i < count; i++) sequence[i] = random.nextInt(4);
    screen = "TRANSITION";
    started = System.currentTimeMillis();
    deadline = 0;
  }

  public void enterGame() {
    if (!"TRANSITION".equals(screen)) return;
    screen = "BET";
    started = System.currentTimeMillis();
  }

  public boolean placeBet(int sips) {
    if (!"BET".equals(screen) || sips < 1 || sips > 3) return false;
    wager = sips;
    startGame();
    return true;
  }

  private void startGame() {
    screen = "GAME";
    resumeGame();
  }

  public void resumeGame() {
    if (!"GAME".equals(screen)) return;
    started = System.currentTimeMillis();
    int seconds =
        switch (game) {
          case 0 -> 16;
          case 1 -> 30;
          case 3 -> 12;
          case 6 -> 22;
          case 7 -> 16;
          case 9 -> 20;
          default -> 0;
        };
    if (bonusId == 3 && seconds > 0) seconds += 5;
    deadline = seconds == 0 ? 0 : started + seconds * 1000L;
  }

  public int winPoints() {
    return (ruleId == 0 ? 2 : 1) * (100 + 50 * (wager - 1) + (bonusId == 1 ? 50 : 0));
  }

  public int lossSips() {
    return ruleId == 1 ? 0 : Math.max(0, wager - (bonusId == 2 ? 1 : 0));
  }

  public void finish(boolean won) {
    if (!"GAME".equals(screen)) return;
    lastWon = won;
    Player p = current();
    if (p == null) return;
    p.games++;
    if (won) {
      p.wins++;
      p.score += winPoints();
    } else {
      if (lossSips() > 0) {
        p.drinks++;
        p.sips += lossSips();
      }
      p.score = Math.max(0, p.score - 25 * wager);
    }
    screen = "RESULT";
    deadline = 0;
    started = System.currentTimeMillis();
  }

  public boolean bombTap(String name) {
    if (!"GAME".equals(screen) || game != 9 || players.isEmpty()
        || !players.get(bombNext).name.equals(name)) return false;
    taps++;
    bombNext = (bombNext + 1) % players.size();
    return true;
  }

  public void advance() {
    if (!"RESULT".equals(screen)) return;
    active = Math.floorMod(active + (ruleId == 2 ? -1 : 1), players.size());
    turn++;
    startSelection();
  }

  public void checkTimeout() {
    if ("GAME".equals(screen) && deadline > 0 && System.currentTimeMillis() > deadline) {
      if (game == 3) finish(taps >= 10);
      else if (game == 7) finish(rhythmHits >= 4);
      else finish(false);
    }
  }

  public JSONObject json() {
    JSONObject j = new JSONObject();
    try {
      j.put("screen", screen);
      j.put("active", active);
      j.put("turn", turn);
      j.put("game", game);
      j.put("variant", variant);
      j.put("progress", progress);
      j.put("taps", taps);
      j.put("selected", selected);
      j.put("target", target);
      j.put("rhythmHits", rhythmHits);
      j.put("wager", wager);
      j.put("bombNext", bombNext);
      j.put("deadline", deadline);
      j.put("started", started);
      j.put("lastWon", lastWon);
      j.put("drawingReady", drawingReady);
      j.put("note", note);
      j.put("targetX", targetX);
      j.put("targetY", targetY);
      j.put("loserCup", loserCup);
      j.put("mode", mode);
      j.put("offers", new JSONArray(offers));
      j.put("votes", new JSONArray(votes));
      j.put("catTaps", new JSONArray(catTaps));
      j.put("ruleOwner", ruleOwner);
      j.put("ruleId", ruleId);
      j.put("voteWinner", voteWinner);
      j.put("freePick", freePick);
      j.put("bonusId", bonusId);
      JSONArray ps = new JSONArray();
      for (Player p : players) ps.put(p.json());
      j.put("players", ps);
      JSONArray d = new JSONArray();
      for (int x : deck) d.put(x);
      j.put("deck", d);
      JSONArray s = new JSONArray();
      for (int x : sequence) s.put(x);
      j.put("sequence", s);
      JSONArray lines = new JSONArray();
      for (float[] v : strokes) {
        JSONArray a = new JSONArray();
        for (float f : v) a.put(f);
        lines.put(a);
      }
      j.put("strokes", lines);
    } catch (Exception ignored) {
    }
    return j;
  }

  public void restore(JSONObject j) {
    players.clear();
    JSONArray ps = j.optJSONArray("players");
    if (ps != null)
      for (int i = 0; i < ps.length(); i++) players.add(Player.from(ps.optJSONObject(i)));
    screen = j.optString("screen", "HOME");
    active = j.optInt("active");
    turn = j.optInt("turn");
    game = j.optInt("game");
    variant = j.optInt("variant");
    progress = j.optInt("progress");
    taps = j.optInt("taps");
    selected = j.optInt("selected", -1);
    target = j.optInt("target");
    rhythmHits = j.optInt("rhythmHits");
    wager = j.optInt("wager", 1);
    bombNext = j.optInt("bombNext", active);
    deadline = j.optLong("deadline");
    started = j.optLong("started");
    lastWon = j.optBoolean("lastWon");
    drawingReady = j.optBoolean("drawingReady");
    note = j.optString("note");
    targetX = (float) j.optDouble("targetX", 200);
    targetY = (float) j.optDouble("targetY", 390);
    loserCup = j.optInt("loserCup");
    mode = j.optString("mode", "VOTE");
    offers = readInts(j.optJSONArray("offers"));
    votes = readInts(j.optJSONArray("votes"));
    catTaps = readInts(j.optJSONArray("catTaps"));
    ruleOwner = j.optString("ruleOwner", "");
    ruleId = j.optInt("ruleId", -1);
    voteWinner = j.optInt("voteWinner", -1);
    freePick = j.optInt("freePick", 0);
    bonusId = j.optInt("bonusId", 0);
    deck.clear();
    JSONArray d = j.optJSONArray("deck");
    if (d != null) for (int i = 0; i < d.length(); i++) deck.add(d.optInt(i));
    JSONArray s = j.optJSONArray("sequence");
    sequence = new int[s == null ? 0 : s.length()];
    for (int i = 0; i < sequence.length; i++) sequence[i] = s.optInt(i);
    strokes.clear();
    JSONArray ls = j.optJSONArray("strokes");
    if (ls != null)
      for (int i = 0; i < ls.length(); i++) {
        JSONArray a = ls.optJSONArray(i);
        if (a != null) {
          float[] f = new float[a.length()];
          for (int k = 0; k < f.length; k++) f[k] = (float) a.optDouble(k);
          strokes.add(f);
        }
      }
    if (active >= players.size()) active = 0;
  }

  private static int[] readInts(JSONArray a) {
    if (a == null) return new int[0];
    int[] result = new int[a.length()];
    for (int i = 0; i < result.length; i++) result[i] = a.optInt(i);
    return result;
  }

  public static final String[][] QUIZ_FR = {
    {"Quel animal a trois cœurs ?", "Pieuvre", "Panda", "Canard", "Moustique"},
    {"Combien de côtés a un dé classique ?", "6", "8", "12", "4"},
    {"Quel fruit est une baie botanique ?", "Banane", "Fraise", "Framboise", "Cerise"},
    {
      "Quelle planète pleut des diamants selon les modèles ?", "Neptune", "Mars", "Mercure", "Vénus"
    },
    {"Quel animal dort debout ?", "Cheval", "Poulpe", "Pingouin", "Taupe"},
    {"Combien de cerveaux a une pieuvre ?", "9", "1", "2", "6"}
  };
  public static final String[][] QUIZ_EN = {
    {"Which animal has three hearts?", "Octopus", "Panda", "Duck", "Mosquito"},
    {"How many sides on a standard die?", "6", "8", "12", "4"},
    {"Which fruit is botanically a berry?", "Banana", "Strawberry", "Raspberry", "Cherry"},
    {"Which planet may rain diamonds?", "Neptune", "Mars", "Mercury", "Venus"},
    {"Which animal can sleep standing?", "Horse", "Octopus", "Penguin", "Mole"},
    {"How many brains does an octopus have?", "9", "1", "2", "6"}
  };
  public static final String[][] POSES = {
    {
      "Une main sur la tête, l'autre sur le genou d'un voisin. Tiens 15 secondes !",
      "One hand on your head, the other on a friend's knee. Hold 15 seconds!"
    },
    {
      "Imite un flamant rose en chantant ton prénom. 15 secondes !",
      "Be a flamingo while singing your name. 15 seconds!"
    },
    {
      "Fais la statue disco sur une jambe. 15 secondes !",
      "Strike a disco pose on one leg. 15 seconds!"
    },
    {
      "Danse au ralenti sans bouger les pieds. 15 secondes !",
      "Dance in slow motion without moving your feet. 15 seconds!"
    },
    {
      "Fais un selfie invisible avec tout le monde. Pose dramatique !",
      "Take an invisible selfie with everyone. Dramatic pose!"
    },
    {
      "Marche comme un crabe royal jusqu'à la porte et reviens.",
      "Crab walk to the door and back like royalty."
    }
  };
  public static final String[][] DRAW = {
    {"Une licorne en boîte de nuit", "A unicorn at a nightclub"},
    {"Un canard DJ", "A DJ duck"},
    {"Un astronaute en slip", "An astronaut in underwear"},
    {"Un avocat qui danse", "A dancing avocado"},
    {"Une pizza qui pleure", "A crying pizza"},
    {"Un robot amoureux", "A robot in love"}
  };
  public static final String[][] BLUFF = {
    {
      "Invente une loi absurde du royaume. Les autres doivent y croire.",
      "Invent a ridiculous kingdom law. Make everyone believe it."
    },
    {
      "Raconte un exploit inventé avec un visage sérieux.",
      "Tell a made-up achievement with a straight face."
    },
    {
      "Vends un objet banal comme une invention géniale.",
      "Pitch an ordinary object as a genius invention."
    },
    {
      "Imite une célébrité imaginaire et donne une interview.",
      "Impersonate an imaginary celebrity and give an interview."
    },
    {
      "Explique pourquoi tu es le champion du monde de rien.",
      "Explain why you're world champion of nothing."
    },
    {"Convaincs le groupe que tu viens du futur.", "Convince the group you're from the future."}
  };
  public static final String[][] TUNES = {
    {"Neon Canard", "Neon Duck"},
    {"Cosmic Banana", "Cosmic Banana"},
    {"Pixel Sunset", "Pixel Sunset"},
    {"Disco Grenouille", "Disco Frog"},
    {"Moon Arcade", "Moon Arcade"},
    {"Royal Bubble", "Royal Bubble"}
  };
}
