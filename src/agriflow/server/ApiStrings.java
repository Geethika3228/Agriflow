package agriflow.server;

import agriflow.algorithms.*;

/** Endpoints for Module 1 (the engine), Module 2 (string algorithms) and Module 3 (dynamic programming). */
final class ApiStrings {
    private final Data d;

    ApiStrings(Data d) { this.d = d; }

    // ------------------------------------------------------------------ helpers
    private int[] find(String algo, String text, String pat) {
        switch (algo.toLowerCase()) {
            case "naive": return NaiveSearch.findAll(text, pat);
            case "z": return ZAlgorithm.findAll(text, pat);
            case "rabin-karp": return RabinKarp.findAll(text, pat);
            default: return KMP.findAll(text, pat);
        }
    }

    private static String complexity(String algo) {
        switch (algo.toLowerCase()) {
            case "naive": return "O(n*m) worst case";
            case "z": return "O(n+m)";
            case "rabin-karp": return "Average O(n+m); worst case can degrade";
            default: return "O(n+m)";
        }
    }

    private String snippet(int pos, int len) {
        int from = Math.max(0, pos - 18), to = Math.min(d.corpus.length(), pos + len + 18);
        return d.corpus.substring(from, to);
    }

    private static void needLen(String s, String name, int max) {
        if (s.length() > max) throw new Q.Bad("'" + name + "' is too long (max " + max + " characters)");
    }

    // ------------------------------------------------------------------ Module 1
    String engine() {
        String[][] rows = {
            {"Pattern search", "KMP / Z / Rabin-Karp / Aho-Corasick", "String algorithms", "O(n+m)", "2"},
            {"Fuzzy matching (typos)", "Wagner-Fischer edit distance", "Dynamic programming", "O(m*n)", "3"},
            {"Document similarity", "Suffix array + LCP (longest common substring)", "Suffix structures", "O(n log^2 n)", "2"},
            {"Water / citation flow", "Edmonds-Karp, Dinic, min-cut", "Max-flow", "O(V E^2), O(V^2 E)", "4"},
            {"Scheduling / routing", "Held-Karp, vertex cover, knapsack + approximations", "NP-hard + approximation", "O(2^n n^2) exact", "5"},
            {"Large-prime testing", "Miller-Rabin", "Randomised algorithms", "O(k log^2 n)", "6"}};
        Json a = Json.arr();
        for (String[] r : rows)
            a.add(Json.obj().put("query", r[0]).put("algorithm", r[1]).put("family", r[2]).put("cost", r[3]).put("module", Integer.parseInt(r[4])));
        return Json.obj().put("records", d.records.length).put("corpusChars", d.corpus.length()).put("vocabulary", d.vocab.length)
                .put("rules", "Engine package uses only arrays + java.lang. No java.util collections, no libraries.")
                .put("classes", a).toString();
    }

    String health() {
        return Json.obj().put("status", "ok").put("records", d.records.length).put("corpusChars", d.corpus.length()).toString();
    }

    String similar(Q q) {
        int id = q.i("id", 1, 1, d.records.length) - 1;
        String base = d.records[id].searchableText().toLowerCase();
        int n = d.records.length;
        int[] lcs = new int[n], at = new int[n], order = new int[n];
        for (int j = 0; j < n; j++) {
            order[j] = j;
            if (j == id) { lcs[j] = -1; continue; }
            int[] r = SuffixTools.longestCommon(base, d.records[j].searchableText().toLowerCase());
            lcs[j] = r[2]; at[j] = r[0];
        }
        for (int i = 1; i < n; i++) {                 // insertion sort, longest first
            int x = order[i], j = i - 1;
            while (j >= 0 && lcs[order[j]] < lcs[x]) { order[j + 1] = order[j]; j--; }
            order[j + 1] = x;
        }
        Json a = Json.arr();
        for (int k = 0; k < n; k++) {
            int j = order[k];
            if (j == id) continue;
            a.add(Json.obj().put("id", j + 1).put("crop", d.records[j].getCrop()).put("lcs", lcs[j])
                    .put("shared", base.substring(at[j], at[j] + lcs[j])));
        }
        return Json.obj().put("record", d.records[id].getCrop()).put("complexity", "O(n log^2 n) per pair (suffix array of a#b)")
                .put("ranked", a).toString();
    }

