package agriflow.algorithms;

/** Real problems that turn into max-flow / min-cut: matching, assignment, project selection. */
public final class FlowApps {
    private FlowApps() {}

    public static final class Matching {
        public int size;
        public int[] matchL;         // matchL[l] = right partner or -1
        public boolean[] coverL, coverR;   // Koenig: a minimum vertex cover with exactly |matching| vertices
        public int dinicPhases;
    }

    /** bipartite matching = max-flow with unit capacities: source->left->right->sink */
    public static Matching bipartite(int nl, int nr, int[][] adj) {
        int s = nl + nr, t = s + 1;
        int edges = nl + nr;
        for (int[] a : adj) edges += a.length;
        FlowNetwork net = new FlowNetwork(nl + nr + 2, edges);
        for (int l = 0; l < nl; l++) net.addEdge(s, l, 1, 0);
        for (int r = 0; r < nr; r++) net.addEdge(nl + r, t, 1, 0);
        int[][] ids = new int[nl][];
        for (int l = 0; l < nl; l++) {
            ids[l] = new int[adj[l].length];
            for (int k = 0; k < adj[l].length; k++) ids[l][k] = net.addEdge(l, nl + adj[l][k], 1, 0);
        }
        Matching res = new Matching();
        res.size = (int) net.dinic(s, t);
        res.dinicPhases = net.phases;
        res.matchL = new int[nl];
        for (int l = 0; l < nl; l++) {
            res.matchL[l] = -1;
            for (int k = 0; k < adj[l].length; k++) if (net.flowOn(ids[l][k]) > 0) res.matchL[l] = adj[l][k];
        }
        boolean[] z = net.sourceSide(s);
        res.coverL = new boolean[nl]; res.coverR = new boolean[nr];
        for (int l = 0; l < nl; l++) res.coverL[l] = !z[l];
        for (int r = 0; r < nr; r++) res.coverR[r] = z[nl + r];
        return res;
    }

    public static final class Selection {
        public long profit;
        public boolean[] chosen;
        public long cutValue;
    }

    /**
     * Project selection: item i has profit[i] (negative = it costs money). Picking item i forces you to also pick every j in
     * requires(i). Maximise total profit. Min-cut: source->i (profit), i->sink (cost), i->j infinite.
     */
    public static Selection projectSelection(long[] profit, int[] from, int[] to) {
        int n = profit.length, s = n, t = n + 1;
        FlowNetwork net = new FlowNetwork(n + 2, n + from.length);
        long positive = 0;
        for (int i = 0; i < n; i++) {
            if (profit[i] > 0) { net.addEdge(s, i, (int) profit[i], 0); positive += profit[i]; }
            else if (profit[i] < 0) net.addEdge(i, t, (int) -profit[i], 0);
        }
        for (int k = 0; k < from.length; k++) net.addEdge(from[k], to[k], FlowNetwork.INF, 0);
        Selection res = new Selection();
        res.cutValue = net.dinic(s, t);
        res.profit = positive - res.cutValue;
        boolean[] side = net.sourceSide(s);
        res.chosen = new boolean[n];
        for (int i = 0; i < n; i++) res.chosen[i] = side[i];
        return res;
    }
}
