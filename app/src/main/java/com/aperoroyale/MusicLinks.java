package com.aperoroyale;

import java.net.URI;
import java.util.Locale;

/** Provider-neutral shortcuts: the music app owns playback and account access. */
public final class MusicLinks {
  private MusicLinks() { }

  public static final int ORIGINAL = 0, SPOTIFY = 1, DEEZER = 2, APPLE = 3,
      AMAZON = 4, SILENT = 5;
  public static final int COUNT = 6;

  public static String name(int provider) {
    return switch (provider) {
      case SPOTIFY -> "Spotify";
      case DEEZER -> "Deezer";
      case APPLE -> "Apple Music";
      case AMAZON -> "Amazon Music";
      case SILENT -> "Silence";
      default -> "Apéro Royale";
    };
  }

  public static String home(int provider) {
    return switch (provider) {
      case SPOTIFY -> "https://open.spotify.com/";
      case DEEZER -> "https://www.deezer.com/";
      case APPLE -> "https://music.apple.com/";
      case AMAZON -> "https://music.amazon.com/";
      default -> "";
    };
  }

  public static boolean external(int provider) {
    return provider >= SPOTIFY && provider <= AMAZON;
  }

  public static boolean allowed(int provider, String raw) {
    if (!external(provider) || raw == null || raw.trim().isEmpty()) return false;
    try {
      URI uri = new URI(raw.trim());
      if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
          || uri.getPort() != -1 || uri.getHost() == null) return false;
      String host = uri.getHost().toLowerCase(Locale.ROOT);
      return switch (provider) {
        case SPOTIFY -> host.equals("open.spotify.com");
        case DEEZER -> host.equals("deezer.com") || host.equals("www.deezer.com");
        case APPLE -> host.equals("music.apple.com");
        case AMAZON -> host.equals("music.amazon.com") || host.equals("music.amazon.fr")
            || host.equals("music.amazon.de") || host.equals("music.amazon.co.uk");
        default -> false;
      };
    } catch (Exception ignored) { return false; }
  }

  public static String destination(int provider, String playlistUrl) {
    return allowed(provider, playlistUrl) ? playlistUrl.trim() : home(provider);
  }
}