    // ------------------------------------------------------------------ Module 2
    String search(Q q) {
        String query = q.s("q", "").toLowerCase();
        String algo = q.s("algorithm", "kmp");
        if (query.isEmpty()) throw new Q.Bad("Query is required");
        needLen(query, "q", 80);
        int total = 0;
        Json res = Json.arr();
        long t0 = System.nanoTime();
        int runs = 20;
        for (int r = 0; r < runs; r++) {
            total = 0;
            for (int i = 0; i < d.records.length; i++) {
                String txt = d.records[i].searchableText().toLowerCase();
                total += find(algo, txt, query).length;
            }
        }
        long t1 = System.nanoTime();
        for (int i = 0; i < d.records.length; i++) {
            String txt = d.records[i].searchableText().toLowerCase();
            int[] pos = find(algo, txt, query);
            if (pos.length > 0) {
                int[] firstFew = new int[Math.min(pos.length, 8)];
                System.arraycopy(pos, 0, firstFew, 0, firstFew.length);
                res.add(Json.obj().put("id", d.records[i].getId()).put("crop", d.records[i].getCrop()).put("matches", pos.length)
                        .put("positions", firstFew).put("text", txt));
            }
        }
        return Json.obj().put("algorithm", algo).put("query", query).put("complexity", complexity(algo))
                .put("elapsedMicros", (t1 - t0) / 1000.0 / runs).put("totalMatches", total).put("results", res).toString();
    }

    String strlab(Q q) {
        String t = q.s("t", "ababcabcabababd");
        String p = q.s("p", "ababd");
        needLen(t, "t", 400);
        needLen(p, "p", 40);
        if (p.isEmpty()) throw new Q.Bad("Pattern is required");
        int mod = q.i("mod", 13, 2, 1_000_000);
        int[] pos = NaiveSearch.findAll(t, p);
        RabinKarp.Result weak = RabinKarp.scan(t, p, mod, 31, 0, 0, true);
        RabinKarp.Result dbl = RabinKarp.scan(t, p, RabinKarp.MOD1, RabinKarp.BASE1, RabinKarp.MOD2, RabinKarp.BASE2, true);
        Rng rng = new Rng(q.l("seed", System.nanoTime()));
        long rmod = MillerRabin.randomPrime(1_000_000_000L, 2_000_000_000L, rng);
        long rbase = 256 + rng.nextLong(rmod - 256);
        RabinKarp.Result rnd = RabinKarp.scan(t, p, rmod, rbase, 0, 0, true);
        int[] zs = ZAlgorithm.zArray(p);
        return Json.obj().put("text", t).put("pattern", p).put("positions", pos)
                .put("kmpPositions", KMP.findAll(t, p)).put("zPositions", ZAlgorithm.findAll(t, p))
                .put("rkPositions", dbl.positions)
                .put("lps", KMP.lps(p)).put("zPattern", zs)
                .put("naiveComparisons", StringLab.naiveComparisons(t, p)).put("kmpComparisons", StringLab.kmpComparisons(t, p))
                .put("rkWeak", Json.obj().put("mod", mod).put("hashHits", weak.hashHits).put("spurious", weak.spurious))
                .put("rkDouble", Json.obj().put("hashHits", dbl.hashHits).put("spurious", dbl.spurious))
                .put("rkRandom", Json.obj().put("mod", rmod).put("base", rbase).put("hashHits", rnd.hashHits).put("spurious", rnd.spurious)
                        .put("matches", rnd.positions.length)).toString();
    }

    private long[] time3(Runnable r) {
        r.run();                                  // warm-up
        long best = Long.MAX_VALUE;
        for (int i = 0; i < 3; i++) { long a = System.nanoTime(); r.run(); best = Math.min(best, System.nanoTime() - a); }
        return new long[]{best};
    }

