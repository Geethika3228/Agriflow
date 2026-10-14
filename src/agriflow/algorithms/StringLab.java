package agriflow.algorithms;

/** Instrumented versions that COUNT character comparisons, so the page can show why KMP beats naive. */
public final class StringLab {
    private StringLab() {}

    public static long naiveComparisons(String t, String p) {
        long cmp = 0;
        int n = t.length(), m = p.length();
        if (m == 0 || m > n) return 0;
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m) {
                cmp++;
                if (t.charAt(i + j) != p.charAt(j)) break;
                j++;
            }
        }
        return cmp;
    }

    public static long kmpComparisons(String t, String p) {
        long cmp = 0;
        int n = t.length(), m = p.length();
        if (m == 0 || m > n) return 0;
        int[] f = KMP.lps(p);
        for (int i = 0, k = 0; i < n; i++) {
            while (true) {
                cmp++;
                if (t.charAt(i) == p.charAt(k)) { k++; break; }
                if (k == 0) break;
                k = f[k - 1];
            }
            if (k == m) k = f[k - 1];
        }
        return cmp;
    }
}
