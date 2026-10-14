package agriflow.algorithms;

/** Knuth-Morris-Pratt: never moves backwards in the text. O(n + m). */
public final class KMP {
    private KMP() {}

    /** failure function: lps[i] = length of longest proper prefix of p[0..i] that is also its suffix */
    public static int[] lps(String p) {
        int m = p.length();
        int[] f = new int[m];
        for (int i = 1, k = 0; i < m; i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = f[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            f[i] = k;
        }
        return f;
    }

    public static int[] findAll(String t, String p) {
        IntList r = new IntList();
        int n = t.length(), m = p.length();
        if (m == 0 || m > n) return r.toArray();
        int[] f = lps(p);
        for (int i = 0, k = 0; i < n; i++) {
            while (k > 0 && t.charAt(i) != p.charAt(k)) k = f[k - 1];
            if (t.charAt(i) == p.charAt(k)) k++;
            if (k == m) {
                r.add(i - m + 1);
                k = f[k - 1];
            }
        }
        return r.toArray();
    }
}
