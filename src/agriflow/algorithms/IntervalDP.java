package agriflow.algorithms;

/** Interval DP: the answer for a range [i, j) is built from answers for smaller ranges inside it. */
public final class IntervalDP {
    private IntervalDP() {}

    public static final class ChainResult {
        public long cost;
        public String parens;
        public long leftToRightCost;
        public int[][] split;
    }

    /** dims has k+1 numbers for k matrices (matrix t is dims[t] x dims[t+1]). O(n^3). */
    public static ChainResult matrixChain(int[] d) {
        int n = d.length - 1;
        ChainResult r = new ChainResult();
        if (n < 1) { r.parens = ""; return r; }
        long[][] m = new long[n][n];
        int[][] s = new int[n][n];
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                m[i][j] = Long.MAX_VALUE;
                for (int k = i; k < j; k++) {
                    long c = m[i][k] + m[k + 1][j] + (long) d[i] * d[k + 1] * d[j + 1];
                    if (c < m[i][j]) { m[i][j] = c; s[i][j] = k; }
                }
            }
        r.cost = m[0][n - 1];
        r.split = s;
        StringBuilder sb = new StringBuilder();
        paren(s, 0, n - 1, sb);
        r.parens = sb.toString();
        long ltr = 0;
        for (int t = 1; t < n; t++) ltr += (long) d[0] * d[t] * d[t + 1];
        r.leftToRightCost = ltr;
        return r;
    }

    private static void paren(int[][] s, int i, int j, StringBuilder sb) {
        if (i == j) { sb.append('A').append(i + 1); return; }
        sb.append('(');
        paren(s, i, s[i][j], sb);
        sb.append(" x ");
        paren(s, s[i][j] + 1, j, sb);
        sb.append(')');
    }

    public static final class ObstResult {
        public long cost;
        public long opsCubic, opsKnuth;
        public int root;
        public int[] left, right;   // child index per key, -1 if none
    }

    /** Optimal BST over keys already sorted; freq[k] = how often key k is looked up.
     *  Runs both the O(n^3) DP and Knuth's O(n^2) speed-up, and reports how many inner steps each took. */
    public static ObstResult obst(long[] f) {
        int n = f.length;
        ObstResult res = new ObstResult();
        res.left = new int[n]; res.right = new int[n];
        for (int i = 0; i < n; i++) { res.left[i] = -1; res.right[i] = -1; }
        if (n == 0) { res.root = -1; return res; }
        long[] pre = new long[n + 1];
        for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + f[i];
        long[][] c = new long[n + 1][n + 1];
        int[][] root = new int[n + 1][n + 1];
        long ops = 0;
        for (int len = 1; len <= n; len++)
            for (int i = 0; i + len <= n; i++) {
                int j = i + len;
                long best = Long.MAX_VALUE; int br = i;
                for (int r = i; r < j; r++) {
                    ops++;
                    long v = c[i][r] + c[r + 1][j];
                    if (v < best) { best = v; br = r; }
                }
                c[i][j] = best + pre[j] - pre[i];
                root[i][j] = br;
            }
        res.opsCubic = ops;
        res.cost = c[0][n];
        // Knuth's speed-up: the best root for [i,j) lies between best root of [i,j-1) and [i+1,j)
        long[][] c2 = new long[n + 1][n + 1];
        int[][] r2 = new int[n + 1][n + 1];
        long ops2 = 0;
        for (int i = 0; i < n; i++) { c2[i][i + 1] = f[i]; r2[i][i + 1] = i; }
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len <= n; i++) {
                int j = i + len;
                long best = Long.MAX_VALUE; int br = r2[i][j - 1];
                for (int r = r2[i][j - 1]; r <= r2[i + 1][j]; r++) {
                    ops2++;
                    long v = c2[i][r] + c2[r + 1][j];
                    if (v < best) { best = v; br = r; }
                }
                c2[i][j] = best + pre[j] - pre[i];
                r2[i][j] = br;
            }
        res.opsKnuth = ops2;
        res.root = build(root, 0, n, res);
        // sanity: Knuth must agree with the cubic DP
        if (c2[0][n] != res.cost) res.opsKnuth = -1;
        return res;
    }

    private static int build(int[][] root, int i, int j, ObstResult r) {
        if (i >= j) return -1;
        int k = root[i][j];
        r.left[k] = build(root, i, k, r);
        r.right[k] = build(root, k + 1, j, r);
        return k;
    }
}