    String race(Q q) {
        String mode = q.s("mode", "corpus");
        String text, pat;
        if (mode.equals("adversarial")) {
            int n = q.i("n", 20000, 100, 200_000), m = q.i("m", 60, 2, 400);
            StringBuilder a = new StringBuilder();
            for (int i = 0; i < n; i++) a.append('a');
            StringBuilder p = new StringBuilder();
            for (int i = 0; i < m - 1; i++) p.append('a');
            p.append('b');
            text = a.toString(); pat = p.toString();
        } else {
            int scale = q.i("scale", 200, 1, 400);
            pat = q.s("q", "blight").toLowerCase();
            needLen(pat, "q", 80);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < scale; i++) sb.append(d.corpus);
            text = sb.toString();
        }
        final String T = text, P = pat;
        Json rows = Json.arr();
        String[] names = {"Naive", "KMP", "Z-function", "Rabin-Karp (double hash)"};
        for (int k = 0; k < 4; k++) {
            final int which = k;
            final int[] cnt = new int[1];
            Runnable r = () -> {
                int[] x = which == 0 ? NaiveSearch.findAll(T, P) : which == 1 ? KMP.findAll(T, P) : which == 2 ? ZAlgorithm.findAll(T, P) : RabinKarp.findAll(T, P);
                cnt[0] = x.length;
            };
            long ns = time3(r)[0];
            Json row = Json.obj().put("name", names[k]).put("millis", ns / 1e6).put("matches", cnt[0]);
            if (k == 0) row.put("comparisons", StringLab.naiveComparisons(T, P));
            if (k == 1) row.put("comparisons", StringLab.kmpComparisons(T, P));
            rows.add(row);
        }
        boolean saOk = T.length() <= 150_000;
        if (saOk) {
            final int[][] saBox = new int[1][];
            long build = time3(() -> saBox[0] = SuffixArray.build(T))[0];
            final int[] cnt = new int[1];
            long query = time3(() -> cnt[0] = SuffixArray.upperBound(T, saBox[0], P) - SuffixArray.lowerBound(T, saBox[0], P))[0];
            rows.add(Json.obj().put("name", "Suffix array: build once").put("millis", build / 1e6).put("matches", cnt[0]));
            rows.add(Json.obj().put("name", "Suffix array: each query").put("millis", query / 1e6).put("matches", cnt[0]));
        }
        return Json.obj().put("mode", mode).put("textLength", T.length()).put("patternLength", P.length())
                .put("saIncluded", saOk).put("rows", rows).toString();
    }

    String multi(Q q) {
        String[] words = q.list("words", ",", new String[]{"blast", "rust", "blight", "wilt", "rot", "curl", "tikka"});
        if (words.length > 40) throw new Q.Bad("max 40 words");
        for (int i = 0; i < words.length; i++) { words[i] = words[i].toLowerCase(); needLen(words[i], "word", 40); }
        boolean own = q.has("text");
        String text = own ? q.s("text", "").toLowerCase() : d.corpus;
        needLen(text, "text", 5000);
        long a = System.nanoTime();
        AhoCorasick ac = new AhoCorasick(words);
        int[] hits = ac.search(text, words);
        long b = System.nanoTime();
        int[] perWord = new int[words.length];
        int[] perRec = new int[d.records.length];
        Json all = Json.arr();
        for (int i = 0; i < hits.length; i += 2) {
            perWord[hits[i]]++;
            if (!own) perRec[d.recordAt(hits[i + 1])]++;
            if (i / 2 < 300) all.add(Json.obj().put("word", words[hits[i]]).put("pos", hits[i + 1]).put("len", words[hits[i]].length()));
        }
        Json wj = Json.arr();
        for (int i = 0; i < words.length; i++) wj.add(Json.obj().put("word", words[i]).put("count", perWord[i]));
        Json rj = Json.arr();
        if (!own) for (int i = 0; i < d.records.length; i++) if (perRec[i] > 0) rj.add(Json.obj().put("crop", d.records[i].getCrop()).put("hits", perRec[i]));
        return Json.obj().put("text", text).put("trieNodes", ac.nodeCount()).put("failLinks", ac.failLinkCount())
                .put("totalMatches", hits.length / 2).put("micros", (b - a) / 1000.0).put("words", wj).put("crops", rj)
                .put("matches", all).put("complexity", "build O(sum of pattern lengths), scan O(text + matches)").toString();
    }

    String suffix(Q q) {
        String query = q.s("q", "").toLowerCase();
        if (query.isEmpty()) throw new Q.Bad("Query is required");
        needLen(query, "q", 80);
        int lo = SuffixArray.lowerBound(d.corpus, d.sa, query), hi = SuffixArray.upperBound(d.corpus, d.sa, query);
        int count = Math.max(hi - lo, 0);
        int show = Math.min(count, 40);
        int[] pos = new int[show];
        String[] crops = new String[show];
        for (int i = 0; i < show; i++) { pos[i] = d.sa[lo + i]; crops[i] = d.records[d.recordAt(pos[i])].getCrop(); }
        int[] lcpS = new int[Math.min(Math.max(count - 1, 0), 12)];
        for (int i = 0; i < lcpS.length; i++) lcpS[i] = d.lcp[lo + 1 + i];
        return Json.obj().put("query", query).put("suffixCount", count).put("lcpSample", lcpS).put("positions", pos).put("crops", crops)
                .put("saRange", new int[]{lo, hi})
                .put("complexity", "Index construction: practical O(n log^2 n); query uses binary search").toString();
    }

    String repeats() {
        int[] r = SuffixTools.longestRepeated(d.corpus);
        long distinct = SuffixTools.distinctSubstrings(d.corpus.length(), d.lcp);
        int[] occ = KMP.findAll(d.corpus, d.corpus.substring(r[0], r[0] + r[1]));
        String[] crops = new String[occ.length];
        for (int i = 0; i < occ.length; i++) crops[i] = d.records[d.recordAt(occ[i])].getCrop();
        return Json.obj().put("repeated", d.corpus.substring(r[0], r[0] + r[1])).put("length", r[1]).put("occurrences", occ).put("crops", crops)
                .put("distinctSubstrings", distinct).put("corpusChars", d.corpus.length())
                .put("note", "Found by scanning the LCP array for its maximum - O(n) after the suffix array exists.").toString();
    }

    // ------------------------------------------------------------------ Module 3
    String editDistance(Q q) {
        String query = q.s("q", "").toLowerCase();
        if (query.isEmpty()) throw new Q.Bad("Query is required");
        needLen(query, "q", 40);
        boolean dam = q.s("mode", "lev").startsWith("dam");
        String[] original = {"rice", "wheat", "tomato", "cotton", "fertilizer", "fertilizers", "irrigation", "pesticide", "compost",
                "urea", "blast", "rust", "blight", "drip irrigation", "organic farming", "soil"};
        String[] terms = new String[original.length + d.vocab.length];
        System.arraycopy(original, 0, terms, 0, original.length);
        int c = original.length;
        for (String w : d.vocab) {
            boolean dup = false;
            for (int i = 0; i < original.length; i++) if (original[i].equals(w)) dup = true;
            if (!dup) terms[c++] = w;
        }
        String[] list = new String[c];
        System.arraycopy(terms, 0, list, 0, c);
        int[] dist = new int[c];
        for (int i = 0; i < c; i++) dist[i] = dam ? EditDistance.damerau(query, list[i]) : EditDistance.distance(query, list[i]);
        for (int i = 1; i < c; i++) {                 // insertion sort by distance, then alphabetical
            String s = list[i]; int dv = dist[i], j = i - 1;
            while (j >= 0 && (dist[j] > dv || (dist[j] == dv && list[j].compareTo(s) > 0))) { list[j + 1] = list[j]; dist[j + 1] = dist[j]; j--; }
            list[j + 1] = s; dist[j + 1] = dv;
        }
        Json res = Json.arr();
        for (int i = 0; i < Math.min(8, c); i++) res.add(Json.obj().put("term", list[i]).put("distance", dist[i]));
        return Json.obj().put("query", query).put("mode", dam ? "damerau" : "levenshtein")
                .put("complexity", "O(m*n)").put("vocabulary", c).put("results", res).toString();
    }

    String edit(Q q) {
        String a = q.s("a", "blast"), b = q.s("b", "balst");
        needLen(a, "a", 40); needLen(b, "b", 40);
        int wi = q.i("wi", 2, 1, 20), wd = q.i("wd", 2, 1, 20), ws = q.i("ws", 3, 1, 20);
        int[][] t = EditDistance.table(a, b);
        return Json.obj().put("a", a).put("b", b).put("levenshtein", t[a.length()][b.length()])
                .put("damerau", EditDistance.damerau(a, b)).put("weighted", EditDistance.weighted(a, b, wi, wd, ws))
                .put("table", t).put("path", EditDistance.path(a, b)).put("ops", EditDistance.ops(a, b)).toString();
    }

    String align(Q q) {
        String a = q.s("a", "agrifloww").toLowerCase(), b = q.s("b", "agriflow").toLowerCase();
        needLen(a, "a", 60); needLen(b, "b", 60);
        String mode = q.s("mode", "global");
        int match = q.i("match", 2, 0, 10), mismatch = q.i("mismatch", -1, -10, 0);
        int gap = q.i("gap", 2, 0, 10), open = q.i("open", 4, 0, 20), extend = q.i("extend", 1, 0, 20);
        Alignment.Result r = mode.equals("local") ? Alignment.local(a, b, match, mismatch, gap)
                : mode.equals("affine") ? Alignment.affine(a, b, match, mismatch, open, extend)
                : Alignment.global(a, b, match, mismatch, gap);
        StringBuilder mid = new StringBuilder();
        for (int i = 0; i < r.alignedA.length(); i++) {
            char x = r.alignedA.charAt(i), y = r.alignedB.charAt(i);
            mid.append(x == '-' || y == '-' ? ' ' : (x == y ? '|' : '.'));
        }
        int len = Math.max(1, r.alignedA.length());
        return Json.obj().put("mode", mode).put("score", r.score).put("alignedA", r.alignedA).put("alignedB", r.alignedB)
                .put("midline", mid.toString()).put("matches", r.matches).put("mismatches", r.mismatches).put("gapChars", r.gapChars)
                .put("identity", 100.0 * r.matches / len).put("table", r.table).put("path", r.path).toString();
    }

    String matrix(Q q) {
        int[] dims = q.ints("dims", new int[]{40, 20, 30, 10, 30});
        if (dims.length < 2 || dims.length > 14) throw new Q.Bad("give between 2 and 14 numbers in dims");
        for (int x : dims) if (x < 1 || x > 10000) throw new Q.Bad("dims must be between 1 and 10000");
        IntervalDP.ChainResult r = IntervalDP.matrixChain(dims);
        return Json.obj().put("dims", dims).put("cost", r.cost).put("parens", r.parens).put("leftToRight", r.leftToRightCost)
                .put("saved", r.leftToRightCost - r.cost).toString();
    }

    private static long balancedCost(long[] f, int i, int j, int depth) {
        if (i >= j) return 0;
        int mid = (i + j) >>> 1;
        return f[mid] * depth + balancedCost(f, i, mid, depth + 1) + balancedCost(f, mid + 1, j, depth + 1);
    }

    String obst(Q q) {
        int n = d.cropsSorted.length;
        int[] dflt = {30, 12, 9, 6, 4, 3, 22, 8, 5, 10, 14, 2, 18, 7, 25};
        int[] fr = q.ints("freq", dflt);
        if (fr.length != n) throw new Q.Bad("give exactly " + n + " frequencies (one per crop, alphabetical order)");
        long[] f = new long[n];
        for (int i = 0; i < n; i++) { if (fr[i] < 1 || fr[i] > 100000) throw new Q.Bad("frequencies must be 1..100000"); f[i] = fr[i]; }
        IntervalDP.ObstResult r = IntervalDP.obst(f);
        return Json.obj().put("keys", d.cropsSorted).put("freq", f).put("cost", r.cost).put("balancedCost", balancedCost(f, 0, n, 1))
                .put("opsCubic", r.opsCubic).put("opsKnuth", r.opsKnuth).put("root", r.root).put("left", r.left).put("right", r.right).toString();
    }

    private double[][] sites(int[] pick) {
        double[][] dm = new double[pick.length][pick.length];
        for (int i = 0; i < pick.length; i++)
            for (int j = 0; j < pick.length; j++)
                dm[i][j] = Math.hypot(Data.SITE_X[pick[i]] - Data.SITE_X[pick[j]], Data.SITE_Y[pick[i]] - Data.SITE_Y[pick[j]]);
        return dm;
    }

    String route(Q q) {
        int[] pick = q.ints("pick", new int[]{0, 1, 2, 3, 4, 5, 6, 7});
        if (pick.length < 2 || pick.length > 14) throw new Q.Bad("choose between 2 and 14 farms");
        for (int p : pick) if (p < 0 || p >= Data.SITE_NAMES.length) throw new Q.Bad("farm index out of range");
        double[][] dm = sites(pick);
        long t0 = System.nanoTime();
        Tsp.Result ex = Tsp.exact(dm);
        long t1 = System.nanoTime();
        Tsp.Result nn = Tsp.nearestNeighbour(dm), ms = Tsp.mstApprox(dm);
        Json nodes = Json.arr();
        for (int i = 0; i < pick.length; i++)
            nodes.add(Json.obj().put("id", pick[i]).put("name", Data.SITE_NAMES[pick[i]]).put("x", Data.SITE_X[pick[i]]).put("y", Data.SITE_Y[pick[i]]));
        return Json.obj().put("nodes", nodes)
                .put("exact", Json.obj().put("cost", ex.cost).put("order", ex.order).put("states", ex.states).put("millis", (t1 - t0) / 1e6))
                .put("nearest", Json.obj().put("cost", nn.cost).put("order", nn.order))
                .put("mst", Json.obj().put("cost", ms.cost).put("order", ms.order).put("lowerBound", ms.mst))
                .put("bitmaskStatesMax", (long) Math.pow(2, pick.length) * pick.length * pick.length).toString();
    }

    String hamilton(Q q) {
        int n = q.i("n", 8, 2, 16);
        String[] es = q.list("edges", ",", new String[]{"0-1", "1-2", "2-3", "3-4", "4-5", "5-6", "6-7", "1-5", "2-6", "0-4"});
        int[] adj = new int[n];
        for (String e : es) {
            String[] p = e.split("-");
            if (p.length != 2) throw new Q.Bad("edges look like 0-1,1-2");
            int u, v;
            try { u = Integer.parseInt(p[0].trim()); v = Integer.parseInt(p[1].trim()); } catch (NumberFormatException x) { throw new Q.Bad("bad edge " + e); }
            if (u < 0 || v < 0 || u >= n || v >= n || u == v) throw new Q.Bad("edge " + e + " is out of range");
            adj[u] |= 1 << v; adj[v] |= 1 << u;
        }
        int[] path = Tsp.hamiltonianPath(n, adj);
        Json o = Json.obj().put("n", n).put("exists", path != null);
        if (path != null) o.put("path", path);
        return o.toString();
    }

    String setcover(Q q) {
        String[] labels = q.list("labels", ",", new String[]{"Blast", "Rust", "Blight", "Wilt", "Leaf curl", "Red rot", "Rhizome rot", "Tikka"});
        int u = labels.length;
        if (u > 16) throw new Q.Bad("at most 16 problems");
        String[] sets = q.list("sets", ";", new String[]{"Tricyclazole:0", "Mancozeb:1,2,7", "Copper fungicide:2,5,6", "Trichoderma:3,6,5",
                "Imidacloprid:4", "Balanced NPK + sanitation:0,1,2", "Seed treatment:3,5,7", "Neem oil:4,7,1"});
        int[] masks = new int[sets.length];
        String[] names = new String[sets.length];
        for (int i = 0; i < sets.length; i++) {
            int c = sets[i].lastIndexOf(':');
            if (c < 0) throw new Q.Bad("each package looks like Name:0,1,2");
            names[i] = sets[i].substring(0, c).trim();
            for (String x : sets[i].substring(c + 1).split(",")) {
                int b;
                try { b = Integer.parseInt(x.trim()); } catch (NumberFormatException e) { throw new Q.Bad("bad number in " + sets[i]); }
                if (b < 0 || b >= u) throw new Q.Bad("problem index " + b + " is out of range");
                masks[i] |= 1 << b;
            }
        }
        long t0 = System.nanoTime();
        int[] ex = SetCover.exact(masks, u);
        long t1 = System.nanoTime();
        int[] gr = SetCover.greedy(masks, u);
        Json o = Json.obj().put("labels", labels).put("packages", names).put("masks", masks).put("coverable", ex != null)
                .put("exactMillis", (t1 - t0) / 1e6);
        if (ex != null) o.put("exact", ex).put("greedy", gr);
        return o.toString();
    }

    String tree(Q q) {
        String[] es = q.list("edges", ",", new String[]{"0-1:4", "0-2:3", "1-3:5", "1-4:2", "2-5:6", "2-6:2", "4-7:3", "5-8:4", "5-9:1", "6-10:5", "8-11:2"});
        int n = es.length + 1;
        if (n > 40) throw new Q.Bad("max 40 nodes");
        int[] eu = new int[es.length], ev = new int[es.length], ew = new int[es.length];
        for (int i = 0; i < es.length; i++) {
            try {
                String[] a = es[i].split(":");
                String[] uv = a[0].split("-");
                eu[i] = Integer.parseInt(uv[0].trim()); ev[i] = Integer.parseInt(uv[1].trim());
                ew[i] = a.length > 1 ? Integer.parseInt(a[1].trim()) : 1;
            } catch (RuntimeException e) { throw new Q.Bad("edges look like 0-1:4,0-2:3 (node-node:length)"); }
            if (eu[i] < 0 || ev[i] < 0 || eu[i] >= n || ev[i] >= n || eu[i] == ev[i] || ew[i] < 1) throw new Q.Bad("bad edge " + es[i] + " (nodes must be 0.." + (n - 1) + ")");
        }
        // must be connected: n-1 edges + connected = tree
        int[] comp = new int[n];
        for (int i = 0; i < n; i++) comp[i] = i;
        for (int i = 0; i < es.length; i++) {
            int a = comp[eu[i]], b = comp[ev[i]];
            if (a == b) throw new Q.Bad("that has a loop - a tree can't");
            for (int k = 0; k < n; k++) if (comp[k] == b) comp[k] = a;
        }
        long[] nw = new long[n];
        int[] wInts = q.ints("yield", null);
        for (int i = 0; i < n; i++) nw[i] = (wInts != null && i < wInts.length) ? wInts[i] : (3 + (i * 7) % 9);
        TreeDP.Result r = TreeDP.analyze(n, eu, ev, ew, nw);
        return Json.obj().put("n", n).put("eu", eu).put("ev", ev).put("ew", ew).put("yield", nw).put("diameter", r.diameter)
                .put("diameterPath", r.diameterPath).put("sumDist", r.sumDist).put("bestHub", r.bestHub).put("centroid", r.centroid)
                .put("maxIndependent", r.maxIndependent).put("picked", r.picked).put("parent", r.parent).toString();
    }

    String sos(Q q) {
        String[] labels = q.list("labels", ",", new String[]{"Blast", "Rust", "Blight", "Wilt", "Curl", "Rot"});
        int k = labels.length;
        if (k > 10) throw new Q.Bad("max 10 problems");
        int[] farms = q.ints("farms", new int[]{1, 3, 4, 2, 9, 6, 8, 5, 5, 16, 7, 33, 0, 2, 12, 24, 40, 10, 3, 1});
        int[] tbl = new int[1 << k];
        for (int m : farms) { if (m < 0 || m >= (1 << k)) throw new Q.Bad("farm mask " + m + " needs bits 0.." + (k - 1) + " only"); tbl[m]++; }
        long[] f = new long[1 << k];
        for (int i = 0; i < f.length; i++) f[i] = tbl[i];
        long[] g = SubsetDP.sos(k, f);
        int pkg = q.i("package", 7, 0, (1 << k) - 1);
        return Json.obj().put("labels", labels).put("k", k).put("farms", farms.length).put("exact", f).put("covered", g)
                .put("package", pkg).put("fullyHandled", g[pkg]).put("notHandled", farms.length - g[pkg])
                .put("fastOps", (long) k * (1 << k)).put("slowOps", (long) Math.pow(3, k)).toString();
    }

    String greedy(Q q) {
        int[] coins = q.ints("coins", new int[]{1, 3, 4});
        int target = q.i("target", 6, 1, 5000);
        if (coins.length > 10) throw new Q.Bad("max 10 bag sizes");
        for (int c : coins) if (c < 1 || c > 5000) throw new Q.Bad("bag sizes must be 1..5000");
        int[] g = SubsetDP.coinGreedy(coins, target), dp = SubsetDP.coinDP(coins, target);
        Json o = Json.obj().put("coins", coins).put("target", target);
        if (g != null) o.put("greedy", g); else o.putNull("greedy");
        if (dp != null) o.put("dp", dp); else o.putNull("dp");
        return o.toString();
    }
}
