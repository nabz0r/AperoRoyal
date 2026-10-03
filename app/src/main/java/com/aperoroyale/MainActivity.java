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
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.util.Base64;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
  SpotifyBridge spotify;
  ArcadeView view;
  private boolean savedSession = false;
  private String status = "";
  private int photoPlayer = -1;
  private String statsOrigin = "HOME";
  private String settingsOrigin = "HOME";
  private long settingsPausedAt = 0;
  private String guideOrigin = "HOME";
  private boolean passPending = false, soundEffects = true, haptics = true;
  private long clockSkew = Long.MAX_VALUE;
  private Runnable pendingBluetooth;

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
    if (savedSession) game.screen = "HOME";
    audio = new ArcadeAudio();
    audio.setEnabled(getPreferences(MODE_PRIVATE).getBoolean("music", false));
    audio.setStyle(getPreferences(MODE_PRIVATE).getInt("musicStyle", 0));
    audio.setVolume(getPreferences(MODE_PRIVATE).getFloat("musicVolume", .5f));
    soundEffects = getPreferences(MODE_PRIVATE).getBoolean("soundEffects", true);
    haptics = getPreferences(MODE_PRIVATE).getBoolean("haptics", true);
    network = new PartyNetwork(this);
    spotify = new SpotifyBridge(this);
    view = new ArcadeView(this, game, this);
    setContentView(view);
    audio.start();
    view.postDelayed(this::clockTick, 100);
  }

  private void clockTick() {
    if (isFinishing()) return;
    audio.setScene(game.screen);
    if (!network.connected && game.predictionTimedOut()) {
      beginGameAudio();
      save();
    }
    if (!network.connected && game.ruleVoteTimedOut()) finishRuleVote();
    if (!network.connected && "GAME".equals(game.screen) && game.game == 4
        && game.chosenCup >= 0 && System.currentTimeMillis() >= game.revealUntil)
      finishGame(game.cupIsSafe());
    if (!network.connected
        && "GAME".equals(game.screen)
        && game.deadline > 0
        && System.currentTimeMillis() > game.deadline) {
      finishGame(game.juryPhase ? game.juryVerdict()
          : (game.game == 3 && game.taps >= 10) || (game.game == 7 && game.rhythmHits >= 4));
    }
    view.invalidate();
    view.postDelayed(this::clockTick, 100);
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
        && !game.strokes.isEmpty())) network.broadcast(game.networkJson());
    view.invalidate();
  }

  @Override
  public void finishGame(boolean won) {
    if (!"GAME".equals(game.screen)) return;
    GameEngine.Player p = game.current();
    long duration = Math.max(0, System.currentTimeMillis() - game.started);
    game.finish(won);
    if (p != null) store.record(p, game.game, won,
        game.roundPoints, game.roundSips > 0 ? 1 : 0, game.roundSips, duration);
    for (int i = 0; i < game.players.size() && i < game.predictions.length; i++) {
      if (i == game.active || game.predictions[i] < 0) continue;
      boolean correct = (game.predictions[i] == 1) == won;
      store.recordPrediction(game.players.get(i), game.game, correct,
          correct || game.ruleId == 1 ? 0 : 1);
    }
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
    save();
  }

  @Override
  public void placeBet(int sips) {
    if (!game.placeBet(sips)) return;
    save();
  }

  public GameEngine.Player localPredictor() {
    if (!"PREDICT".equals(game.screen)) return null;
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i >= 0 && i != game.active && i < game.predictions.length
          && game.predictions[i] < 0 ? game.players.get(i) : null;
    }
    for (int i = 0; i < game.players.size() && i < game.predictions.length; i++)
      if (i != game.active && game.predictions[i] < 0
          && !network.isRemote(game.players.get(i).name)) return game.players.get(i);
    return null;
  }

  @Override
  public void predict(boolean win) {
    GameEngine.Player voter = localPredictor();
    if (voter == null) return;
    if (network.connected) { network.command("PREDICT", win ? 1 : 0); return; }
    if (game.predict(voter.name, win)) {
      if ("GAME".equals(game.screen)) beginGameAudio();
      save();
    }
  }

  @Override
  public void beginJury() {
    if (game.beginJury()) save();
  }

  public GameEngine.Player localJudge() {
    if (!game.juryPhase || !"GAME".equals(game.screen)) return null;
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i >= 0 && i != game.active && i < game.juryVotes.length
          && game.juryVotes[i] < 0 ? game.players.get(i) : null;
    }
    for (int i = 0; i < game.players.size() && i < game.juryVotes.length; i++)
      if (i != game.active && game.juryVotes[i] < 0
          && !network.isRemote(game.players.get(i).name)) return game.players.get(i);
    return null;
  }

  @Override
  public void judge(boolean yes) {
    GameEngine.Player juror = localJudge();
    if (juror == null) return;
    if (network.connected) { network.command("JUDGE", yes ? 1 : 0); return; }
    if (game.castJury(juror.name, yes)) {
      if (game.juryComplete()) finishGame(game.juryVerdict());
      else save();
    }
  }

  @Override
  public void reflexTap(int expectedStep) {
    if (network.connected) { network.command("REFLEX", expectedStep); return; }
    GameEngine.Player player = game.current();
    if (player != null && game.reflexTap(player.name, expectedStep)) {
      if (game.taps >= 10) finishGame(true);
      else save();
    }
  }

  @Override
  public void rhythmTap(int beat) {
    if (network.connected) { network.command("RHYTHM", beat); return; }
    GameEngine.Player player = game.current();
    if (player != null && game.rhythmTap(player.name, beat)) {
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
          else if (game.reportRule(reporter.name, game.players.get(target).name)) save();
        })
        .show();
  }

  public GameEngine.Player localRuleVoter() {
    if (!"RULE_VOTE".equals(game.screen)) return null;
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i >= 0 && i < game.reportVotes.length && game.reportVotes[i] < 0
          ? game.players.get(i) : null;
    }
    for (int i = 0; i < game.players.size() && i < game.reportVotes.length; i++)
      if (game.reportVotes[i] < 0 && !network.isRemote(game.players.get(i).name))
        return game.players.get(i);
    return null;
  }

  @Override
  public void ruleVote(boolean yes) {
    GameEngine.Player voter = localRuleVoter();
    if (voter == null) return;
    if (network.connected) { network.command("RULE_VOTE", yes ? 1 : 0); return; }
    if (game.castRuleVote(voter.name, yes)) {
      if (!"RULE_VOTE".equals(game.screen)) finishRuleVote();
      else save();
    }
  }

  private void finishRuleVote() {
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
    if (game.game == 2) {
      audio.duck(14000);
      if (spotify.ready() && spotify.connected()) {
        game.note = "loading";
        spotify.blind(
            (error, round) -> {
              if (round != null) {
                try {
                  JSONObject j = new JSONObject();
                  JSONArray choices = new JSONArray();
                  for (String c : round.choices) choices.put(c);
                  j.put("choices", choices);
                  j.put("answer", round.answer);
                  j.put("uri", round.uri);
                  game.note = j.toString();
                } catch (Exception ignored) {
                }
              } else {
                game.note = "";
                message(error == null ? "Spotify unavailable" : error);
                audio.tune(game.variant);
              }
              save();
            });
      } else audio.tune(game.variant);
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
    if (game.taps >= 8) finishGame(true);
    else save();
  }

  public boolean passPending() { return passPending; }

  @Override
  public void confirmPass() { passPending = false; view.invalidate(); }

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
        else save();
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
    new AlertDialog.Builder(this)
        .setTitle(player.name)
        .setItems(new String[] {
            game.t("Importer une photo", "Import a photo"),
            game.t("Changer de sprite", "Change sprite"),
            game.t("Supprimer la photo", "Remove photo")
        }, (d, which) -> {
          if (which == 0) {
            openPhotoPicker(index);
          } else if (which == 1) {
            chooseAvatar(index);
          } else {
            player.photo = "";
            if (network.connected) network.profile("");
            else save();
          }
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
      else save();
    } catch (Exception e) {
      message(game.t("Image illisible", "Could not read image"));
    }
  }

  public GameEngine.Player localVoter() {
    if (network.connected) {
      int i = game.indexOf(network.localName);
      return i < 0 || (game.votes.length > i && game.votes[i] >= 0) ? null : game.players.get(i);
    }
    for (int i = 0; i < game.players.size(); i++)
      if (!network.isRemote(game.players.get(i).name)
          && (game.votes.length <= i || game.votes[i] < 0)) return game.players.get(i);
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
        passPending = "VOTE".equals(game.screen) && localVoter() != null;
        save();
      }
    }
  }

  @Override
  public void catTap() {
    if (network.connected) network.command("CAT", 0);
    else {
      GameEngine.Player p = localParticipant();
      if (p != null && game.catTap(p.name)) {
        if (!game.ruleOwner.isEmpty()) audio.win();
        save();
      }
    }
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
    passPending = false;
    clockSkew = Long.MAX_VALUE;
    game.newParty();
    save();
  }

  @Override
  public void resumeParty() {
    if (store.load(game)) {
      if ("HOME".equals(game.screen)) game.screen = "LOBBY";
      if ("GAME".equals(game.screen)) game.resumeGame();
      save();
    } else message("No saved party / Aucune partie");
  }

  @Override
  public boolean hasSavedParty() {
    return savedSession;
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
  public void spotifySettings() {
    EditText id = input("Spotify Client ID");
    id.setText(spotify.clientId());
    EditText playlist = input("Spotify playlist URL or ID");
    playlist.setText(spotify.playlistId());
    String title = game.t("Spotify + musique arcade", "Spotify + Arcade Music");
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setView(column(id, playlist))
        .setPositiveButton(
            game.t("Enregistrer", "Save"),
            (d, w) -> {
              spotify.configure(id.getText().toString(), playlist.getText().toString());
              message("Spotify settings saved");
            })
        .setNeutralButton(
            game.t("Connecter", "Connect"),
            (d, w) -> {
              spotify.configure(id.getText().toString(), playlist.getText().toString());
              spotify.connect(
                  (error, round) -> message(error == null ? "Spotify connected" : error));
            })
        .setNegativeButton(game.t("Fermer", "Close"), null)
        .show();
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

  @Override
  public void radio() {
    if (!spotify.ready()) {
      message("Configure Spotify in settings");
      return;
    }
    spotify.radio((error, round) -> message(error == null ? "Radio Apéro playing" : error));
  }

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
    if (fromSettings && "GAME".equals(game.screen) && !network.connected) {
      long paused = Math.max(0, System.currentTimeMillis() - settingsPausedAt);
      game.started += paused;
      if (game.deadline > 0) game.deadline += paused;
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
    try {
      String uri = new JSONObject(game.note).optString("uri");
      if (!uri.isEmpty()) {
        spotify.replay(
            uri,
            (error, round) -> {
              if (error != null) message(error);
            });
        return;
      }
    } catch (Exception ignored) {
    }
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
    if ("GAME".equals(game.screen) && game.game == 9) return;
    GameEngine.Player p = game.current();
    boolean drawingGuesser = "GAME".equals(game.screen) && game.game == 5
        && game.drawingReady && game.players.size() > 1
        && name.equals(game.players.get((game.active + 1) % game.players.size()).name);
    if (p == null || ("GAME".equals(game.screen) && game.game == 5 && game.drawingReady
        ? !drawingGuesser : !name.equals(p.name))) return;
    boolean bottom =
        "TRANSITION".equals(game.screen)
            || "RESULT".equals(game.screen)
            || ("GAME".equals(game.screen)
                && (game.game == 1
                    || game.game == 8
                    || (game.game == 5 && !game.drawingReady && y > height - 190)));
    if (bottom) y += view.virtualHeight() - height;
    if ("GAME".equals(game.screen)
        || "BET".equals(game.screen)
        || "TRANSITION".equals(game.screen)
        || "RESULT".equals(game.screen)) view.handleRemote(x, y, kind);
  }

  @Override
  public void command(String name, String command, int value) {
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
    if ("REFLEX".equals(command)) {
      if (game.reflexTap(name, value)) {
        if (game.taps >= 10) finishGame(true);
        else save();
      }
      return;
    }
    if ("RHYTHM".equals(command)) {
      if (game.rhythmTap(name, value)) {
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
        if (game.taps >= 8) finishGame(true);
        else save();
      }
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
    boolean changed = switch (command) {
      case "VOTE" -> game.castVote(name, value);
      case "CAT" -> game.catTap(name);
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
    long sample = System.currentTimeMillis() - state.optLong("sentAt", System.currentTimeMillis());
    if (clockSkew == Long.MAX_VALUE || sample < clockSkew) clockSkew = sample;
    game.restore(state);
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
      game.screen = "HOME";
      view.invalidate();
    } else super.onBackPressed();
  }
}
