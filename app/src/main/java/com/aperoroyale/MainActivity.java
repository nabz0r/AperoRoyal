package com.aperoroyale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

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
    network = new PartyNetwork(this);
    spotify = new SpotifyBridge(this);
    view = new ArcadeView(this, game, this);
    setContentView(view);
    audio.start();
    view.postDelayed(this::clockTick, 100);
  }

  private void clockTick() {
    if (isFinishing()) return;
    if (!network.connected
        && "GAME".equals(game.screen)
        && game.deadline > 0
        && System.currentTimeMillis() > game.deadline) {
      finishGame((game.game == 3 && game.taps >= 10) || (game.game == 7 && game.rhythmHits >= 4));
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
    store.save(game);
    savedSession = true;
    network.broadcast(game.json());
    view.invalidate();
  }

  @Override
  public void finishGame(boolean won) {
    if (!"GAME".equals(game.screen)) return;
    GameEngine.Player p = game.current();
    game.finish(won);
    if (p != null) store.record(p, game.game, won);
    if (won) audio.win();
    else audio.lose();
    haptic();
    save();
  }

  @Override
  public void enterGame() {
    game.enterGame();
    if (game.game == 2) {
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
    save();
  }

  @Override
  public void addPlayer() {
    EditText name = input("Pseudo / Nickname");
    Spinner language = spinner(new String[] {"FR", "EN"});
    Spinner avatar = spinner(new String[] {"CAT", "FROG", "DUCK", "ALIEN", "ROBOT", "DISCO"});
    LinearLayout box = column(name, language, avatar);
    new AlertDialog.Builder(this)
        .setTitle(game.t("Nouveau joueur", "New player"))
        .setView(box)
        .setNegativeButton(game.t("Annuler", "Cancel"), null)
        .setPositiveButton(
            "OK",
            (d, w) -> {
              if (game.addPlayer(
                  name.getText().toString(),
                  language.getSelectedItem().toString(),
                  avatar.getSelectedItemPosition())) save();
              else message("Name unavailable / Nom indisponible");
            })
        .show();
  }

  @Override
  public void newParty() {
    network.close();
    game.newParty();
    save();
  }

  @Override
  public void resumeParty() {
    if (store.load(game)) {
      if ("HOME".equals(game.screen)) game.screen = "LOBBY";
      if ("GAME".equals(game.screen)) game.enterGame();
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
    network.host();
    save();
    message("Wi-Fi room: " + network.ip() + ":43867");
  }

  @Override
  public void joinRoom() {
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
              network.join(address, n, lang.getSelectedItem().toString(), code);
            })
        .show();
  }

  @Override
  public void showSettings() {
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
        .setNegativeButton(
            game.t("Musique OUI/NON", "Music ON/OFF"),
            (d, w) -> {
              audio.setEnabled(!audio.enabled());
              message(audio.enabled() ? "Arcade music ON" : "Arcade music OFF");
            })
        .show();
  }

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
    game.screen = "STATS";
    view.invalidate();
  }

  @Override
  public void home() {
    game.screen = "HOME";
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
    return network.hosting ? network.ip() + ":43867 • PIN " + network.roomCode() : "";
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
    audio.click();
    haptic();
  }

  @Override
  public void tune() {
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
    GameEngine.Player p = game.current();
    if (p == null || !name.equals(p.name)) return;
    boolean bottom =
        "TRANSITION".equals(game.screen)
            || "RESULT".equals(game.screen)
            || ("GAME".equals(game.screen)
                && (game.game == 1
                    || game.game == 8
                    || (game.game == 5 && !game.drawingReady && y > height - 190)));
    if (bottom) y += view.virtualHeight() - height;
    if ("GAME".equals(game.screen)
        || "TRANSITION".equals(game.screen)
        || "RESULT".equals(game.screen)) view.handleRemote(x, y, kind);
  }

  @Override
  public void snapshot(JSONObject state) {
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
    if (network.connected) {
      network.close();
      game.screen = "HOME";
      view.invalidate();
    } else if (!"HOME".equals(game.screen)) {
      game.screen = "HOME";
      view.invalidate();
    } else super.onBackPressed();
  }
}
