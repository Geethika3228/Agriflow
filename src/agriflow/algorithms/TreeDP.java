package agriflow.algorithms;

/**
 * DP on a tree (think: an irrigation canal network). Everything is iterative (BFS order),
 * so a long skinny tree can't blow the stack.
 */
public final class TreeDP {
    private TreeDP() {}

    public static final class Result {
        public long diameter;
        public int[] diameterPath;
        public long[] sumDist;       // rerooting: total distance from node v to every other node
        public int bestHub;          // node with the smallest sumDist (the 1-median)
        public int centroid;         // node whose biggest hanging piece is <= n/2
        public long maxIndependent;  // max total weight with no two neighbours both picked
        public boolean[] picked;
        public int[] parent, order;
    }

    public static Result analyze(int n, int[] eu, int[] ev, int[] ew, long[] nodeWeight) {
        int[] head = new int[n], nxt = new int[2 * eu.length], to = new int[2 * eu.length], wt = new int[2 * eu.length];
        for (int i = 0; i < n; i++) head[i] = -1;
        int ec = 0;
        for (int i = 0; i < eu.length; i++) {
            to[ec] = ev[i]; wt[ec] = ew[i]; nxt[ec] = head[eu[i]]; head[eu[i]] = ec++;
            to[ec] = eu[i]; wt[ec] = ew[i]; nxt[ec] = head[ev[i]]; head[ev[i]] = ec++;
        }
        Result r = new Result();
        int[] par = new int[n], order = new int[n], pw = new int[n];
        bfs(0, n, head, nxt, to, wt, par, order, pw, new long[n]);
        r.parent = par; r.order = order;

        // diameter: farthest from anywhere, then farthest from that
        long[] d0 = new long[n];
        bfs(0, n, head, nxt, to, wt, new int[n], new int[n], new int[n], d0);
        int a = 0;
        for (int i = 0; i < n; i++) if (d0[i] > d0[a]) a = i;
        long[] d1 = new long[n];
        int[] par1 = new int[n];
        bfs(a, n, head, nxt, to, wt, par1, new int[n], new int[n], d1);
        int b = a;
        for (int i = 0; i < n; i++) if (d1[i] > d1[b]) b = i;
        r.diameter = d1[b];
        int len = 1;
        for (int x = b; x != a; x = par1[x]) len++;
        r.diameterPath = new int[len];
        int p = len - 1;
        for (int x = b; ; x = par1[x]) { r.diameterPath[p--] = x; if (x == a) break; }

        // subtree sizes + down sums, children-first
        int[] cnt = new int[n];
        long[] down = new long[n];
        for (int i = 0; i < n; i++) cnt[i] = 1;
        for (int k = n - 1; k > 0; k--) {
            int v = order[k], u = par[v];
            cnt[u] += cnt[v];
            down[u] += down[v] + (long) pw[v] * cnt[v];
        }
        // rerooting, parents-first
        r.sumDist = new long[n];
        r.sumDist[0] = down[0];
        for (int k = 1; k < n; k++) {
            int v = order[k], u = par[v];
            r.sumDist[v] = r.sumDist[u] - (long) pw[v] * cnt[v] + (long) pw[v] * (n - cnt[v]);
        }
        r.bestHub = 0;
        for (int i = 0; i < n; i++) if (r.sumDist[i] < r.sumDist[r.bestHub]) r.bestHub = i;

        // centroid
        int[] big = new int[n];
        for (int k = 1; k < n; k++) { int v = order[k], u = par[v]; if (cnt[v] > big[u]) big[u] = cnt[v]; }
        r.centroid = 0;
        int bestMax = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            int mx = Math.max(big[i], n - cnt[i]);
            if (mx < bestMax) { bestMax = mx; r.centroid = i; }
        }

        // max-weight independent set
        long[] in = new long[n], out = new long[n];
        for (int i = 0; i < n; i++) in[i] = nodeWeight[i];
        for (int k = n - 1; k > 0; k--) {
            int v = order[k], u = par[v];
            in[u] += out[v];
            out[u] += Math.max(in[v], out[v]);
        }
        r.maxIndependent = Math.max(in[0], out[0]);
        r.picked = new boolean[n];
        boolean[] takeIn = new boolean[n];
        takeIn[0] = in[0] >= out[0];
        r.picked[0] = takeIn[0];
        for (int k = 1; k < n; k++) {
            int v = order[k], u = par[v];
            takeIn[v] = !takeIn[u] && in[v] >= out[v];
            r.picked[v] = takeIn[v];
        }
        return r;
    }

    private static void bfs(int root, int n, int[] head, int[] nxt, int[] to, int[] wt,
                            int[] par, int[] order, int[] pw, long[] dist) {
        boolean[] seen = new boolean[n];
        int qh = 0, qt = 0;
        order[qt++] = root; seen[root] = true; par[root] = root;
        while (qh < qt) {
            int u = order[qh++];
            for (int e = head[u]; e != -1; e = nxt[e]) {
                int v = to[e];
                if (seen[v]) continue;
                seen[v] = true; par[v] = u; pw[v] = wt[e]; dist[v] = dist[u] + wt[e];
                order[qt++] = v;
            }
        }
    }
}
