package us.myles.ViaVersion.util;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.regex.Pattern;

// Based on https://github.com/SpigotMC/BungeeCord/blob/master/chat/src/main/java/net/md_5/bungee/api/ChatColor.java
public final class ChatColorUtil {

    public static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";
    public static final char COLOR_CHAR = '§';
    public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-ORX]");
    public static final Pattern CODE_PATTERN = Pattern.compile("(?i)[0-9A-FK-ORX]");
    private static final Int2IntMap COLOR_ORDINALS = new Int2IntOpenHashMap();
    private static int ordinalCounter;

    static {
        addColorOrindal('0', '9');
        addColorOrindal('a', 'f');
        addColorOrindal('k', 'o');
        addColorOrindal('r');
    }

    public static int getColorOrdinal(char c) {
        return COLOR_ORDINALS.getOrDefault(c, -1);
    }

    public static String translateAlternateColorCodes(String s) {
        char[] bytes = s.toCharArray();
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] == '&' && ALL_CODES.indexOf(bytes[i + 1]) > -1) {
                bytes[i] = COLOR_CHAR;
                bytes[i + 1] = Character.toLowerCase(bytes[i + 1]);
            }
        }
        return new String(bytes);
    }

    public static String stripColor(final String input) {
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    private static void addColorOrindal(int from, int to) {
        for (int c = from; c < to; c++) {
            addColorOrindal(c);
        }
    }

    private static void addColorOrindal(int colorChar) {
        COLOR_ORDINALS.put(colorChar, ordinalCounter++);
    }
}
