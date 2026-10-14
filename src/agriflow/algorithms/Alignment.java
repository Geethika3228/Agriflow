package agriflow.algorithms;

/**
 * Sequence alignment (the genome-style DP).
 *  - global: Needleman-Wunsch (line up the WHOLE of both strings)
 *  - local : Smith-Waterman (find the best matching REGION; scores never go below 0)
 *  - affine: Gotoh global alignment, where opening a gap costs more than extending it
 */
public final class Alignment {
    private Alignment() {}

    public static final class Result {
        public int score;
        public String alignedA, alignedB;
        public int[][] table;          // DP matrix (for affine: the M matrix)
        public int[] path;             // flattened (i,j) cells of the traceback, end -> start
        public int matches, mismatches, gapChars;
    }

    private static int sc(char x, char y, int match, int mismatch) { return x == y ? match : mismatch; }

    private static void count(Result r) {
        for (int k = 0; k < r.alignedA.length(); k++) {
            char x = r.alignedA.charAt(k), y = r.alignedB.charAt(k);
            if (x == '-' || y == '-') r.gapChars++;
            else if (x == y) r.matches++;
            else r.mismatches++;
        }
    }

    /** gap = a positive penalty per gap character */
    public static Result global(String a, String b, int match, int mismatch, int gap) {
        int n = a.length(), m = b.length();
        int[][] h = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) h[i][0] = -gap * i;
        for (int j = 1; j <= m; j++) h[0][j] = -gap * j;
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++)
                h[i][j] = Math.max(h[i - 1][j - 1] + sc(a.charAt(i - 1), b.charAt(j - 1), match, mismatch),
                        Math.max(h[i - 1][j] - gap, h[i][j - 1] - gap));
        StringBuilder sa = new StringBuilder(), sb = new StringBuilder();
        IntList path = new IntList();
        int i = n, j = m;
        path.add(i); path.add(j);
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && h[i][j] == h[i - 1][j - 1] + sc(a.charAt(i - 1), b.charAt(j - 1), match, mismatch)) {
                sa.append(a.charAt(i - 1)); sb.append(b.charAt(j - 1)); i--; j--;
            } else if (i > 0 && h[i][j] == h[i - 1][j] - gap) {
                sa.append(a.charAt(i - 1)); sb.append('-'); i--;
            } else {
                sa.append('-'); sb.append(b.charAt(j - 1)); j--;
            }
            path.add(i); path.add(j);
        }
        Result r = new Result();
        r.score = h[n][m]; r.table = h; r.path = path.toArray();
        r.alignedA = sa.reverse().toString(); r.alignedB = sb.reverse().toString();
        count(r);
        return r;
    }

    public static Result local(String a, String b, int match, int mismatch, int gap) {
        int n = a.length(), m = b.length();
        int[][] h = new int[n + 1][m + 1];
        int best = 0, bi = 0, bj = 0;
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++) {
                int v = Math.max(0, h[i - 1][j - 1] + sc(a.charAt(i - 1), b.charAt(j - 1), match, mismatch));
                v = Math.max(v, Math.max(h[i - 1][j] - gap, h[i][j - 1] - gap));
                h[i][j] = v;
                if (v > best) { best = v; bi = i; bj = j; }
            }
        StringBuilder sa = new StringBuilder(), sb = new StringBuilder();
        IntList path = new IntList();
        int i = bi, j = bj;
        path.add(i); path.add(j);
        while (i > 0 && j > 0 && h[i][j] > 0) {
            if (h[i][j] == h[i - 1][j - 1] + sc(a.charAt(i - 1), b.charAt(j - 1), match, mismatch)) {
                sa.append(a.charAt(i - 1)); sb.append(b.charAt(j - 1)); i--; j--;
            } else if (h[i][j] == h[i - 1][j] - gap) {
                sa.append(a.charAt(i - 1)); sb.append('-'); i--;
            } else {
                sa.append('-'); sb.append(b.charAt(j - 1)); j--;
            }
            path.add(i); path.add(j);
        }
        Result r = new Result();
        r.score = best; r.table = h; r.path = path.toArray();
        r.alignedA = sa.reverse().toString(); r.alignedB = sb.reverse().toString();
        count(r);
        return r;
    }

    private static final int NEG = -500_000_000;

    /** a gap of length k costs open + (k-1)*extend. One long gap is cheaper than many short ones. */
    public static Result affine(String a, String b, int match, int mismatch, int open, int extend) {
        int n = a.length(), m = b.length();
        int[][] M = new int[n + 1][m + 1], X = new int[n + 1][m + 1], Y = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) for (int j = 0; j <= m; j++) { M[i][j] = NEG; X[i][j] = NEG; Y[i][j] = NEG; }
        M[0][0] = 0;
        for (int i = 0; i <= n; i++)
            for (int j = 0; j <= m; j++) {
                if (i == 0 && j == 0) continue;
                if (i > 0 && j > 0) {
                    int p = Math.max(M[i - 1][j - 1], Math.max(X[i - 1][j - 1], Y[i - 1][j - 1]));
                    if (p > NEG / 2) M[i][j] = p + sc(a.charAt(i - 1), b.charAt(j - 1), match, mismatch);
                }
                if (i > 0) {
                    int p = Math.max(M[i - 1][j] - open, Math.max(X[i - 1][j] - extend, Y[i - 1][j] - open));
                    if (p > NEG / 2) X[i][j] = p;
                }
                if (j > 0) {
                    int p = Math.max(M[i][j - 1] - open, Math.max(Y[i][j - 1] - extend, X[i][j - 1] - open));
                    if (p > NEG / 2) Y[i][j] = p;
                }
            }
        int end = Math.max(M[n][m], Math.max(X[n][m], Y[n][m]));
        int state = end == M[n][m] ? 0 : (end == X[n][m] ? 1 : 2);
        StringBuilder sa = new StringBuilder(), sb = new StringBuilder();
        IntList path = new IntList();
        int i = n, j = m;
        path.add(i); path.add(j);
        while (i > 0 || j > 0) {
            if (state == 0) {
                int prev = M[i][j] - sc(a.charAt(i - 1), b.charAt(j - 1), match, mismatch);
                sa.append(a.charAt(i - 1)); sb.append(b.charAt(j - 1));
                i--; j--;
                state = (M[i][j] == prev) ? 0 : (X[i][j] == prev ? 1 : 2);
            } else if (state == 1) {
                int v = X[i][j];
                sa.append(a.charAt(i - 1)); sb.append('-');
                i--;
                state = (M[i][j] > NEG / 2 && M[i][j] - open == v) ? 0 : (X[i][j] > NEG / 2 && X[i][j] - extend == v ? 1 : 2);
            } else {
                int v = Y[i][j];
                sa.append('-'); sb.append(b.charAt(j - 1));
                j--;
                state = (M[i][j] > NEG / 2 && M[i][j] - open == v) ? 0 : (Y[i][j] > NEG / 2 && Y[i][j] - extend == v ? 2 : 1);
            }
            path.add(i); path.add(j);
        }
        Result r = new Result();
        r.score = end; r.table = M; r.path = path.toArray();
        r.alignedA = sa.reverse().toString(); r.alignedB = sb.reverse().toString();
        count(r);
        return r;
    }
}
