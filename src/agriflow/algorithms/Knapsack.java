package agriflow.algorithms;

/** Knapsack three ways (exact DP / greedy / FPTAS) plus subset-sum. NP-hard, but "pseudo-polynomial" in the numbers. */
public final class Knapsack {
    private Knapsack() {}

    public static final class Result {
        public long value;
        public int weight;
        public boolean[] take;
    }

    /** exact, O(n*W): fine while W is small, but W is a NUMBER, so it is not really polynomial in the input size. */
    public static Result exact(int[] v, int[] w, int cap) {
        int n = v.length;
        long[][] dp = new long[n + 1][cap + 1];
        for (int i = 1; i <= n; i++)
            for (int c = 0; c <= cap; c++) {
                dp[i][c] = dp[i - 1][c];
                if (w[i - 1] <= c) dp[i][c] = Math.max(dp[i][c], dp[i - 1][c - w[i - 1]] + v[i - 1]);
            }
        Result r = new Result();
        r.take = new boolean[n];
        r.value = dp[n][cap];
        for (int i = n, c = cap; i >= 1; i--)
            if (dp[i][c] != dp[i - 1][c]) { r.take[i - 1] = true; c -= w[i - 1]; r.weight += w[i - 1]; }
        return r;
    }

    /** best value-per-weight first. Quick, but can be arbitrarily bad. */
    public static Result greedy(int[] v, int[] w, int cap) {
        int n = v.length;
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        for (int i = 1; i < n; i++) {                       // insertion sort by v/w descending
            int x = idx[i], j = i - 1;
            while (j >= 0 && (long) v[idx[j]] * w[x] < (long) v[x] * w[idx[j]]) { idx[j + 1] = idx[j]; j--; }
            idx[j + 1] = x;
        }
        Result r = new Result();
        r.take = new boolean[n];
        for (int k = 0; k < n; k++) {
            int i = idx[k];
            if (r.weight + w[i] <= cap) { r.take[i] = true; r.weight += w[i]; r.value += v[i]; }
        }
        return r;
    }

    /** greedy, but also compare against the single most valuable item that fits. This one is a proven 1/2-approximation. */
    public static Result greedyHalf(int[] v, int[] w, int cap) {
        Result g = greedy(v, w, cap);
        int best = -1;
        for (int i = 0; i < v.length; i++) if (w[i] <= cap && (best == -1 || v[i] > v[best])) best = i;
        if (best != -1 && v[best] > g.value) {
            Result r = new Result();
            r.take = new boolean[v.length];
            r.take[best] = true; r.value = v[best]; r.weight = w[best];
            return r;
        }
        return g;
    }

    /**
     * FPTAS: round the VALUES down (divide by K = eps*vmax/n), then DP over value instead of weight.
     * Guarantee: result >= (1 - eps) * OPT, running time O(n^3 / eps).
     */
    public static Result fptas(int[] v, int[] w, int cap, double eps) {
        int n = v.length;
        int vmax = 0;
        for (int i = 0; i < n; i++) if (w[i] <= cap && v[i] > vmax) vmax = v[i];
        Result r = new Result();
        r.take = new boolean[n];
        if (vmax == 0) return r;
        double k = eps * vmax / n;
        int[] sv = new int[n];
        int total = 0;
        for (int i = 0; i < n; i++) { sv[i] = (int) (v[i] / k); total += sv[i]; }
        final int INF = Integer.MAX_VALUE / 2;
        int[][] minW = new int[n + 1][total + 1];
        for (int i = 0; i <= n; i++) for (int x = 0; x <= total; x++) minW[i][x] = INF;
        minW[0][0] = 0;
        for (int i = 1; i <= n; i++)
            for (int x = 0; x <= total; x++) {
                minW[i][x] = minW[i - 1][x];
                if (x >= sv[i - 1] && minW[i - 1][x - sv[i - 1]] + w[i - 1] < minW[i][x])
                    minW[i][x] = minW[i - 1][x - sv[i - 1]] + w[i - 1];
            }
        int bestX = 0;
        for (int x = 0; x <= total; x++) if (minW[n][x] <= cap) bestX = x;
        for (int i = n, x = bestX; i >= 1; i--)
            if (minW[i][x] != minW[i - 1][x]) { r.take[i - 1] = true; x -= sv[i - 1]; }
        for (int i = 0; i < n; i++) if (r.take[i]) { r.value += v[i]; r.weight += w[i]; }
        return r;
    }

    /** can some subset of nums add up to exactly target? returns the chosen indices or null */
    public static int[] subsetSum(int[] nums, int target) {
        int n = nums.length;
        boolean[][] ok = new boolean[n + 1][target + 1];
        ok[0][0] = true;
        for (int i = 1; i <= n; i++)
            for (int s = 0; s <= target; s++)
                ok[i][s] = ok[i - 1][s] || (s >= nums[i - 1] && ok[i - 1][s - nums[i - 1]]);
        if (!ok[n][target]) return null;
        IntList out = new IntList();
        for (int i = n, s = target; i >= 1; i--)
            if (!ok[i - 1][s]) { out.add(i - 1); s -= nums[i - 1]; }
        return out.toArray();
    }
}
