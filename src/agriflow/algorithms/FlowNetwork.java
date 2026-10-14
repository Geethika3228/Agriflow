package agriflow.algorithms;

/**
 * Flow network with residual edges (edge e and its reverse e^1 live side by side).
 *  - Edmonds-Karp : Ford-Fulkerson where each augmenting path is the SHORTEST one (BFS). O(V E^2)
 *  - Dinic        : BFS levels, then push a "blocking flow" along level-increasing edges. O(V^2 E)
 *  - min cut      : after max-flow, everything still reachable from the source in the residual graph
 *  - min-cost flow: successive shortest paths (SPFA / Bellman-Ford style) on edge costs
 */
public final class FlowNetwork {
    public static final int INF = 1_000_000_000;
    public final int n;
    private final int[] head, nxt, to, cap, orig, cost;
    private int m;

    // ---- trace for the visualiser ----
    public final IntList trace = new IntList();     // per augmentation: [pathLen, amount, pathCost, node0, node1, ...]
    public int augmentations;
    public int phases;                               // Dinic phases
    public final IntList phaseFlow = new IntList();  // flow pushed in each Dinic phase
    public final int[][] phaseLevels = new int[8][]; // BFS level array of the first 8 Dinic phases

    private int[] level, it;

    public FlowNetwork(int n, int maxEdges) {
        this.n = n;
        head = new int[n];
        for (int i = 0; i < n; i++) head[i] = -1;
        nxt = new int[2 * maxEdges]; to = new int[2 * maxEdges];
        cap = new int[2 * maxEdges]; orig = new int[2 * maxEdges]; cost = new int[2 * maxEdges];
    }

    /** adds u->v (capacity c, price w per unit) and its zero-capacity reverse; returns the forward edge id */
    public int addEdge(int u, int v, int c, int w) {
        to[m] = v; cap[m] = c; orig[m] = c; cost[m] = w; nxt[m] = head[u]; head[u] = m++;
        to[m] = u; cap[m] = 0; orig[m] = 0; cost[m] = -w; nxt[m] = head[v]; head[v] = m++;
        return m - 2;
    }

    public int edgeCount() { return m / 2; }
    public int from(int e) { return to[e ^ 1]; }
    public int dest(int e) { return to[e]; }
    public int capacity(int e) { return orig[e]; }
    public int flowOn(int e) { return orig[e] - cap[e]; }

    private void record(int s, int t, int[] prevEdge, int amount, int pathCost) {
        augmentations++;
        if (augmentations > 80) return;          // keep the trace small; the counter still counts everything
        int len = 1;
        for (int v = t; v != s; v = to[prevEdge[v] ^ 1]) len++;
        int[] nodes = new int[len];
        int p = len - 1;
        for (int v = t; ; v = to[prevEdge[v] ^ 1]) { nodes[p--] = v; if (v == s) break; }
        trace.add(len); trace.add(amount); trace.add(pathCost);
        for (int x : nodes) trace.add(x);
    }

    // ---------------------------------------------------------------- Edmonds-Karp
    public long edmondsKarp(int s, int t) {
        long flow = 0;
        int[] prevEdge = new int[n];
        int[] q = new int[n];
        while (true) {
            for (int i = 0; i < n; i++) prevEdge[i] = -1;
            int qh = 0, qt = 0;
            q[qt++] = s;
            prevEdge[s] = -2;
            while (qh < qt && prevEdge[t] == -1) {
                int u = q[qh++];
                for (int e = head[u]; e != -1; e = nxt[e])
                    if (cap[e] > 0 && prevEdge[to[e]] == -1) { prevEdge[to[e]] = e; q[qt++] = to[e]; }
            }
            if (prevEdge[t] == -1) break;
            int bottleneck = INF;
            for (int v = t; v != s; v = to[prevEdge[v] ^ 1]) bottleneck = Math.min(bottleneck, cap[prevEdge[v]]);
            for (int v = t; v != s; v = to[prevEdge[v] ^ 1]) { cap[prevEdge[v]] -= bottleneck; cap[prevEdge[v] ^ 1] += bottleneck; }
            flow += bottleneck;
            record(s, t, prevEdge, bottleneck, 0);
        }
        return flow;
    }

