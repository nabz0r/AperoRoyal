package com.aperoroyale;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

/** Internet room over TLS MQTT with room-code-derived AES-GCM message encryption. */
public final class InternetPartyNetwork extends PartyNetwork {
  public static final String DEFAULT_RELAY = "ssl://broker.emqx.io:8883";
  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private final Events events;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final ExecutorService writes = Executors.newSingleThreadExecutor();
  private final SecureRandom random = new SecureRandom();
  private final HashMap<String, String> names = new HashMap<>();
  private MqttClient client;
  private String code = "", topic = "", sender = "", language = "FR";
  private SecretKeySpec key;
  private volatile int generation = 0;
  private volatile boolean hasSnapshot = false;
  private volatile JSONObject lastState;
  private volatile GameEngine lastGame;

  public InternetPartyNetwork(Events events) {
    super(events);
    this.events = events;
  }

  @Override public String ip() { return "Internet"; }
  @Override public String roomCode() { return code; }

  public void hostInternet(String relay) {
    byte[] bits = new byte[12];
    random.nextBytes(bits);
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < 12; i++) result.append(ALPHABET.charAt((bits[i] & 255) % ALPHABET.length()));
    open(relay, result.toString(), true, "", "FR");
  }

  public void joinInternet(String relay, String roomCode, String name, String playerLanguage) {
    String normalized = roomCode.trim().toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
    if (normalized.length() != 12 || !normalized.matches("[A-HJ-NP-Z2-9]{12}")) {
      ui.post(() -> events.status("Invalid 12-character room code"));
      return;
    }
    open(relay, normalized, false, name.trim(), playerLanguage);
  }

  private void open(String relay, String roomCode, boolean host, String name, String playerLanguage) {
    close();
    hosting = host;
    localName = name;
    localLanguage = playerLanguage;
    language = playerLanguage;
    code = roomCode;
    sender = randomHex(12);
    hasSnapshot = false;
    lastState = null;
    try {
      byte[] digest = sha256(("apero-royale-v101:" + code).getBytes(StandardCharsets.UTF_8));
      key = new SecretKeySpec(digest, "AES");
      topic = "aperoroyale/v101/" + hex(sha256(code.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      ui.post(() -> events.status("Room encryption error"));
      return;
    }
    int epoch = generation;
    String uri = relay == null || relay.isEmpty() ? DEFAULT_RELAY : relay.trim();
    if (!uri.startsWith("ssl://")) {
      ui.post(() -> events.status("Relay URL must use ssl://"));
      return;
    }
    new Thread(() -> {
      try {
        MqttClient mqtt = new MqttClient(uri, "ar-" + sender, new MemoryPersistence());
        client = mqtt;
        mqtt.setCallback(new MqttCallbackExtended() {
          @Override public void connectComplete(boolean reconnect, String serverURI) {
            if (epoch != generation) return;
            Log.i("AperoInternet", "Connected: host=" + host + " reconnect=" + reconnect);
            try {
              mqtt.subscribe(topic, 1);
              Log.i("AperoInternet", "Subscribed");
              if (!host) {
                connected = true;
                hasSnapshot = false;
                sendJoin();
                retryJoin(epoch, 1);
              }
              ui.post(() -> events.status(host ? "INTERNET • CODE " + code
                  : (reconnect ? "INTERNET RECONNECTED" : "INTERNET CONNECTED")));
            } catch (Exception e) {
              Log.e("AperoInternet", "Subscribe failed", e);
              ui.post(() -> events.status("Internet subscribe: " + e.getMessage()));
            }
          }

          @Override public void connectionLost(Throwable cause) {
            if (epoch == generation) {
              if (!host) connected = false;
              ui.post(() -> events.status("Internet connection lost"));
            }
          }

          @Override public void messageArrived(String receivedTopic, MqttMessage message) {
            if (epoch != generation || message.getPayload().length > 1000000) return;
            try { receive(decrypt(message.getPayload()), host); }
            catch (Exception ignored) { }
          }

          @Override public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) { }
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        for (int attempt = 1; epoch == generation && attempt <= 6; attempt++) {
          try {
            mqtt.connect(options);
            return;
          } catch (Exception e) {
            Log.w("AperoInternet", "Connect attempt " + attempt + " failed", e);
            if (epoch != generation) return;
            final int retry = attempt;
            ui.post(() -> events.status("Internet relay unavailable • retry " + retry + "/6"));
            if (attempt == 6) {
              ui.post(() -> events.status("Internet unavailable. Check relay URL or try Wi-Fi."));
              return;
            }
            try { Thread.sleep(Math.min(8000, 1000L << (attempt - 1))); }
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return; }
          }
        }
      } catch (Exception e) {
        Log.e("AperoInternet", "Connection failed", e);
        if (epoch == generation) ui.post(() -> events.status("Internet: " + e.getMessage()));
      }
    }, "party-internet").start();
  }

  private void sendJoin() {
    try {
      JSONObject message = base("JOIN");
      message.put("name", localName);
      message.put("language", language);
      send(message);
    } catch (Exception ignored) { }
  }

  private void retryJoin(int epoch, int attempt) {
    ui.postDelayed(() -> {
      if (epoch != generation || !connected || hasSnapshot) return;
      if (attempt >= 10) {
        events.status("Room not found. Check the code and host connection.");
        return;
      }
      sendJoin();
      retryJoin(epoch, attempt + 1);
    }, 3000);
  }

  private JSONObject base(String type) throws Exception {
    JSONObject message = new JSONObject();
    message.put("type", type);
    message.put("sender", sender);
    return message;
  }

  private void receive(JSONObject message, boolean host) {
    String from = message.optString("sender");
    if (from.isEmpty() || from.equals(sender)) return;
    String type = message.optString("type");
    Log.i("AperoInternet", "Received " + type + " as host=" + host);
    if (host) {
      if ("JOIN".equals(type)) {
        String name = message.optString("name").trim();
        String lang = message.optString("language", "FR");
        if (name.isEmpty() || name.length() > 16) return;
        synchronized (names) {
          if (names.containsKey(from)) {
            GameEngine current = lastGame;
            if (current != null) ui.post(() -> sendState(from, current.networkJsonFor(name)));
            return;
          }
          names.put(from, name);
        }
        ui.post(() -> events.joined(name, lang));
        return;
      }
      String name;
      synchronized (names) { name = names.get(from); }
      if (name == null) return;
      if ("ACTION".equals(type)) {
        float x = (float) message.optDouble("x");
        float y = (float) message.optDouble("y");
        int kind = message.optInt("kind");
        float height = (float) message.optDouble("height", 800);
        ui.post(() -> events.action(name, x, y, kind, height));
      } else if ("COMMAND".equals(type)) {
        String command = message.optString("command");
        int value = message.optInt("value", -1);
        ui.post(() -> events.command(name, command, value));
      } else if ("PROFILE".equals(type)) {
        String photo = message.optString("photo", "");
        if (photo.length() <= 60000) ui.post(() -> events.profile(name, photo));
      } else if ("LEAVE".equals(type)) {
        synchronized (names) { names.remove(from); }
      }
    } else if ("STATE".equals(type) && sender.equals(message.optString("to"))) {
      JSONObject state = message.optJSONObject("state");
      if (state != null) {
        hasSnapshot = true;
        ui.post(() -> events.snapshot(state));
      }
    } else if ("ERROR".equals(type) && sender.equals(message.optString("to"))) {
      ui.post(() -> events.status(message.optString("message")));
    }
  }

  @Override public void reject(String name) {
    String id = "";
    synchronized (names) {
      for (java.util.Map.Entry<String, String> entry : names.entrySet())
        if (name.equals(entry.getValue())) { id = entry.getKey(); break; }
      if (!id.isEmpty()) names.remove(id);
    }
    if (id.isEmpty()) return;
    try {
      JSONObject error = base("ERROR");
      error.put("to", id);
      error.put("message", "Room full or duplicate name");
      send(error);
    } catch (Exception ignored) { }
  }

  @Override public boolean isRemote(String name) {
    synchronized (names) { return names.containsValue(name); }
  }

  @Override public void action(float x, float y, int kind, float height) {
    try {
      JSONObject message = base("ACTION");
      message.put("x", x);
      message.put("y", y);
      message.put("kind", kind);
      message.put("height", height);
      send(message);
    } catch (Exception ignored) { }
  }

  @Override public void command(String command, int value) {
    try {
      JSONObject message = base("COMMAND");
      message.put("command", command);
      message.put("value", value);
      send(message);
    } catch (Exception ignored) { }
  }

  @Override public void profile(String photo) {
    if (photo.length() > 60000) return;
    try {
      JSONObject message = base("PROFILE");
      message.put("photo", photo);
      send(message);
    } catch (Exception ignored) { }
  }

  @Override public void broadcast(JSONObject state) {
    if (!hosting) return;
    lastState = state;
    try {
      JSONObject message = base("STATE");
      message.put("state", state);
      send(message);
    } catch (Exception ignored) { }
  }

  @Override public void broadcast(GameEngine game) {
    if (!hosting) return;
    lastGame = game;
    HashMap<String, String> recipients;
    synchronized (names) { recipients = new HashMap<>(names); }
    for (java.util.Map.Entry<String, String> entry : recipients.entrySet())
      sendState(entry.getKey(), game.networkJsonFor(entry.getValue()));
  }

  private void sendState(String to, JSONObject state) {
    try {
      JSONObject message = base("STATE");
      message.put("to", to);
      message.put("state", state);
      send(message);
    } catch (Exception ignored) { }
  }

  private void send(JSONObject message) {
    if (!hosting && !connected) return;
    writes.execute(() -> {
      try {
        MqttClient mqtt = client;
        if (mqtt != null && mqtt.isConnected()) mqtt.publish(topic, encrypt(message).getBytes(StandardCharsets.UTF_8), 1, false);
      } catch (Exception ignored) { }
    });
  }

  private String encrypt(JSONObject message) throws Exception {
    byte[] nonce = new byte[12];
    random.nextBytes(nonce);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
    byte[] encoded = cipher.doFinal(message.toString().getBytes(StandardCharsets.UTF_8));
    byte[] packet = new byte[nonce.length + encoded.length];
    System.arraycopy(nonce, 0, packet, 0, nonce.length);
    System.arraycopy(encoded, 0, packet, nonce.length, encoded.length);
    return Base64.encodeToString(packet, Base64.NO_WRAP);
  }

  private JSONObject decrypt(byte[] payload) throws Exception {
    byte[] packet = Base64.decode(payload, Base64.DEFAULT);
    if (packet.length < 29) throw new IllegalArgumentException("Short packet");
    byte[] nonce = java.util.Arrays.copyOfRange(packet, 0, 12);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
    byte[] clear = cipher.doFinal(packet, 12, packet.length - 12);
    return new JSONObject(new String(clear, StandardCharsets.UTF_8));
  }

  private String randomHex(int bytes) {
    byte[] value = new byte[bytes];
    random.nextBytes(value);
    return hex(value);
  }

  private static byte[] sha256(byte[] bytes) throws Exception {
    return MessageDigest.getInstance("SHA-256").digest(bytes);
  }

  private static String hex(byte[] bytes) {
    StringBuilder result = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) result.append(String.format(Locale.ROOT, "%02x", b & 255));
    return result.toString();
  }

  @Override public void close() {
    generation++;
    if (connected && !hosting) {
      try { send(base("LEAVE")); } catch (Exception ignored) { }
    }
    hosting = connected = false;
    MqttClient mqtt = client;
    client = null;
    if (mqtt != null) new Thread(() -> {
      try { mqtt.disconnectForcibly(500, 500, false); } catch (Exception ignored) { }
      try { mqtt.close(); } catch (Exception ignored) { }
    }, "party-internet-close").start();
    synchronized (names) { names.clear(); }
    lastGame = null;
    super.close();
  }
}
