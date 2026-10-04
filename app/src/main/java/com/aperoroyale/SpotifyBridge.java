package com.aperoroyale;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;

/** Spotify Web API via OAuth PKCE; no client secret or preview downloads. */
public final class SpotifyBridge {
  public interface Callback {
    void done(String error, BlindRound round);
  }

  public static final class BlindRound {
    public final String[] choices;
    public final int answer;
    public final String uri;

    BlindRound(String[] c, int a, String u) {
      choices = c;
      answer = a;
      uri = u;
    }
  }

  private static final String REDIRECT = "http://127.0.0.1:43868/callback";
  private final Context context;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final SecureRandom random = new SecureRandom();
  private String clientId = "",
      playlistId = "",
      token = "",
      refresh = "",
      state = "",
      verifier = "";
  private long expiry = 0;

  public SpotifyBridge(Context context) {
    this.context = context;
    clientId = context.getSharedPreferences("spotify", 0).getString("clientId", "");
    playlistId = context.getSharedPreferences("spotify", 0).getString("playlistId", "");
  }

  public void configure(String client, String playlist) {
    clientId = client.trim();
    playlistId = extractId(playlist);
    context
        .getSharedPreferences("spotify", 0)
        .edit()
        .putString("clientId", clientId)
        .putString("playlistId", playlistId)
        .apply();
  }

  public String clientId() {
    return clientId;
  }

  public String playlistId() {
    return playlistId;
  }

  public boolean ready() {
    return !clientId.isEmpty() && !playlistId.isEmpty();
  }

  public boolean connected() {
    return !token.isEmpty() && System.currentTimeMillis() < expiry;
  }

