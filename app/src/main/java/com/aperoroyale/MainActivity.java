package com.aperoroyale;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.util.Base64;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.KeyEvent;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/** Activity owns the host authority, persistence, sound and external integrations. */
public final class MainActivity extends Activity
    implements ArcadeView.Actions, PartyNetwork.Events {
  final GameEngine game = new GameEngine();
  GameStore store;
  ArcadeAudio audio;
  PartyNetwork network;
  ArcadeView view;
  private boolean savedSession = false;
  private String status = "";
  private int photoPlayer = -1;
  private String statsOrigin = "HOME";
  private String settingsOrigin = "HOME";
  private long settingsPausedAt = 0;
  private String guideOrigin = "HOME";
  private boolean passPending = false, soundEffects = true, haptics = true;
  private long passStartedAt = 0;
  private long backgroundAt = 0;
  private boolean foreground = false;
  private long clockSkew = Long.MAX_VALUE;
  private Runnable pendingBluetooth;
  private int lastBeatCue = -1, lastBeatTurn = -1;
  private int musicProvider = MusicLinks.ORIGINAL;
  private int localDialogDepth = 0;
  private long localDialogStartedAt = 0;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(13, 16, 37));
    getWindow().setNavigationBarColor(Color.rgb(13, 16, 37));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    store = new GameStore(this);
    savedSession = store.load(game);
    if (savedSession && !game.players.isEmpty()) {
      GameEngine retained = new GameEngine();
      if (store.loadRoster(retained) == 0) store.saveRoster(game);
    }
    if (savedSession) game.screen = "HOME";
    audio = new ArcadeAudio();
    audio.setEnabled(getPreferences(MODE_PRIVATE).getBoolean("music", true));
    audio.setStyle(getPreferences(MODE_PRIVATE).getInt("musicStyle", 0));
    audio.setVolume(getPreferences(MODE_PRIVATE).getFloat("musicVolume", .18f));
    soundEffects = getPreferences(MODE_PRIVATE).getBoolean("soundEffects", true);
    haptics = getPreferences(MODE_PRIVATE).getBoolean("haptics", true);
    network = new PartyNetwork(this);
    musicProvider = getPreferences(MODE_PRIVATE).getInt("musicProvider", MusicLinks.ORIGINAL);
    if (musicProvider < 0 || musicProvider >= MusicLinks.COUNT)
      musicProvider = MusicLinks.ORIGINAL;
    audio.setExternalRadio(musicProvider != MusicLinks.ORIGINAL);
    view = new ArcadeView(this, game, this);
    setContentView(view);
    audio.start();
    view.postDelayed(this::clockTick, 100);
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  }

  private void clockTick() {
    if (isFinishing()) return;
    audio.setExternalRadio(network.connected || musicProvider != MusicLinks.ORIGINAL);
    audio.setScene(game.screen, game.game);
    if ("GAME".equals(game.screen) && game.game == 7 && foreground && soundEffects) {
      long now = hostNow();
      int beat = RhythmClock.beat(game.started, now);
      int phase = RhythmClock.phase(game.started, now);
      if (lastBeatTurn != game.turn) { lastBeatTurn = game.turn; lastBeatCue = -1; }
      if (beat >= 0 && beat != lastBeatCue && phase >= RhythmClock.TARGET_MS
          && phase < RhythmClock.TARGET_MS + 190) {
        lastBeatCue = beat;
        audio.beat();
      }
    }
    boolean timersActive = (network.hosting || foreground) && localDialogDepth == 0;
    if (!network.connected && timersActive && !passPending
        && (game.voteTimedOut(System.currentTimeMillis())
            || game.rulePickTimedOut(System.currentTimeMillis()))) save();
    if (!network.connected && timersActive && !passPending && game.predictionTimedOut()) {
      setPassPending(false);
      beginGameAudio();
      save();
    }
    if (!network.connected && timersActive && !passPending && game.crewTimedOut()) {
      setPassPending(false);
      beginGameAudio();
      save();
    }
    if (!network.connected && timersActive && !passPending && game.ruleVoteTimedOut())
      finishRuleVote();
    if (!network.connected && timersActive && "GAME".equals(game.screen) && game.game == 4
        && game.chosenCup >= 0 && System.currentTimeMillis() >= game.revealUntil)
      finishGame(game.cupIsSafe());
    if (!network.connected && timersActive && !passPending
        && "GAME".equals(game.screen)
        && game.deadline > 0
        && System.currentTimeMillis() > game.deadline) {
      finishGame(game.juryPhase ? game.juryVerdict()
          : game.game == 5 && game.drawingReady ? game.drawWin()
          : (game.game == 3 && game.taps >= game.reflexGoal())
              || (game.game == 7 && game.rhythmHits >= 4));
    }
    view.postDelayed(this::clockTick, 100);
  }

  @Override
  protected void onPause() {
    foreground = false;
    backgroundAt = System.currentTimeMillis();
    audio.setSuspended(true);
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    boolean timedScreen = "GAME".equals(game.screen) || "PREDICT".equals(game.screen)
        || "CREW".equals(game.screen) || "VOTE".equals(game.screen)
        || "RULE_PICK".equals(game.screen)
        || "RULE_VOTE".equals(game.screen);
    if (backgroundAt > 0 && localDialogDepth == 0 && network != null && !network.hosting && !network.connected
        && !passPending && timedScreen) {
      long paused = Math.max(0, System.currentTimeMillis() - backgroundAt);
      if (game.started > 0) game.started += paused;
      if (game.deadline > 0) game.deadline += paused;
      if (game.reportDeadline > 0) game.reportDeadline += paused;
      if (game.revealUntil > 0) game.revealUntil += paused;
      if (paused > 0 && view != null) save();
    }
    backgroundAt = 0;
    foreground = true;
    if (audio != null) audio.setSuspended(false);
  }

  @Override
  protected void onDestroy() {
    network.close();
    audio.stop();
    store.close();
    super.onDestroy();
  }

  @Override
  public void save() {
    game.revision++;
    store.save(game);
    savedSession = true;
    if (!("GAME".equals(game.screen) && game.game == 5 && !game.drawingReady
        && !game.strokes.isEmpty())) network.broadcast(game);
    view.invalidate();
  }

  @Override
  public void finishGame(boolean won) {
    if (!"GAME".equals(game.screen)) return;
    setPassPending(false);
    GameEngine.Player p = game.current();
    long duration = Math.max(0, System.currentTimeMillis() - game.started);
    game.finish(won);
    if (p != null) store.record(p, game.game, won,
        game.roundPoints, game.roundSips > 0 ? 1 : 0, game.roundSips, duration);
    for (int i = 0; i < game.players.size() && i < game.predictions.length; i++) {
      if (i == game.active || game.predictions[i] < 0 || game.predictions[i] > 1) continue;
      boolean correct = (game.predictions[i] == 1) == won;
      store.recordPrediction(game.players.get(i), game.game, correct,
          correct ? game.predictions[i] == 0 ? 50 : 35 : 0,
          correct || game.ruleId == 1 ? 0 : 1);
    }
    for (int i = 0; i < game.players.size() && i < game.crewPoints.length; i++)
      if (i != game.active && game.crewPoints[i] > 0)
        store.recordCrew(game.players.get(i), game.game,
            (game.game == 0 || game.game == 2) && game.crewChoices[i] == game.target
                || game.game == 5 && game.drawGuesses[i] == game.target,
            game.crewPoints[i]);
    if (soundEffects) {
      if (won) audio.win();
      else audio.lose();
    }
    haptic();
    save();
  }

  @Override
  public void enterGame() {
    game.enterGame();
    if ("HANDOFF".equals(game.screen) && soundEffects) audio.turn();
    save();
  }

  @Override
  public void readyTurn() {
    if (network.connected) { network.command("READY", 0); return; }
    GameEngine.Player current = game.current();
    if (current == null || network.isRemote(current.name)) return;
    game.readyTurn();
    save();
  }

  public GameEngine.Player localGuesser() {
    if (!"GAME".equals(game.screen) || game.game != 5 || !game.drawingReady
        || game.players.size() < 2) return null;
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i >= 0 && i != game.active && i < game.drawGuesses.length
          && game.drawGuesses[i] == -1 ? game.players.get(i) : null;
    }
    for (int step = 1; step < game.players.size(); step++) {
      int i = (game.active + step) % game.players.size();
      if (i < game.drawGuesses.length && game.drawGuesses[i] == -1
          && !network.isRemote(game.players.get(i).name)) return game.players.get(i);
    }
    return null;
  }

  @Override
  public void drawingReady() {
    if (!"GAME".equals(game.screen) || game.game != 5 || game.drawingReady) return;
    if (!game.beginDrawGuess()) return;
    setPassPending(localGuesser() != null);
    save();
  }

  @Override
  public void drawGuess(int choice) {
    if (network.connected) { network.command("DRAW_GUESS", choice); return; }
    GameEngine.Player guesser = localGuesser();
    if (passPending || guesser == null || !game.drawGuess(guesser.name, choice)) return;
    if (game.drawGuessComplete()) finishGame(game.drawWin());
    else { setPassPending(localGuesser() != null); save(); }
  }

  @Override
  public void placeBet(int sips) {
    if (!game.placeBet(sips)) return;
    setPassPending(localCrew() != null || localPredictor() != null);
    save();
  }

  public GameEngine.Player localCrew() {
    if (!"CREW".equals(game.screen)) return null;
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i >= 0 && i != game.active && i < game.crewChoices.length
          && game.crewChoices[i] == -1 ? game.players.get(i) : null;
    }
    for (int step = 1; step < game.players.size(); step++) {
      int i = (game.active + step) % game.players.size();
      if (i < game.crewChoices.length && game.crewChoices[i] == -1
          && !network.isRemote(game.players.get(i).name)) return game.players.get(i);
    }
    return null;
  }

  @Override
  public void crewPick(int choice) {
    GameEngine.Player voter = localCrew();
    if (voter == null || passPending) return;
    if (network.connected) { network.command("CREW_PICK", choice); return; }
    if (game.crewPick(voter.name, choice)) {
      setPassPending("CREW".equals(game.screen) && localCrew() != null);
      if ("GAME".equals(game.screen)) beginGameAudio();
      save();
    }
  }

  public GameEngine.Player localPredictor() {
    if (!"PREDICT".equals(game.screen)) return null;
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i >= 0 && i != game.active && i < game.predictions.length
          && game.predictions[i] == -1 ? game.players.get(i) : null;
    }
    for (int step = 1; step < game.players.size(); step++) {
      int i = (game.active + step) % game.players.size();
      if (i < game.predictions.length && game.predictions[i] == -1
          && !network.isRemote(game.players.get(i).name)) return game.players.get(i);
    }
    return null;
  }

  @Override
  public void predict(boolean win) {
    GameEngine.Player voter = localPredictor();
    if (voter == null || passPending) return;
    if (network.connected) { network.command("PREDICT", win ? 1 : 0); return; }
    if (game.predict(voter.name, win)) {
      setPassPending("PREDICT".equals(game.screen) && localPredictor() != null);
      if ("GAME".equals(game.screen)) beginGameAudio();
      save();
    }
  }

  @Override
  public void beginJury() {
    if (game.beginJury()) {
      setPassPending(localJudge() != null);
      save();
    }
  }

  @Override
  public void bluffTruth(boolean trueStory) {
    if (network.connected) { network.command("BLUFF_TRUTH", trueStory ? 1 : 0); return; }
    GameEngine.Player actor = game.current();
    if (actor == null || network.isRemote(actor.name)) return;
    if (game.chooseBluffTruth(actor.name, trueStory)) {
      setPassPending(localJudge() != null);
      save();
    }
  }

  public GameEngine.Player localJudge() {
    if (!game.juryPhase || !"GAME".equals(game.screen)) return null;
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i >= 0 && i != game.active && i < game.juryVotes.length
          && game.juryVotes[i] == -1 ? game.players.get(i) : null;
    }
    for (int step = 1; step < game.players.size(); step++) {
      int i = (game.active + step) % game.players.size();
      if (i < game.juryVotes.length && game.juryVotes[i] == -1
          && !network.isRemote(game.players.get(i).name)) return game.players.get(i);
    }
    return null;
  }

  @Override
  public void judge(boolean yes) {
    GameEngine.Player juror = localJudge();
    if (juror == null || passPending) return;
    if (network.connected) { network.command("JUDGE", yes ? 1 : 0); return; }
    if (game.castJury(juror.name, yes)) {
      if (game.juryComplete()) finishGame(game.juryVerdict());
      else {
        setPassPending(localJudge() != null);
        save();
      }
    }
  }

  @Override
  public void reflexTap(int expectedStep) {
    if (network.connected) { network.command("REFLEX", expectedStep); return; }
    GameEngine.Player player = game.current();
    if (player != null && game.reflexTap(player.name, expectedStep)) {
      if (game.taps >= game.reflexGoal()) finishGame(true);
      else save();
    }
  }

  @Override
  public void rhythmTap(int beat) {
    if (network.connected) { network.command("RHYTHM", beat); return; }
    GameEngine.Player player = game.current();
    if (player != null && game.rhythmTap(player.name, beat, false)) {
      if (game.rhythmHits >= 4) finishGame(true);
      else save();
    }
  }

  @Override
  public void selectCup(int cup) {
    if (network.connected) { network.command("CUP", cup); return; }
    if (game.selectCup(cup)) save();
  }

  @Override
  public void reportRule() {
    if (game.ruleId < 3 || !("VOTE".equals(game.screen) || "LIBRARY".equals(game.screen))) return;
    GameEngine.Player reporter = localParticipant();
    if (reporter == null) return;
    ArrayList<String> choices = new ArrayList<>();
    ArrayList<Integer> indexes = new ArrayList<>();
    for (int i = 0; i < game.players.size(); i++)
      if (!game.players.get(i).name.equals(reporter.name)) {
        choices.add(game.players.get(i).name);
        indexes.add(i);
      }
    new AlertDialog.Builder(this)
        .setTitle(game.t("Qui a brisé la règle ?", "Who broke the rule?"))
        .setItems(choices.toArray(new String[0]), (dialog, which) -> {
          int target = indexes.get(which);
          if (network.connected) network.command("REPORT", target);
          else if (game.reportRule(reporter.name, game.players.get(target).name)) {
            setPassPending(localRuleVoter() != null);
            save();
          }
        })
        .show();
  }

  public GameEngine.Player localRuleVoter() {
    if (!"RULE_VOTE".equals(game.screen)) return null;
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i >= 0 && i < game.reportVotes.length && game.reportVotes[i] == -1
          ? game.players.get(i) : null;
    }
    for (int step = 0; step < game.players.size(); step++) {
      int i = (game.active + step) % game.players.size();
      if (i < game.reportVotes.length && game.reportVotes[i] == -1
          && !network.isRemote(game.players.get(i).name)) return game.players.get(i);
    }
    return null;
  }

  @Override
  public void ruleVote(boolean yes) {
    GameEngine.Player voter = localRuleVoter();
    if (voter == null) return;
    if (network.connected) { network.command("RULE_VOTE", yes ? 1 : 0); return; }
    if (game.castRuleVote(voter.name, yes)) {
      if (!"RULE_VOTE".equals(game.screen)) finishRuleVote();
      else { setPassPending(localRuleVoter() != null); save(); }
    }
  }

  private void finishRuleVote() {
    setPassPending(false);
    boolean penalty = game.rulePenaltyApplied;
    if (penalty) store.recordRulePenalty(game.reportTarget);
    game.rulePenaltyApplied = false;
    message(penalty ? game.reportTarget + " +1 " + game.t("gorgée", "sip")
        : game.t("Règle non retenue", "Rule claim dismissed"));
    save();
  }

  public long hostNow() {
    return System.currentTimeMillis() - (network.connected && clockSkew != Long.MAX_VALUE ? clockSkew : 0);
  }

  private void beginGameAudio() {
    if (!"GAME".equals(game.screen)) return;
    if (soundEffects && game.crewCount() > 0 && game.game != 2) audio.turn();
    if (game.game == 7) {
      lastBeatTurn = game.turn;
      lastBeatCue = -1;
      audio.duck(18000);
    } else if (game.game == 2) {
      audio.duck(14000);
      audio.tune(game.variant);
    }
  }

  @Override
  public void bombTap() {
    if (network.connected) {
      network.command("BOMB", 0);
      return;
    }
    if (game.players.isEmpty() || game.bombNext >= game.players.size()) return;
    String name = game.players.get(game.bombNext).name;
    if (network.isRemote(name) || !game.bombTap(name)) return;
    if (game.taps >= game.bombGoal()) finishGame(true);
    else save();
  }

  public GameEngine.Player localBomber() {
    if (!"GAME".equals(game.screen) || game.game != 9
        || game.bombNext < 0 || game.bombNext >= game.players.size()) return null;
    GameEngine.Player holder = game.players.get(game.bombNext);
    if (network.connected) return holder.name.equals(network.localName) ? holder : null;
    return network.isRemote(holder.name) ? null : holder;
  }

  @Override
  public void bombPass(int targetPlayer) {
    GameEngine.Player holder = localBomber();
    if (holder == null || passPending) return;
    if (network.connected) { network.command("BOMB_PASS", targetPlayer); return; }
    if (game.bombPass(holder.name, targetPlayer)) {
      setPassPending(localBomber() != null);
      save();
    }
  }

  @Override
  public void bombCut(int wire) {
    GameEngine.Player holder = localBomber();
    if (holder == null || passPending) return;
    if (network.connected) { network.command("BOMB_CUT", wire); return; }
    int outcome = game.bombCut(holder.name, wire);
    if (outcome >= 0) finishGame(outcome == 1);
  }

  public boolean passPending() { return passPending; }

  private void setPassPending(boolean pending) {
    passPending = pending;
    passStartedAt = pending ? System.currentTimeMillis() : 0;
  }

  @Override
  public void confirmPass() {
    if (!passPending) return;
    long paused = Math.max(0, System.currentTimeMillis() - passStartedAt);
    if (("PREDICT".equals(game.screen) || "CREW".equals(game.screen)
        || "VOTE".equals(game.screen) || "RULE_PICK".equals(game.screen))
        && game.deadline > 0)
      game.deadline += paused;
    if ("GAME".equals(game.screen) && (game.juryPhase ||
        (game.game == 5 && game.drawingReady) || game.game == 9) && game.deadline > 0)
      game.deadline += paused;
    if ("GAME".equals(game.screen) && game.game == 9) game.started += paused;
    if ("RULE_VOTE".equals(game.screen) && game.reportDeadline > 0)
      game.reportDeadline += paused;
    setPassPending(false);
    save();
  }

  @Override
  public void skipPass() {
    if (!passPending || network.connected) return;
    String stage = game.screen;
    GameEngine.Player waiting = switch (stage) {
      case "VOTE" -> localVoter();
      case "CREW" -> localCrew();
      case "PREDICT" -> localPredictor();
      case "RULE_VOTE" -> localRuleVoter();
      case "GAME" -> game.juryPhase ? localJudge()
          : game.game == 5 && game.drawingReady ? localGuesser()
          : game.game == 9 ? localBomber() : null;
      default -> null;
    };
    if (waiting == null) return;
    confirmPass(); // The private handoff timer was paused for this person.
    if ("GAME".equals(stage) && game.game == 9 && !game.juryPhase) {
      finishGame(false);
      return;
    }
    if (!game.skipParticipation(waiting.name)) return;
    if ("GAME".equals(stage) && game.juryPhase && game.juryComplete()) {
      finishGame(game.juryVerdict());
      return;
    }
    if ("GAME".equals(stage) && game.game == 5 && game.drawingReady
        && game.drawGuessComplete()) {
      finishGame(game.drawWin());
      return;
    }
    if ("RULE_VOTE".equals(stage) && !"RULE_VOTE".equals(game.screen)) {
      finishRuleVote();
      return;
    }
    setPassPending(switch (game.screen) {
      case "VOTE" -> localVoter() != null;
      case "CREW" -> localCrew() != null;
      case "PREDICT" -> localPredictor() != null;
      case "RULE_VOTE" -> localRuleVoter() != null;
      case "GAME" -> game.juryPhase ? localJudge() != null
          : game.game == 5 && game.drawingReady ? localGuesser() != null : false;
      default -> false;
    });
    if ("GAME".equals(game.screen) && ("CREW".equals(stage) || "PREDICT".equals(stage)))
      beginGameAudio();
    save();
  }

  @Override
  public void addPlayer() {
    if (game.players.size() >= 6) {
      message(game.t("Salle complète (6 joueurs)", "Room full (6 players)"));
      return;
    }
    EditText name = input("Pseudo / Nickname");
    Spinner language = spinner(new String[] {"FR", "EN"});
    String[] labels = {"CAT", "FROG", "DUCK", "ALIEN", "ROBOT", "DISCO"};
    ArrayList<Integer> available = new ArrayList<>();
    ArrayList<String> names = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      boolean used = false;
      for (GameEngine.Player player : game.players) if (player.avatar == i) used = true;
      if (!used) { available.add(i); names.add(labels[i]); }
    }
    Spinner avatar = spinner(names.toArray(new String[0]));
    LinearLayout box = column(name, language, avatar);
    new AlertDialog.Builder(this)
        .setTitle(game.t("Nouveau joueur", "New player"))
        .setView(box)
        .setNegativeButton(game.t("Annuler", "Cancel"), null)
        .setPositiveButton(game.t("CRÉER", "CREATE"), (d, w) ->
            createPlayer(name, language, available.get(avatar.getSelectedItemPosition()), false))
        .setNeutralButton(game.t("CRÉER + PHOTO", "CREATE + PHOTO"), (d, w) ->
            createPlayer(name, language, available.get(avatar.getSelectedItemPosition()), true))
        .show();
  }

  private void createPlayer(EditText name, Spinner language, int avatar, boolean photo) {
    if (!game.addPlayer(name.getText().toString(), language.getSelectedItem().toString(), avatar)) {
      message(game.t("Pseudo indisponible", "Name unavailable"));
      return;
    }
    if (!network.connected && !network.hosting) store.saveRoster(game);
    save();
    if (photo) openPhotoPicker(game.players.size() - 1);
  }

  private void openPhotoPicker(int index) {
    photoPlayer = index;
    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
    pick.setType("image/*");
    pick.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(Intent.createChooser(pick, game.t("Choisir une image", "Choose image")), 301);
  }

  private void chooseAvatar(int index) {
    if (index < 0 || index >= game.players.size()) return;
    Bitmap sheet = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_sheet);
    if (sheet == null) return;
    GridLayout grid = new GridLayout(this);
    grid.setColumnCount(3);
    grid.setPadding(16, 16, 16, 16);
    AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(game.t("Choisis ton sprite", "Choose your sprite"))
        .setView(grid)
        .setNegativeButton(game.t("Fermer", "Close"), null)
        .create();
    for (int avatar = 0; avatar < 6; avatar++) {
      boolean taken = false;
      for (int i = 0; i < game.players.size(); i++)
        if (i != index && game.players.get(i).avatar == avatar) taken = true;
      if (taken) continue;
      int cellW = sheet.getWidth() / 3, cellH = sheet.getHeight() / 2;
      Bitmap crop = Bitmap.createBitmap(sheet, (avatar % 3) * cellW,
          (avatar / 3) * cellH, cellW, cellH);
      ImageView tile = new ImageView(this);
      tile.setImageBitmap(crop);
      tile.setScaleType(ImageView.ScaleType.CENTER_CROP);
      tile.setBackgroundColor(Color.rgb(25, 32, 65));
      tile.setPadding(6, 6, 6, 6);
      GridLayout.LayoutParams params = new GridLayout.LayoutParams();
      params.width = (int) (92 * getResources().getDisplayMetrics().density);
      params.height = (int) (92 * getResources().getDisplayMetrics().density);
      params.setMargins(5, 5, 5, 5);
      grid.addView(tile, params);
      final int chosen = avatar;
      tile.setOnClickListener(v -> {
        game.players.get(index).avatar = chosen;
        game.players.get(index).photo = "";
        if (network.connected) network.command("AVATAR", chosen);
        else { if (!network.hosting) store.saveRoster(game); save(); }
        dialog.dismiss();
      });
    }
    dialog.show();
  }

  @Override
  public void editPlayer(int index) {
    if (index < 0 || index >= game.players.size()) return;
    GameEngine.Player player = game.players.get(index);
    if (network.connected && !player.name.equals(network.localName)) return;
    boolean localLobby = "LOBBY".equals(game.screen) && !network.connected && !network.hosting;
    String[] choices = localLobby ? new String[] {
        game.t("Modifier pseudo et langue", "Edit name and language"),
        game.t("Importer une photo", "Import a photo"),
        game.t("Changer de sprite", "Change sprite"),
        game.t("Supprimer la photo", "Remove photo"),
        game.t("Retirer de la table", "Remove from table")
    } : new String[] {
        game.t("Importer une photo", "Import a photo"),
        game.t("Changer de sprite", "Change sprite"),
        game.t("Supprimer la photo", "Remove photo")
    };
    new AlertDialog.Builder(this)
        .setTitle(player.name)
        .setItems(choices, (d, which) -> {
          if (localLobby && which == 0) {
            editLocalProfile(index);
          } else if (localLobby && which == 4) {
            new AlertDialog.Builder(this)
                .setMessage(game.t("Retirer " + player.name + " de la table ?",
                    "Remove " + player.name + " from this table?"))
                .setNegativeButton(game.t("Garder", "Keep"), null)
                .setPositiveButton(game.t("Retirer", "Remove"), (dialog, choice) -> {
                  game.players.remove(index);
                  game.active = 0;
                  store.saveRoster(game);
                  save();
                }).show();
          } else if (which == (localLobby ? 1 : 0)) {
            openPhotoPicker(index);
          } else if (which == (localLobby ? 2 : 1)) {
            chooseAvatar(index);
          } else {
            player.photo = "";
            if (network.connected) network.profile("");
            else { if (!network.hosting) store.saveRoster(game); save(); }
          }
        }).show();
  }

  private void editLocalProfile(int index) {
    if (index < 0 || index >= game.players.size()) return;
    GameEngine.Player player = game.players.get(index);
    EditText nickname = input(game.t("Pseudo", "Nickname"));
    nickname.setText(player.name);
    Spinner language = spinner(new String[] {"FR", "EN"});
    language.setSelection("EN".equals(player.language) ? 1 : 0);
    new AlertDialog.Builder(this)
        .setTitle(game.t("Profil du joueur", "Player profile"))
        .setView(column(nickname, language))
        .setNegativeButton(game.t("Annuler", "Cancel"), null)
        .setPositiveButton(game.t("Enregistrer", "Save"), (dialog, which) -> {
          String name = nickname.getText().toString().trim();
          if (name.isEmpty() || name.length() > 16) {
            message(game.t("Pseudo : 1 à 16 caractères", "Nickname: 1 to 16 characters"));
            return;
          }
          for (int i = 0; i < game.players.size(); i++)
            if (i != index && game.players.get(i).name.equalsIgnoreCase(name)) {
              message(game.t("Pseudo déjà utilisé", "Nickname already in use"));
              return;
            }
          if (!store.renameProfile(player.name, name)) {
            message(game.t("Ce pseudo appartient déjà à un ancien profil",
                "This nickname belongs to another saved profile"));
            return;
          }
          player.name = name;
          player.language = language.getSelectedItem().toString();
          store.saveRoster(game);
          save();
        }).show();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 302) {
      Runnable continuation = pendingBluetooth;
      pendingBluetooth = null;
      if (resultCode == RESULT_OK && continuation != null) continuation.run();
      else message(game.t("Bluetooth désactivé", "Bluetooth is off"));
      return;
    }
    if (requestCode != 301 || resultCode != RESULT_OK || data == null || data.getData() == null
        || photoPlayer < 0 || photoPlayer >= game.players.size()) return;
    try {
      Uri uri = data.getData();
      BitmapFactory.Options bounds = new BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      try (InputStream input = getContentResolver().openInputStream(uri)) {
        BitmapFactory.decodeStream(input, null, bounds);
      }
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IllegalArgumentException("Invalid image");
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 256);
      Bitmap original;
      try (InputStream input = getContentResolver().openInputStream(uri)) {
        original = BitmapFactory.decodeStream(input, null, options);
      }
      if (original == null) throw new IllegalArgumentException("Invalid image");
      int side = Math.min(original.getWidth(), original.getHeight());
      Bitmap square = Bitmap.createBitmap(original, (original.getWidth() - side) / 2,
          (original.getHeight() - side) / 2, side, side);
      Bitmap small = Bitmap.createScaledBitmap(square, 128, 128, true);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      small.compress(Bitmap.CompressFormat.JPEG, 72, bytes);
      byte[] result = bytes.toByteArray();
      if (result.length > 40000) throw new IllegalArgumentException("Image too large");
      game.players.get(photoPlayer).photo = Base64.encodeToString(result, Base64.NO_WRAP);
      if (small != square) small.recycle();
      if (square != original) square.recycle();
      original.recycle();
      if (network.connected) network.profile(game.players.get(photoPlayer).photo);
      else { if (!network.hosting) store.saveRoster(game); save(); }
    } catch (Exception e) {
      message(game.t("Image illisible", "Could not read image"));
    }
  }

  public GameEngine.Player localVoter() {
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i < 0 || (game.votes.length > i && game.votes[i] >= 0) ? null : game.players.get(i);
    }
    for (int step = 0; step < game.players.size(); step++) {
      int i = (game.active + step) % game.players.size();
      if (!network.isRemote(game.players.get(i).name)
          && (game.votes.length <= i || game.votes[i] < 0)) return game.players.get(i);
    }
    return null;
  }

  public GameEngine.Player localParticipant() {
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i < 0 ? null : game.players.get(i);
    }
    if (!game.ruleOwner.isEmpty() && game.ruleId < 0) {
      int i = game.indexOf(game.ruleOwner);
      if (i >= 0 && !network.isRemote(game.ruleOwner)) return game.players.get(i);
    }
    GameEngine.Player voter = localVoter();
    if (voter != null) return voter;
    for (GameEngine.Player p : game.players) if (!network.isRemote(p.name)) return p;
    return null;
  }

  @Override
  public void vote(int choice) {
    if (network.connected) network.command("VOTE", choice);
    else {
      if (passPending) return;
      GameEngine.Player p = localVoter();
      if (p != null && game.castVote(p.name, choice)) {
        setPassPending("VOTE".equals(game.screen) && localVoter() != null);
        save();
      }
    }
  }

  @Override
  public void hiddenTap(int slot) {
    if (network.connected && game.hiddenWaiting(network.localName))
      network.command("HIDDEN_TAP", slot);
  }

  @Override
  public void chooseRule(int id) {
    if (network.connected) network.command("RULE", id);
    else {
      GameEngine.Player p = localParticipant();
      if (p != null && game.chooseRule(p.name, id)) save();
    }
  }

  @Override
  public void selectGame(int id) {
    if (network.connected || !"LIBRARY".equals(game.screen) || id < 0 || id >= GameEngine.TYPES.length) return;
    game.freePick = id;
    game.startNext(id);
    save();
  }

  @Override
  public void setMode(String mode) {
    if (network.connected || !"LOBBY".equals(game.screen)) return;
    game.mode = mode;
    save();
  }

  @Override
  public void newParty() {
    network.close();
    setPassPending(false);
    clockSkew = Long.MAX_VALUE;
    game.newParty();
    store.loadRoster(game);
    save();
  }

  @Override
  public void clearRoster() {
    if (!"LOBBY".equals(game.screen) || network.connected || network.hosting) return;
    new AlertDialog.Builder(this)
        .setTitle(game.t("Changer de groupe ?", "Change the group?"))
        .setMessage(game.t("Les profils de cette table seront retirés. L'historique reste conservé.",
            "These table profiles will be removed. Your history stays saved."))
        .setNegativeButton(game.t("Garder", "Keep"), null)
        .setPositiveButton(game.t("Changer", "Change"), (dialog, which) -> {
          store.clearRoster();
          game.newParty();
          save();
        }).show();
  }

  private void returnToLobby() {
    setPassPending(false);
    game.newParty();
    store.loadRoster(game);
    save();
  }

  @Override
  public void showPartyMenu() {
    if (network.connected || network.hosting) {
      showSettings();
      return;
    }
    if ("LOBBY".equals(game.screen)) {
      pauseLocalDialog(new AlertDialog.Builder(this)
          .setTitle(game.t("Menu de la soirée", "Party menu"))
          .setItems(new String[] {game.t("Continuer", "Continue"),
              game.t("Accueil", "Home"), game.t("Réglages", "Settings"),
              game.t("Changer de groupe", "Change group")},
              (dialog, which) -> {
                if (which == 1) { game.screen = "HOME"; view.invalidate(); }
                else if (which == 2) showSettings();
                else if (which == 3) clearRoster();
              }).create()).show();
      return;
    }
    boolean resultScreen = "RESULT".equals(game.screen);
    pauseLocalDialog(new AlertDialog.Builder(this)
        .setTitle(game.t("Pause de la soirée", "Party pause"))
        .setItems(new String[] {game.t("Reprendre le jeu", "Resume game"),
            resultScreen ? game.t("Défi suivant", "Next challenge")
                : game.t("Annuler ce défi", "Cancel this challenge"),
            game.t("Retour au salon", "Back to lobby"),
            game.t("Accueil · reprendre plus tard", "Home · resume later"),
            game.t("Réglages", "Settings")},
            (dialog, which) -> {
              if (which == 1) {
                setPassPending(false);
                if (resultScreen) game.advance();
                else game.startSelection();
                save();
              } else if (which == 2) {
                pauseLocalDialog(new AlertDialog.Builder(this)
                    .setMessage(game.t("Quitter cette partie ? Les résultats déjà joués restent dans le classement.",
                        "Leave this party? Completed rounds remain in the leaderboard."))
                    .setNegativeButton(game.t("Rester", "Stay"), null)
                    .setPositiveButton(game.t("Retour au salon", "Back to lobby"),
                        (d, w) -> returnToLobby()).create()).show();
              } else if (which == 3) {
                game.screen = "HOME";
                view.invalidate();
              } else if (which == 4) showSettings();
            }).create()).show();
  }

  private AlertDialog pauseLocalDialog(AlertDialog dialog) {
    if (!network.connected && !network.hosting) {
      if (localDialogDepth++ == 0) localDialogStartedAt = System.currentTimeMillis();
      dialog.setOnDismissListener(d -> {
        if (--localDialogDepth > 0) return;
        localDialogDepth = 0;
        long paused = Math.max(0, System.currentTimeMillis() - localDialogStartedAt);
        boolean timed = "GAME".equals(game.screen) || "PREDICT".equals(game.screen)
            || "CREW".equals(game.screen) || "VOTE".equals(game.screen)
            || "RULE_PICK".equals(game.screen) || "RULE_VOTE".equals(game.screen);
        if (!timed) return;
        if (game.started > 0) game.started += paused;
        if (game.deadline > 0) game.deadline += paused;
        if (game.reportDeadline > 0) game.reportDeadline += paused;
        if (game.revealUntil > 0) game.revealUntil += paused;
        save();
      });
    }
    return dialog;
  }

  @Override
  public void resumeParty() {
    if (store.load(game)) {
      if ("HOME".equals(game.screen)) game.screen = "LOBBY";
      if ("GAME".equals(game.screen)) game.resumeGame();
      if ("GAME".equals(game.screen) && game.game == 7) {
        lastBeatTurn = game.turn;
        lastBeatCue = -1;
      }
      long now = System.currentTimeMillis();
      if ("PREDICT".equals(game.screen) || "CREW".equals(game.screen))
        game.deadline = now + ("TURBO".equals(game.mode) ? 8000 : 12000);
      if ("VOTE".equals(game.screen)) game.deadline = now + 18000;
      if ("RULE_PICK".equals(game.screen)) game.deadline = now + 15000;
      if ("RULE_VOTE".equals(game.screen)) game.reportDeadline = now + 10000;
      setPassPending(("VOTE".equals(game.screen) && game.voteCount() > 0 && localVoter() != null)
          || ("PREDICT".equals(game.screen) && localPredictor() != null)
          || ("CREW".equals(game.screen) && localCrew() != null)
          || ("RULE_VOTE".equals(game.screen) && localRuleVoter() != null)
          || ("GAME".equals(game.screen) && game.juryPhase && localJudge() != null)
          || ("GAME".equals(game.screen) && game.game == 5 && game.drawingReady
              && localGuesser() != null)
          || ("GAME".equals(game.screen) && game.game == 9 && game.taps > 0
              && game.taps % game.bombTapsPerHolder() == 0
              && !game.bombAwaitingPass && localBomber() != null));
      save();
    } else message("No saved party / Aucune partie");
  }

  @Override
  public boolean hasSavedParty() {
    return savedSession && !game.players.isEmpty();
  }

  @Override
  public void startHost() {
    if (network.connected) return;
    new AlertDialog.Builder(this)
        .setTitle(game.t("Héberger une salle", "Host a room"))
        .setItems(new String[] {"Wi-Fi", "Bluetooth", "Internet"}, (d, which) -> {
          if (which == 0) {
            network.close();
            network = new PartyNetwork(this);
            network.host();
            save();
          } else if (which == 1) withBluetooth(() -> {
            network.close();
            BluetoothPartyNetwork bluetooth = new BluetoothPartyNetwork(this);
            network = bluetooth;
            bluetooth.hostBluetooth(bluetoothAdapter());
            save();
          });
          else hostInternet();
        }).show();
  }

  @Override
  public void joinRoom() {
    new AlertDialog.Builder(this)
        .setTitle(game.t("Rejoindre une salle", "Join a room"))
        .setItems(new String[] {"Wi-Fi", "Bluetooth", "Internet"}, (d, which) -> {
          if (which == 0) joinWifi();
          else if (which == 1) withBluetooth(this::joinBluetoothRoom);
          else joinInternet();
        }).show();
  }

  private void hostInternet() {
    EditText relay = input("TLS relay URL");
    relay.setText(getPreferences(MODE_PRIVATE).getString("relay", InternetPartyNetwork.DEFAULT_RELAY));
    new AlertDialog.Builder(this)
        .setTitle(game.t("Salle Internet", "Internet room"))
        .setView(column(relay))
        .setNegativeButton(game.t("Annuler", "Cancel"), null)
        .setPositiveButton(game.t("Créer", "Create"), (d, w) -> {
          String uri = relay.getText().toString().trim();
          getPreferences(MODE_PRIVATE).edit().putString("relay", uri).apply();
          network.close();
          InternetPartyNetwork internet = new InternetPartyNetwork(this);
          network = internet;
          internet.hostInternet(uri);
          save();
        }).show();
  }

  private void joinInternet() {
    EditText code = input("12-character room code / code de salle");
    EditText name = input("Pseudo / Nickname");
    Spinner lang = spinner(new String[] {"FR", "EN"});
    EditText relay = input("TLS relay URL");
    relay.setText(getPreferences(MODE_PRIVATE).getString("relay", InternetPartyNetwork.DEFAULT_RELAY));
    new AlertDialog.Builder(this)
        .setTitle(game.t("Rejoindre par Internet", "Join over Internet"))
        .setView(column(code, name, lang, relay))
        .setNegativeButton(game.t("Annuler", "Cancel"), null)
        .setPositiveButton(game.t("Rejoindre", "Join"), (d, w) -> {
          String nickname = name.getText().toString().trim();
          if (nickname.isEmpty()) { message(game.t("Pseudo requis", "Nickname required")); return; }
          String uri = relay.getText().toString().trim();
          getPreferences(MODE_PRIVATE).edit().putString("relay", uri).apply();
          network.close();
          game.revision = 0;
          clockSkew = Long.MAX_VALUE;
          InternetPartyNetwork internet = new InternetPartyNetwork(this);
          network = internet;
          internet.joinInternet(uri, code.getText().toString(), nickname, lang.getSelectedItem().toString());
        }).show();
  }

  private void joinWifi() {
    EditText ip = input("Host IP / IP de l'hôte");
    EditText pin = input("6-digit room PIN / code à 6 chiffres");
    pin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    EditText name = input("Pseudo / Nickname");
    Spinner lang = spinner(new String[] {"FR", "EN"});
    new AlertDialog.Builder(this)
        .setTitle(game.t("Rejoindre un salon Wi-Fi", "Join Wi-Fi room"))
        .setView(column(ip, pin, name, lang))
        .setNegativeButton(game.t("Annuler", "Cancel"), null)
        .setPositiveButton(
            game.t("Rejoindre", "Join"),
            (d, w) -> {
              String n = name.getText().toString().trim();
              String address = ip.getText().toString().trim();
              String code = pin.getText().toString().trim();
              if (n.isEmpty() || address.isEmpty() || code.length() != 6) {
                message("Name, IP and 6-digit PIN required");
                return;
              }
              network.close();
              game.revision = 0;
              clockSkew = Long.MAX_VALUE;
              network = new PartyNetwork(this);
              network.join(address, n, lang.getSelectedItem().toString(), code);
            })
        .show();
  }

  private BluetoothAdapter bluetoothAdapter() {
    BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    return manager == null ? null : manager.getAdapter();
  }

  private void withBluetooth(Runnable ready) {
    BluetoothAdapter adapter = bluetoothAdapter();
    if (adapter == null) { message(game.t("Bluetooth indisponible", "Bluetooth unavailable")); return; }
    if (Build.VERSION.SDK_INT >= 31
        && checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
      pendingBluetooth = ready;
      requestPermissions(new String[] {android.Manifest.permission.BLUETOOTH_CONNECT}, 104);
      return;
    }
    if (!adapter.isEnabled()) {
      pendingBluetooth = ready;
      startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 302);
      return;
    }
    ready.run();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != 104) return;
    Runnable continuation = pendingBluetooth;
    pendingBluetooth = null;
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
        && continuation != null) withBluetooth(continuation);
    else message(game.t("Autorisation Bluetooth nécessaire", "Bluetooth permission needed"));
  }

  private void joinBluetoothRoom() {
    BluetoothAdapter adapter = bluetoothAdapter();
    if (adapter == null) return;
    ArrayList<BluetoothDevice> devices = new ArrayList<>(adapter.getBondedDevices());
    if (devices.isEmpty()) {
      message(game.t("Associe d'abord les téléphones dans Android > Bluetooth",
          "Pair the phones in Android Bluetooth settings first"));
      return;
    }
    String[] names = new String[devices.size()];
    for (int i = 0; i < devices.size(); i++) {
      String label = devices.get(i).getName();
      names[i] = label == null || label.isEmpty() ? devices.get(i).getAddress() : label;
    }
    new AlertDialog.Builder(this)
        .setTitle(game.t("Téléphone hôte associé", "Paired host phone"))
        .setItems(names, (d, which) -> joinBluetoothDetails(devices.get(which)))
        .show();
  }

  private void joinBluetoothDetails(BluetoothDevice device) {
    EditText pin = input("6-digit room PIN / code à 6 chiffres");
    pin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    EditText name = input("Pseudo / Nickname");
    Spinner lang = spinner(new String[] {"FR", "EN"});
    new AlertDialog.Builder(this)
        .setTitle(game.t("Rejoindre via Bluetooth", "Join over Bluetooth"))
        .setView(column(pin, name, lang))
        .setNegativeButton(game.t("Annuler", "Cancel"), null)
        .setPositiveButton(game.t("Rejoindre", "Join"), (d, w) -> {
          String code = pin.getText().toString().trim();
          String nickname = name.getText().toString().trim();
          if (code.length() != 6 || nickname.isEmpty()) {
            message(game.t("Code et pseudo requis", "PIN and nickname required"));
            return;
          }
          network.close();
          game.revision = 0;
          clockSkew = Long.MAX_VALUE;
          BluetoothPartyNetwork bluetooth = new BluetoothPartyNetwork(this);
          network = bluetooth;
          bluetooth.joinBluetooth(device, nickname, lang.getSelectedItem().toString(), code);
        }).show();
  }

  @Override
  public void showSettings() {
    if ("SETTINGS".equals(game.screen)) return;
    settingsOrigin = game.screen;
    settingsPausedAt = System.currentTimeMillis();
    game.screen = "SETTINGS";
    view.invalidate();
  }

  @Override
  public void guide() {
    guideOrigin = game.screen;
    game.screen = "GUIDE";
    view.invalidate();
  }

  @Override
  public void editMusicLink() {
    if (!MusicLinks.external(musicProvider)) return;
    EditText link = input(game.t("Lien de playlist HTTPS", "HTTPS playlist link"));
    link.setText(musicLink());
    String title = MusicLinks.name(musicProvider);
    pauseLocalDialog(new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(game.t("Colle le lien d'une playlist de cette plateforme. Vide = accueil de l'application.",
            "Paste a playlist link from this platform. Blank = app home."))
        .setView(link)
        .setPositiveButton(
            game.t("Enregistrer", "Save"),
            (d, w) -> {
              String value = link.getText().toString().trim();
              if (!value.isEmpty() && !MusicLinks.allowed(musicProvider, value)) {
                message(game.t("Lien HTTPS invalide pour cette plateforme", "Invalid HTTPS link for this platform"));
                return;
              }
              getPreferences(MODE_PRIVATE).edit().putString("musicLink" + musicProvider, value).apply();
              view.invalidate();
            })
        .setNegativeButton(game.t("Annuler", "Cancel"), null)
        .create()).show();
  }

  @Override
  public void setMusicProvider(int provider) {
    if (provider < 0 || provider >= MusicLinks.COUNT) return;
    musicProvider = provider;
    getPreferences(MODE_PRIVATE).edit().putInt("musicProvider", musicProvider).apply();
    audio.setExternalRadio(musicProvider != MusicLinks.ORIGINAL);
    view.invalidate();
  }

  @Override
  public void openMusicProvider() {
    if (!MusicLinks.external(musicProvider)) {
      showSettings();
      return;
    }
    try {
      startActivity(new Intent(Intent.ACTION_VIEW,
          Uri.parse(MusicLinks.destination(musicProvider, musicLink()))));
    } catch (Exception e) {
      message(game.t("Application musicale indisponible", "Music app unavailable"));
    }
  }

  @Override
  public void showRadioDock() {
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(18), dp(18), dp(18), dp(14));
    GradientDrawable background = new GradientDrawable();
    background.setColor(Color.rgb(22, 27, 40));
    background.setCornerRadius(dp(18));
    background.setStroke(dp(1), Color.rgb(213, 177, 103));
    body.setBackground(background);
    TextView title = radioText(game.t("RADIO APÉRO", "PARTY RADIO"), 23,
        Color.rgb(255, 248, 230));
    body.addView(title);
    TextView current = radioText(game.t("À L'ÉCOUTE · ", "TUNED TO · ")
        + MusicLinks.name(musicProvider).toUpperCase(), 12, Color.rgb(213, 177, 103));
    body.addView(current);
    TextView source = radioText(game.t("CHOISIS TON AMBIANCE", "CHOOSE YOUR SOUND"),
        12, Color.rgb(161, 172, 189));
    LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(-1, -2);
    sourceParams.topMargin = dp(19);
    body.addView(source, sourceParams);
    String[] names = {game.t("♫  ORIGINAL", "♫  ORIGINAL"), "◉  SPOTIFY",
        "◆  DEEZER", "♫  APPLE MUSIC", "▤  AMAZON MUSIC",
        game.t("×  SILENCE", "×  SILENCE")};
    final AlertDialog[] active = new AlertDialog[1];
    for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
      LinearLayout row = new LinearLayout(this);
      for (int column = 0; column < 2; column++) {
        final int provider = rowIndex * 2 + column;
        TextView card = radioCard(names[provider], provider == musicProvider, 66);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(66), 1);
        params.setMargins(dp(3), dp(4), dp(3), dp(4));
        row.addView(card, params);
        card.setOnClickListener(v -> {
          setMusicProvider(provider);
          active[0].dismiss();
          if (MusicLinks.external(provider)) openMusicProvider();
        });
      }
      body.addView(row);
    }
    TextView controls = radioText(game.t("LECTEUR ACTIF", "ACTIVE PLAYER"),
        12, Color.rgb(161, 172, 189));
    LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, -2);
    controlsParams.topMargin = dp(15);
    body.addView(controls, controlsParams);
    LinearLayout transport = new LinearLayout(this);
    TextView play = radioCard(game.t("⏯  PAUSE / JOUER", "⏯  PLAY / PAUSE"), false, 50);
    TextView next = radioCard(game.t("⏭  SUIVANT", "⏭  NEXT"), false, 50);
    LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(50), 1);
    half.setMargins(dp(3), dp(5), dp(3), dp(5));
    transport.addView(play, half);
    LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(50), 1);
    half2.setMargins(dp(3), dp(5), dp(3), dp(5));
    transport.addView(next, half2);
    play.setOnClickListener(v -> mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
    next.setOnClickListener(v -> mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT));
    body.addView(transport);
    LinearLayout playlistRow = new LinearLayout(this);
    TextView playlist = radioCard(game.t("↗  OUVRIR", "↗  OPEN"), false, 48);
    TextView edit = radioCard(game.t("✎  PLAYLIST", "✎  PLAYLIST"), false, 48);
    LinearLayout.LayoutParams playlistHalf = new LinearLayout.LayoutParams(0, dp(48), 1);
    playlistHalf.setMargins(dp(3), dp(5), dp(3), dp(5));
    playlistRow.addView(playlist, playlistHalf);
    LinearLayout.LayoutParams editHalf = new LinearLayout.LayoutParams(0, dp(48), 1);
    editHalf.setMargins(dp(3), dp(5), dp(3), dp(5));
    playlistRow.addView(edit, editHalf);
    body.addView(playlistRow);
    playlist.setOnClickListener(v -> {
      active[0].dismiss();
      if (MusicLinks.external(musicProvider)) openMusicProvider();
      else message(game.t("Choisis d'abord une plateforme", "Choose a music service first"));
    });
    edit.setOnClickListener(v -> {
      active[0].dismiss();
      if (MusicLinks.external(musicProvider)) editMusicLink();
      else message(game.t("Choisis d'abord une plateforme", "Choose a music service first"));
    });
    TextView foot = radioText(game.t("La lecture reste dans l'application musicale. Les touches contrôlent le lecteur Android actif.",
        "Playback stays in the music app. Keys control Android's active player."),
        11, Color.rgb(161, 172, 189));
    LinearLayout.LayoutParams footParams = new LinearLayout.LayoutParams(-1, -2);
    footParams.topMargin = dp(8);
    body.addView(foot, footParams);
    TextView close = radioText(game.t("FERMER  ×", "CLOSE  ×"), 13,
        Color.rgb(213, 177, 103));
    close.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    close.setMinHeight(dp(42));
    close.setClickable(true);
    close.setFocusable(true);
    close.setOnClickListener(v -> active[0].dismiss());
    body.addView(close);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(false);
    scroll.setVerticalScrollBarEnabled(false);
    scroll.addView(body);
    AlertDialog dialog = pauseLocalDialog(new AlertDialog.Builder(this).setView(scroll).create());
    active[0] = dialog;
    dialog.show();
    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - dp(28),
        android.view.WindowManager.LayoutParams.WRAP_CONTENT);
  }

  private int dp(float size) {
    return Math.round(size * getResources().getDisplayMetrics().density);
  }

  private TextView radioText(String value, int size, int color) {
    TextView view = new TextView(this);
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(color);
    view.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
    return view;
  }

  private TextView radioCard(String value, boolean selected, int height) {
    TextView card = radioText(value, height >= 60 ? 15 : 13,
        selected ? Color.rgb(18, 22, 34) : Color.rgb(255, 248, 230));
    card.setGravity(Gravity.CENTER);
    card.setMinHeight(dp(height));
    card.setClickable(true);
    card.setFocusable(true);
    GradientDrawable fill = new GradientDrawable();
    fill.setColor(selected ? Color.rgb(213, 177, 103) : Color.rgb(36, 44, 60));
    fill.setCornerRadius(dp(10));
    fill.setStroke(dp(1), selected ? Color.rgb(245, 215, 153) : Color.rgb(75, 88, 110));
    card.setBackground(fill);
    return card;
  }

  private void mediaKey(int key) {
    AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
    if (manager == null) return;
    manager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
    manager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
  }

  private String musicLink() {
    return getPreferences(MODE_PRIVATE).getString("musicLink" + musicProvider, "");
  }

  @Override
  public void toggleMusic() {
    audio.setEnabled(!audio.enabled());
    getPreferences(MODE_PRIVATE).edit().putBoolean("music", audio.enabled()).apply();
    view.invalidate();
  }

  @Override
  public void toggleEffects() {
    soundEffects = !soundEffects;
    getPreferences(MODE_PRIVATE).edit().putBoolean("soundEffects", soundEffects).apply();
    view.invalidate();
  }

  @Override
  public void toggleHaptics() {
    haptics = !haptics;
    getPreferences(MODE_PRIVATE).edit().putBoolean("haptics", haptics).apply();
    view.invalidate();
  }

  @Override
  public void changeMusicStyle() {
    int style = (audio.style() + 1) % 2;
    audio.setStyle(style);
    getPreferences(MODE_PRIVATE).edit().putInt("musicStyle", style).apply();
    view.invalidate();
  }

  @Override
  public void changeMusicVolume() {
    float next = audio.volume() < .35f ? .5f : audio.volume() < .75f ? 1f : .25f;
    audio.setVolume(next);
    getPreferences(MODE_PRIVATE).edit().putFloat("musicVolume", next).apply();
    view.invalidate();
  }

  public boolean musicEnabled() { return audio.enabled(); }
  public int musicStyle() { return audio.style(); }
  public float musicVolume() { return audio.volume(); }
  public boolean effectsEnabled() { return soundEffects; }
  public boolean hapticsEnabled() { return haptics; }
  public int musicProvider() { return musicProvider; }
  public boolean hasMusicLink() { return !musicLink().isEmpty(); }
  public String settingsOrigin() { return settingsOrigin; }

  @Override
  public void stats() {
    statsOrigin = game.screen;
    game.screen = "STATS";
    view.invalidate();
  }

  @Override
  public void home() {
    boolean fromSettings = "SETTINGS".equals(game.screen);
    game.screen = "STATS".equals(game.screen) ? statsOrigin
        : "SETTINGS".equals(game.screen) ? settingsOrigin
        : "GUIDE".equals(game.screen) ? guideOrigin : "HOME";
    if (fromSettings && !passPending && !network.connected
        && ("GAME".equals(game.screen) || "PREDICT".equals(game.screen)
            || "CREW".equals(game.screen) || "VOTE".equals(game.screen)
            || "RULE_PICK".equals(game.screen)
            || "RULE_VOTE".equals(game.screen))) {
      long paused = Math.max(0, System.currentTimeMillis() - settingsPausedAt);
      game.started += paused;
      if (game.deadline > 0) game.deadline += paused;
      if (game.reportDeadline > 0) game.reportDeadline += paused;
      if (game.revealUntil > 0) game.revealUntil += paused;
      save();
    }
    view.invalidate();
  }

  @Override
  public void message(String text) {
    status = text == null ? "" : text;
    Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    view.invalidate();
  }

  @Override
  public String status() {
    return status;
  }

  @Override
  public String hostAddress() {
    return network.hosting ? (network instanceof BluetoothPartyNetwork
        ? "Bluetooth • PIN " + network.roomCode()
        : network instanceof InternetPartyNetwork ? "Internet • CODE " + network.roomCode()
        : network.ip() + ":43867 • PIN " + network.roomCode()) : "";
  }

  @Override
  public boolean client() {
    return network.connected;
  }

  @Override
  public void remoteTouch(float x, float y, int kind, float height) {
    network.action(x, y, kind, height);
  }

  @Override
  public void click() {
    if (soundEffects) audio.click();
    haptic();
  }

  @Override
  public void tune() {
    audio.duck(4000);
    audio.tune(game.variant);
  }

  private void haptic() {
    if (!haptics) return;
    try {
      Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
      if (v != null)
        v.vibrate(VibrationEffect.createOneShot(24, VibrationEffect.DEFAULT_AMPLITUDE));
    } catch (Exception ignored) {
    }
  }

  @Override
  public void joined(String name, String language) {
    if ("LOBBY".equals(game.screen) && game.addPlayer(name, language, game.players.size() % 6)) {
      save();
      message(name + " joined");
    } else {
      network.reject(name);
      message("Room full or duplicate name");
    }
  }

  @Override
  public void action(String name, float x, float y, int kind, float height) {
    if ("GAME".equals(game.screen) && (game.game == 9 || game.game == 5 && game.drawingReady)) return;
    GameEngine.Player p = game.current();
    boolean drawingGuesser = "GAME".equals(game.screen) && game.game == 5
        && game.drawingReady && game.players.size() > 1
        && name.equals(game.players.get((game.active + 1) % game.players.size()).name);
    if (p == null || ("GAME".equals(game.screen) && game.game == 5 && game.drawingReady
        ? !drawingGuesser : !name.equals(p.name))) return;
    boolean bottom =
        "TRANSITION".equals(game.screen)
            || "HANDOFF".equals(game.screen)
            || "RESULT".equals(game.screen)
            || ("GAME".equals(game.screen)
                && (game.game == 1
                    || game.game == 8
                    || (game.game == 5 && !game.drawingReady && y > height - 190)));
    if (bottom) y += view.virtualHeight() - height;
    if ("GAME".equals(game.screen)
        || "BET".equals(game.screen)
        || "TRANSITION".equals(game.screen)
        || "HANDOFF".equals(game.screen)
        || "RESULT".equals(game.screen)) view.handleRemote(x, y, kind);
  }

  @Override
  public void command(String name, String command, int value) {
    if ("DRAW_GUESS".equals(command)) {
      if (game.drawGuess(name, value)) {
        if (game.drawGuessComplete()) finishGame(game.drawWin());
        else save();
      }
      return;
    }
    if ("CREW_PICK".equals(command)) {
      if (game.crewPick(name, value)) {
        if ("GAME".equals(game.screen)) beginGameAudio();
        save();
      }
      return;
    }
    if ("READY".equals(command)) {
      GameEngine.Player current = game.current();
      if (current != null && current.name.equals(name) && "HANDOFF".equals(game.screen)) {
        game.readyTurn();
        save();
      }
      return;
    }
    if ("REPORT".equals(command)) {
      if (value >= 0 && value < game.players.size()
          && game.reportRule(name, game.players.get(value).name)) save();
      return;
    }
    if ("RULE_VOTE".equals(command)) {
      if (game.castRuleVote(name, value == 1)) {
        if (!"RULE_VOTE".equals(game.screen)) finishRuleVote();
        else save();
      }
      return;
    }
    if ("PREDICT".equals(command)) {
      if (game.predict(name, value == 1)) {
        if ("GAME".equals(game.screen)) beginGameAudio();
        save();
      }
      return;
    }
    if ("JUDGE".equals(command)) {
      if (game.castJury(name, value == 1)) {
        if (game.juryComplete()) finishGame(game.juryVerdict());
        else save();
      }
      return;
    }
    if ("BLUFF_TRUTH".equals(command)) {
      if (value >= 0 && value <= 1 && game.chooseBluffTruth(name, value == 1)) {
        setPassPending(localJudge() != null);
        save();
      }
      return;
    }
    if ("REFLEX".equals(command)) {
      if (game.reflexTap(name, value)) {
        if (game.taps >= game.reflexGoal()) finishGame(true);
        else save();
      }
      return;
    }
    if ("RHYTHM".equals(command)) {
      if (game.rhythmTap(name, value, true)) {
        if (game.rhythmHits >= 4) finishGame(true);
        else save();
      }
      return;
    }
    if ("CUP".equals(command)) {
      GameEngine.Player player = game.current();
      if (player != null && player.name.equals(name) && game.selectCup(value)) save();
      return;
    }
    if ("BOMB".equals(command)) {
      if (game.bombTap(name)) {
        if (game.taps >= game.bombGoal()) finishGame(true);
        else save();
      }
      return;
    }
    if ("BOMB_PASS".equals(command)) {
      if (game.bombPass(name, value)) {
        setPassPending(localBomber() != null);
        save();
      }
      return;
    }
    if ("BOMB_CUT".equals(command)) {
      int outcome = game.bombCut(name, value);
      if (outcome >= 0) finishGame(outcome == 1);
      return;
    }
    if ("AVATAR".equals(command)) {
      int i = game.indexOf(name);
      if ("LOBBY".equals(game.screen) && i >= 0 && value >= 0 && value < 6) {
        for (int other = 0; other < game.players.size(); other++)
          if (other != i && game.players.get(other).avatar == value) return;
        game.players.get(i).avatar = value;
        game.players.get(i).photo = "";
        save();
      }
      return;
    }
    if ("HIDDEN_TAP".equals(command)) {
      if (game.hiddenTap(name, value)) save();
      return;
    }
    boolean changed = switch (command) {
      case "VOTE" -> game.castVote(name, value);
      case "RULE" -> game.chooseRule(name, value);
      default -> false;
    };
    if (changed) save();
  }

  @Override
  public void profile(String name, String photo) {
    int i = game.indexOf(name);
    if (!"LOBBY".equals(game.screen) || i < 0 || photo.length() > 60000) return;
    try {
      if (!photo.isEmpty()) {
        byte[] bytes = Base64.decode(photo, Base64.DEFAULT);
        if (bytes.length > 40000 || BitmapFactory.decodeByteArray(bytes, 0, bytes.length) == null) return;
      }
      game.players.get(i).photo = photo;
      save();
    } catch (Exception ignored) {
    }
  }

  @Override
  public void snapshot(JSONObject state) {
    long incoming = state.optLong("revision", 0);
    if (incoming < game.revision) return;
    String before = game.screen;
    String secretBefore = game.hiddenLastOwner;
    int secretKindBefore = game.hiddenLastKind;
    long sample = System.currentTimeMillis() - state.optLong("sentAt", System.currentTimeMillis());
    if (clockSkew == Long.MAX_VALUE || sample < clockSkew) clockSkew = sample;
    game.restore(state);
    if (network.localName.equals(game.hiddenLastOwner)
        && (!secretBefore.equals(game.hiddenLastOwner)
            || secretKindBefore != game.hiddenLastKind)) audio.win();
    if ("CREW".equals(before) && "GAME".equals(game.screen)) beginGameAudio();
    view.invalidate();
  }

  @Override
  public void status(String message) {
    message(message);
  }

  private EditText input(String hint) {
    EditText e = new EditText(this);
    e.setSingleLine(true);
    e.setHint(hint);
    e.setTextColor(Color.BLACK);
    e.setHintTextColor(Color.GRAY);
    e.setPadding(24, 12, 24, 12);
    return e;
  }

  private Spinner spinner(String[] values) {
    Spinner s = new Spinner(this);
    s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
    return s;
  }

  private LinearLayout column(View... views) {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(24, 12, 24, 12);
    for (View v : views) l.addView(v);
    return l;
  }

  @Override
  public void onBackPressed() {
    if ("SETTINGS".equals(game.screen) || "GUIDE".equals(game.screen)
        || "STATS".equals(game.screen)) {
      home();
    } else if (network.connected) {
      network.close();
      game.screen = "HOME";
      view.invalidate();
    } else if (!"HOME".equals(game.screen)) {
      showPartyMenu();
    } else super.onBackPressed();
  }
}
