package agriflow.server;

import agriflow.algorithms.*;

/** Endpoints for Module 4 (flow), Module 5 (NP + approximation) and Module 6 (randomised + parallel). */
final class ApiGraph {
    private final Data d;

    ApiGraph(Data d) { this.d = d; }

    /** name -> index, in order of first appearance */
    private static final class Names {
        String[] n = new String[8];
        int c;
        int idx(String s) {
            s = s.trim();
            for (int i = 0; i < c; i++) if (n[i].equals(s)) return i;
            if (c == n.length) { String[] b = new String[c * 2]; System.arraycopy(n, 0, b, 0, c); n = b; }
            n[c] = s;
            return c++;
        }
        int find(String s) {
            s = s.trim();
            for (int i = 0; i < c; i++) if (n[i].equals(s)) return i;
            return -1;
        }
        String[] array() { String[] r = new String[c]; System.arraycopy(n, 0, r, 0, c); return r; }
    }

    private static String[] splitPair(String item, String name) {
        int g = item.indexOf('>');
        if (g < 1) throw new Q.Bad("'" + item + "' should look like From>To in " + name);
        return new String[]{item.substring(0, g).trim(), item.substring(g + 1).trim()};
    }

    private static int num(String s, String what) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { throw new Q.Bad(what + " must be a whole number, got '" + s + "'"); }
    }

    // =================================================================== Module 4
    String flow(Q q) {
        String[] es = q.list("edges", ",", new String[]{"Reservoir>Canal A:20", "Reservoir>Canal B:15", "Canal A>Canal B:4", "Canal A>Plot 1:8",
                "Canal A>Plot 2:7", "Canal B>Plot 2:6", "Canal B>Plot 3:9", "Plot 1>Field Tank:9", "Plot 2>Field Tank:10", "Plot 3>Field Tank:8"});
        if (es.length > 120) throw new Q.Bad("max 120 pipes");
        String algo = q.s("algo", "dinic");
        Names nm = new Names();
        int m = es.length;
        int[] eu = new int[m], ev = new int[m], ec = new int[m];
        for (int i = 0; i < m; i++) {
            int colon = es[i].lastIndexOf(':');
            if (colon < 0) throw new Q.Bad("'" + es[i] + "' needs a capacity, like A>B:10");
            String[] p = splitPair(es[i].substring(0, colon), "edges");
            eu[i] = nm.idx(p[0]); ev[i] = nm.idx(p[1]); ec[i] = num(es[i].substring(colon + 1), "capacity");
            if (ec[i] < 0 || ec[i] > 100000) throw new Q.Bad("capacity must be 0..100000");
        }
        if (nm.c > 60) throw new Q.Bad("max 60 nodes");
        int s = q.has("s") ? nm.find(q.s("s", "")) : eu[0], t = q.has("t") ? nm.find(q.s("t", "")) : ev[m - 1];
        if (s < 0 || t < 0) throw new Q.Bad("source/sink name not found in the pipes");
        if (s == t) throw new Q.Bad("source and sink must differ");
        FlowNetwork net = new FlowNetwork(nm.c, m);
        int[] id = new int[m];
        for (int i = 0; i < m; i++) id[i] = net.addEdge(eu[i], ev[i], ec[i], 0);
        long flow = algo.equals("ek") ? net.edmondsKarp(s, t) : net.dinic(s, t);
        boolean[] side = net.sourceSide(s);
        Json edges = Json.arr();
        long cutCap = 0;
        for (int i = 0; i < m; i++) {
            boolean cut = side[eu[i]] && !side[ev[i]] && ec[i] > 0;
            if (cut) cutCap += ec[i];
            edges.add(Json.obj().put("u", eu[i]).put("v", ev[i]).put("cap", ec[i]).put("flow", net.flowOn(id[i])).put("cut", cut));
        }
        Json trace = Json.arr();
        int[] tr = net.trace.toArray();
        String[] names = nm.array();
        for (int i = 0; i < tr.length; ) {
            int len = tr[i];
            int[] path = new int[len];
            System.arraycopy(tr, i + 3, path, 0, len);
            trace.add(Json.obj().put("path", path).put("amount", tr[i + 1]));
            i += 3 + len;
        }
        Json levels = Json.arr();
        for (int[] lv : net.phaseLevels) if (lv != null) levels.add(lv);
        Json o = Json.obj().put("algo", algo.equals("ek") ? "Edmonds-Karp" : "Dinic").put("nodes", names).put("s", s).put("t", t)
                .put("edges", edges).put("maxFlow", flow).put("cutCapacity", cutCap).put("sourceSide", side)
                .put("complexity", algo.equals("ek") ? "O(V E^2)" : "O(V^2 E)");
        if (algo.equals("ek")) o.put("augmentations", net.augmentations).put("trace", trace);
        else o.put("phases", net.phases).put("phaseFlow", net.phaseFlow.toArray()).put("levels", levels);
        return o.toString();
    }

    String matching(Q q) {
        String[] es = q.list("edges", ",", new String[]{"Ravi>Tractor", "Ravi>Sprayer", "Sita>Sprayer", "Sita>Pump", "Joseph>Tractor",
                "Joseph>Harvester", "Meena>Harvester", "Meena>Pump", "Arjun>Pump", "Kavya>Tractor"});
        Names l = new Names(), r = new Names();
        int[] eu = new int[es.length], ev = new int[es.length];
        for (int i = 0; i < es.length; i++) {
            String[] p = splitPair(es[i], "edges");
            eu[i] = l.idx(p[0]); ev[i] = r.idx(p[1]);
        }
        if (l.c > 40 || r.c > 40) throw new Q.Bad("max 40 on each side");
        int[][] adj = new int[l.c][];
        for (int a = 0; a < l.c; a++) {
            int c = 0;
            for (int e : eu) if (e == a) c++;
            adj[a] = new int[c];
            c = 0;
            for (int i = 0; i < es.length; i++) if (eu[i] == a) adj[a][c++] = ev[i];
        }
        FlowApps.Matching mt = FlowApps.bipartite(l.c, r.c, adj);
        Json pairs = Json.arr();
        String[] ln = l.array(), rn = r.array();
        int unmatched = 0;
        for (int a = 0; a < l.c; a++) {
            if (mt.matchL[a] >= 0) pairs.add(Json.obj().put("left", a).put("right", mt.matchL[a]));
            else unmatched++;
        }
        return Json.obj().put("left", ln).put("right", rn).put("edgesFrom", eu).put("edgesTo", ev).put("size", mt.size).put("unmatched", unmatched)
                .put("pairs", pairs).put("coverL", mt.coverL).put("coverR", mt.coverR).put("phases", mt.dinicPhases).toString();
    }

    String mcmf(Q q) {
        String[] sup = q.list("supply", ",", new String[]{"Ravi:40", "Sita:30", "Joseph:25"});
        String[] dem = q.list("demand", ",", new String[]{"Market A:45", "Market B:35", "Market C:30"});
        String[] rows = q.list("cost", ";", new String[]{"4,6,9", "3,5,8", "5,4,2"});
        int ns = sup.length, nd = dem.length;
        if (ns > 12 || nd > 12) throw new Q.Bad("max 12 farmers and 12 markets");
        if (rows.length != ns) throw new Q.Bad("need one cost row per farmer");
        String[] sn = new String[ns], dn = new String[nd];
        int[] sv = new int[ns], dv = new int[nd];
        for (int i = 0; i < ns; i++) { int c = sup[i].lastIndexOf(':'); sn[i] = sup[i].substring(0, c).trim(); sv[i] = num(sup[i].substring(c + 1), "supply"); }
        for (int i = 0; i < nd; i++) { int c = dem[i].lastIndexOf(':'); dn[i] = dem[i].substring(0, c).trim(); dv[i] = num(dem[i].substring(c + 1), "demand"); }
        int[][] cost = new int[ns][nd];
        for (int i = 0; i < ns; i++) {
            String[] p = rows[i].split(",");
            if (p.length != nd) throw new Q.Bad("each cost row needs " + nd + " numbers");
            for (int j = 0; j < nd; j++) cost[i][j] = num(p[j], "cost");
        }
        int s = ns + nd, t = s + 1;
        FlowNetwork net = new FlowNetwork(ns + nd + 2, ns + nd + ns * nd);
        for (int i = 0; i < ns; i++) net.addEdge(s, i, sv[i], 0);
        for (int j = 0; j < nd; j++) net.addEdge(ns + j, t, dv[j], 0);
        int[][] id = new int[ns][nd];
        for (int i = 0; i < ns; i++) for (int j = 0; j < nd; j++) id[i][j] = net.addEdge(i, ns + j, FlowNetwork.INF, cost[i][j]);
        long flow = net.minCostFlow(s, t, FlowNetwork.INF);
        int[][] fl = new int[ns][nd];
        for (int i = 0; i < ns; i++) for (int j = 0; j < nd; j++) fl[i][j] = net.flowOn(id[i][j]);
        Json tr = Json.arr();
        int[] raw = net.trace.toArray();
        for (int i = 0; i < raw.length; ) {
            int len = raw[i];
            int[] path = new int[len];
            System.arraycopy(raw, i + 3, path, 0, len);
            tr.add(Json.obj().put("path", path).put("amount", raw[i + 1]).put("unitCost", raw[i + 2]));
            i += 3 + len;
        }
        int totS = 0, totD = 0;
        for (int x : sv) totS += x;
        for (int x : dv) totD += x;
        return Json.obj().put("farmers", sn).put("markets", dn).put("supply", sv).put("demand", dv).put("cost", cost).put("flow", fl)
                .put("totalFlow", flow).put("totalCost", net.totalCost).put("supplyTotal", totS).put("demandTotal", totD)
                .put("steps", tr).put("rounds", net.augmentations).toString();
    }

    String projects(Q q) {
        String[] items = q.list("items", ",", new String[]{"Grow tomatoes:50", "Grow chilli:40", "Grow turmeric:25", "Drip system:-30", "Polyhouse:-60",
                "Mulching kit:-8", "Sell at mandi:-10"});
        String[] deps = q.list("deps", ",", new String[]{"Grow tomatoes>Drip system", "Grow tomatoes>Polyhouse", "Grow tomatoes>Sell at mandi",
                "Grow chilli>Drip system", "Grow chilli>Sell at mandi", "Grow turmeric>Mulching kit", "Grow turmeric>Sell at mandi"});
        if (items.length > 30) throw new Q.Bad("max 30 items");
        Names nm = new Names();
        long[] profit = new long[items.length];
        for (int i = 0; i < items.length; i++) {
            int c = items[i].lastIndexOf(':');
            if (c < 0) throw new Q.Bad("items look like Name:profit (negative = costs money)");
            nm.idx(items[i].substring(0, c));
            profit[i] = num(items[i].substring(c + 1), "profit");
        }
        int[] fr = new int[deps.length], to = new int[deps.length];
        for (int i = 0; i < deps.length; i++) {
            String[] p = splitPair(deps[i], "deps");
            fr[i] = nm.find(p[0]); to[i] = nm.find(p[1]);
            if (fr[i] < 0 || to[i] < 0) throw new Q.Bad("dependency '" + deps[i] + "' names an item that isn't in the list");
        }
        FlowApps.Selection sel = FlowApps.projectSelection(profit, fr, to);
        long upper = 0;
        for (long p : profit) if (p > 0) upper += p;
        return Json.obj().put("items", nm.array()).put("profit", profit).put("from", fr).put("to", to).put("chosen", sel.chosen)
                .put("best", sel.profit).put("minCut", sel.cutValue).put("sumOfPositives", upper).toString();
    }

    // =================================================================== Module 5
    private static void parseEdges(String[] es, int n, int[] eu, int[] ev) {
        for (int i = 0; i < es.length; i++) {
            String[] p = es[i].split("-");
            if (p.length != 2) throw new Q.Bad("edges look like 0-1,1-2");
            eu[i] = num(p[0], "node"); ev[i] = num(p[1], "node");
            if (eu[i] < 0 || ev[i] < 0 || eu[i] >= n || ev[i] >= n || eu[i] == ev[i]) throw new Q.Bad("edge " + es[i] + " must use two different nodes in 0.." + (n - 1));
        }
    }

    String vcover(Q q) {
        int n = q.i("n", 10, 2, 20);
        String[] es = q.list("edges", ",", new String[]{"0-1", "0-2", "0-3", "0-4", "1-2", "3-4", "4-5", "5-6", "5-7", "6-7", "7-8", "8-9"});
        int[] eu = new int[es.length], ev = new int[es.length];
        parseEdges(es, n, eu, ev);
        boolean[] ex = VertexCover.exact(n, eu, ev), ap = VertexCover.approx2(n, eu, ev);
        int opt = VertexCover.size(ex), apx = VertexCover.size(ap);
        int k = q.i("k", opt, 0, n);
        VertexCover.Fpt f = VertexCover.fpt(n, eu, ev, k);
        // the edges the 2-approximation grabbed (a maximal matching)
        boolean[] c = new boolean[n];
        IntList mEdges = new IntList();
        for (int i = 0; i < eu.length; i++) if (!c[eu[i]] && !c[ev[i]]) { c[eu[i]] = true; c[ev[i]] = true; mEdges.add(i); }
        boolean[] indep = new boolean[n];
        for (int i = 0; i < n; i++) indep[i] = !ex[i];
        boolean clique = true;
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++)
            if (indep[i] && indep[j]) for (int e = 0; e < eu.length; e++)
                if ((eu[e] == i && ev[e] == j) || (eu[e] == j && ev[e] == i)) clique = false;
        Json fj = Json.obj().put("k", k).put("found", f.cover != null).put("branches", f.branches).put("forced", f.forced)
                .put("kernelEdges", f.kernelEdges).put("kernelRejected", f.kernelRejected).put("bound", (long) Math.pow(2, k + 1));
        if (f.cover != null) fj.put("cover", f.cover);
        return Json.obj().put("n", n).put("eu", eu).put("ev", ev).put("exact", ex).put("exactSize", opt).put("approx", ap).put("approxSize", apx)
                .put("ratio", opt == 0 ? 1.0 : (double) apx / opt).put("matchingEdges", mEdges.toArray()).put("independent", indep)
                .put("independentSize", n - opt).put("isCliqueInComplement", clique).put("fpt", fj).toString();
    }

    String knapsack(Q q) {
        String[] items = q.list("items", ",", new String[]{"Rice:60:20", "Wheat:50:15", "Tomato:70:25", "Cotton:55:30", "Maize:40:12", "Chilli:65:18",
                "Potato:45:22", "Soybean:35:10", "Mustard:30:9", "Turmeric:75:28"});
        int cap = q.i("cap", 80, 1, 5000);
        double eps = q.d("eps", 0.2, 0.05, 1.0);
        int n = items.length;
        if (n > 25) throw new Q.Bad("max 25 crops");
        String[] names = new String[n];
        int[] v = new int[n], w = new int[n];
        for (int i = 0; i < n; i++) {
            String[] p = items[i].split(":");
            if (p.length != 3) throw new Q.Bad("items look like Name:value:cost");
            names[i] = p[0].trim(); v[i] = num(p[1], "value"); w[i] = num(p[2], "cost");
            if (v[i] < 1 || w[i] < 1 || v[i] > 100000 || w[i] > 5000) throw new Q.Bad("values 1..100000 and costs 1..5000");
        }
        Knapsack.Result ex = Knapsack.exact(v, w, cap), gr = Knapsack.greedy(v, w, cap), gh = Knapsack.greedyHalf(v, w, cap), fp = Knapsack.fptas(v, w, cap, eps);
        return Json.obj().put("names", names).put("values", v).put("weights", w).put("cap", cap).put("eps", eps)
                .put("exact", kn(ex, ex)).put("greedy", kn(gr, ex)).put("greedyHalf", kn(gh, ex)).put("fptas", kn(fp, ex))
                .put("dpCells", (long) n * (cap + 1)).toString();
    }

    private static Json kn(Knapsack.Result r, Knapsack.Result opt) {
        return Json.obj().put("value", r.value).put("weight", r.weight).put("take", r.take).put("ratio", opt.value == 0 ? 1.0 : (double) r.value / opt.value);
    }

    String subsetsum(Q q) {
        int[] nums = q.ints("nums", new int[]{12, 7, 5, 9, 3, 14, 8});
        int target = q.i("target", 30, 0, 20000);
        if (nums.length > 30) throw new Q.Bad("max 30 numbers");
        for (int x : nums) if (x < 1 || x > 20000) throw new Q.Bad("numbers must be 1..20000");
        int[] r = Knapsack.subsetSum(nums, target);
        Json o = Json.obj().put("nums", nums).put("target", target).put("possible", r != null);
        if (r != null) { int s = 0; for (int i : r) s += nums[i]; o.put("indices", r).put("sum", s); }
        return o.toString();
    }

    // =================================================================== Module 6
    String prime(Q q) {
        long n = q.l("n", 561);
        if (n < 0) throw new Q.Bad("n must be 0 or more");
        int rounds = q.i("rounds", 5, 1, 40);
        Rng rng = new Rng(q.l("seed", System.nanoTime()));
        boolean det = MillerRabin.isPrime(n);
        boolean prob = MillerRabin.probablyPrime(n, rounds, rng);
        Json tests = Json.arr();
        long[] bases = {2, 3, 5, 7, 11, 13, 17, 19};
        boolean usable = n > 3 && (n & 1) == 1;
        if (usable) for (long a : bases) {
            if (a >= n) continue;
            tests.add(Json.obj().put("a", a).put("witness", MillerRabin.isWitness(a, n)).put("fermatPasses", MillerRabin.fermatPasses(a, n)));
        }
        Json o = Json.obj().put("n", n).put("isPrime", det).put("probablyPrime", prob).put("rounds", rounds).put("tests", tests)
                .put("errorBound", Math.pow(4, -rounds));
        if (usable) { int s = MillerRabin.twoPower(n); o.put("s", s).put("d", (n - 1) >> s); }
        if (n < 2_000_000_000_000L) o.put("trialDivision", MillerRabin.trialDivision(n));
        if (usable && n < 100000 && !det) {
            boolean allFermat = true;
            long coprime = 0;
            for (long a = 2; a < n; a++) if (gcd(a, n) == 1) { coprime++; if (!MillerRabin.fermatPasses(a, n)) allFermat = false; }
            o.put("carmichael", allFermat && coprime > 0);
        }
        return o.toString();
    }

    private static long gcd(long a, long b) { return b == 0 ? a : gcd(b, a % b); }

    String qsort(Q q) {
        int n = q.i("n", 2000, 10, 8000), trials = q.i("trials", 100, 1, 300);
        String mode = q.s("mode", "sorted");
        Rng rng = new Rng(q.l("seed", 4242));
        int[] base = new int[n];
        for (int i = 0; i < n; i++)
            base[i] = mode.equals("sorted") ? i : mode.equals("reversed") ? n - i : mode.equals("dups") ? rng.nextInt(5) : rng.nextInt(1_000_000);
        long[] first = RandomizedSort.sort(base.clone(), false, rng);
        long min = Long.MAX_VALUE, max = 0, sum = 0, dsum = 0;
        for (int t = 0; t < trials; t++) {
            int[] c = base.clone();
            long[] r = RandomizedSort.sort(c, true, rng);
            if (!RandomizedSort.isSorted(c)) throw new IllegalStateException("randomised quicksort produced unsorted output");
            min = Math.min(min, r[0]); max = Math.max(max, r[0]); sum += r[0]; dsum += r[1];
        }
        double nlog = n * Math.log(n) / Math.log(2);
        double harmonic = 0;
        for (int i = 1; i <= n; i++) harmonic += 1.0 / i;
        double exactExpected = 2.0 * (n + 1) * harmonic - 4.0 * n;     // E[comparisons] = 2(n+1)H_n - 4n
        return Json.obj().put("n", n).put("mode", mode).put("trials", trials)
                .put("firstPivot", Json.obj().put("comparisons", first[0]).put("depth", first[1]))
                .put("randomPivot", Json.obj().put("min", min).put("avg", (double) sum / trials).put("max", max).put("avgDepth", (double) dsum / trials))
                .put("nLog2n", nlog).put("expected", exactExpected).put("quadratic", (long) n * (n - 1) / 2).toString();
    }

    String hash(Q q) {
        int m = q.i("m", 128, 4, 5000), trials = q.i("trials", 200, 1, 2000);
        Rng rng = new Rng(q.l("seed", System.nanoTime()));
        String[] words = d.vocab;
        long[] keys = new long[words.length];
        int kc = 0;
        for (String w : words) {
            long k = PerfectHash.keyOf(w);
            boolean dup = false;
            for (int i = 0; i < kc; i++) if (keys[i] == k) dup = true;
            if (!dup) keys[kc++] = k;
        }
        long[] ks = new long[kc];
        System.arraycopy(keys, 0, ks, 0, kc);
        long mn = Long.MAX_VALUE, mx = 0;
        double sum = 0;
        for (int t = 0; t < trials; t++) {
            long c = PerfectHash.collisions(ks, m, 1 + rng.nextLong(PerfectHash.P - 1), rng.nextLong(PerfectHash.P));
            mn = Math.min(mn, c); mx = Math.max(mx, c); sum += c;
        }
        int[] cnt = new int[m];
        for (String w : words) cnt[PerfectHash.sumHash(w, m)]++;
        long sumPairs = 0;
        int worst = 0;
        for (int c : cnt) { sumPairs += (long) c * (c - 1) / 2; worst = Math.max(worst, c); }
        // adversarial input: all 120 anagrams of "crops". "Add up the letters" puts every one in the SAME bucket.
        String[] ana = new String[120];
        perms("crops".toCharArray(), 0, ana, new int[1]);
        long[] anaKeys = new long[ana.length];
        int anaSumBucket0 = PerfectHash.sumHash(ana[0], m);
        boolean sameBucket = true;
        for (int i = 0; i < ana.length; i++) { anaKeys[i] = PerfectHash.keyOf(ana[i]); if (PerfectHash.sumHash(ana[i], m) != anaSumBucket0) sameBucket = false; }
        double anaSum = 0;
        for (int t = 0; t < trials; t++) anaSum += PerfectHash.collisions(anaKeys, m, 1 + rng.nextLong(PerfectHash.P - 1), rng.nextLong(PerfectHash.P));
        Json anaJson = Json.obj().put("keys", ana.length).put("sumHashAllSameBucket", sameBucket)
                .put("sumHashCollidingPairs", (long) ana.length * (ana.length - 1) / 2)
                .put("universalAvgPairs", anaSum / trials).put("bound", (double) ana.length * (ana.length - 1) / 2 / m);
        PerfectHash ph = PerfectHash.build(ks, rng);
        boolean all = true;
        for (long k : ks) if (!ph.contains(k)) all = false;
        int falseHits = 0;
        for (int i = 0; i < 5000; i++) { long k = rng.nextLong(PerfectHash.P); if (ph.contains(k)) { boolean real = false; for (long x : ks) if (x == k) real = true; if (!real) falseHits++; } }
        int maxBucket = 0;
        int[] bs = new int[ph.m];
        for (long k : ks) bs[PerfectHash.hash(ph.a1, ph.b1, k, ph.m)]++;
        for (int b : bs) maxBucket = Math.max(maxBucket, b);
        String look = q.s("word", "blight").toLowerCase();
        Json o = Json.obj().put("keys", kc).put("m", m).put("trials", trials)
                .put("universal", Json.obj().put("min", mn).put("avg", sum / trials).put("max", mx).put("bound", (double) kc * (kc - 1) / 2 / m))
                .put("sumHash", Json.obj().put("collidingPairs", sumPairs).put("worstBucket", worst))
                .put("anagrams", anaJson)
                .put("fks", Json.obj().put("n", kc).put("attemptsLevel1", ph.attempts1).put("attemptsLevel2", ph.attempts2).put("space", ph.totalSpace)
                        .put("maxBucket", maxBucket).put("allFound", all).put("falseHits", falseHits).put("lookupWord", look)
                        .put("lookupFound", ph.contains(PerfectHash.keyOf(look))));
        return o.toString();
    }

    private static void perms(char[] a, int k, String[] out, int[] c) {
        if (k == a.length) { out[c[0]++] = new String(a); return; }
        for (int i = k; i < a.length; i++) {
            char t = a[k]; a[k] = a[i]; a[i] = t;
            perms(a, k + 1, out, c);
            t = a[k]; a[k] = a[i]; a[i] = t;
        }
    }

    String reservoir(Q q) {
        int n = q.i("n", 40, 2, 500), k = q.i("k", 5, 1, n), trials = q.i("trials", 20000, 100, 100000);
        Rng rng = new Rng(q.l("seed", System.nanoTime()));
        int[] cnt = Reservoir.inclusionCounts(n, k, trials, rng);
        double expect = (double) trials * k / n;
        double worst = 0;
        for (int c : cnt) worst = Math.max(worst, Math.abs(c - expect) / expect);
        return Json.obj().put("n", n).put("k", k).put("trials", trials).put("counts", cnt).put("expected", expect)
                .put("worstDeviationPct", worst * 100).put("oneSample", Reservoir.sample(n, k, rng)).toString();
    }

    private static long best(Runnable r, int runs) {
        long b = Long.MAX_VALUE;
        for (int i = 0; i < runs; i++) { long a = System.nanoTime(); r.run(); b = Math.min(b, System.nanoTime() - a); }
        return b;
    }

    String parallel(Q q) {
        int n = q.i("n", 1_000_000, 1000, 4_000_000);
        int runs = n > 2_000_000 ? 2 : 3;
        Rng rng = new Rng(2024);
        final int[] rain = new int[n];
        for (int i = 0; i < n; i++) rain[i] = rng.nextInt(100);
        int[] threadList = {1, 2, 4, 8};
        Json ops = Json.arr();
        for (int op = 0; op < 3; op++) {
            String name = op == 0 ? "Parallel reduce (total rainfall)" : op == 1 ? "Parallel scan (running total, Blelloch)" : "Parallel merge sort (rank fields by yield)";
            long[] ws = op == 0 ? ParallelOps.reduceWorkSpan(n) : op == 1 ? ParallelOps.scanWorkSpan(n) : ParallelOps.sortWorkSpan(n);
            final long[] wantSum = {ParallelOps.sumSeq(rain)};
            final long[] wantScan = op == 1 ? ParallelOps.scanSeq(rain) : null;
            final int[] wantSort = op == 2 ? rain.clone() : null;
            if (wantSort != null) ParallelOps.mergeSortSeq(wantSort);
            final int fop = op;
            long seq = best(() -> {
                if (fop == 0) ParallelOps.sumSeq(rain);
                else if (fop == 1) ParallelOps.scanSeq(rain);
                else { int[] c = rain.clone(); ParallelOps.mergeSortSeq(c); }
            }, runs);
            Json rj = Json.arr();
            for (int th : threadList) {
                final boolean[] correct = {true};
                long t = best(() -> {
                    if (fop == 0) correct[0] = ParallelOps.sumParallel(rain, th) == wantSum[0];
                    else if (fop == 1) { long[] r = ParallelOps.scanParallel(rain, th); correct[0] = r[r.length - 1] == wantScan[wantScan.length - 1] && r[n / 2] == wantScan[n / 2]; }
                    else { int[] c = rain.clone(); ParallelOps.mergeSortParallel(c, th); boolean ok = true; for (int i = 0; i < n; i += 997) if (c[i] != wantSort[i]) ok = false; correct[0] = ok; }
                }, runs);
                double predicted = (double) ws[0] / ParallelOps.brentBound(ws[0], ws[1], th);
                rj.add(Json.obj().put("threads", th).put("millis", t / 1e6).put("speedup", (double) seq / t).put("brentSpeedup", predicted).put("correct", correct[0]));
            }
            ops.add(Json.obj().put("name", name).put("work", ws[0]).put("span", ws[1]).put("parallelism", (double) ws[0] / ws[1])
                    .put("seqMillis", seq / 1e6).put("runs", rj));
        }
        return Json.obj().put("n", n).put("cpus", Runtime.getRuntime().availableProcessors()).put("ops", ops).toString();
    }
}
