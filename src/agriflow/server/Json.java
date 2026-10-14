package agriflow.server;

/** Minimal JSON writer (no libraries). Json.obj().put("k", v)... or Json.arr().add(v)... */
public final class Json {
    private final StringBuilder sb;
    private final char close;
    private boolean first = true;

    private Json(char open, char close) { sb = new StringBuilder().append(open); this.close = close; }

    public static Json obj() { return new Json('{', '}'); }
    public static Json arr() { return new Json('[', ']'); }

    private void sep() { if (!first) sb.append(','); first = false; }
    private Json key(String k) { sep(); sb.append('"').append(esc(k)).append("\":"); return this; }

    public static String esc(String s) {
        StringBuilder o = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': o.append("\\\\"); break;
                case '"': o.append("\\\""); break;
                case '\n': o.append("\\n"); break;
                case '\r': o.append("\\r"); break;
                case '\t': o.append("\\t"); break;
                default:
                    if (c < 0x20) o.append(String.format("\\u%04x", (int) c)); else o.append(c);
            }
        }
        return o.toString();
    }

    private static String num(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "null";
        double r = Math.round(v * 10000.0) / 10000.0;
        if (r == Math.rint(r) && Math.abs(r) < 1e15) return Long.toString((long) r);
        return Double.toString(r);
    }

    private static String str(String v) { return v == null ? "null" : "\"" + esc(v) + "\""; }

    // ---- values as raw JSON text ----
    public static String of(int[] a) {
        StringBuilder o = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) { if (i > 0) o.append(','); o.append(a[i]); }
        return o.append(']').toString();
    }
    public static String of(long[] a) {
        StringBuilder o = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) { if (i > 0) o.append(','); o.append(a[i]); }
        return o.append(']').toString();
    }
    public static String of(double[] a) {
        StringBuilder o = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) { if (i > 0) o.append(','); o.append(num(a[i])); }
        return o.append(']').toString();
    }
    public static String of(boolean[] a) {
        StringBuilder o = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) { if (i > 0) o.append(','); o.append(a[i]); }
        return o.append(']').toString();
    }
    public static String of(String[] a) {
        StringBuilder o = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) { if (i > 0) o.append(','); o.append(str(a[i])); }
        return o.append(']').toString();
    }
    public static String of(int[][] a) {
        StringBuilder o = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) { if (i > 0) o.append(','); o.append(of(a[i])); }
        return o.append(']').toString();
    }

    // ---- object members ----
    public Json put(String k, String v) { key(k); sb.append(str(v)); return this; }
    public Json put(String k, long v) { key(k); sb.append(v); return this; }
    public Json put(String k, double v) { key(k); sb.append(num(v)); return this; }
    public Json put(String k, boolean v) { key(k); sb.append(v); return this; }
    public Json put(String k, int[] v) { key(k); sb.append(of(v)); return this; }
    public Json put(String k, long[] v) { key(k); sb.append(of(v)); return this; }
    public Json put(String k, double[] v) { key(k); sb.append(of(v)); return this; }
    public Json put(String k, boolean[] v) { key(k); sb.append(of(v)); return this; }
    public Json put(String k, String[] v) { key(k); sb.append(of(v)); return this; }
    public Json put(String k, int[][] v) { key(k); sb.append(of(v)); return this; }
    public Json put(String k, Json v) { key(k); sb.append(v == null ? "null" : v.toString()); return this; }
    public Json putNull(String k) { key(k); sb.append("null"); return this; }

    // ---- array elements ----
    public Json add(String v) { sep(); sb.append(str(v)); return this; }
    public Json add(long v) { sep(); sb.append(v); return this; }
    public Json add(double v) { sep(); sb.append(num(v)); return this; }
    public Json add(int[] v) { sep(); sb.append(of(v)); return this; }
    public Json add(Json v) { sep(); sb.append(v.toString()); return this; }

    @Override public String toString() { return sb.toString() + close; }
}
