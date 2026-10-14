package agriflow.algorithms;

/**
 * Vertex cover: pick the fewest nodes so that EVERY edge touches a picked node. NP-hard.
 * Three ways to live with that:  exact (exponential) / 2-approximation (fast, within 2x) / FPT (exponential only in k).
 */
public final class VertexCover {
    private VertexCover() {}

    public static boolean isCover(int n, int[] eu, int[] ev, boolean[] c) {
        for (int i = 0; i < eu.length; i++) if (!c[eu[i]] && !c[ev[i]]) return false;
        return true;
    }

    public static int size(boolean[] c) {
        int k = 0;
        for (boolean b : c) if (b) k++;
        return k;
    }

    /** take any uncovered edge, grab BOTH ends. The edges we grab form a matching, so OPT needs one end of each: ratio <= 2. */
    public static boolean[] approx2(int n, int[] eu, int[] ev) {
        boolean[] c = new boolean[n];
        for (int i = 0; i < eu.length; i++) if (!c[eu[i]] && !c[ev[i]]) { c[eu[i]] = true; c[ev[i]] = true; }
        return c;
    }

    /** try every subset (n <= 24), keep the smallest cover */
    public static boolean[] exact(int n, int[] eu, int[] ev) {
        int[] mu = new int[eu.length];
        for (int i = 0; i < eu.length; i++) mu[i] = (1 << eu[i]) | (1 << ev[i]);
        int best = -1, bestSize = n + 1;
        for (int mask = 0; mask < (1 << n); mask++) {
            int bc = Integer.bitCount(mask);
            if (bc >= bestSize) continue;
            boolean ok = true;
            for (int i = 0; i < mu.length && ok; i++) if ((mask & mu[i]) == 0) ok = false;
            if (ok) { best = mask; bestSize = bc; }
        }
        boolean[] c = new boolean[n];
        for (int i = 0; i < n; i++) c[i] = (best & (1 << i)) != 0;
        return c;
    }

    public static final class Fpt {
        public boolean[] cover;     // null = no cover of size <= k exists
        public long branches;       // search-tree nodes visited (<= 2^(k+1))
        public int forced;          // nodes forced into the cover by the kernel rule
        public int kernelEdges;     // edges left after the kernel rule
        public boolean kernelRejected;
    }

    /** Is there a cover of size <= k?  Buss kernel (a node with degree > k MUST be in the cover) + branch on an edge. */
    public static Fpt fpt(int n, int[] eu, int[] ev, int k) {
        Fpt r = new Fpt();
        boolean[] c = new boolean[n];
        int budget = k;
        boolean changed = true;
        while (changed && budget >= 0) {
            changed = false;
            int[] deg = new int[n];
            for (int i = 0; i < eu.length; i++) if (!c[eu[i]] && !c[ev[i]]) { deg[eu[i]]++; deg[ev[i]]++; }
            for (int v = 0; v < n; v++) if (!c[v] && deg[v] > budget) { c[v] = true; budget--; r.forced++; changed = true; break; }
        }
        int left = 0;
        for (int i = 0; i < eu.length; i++) if (!c[eu[i]] && !c[ev[i]]) left++;
        r.kernelEdges = left;
        if (budget < 0 || left > budget * budget) { r.kernelRejected = true; return r; }
        long[] cnt = new long[1];
        if (branch(eu, ev, c, budget, cnt)) r.cover = c;
        r.branches = cnt[0];
        return r;
    }

    private static boolean branch(int[] eu, int[] ev, boolean[] c, int k, long[] cnt) {
        cnt[0]++;
        int e = -1;
        for (int i = 0; i < eu.length; i++) if (!c[eu[i]] && !c[ev[i]]) { e = i; break; }
        if (e == -1) return true;
        if (k == 0) return false;
        c[eu[e]] = true;
        if (branch(eu, ev, c, k - 1, cnt)) return true;
        c[eu[e]] = false;
        c[ev[e]] = true;
        if (branch(eu, ev, c, k - 1, cnt)) return true;
        c[ev[e]] = false;
        return false;
    }
}