  public void connect(Callback callback) {
    if (clientId.isEmpty()) {
      callback.done("Spotify Client ID missing", null);
      return;
    }
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    verifier = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    random.nextBytes(bytes);
    state = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    new Thread(
            () -> {
              try (ServerSocket server =
                  new ServerSocket(43868, 1, InetAddress.getByName("127.0.0.1"))) {
                server.setSoTimeout(120000);
                String challenge =
                    Base64.encodeToString(
                        MessageDigest.getInstance("SHA-256")
                            .digest(verifier.getBytes(StandardCharsets.US_ASCII)),
                        Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
                String url =
                    "https://accounts.spotify.com/authorize?response_type=code&client_id="
                        + enc(clientId)
                        + "&redirect_uri="
                        + enc(REDIRECT)
                        + "&code_challenge_method=S256&code_challenge="
                        + enc(challenge)
                        + "&state="
                        + enc(state)
                        + "&scope="
                        + enc(
                            "playlist-read-private playlist-read-collaborative"
                                + " user-modify-playback-state user-read-playback-state");
                ui.post(
                    () ->
                        context.startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                try (Socket socket = server.accept()) {
                  BufferedReader input =
                      new BufferedReader(new InputStreamReader(socket.getInputStream()));
                  String request = input.readLine();
                  if (request == null) throw new Exception("No callback");
                  String html =
                      "<html><body style='background:#11152d;color:white;font:24px"
                          + " sans-serif;padding:40px'>Spotify connected.<br><br><a"
                          + " style='color:#42e5d0' href='aperoroyale://spotify-callback'>Return to"
                          + " Apéro Royale</a></body></html>";
                  byte[] response = html.getBytes(StandardCharsets.UTF_8);
                  OutputStream out = socket.getOutputStream();
                  out.write(
                      ("HTTP/1.1 200 OK\r\n"
                              + "Content-Type: text/html; charset=utf-8\r\n"
                              + "Content-Length: "
                              + response.length
                              + "\r\nConnection: close\r\n\r\n")
                          .getBytes(StandardCharsets.US_ASCII));
                  out.write(response);
                  out.flush();
                  String path = request.split(" ")[1];
                  Uri uri = Uri.parse("http://127.0.0.1" + path);
                  if (!state.equals(uri.getQueryParameter("state")))
                    throw new Exception("Invalid OAuth state");
                  String code = uri.getQueryParameter("code");
                  if (code == null) throw new Exception("Spotify authorization denied");
                  JSONObject data =
                      postForm(
                          "https://accounts.spotify.com/api/token",
                          "grant_type=authorization_code&code="
                              + enc(code)
                              + "&redirect_uri="
                              + enc(REDIRECT)
                              + "&client_id="
                              + enc(clientId)
                              + "&code_verifier="
                              + enc(verifier));
                  setTokens(data);
                  ui.post(() -> callback.done(null, null));
                }
              } catch (Exception e) {
                ui.post(() -> callback.done(e.getMessage(), null));
              }
            },
            "spotify-oauth")
        .start();
  }

  private void setTokens(JSONObject j) {
    token = j.optString("access_token", "");
    refresh = j.optString("refresh_token", refresh);
    expiry = System.currentTimeMillis() + j.optLong("expires_in", 3600) * 1000 - 30000;
  }

  private void ensureToken() throws Exception {
    if (connected()) return;
    if (refresh.isEmpty()) throw new Exception("Connect Spotify first");
    setTokens(
        postForm(
            "https://accounts.spotify.com/api/token",
            "grant_type=refresh_token&refresh_token="
                + enc(refresh)
                + "&client_id="
                + enc(clientId)));
  }

  public void radio(Callback callback) {
    new Thread(
            () -> {
              try {
                ensureToken();
                JSONObject body = new JSONObject();
                body.put("context_uri", "spotify:playlist:" + playlistId);
                api("PUT", "https://api.spotify.com/v1/me/player/play", body);
                ui.post(() -> callback.done(null, null));
              } catch (Exception e) {
                ui.post(() -> callback.done(e.getMessage(), null));
              }
            },
            "spotify-radio")
        .start();
  }

  public void pause(Callback callback) {
    new Thread(() -> {
      try {
        ensureToken();
        api("PUT", "https://api.spotify.com/v1/me/player/pause", null);
        ui.post(() -> callback.done(null, null));
      } catch (Exception e) {
        ui.post(() -> callback.done(e.getMessage(), null));
      }
    }, "spotify-pause").start();
  }

  public void replay(String uri, Callback callback) {
    new Thread(
            () -> {
              try {
                ensureToken();
                JSONObject body = new JSONObject();
                JSONArray uris = new JSONArray();
                uris.put(uri);
                body.put("uris", uris);
                api("PUT", "https://api.spotify.com/v1/me/player/play", body);
                ui.post(() -> callback.done(null, null));
              } catch (Exception e) {
                ui.post(() -> callback.done(e.getMessage(), null));
              }
            },
            "spotify-replay")
        .start();
  }

  public void blind(Callback callback) {
    new Thread(
            () -> {
              try {
                ensureToken();
                JSONObject result =
                    api(
                        "GET",
                        "https://api.spotify.com/v1/playlists/"
                            + enc(playlistId)
                            + "/items?limit=50",
                        null);
                JSONArray items = result.optJSONArray("items");
                ArrayList<String[]> songs = new ArrayList<>();
                if (items != null)
                  for (int i = 0; i < items.length(); i++) {
                    JSONObject row = items.optJSONObject(i);
                    if (row == null) continue;
                    JSONObject track = row.optJSONObject("item");
                    if (track == null) track = row.optJSONObject("track");
                    if (track != null
                        && track.optString("uri").startsWith("spotify:track:")
                        && !track.optString("name").isEmpty())
                      songs.add(new String[] {track.optString("name"), track.optString("uri")});
                  }
                if (songs.size() < 4) throw new Exception("Playlist needs at least 4 tracks");
                Collections.shuffle(songs, random);
                String[] choices = new String[4];
                for (int i = 0; i < 4; i++) choices[i] = songs.get(i)[0];
                JSONObject body = new JSONObject();
                JSONArray uris = new JSONArray();
                uris.put(songs.get(0)[1]);
                body.put("uris", uris);
                api("PUT", "https://api.spotify.com/v1/me/player/play", body);
                BlindRound round = new BlindRound(choices, 0, songs.get(0)[1]);
                ui.post(() -> callback.done(null, round));
              } catch (Exception e) {
                ui.post(() -> callback.done(e.getMessage(), null));
              }
            },
            "spotify-blind")
        .start();
  }

  private JSONObject api(String method, String path, JSONObject body) throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL(path).openConnection();
    c.setConnectTimeout(9000);
    c.setReadTimeout(9000);
    c.setRequestMethod(method);
    c.setRequestProperty("Authorization", "Bearer " + token);
    if (body != null) {
      c.setDoOutput(true);
      c.setRequestProperty("Content-Type", "application/json");
      c.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
    }
    int code = c.getResponseCode();
    if (code >= 300) throw new Exception("Spotify HTTP " + code);
    if (code == 204) return new JSONObject();
    return new JSONObject(new String(read(c.getInputStream()), StandardCharsets.UTF_8));
  }

  private JSONObject postForm(String path, String form) throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL(path).openConnection();
    c.setConnectTimeout(9000);
    c.setReadTimeout(9000);
    c.setRequestMethod("POST");
    c.setDoOutput(true);
    c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    c.getOutputStream().write(form.getBytes(StandardCharsets.UTF_8));
    if (c.getResponseCode() >= 300)
      throw new Exception("Spotify token HTTP " + c.getResponseCode());
    return new JSONObject(new String(read(c.getInputStream()), StandardCharsets.UTF_8));
  }

  private static byte[] read(InputStream in) throws Exception {
    try (InputStream stream = in;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      byte[] buf = new byte[4096];
      int n;
      while ((n = stream.read(buf)) != -1) bytes.write(buf, 0, n);
      return bytes.toByteArray();
    }
  }

  private static String enc(String s) throws Exception {
    return URLEncoder.encode(s, "UTF-8");
  }

  private static String extractId(String s) {
    s = s.trim();
    int q = s.indexOf('?');
    if (q >= 0) s = s.substring(0, q);
    int slash = s.lastIndexOf('/');
    if (slash >= 0) s = s.substring(slash + 1);
    int colon = s.lastIndexOf(':');
    if (colon >= 0) s = s.substring(colon + 1);
    return s;
  }
}
