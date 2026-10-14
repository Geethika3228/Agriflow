package agriflow.algorithms;

/** Edit distance family: Levenshtein (Wagner-Fischer), Damerau (with swaps), weighted. */
public final class EditDistance {
    private EditDistance() {}

    /** Levenshtein, memory-light (two rows). */
    public static int distance(String a, String b) {
        int n = a.length(), m = b.length();
        int[] prev = new int[m + 1], cur = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            for (int j = 1; j <= m; j++) {
                int sub = prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                cur[j] = Math.min(sub, Math.min(prev[j] + 1, cur[j - 1] + 1));
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[m];
    }

    /** the full Wagner-Fischer table, t[i][j] = distance(a[0..i), b[0..j)) */
    public static int[][] table(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] t = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) t[i][0] = i;
        for (int j = 0; j <= m; j++) t[0][j] = j;
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++) {
                int sub = t[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                t[i][j] = Math.min(sub, Math.min(t[i - 1][j] + 1, t[i][j - 1] + 1));
            }
        return t;
    }

    /** true Damerau-Levenshtein (Lowrance-Wagner): insert, delete, substitute, swap neighbours. */
    public static int damerau(String a, String b) {
        int n = a.length(), m = b.length();
        int inf = n + m;
        int[][] d = new int[n + 2][m + 2];
        d[0][0] = inf;
        for (int i = 0; i <= n; i++) { d[i + 1][0] = inf; d[i + 1][1] = i; }
        for (int j = 0; j <= m; j++) { d[0][j + 1] = inf; d[1][j + 1] = j; }
        int[] da = new int[65536];
        for (int i = 1; i <= n; i++) {
            int db = 0;
            for (int j = 1; j <= m; j++) {
                int i1 = da[b.charAt(j - 1)], j1 = db, cost;
                if (a.charAt(i - 1) == b.charAt(j - 1)) { cost = 0; db = j; } else cost = 1;
                int v = Math.min(d[i][j] + cost, Math.min(d[i + 1][j] + 1, d[i][j + 1] + 1));
                v = Math.min(v, d[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1));
                d[i + 1][j + 1] = v;
            }
            da[a.charAt(i - 1)] = i;
        }
        return d[n + 1][m + 1];
    }

    private static boolean vowel(char c) { return "aeiou".indexOf(Character.toLowerCase(c)) >= 0; }

    /** weighted edit distance: you choose insert/delete/substitute prices; vowel-for-vowel swaps are half price. */
    public static int weighted(String a, String b, int wIns, int wDel, int wSub) {
        int n = a.length(), m = b.length();
        int[][] t = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) t[i][0] = i * wDel;
        for (int j = 1; j <= m; j++) t[0][j] = j * wIns;
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++) {
                char x = a.charAt(i - 1), y = b.charAt(j - 1);
                int sc = x == y ? 0 : (vowel(x) && vowel(y) ? (wSub + 1) / 2 : wSub);
                t[i][j] = Math.min(t[i - 1][j - 1] + sc, Math.min(t[i - 1][j] + wDel, t[i][j - 1] + wIns));
            }
        return t[n][m];
    }

    public static int weightedDistance(String a, String b) { return weighted(a, b, 2, 2, 2); }

    /** the actual edit script, as tokens like "M:r" (match), "S:i>e" (substitute), "I:x", "D:y". */
    public static String[] ops(String a, String b) {
        int[][] t = table(a, b);
        int i = a.length(), j = b.length();
        String[] rev = new String[i + j];
        int c = 0;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && t[i][j] == t[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1)) {
                rev[c++] = a.charAt(i - 1) == b.charAt(j - 1) ? "M:" + a.charAt(i - 1) : "S:" + a.charAt(i - 1) + ">" + b.charAt(j - 1);
                i--; j--;
            } else if (i > 0 && t[i][j] == t[i - 1][j] + 1) {
                rev[c++] = "D:" + a.charAt(i - 1);
                i--;
            } else {
                rev[c++] = "I:" + b.charAt(j - 1);
                j--;
            }
        }
        String[] r = new String[c];
        for (int k = 0; k < c; k++) r[k] = rev[c - 1 - k];
        return r;
    }

    /** the cells (i,j) visited by the backtrace, flattened [i0,j0,i1,j1,...] from the end to (0,0) */
    public static int[] path(String a, String b) {
        int[][] t = table(a, b);
        int i = a.length(), j = b.length();
        IntList p = new IntList();
        p.add(i); p.add(j);
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && t[i][j] == t[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1)) { i--; j--; }
            else if (i > 0 && t[i][j] == t[i - 1][j] + 1) i--;
            else j--;
            p.add(i); p.add(j);
        }
        return p.toArray();
    }
}
