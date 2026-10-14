package agriflow.algorithms;

/** Set cover: pick the fewest "packages" (bitmasks) that together cover every element 0..u-1. */
public final class SetCover {
    private SetCover() {}

    /** exact, bitmask DP over covered-so-far masks. O(2^u * sets). Returns chosen set indices, or null if impossible. */
    public static int[] exact(int[] sets, int u) {
        int full = (1 << u) - 1;
        int[] dp = new int[full + 1], from = new int[full + 1], via = new int[full + 1];
        for (int i = 0; i <= full; i++) dp[i] = Integer.MAX_VALUE;
        dp[0] = 0;
        for (int mask = 0; mask <= full; mask++) {
            if (dp[mask] == Integer.MAX_VALUE) continue;
            for (int s = 0; s < sets.length; s++) {
                int nm = mask | sets[s];
                if (nm != mask && dp[mask] + 1 < dp[nm]) { dp[nm] = dp[mask] + 1; from[nm] = mask; via[nm] = s; }
            }
        }
        if (dp[full] == Integer.MAX_VALUE) return null;
        int[] chosen = new int[dp[full]];
        int mask = full;
        for (int k = chosen.length - 1; k >= 0; k--) { chosen[k] = via[mask]; mask = from[mask]; }
        return chosen;
    }

    /** greedy: always grab the package covering the most still-uncovered elements. ln(n)-approximation. */
    public static int[] greedy(int[] sets, int u) {
        int full = (1 << u) - 1, covered = 0;
        IntList out = new IntList();
        while (covered != full) {
            int best = -1, gain = 0;
            for (int s = 0; s < sets.length; s++) {
                int g = Integer.bitCount(sets[s] & ~covered);
                if (g > gain) { gain = g; best = s; }
            }
            if (best == -1) return null;
            covered |= sets[best];
            out.add(best);
        }
        return out.toArray();
    }
}
