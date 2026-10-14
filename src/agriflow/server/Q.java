package agriflow.server;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Query-string reader with sane defaults and clamping, so a silly URL can't crash an algorithm. */
final class Q {
    static final class Bad extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Bad(String m) { super(m); }
    }

    private final String raw;

    Q(String raw) { this.raw = raw == null ? "" : raw; }

    String s(String key, String def) {
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).equals(key))
                return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
        }
        return def;
    }

    boolean has(String key) {
        String v = s(key, null);
        return v != null && v.length() > 0;
    }

    int i(String key, int def, int lo, int hi) {
        String v = s(key, null);
        if (v == null || v.trim().isEmpty()) return def;
        try {
            long x = Long.parseLong(v.trim());
            return (int) Math.max(lo, Math.min(hi, x));
        } catch (NumberFormatException e) {
            throw new Bad("'" + key + "' must be a whole number");
        }
    }

    long l(String key, long def) {
        String v = s(key, null);
        if (v == null || v.trim().isEmpty()) return def;
        try { return Long.parseLong(v.trim()); }
        catch (NumberFormatException e) { throw new Bad("'" + key + "' must be a whole number that fits in 63 bits"); }
    }

    double d(String key, double def, double lo, double hi) {
        String v = s(key, null);
        if (v == null || v.trim().isEmpty()) return def;
        try { return Math.max(lo, Math.min(hi, Double.parseDouble(v.trim()))); }
        catch (NumberFormatException e) { throw new Bad("'" + key + "' must be a number"); }
    }

    /** "3, 5,8" -> {3,5,8} */
    int[] ints(String key, int[] def) {
        String v = s(key, null);
        if (v == null || v.trim().isEmpty()) return def;
        String[] p = v.trim().split("\\s*[,;]\\s*");
        int[] r = new int[p.length];
        for (int k = 0; k < p.length; k++) {
            try { r[k] = Integer.parseInt(p[k].trim()); }
            catch (NumberFormatException e) { throw new Bad("'" + key + "' must be comma-separated whole numbers"); }
        }
        return r;
    }

    String[] list(String key, String sep, String[] def) {
        String v = s(key, null);
        if (v == null || v.trim().isEmpty()) return def;
        String[] p = v.split(sep);
        int c = 0;
        for (String x : p) if (!x.trim().isEmpty()) c++;
        String[] r = new String[c];
        c = 0;
        for (String x : p) if (!x.trim().isEmpty()) r[c++] = x.trim();
        return r;
    }
}
