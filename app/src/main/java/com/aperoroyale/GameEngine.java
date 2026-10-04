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

    JSONObject json(boolean includePhoto) {
      JSONObject j = new JSONObject();
      try {
        j.put("name", name);
        j.put("language", language);
        j.put("avatar", avatar);
        if (includePhoto) j.put("photo", photo);
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
  private final Random random;

  public GameEngine() {
    this(new Random().nextLong());
  }

  GameEngine(long seed) {
    random = new Random(seed);
  }
  private final ArrayList<Integer> deck = new ArrayList<>();
  private final int[] lastVariant = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
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
  public int roundPoints = 0, roundSips = 0, chosenCup = -1, reflexSeed = 0;
  public long revealUntil = 0, revision = 0;
  public boolean juryPhase = false;
  public int bluffTruth = -1;
  public int[] juryVotes = {};
  public int[] predictions = {};
  public boolean betPlaced = false;
  public long deadline = 0, started = 0;
  public boolean lastWon = false, drawingReady = false;
  public String note = "";
  public int[] sequence = {};
  public String screenPromptFr = "", screenPromptEn = "";
  public String[] screenChoicesFr = {}, screenChoicesEn = {};
  public float targetX = 200, targetY = 390;
  public final ArrayList<float[]> strokes = new ArrayList<>();
  public int loserCup = 0;
  public String mode = "VOTE";
  public int[] offers = {}, votes = {}, catTaps = {};
  public String ruleOwner = "";
  public int ruleId = -1, voteWinner = -1, freePick = 0;
  public int bonusId = 0;
  public int secretMask = 0, secretId = -1;
  public int[] ruleOffers = {};
  public String reportTarget = "", reportBy = "", reportReturn = "VOTE";
  public int[] reportVotes = {};
  public long reportDeadline = 0;
  public boolean rulePenaltyApplied = false;

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
    int chosen = Math.floorMod(avatar, 6);
    for (int tries = 0; tries < 6; tries++) {
      boolean used = false;
      for (Player player : players) if (player.avatar == chosen) { used = true; break; }
      if (!used) break;
      chosen = (chosen + 1) % 6;
    }
    players.add(new Player(name, language, chosen));
    screen = "LOBBY";
    return true;
  }

  public void newParty() {
    players.clear();
    active = turn = 0;
    screen = "LOBBY";
    deck.clear();
    java.util.Arrays.fill(lastVariant, -1);
    mode = "VOTE";
    offers = votes = catTaps = new int[0];
    ruleOwner = "";
    ruleId = voteWinner = -1;
    secretMask = 0;
    secretId = -1;
    ruleOffers = new int[0];
    reportTarget = reportBy = "";
    reportReturn = "VOTE";
    reportVotes = new int[0];
    reportDeadline = 0;
    rulePenaltyApplied = false;
    deadline = 0;
    juryPhase = false;
    bluffTruth = -1;
    juryVotes = predictions = new int[0];
    revision = 0;
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
    if ("TURBO".equals(mode) && !ruleOwner.isEmpty() && ruleId < 0) {
      screen = "RULE_PICK";
      return;
    }
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
    if ("TURBO".equals(mode)) {
      offers = new int[0];
      votes = new int[0];
      startNext(pool.get(0));
      return;
    }
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
    if (catTaps[i] >= 20) unlockSecret(0, name);
    return true;
  }

  private void unlockSecret(int id, String owner) {
    int bit = 1 << id;
    if ((secretMask & bit) != 0) return;
    secretMask |= bit;
    if (ruleId >= 0 || !ruleOwner.isEmpty()) return;
    ruleOwner = owner;
    secretId = id;
    int base = (id * 3) % 10;
    ruleOffers = new int[] {base, (base + 3) % 10, (base + 7) % 10};
  }

  public boolean chooseRule(String name, int id) {
    if (!name.equals(ruleOwner) || ruleId >= 0 || id < 0 || id >= 10) return false;
    boolean offered = false;
    for (int value : ruleOffers) if (value == id) offered = true;
    if (!offered) return false;
    ruleId = id;
    if ("VOTE".equals(screen)) completeVoteIfReady();
    else if ("RULE_PICK".equals(screen)) startSelection();
    return true;
  }

  public void startNext(int chosen) {
    if (deck.isEmpty()) for (int i = 0; i < TYPES.length; i++) deck.add(i);
    game = chosen;
    deck.remove(Integer.valueOf(chosen));
    int variants = switch (chosen) {
      case 0 -> QUIZ_FR.length;
      case 1 -> POSES.length;
      case 2 -> TUNES.length;
      case 5 -> DRAW.length;
      case 8 -> BLUFF.length;
      default -> 6;
    };
    variant = random.nextInt(variants);
    if (variants > 1 && variant == lastVariant[chosen])
      variant = (variant + 1 + random.nextInt(variants - 1)) % variants;
    lastVariant[chosen] = variant;
    target = random.nextInt(4);
    progress = chosen == 7 ? -1 : 0;
    taps = 0;
    selected = -1;
    rhythmHits = 0;
    wager = 1;
    roundPoints = roundSips = 0;
    chosenCup = -1;
    revealUntil = 0;
    juryPhase = false;
    bluffTruth = -1;
    juryVotes = new int[0];
    predictions = new int[0];
    betPlaced = false;
    bombNext = active;
    drawingReady = false;
    note = "";
    strokes.clear();
    reflexSeed = random.nextInt();
    targetX = reflexX(0);
    targetY = reflexY(0);
    loserCup = random.nextInt(6);
    int count = 4 + Math.min(3, turn / 5);
    sequence = new int[count];
    for (int i = 0; i < count; i++) sequence[i] = random.nextInt(4);
    screen = "TRANSITION";
    started = System.currentTimeMillis();
    deadline = 0;
  }

  public void enterGame() {
    if (!"TRANSITION".equals(screen)) return;
    screen = "HANDOFF";
    started = System.currentTimeMillis();
  }

  public void readyTurn() {
    if (!"HANDOFF".equals(screen)) return;
    screen = "BET";
    started = System.currentTimeMillis();
  }

  public boolean placeBet(int sips) {
    if (!"BET".equals(screen) || sips < 1 || sips > 3) return false;
    wager = sips;
    betPlaced = true;
    predictions = new int[players.size()];
    java.util.Arrays.fill(predictions, -1);
    predictions[active] = 2;
    screen = "PREDICT";
    deadline = System.currentTimeMillis() + ("TURBO".equals(mode) ? 5000 : 12000);
    return true;
  }

  public boolean predict(String name, boolean win) {
    int i = indexOf(name);
    if (!"PREDICT".equals(screen) || i < 0 || i == active
        || i >= predictions.length || predictions[i] >= 0) return false;
    predictions[i] = win ? 1 : 0;
    if (predictionsReady()) startGame();
    return true;
  }

  public boolean predictionsReady() {
    if (!"PREDICT".equals(screen)) return false;
    for (int i = 0; i < predictions.length; i++)
      if (i != active && predictions[i] < 0) return false;
    return true;
  }

  public boolean predictionTimedOut() {
    if (!"PREDICT".equals(screen) || System.currentTimeMillis() < deadline) return false;
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
    if (juryPhase) {
      deadline = started + 20000;
      return;
    }
    int seconds =
        switch (game) {
          case 0 -> 16;
          case 1 -> 24;
          case 2 -> 18;
          case 3 -> 15;
          case 6 -> 22;
          case 7 -> 16;
          case 8 -> 24;
          case 9 -> 30;
          default -> 0;
        };
    if ("TURBO".equals(mode) && seconds > 0) seconds = Math.max(10, seconds - 4);
    if (bonusId == 3 && seconds > 0) seconds += 5;
    if (seconds > 0 && game != 7) seconds += Math.min(6, supportCount() * 2);
    deadline = seconds == 0 ? 0 : started + seconds * 1000L;
  }

  public int supportCount() {
    int count = 0;
    for (int i = 0; i < predictions.length; i++)
      if (i != active && predictions[i] == 1) count++;
    return count;
  }

  public int challengeCount() {
    int count = 0;
    for (int i = 0; i < predictions.length; i++)
      if (i != active && predictions[i] == 0) count++;
    return count;
  }

  public int winPoints() {
    return (ruleId == 0 ? 2 : 1) * (100 + 50 * (wager - 1)
        + (bonusId == 1 ? 50 : 0)) + challengeCount() * 25;
  }

  public int lossSips() {
    return ruleId == 1 ? 0 : Math.max(0, wager - (bonusId == 2 ? 1 : 0)
        - (supportCount() > 0 ? 1 : 0));
  }

  public float reflexX(int step) {
    Random pattern = new Random(reflexSeed ^ (0x9e3779b9L * (step + 1)));
    return 77 + pattern.nextInt(246);
  }

  public float reflexY(int step) {
    Random pattern = new Random((reflexSeed * 31L) ^ (0x6a09e667L * (step + 1)));
    return 380 + pattern.nextInt(198);
  }

  public boolean reflexTap(String name, int expectedStep) {
    if (!"GAME".equals(screen) || game != 3 || current() == null
        || !current().name.equals(name) || expectedStep != taps || taps >= 10
        || (deadline > 0 && System.currentTimeMillis() > deadline)) return false;
    taps++;
    targetX = reflexX(taps);
    targetY = reflexY(taps);
    return true;
  }

  public boolean rhythmTap(String name, int beat) {
    return rhythmTap(name, beat, false);
  }

  public boolean rhythmTap(String name, int beat, boolean remote) {
    if (!"GAME".equals(screen) || game != 7 || current() == null
        || !current().name.equals(name) || rhythmHits >= 4 || beat <= progress
        || !RhythmClock.accepts(started, System.currentTimeMillis(), beat, remote)
        || (deadline > 0 && System.currentTimeMillis() > deadline)) return false;
    progress = beat;
    rhythmHits++;
    return true;
  }

  public boolean selectCup(int cup) {
    if (!"GAME".equals(screen) || game != 4 || cup < 0 || cup >= 6 || chosenCup >= 0) return false;
    chosenCup = cup;
    revealUntil = System.currentTimeMillis() + 1300;
    return true;
  }

  public boolean cupIsSafe() { return Math.floorMod(chosenCup - loserCup, 6) >= wager; }

  public boolean beginJury() {
    if (!"GAME".equals(screen) || (game != 1 && game != 8) || juryPhase
        || (game == 8 && bluffTruth < 0)) return false;
    juryPhase = true;
    juryVotes = new int[players.size()];
    java.util.Arrays.fill(juryVotes, -1);
    if (active < juryVotes.length) juryVotes[active] = 2;
    deadline = System.currentTimeMillis() + 20000;
    return true;
  }

  public boolean chooseBluffTruth(String name, boolean trueStory) {
    if (!"GAME".equals(screen) || game != 8 || juryPhase || bluffTruth >= 0
        || current() == null || !current().name.equals(name)) return false;
    bluffTruth = trueStory ? 1 : 0;
    return beginJury();
  }

  public boolean castJury(String name, boolean yes) {
    int i = indexOf(name);
    if (!"GAME".equals(screen) || !juryPhase || i < 0 || i == active
        || i >= juryVotes.length || juryVotes[i] >= 0) return false;
    juryVotes[i] = yes ? 1 : 0;
    return true;
  }

  public boolean juryComplete() {
    if (!juryPhase) return false;
    for (int i = 0; i < juryVotes.length; i++) if (i != active && juryVotes[i] < 0) return false;
    return true;
  }

  public boolean juryVerdict() {
    int yes = 0;
    int no = 0;
    for (int i = 0; i < juryVotes.length; i++) if (i != active && juryVotes[i] == 1) yes++;
    for (int i = 0; i < juryVotes.length; i++) if (i != active && juryVotes[i] == 0) no++;
    if (game == 8) return yes + no > 0 && (yes > no ? 1 : 0) != bluffTruth;
    return yes > (players.size() - 1) / 2;
  }

  public void finish(boolean won) {
    if (!"GAME".equals(screen)) return;
    lastWon = won;
    Player p = current();
    if (p == null) return;
    p.games++;
    roundPoints = 0;
    roundSips = 0;
    if (won) {
      p.wins++;
      int speed = (game == 0 || game == 2) && deadline > 0
          ? Math.min(80, (int) Math.max(0, (deadline - System.currentTimeMillis()) / 1000L) * 5) : 0;
      roundPoints = winPoints() + speed;
      p.score += roundPoints;
    } else {
      if (lossSips() > 0) {
        p.drinks++;
        roundSips = lossSips();
        p.sips += roundSips;
      }
      roundPoints = -Math.min(p.score, 25 * wager);
      p.score += roundPoints;
    }
    for (int i = 0; i < players.size() && i < predictions.length; i++) {
      if (i == active || predictions[i] < 0) continue;
      Player spectator = players.get(i);
      if ((predictions[i] == 1) == won) spectator.score += predictions[i] == 0 ? 50 : 35;
      else if (ruleId != 1) { spectator.drinks++; spectator.sips++; }
    }
    if (won) {
      boolean secret = switch (game) {
        case 0 -> deadline - System.currentTimeMillis() >= 8000;
        case 1, 8 -> juryPhase && juryComplete() && juryVerdict();
        case 2 -> deadline - System.currentTimeMillis() >= 4000;
        case 3 -> taps >= 10 && deadline - System.currentTimeMillis() >= 4000;
        case 4 -> wager == 3;
        case 5 -> strokes.size() >= 12;
        case 6 -> sequence.length >= 5;
        case 7 -> rhythmHits >= 4;
        case 9 -> taps >= 8;
        default -> false;
      };
      if (secret) unlockSecret(game + 1, p.name);
    }
    screen = "RESULT";
    deadline = 0;
    started = System.currentTimeMillis();
  }

  public boolean bombTap(String name) {
    if (!"GAME".equals(screen) || game != 9 || players.isEmpty()
        || !players.get(bombNext).name.equals(name)) return false;
    taps++;
    if (taps % 2 == 0) bombNext = (bombNext + 1) % players.size();
    return true;
  }

  public int bombGoal() {
    return Math.max(8, 2 * players.size());
  }

  public void advance() {
    if (!"RESULT".equals(screen)) return;
    active = Math.floorMod(active + (ruleId == 2 ? -1 : 1), players.size());
    turn++;
    startSelection();
  }

  public boolean reportRule(String reporter, String targetName) {
    int reporterIndex = indexOf(reporter), targetIndex = indexOf(targetName);
    if (!("VOTE".equals(screen) || "LIBRARY".equals(screen)) || ruleId < 3
        || reporterIndex < 0 || targetIndex < 0 || reporterIndex == targetIndex) return false;
    reportReturn = screen;
    reportBy = reporter;
    reportTarget = targetName;
    reportVotes = new int[players.size()];
    java.util.Arrays.fill(reportVotes, -1);
    if (players.size() > 2) reportVotes[targetIndex] = 2;
    reportVotes[reporterIndex] = 1;
    reportDeadline = System.currentTimeMillis() + 10000;
    rulePenaltyApplied = false;
    screen = "RULE_VOTE";
    return true;
  }

  public boolean castRuleVote(String name, boolean yes) {
    int i = indexOf(name);
    if (!"RULE_VOTE".equals(screen) || i < 0 || i >= reportVotes.length
        || reportVotes[i] >= 0) return false;
    reportVotes[i] = yes ? 1 : 0;
    if (ruleVoteReady()) resolveRuleVote();
    return true;
  }

  public boolean ruleVoteReady() {
    if (!"RULE_VOTE".equals(screen)) return false;
    for (int value : reportVotes) if (value < 0) return false;
    return true;
  }

  public boolean ruleVoteTimedOut() {
    if (!"RULE_VOTE".equals(screen) || System.currentTimeMillis() < reportDeadline) return false;
    resolveRuleVote();
    return true;
  }

  private void resolveRuleVote() {
    int yes = 0, no = 0;
    for (int value : reportVotes) { if (value == 1) yes++; else if (value == 0) no++; }
    if (yes > no && ruleId != 1) {
      int i = indexOf(reportTarget);
      if (i >= 0) { players.get(i).sips++; players.get(i).drinks++; rulePenaltyApplied = true; }
    }
    screen = reportReturn;
  }

  public void checkTimeout() {
    if ("GAME".equals(screen) && deadline > 0 && System.currentTimeMillis() > deadline) {
      if (juryPhase) finish(juryVerdict());
      else if (game == 3) finish(taps >= 10);
      else if (game == 7) finish(rhythmHits >= 4);
      else finish(false);
    }
  }

  public JSONObject json() {
    return json(true, true);
  }

  public JSONObject networkJson() {
    return json("LOBBY".equals(screen), game != 5 || drawingReady);
  }

  public JSONObject networkJsonFor(String recipient) {
    JSONObject j = networkJson();
    try {
      j.remove("lastVariant");
      boolean actor = current() != null && current().name.equals(recipient);
      int guesser = players.isEmpty() ? -1 : (active + 1) % players.size();
      boolean drawGuesser = game == 5 && drawingReady && guesser >= 0
          && players.get(guesser).name.equals(recipient);
      if (!"RESULT".equals(screen)) {
        j.put("loserCup", -1);
        j.put("target", -1);
        if ("VOTE".equals(screen)) j.put("votes", masked(votes));
        if ("PREDICT".equals(screen) || "GAME".equals(screen))
          j.put("predictions", masked(predictions));
        if (juryPhase) j.put("juryVotes", masked(juryVotes));
        if (!actor && game == 8) j.put("bluffTruth", -1);
        if ("RULE_VOTE".equals(screen)) j.put("reportVotes", masked(reportVotes));
        if ("GAME".equals(screen) && (game == 0 || game == 2 || game == 5)) {
          String[] fr = new String[4], en = new String[4];
          if (game == 0) {
            j.put("screenPromptFr", QUIZ_FR[variant][0]);
            j.put("screenPromptEn", QUIZ_EN[variant][0]);
            for (int i = 0; i < 4; i++) {
              int option = (i - target + 4) % 4;
              fr[i] = QUIZ_FR[variant][option + 1];
              en[i] = QUIZ_EN[variant][option + 1];
            }
          } else if (game == 2) {
            for (int i = 0; i < 4; i++) {
              int option = (i - target + 4) % 4;
              fr[i] = TUNES[(variant + option) % TUNES.length][0];
              en[i] = TUNES[(variant + option) % TUNES.length][1];
            }
          } else {
            if (actor && !drawingReady) {
              j.put("screenPromptFr", DRAW[variant][0]);
              j.put("screenPromptEn", DRAW[variant][1]);
            }
            for (int i = 0; i < 4; i++) {
              int option = (i - target + 4) % 4;
              fr[i] = DRAW[(variant + option) % DRAW.length][0];
              en[i] = DRAW[(variant + option) % DRAW.length][1];
            }
          }
          j.put("screenChoicesFr", new JSONArray(fr));
          j.put("screenChoicesEn", new JSONArray(en));
          if (game == 0 || game == 5) j.put("variant", -1);
        }
        if (("GAME".equals(screen) || "TRANSITION".equals(screen)
            || "HANDOFF".equals(screen) || "BET".equals(screen)
            || "PREDICT".equals(screen)) && !actor && game != 9) {
          if (!drawGuesser) {
            j.put("variant", -1);
          }
          if (game == 6) j.put("sequence", new JSONArray());
        }
        if (game == 5 && !actor && !drawingReady) j.put("variant", -1);
      }
    } catch (Exception ignored) { }
    return j;
  }

  private static JSONArray masked(int[] values) {
    JSONArray result = new JSONArray();
    for (int value : values) result.put(value < 0 ? -1 : 2);
    return result;
  }

  private JSONObject json(boolean includePhotos, boolean includeStrokes) {
    JSONObject j = new JSONObject();
    try {
      j.put("revision", revision);
      j.put("sentAt", System.currentTimeMillis());
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
      j.put("roundPoints", roundPoints);
      j.put("roundSips", roundSips);
      j.put("chosenCup", chosenCup);
      j.put("reflexSeed", reflexSeed);
      j.put("revealUntil", revealUntil);
      j.put("juryPhase", juryPhase);
      j.put("bluffTruth", bluffTruth);
      j.put("juryVotes", new JSONArray(juryVotes));
      j.put("predictions", new JSONArray(predictions));
      j.put("betPlaced", betPlaced);
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
      j.put("secretMask", secretMask);
      j.put("secretId", secretId);
      j.put("ruleOffers", new JSONArray(ruleOffers));
      j.put("reportTarget", reportTarget);
      j.put("reportBy", reportBy);
      j.put("reportReturn", reportReturn);
      j.put("reportVotes", new JSONArray(reportVotes));
      j.put("reportDeadline", reportDeadline);
      j.put("rulePenaltyApplied", rulePenaltyApplied);
      JSONArray ps = new JSONArray();
      for (Player p : players) ps.put(p.json(includePhotos));
      j.put("players", ps);
      JSONArray d = new JSONArray();
      for (int x : deck) d.put(x);
      j.put("deck", d);
      j.put("lastVariant", new JSONArray(lastVariant));
      JSONArray s = new JSONArray();
      for (int x : sequence) s.put(x);
      j.put("sequence", s);
      JSONArray lines = new JSONArray();
      for (float[] v : strokes) {
        if (!includeStrokes) break;
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
    java.util.HashMap<String, String> knownPhotos = new java.util.HashMap<>();
    for (Player player : players) knownPhotos.put(player.name, player.photo);
    players.clear();
    JSONArray ps = j.optJSONArray("players");
    if (ps != null)
      for (int i = 0; i < ps.length(); i++) {
        JSONObject row = ps.optJSONObject(i);
        if (row == null) continue;
        Player player = Player.from(row);
        if (!row.has("photo")) player.photo = knownPhotos.getOrDefault(player.name, "");
        players.add(player);
      }
    revision = j.optLong("revision", revision);
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
    roundPoints = j.optInt("roundPoints");
    roundSips = j.optInt("roundSips");
    chosenCup = j.optInt("chosenCup", -1);
    reflexSeed = j.optInt("reflexSeed");
    revealUntil = j.optLong("revealUntil");
    juryPhase = j.optBoolean("juryPhase");
    bluffTruth = j.optInt("bluffTruth", -1);
    juryVotes = readInts(j.optJSONArray("juryVotes"));
    predictions = readInts(j.optJSONArray("predictions"));
    betPlaced = j.optBoolean("betPlaced");
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
    secretMask = j.optInt("secretMask");
    secretId = j.optInt("secretId", -1);
    ruleOffers = readInts(j.optJSONArray("ruleOffers"));
    if (!ruleOwner.isEmpty() && ruleId < 0 && ruleOffers.length != 3)
      ruleOffers = new int[] {0, 1, 2};
    reportTarget = j.optString("reportTarget", "");
    reportBy = j.optString("reportBy", "");
    reportReturn = j.optString("reportReturn", "VOTE");
    reportVotes = readInts(j.optJSONArray("reportVotes"));
    reportDeadline = j.optLong("reportDeadline");
    rulePenaltyApplied = j.optBoolean("rulePenaltyApplied");
    deck.clear();
    JSONArray d = j.optJSONArray("deck");
    if (d != null) for (int i = 0; i < d.length(); i++) deck.add(d.optInt(i));
    java.util.Arrays.fill(lastVariant, -1);
    JSONArray previous = j.optJSONArray("lastVariant");
    if (previous != null)
      for (int i = 0; i < Math.min(lastVariant.length, previous.length()); i++)
        lastVariant[i] = previous.optInt(i, -1);
    JSONArray s = j.optJSONArray("sequence");
    sequence = new int[s == null ? 0 : s.length()];
    for (int i = 0; i < sequence.length; i++) sequence[i] = s.optInt(i);
    screenPromptFr = j.optString("screenPromptFr", "");
    screenPromptEn = j.optString("screenPromptEn", "");
    screenChoicesFr = readStrings(j.optJSONArray("screenChoicesFr"));
    screenChoicesEn = readStrings(j.optJSONArray("screenChoicesEn"));
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

  private static String[] readStrings(JSONArray a) {
    if (a == null) return new String[0];
    String[] result = new String[a.length()];
    for (int i = 0; i < result.length; i++) result[i] = a.optString(i, "");
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
    {"Combien de cerveaux a une pieuvre ?", "9", "1", "2", "6"},
    {"Quelle est la couleur de la peau de l'ours polaire ?", "Noire", "Blanche", "Rose", "Bleue"},
    {"Quel oiseau sait voler en marche arrière ?", "Colibri", "Corbeau", "Flamant", "Manchot"},
    {"Combien d'os possède généralement un adulte ?", "206", "106", "306", "406"},
    {"Quelle planète est la plus chaude ?", "Vénus", "Mercure", "Mars", "Jupiter"},
    {"Quel est le plus grand désert du monde ?", "Antarctique", "Sahara", "Gobi", "Atacama"},
    {"Quel animal a des empreintes proches des nôtres ?", "Koala", "Pingouin", "Dauphin", "Lama"},
    {"Dans quel pays le papier a-t-il été inventé ?", "Chine", "Italie", "Pérou", "Égypte"},
    {"Combien de faces a un dé de jeu de rôle D20 ?", "20", "12", "16", "24"},
    {"Quel organe humain est le plus grand ?", "Peau", "Foie", "Cœur", "Poumon"},
    {"Quel métal est liquide à température ambiante ?", "Mercure", "Cuivre", "Fer", "Or"},
    {"Quelle planète a les anneaux les plus visibles ?", "Saturne", "Mars", "Terre", "Vénus"}
  };
  public static final String[][] QUIZ_EN = {
    {"Which animal has three hearts?", "Octopus", "Panda", "Duck", "Mosquito"},
    {"How many sides on a standard die?", "6", "8", "12", "4"},
    {"Which fruit is botanically a berry?", "Banana", "Strawberry", "Raspberry", "Cherry"},
    {"Which planet may rain diamonds?", "Neptune", "Mars", "Mercury", "Venus"},
    {"Which animal can sleep standing?", "Horse", "Octopus", "Penguin", "Mole"},
    {"How many brains does an octopus have?", "9", "1", "2", "6"},
    {"What color is a polar bear's skin?", "Black", "White", "Pink", "Blue"},
    {"Which bird can fly backward?", "Hummingbird", "Crow", "Flamingo", "Penguin"},
    {"How many bones does an adult usually have?", "206", "106", "306", "406"},
    {"Which planet is the hottest?", "Venus", "Mercury", "Mars", "Jupiter"},
    {"What is the world's largest desert?", "Antarctica", "Sahara", "Gobi", "Atacama"},
    {"Which animal has fingerprints like ours?", "Koala", "Penguin", "Dolphin", "Llama"},
    {"Where was paper invented?", "China", "Italy", "Peru", "Egypt"},
    {"How many faces does a D20 role-playing die have?", "20", "12", "16", "24"},
    {"What is the human body's largest organ?", "Skin", "Liver", "Heart", "Lung"},
    {"Which metal is liquid at room temperature?", "Mercury", "Copper", "Iron", "Gold"},
    {"Which planet has the most visible rings?", "Saturn", "Mars", "Earth", "Venus"}
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
    },
    {"Rejoue la démarche du pote à ta droite. Qu'il valide !", "Copy the walk of the friend on your right. Get their verdict!"},
    {"Pose de couverture d'album avec deux voisins.", "Pose for an album cover with two friends."},
    {"Fais une entrée de star au ralenti, applaudissements obligatoires.", "Make a slow-motion star entrance. Demand applause."},
    {"Mets-toi en statue de musée. Les autres te donnent un titre.", "Become a museum statue. Let the others name the artwork."},
    {"Imite un barman qui sert un cocktail invisible.", "Mime a bartender serving an invisible cocktail."},
    {"Fais le robot qui manque de batterie pendant dix secondes.", "Be a robot running out of battery for ten seconds."},
    {"Danse avec un partenaire imaginaire sans quitter ta place.", "Dance with an imaginary partner without leaving your spot."},
    {"Deviens le commentateur sportif du prochain geste d'un ami.", "Commentate a friend's next move like a sports announcer."},
    {"Mime ton humeur du lundi à huit heures. Le groupe doit deviner.", "Mime your Monday 8 a.m. mood. The group must guess."},
    {"Pose en super-héros dont le pouvoir est de faire l'apéro.", "Pose as a superhero whose power is hosting parties."},
    {"Fais une révérence royale à la personne de ton choix.", "Give a royal bow to someone of your choosing."},
    {"Transforme une chaise en trône avec une pose grandiose.", "Turn a chair into a throne with a grand pose."}
  };
  public static final String[][] DRAW = {
    {"Une licorne en boîte de nuit", "A unicorn at a nightclub"},
    {"Un canard DJ", "A DJ duck"},
    {"Un astronaute en slip", "An astronaut in underwear"},
    {"Un avocat qui danse", "A dancing avocado"},
    {"Une pizza qui pleure", "A crying pizza"},
    {"Un robot amoureux", "A robot in love"},
    {"Un chat qui gouverne la ville", "A cat ruling the city"},
    {"Une licorne en métro", "A unicorn on the subway"},
    {"Un croissant bodybuilder", "A bodybuilder croissant"},
    {"Un lama qui fait du skate", "A llama on a skateboard"},
    {"Une boule disco triste", "A sad disco ball"},
    {"Une grenouille en costume", "A frog in a suit"},
    {"Un taco astronaute", "An astronaut taco"},
    {"Un fantôme au karaoké", "A ghost at karaoke"},
    {"Un dragon qui souffle des bulles", "A dragon blowing bubbles"},
    {"Un pigeon DJ", "A DJ pigeon"},
    {"Une chaussette couronnée", "A crowned sock"}
  };
  public static final String[][] BLUFF = {
    {"Raconte une rencontre improbable pendant une soirée.", "Tell us about an unlikely encounter at a party."},
    {"Décris ton pire raté en cuisine.", "Describe your biggest kitchen disaster."},
    {"Raconte un surnom que quelqu'un t'a donné.", "Tell us about a nickname someone gave you."},
    {"Raconte une fois où tu as fait semblant de comprendre un jeu.", "Tell us about a time you pretended to understand a game."},
    {"Décris un objet perdu au pire moment.", "Describe an item you lost at the worst moment."},
    {"Raconte un toast qui a tourné bizarrement.", "Tell us about a toast that went strangely wrong."},
    {"Raconte une petite victoire dont tu étais trop fier.", "Tell us about a tiny win you were too proud of."},
    {"Décris un cadeau que tu n'as pas su comment recevoir.", "Describe a gift you did not know how to react to."},
    {"Raconte une conversation avec un inconnu mémorable.", "Tell us about a memorable conversation with a stranger."},
    {"Raconte une fois où tu t'es trompé de personne.", "Tell us about a time you mistook someone for somebody else."},
    {"Décris un trajet qui a pris une tournure absurde.", "Describe a journey that took an absurd turn."},
    {"Raconte une excuse beaucoup trop créative.", "Tell us about an excuse that was far too creative."},
    {"Raconte ton talent le plus inutile.", "Tell us about your most useless talent."},
    {"Décris le meilleur hasard de ta semaine.", "Describe the best coincidence of your week."},
    {"Raconte une mésaventure avec une photo de groupe.", "Tell us about a mishap with a group photo."},
    {"Décris une règle de maison complètement inattendue.", "Describe a completely unexpected house rule."},
    {"Raconte une scène digne d'un jeu vidéo dans la vraie vie.", "Tell us about a real-life moment that felt like a video game."}
  };
  public static final String[][] TUNES = {
    {"Une montée", "Rising notes"},
    {"Une descente", "Falling notes"},
    {"De grands sauts", "Big jumps"},
    {"Des échos espacés", "Spaced echoes"},
    {"Des notes doublées", "Double notes"},
    {"Un rythme cassé", "A broken rhythm"}
  };
}
