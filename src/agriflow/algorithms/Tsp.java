package agriflow.algorithms;

/**
 * Travelling salesman three ways:
 *  - exact bitmask DP (Held-Karp) O(2^n * n^2): perfect, but explodes
 *  - nearest neighbour: quick and loose, no guarantee
 *  - MST preorder: a real 2-approximation when distances obey the triangle inequality
 */
public final class Tsp {
    private Tsp() {}

    public static final class Result {
        public double cost;
        public int[] order;    // visiting order starting at node 0 (the return leg to 0 is implied)
        public long states;    // DP states touched (exact only)
        public double mst;     // MST weight (mstApprox only) - a lower bound on the best tour
    }

    public static double tourCost(double[][] d, int[] order) {
        double c = 0;
        for (int i = 0; i < order.length; i++) c += d[order[i]][order[(i + 1) % order.length]];
        return c;
    }

    public static Result exact(double[][] d) {
        int n = d.length;
        Result res = new Result();
        if (n == 1) { res.order = new int[]{0}; return res; }
        int full = 1 << n;
        double[][] dp = new double[full][n];
        int[][] par = new int[full][n];
        for (int i = 0; i < full; i++) for (int j = 0; j < n; j++) dp[i][j] = Double.MAX_VALUE;
        dp[1][0] = 0;
        long states = 0;
        for (int mask = 1; mask < full; mask += 2) {
            for (int last = 0; last < n; last++) {
                if ((mask & (1 << last)) == 0 || dp[mask][last] == Double.MAX_VALUE) continue;
                for (int nx = 1; nx < n; nx++) {
                    if ((mask & (1 << nx)) != 0) continue;
                    states++;
                    int nm = mask | (1 << nx);
                    double v = dp[mask][last] + d[last][nx];
                    if (v < dp[nm][nx]) { dp[nm][nx] = v; par[nm][nx] = last; }
                }
            }
        }
        double best = Double.MAX_VALUE;
        int bl = 1;
        for (int last = 1; last < n; last++) {
            double v = dp[full - 1][last] + d[last][0];
            if (v < best) { best = v; bl = last; }
        }
        int[] order = new int[n];
        int mask = full - 1, cur = bl;
        for (int k = n - 1; k >= 1; k--) {
            order[k] = cur;
            int p = par[mask][cur];
            mask ^= 1 << cur;
            cur = p;
        }
        order[0] = 0;
        res.cost = best; res.order = order; res.states = states;
        return res;
    }

    public static Result nearestNeighbour(double[][] d) {
        int n = d.length;
        boolean[] seen = new boolean[n];
        int[] order = new int[n];
        int cur = 0;
        seen[0] = true;
        for (int k = 1; k < n; k++) {
            int best = -1;
            for (int v = 0; v < n; v++) if (!seen[v] && (best == -1 || d[cur][v] < d[cur][best])) best = v;
            order[k] = best; seen[best] = true; cur = best;
        }
        Result r = new Result();
        r.order = order; r.cost = tourCost(d, order);
        return r;
    }

    /** Prim MST, then visit nodes in DFS preorder (shortcutting repeats). Cost <= 2 * optimal for metric inputs. */
    public static Result mstApprox(double[][] d) {
        int n = d.length;
        int[] parent = new int[n];
        double[] key = new double[n];
        boolean[] in = new boolean[n];
        for (int i = 0; i < n; i++) { key[i] = Double.MAX_VALUE; parent[i] = -1; }
        key[0] = 0;
        double mst = 0;
        for (int it = 0; it < n; it++) {
            int u = -1;
            for (int v = 0; v < n; v++) if (!in[v] && (u == -1 || key[v] < key[u])) u = v;
            in[u] = true;
            mst += key[u];
            for (int v = 0; v < n; v++) if (!in[v] && d[u][v] < key[v]) { key[v] = d[u][v]; parent[v] = u; }
        }
        int[] order = new int[n];
        int[] cnt = {0};
        preorder(0, parent, n, order, cnt);
        Result r = new Result();
        r.order = order; r.cost = tourCost(d, order);
        r.mst = mst;
        return r;
    }

    public static double mstWeight(double[][] d) { return mstApprox(d).mst; }

    private static void preorder(int u, int[] parent, int n, int[] order, int[] cnt) {
        order[cnt[0]++] = u;
        for (int v = 0; v < n; v++) if (parent[v] == u) preorder(v, parent, n, order, cnt);
    }

    /** Does a path that visits every node exactly once exist? bitmask DP O(2^n * n). Returns the path or null. */
    public static int[] hamiltonianPath(int n, int[] adjMask) {
        int full = 1 << n;
        boolean[][] ok = new boolean[full][n];
        for (int i = 0; i < n; i++) ok[1 << i][i] = true;
        for (int mask = 1; mask < full; mask++)
            for (int last = 0; last < n; last++) {
                if (!ok[mask][last]) continue;
                int nxt = adjMask[last] & ~mask;
                while (nxt != 0) {
                    int b = Integer.numberOfTrailingZeros(nxt);
                    nxt &= nxt - 1;
                    ok[mask | (1 << b)][b] = true;
                }
            }
        int end = -1;
        for (int i = 0; i < n; i++) if (ok[full - 1][i]) { end = i; break; }
        if (end == -1) return null;
        int[] path = new int[n];
        int mask = full - 1, cur = end;
        for (int k = n - 1; k >= 0; k--) {
            path[k] = cur;
            int pm = mask ^ (1 << cur);
            if (k > 0) {
                int prev = -1;
                for (int p = 0; p < n; p++) if (ok[pm][p] && (adjMask[p] & (1 << cur)) != 0) { prev = p; break; }
                cur = prev;
            }
            mask = pm;
        }
        return path;
    }
}