    // ---------------------------------------------------------------- Dinic
    public long dinic(int s, int t) {
        long flow = 0;
        level = new int[n];
        it = new int[n];
        int[] q = new int[n];
        while (true) {
            for (int i = 0; i < n; i++) level[i] = -1;
            int qh = 0, qt = 0;
            q[qt++] = s; level[s] = 0;
            while (qh < qt) {
                int u = q[qh++];
                for (int e = head[u]; e != -1; e = nxt[e])
                    if (cap[e] > 0 && level[to[e]] == -1) { level[to[e]] = level[u] + 1; q[qt++] = to[e]; }
            }
            if (level[t] == -1) break;
            if (phases < phaseLevels.length) {
                phaseLevels[phases] = new int[n];
                System.arraycopy(level, 0, phaseLevels[phases], 0, n);
            }
            phases++;
            for (int i = 0; i < n; i++) it[i] = head[i];
            long pushed = 0;
            int f;
            while ((f = dfs(s, t, INF)) > 0) pushed += f;
            phaseFlow.add((int) pushed);
            flow += pushed;
        }
        return flow;
    }

    private int dfs(int u, int t, int f) {
        if (u == t) return f;
        for (; it[u] != -1; it[u] = nxt[it[u]]) {
            int e = it[u], v = to[e];
            if (cap[e] > 0 && level[v] == level[u] + 1) {
                int d = dfs(v, t, Math.min(f, cap[e]));
                if (d > 0) { cap[e] -= d; cap[e ^ 1] += d; return d; }
            }
        }
        return 0;
    }

    /** nodes still reachable from s in the residual graph = the source side of a minimum cut (call after max-flow) */
    public boolean[] sourceSide(int s) {
        boolean[] seen = new boolean[n];
        int[] q = new int[n];
        int qh = 0, qt = 0;
        q[qt++] = s; seen[s] = true;
        while (qh < qt) {
            int u = q[qh++];
            for (int e = head[u]; e != -1; e = nxt[e])
                if (cap[e] > 0 && !seen[to[e]]) { seen[to[e]] = true; q[qt++] = to[e]; }
        }
        return seen;
    }

    // ---------------------------------------------------------------- min-cost max-flow (SSP)
    public long totalCost;

    /** push up to `limit` units from s to t, always along the currently cheapest path. Returns the flow pushed. */
    public long minCostFlow(int s, int t, long limit) {
        long flow = 0;
        totalCost = 0;
        long[] dist = new long[n];
        int[] prevEdge = new int[n];
        boolean[] inQ = new boolean[n];
        int[] q = new int[n + 1];
        while (flow < limit) {
            for (int i = 0; i < n; i++) { dist[i] = Long.MAX_VALUE; prevEdge[i] = -1; inQ[i] = false; }
            int qh = 0, qt = 0, size = 0;
            dist[s] = 0; q[qt] = s; qt = (qt + 1) % (n + 1); size++; inQ[s] = true;
            while (size > 0) {
                int u = q[qh]; qh = (qh + 1) % (n + 1); size--; inQ[u] = false;
                for (int e = head[u]; e != -1; e = nxt[e])
                    if (cap[e] > 0 && dist[u] + cost[e] < dist[to[e]]) {
                        dist[to[e]] = dist[u] + cost[e];
                        prevEdge[to[e]] = e;
                        if (!inQ[to[e]]) { inQ[to[e]] = true; q[qt] = to[e]; qt = (qt + 1) % (n + 1); size++; }
                    }
            }
            if (dist[t] == Long.MAX_VALUE) break;
            long amount = limit - flow;
            for (int v = t; v != s; v = to[prevEdge[v] ^ 1]) amount = Math.min(amount, cap[prevEdge[v]]);
            for (int v = t; v != s; v = to[prevEdge[v] ^ 1]) { cap[prevEdge[v]] -= (int) amount; cap[prevEdge[v] ^ 1] += (int) amount; }
            flow += amount;
            totalCost += amount * dist[t];
            record(s, t, prevEdge, (int) amount, (int) dist[t]);
        }
        return flow;
    }
}
