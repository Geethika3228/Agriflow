package agriflow.algorithms;

/** Sum-over-subsets DP, plus the "greedy vs DP" reality check. */
public final class SubsetDP {
    private SubsetDP() {}

    /** g[mask] = sum of f[sub] over every sub that is a subset of mask. O(k * 2^k) instead of O(3^k). */
    public static long[] sos(int k, long[] f) {
        int full = 1 << k;
        long[] g = new long[full];
        System.arraycopy(f, 0, g, 0, full);
        for (int bit = 0; bit < k; bit++)
            for (int mask = 0; mask < full; mask++)
                if ((mask & (1 << bit)) != 0) g[mask] += g[mask ^ (1 << bit)];
        return g;
    }

    /** the slow way, for checking: O(3^k) by enumerating submasks */
    public static long[] sosSlow(int k, long[] f) {
        int full = 1 << k;
        long[] g = new long[full];
        for (int mask = 0; mask < full; mask++) {
            int sub = mask;
            while (true) {
                g[mask] += f[sub];
                if (sub == 0) break;
                sub = (sub - 1) & mask;
            }
        }
        return g;
    }

    /** fewest coins/bags to make exactly target, or null. DP over amounts. */
    public static int[] coinDP(int[] coins, int target) {
        int[] best = new int[target + 1], last = new int[target + 1];
        for (int i = 1; i <= target; i++) best[i] = Integer.MAX_VALUE;
        for (int v = 1; v <= target; v++)
            for (int c : coins)
                if (c > 0 && c <= v && best[v - c] != Integer.MAX_VALUE && best[v - c] + 1 < best[v]) { best[v] = best[v - c] + 1; last[v] = c; }
        if (best[target] == Integer.MAX_VALUE) return null;
        int[] out = new int[best[target]];
        for (int v = target, k = 0; v > 0; v -= last[v]) out[k++] = last[v];
        return out;
    }

    /** biggest-coin-first. Fine for "nice" coin sets, wrong for others. Returns null if it gets stuck. */
    public static int[] coinGreedy(int[] coins, int target) {
        int[] sorted = new int[coins.length];
        System.arraycopy(coins, 0, sorted, 0, coins.length);
        for (int i = 1; i < sorted.length; i++) {            // insertion sort, descending
            int x = sorted[i], j = i - 1;
            while (j >= 0 && sorted[j] < x) { sorted[j + 1] = sorted[j]; j--; }
            sorted[j + 1] = x;
        }
        IntList out = new IntList();
        int left = target;
        for (int c : sorted) while (c > 0 && left >= c) { left -= c; out.add(c); }
        return left == 0 ? out.toArray() : null;
    }
}
